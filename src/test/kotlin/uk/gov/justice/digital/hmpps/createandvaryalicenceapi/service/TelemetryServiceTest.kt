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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.TIMED_OUT

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
          "policyVersion" to licence.version,
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
  fun `recordDeactivateLicencesJobEvent tracks event`() {
    val licencesDeactivated = 126
    service.recordDeactivateLicencesJobEvent(licencesDeactivated)

    verify(telemetryClient).trackEvent(
      eq("DeactivateLicencesJob"),
      eq(mapOf("licences" to licencesDeactivated.toString())),
      eq(null),
    )
  }

  @Test
  fun `recordActivateLicencesJobEvent tracks event`() {
    val iS91Licences = 4
    val remandLicences = 29
    val standardLicences = 17
    val ineligibleLicences = 3

    service.recordActivateLicencesJobEvent(iS91Licences, remandLicences, standardLicences, ineligibleLicences)

    verify(telemetryClient).trackEvent(
      eq("ActivateLicencesJob"),
      eq(
        mapOf(
          "IS91" to iS91Licences.toString(),
          "remand" to remandLicences.toString(),
          "standard" to standardLicences.toString(),
          "ineligible" to ineligibleLicences.toString(),
        ),
      ),
      eq(null),
    )
  }

  @Test
  fun `recordExpireLicencesJobEvent tracks event`() {
    val licencesExpired = 26
    service.recordExpireLicencesJobEvent(licencesExpired)

    verify(telemetryClient).trackEvent(
      eq("ExpireLicencesJob"),
      eq(mapOf("licences" to licencesExpired.toString())),
      eq(null),
    )
  }

  @Test
  fun `recordComReviewEmailJobEvent tracks event`() {
    val licencesToReview = 52
    service.recordComReviewEmailJobEvent(licencesToReview)

    verify(telemetryClient).trackEvent(
      eq("ComReviewEmailJob"),
      eq(mapOf("licences" to licencesToReview.toString())),
      eq(null),
    )
  }

  @Test
  fun `recordMigrateStandardConditionsJobEvent tracks event`() {
    val licencesMigrated = 803
    service.recordMigrateStandardConditionsJobEvent(licencesMigrated)

    verify(telemetryClient).trackEvent(
      eq("MigrateStandardConditionsJob"),
      eq(mapOf("licences" to licencesMigrated.toString())),
      eq(null),
    )
  }

  @Test
  fun `recordNotifyProbationOfUnapprovedLicencesJobEvent tracks event`() {
    val emailsSent = 12
    service.recordNotifyProbationOfUnapprovedLicencesJobEvent(emailsSent)

    verify(telemetryClient).trackEvent(
      eq("NotifyProbationOfUnapprovedLicencesJob"),
      eq(mapOf("emailsSent" to emailsSent.toString())),
      eq(null),
    )
  }

  @Test
  fun `recordTimeOutLicenceJobEvent tracks event`() {
    val licencesTimedOut = 52
    service.recordTimeOutLicenceJobEvent(licencesTimedOut)

    verify(telemetryClient).trackEvent(
      eq("TimeOutLicencesJob"),
      eq(mapOf("licences" to licencesTimedOut.toString())),
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
        .copy(licenceStatus = TIMED_OUT, kind = HARD_STOP),
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
    assertThat(props).containsEntry("CASES_HARD_STOP_UNSTARTED", "2")
  }

  @Test
  fun `recordLicenceCreatedEvent with prisoner tracks restrictedPatient property when true`() {
    val licence = createCrdLicence()
    val prisoner = TestData.prisonerSearchResult().copy(
      prisonId = "OUT",
      status = "INACTIVE OUT",
      restrictedPatient = true,
      supportingPrisonId = "BMI",
    )

    service.recordLicenceCreatedEvent(licence, prisoner)

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
          "restrictedPatient" to "true",
          "policyVersion" to licence.version,
        ),
      ),
      eq(null),
    )
  }

  @Test
  fun `recordLicenceCreatedEvent with prisoner tracks restrictedPatient property when false`() {
    val licence = createCrdLicence()
    val prisoner = TestData.prisonerSearchResult().copy(
      restrictedPatient = false,
      status = "ACTIVE IN",
    )

    service.recordLicenceCreatedEvent(licence, prisoner)

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
          "restrictedPatient" to "false",
          "policyVersion" to licence.version,
        ),
      ),
      eq(null),
    )
  }
}
