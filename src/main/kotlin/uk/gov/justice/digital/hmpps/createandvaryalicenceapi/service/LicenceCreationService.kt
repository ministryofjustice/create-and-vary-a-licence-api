package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StaffRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StandardConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.CommunityApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ProbationSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.APPROVED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.IN_PROGRESS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.REJECTED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.SUBMITTED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType.Companion.getLicenceType
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
) {

  companion object {
    private val log = LoggerFactory.getLogger(LicenceCreationService::class.java)
  }

  @Transactional
  fun createLicence(prisonNumber: String): LicenceSummary {
    if (offenderHasLicenceInFlight(prisonNumber)) {
      throw ValidationException("A licence already exists for person with prison number: $prisonNumber (IN_PROGRESS, SUBMITTED, APPROVED or REJECTED)")
    }

    val username = SecurityContextHolder.getContext().authentication.name

    val nomisRecord = prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(prisonNumber)).first()
    val deliusRecord = probationSearchApiClient.searchForPersonOnProbation(prisonNumber)
    val prisonInformation = prisonApiClient.getPrisonInformation(nomisRecord.prisonId)
    val offenderManagers = communityApiClient.getAllOffenderManagers(deliusRecord.otherIds.crn)
    val currentActiveOffenderManager = deliusRecord.offenderManagers.find { it.active } ?: error(
      "No active offender manager found for $prisonNumber",
    )
    val currentResponsibleOfficerDetails = offenderManagers.find {
      it.staffCode == currentActiveOffenderManager.staffDetail.code
    } ?: error("No responsible officer details found for $prisonNumber")

    val responsibleCom = staffRepository.findByStaffIdentifier(currentResponsibleOfficerDetails.staffId)
      ?: createCom(currentResponsibleOfficerDetails.staffId)

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
      creatingCom = createdBy,
    )

    val licenceEntity = licenceRepository.saveAndFlush(licence)
    val createLicenceResponse = transformToLicenceSummary(licenceEntity)

    val standardConditions = licencePolicyService.getStandardConditionsForLicence(licenceEntity)
    standardConditionRepository.saveAllAndFlush(standardConditions)

    auditEventRepository.saveAndFlush(
      AuditEvent(
        licenceId = createLicenceResponse.licenceId,
        username = username,
        fullName = "${createdBy.firstName} ${createdBy.lastName}",
        summary = "Licence created for ${licence.forename} ${licence.surname}",
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

    return createLicenceResponse
  }

  private fun offenderHasLicenceInFlight(nomsId: String): Boolean {
    val inFlight =
      licenceRepository.findAllByNomsIdAndStatusCodeIn(nomsId, listOf(IN_PROGRESS, SUBMITTED, APPROVED, REJECTED))
    return inFlight.isNotEmpty()
  }

  private fun createCom(staffId: Long): CommunityOffenderManager {
    log.info("Creating com record for staff with identifier: $staffId")
    val com = communityApiClient.getStaffByIdentifier(staffId) ?: missing(staffId, "record in delius")
    return staffRepository.saveAndFlush(
      CommunityOffenderManager(
        staffIdentifier = staffId,
        username = com.username?.uppercase() ?: missing(staffId, "username"),
        email = com.email,
        firstName = com.staff?.forenames,
        lastName = com.staff?.surname,
      ),
    )
  }

  private fun missing(staffId: Long, field: String): Nothing =
    error("staff with staff identifier: '$staffId', missing $field")
}
