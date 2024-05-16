package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService

@Service
class InactivateRecallLicencesService(
  private val licenceRepository: LicenceRepository,
  private val licenceService: LicenceService,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  fun inactivateLicences() {
    log.info("Starting runInactivateRecallLicencesJob")
    val licences = licenceRepository.getActiveAndVariedLicencesWhichAreNowRecalls()
    if (licences.isEmpty()) {
      log.info("No matching licences to inactivate were found")
      return
    }
    licenceService.inactivateLicences(
      licences,
      "Licence automatically inactivated as licence has a PRRD and is now a recall",
    )
    log.info("${licences.size} licences were automatically inactivated")
  }
}
