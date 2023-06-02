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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
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
import java.lang.IllegalStateException
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
  private val prisonApiClient: PrisonApiClient,
  private val prisonerSearchApiClient: PrisonerSearchApiClient
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
      ?: throw RuntimeException("Staff with username $username not found")

    val licence = transform(request)

    licence.dateCreated = LocalDateTime.now()
    licence.responsibleCom = responsibleCom
    licence.createdBy = createdBy
    licence.mailingList.add(responsibleCom)
    licence.mailingList.add(createdBy)
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
      )
    )

    licenceEventRepository.saveAndFlush(
      EntityLicenceEvent(
        licenceId = createLicenceResponse.licenceId,
        eventType = LicenceEventType.CREATED,
        username = username,
        forenames = createdBy.firstName,
        surname = createdBy.lastName,
        eventDescription = "Licence created for ${licenceEntity.forename} ${licenceEntity.surname}",
      )
    )

    return createLicenceResponse
  }

  fun getLicenceById(licenceId: Long): Licence {
    val entityLicence = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }
    return transform(entityLicence)
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
    val supersededDate: LocalDateTime?

    when (request.status) {
      APPROVED -> {
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
    )
    licenceRepository.saveAndFlush(updatedLicence)

    // if previous status was APPROVED and the new status is IN_PROGRESS then email OMU regarding re-approval
    if (isReApproval) {
      notifyReApprovalNeeded(licenceEntity)
    }

    recordLicenceEventForStatus(licenceId, updatedLicence, request)
    auditStatusChange(licenceId, updatedLicence, request)

    // Notify approvals only
    if (request.status == APPROVED) {
      notifyApproval(licenceId, updatedLicence)
    }
  }

  private fun auditStatusChange(licenceId: Long, licenceEntity: EntityLicence, request: StatusUpdateRequest) {
    val fullName = "${licenceEntity.forename} ${licenceEntity.surname}"
    val detailText =
      "ID $licenceId type ${licenceEntity.typeCode} status ${licenceEntity.statusCode.name} version ${licenceEntity.version}"

    val summaryText = when (licenceEntity.statusCode) {
      APPROVED -> "Licence approved for $fullName"
      REJECTED -> "Licence rejected for $fullName"
      IN_PROGRESS -> "Licence edited for $fullName"
      ACTIVE -> "Licence set to ACTIVE for $fullName"
      INACTIVE -> "Licence set to INACTIVE for $fullName"
      VARIATION_IN_PROGRESS -> "Licence variation changed to in progress for $fullName"
      VARIATION_APPROVED -> "Licence variation approved for $fullName"
      VARIATION_REJECTED -> "Licence variation referred for $fullName"
      else -> "Check - licence status not accounted for in auditing"
    }

    auditEventRepository.saveAndFlush(
      AuditEvent(
        licenceId = licenceId,
        username = request.username,
        fullName = request.fullName,
        eventType = getAuditEventType(request),
        summary = summaryText,
        detail = detailText,
      )
    )
  }

  private fun getAuditEventType(request: StatusUpdateRequest): AuditEventType {
    return if (request.username == "SYSTEM") {
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

    // Break the full name up into first and last parts
    val names = request.fullName?.split(" ")?.toMutableList()
    val firstName = names?.firstOrNull()
    names?.removeAt(0)
    val lastName = names?.joinToString(" ").orEmpty()

    licenceEventRepository.saveAndFlush(
      EntityLicenceEvent(
        licenceId = licenceId,
        eventType = eventType,
        username = request.username,
        forenames = firstName,
        surname = lastName,
        eventDescription = "Licence updated to ${licenceEntity.statusCode} for ${licenceEntity.forename} ${licenceEntity.surname}",
      )
    )
  }

  private fun notifyApproval(licenceId: Long, licenceEntity: EntityLicence) {
    licenceEntity.mailingList.forEach {
      notifyService.sendLicenceApprovedEmail(
        it.email.orEmpty(),
        mapOf(
          "fullName" to "${licenceEntity.forename} ${licenceEntity.surname}",
          "prisonName" to licenceEntity.prisonDescription.orEmpty(),
        ),
        licenceId.toString(),
      )
    }
  }

  private fun notifyReApprovalNeeded(licenceEntity: EntityLicence) {
    val omuEmail = licenceEntity.prisonCode?.let { omuService.getOmuContactEmail(it)?.email }
    notifyService.sendVariationForReApprovalEmail(
      omuEmail,
      licenceEntity.forename ?: "unknown",
      licenceEntity.surname ?: "unknown",
      licenceEntity.nomsId,
      licenceEntity.conditionalReleaseDate
    )
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
      dateLastUpdated = LocalDateTime.now()
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
      )
    )

    auditEventRepository.saveAndFlush(
      AuditEvent(
        licenceId = licenceId,
        username = username,
        fullName = "${submitter.firstName} ${submitter.lastName}",
        summary = "Licence submitted for approval for ${updatedLicence.forename} ${updatedLicence.surname}",
        detail = "ID $licenceId type ${updatedLicence.typeCode} status ${licenceEntity.statusCode.name} version ${updatedLicence.version}",
      )
    )

    // Notify the head of PDU of this submitted licence variation
    if (eventType === LicenceEventType.VARIATION_SUBMITTED) {
      notifyRequest?.forEach {
        notifyService.sendVariationForApprovalEmail(
          it,
          licenceId.toString(),
          updatedLicence.forename!!,
          updatedLicence.surname!!,
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
      throw ValidationException(e.message)
    }
  }

  fun findSubmittedVariationsByRegion(probationAreaCode: String): List<LicenceSummary> {
    val matchingLicences =
      licenceRepository.findByStatusCodeAndProbationAreaCode(VARIATION_SUBMITTED, probationAreaCode)
    return transformToListOfSummaries(matchingLicences)
  }

  @Transactional
  fun licenceActivationJob(){
    val potentialLicences = licenceRepository.getApprovedLicencesOnReleaseDate()
    val licenceBookingIds = potentialLicences.map { it.bookingId as Long }
    val prisonersWithApprovedLicences = prisonerSearchApiClient.searchPrisonersByBookingIds(licenceBookingIds)
    val iS91AndExtraditionBookingIds = prisonApiClient.getIS91AndExtraditionBookingIds(licenceBookingIds)
    val nonHdcLicences = deactivateApprovedHdcLicencesAndReturnOthers(potentialLicences)
    val (iS91AndExtraditionLicences, standardLicences) =
      nonHdcLicences.partition { iS91AndExtraditionBookingIds.contains(it.bookingId) }
    val licencesToActivate = mutableListOf<EntityLicence>()
    for(licence in iS91AndExtraditionLicences){
      val releaseDate = getReleaseDateForIs91Case(licence)
      if(releaseDate != null && releaseDate <= LocalDate.now()){
        licencesToActivate.add(licence)
      }
    }
    for(licence in standardLicences){
      val prisoner: PrisonerSearchPrisoner = prisonersWithApprovedLicences.first { it.prisonerNumber == licence.nomsId }
      if(licence.actualReleaseDate != null &&
        licence.actualReleaseDate <= LocalDate.now() &&
        prisoner.status?.startsWith("INACTIVE") == true
      ){
        licencesToActivate.add(licence)
      }
    }
    if(licencesToActivate.any()) { activateLicences(licencesToActivate.map { it.id }) }
  }

  @Transactional
  fun activateLicences(licenceIds: List<Long>) {
    val matchingLicences =
      licenceRepository.findAllById(licenceIds).filter { licence -> licence.statusCode == APPROVED }
    val activatedLicences = matchingLicences.map { licence -> licence.copy(statusCode = ACTIVE) }
    if (activatedLicences.isNotEmpty()) {
      licenceRepository.saveAllAndFlush(activatedLicences)

      activatedLicences.map { licence ->
        auditEventRepository.saveAndFlush(
          AuditEvent(
            licenceId = licence.id,
            username = "SYSTEM",
            fullName = "SYSTEM",
            eventType = AuditEventType.SYSTEM_EVENT,
            summary = "Licence automatically activated for ${licence.forename} ${licence.surname}",
            detail = "ID ${licence.id} type ${licence.typeCode} status ${licence.statusCode.name} version ${licence.version}",
          )
        )

        licenceEventRepository.saveAndFlush(
          EntityLicenceEvent(
            licenceId = licence.id,
            eventType = LicenceEventType.ACTIVATED,
            username = "SYSTEM",
            forenames = "SYSTEM",
            surname = "SYSTEM",
            eventDescription = "Licence automatically activated for ${licence.forename} ${licence.surname}",
          )
        )
      }
    }
  }

  @Transactional
  fun inActivateLicences(licenceIds: List<Long>) {
    val matchingLicences =
      licenceRepository.findAllById(licenceIds).filter { licence -> licence.statusCode == APPROVED }
    val inActivatedLicences = matchingLicences.map { licence -> licence.copy(statusCode = INACTIVE) }
    if (inActivatedLicences.isNotEmpty()) {
      licenceRepository.saveAllAndFlush(inActivatedLicences)

      inActivatedLicences.map { licence ->
        auditEventRepository.saveAndFlush(
          AuditEvent(
            licenceId = licence.id,
            username = "SYSTEM",
            fullName = "SYSTEM",
            eventType = AuditEventType.SYSTEM_EVENT,
            summary = "Licence automatically inactivated for ${licence.forename} ${licence.surname}",
            detail = "ID ${licence.id} type ${licence.typeCode} status ${licence.statusCode.name} version ${licence.version}",
          )
        )

        licenceEventRepository.saveAndFlush(
          EntityLicenceEvent(
            licenceId = licence.id,
            eventType = LicenceEventType.SUPERSEDED,
            username = "SYSTEM",
            forenames = "SYSTEM",
            surname = "SYSTEM",
            eventDescription = "Licence automatically inactivated for ${licence.forename} ${licence.surname}",
          )
        )
      }
    }
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

    val licenceCopy = copyLicenceAndConditions(licenceEntity, IN_PROGRESS)
    return transformToLicenceSummary(licenceCopy)
  }

  fun updateSpoDiscussion(licenceId: Long, spoDiscussionRequest: UpdateSpoDiscussionRequest) {
    val licenceEntity = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }

    val username = SecurityContextHolder.getContext().authentication.name

    val updatedLicenceEntity = licenceEntity.copy(
      spoDiscussion = spoDiscussionRequest.spoDiscussion,
      dateLastUpdated = LocalDateTime.now(),
      updatedByUsername = username
    )

    licenceRepository.saveAndFlush(updatedLicenceEntity)
  }

  fun updateVloDiscussion(licenceId: Long, vloDiscussionRequest: UpdateVloDiscussionRequest) {
    val licenceEntity = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }

    val username = SecurityContextHolder.getContext().authentication.name

    val updatedLicenceEntity = licenceEntity.copy(
      vloDiscussion = vloDiscussionRequest.vloDiscussion,
      dateLastUpdated = LocalDateTime.now(),
      updatedByUsername = username
    )

    licenceRepository.saveAndFlush(updatedLicenceEntity)
  }

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
      )
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
      )
    )

    auditEventRepository.saveAndFlush(
      AuditEvent(
        licenceId = licenceId,
        username = username,
        fullName = "${createdBy?.firstName} ${createdBy?.lastName}",
        summary = "Licence variation rejected for ${licenceEntity.forename} ${licenceEntity.surname}",
        detail = "ID $licenceId type ${licenceEntity.typeCode} status ${updatedLicenceEntity.statusCode.name} version ${licenceEntity.version}",
      )
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
      approvedByName = "${user?.firstName} ${user?.lastName}"
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
      )
    )

    auditEventRepository.saveAndFlush(
      AuditEvent(
        licenceId = licenceId,
        username = username,
        fullName = "${user?.firstName} ${user?.lastName}",
        summary = "Licence variation approved for ${licenceEntity.forename} ${licenceEntity.surname}",
        detail = "ID $licenceId type ${licenceEntity.typeCode} status ${updatedLicenceEntity.statusCode.name} version ${licenceEntity.version}"
      )
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
      )
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
      updatedByUsername = username
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
      )
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

    val licenceCopy = licence.copyLicence(newStatus)
    licenceCopy.createdBy = createdBy
    licenceCopy.mailingList.add(licenceCopy.responsibleCom!!)
    licenceCopy.mailingList.add(createdBy!!)
    licenceCopy.version = licencePolicyService.currentPolicy().version

    if (newStatus == VARIATION_IN_PROGRESS) {
      licenceCopy.variationOfId = licence.id
    } else if (newStatus == IN_PROGRESS) {
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
    bespokeConditionRepository.saveAll(bespokeConditions)

    val additionalConditions = licence.additionalConditions.map {
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
        additionalConditionUploadSummary = additionalConditionUploadSummary
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
        additionalConditionUploadSummary = updatedAdditionalConditionUploadSummary
      )
    } as MutableList<AdditionalCondition>

    additionalConditionRepository.saveAll(newAdditionalConditions)

    val licenceEventMessage = when (newStatus) {
      VARIATION_IN_PROGRESS -> "A variation was created for ${newLicence.forename} ${newLicence.surname} from ID ${licence.id}"
      IN_PROGRESS -> "A new licence version was created for ${newLicence.forename} ${newLicence.surname} from ID ${licence.id}"
      else -> { throw IllegalStateException("Invalid new licence status of $newStatus when creating a licence copy ") }
    }
    licenceEventRepository.saveAndFlush(
      EntityLicenceEvent(
        licenceId = newLicence.id,
        eventType = LicenceEventType.VARIATION_CREATED,
        username = username,
        forenames = createdBy.firstName,
        surname = createdBy.lastName,
        eventDescription = licenceEventMessage,
      )
    )

    val auditEventSummary = when (newStatus) {
      VARIATION_IN_PROGRESS -> "Licence varied for ${newLicence.forename} ${newLicence.surname}"
      IN_PROGRESS -> "New licence version created for ${newLicence.forename} ${newLicence.surname}"
      else -> { throw IllegalStateException("Invalid new licence status of $newStatus when creating a licence copy ") }
    }
    auditEventRepository.saveAndFlush(
      AuditEvent(
        licenceId = licence.id,
        username = username,
        fullName = "${createdBy.firstName} ${createdBy.lastName}",
        summary = auditEventSummary,
        detail = "Old ID ${licence.id}, new ID ${newLicence.id} type ${newLicence.typeCode} status ${newLicence.statusCode.name} version ${newLicence.version}",
      )
    )

    return newLicence
  }
  
  private fun deactivateApprovedHdcLicencesAndReturnOthers(licences: List<EntityLicence>): List<EntityLicence>{
    val bookingIds = licences.map{ it.bookingId } as List<Long>
    val hdcStatuses = prisonApiClient.hdcStatuses(bookingIds)
    val approvedHdcBookingIds = hdcStatuses.filter { it.approvalStatus == "APPROVED" }.map { it.bookingId }
    val (licencesWithApprovedHdc, licencesToReturn) = licences.partition {
      approvedHdcBookingIds.contains(it.bookingId)
    }
    if(licencesWithApprovedHdc.any()) { inActivateLicences(licencesWithApprovedHdc.map { it.id }) }
    return licencesToReturn
  }

  private fun getReleaseDateForIs91Case(licence: EntityLicence): LocalDate? {
    if(licence.conditionalReleaseDate == null) { return null }
    // If ARD within CRD minus 4 days and CRD (inclusive), use ARD
    val releaseDate = if(licence.actualReleaseDate != null &&
      !licence.actualReleaseDate.isBefore(licence.conditionalReleaseDate.minusDays(4)) &&
      !licence.actualReleaseDate.isAfter(licence.conditionalReleaseDate)
    ){
      licence.actualReleaseDate
    } else {
      licence.conditionalReleaseDate
    }
    return releaseDate
  }
}
