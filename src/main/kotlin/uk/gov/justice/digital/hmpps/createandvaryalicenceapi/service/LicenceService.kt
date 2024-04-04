package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.springframework.data.mapping.PropertyReferenceException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HardStopLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence.Companion.SYSTEM_USER
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.PrisonUser
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Staff
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.VariationLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceSummaryApproverView
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.StatusUpdateRequest
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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceQueryObject
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StaffRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StandardConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.getSort
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.toSpecification
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.DomainEventsService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AuditEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.CRD
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.VARIATION
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.ACTIVE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.APPROVED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.INACTIVE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.IN_PROGRESS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.REJECTED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.SUBMITTED
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
) {

  @Transactional
  fun getLicenceById(licenceId: Long): Licence {
    val entityLicence = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }

    val releaseDate = entityLicence.actualReleaseDate ?: entityLicence.conditionalReleaseDate
    val isEligibleForEarlyRelease =
      releaseDate !== null && releaseDateService.isEligibleForEarlyRelease(releaseDate)

    val earliestReleaseDate = when {
      isEligibleForEarlyRelease -> releaseDateService.getEarliestReleaseDate(releaseDate!!)
      else -> releaseDate
    }

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
  ): Licence =
    when (licence) {
      is CrdLicence -> toCrd(
        licence = licence,
        earliestReleaseDate = earliestReleaseDate,
        isEligibleForEarlyRelease = isEligibleForEarlyRelease,
        isInHardStopPeriod = releaseDateService.isInHardStopPeriod(licence),
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
        conditionSubmissionStatus = conditionSubmissionStatus,
      )

      else -> error("could not convert licence of type: ${licence.kind} for licence: ${licence.id}")
    }

  @Transactional
  fun updateLicenceStatus(licenceId: Long, request: StatusUpdateRequest) {
    val licenceEntity = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }
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
          is CrdLicence -> deactivatePreviousLicenceVersion(licenceEntity, request.fullName, staffMember)
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

    val updatedLicence = licenceEntity.updateStatus(
      statusCode = request.status,
      staffMember = staffMember,
      approvedByUsername = approvedByUser,
      approvedByName = approvedByName,
      approvedDate = approvedDate,
      supersededDate = supersededDate,
      submittedDate = submittedDate,
      licenceActivatedDate = licenceActivatedDate,
    )
    licenceRepository.saveAndFlush(updatedLicence)

    // if previous status was APPROVED and the new status is IN_PROGRESS then email OMU regarding re-approval
    if (isReApproval) {
      notifyReApprovalNeeded(licenceEntity)
    }

    recordLicenceEventForStatus(licenceEntity.id, updatedLicence, request)
    auditStatusChange(updatedLicence, staffMember)
    domainEventsService.recordDomainEvent(updatedLicence, request.status)
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

  private fun getAuditEventType(username: String): AuditEventType {
    return if (username == "SYSTEM_USER") {
      AuditEventType.SYSTEM_EVENT
    } else {
      AuditEventType.USER_EVENT
    }
  }

  private fun recordLicenceEventForStatus(
    licenceId: Long,
    licenceEntity: EntityLicence,
    request: StatusUpdateRequest,
  ) {
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
        licenceId = licenceId,
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

  private fun deactivatePreviousLicenceVersion(licence: CrdLicence, fullName: String?, staffMember: Staff?) {
    val previousVersionId = licence.versionOfId

    if (previousVersionId != null) {
      val previousLicenceVersion =
        licenceRepository.findById(previousVersionId)
          .orElseThrow { EntityNotFoundException("$previousVersionId") }

      if (previousLicenceVersion !is CrdLicence) error("Trying to inactivate non-crd licence: $previousVersionId")

      val updatedLicence = previousLicenceVersion.copy(
        dateLastUpdated = LocalDateTime.now(),
        updatedByUsername = staffMember?.username ?: SYSTEM_USER,
        statusCode = INACTIVE,
        updatedBy = staffMember ?: previousLicenceVersion.updatedBy,
      )
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
  }

  @Transactional
  fun submitLicence(licenceId: Long, notifyRequest: List<NotifyRequest>?) {
    val licenceEntity = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }

    val username = SecurityContextHolder.getContext().authentication.name
    val submitter = staffRepository.findByUsernameIgnoreCase(username)
      ?: throw ValidationException("Staff with username $username not found")

    val updatedLicence = when (licenceEntity) {
      is CrdLicence -> licenceEntity.submit(submitter as CommunityOffenderManager)
      is VariationLicence -> licenceEntity.submit(submitter as CommunityOffenderManager)
      is HardStopLicence -> licenceEntity.submit(submitter as PrisonUser)
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
    if (updatedLicence.kind == VARIATION) {
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
      return transformToListOfSummaries(matchingLicences)
    } catch (e: PropertyReferenceException) {
      throw ValidationException(e.message, e)
    }
  }

  @Transactional
  fun findRecentlyApprovedLicences(
    prisonCodes: List<String>,
  ): List<LicenceSummaryApproverView> {
    try {
      val releasedAfterDate = LocalDate.now().minusDays(14L)
      val recentActiveAndApprovedLicences =
        licenceRepository.getRecentlyApprovedLicences(prisonCodes, releasedAfterDate)

      // if a licence is an active variation then we want to return the original
      // licence that the variation was created from and not the variation itself
      val recentlyApprovedLicences = recentActiveAndApprovedLicences.map {
        if (it.statusCode == ACTIVE && it is VariationLicence) {
          findOriginalLicenceForVariation(it)
        } else {
          it
        }
      }
      return transformToListOfLicenceSummariesForApproverView(recentlyApprovedLicences)
    } catch (e: PropertyReferenceException) {
      throw ValidationException(e.message, e)
    }
  }

  fun findSubmittedVariationsByRegion(probationAreaCode: String): List<LicenceSummary> {
    val matchingLicences =
      licenceRepository.findByStatusCodeAndProbationAreaCode(VARIATION_SUBMITTED, probationAreaCode)
    return transformToListOfSummaries(matchingLicences)
  }

  @Transactional
  fun activateLicences(licences: List<EntityLicence>, reason: String? = null) {
    val activatedLicences = licences.map { it.activate() }
    if (activatedLicences.isNotEmpty()) {
      licenceRepository.saveAllAndFlush(activatedLicences)

      activatedLicences.map { licence ->
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
        activatedLicences,
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
    val inActivatedLicences = licences.map { it.deactivate() }
    if (inActivatedLicences.isNotEmpty()) {
      licenceRepository.saveAllAndFlush(inActivatedLicences)

      inActivatedLicences.map { licence ->
        auditEventRepository.saveAndFlush(
          AuditEvent(
            licenceId = licence.id,
            username = "SYSTEM",
            fullName = "SYSTEM",
            eventType = AuditEventType.SYSTEM_EVENT,
            summary = "${reason ?: "Licence automatically inactivated"} for ${licence.forename} ${licence.surname}",
            detail = "ID ${licence.id} type ${licence.typeCode} status ${licence.statusCode.name} version ${licence.version}",
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
        inactivateInProgressLicenceVersions(
          inActivatedLicences,
        )
      }
    }
  }

  @Transactional
  fun inActivateLicencesByIds(licenceIds: List<Long>) {
    val matchingLicences = licenceRepository.findAllById(licenceIds)
    inactivateLicences(matchingLicences)
  }

  @Transactional
  fun createVariation(licenceId: Long): LicenceSummary {
    val licence = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }
    val creator = getCommunityOffenderManagerForCurrentUser()
    val licenceCopy = LicenceFactory.createVariation(licence, creator)
    val licenceVariation = populateCopyAndAudit(VARIATION, licence, licenceCopy, creator)
    return transformToLicenceSummary(licenceVariation)
  }

  @Transactional
  fun editLicence(licenceId: Long): LicenceSummary {
    val licence = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }
    if (licence !is CrdLicence) error("Trying to edit licence for non-crd licence: $licenceId")

    if (licence.statusCode != APPROVED) {
      throw ValidationException("Can only edit APPROVED licences")
    }

    val inProgressVersions =
      licenceRepository.findAllByVersionOfIdInAndStatusCodeIn(listOf(licenceId), listOf(IN_PROGRESS, SUBMITTED))
    if (inProgressVersions.isNotEmpty()) {
      return transformToLicenceSummary(inProgressVersions[0])
    }

    val creator = getCommunityOffenderManagerForCurrentUser()
    val copyToEdit = LicenceFactory.createCopyToEdit(licence, creator)
    val licenceCopy = populateCopyAndAudit(CRD, licence, copyToEdit, creator)
    notifyReApprovalNeeded(licence)

    return transformToLicenceSummary(licenceCopy)
  }

  @Transactional
  fun updateSpoDiscussion(licenceId: Long, spoDiscussionRequest: UpdateSpoDiscussionRequest) {
    val licenceEntity = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }
    if (licenceEntity !is VariationLicence) error("Trying to update spo discussion for non-variation: $licenceId")

    val username = SecurityContextHolder.getContext().authentication.name

    val staffMember = this.staffRepository.findByUsernameIgnoreCase(username)

    val updatedLicenceEntity = licenceEntity.copy(
      spoDiscussion = spoDiscussionRequest.spoDiscussion,
      dateLastUpdated = LocalDateTime.now(),
      updatedByUsername = staffMember?.username ?: SYSTEM_USER,
      updatedBy = staffMember ?: licenceEntity.updatedBy,
    )

    licenceRepository.saveAndFlush(updatedLicenceEntity)
  }

  @Transactional
  fun updateVloDiscussion(licenceId: Long, vloDiscussionRequest: UpdateVloDiscussionRequest) {
    val licenceEntity = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }
    if (licenceEntity !is VariationLicence) error("Trying to update vlo discussion for non-variation: $licenceId")

    val username = SecurityContextHolder.getContext().authentication.name

    val staffMember = this.staffRepository.findByUsernameIgnoreCase(username)

    val updatedLicenceEntity = licenceEntity.copy(
      vloDiscussion = vloDiscussionRequest.vloDiscussion,
      dateLastUpdated = LocalDateTime.now(),
      updatedByUsername = staffMember?.username ?: SYSTEM_USER,
      updatedBy = staffMember ?: licenceEntity.updatedBy,
    )

    licenceRepository.saveAndFlush(updatedLicenceEntity)
  }

  @Transactional
  fun updateReasonForVariation(licenceId: Long, reasonForVariationRequest: UpdateReasonForVariationRequest) {
    val licenceEntity = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }
    if (licenceEntity !is VariationLicence) error("Trying to update variation reason for non-variation: $licenceId")

    val username = SecurityContextHolder.getContext().authentication.name
    val staffMember = this.staffRepository.findByUsernameIgnoreCase(username)

    val updatedLicenceEntity = licenceEntity.copy(
      dateLastUpdated = LocalDateTime.now(),
      updatedByUsername = staffMember?.username ?: SYSTEM_USER,
      updatedBy = staffMember ?: licenceEntity.updatedBy,
    )
    licenceRepository.saveAndFlush(updatedLicenceEntity)

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
    val licenceEntity = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }
    if (licenceEntity !is VariationLicence) error("Trying to reject non-variation: $licenceId")
    val username = SecurityContextHolder.getContext().authentication.name
    val staffMember = this.staffRepository.findByUsernameIgnoreCase(username)

    val updatedLicenceEntity = licenceEntity.copy(
      statusCode = VARIATION_REJECTED,
      dateLastUpdated = LocalDateTime.now(),
      updatedByUsername = staffMember?.username ?: SYSTEM_USER,
      updatedBy = staffMember ?: licenceEntity.updatedBy,
    )

    licenceRepository.saveAndFlush(updatedLicenceEntity)

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
        detail = "ID $licenceId type ${licenceEntity.typeCode} status ${updatedLicenceEntity.statusCode.name} version ${licenceEntity.version}",
      ),
    )

    notifyService.sendVariationReferredEmail(
      licenceEntity.createdBy?.email ?: "",
      "${licenceEntity.createdBy?.firstName} ${licenceEntity.createdBy?.lastName}",
      licenceEntity.responsibleCom?.email ?: "",
      "${licenceEntity.responsibleCom?.firstName} ${licenceEntity.responsibleCom?.lastName}",
      "${licenceEntity.forename} ${licenceEntity.surname}",
      licenceId.toString(),
    )
  }

  @Transactional
  fun approveLicenceVariation(licenceId: Long) {
    val licenceEntity = licenceRepository.findById(licenceId).orElseThrow { EntityNotFoundException("$licenceId") }
    if (licenceEntity !is VariationLicence) error("Trying to approve non-variation: $licenceId")
    val username = SecurityContextHolder.getContext().authentication.name
    val staffMember = this.staffRepository.findByUsernameIgnoreCase(username)

    val updatedLicenceEntity = licenceEntity.copy(
      statusCode = VARIATION_APPROVED,
      dateLastUpdated = LocalDateTime.now(),
      updatedByUsername = staffMember?.username ?: SYSTEM_USER,
      approvedByUsername = username,
      approvedDate = LocalDateTime.now(),
      approvedByName = "${staffMember?.firstName} ${staffMember?.lastName}",
      updatedBy = staffMember ?: licenceEntity.updatedBy,
    )

    licenceRepository.saveAndFlush(updatedLicenceEntity)

    licenceEventRepository.saveAndFlush(
      EntityLicenceEvent(
        licenceId = licenceId,
        eventType = LicenceEventType.VARIATION_APPROVED,
        username = staffMember?.username ?: SYSTEM_USER,
        forenames = staffMember?.firstName,
        surname = staffMember?.lastName,
        eventDescription = "Licence variation approved for ${updatedLicenceEntity.forename} ${updatedLicenceEntity.surname}",
      ),
    )

    auditEventRepository.saveAndFlush(
      AuditEvent(
        licenceId = licenceId,
        username = staffMember?.username ?: SYSTEM_USER,
        fullName = "${staffMember?.firstName} ${staffMember?.lastName}",
        summary = "Licence variation approved for ${licenceEntity.forename} ${licenceEntity.surname}",
        detail = "ID $licenceId type ${licenceEntity.typeCode} status ${updatedLicenceEntity.statusCode.name} version ${licenceEntity.version}",
      ),
    )

    notifyService.sendVariationApprovedEmail(
      licenceEntity.createdBy?.email ?: "",
      "${licenceEntity.createdBy?.firstName} ${licenceEntity.createdBy?.lastName}",
      licenceEntity.responsibleCom?.email ?: "",
      "${licenceEntity.responsibleCom?.firstName} ${licenceEntity.responsibleCom?.lastName}",
      "${licenceEntity.forename} ${licenceEntity.surname}",
      licenceId.toString(),
    )
  }

  @Transactional
  fun activateVariation(licenceId: Long) {
    val licence = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }

    if (licence !is VariationLicence || licence.statusCode != VARIATION_APPROVED) {
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
    }

    updateLicenceStatus(
      previousLicence,
      StatusUpdateRequest(status = INACTIVE, username = user.username, fullName = user.fullName),
    )
  }

  @Transactional
  fun discardLicence(licenceId: Long) {
    val licenceEntity = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }

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
    val licenceEntity = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }

    val username = SecurityContextHolder.getContext().authentication.name

    val staffMember = this.staffRepository.findByUsernameIgnoreCase(username)

    val updatedLicenceEntity = licenceEntity.updatePrisonInfo(
      prisonCode = prisonInformationRequest.prisonCode,
      prisonDescription = prisonInformationRequest.prisonDescription,
      prisonTelephone = prisonInformationRequest.prisonTelephone,
      staffMember = staffMember,
    )

    licenceRepository.saveAndFlush(updatedLicenceEntity)

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
    val isVariation = kind == VARIATION
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
      isVariation && licence.typeCode == LicenceType.AP_PSS && licence.isInPssPeriod()

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

  private fun AdditionalCondition.isNotAp() = LicenceType.valueOf(this.conditionType!!) != LicenceType.AP

  private fun splitName(fullName: String?): Pair<String?, String?> {
    val names = fullName?.split(" ")?.toMutableList()
    val firstName = names?.firstOrNull()
    names?.removeAt(0)
    val lastName = names?.joinToString(" ").orEmpty()
    return Pair(firstName, lastName)
  }

  private fun findOriginalLicenceForVariation(variationLicence: VariationLicence): CrdLicence {
    var originalLicence = variationLicence
    while (originalLicence.variationOfId != null) {
      val licence = licenceRepository
        .findById(originalLicence.variationOfId!!)
        .orElseThrow { EntityNotFoundException("${originalLicence.variationOfId}") }
      when (licence) {
        is CrdLicence -> return licence
        is VariationLicence -> originalLicence = licence
      }
    }
    error("original licence not found for licence: ${variationLicence.id}")
  }

  @Transactional
  fun inactivateInProgressLicenceVersions(licences: List<EntityLicence>, reason: String? = null) {
    val licenceIds = licences.map { it.id }
    val licencesToDeactivate =
      licenceRepository.findAllByVersionOfIdInAndStatusCodeIn(licenceIds, listOf(IN_PROGRESS, SUBMITTED))
    if (licencesToDeactivate.isNotEmpty()) {
      inactivateLicences(licencesToDeactivate, reason, false)
    }
  }

  @Transactional
  fun reviewWithNoVariationRequired(licenceId: Long) {
    val licenceEntity = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }

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
        eventType = LicenceEventType.HARD_STOP_REVIEWED,
        username = staffMember?.username ?: SYSTEM_USER,
        forenames = staffMember?.firstName,
        surname = staffMember?.lastName,
        eventDescription = "Licence reviewed without being varied",
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
  fun getLicencesForApproval(prisons: List<String>?): List<LicenceSummaryApproverView> {
    if (prisons.isNullOrEmpty()) {
      return emptyList()
    }
    val licences = licenceRepository.getLicencesReadyForApproval(prisons)
      .sortedWith(compareBy(nullsLast()) { it.actualReleaseDate ?: it.conditionalReleaseDate })
    return transformToListOfLicenceSummariesForApproverView(licences)
  }
}
