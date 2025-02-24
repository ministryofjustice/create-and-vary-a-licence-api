package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs

import org.slf4j.LoggerFactory
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
    log.info("Running job to expire licences passed their expiry date")
    val licences = licenceRepository.getLicencesPassedExpiryDate()
    if (licences.isEmpty()) {
      log.info("There are no licences to expire")
      return
    }
    log.info("Found {} licences that have passed their expiry date", licences.size)
    licenceService.inactivateLicences(
      licences = licences,
      reason = "Licence inactivated due to passing expiry date",
    )
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
