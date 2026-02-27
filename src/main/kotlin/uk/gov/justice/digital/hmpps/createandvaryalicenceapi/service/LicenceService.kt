package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.timeserved.TimeServedLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CreateVariationResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.EditLicenceResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.EligibilityAssessment
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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response.LicencePermissionsResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.CrdLicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceQueryObject
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StaffRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.getSort
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.toSpecification
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions.ConditionPolicyData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions.getLicenceConditionPolicyData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions.upload.UploadFileConditionsService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.DomainEventsService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.LicencePolicyService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.util.Reviewable
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AuditEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.HARD_STOP
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.HDC
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.HDC_VARIATION
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.TIME_SERVED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.VARIATION
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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType.AP
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType.AP_PSS
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
  private val auditEventRepository: AuditEventRepository,
  private val notifyService: NotifyService,
  private val omuService: OmuService,
  private val releaseDateService: ReleaseDateService,
  private val domainEventsService: DomainEventsService,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val eligibilityService: EligibilityService,
  private val uploadFileConditionsService: UploadFileConditionsService,
  private val deliusApiClient: DeliusApiClient,
  private val telemetryService: TelemetryService,
  private val auditService: AuditService,
  @param:Value("\${feature.toggle.timeServed.enabled:false}")
  private val isTimeServedLogicEnabled: Boolean = false,
) {

  @Transactional(readOnly = true)
  fun getLicenceById(licenceId: Long): Licence {
    val entityLicence = getLicence(licenceId)
    val isEligibleForEarlyRelease = releaseDateService.isEligibleForEarlyRelease(entityLicence)
    val earliestReleaseDate = releaseDateService.getEarliestReleaseDate(entityLicence)

    val conditionsSubmissionStatus =
      getLicenceConditionPolicyData(
        entityLicence.additionalConditions,
        licencePolicyService.getAllAdditionalConditions(),
      )

    val licence = transform(entityLicence, earliestReleaseDate, isEligibleForEarlyRelease, conditionsSubmissionStatus)
    uploadFileConditionsService.getThumbnailForImages(entityLicence, licence)
    return licence
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
      isInHardStopPeriod = releaseDateService.isInHardStopPeriod(licence.licenceStartDate, licence.kind),
      hardStopDate = releaseDateService.getHardStopDate(licence.licenceStartDate, licence.kind),
      hardStopWarningDate = releaseDateService.getHardStopWarningDate(licence.licenceStartDate, licence.kind),
      isDueToBeReleasedInTheNextTwoWorkingDays = releaseDateService.isDueToBeReleasedInTheNextTwoWorkingDays(licence.licenceStartDate),
      conditionPolicyData = conditionPolicyData,
    )

    is CrdLicence -> toCrd(
      licence = licence,
      earliestReleaseDate = earliestReleaseDate,
      isEligibleForEarlyRelease = isEligibleForEarlyRelease,
      isInHardStopPeriod = releaseDateService.isInHardStopPeriod(licence.licenceStartDate, licence.kind),
      hardStopDate = releaseDateService.getHardStopDate(licence.licenceStartDate, licence.kind),
      hardStopWarningDate = releaseDateService.getHardStopWarningDate(licence.licenceStartDate, licence.kind),
      isDueToBeReleasedInTheNextTwoWorkingDays = releaseDateService.isDueToBeReleasedInTheNextTwoWorkingDays(licence.licenceStartDate),
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
      isInHardStopPeriod = releaseDateService.isInHardStopPeriod(licence.licenceStartDate, licence.kind),
      hardStopDate = releaseDateService.getHardStopDate(licence.licenceStartDate, licence.kind),
      hardStopWarningDate = releaseDateService.getHardStopWarningDate(licence.licenceStartDate, licence.kind),
      isDueToBeReleasedInTheNextTwoWorkingDays = releaseDateService.isDueToBeReleasedInTheNextTwoWorkingDays(licence.licenceStartDate),
      conditionPolicyData = conditionPolicyData,
    )

    is TimeServedLicence -> toTimeServed(
      licence = licence,
      earliestReleaseDate = earliestReleaseDate,
      isEligibleForEarlyRelease = isEligibleForEarlyRelease,
      isInHardStopPeriod = releaseDateService.isInHardStopPeriod(licence.licenceStartDate, licence.kind),
      hardStopDate = releaseDateService.getHardStopDate(licence.licenceStartDate, licence.kind),
      hardStopWarningDate = releaseDateService.getHardStopWarningDate(licence.licenceStartDate, licence.kind),
      isDueToBeReleasedInTheNextTwoWorkingDays = releaseDateService.isDueToBeReleasedInTheNextTwoWorkingDays(licence.licenceStartDate),
      conditionPolicyData = conditionPolicyData,
    )

    is HdcLicence -> toHdc(
      licence = licence,
      earliestReleaseDate = earliestReleaseDate,
      isEligibleForEarlyRelease = isEligibleForEarlyRelease,
      isInHardStopPeriod = releaseDateService.isInHardStopPeriod(licence.licenceStartDate, licence.kind),
      hardStopDate = releaseDateService.getHardStopDate(licence.licenceStartDate, licence.kind),
      hardStopWarningDate = releaseDateService.getHardStopWarningDate(licence.licenceStartDate, licence.kind),
      isDueToBeReleasedInTheNextTwoWorkingDays = releaseDateService.isDueToBeReleasedInTheNextTwoWorkingDays(licence.licenceStartDate),
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
      notifyOmuReApprovalNeeded(licenceEntity)
    }

    if (request.status == APPROVED) {
      when (licenceEntity) {
        is HardStopLicence -> notifyComAboutHardstopLicenceApproval(licenceEntity)

        is TimeServedLicence -> licenceEntity.responsibleCom?.let {
          notifyComAboutTimeServedLicenceApproval(licenceEntity)
        }
      }
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

  private fun notifyOmuReApprovalNeeded(licenceEntity: EntityLicence) {
    val omuEmail = licenceEntity.prisonCode?.let { omuService.getOmuContactEmail(it)?.email }
    notifyService.sendLicenceToOmuForReApprovalEmail(
      omuEmail,
      licenceEntity.forename ?: "unknown",
      licenceEntity.surname ?: "unknown",
      licenceEntity.nomsId,
      lsd = licenceEntity.licenceStartDate,
      crd = licenceEntity.conditionalReleaseDate,
    )
  }

  private fun notifyComAboutHardstopLicenceApproval(licenceEntity: HardStopLicence) {
    val com = licenceEntity.getCom()
    if (isTimeServedLogicEnabled) {
      notifyService.sendReviewableLicenceApprovedEmail(
        com.email,
        licenceEntity.forename!!,
        licenceEntity.surname!!,
        licenceEntity.crn,
        licenceEntity.licenceStartDate,
        licenceEntity.id.toString(),
        licenceEntity.prisonDescription!!,
      )
    } else {
      notifyService.sendHardStopLicenceApprovedEmail(
        com.email,
        licenceEntity.forename!!,
        licenceEntity.surname!!,
        licenceEntity.crn,
        licenceEntity.licenceStartDate,
        licenceEntity.id.toString(),
      )
    }
  }

  private fun notifyComAboutTimeServedLicenceApproval(licenceEntity: TimeServedLicence) {
    val comEmail = licenceEntity.responsibleCom?.email
    notifyService.sendReviewableLicenceApprovedEmail(
      comEmail,
      licenceEntity.forename!!,
      licenceEntity.surname!!,
      licenceEntity.crn,
      licenceEntity.licenceStartDate,
      licenceEntity.id.toString(),
      licenceEntity.prisonDescription!!,
      isTimeServedLicence = true,
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

    val nomisRecord = prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(licenceEntity.nomsId!!)).first()
    val eligibilityAssessment =
      eligibilityService.getEligibilityAssessment(nomisRecord)

    when (licenceEntity) {
      is PrrdLicence -> {
        assertCaseIsEligible(eligibilityAssessment, licenceId)
        licenceEntity.submit(submitter as CommunityOffenderManager)
      }

      is CrdLicence -> {
        assertCaseIsEligible(eligibilityAssessment, licenceId)
        licenceEntity
          .submit(submitter as CommunityOffenderManager)
      }

      is VariationLicence -> licenceEntity.submit(submitter as CommunityOffenderManager)

      is HardStopLicence -> {
        assertCaseIsEligible(eligibilityAssessment, licenceId)
        licenceEntity.submit(submitter as PrisonUser)
      }

      is HdcLicence -> {
        assertCaseIsEligible(eligibilityAssessment, licenceId)
        licenceEntity.submit(submitter as CommunityOffenderManager)
      }

      is HdcVariationLicence -> licenceEntity.submit(submitter as CommunityOffenderManager)

      is TimeServedLicence -> {
        assertCaseIsEligible(eligibilityAssessment, licenceId)
        licenceEntity.submit(submitter as PrisonUser)
      }

      else -> error("Unexpected licence type: $licenceEntity")
    }

    licenceRepository.saveAndFlush(licenceEntity)

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

  @Deprecated("Use specific queries instead")
  @Transactional(readOnly = true)
  fun findLicencesMatchingCriteria(licenceQueryObject: LicenceQueryObject): List<LicenceSummary> {
    try {
      val spec = licenceQueryObject.toSpecification()
      val sort = licenceQueryObject.getSort()
      val matchingLicences = licenceRepository.findAll(spec, sort)
      return matchingLicences.map { it.toSummary() }
    } catch (e: PropertyReferenceException) {
      throw ValidationException(e.message, e)
    }
  }

  @Transactional
  fun activateLicences(licences: List<EntityLicence>, reason: String? = null) {
    licences.forEach { it.activate() }
    if (licences.isNotEmpty()) {
      licenceRepository.saveAllAndFlush(licences)

      licences.forEach { licence ->
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

      licences.forEach { licence ->
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
  fun createVariation(licenceId: Long): CreateVariationResponse {
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

    telemetryService.recordLicenceCreatedEvent(licenceVariation)
    return CreateVariationResponse(licenceVariation.id)
  }

  @Transactional
  fun editLicence(licenceId: Long): EditLicenceResponse {
    val licence = getLicence(licenceId)
    if (isNotValidLicenceForEdit(licence)) error("Trying to edit licence for non-crd,non-hdc or non-prrd licence: $licenceId")

    if (licence.statusCode != APPROVED) {
      throw ValidationException("Can only edit APPROVED licences")
    }

    val inProgressVersions =
      licenceRepository.findAllByVersionOfIdInAndStatusCodeIn(listOf(licenceId), listOf(IN_PROGRESS, SUBMITTED))
    if (inProgressVersions.isNotEmpty()) {
      return EditLicenceResponse(inProgressVersions[0].id)
    }

    val nomisRecord = prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(licence.nomsId!!)).first()
    val eligibilityAssessment = eligibilityService.getEligibilityAssessment(nomisRecord)

    assertCaseIsEligible(eligibilityAssessment, licenceId)

    val creator = getCommunityOffenderManagerForCurrentUser()

    val copyToEdit = when (licence) {
      is HdcLicence -> LicenceFactory.createHdcCopyToEdit(licence, creator)
      is CrdLicence -> LicenceFactory.createCrdCopyToEdit(licence, creator)
      is PrrdLicence -> LicenceFactory.createPrrdCopyToEdit(licence, creator)
      else -> throw IllegalArgumentException("Unsupported licence type: ${licence.javaClass.simpleName}")
    }

    val licenceCopy = populateCopyAndAudit(licence.kind, licence, copyToEdit, creator)

    notifyOmuReApprovalNeeded(licence)
    return EditLicenceResponse(licenceCopy.id)
  }

  private fun isNotValidLicenceForEdit(licence: EntityLicence?): Boolean = licence != null &&
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

    val creatorName = licenceEntity.createdBy?.fullName().orEmpty()

    notifyService.sendVariationReferredEmail(
      licenceEntity.createdBy?.email.orEmpty(),
      creatorName,
      licenceEntity.responsibleCom?.email.orEmpty(),
      licenceEntity.responsibleCom?.fullName ?: creatorName,
      "${licenceEntity.forename.orEmpty()} ${licenceEntity.surname.orEmpty()}",
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

    val creatorName = licenceEntity.createdBy?.fullName().orEmpty()

    notifyService.sendVariationApprovedEmail(
      licenceEntity.createdBy?.email.orEmpty(),
      creatorName,
      licenceEntity.responsibleCom?.email.orEmpty(),
      licenceEntity.responsibleCom?.fullName ?: creatorName,
      "${licenceEntity.forename.orEmpty()} ${licenceEntity.surname.orEmpty()}",
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

    if (previousLicence is Reviewable) {
      previousLicence.markAsReviewed(user)
      licenceEventRepository.saveAndFlush(
        EntityLicenceEvent(
          licenceId = licenceId,
          eventType = LicenceEventType.REVIEWED_WITH_VARIATION,
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

    // get deletableDocumentUuids before data is changed on the DB
    val deletableDocumentUuids =
      uploadFileConditionsService.getDeletableDocumentUuids(licenceEntity.additionalConditions)
    licenceRepository.delete(licenceEntity)
    // Delete Documents after all above work is done, just encase exception is thrown before now!
    uploadFileConditionsService.deleteDocuments(deletableDocumentUuids)
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

    licenceCopy.version = licencePolicyService.currentPolicy(licence.licenceStartDate).version
    licenceCopy.standardConditions.addAll(
      licence.standardConditions.map { it.copy(id = null, licence = licenceCopy) },
    )

    val isNowInPssPeriod =
      licence.kind.isVariation() && licence.typeCode == AP_PSS && licence.isInPssPeriod()

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
          additionalConditionUpload = mutableListOf(),
        )

        val data = condition.additionalConditionData.map {
          it.copy(id = null, additionalCondition = copiedCondition)
        }
        val summary = condition.additionalConditionUpload.map {
          it.copy(id = null, additionalCondition = copiedCondition)
        }

        copiedCondition.additionalConditionData.addAll(data)
        copiedCondition.additionalConditionUpload.addAll(summary)
        copiedCondition
      }

    // This needs to be saved here before the below code uses the condition.id
    licenceCopy.additionalConditions.addAll(copiedAdditionalConditions)
    licenceRepository.saveAndFlush(licenceCopy)

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

  private fun AdditionalCondition.isNotAp() = LicenceType.valueOf(this.conditionType) != AP

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

    if (licenceEntity !is Reviewable) throw ValidationException("Trying to review a ${licenceEntity::class.java.simpleName}: $licenceId")
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
        eventType = LicenceEventType.REVIEWED_WITHOUT_VARIATION,
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
          this.getCom().email,
          this.getCom().fullName,
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

  @Transactional
  fun getLicencePermissions(licenceId: Long, teamCodes: List<String>): LicencePermissionsResponse {
    val licenceEntity = getLicence(licenceId)
    val offenderManager = deliusApiClient.getOffenderManager(licenceEntity.crn!!)
      ?: error("No active offender manager found for CRN: ${licenceEntity.crn}")

    val licences = findLicencesMatchingCriteria(LicenceQueryObject(nomsIds = listOf(licenceEntity.nomsId!!)))
    val viewAccess = licences.any {
      teamCodes.contains(offenderManager.team.code)
    }
    return LicencePermissionsResponse(viewAccess)
  }

  @Transactional
  fun updateLicenceKind(licence: EntityLicence, updatedKind: LicenceKind): EntityLicence {
    if (licence.kind == HDC) return licence

    val isKindUpdated =
      licence.kind !in listOf(HARD_STOP, TIME_SERVED, VARIATION) && updatedKind != licence.kind
    val isEligibleKindUpdated = updatedKind != licence.eligibleKind

    val newKind = if (isKindUpdated) updatedKind else licence.kind
    val newEligibleKind = if (isEligibleKindUpdated) updatedKind else licence.eligibleKind

    if (isKindUpdated || isEligibleKindUpdated) {
      if (isKindUpdated) {
        log.info("Updating licence kind for nomis id: ${licence.nomsId} from ${licence.kind} to $newKind")
      }

      if (isEligibleKindUpdated) {
        log.info("Updating eligible licence kind for nomis id: ${licence.nomsId} from ${licence.eligibleKind} to $newEligibleKind")
      }

      val userUpdating =
        staffRepository.findByUsernameIgnoreCase(SecurityContextHolder.getContext().authentication.name)
      auditService.recordAuditEventLicenceKindUpdated(
        licence,
        licence.kind,
        newKind,
        licence.eligibleKind,
        newEligibleKind,
        userUpdating,
      )
      licenceRepository.updateLicenceKinds(licence.id, newKind, newEligibleKind)
      return getLicence(licence.id)
    }
    return licence
  }

  private fun EntityLicence.toSummary(): LicenceSummary = transformToLicenceSummary(
    this,
    hardStopDate = releaseDateService.getHardStopDate(licenceStartDate, this.kind),
    hardStopWarningDate = releaseDateService.getHardStopWarningDate(licenceStartDate, this.kind),
    isInHardStopPeriod = releaseDateService.isInHardStopPeriod(licenceStartDate, this.kind),
    isDueToBeReleasedInTheNextTwoWorkingDays = releaseDateService.isDueToBeReleasedInTheNextTwoWorkingDays(
      licenceStartDate,
    ),
  )

  private fun assertCaseIsEligible(eligibilityAssessment: EligibilityAssessment, licenceId: Long) {
    if (!eligibilityAssessment.isEligible) {
      throw ValidationException("Unable to perform action, licence $licenceId is ineligible for CVL")
    }
  }

  private fun CommunityOffenderManager.fullName(): String = "${firstName.orEmpty()} ${lastName.orEmpty()}".trim()

  private fun getLicence(licenceId: Long): EntityLicence = licenceRepository
    .findById(licenceId)
    .orElseThrow { EntityNotFoundException("$licenceId") }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
