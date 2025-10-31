package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import com.microsoft.applicationinsights.TelemetryClient
import org.junit.jupiter.api.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createCrdLicence

class TelemetryServiceTest {

  private val telemetryClient = mock<TelemetryClient>()
  private val service = TelemetryService(telemetryClient)

  @Test
  fun `recordLicenceCreatedEvent tracks event with correct properties`() {
    val licence = createCrdLicence()

    service.recordLicenceCreatedEvent(licence)

    verify(telemetryClient).trackEvent(
      eq("LicenceCreated"),
      eq(
        mapOf(
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
        ),
      ),
      eq(null),
    )
  }

  @Test
  fun `recordPromptComJobEvent tracks event`() {
    val casesProcessed = 42
    service.recordPromptComJobEvent(casesProcessed)

    verify(telemetryClient).trackEvent(
      eq("PromptComJob"),
      eq(mapOf("cases" to casesProcessed.toString())),
      eq(null),
    )
  }

  @Test
  fun `recordDeactivateHdcLicencesJobEvent tracks event`() {
    val licencesDeactivated = 15
    service.recordDeactivateHdcLicencesJobEvent(licencesDeactivated)

    verify(telemetryClient).trackEvent(
      eq("DeactivateHdcLicencesJob"),
      eq(mapOf("licences" to licencesDeactivated.toString())),
      eq(null),
    )
  }
}
