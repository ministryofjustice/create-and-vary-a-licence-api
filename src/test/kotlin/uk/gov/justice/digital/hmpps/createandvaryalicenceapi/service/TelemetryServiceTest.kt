package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CaseloadType.CaPrisonCaseload
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.caCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createCrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.CRD
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.HARD_STOP
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.IN_PROGRESS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.NOT_STARTED

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

  @Test
  fun `should track event with correct properties`() {
    // Given

    val items = listOf(
      caCase()
        .copy(licenceStatus = IN_PROGRESS, kind = HARD_STOP),
      caCase()
        .copy(licenceStatus = IN_PROGRESS, kind = HARD_STOP),
      caCase()
        .copy(licenceStatus = IN_PROGRESS, kind = CRD),
      caCase()
        .copy(licenceStatus = NOT_STARTED, kind = HARD_STOP),
      caCase()
        .copy(licenceStatus = NOT_STARTED, kind = CRD),
      caCase()
        .copy(licenceStatus = NOT_STARTED, kind = CRD),
    )

    val context = listOf("MDI", "LEI")

    val propertiesCaptor = ArgumentCaptor.captor<Map<String, String>>()

    // When
    service.recordCaseloadLoad(CaPrisonCaseload, context, items)

    // Then
    verify(telemetryClient).trackEvent(eq("CaseLoadRequest"), propertiesCaptor.capture(), isNull())

    val props = propertiesCaptor.value
    assertThat(props).containsEntry("caseloadType", "CA_PRISON")
    assertThat(props).containsEntry("context", "MDI, LEI")
    assertThat(props).containsEntry("CASES_CRD_STARTED", "1")
    assertThat(props).containsEntry("CASES_CRD_UNSTARTED", "2")
    assertThat(props).containsEntry("CASES_HARD_STOP_STARTED", "2")
    assertThat(props).containsEntry("CASES_HARD_STOP_UNSTARTED", "1")
  }
}
