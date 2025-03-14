package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.UnapprovedLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.NotifyService

@Service
class SendNeedsApprovalReminderService(
  private val licenceRepository: LicenceRepository,
  private val notifyService: NotifyService,
) {

  fun sendEmailsToProbationPractitioner() {
    log.info("Running job")
    val licences = licenceRepository.getEditedLicencesNotReApprovedByLsd().map { licence ->
      UnapprovedLicence(
        crn = licence.getCrn(),
        forename = licence.getForename(),
        surname = licence.getSurname(),
        comFirstName = licence.getComFirstName(),
        comLastName = licence.getComLastName(),
        comEmail = licence.getComEmail(),
      )
    }
    log.info("Found {} previously approved licences that have been edited but not re-approved by prisoners release date ", licences.size)
    notifyService.sendUnapprovedLicenceEmail(licences)
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
