package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalConditionsRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AppointmentAddressRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AppointmentPersonRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AppointmentTimeRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.BespokeConditionRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ContactNumberRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CreateLicenceRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.StatusUpdateRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.UpdateAdditionalConditionDataRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.BespokeConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceHistoryRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceQueryObject
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StandardConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.toSpecification
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.ACTIVE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.APPROVED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.INACTIVE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.IN_PROGRESS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.REJECTED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.SUBMITTED
import java.time.LocalDateTime
import javax.persistence.EntityNotFoundException
import javax.validation.ValidationException
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.BespokeCondition as EntityBespokeCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.LicenceHistory as EntityLicenceHistory

@Service
class LicenceService(
  private val licenceRepository: LicenceRepository,
  private val standardConditionRepository: StandardConditionRepository,
  private val additionalConditionRepository: AdditionalConditionRepository,
  private val bespokeConditionRepository: BespokeConditionRepository,
  private val licenceHistoryRepository: LicenceHistoryRepository,
) {

  @Transactional
  fun createLicence(request: CreateLicenceRequest): LicenceSummary {
    if (offenderHasLicenceInFlight(request.nomsId!!)) {
      throw ValidationException("A licence already exists for this person (IN_PROGRESS, SUBMITTED, APPROVED or REJECTED)")
    }
    val createLicenceResponse = transformToLicenceSummary(licenceRepository.saveAndFlush(transform(request)))
    val entityStandardLicenceConditions = request.standardLicenceConditions.transformToEntityStandard(createLicenceResponse.licenceId, "AP")
    val entityStandardPssConditions = request.standardPssConditions.transformToEntityStandard(createLicenceResponse.licenceId, "PSS")
    standardConditionRepository.saveAllAndFlush(entityStandardLicenceConditions + entityStandardPssConditions)
    return createLicenceResponse
  }

  fun getLicenceById(licenceId: Long): Licence {
    val entityLicence = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }
    return transform(entityLicence)
  }

  fun updateAppointmentPerson(licenceId: Long, request: AppointmentPersonRequest) {
    val licenceEntity = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }
    val updatedLicence = licenceEntity.copy(appointmentPerson = request.appointmentPerson)
    licenceRepository.saveAndFlush(updatedLicence)
  }

  fun updateAppointmentTime(licenceId: Long, request: AppointmentTimeRequest) {
    val licenceEntity = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }
    val updatedLicence = licenceEntity.copy(appointmentTime = request.appointmentTime)
    licenceRepository.saveAndFlush(updatedLicence)
  }

  fun updateContactNumber(licenceId: Long, request: ContactNumberRequest) {
    val licenceEntity = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }
    val updatedLicence = licenceEntity.copy(comTelephone = request.comTelephone)
    licenceRepository.saveAndFlush(updatedLicence)
  }

  fun updateAppointmentAddress(licenceId: Long, request: AppointmentAddressRequest) {
    val licenceEntity = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }

    val updatedLicence = licenceEntity.copy(appointmentAddress = request.appointmentAddress)
    licenceRepository.saveAndFlush(updatedLicence)
  }

  @Transactional
  fun updateBespokeConditions(licenceId: Long, request: BespokeConditionRequest) {
    licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }

    bespokeConditionRepository.deleteByLicenceId(licenceId)

    request.conditions.forEachIndexed { index, condition ->
      bespokeConditionRepository.saveAndFlush(
        EntityBespokeCondition(licenceId = licenceId, conditionSequence = index, conditionText = condition)
      )
    }
  }

  fun updateAdditionalConditions(licenceId: Long, request: AdditionalConditionsRequest) {
    val licenceEntity = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }

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

    val updatedLicence = licenceEntity.copy(additionalConditions = resultAdditionalConditionsList)

    licenceRepository.saveAndFlush(updatedLicence)
  }

  fun updateAdditionalConditionData(licenceId: Long, additionalConditionId: Long, request: UpdateAdditionalConditionDataRequest) {
    licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }

    val additionalCondition = additionalConditionRepository
      .findById(additionalConditionId)
      .orElseThrow { EntityNotFoundException("$additionalConditionId") }

    val updatedAdditionalCondition = additionalCondition.copy(additionalConditionData = request.data.transformToEntityAdditionalData(additionalCondition))

    additionalConditionRepository.saveAndFlush(updatedAdditionalCondition)
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
  }

  fun findLicencesByStaffIdAndStatuses(staffId: Long, statuses: List<LicenceStatus>?): List<LicenceSummary> {
    val licences = if (!statuses.isNullOrEmpty()) {
      licenceRepository.findAllByComStaffIdAndStatusCodeIn(staffId, statuses)
    } else {
      licenceRepository.findAllByComStaffId(staffId)
    }
    return transformToListOfSummaries(licences)
  }

  fun findLicencesForApprovalByPrisonCaseload(prisonCaseload: List<String>?): List<LicenceSummary> {
    val licences = if (!prisonCaseload.isNullOrEmpty()) {
      licenceRepository.findAllByStatusCodeAndPrisonCodeIn(SUBMITTED, prisonCaseload)
    } else {
      licenceRepository.findAllByStatusCode(SUBMITTED)
    }
    return transformToListOfSummaries(licences)
  }

  fun findLicencesMatchingCriteria(licenceQueryObject: LicenceQueryObject): List<LicenceSummary> {
    val matchingLicences = licenceRepository.findAll(licenceQueryObject.toSpecification())
    return transformToListOfSummaries(matchingLicences)
  }

  fun activateLicences(licenceIds: List<Long>) {
    val matchingLicences = licenceRepository.findAllById(licenceIds).filter { licence -> licence.statusCode == APPROVED }
    val activatedLicences = matchingLicences.map { licence -> licence.copy(statusCode = ACTIVE) }
    if (activatedLicences.isNotEmpty()) {
      licenceRepository.saveAllAndFlush(activatedLicences)
    }
  }

  private fun offenderHasLicenceInFlight(nomsId: String): Boolean {
    val inFlight = licenceRepository.findAllByNomsIdAndStatusCodeIn(nomsId, listOf(IN_PROGRESS, SUBMITTED, APPROVED, REJECTED))
    return inFlight.isNotEmpty()
  }
}
