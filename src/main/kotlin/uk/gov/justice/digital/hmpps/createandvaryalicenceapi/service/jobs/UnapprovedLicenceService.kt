package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.NotifyService

@Service
class UnapprovedLicenceService(
  private val licenceRepository: LicenceRepository,
  private val notifyService: NotifyService,
) {

  fun sendEmailsToProbationPractitioner() {
    notifyService.sendUnapprovedLicenceEmail(licenceRepository.getEditedLicencesNotReApprovedByCrd())
  }
}
