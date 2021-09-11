package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CreateLicenceRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CreateLicenceResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.REJECTED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.SUBMITTED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.IN_PROGRESS
import javax.persistence.EntityNotFoundException
import javax.validation.ValidationException

@Service
class LicenceService(private val licenceRepository: LicenceRepository) {

  fun createLicence(request: CreateLicenceRequest): CreateLicenceResponse {
    if (getLicencesInFlight(request.nomsId!!) > 0) {
      throw ValidationException("A licence already exists for this person (IN_PROGRESS, SUBMITTED or REJECTED)")
    }
    return transformToCreateResponse(licenceRepository.saveAndFlush(transform(request)))
  }

  fun getLicenceById(licenceId: Long): Licence {
    val entityLicence = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }
    return transform(entityLicence)
  }

  private fun getLicencesInFlight(nomsId: String): Int {
    val inFlight = licenceRepository.findAllByNomsIdAndStatusCodeIn(nomsId, listOf(IN_PROGRESS, SUBMITTED, REJECTED))
    return inFlight.size
  }
}
