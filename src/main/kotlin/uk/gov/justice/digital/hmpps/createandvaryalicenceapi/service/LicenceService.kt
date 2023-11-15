package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.springframework.data.mapping.PropertyReferenceException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AppointmentAddressRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AppointmentPersonRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AppointmentTimeRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ContactNumberRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.StatusUpdateRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.CreateLicenceRequest
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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.CommunityOffenderManagerRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceQueryObject
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StandardConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.getSort
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.toSpecification
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AuditEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
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
  private val communityOffenderManagerRepository: CommunityOffenderManagerRepository,
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
) {

  @Transactional
  fun createLicence(request: CreateLicenceRequest): LicenceSummary {
    if (offenderHasLicenceInFlight(request.nomsId!!)) {
      throw ValidationException("A licence already exists for this person (IN_PROGRESS, SUBMITTED, APPROVED or REJECTED)")
    }

    val username = SecurityContextHolder.getContext().authentication.name

    val responsibleCom = communityOffenderManagerRepository.findByStaffIdentifier(request.responsibleComStaffId)
      ?: throw ValidationException("Staff with staffIdentifier ${request.responsibleComStaffId} not found")

    val createdBy = communityOffenderManagerRepository.findByUsernameIgnoreCase(username)
      ?: error("Staff with username $username not found")

    val licence = transform(request)

    licence.dateCreated = LocalDateTime.now()
    licence.responsibleCom = responsibleCom
    licence.createdBy = createdBy
    licence.updatedByUsername = username

    val licenceEntity = licenceRepository.saveAndFlush(licence)
    val createLicenceResponse = transformToLicenceSummary(licenceEntity)

    val entityStandardLicenceConditions =
      request.standardLicenceConditions.transformToEntityStandard(licenceEntity, "AP")
    val entityStandardPssConditions = request.standardPssConditions.transformToEntityStandard(licenceEntity, "PSS")
    standardConditionRepository.saveAllAndFlush(entityStandardLicenceConditions + entityStandardPssConditions)

    auditEventRepository.saveAndFlush(
      AuditEvent(
        licenceId = createLicenceResponse.licenceId,
        username = username,
        fullName = "${createdBy.firstName} ${createdBy.lastName}",
        summary = "Licence created for ${request.forename} ${request.surname}",
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

  @Transactional
  fun getLicenceById(licenceId: Long): Licence {
    val entityLicence = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }

    val releaseDate = entityLicence.actualReleaseDate ?: entityLicence.conditionalReleaseDate
    val isEligibleForEarlyRelease =
      releaseDate !== null && releaseDateService.isEligibleForEarlyRelease(releaseDate, true)

    val earliestReleaseDate = when {
      isEligibleForEarlyRelease -> releaseDateService.getEarliestReleaseDate(releaseDate!!, true)
      else -> releaseDate
    }

    return transform(entityLicence, earliestReleaseDate, isEligibleForEarlyRelease)
  }

  @Transactional
  fun updateAppointmentPerson(licenceId: Long, request: AppointmentPersonRequest) {
    val licenceEntity = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }

    val username = SecurityContextHolder.getContext().authentication.name
    val updatedLicence = licenceEntity.copy(
      appointmentPerson = request.appointmentPerson,
      dateLastUpdated = LocalDateTime.now(),
      updatedByUsername = username,
    )
    licenceRepository.saveAndFlush(updatedLicence)
  }

  @Transactional
  fun updateAppointmentTime(licenceId: Long, request: AppointmentTimeRequest) {
    val licenceEntity = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }

    // Update appointment time and licence contact
    val username = SecurityContextHolder.getContext().authentication.name
    val updatedLicence = licenceEntity.copy(
      appointmentTime = request.appointmentTime,
      dateLastUpdated = LocalDateTime.now(),
      updatedByUsername = username,
    )
    licenceRepository.saveAndFlush(updatedLicence)
  }

  @Transactional
  fun updateContactNumber(licenceId: Long, request: ContactNumberRequest) {
    val licenceEntity = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }

    val username = SecurityContextHolder.getContext().authentication.name

    val updatedLicence = licenceEntity.copy(
      appointmentContact = request.telephone,
      dateLastUpdated = LocalDateTime.now(),
      updatedByUsername = username,
    )

    licenceRepository.saveAndFlush(updatedLicence)
  }

  @Transactional
  fun updateAppointmentAddress(licenceId: Long, request: AppointmentAddressRequest) {
    val licenceEntity = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }

    val username = SecurityContextHolder.getContext().authentication.name

    val updatedLicence = licenceEntity.copy(
      appointmentAddress = request.appointmentAddress,
      dateLastUpdated = LocalDateTime.now(),
      updatedByUsername = username,
    )

    licenceRepository.saveAndFlush(updatedLicence)
  }

  @Transactional
  fun updateLicenceStatus(licenceId: Long, request: StatusUpdateRequest) {
    val licenceEntity = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }

    var approvedByUser = licenceEntity.approvedByUsername
    var approvedByName = licenceEntity.approvedByName
    var approvedDate = licenceEntity.approvedDate
    var submittedDate = licenceEntity.submittedDate
    val supersededDate: LocalDateTime?
    var licenceActivatedDate = licenceEntity.licenceActivatedDate

    when (request.status) {
      APPROVED -> {
        deactivatePreviousLicenceVersion(licenceEntity, request.username, request.fullName)
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

    val updatedLicence = licenceEntity.copy(
      statusCode = request.status,
      dateLastUpdated = LocalDateTime.now(),
      updatedByUsername = request.username,
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

    recordLicenceEventForStatus(licenceId, updatedLicence, request)
    auditStatusChange(updatedLicence, request.username, request.fullName)
  }

  private fun auditStatusChange(licenceEntity: EntityLicence, username: String, fullName: String?) {
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

    auditEventRepository.saveAndFlush(
      AuditEvent(
        licenceId = licenceEntity.id,
        username = username,
        fullName = fullName,
        eventType = getAuditEventType(username),
        summary = summaryText,
        detail = detailText,
      ),
    )
  }

  private fun getAuditEventType(username: String): AuditEventType {
    return if (username == "SYSTEM") {
      AuditEventType.SYSTEM_EVENT
    } else {
      AuditEventType.USER_EVENT
    }
  }

  private fun recordLicenceEventForStatus(licenceId: Long, licenceEntity: EntityLicence, request: StatusUpdateRequest) {
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

  private fun deactivatePreviousLicenceVersion(licence: EntityLicence, username: String, fullName: String?) {
    val previousVersionId = licence.versionOfId

    if (previousVersionId != null) {
      val previousLicenceVersion =
        licenceRepository.findById(previousVersionId).orElseThrow { EntityNotFoundException("$previousVersionId") }
      val updatedLicence = previousLicenceVersion.copy(
        dateLastUpdated = LocalDateTime.now(),
        updatedByUsername = username,
        statusCode = INACTIVE,
      )
      licenceRepository.saveAndFlush(updatedLicence)

      val (firstName, lastName) = splitName(fullName)
      licenceEventRepository.saveAndFlush(
        EntityLicenceEvent(
          licenceId = previousVersionId,
          eventType = LicenceEventType.SUPERSEDED,
          username = username,
          forenames = firstName,
          surname = lastName,
          eventDescription = "Licence deactivated as a newer version was approved for ${licence.forename} ${licence.surname}",
        ),
      )

      auditStatusChange(updatedLicence, username, fullName)
    }
  }

  @Transactional
  fun submitLicence(licenceId: Long, notifyRequest: List<NotifyRequest>?) {
    val licenceEntity = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }

    val username = SecurityContextHolder.getContext().authentication.name
    val submitter = communityOffenderManagerRepository.findByUsernameIgnoreCase(username)
      ?: throw ValidationException("Staff with username $username not found")

    val newStatus = if (licenceEntity.variationOfId == null) SUBMITTED else VARIATION_SUBMITTED

    val updatedLicence = licenceEntity.copy(
      statusCode = newStatus,
      submittedBy = submitter,
      updatedByUsername = username,
      submittedDate = LocalDateTime.now(),
      dateLastUpdated = LocalDateTime.now(),
    )

    licenceRepository.saveAndFlush(updatedLicence)

    val eventType = if (newStatus == SUBMITTED) LicenceEventType.SUBMITTED else LicenceEventType.VARIATION_SUBMITTED

    licenceEventRepository.saveAndFlush(
      EntityLicenceEvent(
        licenceId = licenceId,
        eventType = eventType,
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
    if (eventType === LicenceEventType.VARIATION_SUBMITTED) {
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

  fun findRecentlyApprovedLicences(
    prisonCodes: List<String>,
  ): List<LicenceSummary> {
    try {
      val releasedAfterDate = LocalDate.now().minusDays(14L)
      val recentActiveAndApprovedLicences =
        licenceRepository.getRecentlyApprovedLicences(prisonCodes, releasedAfterDate)

      // if a licence is an active variation then we want to return the original
      // licence that the variation was created from and not the variation itself
      val recentlyApprovedLicences = recentActiveAndApprovedLicences.map {
        if (it.statusCode == ACTIVE && it.variationOfId != null) {
          findOriginalLicenceForVariation(it)
        } else {
          it
        }
      }
      return transformToListOfSummaries(recentlyApprovedLicences)
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
    val activatedLicences = licences.map {
      it.copy(
        statusCode = ACTIVE,
        licenceActivatedDate = LocalDateTime.now(),
      )
    }
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
    val inActivatedLicences = licences.map { it.copy(statusCode = INACTIVE) }
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
    val matchingLicences =
      licenceRepository.findAllById(licenceIds)
    inactivateLicences(matchingLicences)
  }

  @Transactional
  fun createVariation(licenceId: Long): LicenceSummary {
    val licenceEntity = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }

    val licenceVariation = copyLicenceAndConditions(licenceEntity, VARIATION_IN_PROGRESS)
    return transformToLicenceSummary(licenceVariation)
  }

  @Transactional
  fun editLicence(licenceId: Long): LicenceSummary {
    val licenceEntity = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }

    if (licenceEntity.statusCode != APPROVED) {
      throw ValidationException("Can only edit APPROVED licences")
    }

    val inProgressVersions =
      licenceRepository.findAllByVersionOfIdInAndStatusCodeIn(listOf(licenceId), listOf(IN_PROGRESS, SUBMITTED))
    if (inProgressVersions.isNotEmpty()) {
      return transformToLicenceSummary(inProgressVersions[0])
    }

    val licenceCopy = copyLicenceAndConditions(licenceEntity, IN_PROGRESS)
    notifyReApprovalNeeded(licenceEntity)

    return transformToLicenceSummary(licenceCopy)
  }

  @Transactional
  fun updateSpoDiscussion(licenceId: Long, spoDiscussionRequest: UpdateSpoDiscussionRequest) {
    val licenceEntity = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }

    val username = SecurityContextHolder.getContext().authentication.name

    val updatedLicenceEntity = licenceEntity.copy(
      spoDiscussion = spoDiscussionRequest.spoDiscussion,
      dateLastUpdated = LocalDateTime.now(),
      updatedByUsername = username,
    )

    licenceRepository.saveAndFlush(updatedLicenceEntity)
  }

  @Transactional
  fun updateVloDiscussion(licenceId: Long, vloDiscussionRequest: UpdateVloDiscussionRequest) {
    val licenceEntity = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }

    val username = SecurityContextHolder.getContext().authentication.name

    val updatedLicenceEntity = licenceEntity.copy(
      vloDiscussion = vloDiscussionRequest.vloDiscussion,
      dateLastUpdated = LocalDateTime.now(),
      updatedByUsername = username,
    )

    licenceRepository.saveAndFlush(updatedLicenceEntity)
  }

  @Transactional
  fun updateReasonForVariation(licenceId: Long, reasonForVariationRequest: UpdateReasonForVariationRequest) {
    val licenceEntity = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }

    val username = SecurityContextHolder.getContext().authentication.name
    val createdBy = this.communityOffenderManagerRepository.findByUsernameIgnoreCase(username)

    val updatedLicenceEntity = licenceEntity.copy(dateLastUpdated = LocalDateTime.now(), updatedByUsername = username)
    licenceRepository.saveAndFlush(updatedLicenceEntity)

    licenceEventRepository.saveAndFlush(
      EntityLicenceEvent(
        licenceId = licenceId,
        eventType = LicenceEventType.VARIATION_SUBMITTED_REASON,
        username = username,
        forenames = createdBy?.firstName,
        surname = createdBy?.lastName,
        eventDescription = reasonForVariationRequest.reasonForVariation,
      ),
    )
  }

  @Transactional
  fun referLicenceVariation(licenceId: Long, referVariationRequest: ReferVariationRequest) {
    val licenceEntity = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }

    val username = SecurityContextHolder.getContext().authentication.name
    val createdBy = this.communityOffenderManagerRepository.findByUsernameIgnoreCase(username)

    val updatedLicenceEntity = licenceEntity.copy(
      statusCode = VARIATION_REJECTED,
      dateLastUpdated = LocalDateTime.now(),
      updatedByUsername = username,
    )

    licenceRepository.saveAndFlush(updatedLicenceEntity)

    licenceEventRepository.saveAndFlush(
      EntityLicenceEvent(
        licenceId = licenceId,
        eventType = LicenceEventType.VARIATION_REFERRED,
        username = username,
        forenames = createdBy?.firstName,
        surname = createdBy?.lastName,
        eventDescription = referVariationRequest.reasonForReferral,
      ),
    )

    auditEventRepository.saveAndFlush(
      AuditEvent(
        licenceId = licenceId,
        username = username,
        fullName = "${createdBy?.firstName} ${createdBy?.lastName}",
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
    val username = SecurityContextHolder.getContext().authentication.name
    val user = this.communityOffenderManagerRepository.findByUsernameIgnoreCase(username)

    val updatedLicenceEntity = licenceEntity.copy(
      statusCode = VARIATION_APPROVED,
      dateLastUpdated = LocalDateTime.now(),
      updatedByUsername = username,
      approvedByUsername = username,
      approvedDate = LocalDateTime.now(),
      approvedByName = "${user?.firstName} ${user?.lastName}",
    )

    licenceRepository.saveAndFlush(updatedLicenceEntity)

    licenceEventRepository.saveAndFlush(
      EntityLicenceEvent(
        licenceId = licenceId,
        eventType = LicenceEventType.VARIATION_APPROVED,
        username = username,
        forenames = user?.firstName,
        surname = user?.lastName,
        eventDescription = "Licence variation approved for ${updatedLicenceEntity.forename}${updatedLicenceEntity.surname}",
      ),
    )

    auditEventRepository.saveAndFlush(
      AuditEvent(
        licenceId = licenceId,
        username = username,
        fullName = "${user?.firstName} ${user?.lastName}",
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
  fun discardLicence(licenceId: Long) {
    val licenceEntity = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }

    val username = SecurityContextHolder.getContext().authentication.name
    val discardedBy = this.communityOffenderManagerRepository.findByUsernameIgnoreCase(username)

    auditEventRepository.saveAndFlush(
      AuditEvent(
        licenceId = licenceId,
        username = username,
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

    val updatedLicenceEntity = licenceEntity.copy(
      prisonCode = prisonInformationRequest.prisonCode,
      prisonDescription = prisonInformationRequest.prisonDescription,
      prisonTelephone = prisonInformationRequest.prisonTelephone,
      dateLastUpdated = LocalDateTime.now(),
      updatedByUsername = username,
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

  private fun offenderHasLicenceInFlight(nomsId: String): Boolean {
    val inFlight =
      licenceRepository.findAllByNomsIdAndStatusCodeIn(nomsId, listOf(IN_PROGRESS, SUBMITTED, APPROVED, REJECTED))
    return inFlight.isNotEmpty()
  }

  private fun copyLicenceAndConditions(licence: EntityLicence, newStatus: LicenceStatus): EntityLicence {
    require(newStatus == IN_PROGRESS || newStatus == VARIATION_IN_PROGRESS) { "newStatus must be IN_PROGRESS or VARIATION_IN_PROGRESS was $newStatus" }

    val username = SecurityContextHolder.getContext().authentication.name
    val createdBy = this.communityOffenderManagerRepository.findByUsernameIgnoreCase(username)
      ?: error("Cannot find staff with username: $username")

    val newLicenceVersion =
      licence.licenceVersion?.let { getNextLicenceVersion(it, newStatus) }

    val licenceCopy = licence.copyLicence(newStatus, newLicenceVersion)
    licenceCopy.createdBy = createdBy
    licenceCopy.version = licencePolicyService.currentPolicy().version
    if (newStatus == VARIATION_IN_PROGRESS) {
      licenceCopy.variationOfId = licence.id
    } else {
      licenceCopy.versionOfId = licence.id
    }

    val newLicence = licenceRepository.save(licenceCopy)

    val standardConditions = licence.standardConditions.map {
      it.copy(id = -1, licence = newLicence)
    }

    val bespokeConditions = licence.bespokeConditions.map {
      it.copy(id = -1, licence = newLicence)
    }

    standardConditionRepository.saveAll(standardConditions)

    val blockConditions =
      newStatus == VARIATION_IN_PROGRESS && licence.typeCode == LicenceType.AP_PSS && licence.isInPssPeriod()

    if (!blockConditions) {
      bespokeConditionRepository.saveAll(bespokeConditions)
    }

    val licenceConditions: List<AdditionalCondition> =
      if (blockConditions) {
        licence.additionalConditions.filter {
          LicenceType.valueOf(it.conditionType!!) != LicenceType.AP
        }
      } else {
        licence.additionalConditions
      }

    val additionalConditions = licenceConditions.map {
      val additionalConditionData = it.additionalConditionData.map { data ->
        data.copy(id = -1)
      }
      val additionalConditionUploadSummary = it.additionalConditionUploadSummary.map { upload ->
        upload.copy(id = -1)
      }
      it.copy(
        id = -1,
        licence = newLicence,
        additionalConditionData = additionalConditionData,
        additionalConditionUploadSummary = additionalConditionUploadSummary,
      )
    }

    var newAdditionalConditions = additionalConditionRepository.saveAll(additionalConditions).toMutableList()

    newAdditionalConditions = newAdditionalConditions.map { condition ->
      val updatedAdditionalConditionData = condition.additionalConditionData.map {
        it.copy(additionalCondition = condition)
      }

      val updatedAdditionalConditionUploadSummary = condition.additionalConditionUploadSummary.map {
        var uploadDetail = additionalConditionUploadDetailRepository.getReferenceById(it.uploadDetailId)
        uploadDetail = uploadDetail.copy(id = -1, licenceId = newLicence.id, additionalConditionId = condition.id)
        uploadDetail = additionalConditionUploadDetailRepository.save(uploadDetail)
        it.copy(additionalCondition = condition, uploadDetailId = uploadDetail.id)
      }

      condition.copy(
        additionalConditionData = updatedAdditionalConditionData,
        additionalConditionUploadSummary = updatedAdditionalConditionUploadSummary,
      )
    } as MutableList<AdditionalCondition>

    additionalConditionRepository.saveAll(newAdditionalConditions)

    val licenceEventMessage = when (newStatus) {
      VARIATION_IN_PROGRESS -> "A variation was created for ${newLicence.forename} ${newLicence.surname} from ID ${licence.id}"
      IN_PROGRESS -> "A new licence version was created for ${newLicence.forename} ${newLicence.surname} from ID ${licence.id}"
      else -> error("Invalid new licence status of $newStatus when creating a licence copy ")
    }
    licenceEventRepository.saveAndFlush(
      EntityLicenceEvent(
        licenceId = newLicence.id,
        eventType = if (newStatus == VARIATION_IN_PROGRESS) LicenceEventType.VARIATION_CREATED else LicenceEventType.VERSION_CREATED,
        username = username,
        forenames = createdBy.firstName,
        surname = createdBy.lastName,
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
        username = username,
        fullName = "${createdBy.firstName} ${createdBy.lastName}",
        summary = auditEventSummary,
        detail = "Old ID ${licence.id}, new ID ${newLicence.id} type ${newLicence.typeCode} status ${newLicence.statusCode.name} version ${newLicence.version}",
      ),
    )

    return newLicence
  }

  private fun splitName(fullName: String?): Pair<String?, String?> {
    val names = fullName?.split(" ")?.toMutableList()
    val firstName = names?.firstOrNull()
    names?.removeAt(0)
    val lastName = names?.joinToString(" ").orEmpty()
    return Pair(firstName, lastName)
  }

  private fun findOriginalLicenceForVariation(variationLicence: EntityLicence): EntityLicence {
    var originalLicence = variationLicence
    while (originalLicence.variationOfId != null) {
      originalLicence = licenceRepository
        .findById(originalLicence.variationOfId!!)
        .orElseThrow { EntityNotFoundException("${originalLicence.variationOfId}") }
    }
    return originalLicence
  }

  private fun getNextLicenceVersion(currentVersion: String, status: LicenceStatus): String {
    val (majorVersion, minorVersion) = getVersionParts(currentVersion)
    return when (status) {
      VARIATION_IN_PROGRESS -> "${majorVersion + 1}.0"
      IN_PROGRESS -> "$majorVersion.${minorVersion + 1}"
      else -> error("Can only update licence version when editing an existing version or creating a variation")
    }
  }

  private fun getVersionParts(version: String): Pair<Int, Int> {
    val parts = version.split(".")
    return parts[0].toInt() to parts[1].toInt()
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
}
