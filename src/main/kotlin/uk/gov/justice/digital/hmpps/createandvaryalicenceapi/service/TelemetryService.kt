package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence

@Service
class TelemetryService(
  private val telemetryClient: TelemetryClient,
) {
  fun recordLicenceCreatedEvent(licence: Licence) {
    val properties = mapOf(
      "kind" to licence.kind.name,
      "licenceId" to licence.id.toString(),
      "bookingId" to licence.bookingId.toString(),
      "nomsId" to licence.nomsId,
      "crn" to licence.crn,
      "prisonCode" to licence.prisonCode,
      "probationTeamCode" to licence.probationTeamCode,
      "probationAreaCode" to licence.probationAreaCode,
      "probationLauCode" to licence.probationLauCode,
      "probationPduCode" to licence.probationPduCode,
    )
    telemetryClient.trackEvent("LicenceCreated", properties, null)
  }

  fun recordPromptComJobEvent(casesProcessed: Int = 0) {
    telemetryClient.trackEvent("PromptComJob", mapOf("cases" to casesProcessed.toString()), null)
  }

  fun recordDeactivateHdcLicencesJobEvent(licencesDeactivated: Int = 0) {
    telemetryClient.trackEvent("DeactivateHdcLicencesJob", mapOf("licences" to licencesDeactivated.toString()), null)
  }
}
