package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.data.mapping.PropertyReferenceException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AlwaysHasCom
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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.SupportsHardStop
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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionUploadDetailRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.CrdLicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceQueryObject
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StaffRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.getSort
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.toSpecification
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions.ConditionPolicyData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions.ExclusionZoneService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions.getLicenceConditionPolicyData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.DomainEventsService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.LicencePolicyService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AuditEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.HDC_VARIATION
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.PRRD
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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.TimeServedConsiderations
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.determineReleaseDateKind
import java.time.LocalDate
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence as EntityLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.LicenceEvent as EntityLicenceEvent

@Service
class LicenceService(
  private val licenceRepository: LicenceRepository,
  private val crdLicenceRepository: CrdLicenceRepository,
  private val staffRepository: StaffRepository,
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
  private val exclusionZoneService: ExclusionZoneService,
) {

  @TimeServedConsiderations("Spike finding - uses COM when retrieving the licence - should be fine - need to change transform if new licence kind created or existing licence has nullable COM")
  @Transactional
  fun getLicenceById(licenceId: Long): Licence {
    val entityLicence = getLicence(licenceId)

    exclusionZoneService.preloadThumbnailsFor(entityLicence)

    val isEligibleForEarlyRelease = releaseDateService.isEligibleForEarlyRelease(entityLicence)
    val earliestReleaseDate = releaseDateService.getEarliestReleaseDate(entityLicence)

    val conditionsSubmissionStatus =
      getLicenceConditionPolicyData(
        entityLicence.additionalConditions,
        licencePolicyService.getAllAdditionalConditions(),
      )

    return transform(entityLicence, earliestReleaseDate, isEligibleForEarlyRelease, conditionsSubmissionStatus)
  }

  fun transform(
    licence: EntityLicence,
    earliestReleaseDate: LocalDate?,
    isEligibleForEarlyRelease: Boolean,
    conditionPolicyData: Map<String, ConditionPolicyData>,
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
      conditionPolicyData = conditionPolicyData,
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
      conditionPolicyData = conditionPolicyData,
    )

    is VariationLicence -> toVariation(
      licence = licence,
      earliestReleaseDate = earliestReleaseDate,
      isEligibleForEarlyRelease = isEligibleForEarlyRelease,
      conditionPolicyData = conditionPolicyData,
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
      conditionPolicyData = conditionPolicyData,
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
      conditionPolicyData = conditionPolicyData,
    )

    is HdcVariationLicence -> toHdcVariation(
      licence = licence,
      earliestReleaseDate = earliestReleaseDate,
      isEligibleForEarlyRelease = isEligibleForEarlyRelease,
      conditionPolicyData = conditionPolicyData,
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
          is PrrdLicence, is CrdLicence, is HdcLicence ->
            deactivatePreviousLicenceVersion(licenceEntity, request.fullName, staffMember)
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

    val isReApproval = licenceEntity.statusCode === APPROVED && request.status === SUBMITTED

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

    // if previous status was APPROVED and the new status is SUBMITTED then email OMU regarding re-approval
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
      lsd = licenceEntity.licenceStartDate,
      crd = licenceEntity.conditionalReleaseDate,
    )
  }

  private fun notifyComAboutHardstopLicenceApproval(licenceEntity: HardStopLicence) {
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

    val licenceToDeactivate = licenceRepository.findById(previousVersionId)
      .orElseThrow { EntityNotFoundException("$previousVersionId") }

    when (licenceToDeactivate) {
      is CrdLicence, is PrrdLicence, is HdcLicence -> {
        licenceToDeactivate.dateLastUpdated = LocalDateTime.now()
        licenceToDeactivate.updatedByUsername = staffMember?.username ?: SYSTEM_USER
        licenceToDeactivate.statusCode = INACTIVE
        licenceToDeactivate.updatedBy = staffMember ?: licenceToDeactivate.updatedBy
      }

      else -> error("Trying to inactivate non-crd licence: $previousVersionId")
    }

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

    auditStatusChange(licenceToDeactivate, staffMember)
  }

  @Transactional
  fun submitLicence(licenceId: Long, notifyRequest: List<NotifyRequest>?) {
    val licenceEntity = getLicence(licenceId)

    val username = SecurityContextHolder.getContext().authentication.name
    val submitter = staffRepository.findByUsernameIgnoreCase(username)
      ?: throw ValidationException("Staff with username $username not found")

    when (licenceEntity) {
      is PrrdLicence -> {
        licenceEntity.submit(submitter as CommunityOffenderManager)
      }

      is CrdLicence -> {
        assertCaseIsEligible(licenceEntity, licenceEntity.nomsId)
        licenceEntity
          .submit(submitter as CommunityOffenderManager)
      }

      is VariationLicence -> licenceEntity.submit(submitter as CommunityOffenderManager)
      is HardStopLicence -> {
        if (determineReleaseDateKind(licenceEntity.postRecallReleaseDate, licenceEntity.conditionalReleaseDate) != PRRD) {
          assertCaseIsEligible(licenceEntity, licenceEntity.nomsId)
        }
        licenceEntity.submit(submitter as PrisonUser)
      }

      is HdcLicence -> {
        assertCaseIsEligible(licenceEntity, licenceEntity.nomsId)
        licenceEntity.submit(submitter as CommunityOffenderManager)
      }

      is HdcVariationLicence -> licenceEntity.submit(submitter as CommunityOffenderManager)
      else -> error("Unexpected licence type: $licenceEntity")
    }

    licenceEventRepository.saveAndFlush(
      EntityLicenceEvent(
        licenceId = licenceId,
        eventType = licenceEntity.kind.submittedEventType(),
        username = username,
        forenames = submitter.firstName,
        surname = submitter.lastName,
        eventDescription = "Licence submitted for approval for ${licenceEntity.forename} ${licenceEntity.surname}",
      ),
    )

    auditEventRepository.saveAndFlush(
      AuditEvent(
        licenceId = licenceId,
        username = username,
        fullName = submitter.fullName,
        summary = "Licence submitted for approval for ${licenceEntity.forename} ${licenceEntity.surname}",
        detail = "ID $licenceId type ${licenceEntity.typeCode} status ${licenceEntity.statusCode.name} version ${licenceEntity.version}",
      ),
    )

    // Notify the head of PDU of this submitted licence variation
    if (licenceEntity is Variation) {
      notifyRequest?.forEach {
        notifyService.sendVariationForApprovalEmail(
          it,
          licenceId.toString(),
          licenceEntity.forename!!,
          licenceEntity.surname!!,
          licenceEntity.crn!!,
          submitter.fullName,
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

    if (licence.kind != PRRD) {
      assertCaseIsEligible(licence, licence.nomsId)
    }

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

  @TimeServedConsiderations("Do we refer this variation if it does not have a COM - should variations always have a COM?")
  @Transactional
  fun referLicenceVariation(licenceId: Long, referVariationRequest: ReferVariationRequest) {
    val licenceEntity = getLicence(licenceId)
    if (licenceEntity !is Variation) error("Trying to reject non-variation: $licenceId")
    check(licenceEntity is AlwaysHasCom) { "Licence has no responsible COM: ${licenceEntity.id}" }
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

  @TimeServedConsiderations("Do we approve a variation if it does not have a COM - should variations always have a COM?")
  @Transactional
  fun approveLicenceVariation(licenceId: Long) {
    val licenceEntity = getLicence(licenceId)
    if (licenceEntity !is Variation) error("Trying to approve non-variation: $licenceId")
    check(licenceEntity is AlwaysHasCom) { "Licence has no responsible COM: ${licenceEntity.id}" }
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
    log.info("Deleting documents for Licence id={}", licenceEntity.id)
    exclusionZoneService.deleteDocumentsFor(licenceEntity.additionalConditions)
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

    licenceCopy.bespokeConditions.clear()
    licenceCopy.standardConditions.clear()
    licenceCopy.additionalConditions.clear()

    licenceCopy.version = licencePolicyService.currentPolicy().version
    licenceCopy.standardConditions.addAll(
      licence.standardConditions.map { it.copy(id = null, licence = licenceCopy) },
    )

    val isNowInPssPeriod =
      licence is Variation && licence.typeCode == LicenceType.AP_PSS && licence.isInPssPeriod()

    if (!isNowInPssPeriod) {
      licenceCopy.bespokeConditions.addAll(
        licence.bespokeConditions.map { it.copy(id = null, licence = licenceCopy) },
      )
    }

    val copiedAdditionalConditions = licence.additionalConditions
      .run { if (isNowInPssPeriod) filter { it.isNotAp() } else this }
      .map { condition ->

        val copiedCondition = condition.copy(
          id = null,
          licence = licenceCopy,
          additionalConditionData = mutableListOf(),
          additionalConditionUploadSummary = mutableListOf(),
        )

        val data = condition.additionalConditionData.map {
          it.copy(id = null, additionalCondition = copiedCondition)
        }
        val summary = condition.additionalConditionUploadSummary.map {
          it.copy(id = null, additionalCondition = copiedCondition)
        }

        copiedCondition.additionalConditionData.addAll(data)
        copiedCondition.additionalConditionUploadSummary.addAll(summary)
        copiedCondition
      }

    // This needs to be saved here before the below code uses the condition.id
    licenceCopy.additionalConditions.addAll(copiedAdditionalConditions)
    licenceRepository.saveAndFlush(licenceCopy)

    copiedAdditionalConditions.forEach { condition ->
      condition.additionalConditionUploadSummary.forEach {
        var uploadDetail = additionalConditionUploadDetailRepository.getReferenceById(it.uploadDetailId)
        uploadDetail = uploadDetail.copy(id = null, licenceId = licenceCopy.id, additionalConditionId = condition.id!!)
        val savedUploadDetail = additionalConditionUploadDetailRepository.save(uploadDetail)
        it.uploadDetailId = savedUploadDetail.id!!
      }
    }

    val licenceEventMessage = when (licenceCopy.statusCode) {
      VARIATION_IN_PROGRESS -> "A variation was created for ${licenceCopy.forename} ${licenceCopy.surname} from ID ${licence.id}"
      IN_PROGRESS -> "A new licence version was created for ${licenceCopy.forename} ${licenceCopy.surname} from ID ${licence.id}"
      else -> error("Invalid new licence status of ${licenceCopy.statusCode} when creating a licence copy ")
    }
    licenceEventRepository.saveAndFlush(
      EntityLicenceEvent(
        licenceId = licenceCopy.id,
        eventType = kind.copyEventType(),
        username = creator.username,
        forenames = creator.firstName,
        surname = creator.lastName,
        eventDescription = licenceEventMessage,
      ),
    )

    val auditEventSummary = when (newStatus) {
      VARIATION_IN_PROGRESS -> "Licence varied for ${licenceCopy.forename} ${licenceCopy.surname}"
      IN_PROGRESS -> "New licence version created for ${licenceCopy.forename} ${licenceCopy.surname}"
      else -> error("Invalid new licence status of $newStatus when creating a licence copy ")
    }
    auditEventRepository.saveAndFlush(
      AuditEvent(
        licenceId = licence.id,
        username = creator.username,
        fullName = "${creator.firstName} ${creator.lastName}",
        summary = auditEventSummary,
        detail = "Old ID ${licence.id}, new ID ${licenceCopy.id} type ${licenceCopy.typeCode} status ${licenceCopy.statusCode.name} version ${licenceCopy.version}",
      ),
    )

    return licenceCopy
  }

  private fun getCommunityOffenderManagerForCurrentUser(): CommunityOffenderManager {
    val username = SecurityContextHolder.getContext().authentication.name
    val staff = this.staffRepository.findByUsernameIgnoreCase(username)
      ?: error("Cannot find staff with username: $username")
    return staff as? CommunityOffenderManager ?: error("Cannot find staff with username: $username")
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
  fun timeout(licence: EntityLicence, reason: String? = null) {
    check(licence is SupportsHardStop) { "Can only timeout licence kinds that support hard stop: ${licence.id}" }
    licence.timeOut()
    licenceRepository.saveAndFlush(licence)
    auditEventRepository.saveAndFlush(
      AuditEvent(
        licenceId = licence.id,
        username = "SYSTEM",
        fullName = "SYSTEM",
        eventType = AuditEventType.SYSTEM_EVENT,
        summary = "Licence automatically timed out for ${licence.forename} ${licence.surname} ${reason ?: ""}",
        detail = "ID ${licence.id} type ${licence.typeCode} status ${licence.statusCode} version ${licence.version}",
      ),
    )
    licenceEventRepository.saveAndFlush(
      LicenceEvent(
        licenceId = licence.id,
        eventType = LicenceEventType.TIMED_OUT,
        username = "SYSTEM",
        forenames = "SYSTEM",
        surname = "SYSTEM",
        eventDescription = "Licence automatically timed out for ${licence.forename} ${licence.surname} ${reason ?: ""}",
      ),
    )

    if (licence.versionOfId != null && licence is AlwaysHasCom) {
      with(licence) {
        notifyService.sendEditedLicenceTimedOutEmail(
          responsibleCom.email,
          "${responsibleCom.firstName} ${responsibleCom.lastName}",
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

  private fun assertCaseIsEligible(licence: EntityLicence, nomisId: String?) {
    if (nomisId == null) {
      throw ValidationException("Unable to perform action, licence ${licence.id} is missing NOMS ID")
    }

    val prisoner = prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(nomisId)).first()
    if (!eligibilityService.isEligibleForCvl(prisoner, licence.probationAreaCode)) {
      throw ValidationException("Unable to perform action, licence ${licence.id} is ineligible for CVL")
    }
  }

  private fun getLicence(licenceId: Long): uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence = licenceRepository
    .findById(licenceId)
    .orElseThrow { EntityNotFoundException("$licenceId") }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
