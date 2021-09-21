package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AppointmentPersonRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CreateLicenceRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CreateLicenceResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StandardConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.IN_PROGRESS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.REJECTED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.SUBMITTED
import javax.persistence.EntityNotFoundException
import javax.validation.ValidationException

@Service
class LicenceService(
  private val licenceRepository: LicenceRepository,
  private val standardConditionRepository: StandardConditionRepository,
) {

  @Transactional
  fun createLicence(request: CreateLicenceRequest): CreateLicenceResponse {
    if (offenderHasLicenceInFlight(request.nomsId!!)) {
      throw ValidationException("A licence already exists for this person (IN_PROGRESS, SUBMITTED or REJECTED)")
    }
    val createLicenceResponse = transformToCreateResponse(licenceRepository.saveAndFlush(transform(request)))
    val entityStandardConditions = request.standardConditions.transformToEntityStandard(createLicenceResponse.licenceId)
    standardConditionRepository.saveAllAndFlush(entityStandardConditions)
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

  private fun offenderHasLicenceInFlight(nomsId: String): Boolean {
    val inFlight = licenceRepository.findAllByNomsIdAndStatusCodeIn(nomsId, listOf(IN_PROGRESS, SUBMITTED, REJECTED))
    return inFlight.isNotEmpty()
  }
}
