package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService

@Service
class LicenceExpiryService(
  private val licenceRepository: LicenceRepository,
  private val licenceService: LicenceService,
) {

  @Transactional
  fun expireLicences() {
    licenceService.inactivateLicences(
      licences = licenceRepository.getLicencesPassedExpiryDate(),
      reason = "Licence inactivated due to passing expiry date",
    )
  }
}
