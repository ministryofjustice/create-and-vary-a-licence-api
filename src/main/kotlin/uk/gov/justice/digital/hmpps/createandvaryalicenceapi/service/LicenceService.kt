package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.springframework.data.mapping.PropertyReferenceException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalConditionsRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AppointmentAddressRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AppointmentPersonRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AppointmentTimeRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.BespokeConditionRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ContactNumberRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.StatusUpdateRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.UpdateAdditionalConditionDataRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.CreateLicenceRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateReasonForVariationRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateSpoDiscussionRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateVloDiscussionRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionUploadDetailRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.BespokeConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.CommunityOffenderManagerRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceHistoryRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceQueryObject
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StandardConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.getSort
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.toSpecification
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AuditEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.ACTIVE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.APPROVED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.INACTIVE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.IN_PROGRESS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.REJECTED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.SUBMITTED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.VARIATION_IN_PROGRESS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.VARIATION_SUBMITTED
import java.time.LocalDateTime
import javax.persistence.EntityNotFoundException
import javax.validation.ValidationException
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.BespokeCondition as EntityBespokeCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence as EntityLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.LicenceHistory as EntityLicenceHistory
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AuditEvent as ModelAuditEvent

@Service
class LicenceService(
  private val licenceRepository: LicenceRepository,
  private val communityOffenderManagerRepository: CommunityOffenderManagerRepository,
  private val standardConditionRepository: StandardConditionRepository,
  private val additionalConditionRepository: AdditionalConditionRepository,
  private val bespokeConditionRepository: BespokeConditionRepository,
  private val licenceHistoryRepository: LicenceHistoryRepository,
  private val additionalConditionUploadDetailRepository: AdditionalConditionUploadDetailRepository,
  private val auditEventRepository: AuditEventRepository,
  private val notifyService: NotifyService,
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
    licence.responsibleCom = responsibleCom
    licence.createdBy = createdBy
    licence.mailingList.add(responsibleCom)
    licence.mailingList.add(createdBy)
    licence.updatedByUsername = username

    val licenceEntity = licenceRepository.saveAndFlush(licence)
    val createLicenceResponse = transformToLicenceSummary(licenceEntity)

    val entityStandardLicenceConditions = request.standardLicenceConditions.transformToEntityStandard(licenceEntity, "AP")
    val entityStandardPssConditions = request.standardPssConditions.transformToEntityStandard(licenceEntity, "PSS")
    standardConditionRepository.saveAllAndFlush(entityStandardLicenceConditions + entityStandardPssConditions)

    auditEventRepository.saveAndFlush(
      transform(
        ModelAuditEvent(
          licenceId = createLicenceResponse.licenceId,
          username = username,
          fullName = "${createdBy.firstName} ${createdBy.lastName}",
          summary = "Licence created for ${request.forename} ${request.surname}",
          detail = "ID ${licenceEntity.id} type ${licenceEntity.typeCode} status ${licenceEntity.statusCode.name} version ${licenceEntity.version}",
        )
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
  fun updateBespokeConditions(licenceId: Long, request: BespokeConditionRequest) {
    val licenceEntity = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }

    val username = SecurityContextHolder.getContext().authentication.name
    val updatedLicence = licenceEntity.copy(dateLastUpdated = LocalDateTime.now(), updatedByUsername = username)
    licenceRepository.saveAndFlush(updatedLicence)

    // Replace the bespoke conditions
    bespokeConditionRepository.deleteByLicenceId(licenceId)
    request.conditions.forEachIndexed { index, condition ->
      bespokeConditionRepository.saveAndFlush(
        EntityBespokeCondition(licence = licenceEntity, conditionSequence = index, conditionText = condition)
      )
    }
  }

  @Transactional
  fun updateAdditionalConditions(licenceId: Long, request: AdditionalConditionsRequest) {
    val licenceEntity = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }

    val username = SecurityContextHolder.getContext().authentication.name
    val additionalConditions = licenceEntity.additionalConditions.associateBy { it.conditionCode }.toMutableMap()
    val newAdditionalConditions = request.additionalConditions.transformToEntityAdditional(licenceEntity, request.conditionType)

    // Update any existing additional conditions with new values, or add the new condition if it doesn't exist.
    newAdditionalConditions.forEach {
      if (additionalConditions[it.conditionCode] != null) {
        additionalConditions[it.conditionCode]?.conditionCategory = it.conditionCategory
        additionalConditions[it.conditionCode]?.conditionText = it.conditionText
        additionalConditions[it.conditionCode]?.conditionSequence = it.conditionSequence
        additionalConditions[it.conditionCode]?.conditionType = it.conditionType
      } else {
        it.licence = licenceEntity
        additionalConditions[it.conditionCode] = it
      }
    }

    // Remove any additional conditions which exist on the licence, but were not specified in the request
    val resultAdditionalConditionsList = additionalConditions.values.filter { condition ->
      newAdditionalConditions.find { newAdditionalCondition -> newAdditionalCondition.conditionCode == condition.conditionCode } != null ||
        condition.conditionType != request.conditionType
    }

    val updatedLicence = licenceEntity.copy(
      additionalConditions = resultAdditionalConditionsList,
      dateLastUpdated = LocalDateTime.now(),
      updatedByUsername = username,
    )
    licenceRepository.saveAndFlush(updatedLicence)

    // If any removed additional conditions had a file upload associated then remove the detail row to prevent being orphaned
    val oldConditionsWithUploads = additionalConditions.values.filter { condition -> condition.additionalConditionUploadSummary.isNotEmpty() }
    oldConditionsWithUploads.forEach { oldCondition ->
      if (resultAdditionalConditionsList.find { newCondition -> newCondition.conditionCode == oldCondition.conditionCode } == null) {
        val uploadId = oldCondition.additionalConditionUploadSummary.first().uploadDetailId
        additionalConditionUploadDetailRepository.findById(uploadId).ifPresent {
          additionalConditionUploadDetailRepository.delete(it)
        }
      }
    }
  }

  @Transactional
  fun updateAdditionalConditionData(licenceId: Long, additionalConditionId: Long, request: UpdateAdditionalConditionDataRequest) {
    val licenceEntity = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }

    val additionalCondition = additionalConditionRepository
      .findById(additionalConditionId)
      .orElseThrow { EntityNotFoundException("$additionalConditionId") }

    val updatedAdditionalCondition = additionalCondition.copy(
      additionalConditionData = request.data.transformToEntityAdditionalData(additionalCondition)
    )
    additionalConditionRepository.saveAndFlush(updatedAdditionalCondition)

    val username = SecurityContextHolder.getContext().authentication.name
    val updatedLicence = licenceEntity.copy(dateLastUpdated = LocalDateTime.now(), updatedByUsername = username)
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

    licenceHistoryRepository.saveAndFlush(
      EntityLicenceHistory(
        licenceId = licenceId,
        statusCode = request.status.name,
        actionTime = LocalDateTime.now(),
        actionDescription = "Status changed to ${request.status.name}",
        actionUsername = request.username,
      )
    )

    auditStatusChange(licenceId, updatedLicence, request)

    if (request.status == APPROVED) {
      notifyApproval(licenceId, updatedLicence)
    }
  }

  private fun auditStatusChange(licenceId: Long, licenceEntity: EntityLicence, request: StatusUpdateRequest) {
    val fullName = "${licenceEntity.forename} ${licenceEntity.surname}"
    val detailText = "ID $licenceId type ${licenceEntity.typeCode} status ${licenceEntity.statusCode.name} version ${licenceEntity.version}"

    val summaryText = when (licenceEntity.statusCode) {
      APPROVED -> "Licence approved for $fullName"
      REJECTED -> "Licence rejected for $fullName"
      IN_PROGRESS -> "Licence edited for $fullName"
      ACTIVE -> "Licence set to ACTIVE for $fullName"
      INACTIVE -> "Licence set to INACTIVE for $fullName"
      VARIATION_IN_PROGRESS -> "Licence variation changed to in progress for $fullName"
      else -> "Check - licence status not accounted for in auditing"
    }

    auditEventRepository.saveAndFlush(
      transform(
        ModelAuditEvent(
          licenceId = licenceId,
          username = request.username,
          fullName = request.fullName,
          summary = summaryText,
          detail = detailText,
        )
      )
    )
  }

  private fun notifyApproval(licenceId: Long, licenceEntity: EntityLicence) {
    licenceEntity.mailingList.forEach {
      notifyService.sendLicenceApprovedEmail(
        it.email.orEmpty(),
        mapOf(
          Pair("fullName", "${licenceEntity.forename} ${licenceEntity.surname}"),
          Pair("prisonName", licenceEntity.prisonDescription.orEmpty()),
        ),
        licenceId.toString(),
      )
    }
  }

  @Transactional
  fun submitLicence(licenceId: Long) {
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

    licenceHistoryRepository.saveAndFlush(
      EntityLicenceHistory(
        licenceId = licenceId,
        statusCode = newStatus.name,
        actionTime = LocalDateTime.now(),
        actionDescription = "Status changed to ${newStatus.name}",
        actionUsername = username,
      )
    )

    val summary = if (newStatus == SUBMITTED) {
      "Licence submitted for ${updatedLicence.forename} ${updatedLicence.surname}"
    } else {
      "Licence variation submitted for ${updatedLicence.forename} ${updatedLicence.surname}"
    }

    auditEventRepository.saveAndFlush(
      transform(
        ModelAuditEvent(
          licenceId = licenceId,
          username = username,
          fullName = "${submitter.firstName} ${submitter.lastName}",
          summary = summary,
          detail = "ID $licenceId type ${updatedLicence.typeCode} status ${licenceEntity.statusCode.name} version ${updatedLicence.version}",
        )
      )
    )
  }

  fun findLicencesMatchingCriteria(licenceQueryObject: LicenceQueryObject): List<LicenceSummary> {
    try {
      val matchingLicences = licenceRepository.findAll(licenceQueryObject.toSpecification(), licenceQueryObject.getSort())
      return transformToListOfSummaries(matchingLicences)
    } catch (e: PropertyReferenceException) {
      throw ValidationException(e.message)
    }
  }

  @Transactional
  fun activateLicences(licenceIds: List<Long>) {
    val matchingLicences = licenceRepository.findAllById(licenceIds).filter { licence -> licence.statusCode == APPROVED }
    val activatedLicences = matchingLicences.map { licence -> licence.copy(statusCode = ACTIVE) }
    if (activatedLicences.isNotEmpty()) {
      licenceRepository.saveAllAndFlush(activatedLicences)

      activatedLicences.map { licence ->
        auditEventRepository.saveAndFlush(
          transform(
            ModelAuditEvent(
              licenceId = licence.id,
              username = "SYSTEM",
              fullName = "SYSTEM",
              eventType = AuditEventType.SYSTEM_EVENT,
              summary = "Licence automatically activated for ${licence.forename} ${licence.surname}",
              detail = "ID ${licence.id} type ${licence.typeCode} status ${licence.statusCode.name} version ${licence.version}",
            )
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

    val username = SecurityContextHolder.getContext().authentication.name
    val createdBy = this.communityOffenderManagerRepository.findByUsernameIgnoreCase(username)

    val licenceVariation = licenceEntity.createVariation()
    licenceVariation.createdBy = createdBy
    licenceVariation.mailingList.add(licenceVariation.responsibleCom!!)
    licenceVariation.mailingList.add(createdBy!!)

    val newLicence = licenceRepository.save(licenceVariation)
    val standardConditions = licenceEntity.standardConditions.map {
      it.copy(id = -1, licence = newLicence)
    }
    val bespokeConditions = licenceEntity.bespokeConditions.map {
      it.copy(id = -1, licence = newLicence)
    }

    standardConditionRepository.saveAll(standardConditions)
    bespokeConditionRepository.saveAll(bespokeConditions)

    val additionalConditions = licenceEntity.additionalConditions.map {
      val additionalConditionData = it.additionalConditionData.map { data ->
        data.copy(id = -1)
      }
      val additionalConditionUploadSummary = it.additionalConditionUploadSummary.map { upload ->
        upload.copy(id = -1)
      }

      it.copy(id = -1, licence = newLicence, additionalConditionData = additionalConditionData, additionalConditionUploadSummary = additionalConditionUploadSummary)
    }

    var newAdditionalConditions = additionalConditionRepository.saveAll(additionalConditions).toMutableList()

    newAdditionalConditions = newAdditionalConditions.map { condition ->
      val updatedAdditionalConditionData = condition.additionalConditionData.map {
        it.copy(additionalCondition = condition)
      }
      val updatedAdditionalConditionUploadSummary = condition.additionalConditionUploadSummary.map {
        var uploadDetail = additionalConditionUploadDetailRepository.getById(it.uploadDetailId)
        uploadDetail = uploadDetail.copy(id = -1, licenceId = newLicence.id, additionalConditionId = condition.id)
        uploadDetail = additionalConditionUploadDetailRepository.save(uploadDetail)
        it.copy(additionalCondition = condition, uploadDetailId = uploadDetail.id)
      }
      condition.copy(additionalConditionData = updatedAdditionalConditionData, additionalConditionUploadSummary = updatedAdditionalConditionUploadSummary)
    } as MutableList<AdditionalCondition>

    additionalConditionRepository.saveAll(newAdditionalConditions)

    auditEventRepository.saveAndFlush(
      transform(
        ModelAuditEvent(
          licenceId = licenceId,
          username = username,
          fullName = "${createdBy.firstName} ${createdBy.lastName}",
          summary = "Licence varied for ${newLicence.forename} ${newLicence.surname}",
          detail = "Old ID $licenceId, new ID ${newLicence.id} type ${newLicence.typeCode} status ${newLicence.statusCode.name} version ${newLicence.version}",
        )
      )
    )

    return transformToLicenceSummary(newLicence)
  }

  fun updateSpoDiscussion(licenceId: Long, spoDiscussionRequest: UpdateSpoDiscussionRequest) {
    val licenceEntity = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }

    val username = SecurityContextHolder.getContext().authentication.name

    val updatedLicenceEntity = licenceEntity.copy(spoDiscussion = spoDiscussionRequest.spoDiscussion, dateLastUpdated = LocalDateTime.now(), updatedByUsername = username)

    licenceRepository.saveAndFlush(updatedLicenceEntity)
  }

  fun updateVloDiscussion(licenceId: Long, vloDiscussionRequest: UpdateVloDiscussionRequest) {
    val licenceEntity = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }

    val username = SecurityContextHolder.getContext().authentication.name

    val updatedLicenceEntity = licenceEntity.copy(vloDiscussion = vloDiscussionRequest.vloDiscussion, dateLastUpdated = LocalDateTime.now(), updatedByUsername = username)

    licenceRepository.saveAndFlush(updatedLicenceEntity)
  }

  fun updateReasonForVariation(licenceId: Long, reasonForVariationRequest: UpdateReasonForVariationRequest) {
    val licenceEntity = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }

    val username = SecurityContextHolder.getContext().authentication.name

    val updatedLicenceEntity = licenceEntity.copy(reasonForVariation = reasonForVariationRequest.reasonForVariation, dateLastUpdated = LocalDateTime.now(), updatedByUsername = username)

    licenceRepository.saveAndFlush(updatedLicenceEntity)
  }

  @Transactional
  fun discardLicence(licenceId: Long) {
    val licenceEntity = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }

    val username = SecurityContextHolder.getContext().authentication.name
    val discardedBy = this.communityOffenderManagerRepository.findByUsernameIgnoreCase(username)

    auditEventRepository.saveAndFlush(
      transform(
        ModelAuditEvent(
          licenceId = licenceId,
          username = username,
          fullName = "${discardedBy?.firstName} ${discardedBy?.lastName}",
          summary = "Licence variation discarded for ${licenceEntity.forename} ${licenceEntity.surname}",
          detail = "ID $licenceId type ${licenceEntity.typeCode} status ${licenceEntity.statusCode.name} version ${licenceEntity.version}",
        )
      )
    )

    licenceRepository.delete(licenceEntity)
  }

  private fun offenderHasLicenceInFlight(nomsId: String): Boolean {
    val inFlight = licenceRepository.findAllByNomsIdAndStatusCodeIn(nomsId, listOf(IN_PROGRESS, SUBMITTED, APPROVED, REJECTED))
    return inFlight.isNotEmpty()
  }
}
