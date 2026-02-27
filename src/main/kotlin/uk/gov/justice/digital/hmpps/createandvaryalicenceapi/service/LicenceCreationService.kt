package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Creator
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.PrisonUser
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CreateLicenceResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.CrdLicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StaffRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StandardConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.InvalidStateException
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.ResourceAlreadyExistsException
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.LicencePolicyService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.timeserved.TimeServedExternalRecordService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.APPROVED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.IN_PROGRESS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.REJECTED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.SUBMITTED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.TIMED_OUT
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.LicenceEvent as EntityLicenceEvent

@Service
class LicenceCreationService(
  private val licenceRepository: LicenceRepository,
  private val crdLicenceRepository: CrdLicenceRepository,
  private val staffRepository: StaffRepository,
  private val standardConditionRepository: StandardConditionRepository,
  private val additionalConditionRepository: AdditionalConditionRepository,
  private val licenceEventRepository: LicenceEventRepository,
  private val licencePolicyService: LicencePolicyService,
  private val auditEventRepository: AuditEventRepository,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val prisonApiClient: PrisonApiClient,
  private val deliusApiClient: DeliusApiClient,
  private val cvlRecordService: CvlRecordService,
  @param:Value("\${feature.toggle.timeServed.enabled:false}")
  private val isTimeServedLogicEnabled: Boolean = false,
  private val telemetryService: TelemetryService,
  private val timeServedExternalRecordService: TimeServedExternalRecordService,
) {
  companion object {
    private val log = LoggerFactory.getLogger(LicenceCreationService::class.java)
  }

  @Transactional
  fun createLicence(prisonNumber: String): CreateLicenceResponse {
    verifyNoInFlightLicence(prisonNumber)

    val username = SecurityContextHolder.getContext().authentication.name

    val nomisRecord = prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(prisonNumber)).first()

    val deliusRecord = deliusApiClient.getProbationCase(prisonNumber)
    val prisonInformation = prisonApiClient.getPrisonInformation(nomisRecord.prisonId!!)
    val offenderManager =
      deliusApiClient.getOffenderManager(deliusRecord!!.crn) ?: error("Offender manager for $prisonNumber not found")

    require(!offenderManager.unallocated) { "offender $prisonNumber is currently unallocated in delius" }

    val cvlRecord = cvlRecordService.getCvlRecord(nomisRecord)

    val responsibleCom = getOrCreateCom(offenderManager.id)

    val createdBy = staffRepository.findByUsernameIgnoreCase(username) as CommunityOffenderManager?
      ?: error("Staff with username $username not found")

    val licence = when (cvlRecord.eligibleKind) {
      LicenceKind.PRRD -> LicenceFactory.createPrrd(
        licenceType = cvlRecord.licenceType,
        nomsId = nomisRecord.prisonerNumber,
        version = licencePolicyService.currentPolicy(cvlRecord.licenceStartDate).version,
        nomisRecord = nomisRecord,
        prisonInformation = prisonInformation,
        team = offenderManager.team,
        deliusRecord = deliusRecord,
        responsibleCom = responsibleCom,
        creator = createdBy,
        licenceStartDate = cvlRecord.licenceStartDate,
      )

      LicenceKind.CRD -> LicenceFactory.createCrd(
        licenceType = cvlRecord.licenceType,
        nomsId = nomisRecord.prisonerNumber,
        version = licencePolicyService.currentPolicy(cvlRecord.licenceStartDate).version,
        nomisRecord = nomisRecord,
        prisonInformation = prisonInformation,
        team = offenderManager.team,
        deliusRecord = deliusRecord,
        responsibleCom = responsibleCom,
        creator = createdBy,
        licenceStartDate = cvlRecord.licenceStartDate,
      )

      LicenceKind.HDC -> LicenceFactory.createHdc(
        licenceType = cvlRecord.licenceType,
        nomsId = nomisRecord.prisonerNumber,
        version = licencePolicyService.currentPolicy(cvlRecord.licenceStartDate).version,
        nomisRecord = nomisRecord,
        prisonInformation = prisonInformation,
        team = offenderManager.team,
        deliusRecord = deliusRecord,
        responsibleCom = responsibleCom,
        creator = createdBy,
        licenceStartDate = cvlRecord.licenceStartDate,
      )

      else -> throw ValidationException("Generic licence creation route not suitable for $prisonNumber - eligibleKind was ${cvlRecord.eligibleKind}.")
    }

    val createdLicence = licenceRepository.saveAndFlush(licence)

    val standardConditions = licencePolicyService.getStandardConditionsForLicence(createdLicence)
    standardConditionRepository.saveAllAndFlush(standardConditions)

    recordLicenceCreation(createdBy, createdLicence)

    return CreateLicenceResponse(createdLicence.id)
  }

  @Transactional
  fun createHardStopLicence(prisonNumber: String): CreateLicenceResponse {
    verifyNoInFlightLicence(prisonNumber)

    val username = SecurityContextHolder.getContext().authentication.name
    val nomisRecord = prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(prisonNumber)).first()
    val deliusRecord = deliusApiClient.getProbationCase(prisonNumber)
      ?: throw InvalidStateException("Could not find a probation case in Delius for nomis id $prisonNumber")

    val prisonInformation = prisonApiClient.getPrisonInformation(nomisRecord.prisonId!!)
    val offenderManager = deliusApiClient.getOffenderManager(deliusRecord.crn)
    val cvlRecord = cvlRecordService.getCvlRecord(nomisRecord)

    val createdBy = staffRepository.findByUsernameIgnoreCase(username) as PrisonUser?
      ?: error("Staff with username $username not found")
    val licenceType = cvlRecord.licenceType
    val version = licencePolicyService.currentPolicy(cvlRecord.licenceStartDate).version
    val licenceStartDate = cvlRecord.licenceStartDate
    val hardStopKind = cvlRecord.hardStopKind
      ?: error("No hardStopKind on CVL record for $prisonNumber - not eligible for hard stop licence")

    val isTimeServedLicenceCreation = isTimeServedLogicEnabled && hardStopKind == LicenceKind.TIME_SERVED

    val licence = if (isTimeServedLicenceCreation) {
      val responsibleCom =
        if (offenderManager == null || offenderManager.unallocated) {
          null
        } else {
          getOrCreateCom(offenderManager.id)
        }

      LicenceFactory.createTimeServe(
        licenceType = licenceType,
        eligibleKind = cvlRecord.eligibleKind,
        nomsId = nomisRecord.prisonerNumber,
        version = version,
        nomisRecord = nomisRecord,
        prisonInformation = prisonInformation,
        team = offenderManager?.team,
        deliusRecord = deliusRecord,
        responsibleCom = responsibleCom,
        creator = createdBy,
        licenceStartDate = licenceStartDate,
      )
    } else {
      require(offenderManager != null) { "Offender manager for ${deliusRecord.crn} not found" }
      require(!offenderManager.unallocated) { "offender $prisonNumber is currently unallocated in delius" }
      val responsibleCom = getOrCreateCom(offenderManager.id)

      val timedOutLicence: CrdLicence? = crdLicenceRepository.findAllByBookingIdInAndStatusCodeOrderByDateCreatedDesc(
        listOf(nomisRecord.bookingId!!.toLong()),
        TIMED_OUT,
      ).firstOrNull()

      LicenceFactory.createHardStop(
        licenceType = licenceType,
        nomsId = nomisRecord.prisonerNumber,
        version = version,
        nomisRecord = nomisRecord,
        prisonInformation = prisonInformation,
        team = offenderManager.team,
        deliusRecord = deliusRecord,
        responsibleCom = responsibleCom,
        creator = createdBy,
        timedOutLicence = timedOutLicence,
        licenceStartDate = licenceStartDate,
        eligibleKind = cvlRecord.eligibleKind,
      )
    }

    val createdLicence = licenceRepository.saveAndFlush(licence)

    val standardConditions = licencePolicyService.getStandardConditionsForLicence(createdLicence)
    standardConditionRepository.saveAllAndFlush(standardConditions)

    val additionalConditions = licencePolicyService.getHardStopAdditionalConditions(createdLicence)
    additionalConditionRepository.saveAllAndFlush(additionalConditions)

    recordLicenceCreation(createdBy, createdLicence)

    if (isTimeServedLicenceCreation) {
      val nomisId = nomisRecord.prisonerNumber
      val bookingId = nomisRecord.bookingId!!.toLong()
      timeServedExternalRecordService.deleteTimeServedExternalRecordIfPresent(nomisId, bookingId)
    }

    return CreateLicenceResponse(createdLicence.id)
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
    telemetryService.recordLicenceCreatedEvent(licence)
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

  private fun getOrCreateCom(staffId: Long): CommunityOffenderManager {
    val staff = staffRepository.findByStaffIdentifier(staffId)
    if (staff != null) {
      return staff
    }
    log.info("Creating com record for staff with identifier: $staffId")
    val com = deliusApiClient.getStaffByIdentifier(staffId) ?: missing(staffId, "record in delius")
    return staffRepository.saveAndFlush(
      CommunityOffenderManager(
        staffIdentifier = staffId,
        staffCode = com.code,
        username = com.username?.uppercase() ?: missing(staffId, "username"),
        email = com.email,
        firstName = com.name.forename,
        lastName = com.name.surname,
      ),
    )
  }

  private fun missing(staffId: Long, field: String): Nothing = error("staff with staff identifier: '$staffId', missing $field")
}
