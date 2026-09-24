package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.model.UnapprovedLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.NotifyService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TelemetryService

@Service
class SendNeedsApprovalReminderService(
  private val licenceRepository: LicenceRepository,
  private val notifyService: NotifyService,
  private val telemetryService: TelemetryService,
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
    log.info(
      "Found {} previously approved licences that have been edited but not re-approved by prisoners release date ",
      licences.size,
    )
    notifyService.sendUnapprovedLicenceEmail(licences)

    telemetryService.recordNotifyProbationOfUnapprovedLicencesJobEvent(licences.size)
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
