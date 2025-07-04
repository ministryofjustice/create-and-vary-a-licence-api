package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.data.mapping.PropertyReferenceException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HardStopLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcVariationLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence.Companion.SYSTEM_USER
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.LicenceEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.PrisonUser
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.PrrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Staff
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Variation
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.VariationLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.StatusUpdateRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.DeactivateLicenceAndVariationsRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.NotifyRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.ReferVariationRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdatePrisonInformationRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateReasonForVariationRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateSpoDiscussionRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateVloDiscussionRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionUploadDetailRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.BespokeConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.CrdLicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceQueryObject
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StaffRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StandardConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.getSort
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.toSpecification
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions.isLicenceReadyToSubmit
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.DomainEventsService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.LicencePolicyService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AuditEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.HDC_VARIATION
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.VARIATION
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.ACTIVE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.APPROVED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.INACTIVE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.IN_PROGRESS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.REJECTED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.SUBMITTED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.TIMED_OUT
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.VARIATION_APPROVED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.VARIATION_IN_PROGRESS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.VARIATION_REJECTED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.VARIATION_SUBMITTED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence as EntityLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.LicenceEvent as EntityLicenceEvent

@Service
class LicenceService(
  private val licenceRepository: LicenceRepository,
  private val crdLicenceRepository: CrdLicenceRepository,
  private val staffRepository: StaffRepository,
  private val standardConditionRepository: StandardConditionRepository,
  private val additionalConditionRepository: AdditionalConditionRepository,
  private val bespokeConditionRepository: BespokeConditionRepository,
  private val licenceEventRepository: LicenceEventRepository,
  private val licencePolicyService: LicencePolicyService,
  private val additionalConditionUploadDetailRepository: AdditionalConditionUploadDetailRepository,
  private val auditEventRepository: AuditEventRepository,
  private val notifyService: NotifyService,
  private val omuService: OmuService,
  private val releaseDateService: ReleaseDateService,
  private val domainEventsService: DomainEventsService,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val eligibilityService: EligibilityService,
) {

  @Transactional
  fun getLicenceById(licenceId: Long): Licence {
    val entityLicence = getLicence(licenceId)

    val isEligibleForEarlyRelease = releaseDateService.isEligibleForEarlyRelease(entityLicence)
    val earliestReleaseDate = releaseDateService.getEarliestReleaseDate(entityLicence)

    val conditionsSubmissionStatus =
      isLicenceReadyToSubmit(
        entityLicence.additionalConditions,
        licencePolicyService.getAllAdditionalConditions(),
      )

    return transform(entityLicence, earliestReleaseDate, isEligibleForEarlyRelease, conditionsSubmissionStatus)
  }

  fun transform(
    licence: EntityLicence,
    earliestReleaseDate: LocalDate?,
    isEligibleForEarlyRelease: Boolean,
    conditionSubmissionStatus: Map<String, Boolean>,
  ): Licence = when (licence) {
    is PrrdLicence -> toPrrd(
      licence = licence,
      earliestReleaseDate = earliestReleaseDate,
      isEligibleForEarlyRelease = isEligibleForEarlyRelease,
      isInHardStopPeriod = releaseDateService.isInHardStopPeriod(licence),
      hardStopDate = releaseDateService.getHardStopDate(licence),
      hardStopWarningDate = releaseDateService.getHardStopWarningDate(licence),
      isDueForEarlyRelease = releaseDateService.isDueForEarlyRelease(licence),
      isDueToBeReleasedInTheNextTwoWorkingDays = releaseDateService.isDueToBeReleasedInTheNextTwoWorkingDays(licence),
      conditionSubmissionStatus = conditionSubmissionStatus,
    )

    is CrdLicence -> toCrd(
      licence = licence,
      earliestReleaseDate = earliestReleaseDate,
      isEligibleForEarlyRelease = isEligibleForEarlyRelease,
      isInHardStopPeriod = releaseDateService.isInHardStopPeriod(licence),
      hardStopDate = releaseDateService.getHardStopDate(licence),
      hardStopWarningDate = releaseDateService.getHardStopWarningDate(licence),
      isDueForEarlyRelease = releaseDateService.isDueForEarlyRelease(licence),
      isDueToBeReleasedInTheNextTwoWorkingDays = releaseDateService.isDueToBeReleasedInTheNextTwoWorkingDays(licence),
      conditionSubmissionStatus = conditionSubmissionStatus,
    )

    is VariationLicence -> toVariation(
      licence = licence,
      earliestReleaseDate = earliestReleaseDate,
      isEligibleForEarlyRelease = isEligibleForEarlyRelease,
      conditionSubmissionStatus = conditionSubmissionStatus,
    )

    is HardStopLicence -> toHardstop(
      licence = licence,
      earliestReleaseDate = earliestReleaseDate,
      isEligibleForEarlyRelease = isEligibleForEarlyRelease,
      isInHardStopPeriod = releaseDateService.isInHardStopPeriod(licence),
      hardStopDate = releaseDateService.getHardStopDate(licence),
      hardStopWarningDate = releaseDateService.getHardStopWarningDate(licence),
      isDueForEarlyRelease = releaseDateService.isDueForEarlyRelease(licence),
      isDueToBeReleasedInTheNextTwoWorkingDays = releaseDateService.isDueToBeReleasedInTheNextTwoWorkingDays(licence),
      conditionSubmissionStatus = conditionSubmissionStatus,
    )

    is HdcLicence -> toHdc(
      licence = licence,
      earliestReleaseDate = earliestReleaseDate,
      isEligibleForEarlyRelease = isEligibleForEarlyRelease,
      isInHardStopPeriod = releaseDateService.isInHardStopPeriod(licence),
      hardStopDate = releaseDateService.getHardStopDate(licence),
      hardStopWarningDate = releaseDateService.getHardStopWarningDate(licence),
      isDueForEarlyRelease = releaseDateService.isDueForEarlyRelease(licence),
      isDueToBeReleasedInTheNextTwoWorkingDays = releaseDateService.isDueToBeReleasedInTheNextTwoWorkingDays(licence),
      conditionSubmissionStatus = conditionSubmissionStatus,
    )

    is HdcVariationLicence -> toHdcVariation(
      licence = licence,
      earliestReleaseDate = earliestReleaseDate,
      isEligibleForEarlyRelease = isEligibleForEarlyRelease,
      conditionSubmissionStatus = conditionSubmissionStatus,
    )

    else -> error("could not convert licence of type: ${licence.kind} for licence: ${licence.id}")
  }

  @Transactional
  fun updateLicenceStatus(licenceId: Long, request: StatusUpdateRequest) {
    val licenceEntity = getLicence(licenceId)
    updateLicenceStatus(licenceEntity, request)
  }

  private fun updateLicenceStatus(licenceEntity: EntityLicence, request: StatusUpdateRequest) {
    var approvedByUser = licenceEntity.approvedByUsername
    var approvedByName = licenceEntity.approvedByName
    var approvedDate = licenceEntity.approvedDate
    var submittedDate = licenceEntity.submittedDate
    val supersededDate: LocalDateTime?
    var licenceActivatedDate = licenceEntity.licenceActivatedDate
    val staffMember = staffRepository.findByUsernameIgnoreCase(request.username)

    when (request.status) {
      APPROVED -> {
        when (licenceEntity) {
          is VariationLicence -> error("Cannot approve a Variation licence: ${licenceEntity.id}")
          is HdcVariationLicence -> error("Cannot approve an HDC Variation licence: ${licenceEntity.id}")
          is PrrdLicence -> deactivatePreviousLicenceVersion(licenceEntity, request.fullName, staffMember)
          is CrdLicence -> deactivatePreviousLicenceVersion(licenceEntity, request.fullName, staffMember)
          is HdcLicence -> deactivatePreviousLicenceVersion(licenceEntity, request.fullName, staffMember)
        }
        approvedByUser = request.username
        approvedByName = request.fullName
        approvedDate = LocalDateTime.now()
        supersededDate = null
      }

      IN_PROGRESS -> {
        approvedByUser = null
        approvedByName = null
        approvedDate = null
        supersededDate = null
      }

      INACTIVE -> {
        supersededDate = LocalDateTime.now()
        inactivateInProgressLicenceVersions(
          listOf(licenceEntity),
          "Deactivating licence as the parent licence version was deactivated",
        )
      }

      ACTIVE -> {
        supersededDate = null
        licenceActivatedDate = LocalDateTime.now()
        inactivateInProgressLicenceVersions(
          listOf(licenceEntity),
          "Deactivating licence as the parent licence version was activated",
        )
        inactivateTimedOutLicenceVersions(
          listOf(licenceEntity),
          "Deactivating licence as the parent licence version was activated",
        )
      }

      SUBMITTED -> {
        supersededDate = null
        submittedDate = LocalDateTime.now()
      }

      else -> {
        supersededDate = null
      }
    }

    val isReApproval = licenceEntity.statusCode === APPROVED && request.status === IN_PROGRESS

    licenceEntity.updateStatus(
      statusCode = request.status,
      staffMember = staffMember,
      approvedByUsername = approvedByUser,
      approvedByName = approvedByName,
      approvedDate = approvedDate,
      supersededDate = supersededDate,
      submittedDate = submittedDate,
      licenceActivatedDate = licenceActivatedDate,
    )
    licenceRepository.saveAndFlush(licenceEntity)

    // if previous status was APPROVED and the new status is IN_PROGRESS then email OMU regarding re-approval
    if (isReApproval) {
      notifyReApprovalNeeded(licenceEntity)
    }

    if (request.status === APPROVED && licenceEntity is HardStopLicence) {
      notifyComAboutHardstopLicenceApproval(licenceEntity)
    }

    recordLicenceEventForStatus(licenceEntity, request)
    auditStatusChange(licenceEntity, staffMember)
    domainEventsService.recordDomainEvent(licenceEntity, request.status)
  }

  private fun auditStatusChange(licenceEntity: EntityLicence, staffMember: Staff?) {
    val licenceFullName = "${licenceEntity.forename} ${licenceEntity.surname}"
    val detailText =
      "ID ${licenceEntity.id} type ${licenceEntity.typeCode} status ${licenceEntity.statusCode.name} version ${licenceEntity.version}"

    val summaryText = when (licenceEntity.statusCode) {
      APPROVED -> "Licence approved for $licenceFullName"
      REJECTED -> "Licence rejected for $licenceFullName"
      IN_PROGRESS -> "Licence edited for $licenceFullName"
      ACTIVE -> "Licence set to ACTIVE for $licenceFullName"
      INACTIVE -> "Licence set to INACTIVE for $licenceFullName"
      VARIATION_IN_PROGRESS -> "Licence variation changed to in progress for $licenceFullName"
      VARIATION_APPROVED -> "Licence variation approved for $licenceFullName"
      VARIATION_REJECTED -> "Licence variation referred for $licenceFullName"
      else -> "Check - licence status not accounted for in auditing"
    }

    val username = staffMember?.username ?: SYSTEM_USER

    auditEventRepository.saveAndFlush(
      AuditEvent(
        licenceId = licenceEntity.id,
        username = username,
        fullName = staffMember?.fullName ?: SYSTEM_USER,
        eventType = getAuditEventType(username),
        summary = summaryText,
        detail = detailText,
      ),
    )
  }

  private fun getAuditEventType(username: String): AuditEventType = if (username == "SYSTEM_USER") {
    AuditEventType.SYSTEM_EVENT
  } else {
    AuditEventType.USER_EVENT
  }

  private fun recordLicenceEventForStatus(licenceEntity: EntityLicence, request: StatusUpdateRequest) {
    // Only interested when moving to the APPROVED, ACTIVE or INACTIVE states
    val eventType = when (licenceEntity.statusCode) {
      APPROVED -> LicenceEventType.APPROVED
      ACTIVE -> LicenceEventType.ACTIVATED
      INACTIVE -> LicenceEventType.SUPERSEDED
      else -> return
    }

    val (firstName, lastName) = splitName(request.fullName)
    licenceEventRepository.saveAndFlush(
      EntityLicenceEvent(
        licenceId = licenceEntity.id,
        eventType = eventType,
        username = request.username,
        forenames = firstName,
        surname = lastName,
        eventDescription = "Licence updated to ${licenceEntity.statusCode} for ${licenceEntity.forename} ${licenceEntity.surname}",
      ),
    )
  }

  private fun notifyReApprovalNeeded(licenceEntity: EntityLicence) {
    val omuEmail = licenceEntity.prisonCode?.let { omuService.getOmuContactEmail(it)?.email }
    notifyService.sendVariationForReApprovalEmail(
      omuEmail,
      licenceEntity.forename ?: "unknown",
      licenceEntity.surname ?: "unknown",
      licenceEntity.nomsId,
      licenceEntity.conditionalReleaseDate,
    )
  }

  private fun notifyComAboutHardstopLicenceApproval(licenceEntity: EntityLicence) {
    val com = licenceEntity.responsibleCom
    notifyService.sendHardStopLicenceApprovedEmail(
      com.email,
      licenceEntity.forename!!,
      licenceEntity.surname!!,
      licenceEntity.crn,
      licenceEntity.licenceStartDate,
      licenceEntity.id.toString(),
    )
  }

  private fun deactivatePreviousLicenceVersion(licence: EntityLicence, fullName: String?, staffMember: Staff?) {
    val previousVersionId = getVersionOf(licence) ?: return

    val previousLicenceVersion =
      licenceRepository.findById(previousVersionId)
        .orElseThrow { EntityNotFoundException("$previousVersionId") }

    val updatedLicence = when (previousLicenceVersion) {
      is CrdLicence -> previousLicenceVersion.copy(
        dateLastUpdated = LocalDateTime.now(),
        updatedByUsername = staffMember?.username ?: SYSTEM_USER,
        statusCode = INACTIVE,
        updatedBy = staffMember ?: previousLicenceVersion.updatedBy,
      )
      is PrrdLicence -> previousLicenceVersion.copy(
        dateLastUpdated = LocalDateTime.now(),
        updatedByUsername = staffMember?.username ?: SYSTEM_USER,
        statusCode = INACTIVE,
        updatedBy = staffMember ?: previousLicenceVersion.updatedBy,
      )
      is HdcLicence -> previousLicenceVersion.copy(
        dateLastUpdated = LocalDateTime.now(),
        updatedByUsername = staffMember?.username ?: SYSTEM_USER,
        statusCode = INACTIVE,
        updatedBy = staffMember ?: previousLicenceVersion.updatedBy,
      )
      else -> error("Trying to inactivate non-crd licence: $previousVersionId")
    }

    licenceRepository.saveAndFlush(updatedLicence)

    val (firstName, lastName) = splitName(fullName)
    licenceEventRepository.saveAndFlush(
      EntityLicenceEvent(
        licenceId = previousVersionId,
        eventType = LicenceEventType.SUPERSEDED,
        username = staffMember?.username ?: SYSTEM_USER,
        forenames = firstName,
        surname = lastName,
        eventDescription = "Licence deactivated as a newer version was approved for ${licence.forename} ${licence.surname}",
      ),
    )

    auditStatusChange(updatedLicence, staffMember)
  }

  @Transactional
  fun submitLicence(licenceId: Long, notifyRequest: List<NotifyRequest>?) {
    val licenceEntity = getLicence(licenceId)

    val username = SecurityContextHolder.getContext().authentication.name
    val submitter = staffRepository.findByUsernameIgnoreCase(username)
      ?: throw ValidationException("Staff with username $username not found")

    val updatedLicence = when (licenceEntity) {
      is PrrdLicence -> {
        assertCaseIsEligible(licenceEntity.id, licenceEntity.nomsId)
        licenceEntity.submit(submitter as CommunityOffenderManager)
      }
      is CrdLicence -> {
        assertCaseIsEligible(licenceEntity.id, licenceEntity.nomsId)
        licenceEntity.submit(submitter as CommunityOffenderManager)
      }
      is VariationLicence -> licenceEntity.submit(submitter as CommunityOffenderManager)
      is HardStopLicence -> {
        assertCaseIsEligible(licenceEntity.id, licenceEntity.nomsId)
        licenceEntity.submit(submitter as PrisonUser)
      }
      is HdcLicence -> {
        assertCaseIsEligible(licenceEntity.id, licenceEntity.nomsId)
        licenceEntity.submit(submitter as CommunityOffenderManager)
      }
      is HdcVariationLicence -> licenceEntity.submit(submitter as CommunityOffenderManager)
      else -> error("Unexpected licence type: $licenceEntity")
    }

    licenceRepository.saveAndFlush(updatedLicence)

    licenceEventRepository.saveAndFlush(
      EntityLicenceEvent(
        licenceId = licenceId,
        eventType = updatedLicence.kind.submittedEventType(),
        username = username,
        forenames = submitter.firstName,
        surname = submitter.lastName,
        eventDescription = "Licence submitted for approval for ${updatedLicence.forename} ${updatedLicence.surname}",
      ),
    )

    auditEventRepository.saveAndFlush(
      AuditEvent(
        licenceId = licenceId,
        username = username,
        fullName = "${submitter.firstName} ${submitter.lastName}",
        summary = "Licence submitted for approval for ${updatedLicence.forename} ${updatedLicence.surname}",
        detail = "ID $licenceId type ${updatedLicence.typeCode} status ${licenceEntity.statusCode.name} version ${updatedLicence.version}",
      ),
    )

    // Notify the head of PDU of this submitted licence variation
    if (updatedLicence is Variation) {
      notifyRequest?.forEach {
        notifyService.sendVariationForApprovalEmail(
          it,
          licenceId.toString(),
          updatedLicence.forename!!,
          updatedLicence.surname!!,
          updatedLicence.crn!!,
          username!!,
        )
      }
    }
  }

  fun findLicencesMatchingCriteria(licenceQueryObject: LicenceQueryObject): List<LicenceSummary> {
    try {
      val matchingLicences =
        licenceRepository.findAll(licenceQueryObject.toSpecification(), licenceQueryObject.getSort())
      return matchingLicences.map { it.toSummary() }
    } catch (e: PropertyReferenceException) {
      throw ValidationException(e.message, e)
    }
  }

  fun findSubmittedVariationsByRegion(probationAreaCode: String): List<LicenceSummary> {
    val matchingLicences =
      licenceRepository.findByStatusCodeAndProbationAreaCode(VARIATION_SUBMITTED, probationAreaCode)
    return matchingLicences.map { it.toSummary() }
  }

  fun findLicencesForCrnsAndStatuses(crns: List<String>, statusCodes: List<LicenceStatus>): List<LicenceSummary> {
    val matchingLicences = licenceRepository.findAllByCrnInAndStatusCodeIn(crns, statusCodes)
    return matchingLicences.map { it.toSummary() }
  }

  @Transactional
  fun activateLicences(licences: List<EntityLicence>, reason: String? = null) {
    licences.forEach { it.activate() }
    if (licences.isNotEmpty()) {
      licenceRepository.saveAllAndFlush(licences)

      licences.map { licence ->
        auditEventRepository.saveAndFlush(
          AuditEvent(
            licenceId = licence.id,
            username = "SYSTEM",
            fullName = "SYSTEM",
            eventType = AuditEventType.SYSTEM_EVENT,
            summary = "${reason ?: "Licence automatically activated"} for ${licence.forename} ${licence.surname}",
            detail = "ID ${licence.id} type ${licence.typeCode} status ${licence.statusCode.name} version ${licence.version}",
          ),
        )

        licenceEventRepository.saveAndFlush(
          EntityLicenceEvent(
            licenceId = licence.id,
            eventType = LicenceEventType.ACTIVATED,
            username = "SYSTEM",
            forenames = "SYSTEM",
            surname = "SYSTEM",
            eventDescription = "${reason ?: "Licence automatically activated"} for ${licence.forename} ${licence.surname}",
          ),
        )

        domainEventsService.recordDomainEvent(licence, ACTIVE)
      }
      inactivateInProgressLicenceVersions(
        licences,
        "Licence automatically deactivated as the approved licence version was activated",
      )
      inactivateTimedOutLicenceVersions(
        licences,
        "Licence automatically deactivated as the approved licence version was activated",
      )
    }
  }

  @Transactional
  fun inactivateLicences(
    licences: List<EntityLicence>,
    reason: String? = null,
    deactivateInProgressVersions: Boolean? = true,
  ) {
    licences.forEach { it.deactivate() }
    if (licences.isNotEmpty()) {
      licenceRepository.saveAllAndFlush(licences)

      licences.map { licence ->
        auditEventRepository.saveAndFlush(
          AuditEvent(
            licenceId = licence.id,
            username = "SYSTEM",
            fullName = "SYSTEM",
            eventType = AuditEventType.SYSTEM_EVENT,
            summary = "${reason ?: "Licence automatically inactivated"} for ${licence.forename} ${licence.surname}",
            detail = "ID ${licence.id} kind ${licence.kind} type ${licence.typeCode} status ${licence.statusCode.name} version ${licence.version}",
          ),
        )

        licenceEventRepository.saveAndFlush(
          EntityLicenceEvent(
            licenceId = licence.id,
            eventType = LicenceEventType.SUPERSEDED,
            username = "SYSTEM",
            forenames = "SYSTEM",
            surname = "SYSTEM",
            eventDescription = "${reason ?: "Licence automatically inactivated"} for ${licence.forename} ${licence.surname}",
          ),
        )

        domainEventsService.recordDomainEvent(licence, INACTIVE)
      }
      if (deactivateInProgressVersions == true) {
        inactivateInProgressLicenceVersions(licences)
      }
    }
  }

  @Transactional
  fun inActivateLicencesByIds(licenceIds: List<Long>) {
    val matchingLicences = licenceRepository.findAllById(licenceIds)
    inactivateLicences(
      matchingLicences,
      deactivateInProgressVersions = true,
    )
  }

  @Transactional
  fun createVariation(licenceId: Long): LicenceSummary {
    val licence = getLicence(licenceId)
    val creator = getCommunityOffenderManagerForCurrentUser()

    val licenceVariation = when (licence) {
      is HdcLicence -> {
        val licenceCopy = LicenceFactory.createHdcVariation(licence, creator)
        populateCopyAndAudit(HDC_VARIATION, licence, licenceCopy, creator)
      }
      else -> {
        val licenceCopy = LicenceFactory.createVariation(licence, creator)
        populateCopyAndAudit(VARIATION, licence, licenceCopy, creator)
      }
    }
    return licenceVariation.toSummary()
  }

  @Transactional
  fun editLicence(licenceId: Long): LicenceSummary {
    val licence = getLicence(licenceId)
    if (isNotValidLicenceForEdit(licence)) error("Trying to edit licence for non-crd,non-hdc or non-prrd licence: $licenceId")

    if (licence.statusCode != APPROVED) {
      throw ValidationException("Can only edit APPROVED licences")
    }

    val inProgressVersions =
      licenceRepository.findAllByVersionOfIdInAndStatusCodeIn(listOf(licenceId), listOf(IN_PROGRESS, SUBMITTED))
    if (inProgressVersions.isNotEmpty()) {
      return inProgressVersions[0].toSummary()
    }

    assertCaseIsEligible(licence.id, licence.nomsId)

    val creator = getCommunityOffenderManagerForCurrentUser()

    val copyToEdit = when (licence) {
      is HdcLicence -> LicenceFactory.createHdcCopyToEdit(licence, creator)
      is CrdLicence -> LicenceFactory.createCrdCopyToEdit(licence, creator)
      is PrrdLicence -> LicenceFactory.createPrrdCopyToEdit(licence, creator)
      else -> throw IllegalArgumentException("Unsupported licence type: ${licence.javaClass.simpleName}")
    }

    val licenceCopy = populateCopyAndAudit(licence.kind, licence, copyToEdit, creator)
    notifyReApprovalNeeded(licence)
    return licenceCopy.toSummary()
  }

  private fun isNotValidLicenceForEdit(licence: uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence?): Boolean = licence != null &&
    licence !is CrdLicence &&
    licence !is HdcLicence &&
    licence !is PrrdLicence

  @Transactional
  fun updateSpoDiscussion(licenceId: Long, spoDiscussionRequest: UpdateSpoDiscussionRequest) {
    val licenceEntity = getLicence(licenceId)
    if (licenceEntity !is Variation) error("Trying to update spo discussion for non-variation: $licenceId")

    val username = SecurityContextHolder.getContext().authentication.name

    val staffMember = this.staffRepository.findByUsernameIgnoreCase(username)

    licenceEntity.updateSpoDiscussion(
      spoDiscussion = spoDiscussionRequest.spoDiscussion,
      staffMember = staffMember,
    )

    licenceRepository.saveAndFlush(licenceEntity)
  }

  @Transactional
  fun updateVloDiscussion(licenceId: Long, vloDiscussionRequest: UpdateVloDiscussionRequest) {
    val licenceEntity = getLicence(licenceId)
    if (licenceEntity !is Variation) error("Trying to update vlo discussion for non-variation: $licenceId")

    val username = SecurityContextHolder.getContext().authentication.name

    val staffMember = this.staffRepository.findByUsernameIgnoreCase(username)

    licenceEntity.updateVloDiscussion(
      vloDiscussion = vloDiscussionRequest.vloDiscussion,
      staffMember = staffMember,
    )

    licenceRepository.saveAndFlush(licenceEntity)
  }

  @Transactional
  fun updateReasonForVariation(licenceId: Long, reasonForVariationRequest: UpdateReasonForVariationRequest) {
    val licenceEntity = getLicence(licenceId)
    if (licenceEntity !is Variation) error("Trying to update variation reason for non-variation: $licenceId")

    val username = SecurityContextHolder.getContext().authentication.name
    val staffMember = this.staffRepository.findByUsernameIgnoreCase(username)

    licenceEntity.recordUpdate(
      staffMember = staffMember,
    )
    licenceRepository.saveAndFlush(licenceEntity)

    licenceEventRepository.saveAndFlush(
      EntityLicenceEvent(
        licenceId = licenceId,
        eventType = LicenceEventType.VARIATION_SUBMITTED_REASON,
        username = staffMember?.username ?: SYSTEM_USER,
        forenames = staffMember?.firstName,
        surname = staffMember?.lastName,
        eventDescription = reasonForVariationRequest.reasonForVariation,
      ),
    )
  }

  @Transactional
  fun referLicenceVariation(licenceId: Long, referVariationRequest: ReferVariationRequest) {
    val licenceEntity = getLicence(licenceId)
    if (licenceEntity !is Variation) error("Trying to reject non-variation: $licenceId")
    val username = SecurityContextHolder.getContext().authentication.name
    val staffMember = this.staffRepository.findByUsernameIgnoreCase(username)

    licenceEntity.referVariation(staffMember)

    licenceRepository.saveAndFlush(licenceEntity)

    licenceEventRepository.saveAndFlush(
      EntityLicenceEvent(
        licenceId = licenceId,
        eventType = LicenceEventType.VARIATION_REFERRED,
        username = staffMember?.username ?: SYSTEM_USER,
        forenames = staffMember?.firstName,
        surname = staffMember?.lastName,
        eventDescription = referVariationRequest.reasonForReferral,
      ),
    )

    auditEventRepository.saveAndFlush(
      AuditEvent(
        licenceId = licenceId,
        username = staffMember?.username ?: SYSTEM_USER,
        fullName = "${staffMember?.firstName} ${staffMember?.lastName}",
        summary = "Licence variation rejected for ${licenceEntity.forename} ${licenceEntity.surname}",
        detail = "ID $licenceId type ${licenceEntity.typeCode} status ${licenceEntity.statusCode.name} version ${licenceEntity.version}",
      ),
    )

    notifyService.sendVariationReferredEmail(
      licenceEntity.createdBy?.email ?: "",
      "${licenceEntity.createdBy?.firstName} ${licenceEntity.createdBy?.lastName}",
      licenceEntity.responsibleCom.email ?: "",
      "${licenceEntity.responsibleCom.firstName} ${licenceEntity.responsibleCom.lastName}",
      "${licenceEntity.forename} ${licenceEntity.surname}",
      licenceId.toString(),
    )
  }

  @Transactional
  fun approveLicenceVariation(licenceId: Long) {
    val licenceEntity = getLicence(licenceId)
    if (licenceEntity !is Variation) error("Trying to approve non-variation: $licenceId")
    val username = SecurityContextHolder.getContext().authentication.name
    val staffMember = this.staffRepository.findByUsernameIgnoreCase(username)

    licenceEntity.approveVariation(username, staffMember)

    licenceRepository.saveAndFlush(licenceEntity)

    licenceEventRepository.saveAndFlush(
      EntityLicenceEvent(
        licenceId = licenceId,
        eventType = LicenceEventType.VARIATION_APPROVED,
        username = staffMember?.username ?: SYSTEM_USER,
        forenames = staffMember?.firstName,
        surname = staffMember?.lastName,
        eventDescription = "Licence variation approved for ${licenceEntity.forename} ${licenceEntity.surname}",
      ),
    )

    auditEventRepository.saveAndFlush(
      AuditEvent(
        licenceId = licenceId,
        username = staffMember?.username ?: SYSTEM_USER,
        fullName = "${staffMember?.firstName} ${staffMember?.lastName}",
        summary = "Licence variation approved for ${licenceEntity.forename} ${licenceEntity.surname}",
        detail = "ID $licenceId type ${licenceEntity.typeCode} status ${licenceEntity.statusCode.name} version ${licenceEntity.version}",
      ),
    )

    notifyService.sendVariationApprovedEmail(
      licenceEntity.createdBy?.email ?: "",
      "${licenceEntity.createdBy?.firstName} ${licenceEntity.createdBy?.lastName}",
      licenceEntity.responsibleCom.email ?: "",
      "${licenceEntity.responsibleCom.firstName} ${licenceEntity.responsibleCom.lastName}",
      "${licenceEntity.forename} ${licenceEntity.surname}",
      licenceId.toString(),
    )
  }

  @Transactional
  fun activateVariation(licenceId: Long) {
    val licence = getLicence(licenceId)

    if (licence !is Variation || licence.statusCode != VARIATION_APPROVED) {
      return
    }

    val username = SecurityContextHolder.getContext().authentication.name
    val user =
      this.staffRepository.findByUsernameIgnoreCase(username) ?: error("need user in scope to activate variation")

    updateLicenceStatus(
      licence,
      StatusUpdateRequest(status = ACTIVE, username = user.username, fullName = user.fullName),
    )

    val previousLicence = licenceRepository
      .findById(licence.variationOfId!!)
      .orElseThrow { EntityNotFoundException("$licenceId") }

    if (previousLicence is HardStopLicence) {
      previousLicence.markAsReviewed(user)
      licenceEventRepository.saveAndFlush(
        EntityLicenceEvent(
          licenceId = licenceId,
          eventType = LicenceEventType.HARD_STOP_REVIEWED_WITH_VARIATION,
          username = user.username,
          forenames = user.firstName,
          surname = user.lastName,
          eventDescription = "Licence reviewed with variation for ${licence.forename} ${licence.surname}",
        ),
      )
    }

    updateLicenceStatus(
      previousLicence,
      StatusUpdateRequest(status = INACTIVE, username = user.username, fullName = user.fullName),
    )
  }

  @Transactional
  fun discardLicence(licenceId: Long) {
    val licenceEntity = getLicence(licenceId)

    val username = SecurityContextHolder.getContext().authentication.name
    val discardedBy = this.staffRepository.findByUsernameIgnoreCase(username)

    auditEventRepository.saveAndFlush(
      AuditEvent(
        licenceId = licenceId,
        username = discardedBy?.username ?: SYSTEM_USER,
        fullName = "${discardedBy?.firstName} ${discardedBy?.lastName}",
        summary = "Licence variation discarded for ${licenceEntity.forename} ${licenceEntity.surname}",
        detail = "ID $licenceId type ${licenceEntity.typeCode} status ${licenceEntity.statusCode.name} version ${licenceEntity.version}",
      ),
    )

    licenceRepository.delete(licenceEntity)
  }

  @Transactional
  fun updatePrisonInformation(licenceId: Long, prisonInformationRequest: UpdatePrisonInformationRequest) {
    val licenceEntity = getLicence(licenceId)

    val username = SecurityContextHolder.getContext().authentication.name

    val staffMember = this.staffRepository.findByUsernameIgnoreCase(username)

    licenceEntity.updatePrisonInfo(
      prisonCode = prisonInformationRequest.prisonCode,
      prisonDescription = prisonInformationRequest.prisonDescription,
      prisonTelephone = prisonInformationRequest.prisonTelephone,
      staffMember = staffMember,
    )

    licenceRepository.saveAndFlush(licenceEntity)

    auditEventRepository.saveAndFlush(
      AuditEvent(
        licenceId = licenceEntity.id,
        username = "SYSTEM",
        fullName = "SYSTEM",
        eventType = AuditEventType.SYSTEM_EVENT,
        summary = "Prison information updated for ${licenceEntity.forename} ${licenceEntity.surname}",
        detail = "ID ${licenceEntity.id} type ${licenceEntity.typeCode} status ${licenceEntity.statusCode} version ${licenceEntity.version}",
      ),
    )
  }

  private fun populateCopyAndAudit(
    kind: LicenceKind,
    licence: EntityLicence,
    licenceCopy: EntityLicence,
    creator: CommunityOffenderManager,
  ): EntityLicence {
    val newStatus = kind.initialStatus()

    licenceCopy.version = licencePolicyService.currentPolicy().version
    val newLicence = licenceRepository.save(licenceCopy)

    val standardConditions = licence.standardConditions.map {
      it.copy(id = -1, licence = newLicence)
    }

    val bespokeConditions = licence.bespokeConditions.map {
      it.copy(id = -1, licence = newLicence)
    }

    standardConditionRepository.saveAll(standardConditions)

    val isNowInPssPeriod =
      licence is Variation && licence.typeCode == LicenceType.AP_PSS && licence.isInPssPeriod()

    if (!isNowInPssPeriod) {
      bespokeConditionRepository.saveAll(bespokeConditions)
    }

    val licenceConditions: List<AdditionalCondition> =
      if (isNowInPssPeriod) {
        licence.additionalConditions.filter { it.isNotAp() }
      } else {
        licence.additionalConditions
      }

    val additionalConditions = licenceConditions.map {
      it.copy(
        id = -1,
        licence = newLicence,
        additionalConditionData = it.additionalConditionData.map { it.copy(id = -1) },
        additionalConditionUploadSummary = it.additionalConditionUploadSummary.map { it.copy(id = -1) },
      )
    }

    val copyOfAdditionalConditions = additionalConditionRepository.saveAll(additionalConditions)

    val newAdditionalConditions = copyOfAdditionalConditions.map { condition ->
      val updatedAdditionalConditionData = condition.additionalConditionData.map {
        it.copy(additionalCondition = condition)
      }

      val updatedAdditionalConditionUploadSummary = condition.additionalConditionUploadSummary.map {
        var uploadDetail = additionalConditionUploadDetailRepository.getReferenceById(it.uploadDetailId)
        uploadDetail =
          uploadDetail.copy(id = -1, licenceId = newLicence.id, additionalConditionId = condition.id)
        uploadDetail = additionalConditionUploadDetailRepository.save(uploadDetail)
        it.copy(additionalCondition = condition, uploadDetailId = uploadDetail.id)
      }

      condition.copy(
        additionalConditionData = updatedAdditionalConditionData,
        additionalConditionUploadSummary = updatedAdditionalConditionUploadSummary,
      )
    }

    additionalConditionRepository.saveAll(newAdditionalConditions)

    val licenceEventMessage = when (licenceCopy.statusCode) {
      VARIATION_IN_PROGRESS -> "A variation was created for ${newLicence.forename} ${newLicence.surname} from ID ${licence.id}"
      IN_PROGRESS -> "A new licence version was created for ${newLicence.forename} ${newLicence.surname} from ID ${licence.id}"
      else -> error("Invalid new licence status of ${licenceCopy.statusCode} when creating a licence copy ")
    }
    licenceEventRepository.saveAndFlush(
      EntityLicenceEvent(
        licenceId = newLicence.id,
        eventType = kind.copyEventType(),
        username = creator.username,
        forenames = creator.firstName,
        surname = creator.lastName,
        eventDescription = licenceEventMessage,
      ),
    )

    val auditEventSummary = when (newStatus) {
      VARIATION_IN_PROGRESS -> "Licence varied for ${newLicence.forename} ${newLicence.surname}"
      IN_PROGRESS -> "New licence version created for ${newLicence.forename} ${newLicence.surname}"
      else -> error("Invalid new licence status of $newStatus when creating a licence copy ")
    }
    auditEventRepository.saveAndFlush(
      AuditEvent(
        licenceId = licence.id,
        username = creator.username,
        fullName = "${creator.firstName} ${creator.lastName}",
        summary = auditEventSummary,
        detail = "Old ID ${licence.id}, new ID ${newLicence.id} type ${newLicence.typeCode} status ${newLicence.statusCode.name} version ${newLicence.version}",
      ),
    )

    return newLicence
  }

  private fun getCommunityOffenderManagerForCurrentUser(): CommunityOffenderManager {
    val username = SecurityContextHolder.getContext().authentication.name
    val staff = this.staffRepository.findByUsernameIgnoreCase(username)
      ?: error("Cannot find staff with username: $username")
    return if (staff is CommunityOffenderManager) staff else error("Cannot find staff with username: $username")
  }

  private fun AdditionalCondition.isNotAp() = LicenceType.valueOf(this.conditionType) != LicenceType.AP

  private fun splitName(fullName: String?): Pair<String?, String?> {
    val names = fullName?.split(" ")?.toMutableList()
    val firstName = names?.firstOrNull()
    names?.removeAt(0)
    val lastName = names?.joinToString(" ").orEmpty()
    return Pair(firstName, lastName)
  }

  @Transactional
  fun inactivateInProgressLicenceVersions(licences: List<EntityLicence>, reason: String? = null) {
    val licenceIds = licences.map { it.id }
    val licencesToDeactivate =
      licenceRepository.findAllByVersionOfIdInAndStatusCodeIn(licenceIds, listOf(IN_PROGRESS, SUBMITTED))
    if (licencesToDeactivate.isNotEmpty()) {
      inactivateLicences(licencesToDeactivate, reason, deactivateInProgressVersions = false)
    }
  }

  @Transactional
  fun inactivateTimedOutLicenceVersions(licences: List<EntityLicence>, reason: String? = null) {
    val bookingIds = licences.mapNotNull { it.bookingId }
    val licencesToDeactivate =
      crdLicenceRepository.findAllByBookingIdInAndStatusCodeOrderByDateCreatedDesc(bookingIds, TIMED_OUT)
    if (licencesToDeactivate.isNotEmpty()) {
      log.info("deactivating timeout licences: ${licencesToDeactivate.map { it.id }}")
      inactivateLicences(licencesToDeactivate, reason, deactivateInProgressVersions = false)
    }
  }

  @Transactional
  fun reviewWithNoVariationRequired(licenceId: Long) {
    val licenceEntity = getLicence(licenceId)

    if (licenceEntity !is HardStopLicence) throw ValidationException("Trying to review a ${licenceEntity::class.java.simpleName}: $licenceId")
    val username = SecurityContextHolder.getContext().authentication.name
    val staffMember = this.staffRepository.findByUsernameIgnoreCase(username)

    if (licenceEntity.reviewDate != null) {
      return
    }

    licenceEntity.markAsReviewed(staffMember)

    licenceRepository.saveAndFlush(licenceEntity)

    licenceEventRepository.saveAndFlush(
      EntityLicenceEvent(
        licenceId = licenceId,
        eventType = LicenceEventType.HARD_STOP_REVIEWED_WITHOUT_VARIATION,
        username = staffMember?.username ?: SYSTEM_USER,
        forenames = staffMember?.firstName,
        surname = staffMember?.lastName,
        eventDescription = "Licence reviewed without being varied for ${licenceEntity.forename} ${licenceEntity.surname}",
      ),
    )

    auditEventRepository.saveAndFlush(
      AuditEvent(
        licenceId = licenceId,
        username = staffMember?.username ?: SYSTEM_USER,
        fullName = "${staffMember?.firstName} ${staffMember?.lastName}",
        summary = "Licence reviewed without being varied for ${licenceEntity.forename} ${licenceEntity.surname}",
        detail = "ID $licenceId type ${licenceEntity.typeCode} status ${licenceEntity.reviewDate} version ${licenceEntity.version}",
      ),
    )
  }

  @Transactional
  fun timeout(licence: CrdLicence, reason: String? = null) {
    val timedOutLicence = licence.timeOut()
    licenceRepository.saveAndFlush(timedOutLicence)
    auditEventRepository.saveAndFlush(
      AuditEvent(
        licenceId = timedOutLicence.id,
        username = "SYSTEM",
        fullName = "SYSTEM",
        eventType = AuditEventType.SYSTEM_EVENT,
        summary = "Licence automatically timed out for ${timedOutLicence.forename} ${timedOutLicence.surname} ${reason ?: ""}",
        detail = "ID ${timedOutLicence.id} type ${timedOutLicence.typeCode} status ${timedOutLicence.statusCode} version ${timedOutLicence.version}",
      ),
    )
    licenceEventRepository.saveAndFlush(
      LicenceEvent(
        licenceId = timedOutLicence.id,
        eventType = LicenceEventType.TIMED_OUT,
        username = "SYSTEM",
        forenames = "SYSTEM",
        surname = "SYSTEM",
        eventDescription = "Licence automatically timed out for ${timedOutLicence.forename} ${timedOutLicence.surname} ${reason ?: ""}",
      ),
    )
    if (timedOutLicence.versionOfId != null) {
      val com = timedOutLicence.responsibleCom
      with(timedOutLicence) {
        notifyService.sendEditedLicenceTimedOutEmail(
          com.email,
          "${com.firstName} ${com.lastName}",
          this.forename!!,
          this.surname!!,
          this.crn,
          this.licenceStartDate,
          this.id.toString(),
        )
      }
    }
  }

  @Transactional
  fun deactivateLicenceAndVariations(licenceId: Long, body: DeactivateLicenceAndVariationsRequest) {
    val licences = licenceRepository.findLicenceAndVariations(licenceId)
    if (licences.isEmpty()) {
      log.info("Unable to deactivate licence and variations due to being unable to locate active licence for id: $licenceId")
      return
    }
    val deactivationReason = body.reason.message
    inactivateLicences(licences, deactivationReason, false)
  }

  private fun EntityLicence.toSummary() = transformToLicenceSummary(
    this,
    hardStopDate = releaseDateService.getHardStopDate(this),
    hardStopWarningDate = releaseDateService.getHardStopWarningDate(this),
    isInHardStopPeriod = releaseDateService.isInHardStopPeriod(this),
    isDueForEarlyRelease = releaseDateService.isDueForEarlyRelease(this),
    isDueToBeReleasedInTheNextTwoWorkingDays = releaseDateService.isDueToBeReleasedInTheNextTwoWorkingDays(this),
  )

  private fun assertCaseIsEligible(licenceId: Long, nomisId: String?) {
    if (nomisId == null) {
      throw ValidationException("Unable to perform action, licence $licenceId is missing NOMS ID")
    }

    val prisoner = prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(nomisId)).first()
    if (!eligibilityService.isEligibleForCvl(prisoner)) {
      throw ValidationException("Unable to perform action, licence $licenceId is ineligible for CVL")
    }
  }

  private fun getLicence(licenceId: Long): uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence = licenceRepository
    .findById(licenceId)
    .orElseThrow { EntityNotFoundException("$licenceId") }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
