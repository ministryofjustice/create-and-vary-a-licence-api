package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.CreateLicenceRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StaffRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StandardConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.Prison
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.CommunityApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.CommunityOrPrisonOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.OffenderDetail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ProbationSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.APPROVED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.IN_PROGRESS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.REJECTED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.SUBMITTED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType.Companion.getLicenceType
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.LicenceEvent as EntityLicenceEvent

@Service
class LicenceCreationService(
  private val licenceRepository: LicenceRepository,
  private val staffRepository: StaffRepository,
  private val standardConditionRepository: StandardConditionRepository,
  private val licenceEventRepository: LicenceEventRepository,
  private val licencePolicyService: LicencePolicyService,
  private val auditEventRepository: AuditEventRepository,
  private val probationSearchApiClient: ProbationSearchApiClient,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val prisonApiClient: PrisonApiClient,
  private val communityApiClient: CommunityApiClient,
  private val frontendPayloadTakesPriority: Boolean = true,
) {

  companion object {
    private val log = LoggerFactory.getLogger(LicenceCreationService::class.java)
  }

  @Transactional
  fun createLicence(request: CreateLicenceRequest): LicenceSummary {
    val nomsId = request.nomsId!!

    if (offenderHasLicenceInFlight(nomsId)) {
      throw ValidationException("A licence already exists for this person (IN_PROGRESS, SUBMITTED, APPROVED or REJECTED)")
    }

    val username = SecurityContextHolder.getContext().authentication.name

    val nomisRecord = prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(nomsId)).first()
    val deliusRecord = probationSearchApiClient.searchForPersonOnProbation(nomsId)
    val prisonInformation = prisonApiClient.getPrisonInformation(nomisRecord.prisonId)
    val offenderManagers = communityApiClient.getAllOffenderManagers(deliusRecord.otherIds.crn)

    val currentActiveOffenderManager = deliusRecord.offenderManagers.find { it.active } ?: error(
      "No active offender manager found for $nomsId",
    )
    val currentResponsibleOfficerDetails = offenderManagers.find {
      it.staffCode == currentActiveOffenderManager.staffDetail.code
    } ?: error("No responsible officer details found for $nomsId")

    val responsibleCom = staffRepository.findByStaffIdentifier(currentResponsibleOfficerDetails.staffId)
      ?: error("Staff with staffIdentifier ${currentResponsibleOfficerDetails.staffId} not found")

    val createdBy = staffRepository.findByUsernameIgnoreCase(username)
      ?: error("Staff with username $username not found")

    val licenceType = getLicenceType(nomisRecord)

    val (createLicenceRequest, licenceEqualityCheck) = getRequestToSave(
      licenceType,
      nomsId,
      nomisRecord,
      prisonInformation,
      currentResponsibleOfficerDetails,
      deliusRecord,
      request,
    )

    val licence = transform(createLicenceRequest)

    licence.dateCreated = LocalDateTime.now()
    licence.responsibleCom = responsibleCom
    licence.createdBy = createdBy
    licence.updatedByUsername = username

    val licenceEntity = licenceRepository.saveAndFlush(licence)
    val createLicenceResponse = transformToLicenceSummary(licenceEntity)

    val entityStandardLicenceConditions =
      createLicenceRequest.standardLicenceConditions.transformToEntityStandard(licenceEntity, "AP")
    val entityStandardPssConditions =
      createLicenceRequest.standardPssConditions.transformToEntityStandard(licenceEntity, "PSS")
    standardConditionRepository.saveAllAndFlush(entityStandardLicenceConditions + entityStandardPssConditions)

    auditEventRepository.saveAndFlush(
      AuditEvent(
        licenceId = createLicenceResponse.licenceId,
        username = username,
        fullName = "${createdBy.firstName} ${createdBy.lastName}",
        summary = "Licence created for ${createLicenceRequest.forename} ${createLicenceRequest.surname}",
        detail = "ID ${licenceEntity.id} type ${licenceEntity.typeCode} status ${licenceEntity.statusCode.name} version ${licenceEntity.version}",
      ),
    )

    licenceEventRepository.saveAndFlush(
      EntityLicenceEvent(
        licenceId = createLicenceResponse.licenceId,
        eventType = LicenceEventType.CREATED,
        username = username,
        forenames = createdBy.firstName,
        surname = createdBy.lastName,
        eventDescription = "Licence created for ${licenceEntity.forename} ${licenceEntity.surname}",
      ),
    )

    if (licenceEqualityCheck.isNotEmpty()) {
      log.warn("Licence inconsistency for ${licenceEntity.id} with the following differences: $licenceEqualityCheck")
    } else {
      log.info("Licence request and creation are identical")
    }

    return createLicenceResponse
  }

  private fun getRequestToSave(
    licenceType: LicenceType,
    nomsId: String,
    nomisRecord: PrisonerSearchPrisoner,
    prisonInformation: Prison,
    currentResponsibleOfficerDetails: CommunityOrPrisonOffenderManager,
    deliusRecord: OffenderDetail,
    request: CreateLicenceRequest,
  ): Pair<CreateLicenceRequest, List<String>> {
    val createLicenceRequest = createLicenceRequest(
      licenceType,
      nomsId,
      nomisRecord,
      prisonInformation,
      currentResponsibleOfficerDetails,
      deliusRecord,
    )

    val licenceEqualityCheck = checkCreatedLicence(request, createLicenceRequest)
      .filter { (test, _) -> !test }
      .map { (_, field) -> field }

    val requestToSave = if (frontendPayloadTakesPriority) request else createLicenceRequest
    return Pair(requestToSave, licenceEqualityCheck)
  }

  private fun createLicenceRequest(
    licenceType: LicenceType,
    nomsId: String,
    nomisRecord: PrisonerSearchPrisoner,
    prisonInformation: Prison,
    currentResponsibleOfficerDetails: CommunityOrPrisonOffenderManager,
    deliusRecord: OffenderDetail,
  ) = CreateLicenceRequest(
    typeCode = licenceType,
    version = licencePolicyService.currentPolicy().version,
    nomsId = nomsId,
    bookingNo = nomisRecord.bookNumber,
    bookingId = nomisRecord.bookingId.toLong(),
    prisonCode = nomisRecord.prisonId,
    forename = nomisRecord.firstName.convertToTitleCase(),
    middleNames = nomisRecord.middleNames?.convertToTitleCase() ?: "",
    surname = nomisRecord.lastName.convertToTitleCase(),
    dateOfBirth = nomisRecord.dateOfBirth,
    conditionalReleaseDate = nomisRecord.conditionalReleaseDateOverrideDate ?: nomisRecord.conditionalReleaseDate,
    actualReleaseDate = nomisRecord.confirmedReleaseDate,
    sentenceStartDate = nomisRecord.sentenceStartDate,
    sentenceEndDate = nomisRecord.sentenceExpiryDate,
    licenceStartDate = nomisRecord.confirmedReleaseDate ?: nomisRecord.conditionalReleaseDate,
    licenceExpiryDate = nomisRecord.licenceExpiryDate,
    topupSupervisionStartDate = nomisRecord.topupSupervisionStartDate,
    topupSupervisionExpiryDate = nomisRecord.topupSupervisionExpiryDate,
    prisonDescription = prisonInformation.description,
    prisonTelephone = prisonInformation.getPrisonContactNumber(),
    probationAreaCode = currentResponsibleOfficerDetails.probationArea.code,
    probationAreaDescription = currentResponsibleOfficerDetails.probationArea.description,
    probationPduCode = currentResponsibleOfficerDetails.team.borough.code,
    probationPduDescription = currentResponsibleOfficerDetails.team.borough.description,
    probationLauCode = currentResponsibleOfficerDetails.team.district.code,
    probationLauDescription = currentResponsibleOfficerDetails.team.district.description,
    probationTeamCode = currentResponsibleOfficerDetails.team.code,
    probationTeamDescription = currentResponsibleOfficerDetails.team.description,
    crn = deliusRecord.otherIds.crn,
    pnc = deliusRecord.otherIds.pncNumber,
    cro = deliusRecord.otherIds.croNumber ?: nomisRecord.croNumber,
    standardLicenceConditions = licencePolicyService.getCurrentStandardApConditions(licenceType),
    standardPssConditions = licencePolicyService.getCurrentStandardPssConditions(licenceType),
    responsibleComStaffId = currentResponsibleOfficerDetails.staffId,
  )

  fun checkCreatedLicence(
    frontendRequest: CreateLicenceRequest,
    backendRequest: CreateLicenceRequest,
  ): List<Pair<Boolean, String>> {
    return listOf(
      (frontendRequest.typeCode == backendRequest.typeCode) to "type code",
      (frontendRequest.version == backendRequest.version) to "version",
      (frontendRequest.nomsId == backendRequest.nomsId) to "nomsId",
      (frontendRequest.bookingNo == backendRequest.bookingNo) to "bookingNo",
      (frontendRequest.bookingId == backendRequest.bookingId) to "bookingId",
      (frontendRequest.prisonCode == backendRequest.prisonCode) to "prisonCode",
      (frontendRequest.forename == backendRequest.forename) to "forename",
      (frontendRequest.middleNames == backendRequest.middleNames) to "middleNames",
      (frontendRequest.surname == backendRequest.surname) to "surname",
      (frontendRequest.dateOfBirth == backendRequest.dateOfBirth) to "dateOfBirth",
      (frontendRequest.conditionalReleaseDate == backendRequest.conditionalReleaseDate) to "conditionalReleaseDate",
      (frontendRequest.actualReleaseDate == backendRequest.actualReleaseDate) to "actualReleaseDate",
      (frontendRequest.sentenceStartDate == backendRequest.sentenceStartDate) to "sentenceStartDate",
      (frontendRequest.sentenceEndDate == backendRequest.sentenceEndDate) to "sentenceEndDate",
      (frontendRequest.licenceStartDate == backendRequest.licenceStartDate) to "licenceStartDate",
      (frontendRequest.licenceExpiryDate == backendRequest.licenceExpiryDate) to "licenceExpiryDate",
      (frontendRequest.topupSupervisionStartDate == backendRequest.topupSupervisionStartDate) to "topupSupervisionStartDate",
      (frontendRequest.topupSupervisionExpiryDate == backendRequest.topupSupervisionExpiryDate) to "topupSupervisionExpiryDate",
      (frontendRequest.prisonDescription == backendRequest.prisonDescription) to "prisonDescription",
      (frontendRequest.prisonTelephone == backendRequest.prisonTelephone) to "prisonTelephone",
      (frontendRequest.probationAreaCode == backendRequest.probationAreaCode) to "probationAreaCode",
      (frontendRequest.probationAreaDescription == backendRequest.probationAreaDescription) to "probationAreaDescription",
      (frontendRequest.probationPduCode == backendRequest.probationPduCode) to "probationPduCode",
      (frontendRequest.probationPduDescription == backendRequest.probationPduDescription) to "probationPduDescription",
      (frontendRequest.probationLauCode == backendRequest.probationLauCode) to "probationLauCode",
      (frontendRequest.probationLauDescription == backendRequest.probationLauDescription) to "probationLauDescription",
      (frontendRequest.probationTeamCode == backendRequest.probationTeamCode) to "probationTeamCode",
      (frontendRequest.probationTeamDescription == backendRequest.probationTeamDescription) to "probationTeamDescription",
      (frontendRequest.crn == backendRequest.crn) to "crn",
      (frontendRequest.pnc == backendRequest.pnc) to "pnc",
      (frontendRequest.cro == backendRequest.cro) to "cro",
      (frontendRequest.standardLicenceConditions == backendRequest.standardLicenceConditions) to "standardLicenceConditions",
      (frontendRequest.standardPssConditions == backendRequest.standardPssConditions) to "standardPssConditions",
      (frontendRequest.responsibleComStaffId == backendRequest.responsibleComStaffId) to "responsibleComStaffId",
    )
  }

  private fun offenderHasLicenceInFlight(nomsId: String): Boolean {
    val inFlight =
      licenceRepository.findAllByNomsIdAndStatusCodeIn(nomsId, listOf(IN_PROGRESS, SUBMITTED, APPROVED, REJECTED))
    return inFlight.isNotEmpty()
  }
}
