package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Creator
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.PrisonUser
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceCreationResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StaffRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StandardConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.ResourceAlreadyExistsException
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.CommunityOrPrisonOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.OffenderDetail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ProbationSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.APPROVED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.IN_PROGRESS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.REJECTED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.SUBMITTED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.TIMED_OUT
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType.Companion.getLicenceType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.LicenceEvent as EntityLicenceEvent

@Service
class LicenceCreationService(
  private val licenceRepository: LicenceRepository,
  private val staffRepository: StaffRepository,
  private val standardConditionRepository: StandardConditionRepository,
  private val additionalConditionRepository: AdditionalConditionRepository,
  private val licenceEventRepository: LicenceEventRepository,
  private val licencePolicyService: LicencePolicyService,
  private val auditEventRepository: AuditEventRepository,
  private val probationSearchApiClient: ProbationSearchApiClient,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val prisonApiClient: PrisonApiClient,
  private val deliusApiClient: DeliusApiClient,
) {
  companion object {
    private val log = LoggerFactory.getLogger(LicenceCreationService::class.java)
  }

  @Transactional
  fun createLicence(prisonNumber: String): LicenceCreationResponse {
    verifyNoInFlightLicence(prisonNumber)

    val username = SecurityContextHolder.getContext().authentication.name

    val nomisRecord = prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(prisonNumber)).first()
    val deliusRecord = probationSearchApiClient.searchForPersonOnProbation(prisonNumber)
    val prisonInformation = prisonApiClient.getPrisonInformation(nomisRecord.prisonId!!)
    val currentResponsibleOfficerDetails = getCurrentResponsibleOfficer(deliusRecord, prisonNumber)

    val responsibleCom = staffRepository.findByStaffIdentifier(currentResponsibleOfficerDetails.id)
      ?: createCom(currentResponsibleOfficerDetails.id)

    val createdBy = staffRepository.findByUsernameIgnoreCase(username) as CommunityOffenderManager?
      ?: error("Staff with username $username not found")

    val licence = LicenceFactory.createCrd(
      licenceType = getLicenceType(nomisRecord),
      nomsId = nomisRecord.prisonerNumber,
      version = licencePolicyService.currentPolicy().version,
      nomisRecord = nomisRecord,
      prisonInformation = prisonInformation,
      currentResponsibleOfficerDetails = currentResponsibleOfficerDetails,
      deliusRecord = deliusRecord,
      responsibleCom = responsibleCom,
      creator = createdBy,
    )

    val createdLicence = licenceRepository.saveAndFlush(licence)

    val standardConditions = licencePolicyService.getStandardConditionsForLicence(createdLicence)
    standardConditionRepository.saveAllAndFlush(standardConditions)

    recordLicenceCreation(createdBy, createdLicence)

    return LicenceCreationResponse(createdLicence.id)
  }

  @Transactional
  fun createHardStopLicence(prisonNumber: String): LicenceCreationResponse {
    verifyNoInFlightLicence(prisonNumber)

    val username = SecurityContextHolder.getContext().authentication.name

    val nomisRecord = prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(prisonNumber)).first()
    val deliusRecord = probationSearchApiClient.searchForPersonOnProbation(prisonNumber)
    val prisonInformation = prisonApiClient.getPrisonInformation(nomisRecord.prisonId!!)
    val currentResponsibleOfficerDetails = getCurrentResponsibleOfficer(deliusRecord, prisonNumber)

    val responsibleCom = staffRepository.findByStaffIdentifier(currentResponsibleOfficerDetails.id)
      ?: createCom(currentResponsibleOfficerDetails.id)

    val createdBy = staffRepository.findByUsernameIgnoreCase(username) as PrisonUser?
      ?: error("Staff with username $username not found")

    val timedOutLicence: CrdLicence? = licenceRepository.findAllByBookingIdInAndStatusCodeOrderByDateCreatedDesc(
      listOf(nomisRecord.bookingId!!.toLong()),
      TIMED_OUT,
    ).firstOrNull()

    val licence = LicenceFactory.createHardStop(
      licenceType = getLicenceType(nomisRecord),
      nomsId = nomisRecord.prisonerNumber,
      version = licencePolicyService.currentPolicy().version,
      nomisRecord = nomisRecord,
      prisonInformation = prisonInformation,
      currentResponsibleOfficerDetails = currentResponsibleOfficerDetails,
      deliusRecord = deliusRecord,
      responsibleCom = responsibleCom,
      creator = createdBy,
      timedOutLicence = timedOutLicence,
    )

    val createdLicence = licenceRepository.saveAndFlush(licence)

    val standardConditions = licencePolicyService.getStandardConditionsForLicence(createdLicence)
    standardConditionRepository.saveAllAndFlush(standardConditions)

    val additionalConditions = licencePolicyService.getHardStopAdditionalConditions(createdLicence)
    additionalConditionRepository.saveAllAndFlush(additionalConditions)

    recordLicenceCreation(createdBy, createdLicence)

    return LicenceCreationResponse(createdLicence.id)
  }

  @Transactional
  fun createHdcLicence(prisonNumber: String): LicenceCreationResponse {
    verifyNoInFlightLicence(prisonNumber)

    val username = SecurityContextHolder.getContext().authentication.name

    val nomisRecord = prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(prisonNumber)).first()

    if (getLicenceType(nomisRecord) == LicenceType.PSS) error("HDC Licence for ${nomisRecord.prisonerNumber} can not be of type PSS")

    val deliusRecord = probationSearchApiClient.searchForPersonOnProbation(prisonNumber)
    val prisonInformation = prisonApiClient.getPrisonInformation(nomisRecord.prisonId!!)
    val currentResponsibleOfficerDetails = getCurrentResponsibleOfficer(deliusRecord, prisonNumber)

    val responsibleCom = staffRepository.findByStaffIdentifier(currentResponsibleOfficerDetails.id)
      ?: createCom(currentResponsibleOfficerDetails.id)

    val createdBy = staffRepository.findByUsernameIgnoreCase(username) as CommunityOffenderManager?
      ?: error("Staff with username $username not found")

    val licence = LicenceFactory.createHdc(
      licenceType = getLicenceType(nomisRecord),
      nomsId = nomisRecord.prisonerNumber,
      version = licencePolicyService.currentPolicy().version,
      nomisRecord = nomisRecord,
      prisonInformation = prisonInformation,
      currentResponsibleOfficerDetails = currentResponsibleOfficerDetails,
      deliusRecord = deliusRecord,
      responsibleCom = responsibleCom,
      creator = createdBy,
    )

    val createdLicence = licenceRepository.saveAndFlush(licence)

    val standardConditions = licencePolicyService.getStandardConditionsForLicence(createdLicence)
    standardConditionRepository.saveAllAndFlush(standardConditions)

    recordLicenceCreation(createdBy, createdLicence)

    return LicenceCreationResponse(createdLicence.id)
  }

  private fun recordLicenceCreation(
    creator: Creator,
    licence: Licence,
  ) {
    auditEventRepository.saveAndFlush(
      AuditEvent(
        licenceId = licence.id,
        username = creator.username,
        fullName = "${creator.firstName} ${creator.lastName}",
        summary = "Licence created for ${licence.forename} ${licence.surname}",
        detail = "ID ${licence.id} type ${licence.typeCode} status ${licence.statusCode.name} version ${licence.version} kind ${licence.kind}",
      ),
    )

    licenceEventRepository.saveAndFlush(
      EntityLicenceEvent(
        licenceId = licence.id,
        eventType = licence.kind.creationEventType(),
        username = creator.username,
        forenames = creator.firstName,
        surname = creator.lastName,
        eventDescription = "Licence created for ${licence.forename} ${licence.surname}",
      ),
    )
  }

  private fun verifyNoInFlightLicence(nomsId: String) {
    val inflightLicences =
      licenceRepository.findAllByNomsIdAndStatusCodeIn(nomsId, listOf(IN_PROGRESS, SUBMITTED, APPROVED, REJECTED))

    if (inflightLicences.isNotEmpty()) {
      val currentInflightLicence = when {
        inflightLicences.size == 1 -> inflightLicences.first()
        else -> inflightLicences.first { it.statusCode != APPROVED }
      }
      throw ResourceAlreadyExistsException(
        message = "A licence already exists for person with prison number: $nomsId (IN_PROGRESS, SUBMITTED, APPROVED or REJECTED)",
        existingResourceId = currentInflightLicence.id,
      )
    }
  }

  private fun createCom(staffId: Long): CommunityOffenderManager {
    log.info("Creating com record for staff with identifier: $staffId")
    val com = deliusApiClient.getStaffByIdentifier(staffId) ?: missing(staffId, "record in delius")
    return staffRepository.saveAndFlush(
      CommunityOffenderManager(
        staffIdentifier = staffId,
        username = com.username?.uppercase() ?: missing(staffId, "username"),
        email = com.email,
        firstName = com.name?.forename,
        lastName = com.name?.surname,
      ),
    )
  }

  private fun getCurrentResponsibleOfficer(
    deliusRecord: OffenderDetail,
    prisonNumber: String,
  ): CommunityOrPrisonOffenderManager =
    deliusApiClient.getOffenderManager(deliusRecord.otherIds.crn)
      ?: error("No active offender manager found for $prisonNumber")

  private fun missing(staffId: Long, field: String): Nothing =
    error("staff with staff identifier: '$staffId', missing $field")
}
