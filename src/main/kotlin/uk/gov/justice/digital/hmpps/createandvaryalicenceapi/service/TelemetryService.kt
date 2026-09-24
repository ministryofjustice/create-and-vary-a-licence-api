package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner

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
      "policyVersion" to licence.version,
    )
    telemetryClient.trackEvent("LicenceCreated", properties, null)
  }

  fun recordLicenceCreatedEvent(licence: Licence, nomisRecord: PrisonerSearchPrisoner) {
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
      "restrictedPatient" to nomisRecord.restrictedPatient.toString(),
      "policyVersion" to licence.version,
    )
    telemetryClient.trackEvent("LicenceCreated", properties, null)
  }

  fun recordPromptComJobEvent(casesProcessed: Int = 0) {
    telemetryClient.trackEvent("PromptComJob", mapOf("cases" to casesProcessed.toString()), null)
  }

  fun recordDeactivateHdcLicencesJobEvent(licencesDeactivated: Int = 0) {
    telemetryClient.trackEvent(
      "DeactivateHdcLicencesJob",
      mapOf("licences" to licencesDeactivated.toString()),
      null,
    )
  }

  fun recordDeactivateLicencesJobEvent(licencesDeactivated: Int = 0) {
    telemetryClient.trackEvent(
      "DeactivateLicencesJob",
      mapOf("licences" to licencesDeactivated.toString()),
      null,
    )
  }

  fun recordActivateLicencesJobEvent(
    iS91Licences: Int = 0,
    remandLicences: Int = 0,
    standardLicences: Int = 0,
    ineligibleLicences: Int = 0,
  ) {
    telemetryClient.trackEvent(
      "ActivateLicencesJob",
      mapOf(
        "IS91" to iS91Licences.toString(),
        "remand" to remandLicences.toString(),
        "standard" to standardLicences.toString(),
        "ineligible" to ineligibleLicences.toString(),
      ),
      null,
    )
  }

  fun recordExpireLicencesJobEvent(licencesExpired: Int = 0) {
    telemetryClient.trackEvent(
      "ExpireLicencesJob",
      mapOf("licences" to licencesExpired.toString()),
      null,
    )
  }

  fun recordComReviewEmailJobEvent(licencesToReview: Int = 0) {
    telemetryClient.trackEvent(
      "ComReviewEmailJob",
      mapOf("licences" to licencesToReview.toString()),
      null,
    )
  }

  fun recordMigrateStandardConditionsJobEvent(licencesMigrated: Int = 0) {
    telemetryClient.trackEvent(
      "MigrateStandardConditionsJob",
      mapOf("licences" to licencesMigrated.toString()),
      null,
    )
  }

  fun recordNotifyProbationOfUnapprovedLicencesJobEvent(emailsSent: Int = 0) {
    telemetryClient.trackEvent(
      "NotifyProbationOfUnapprovedLicencesJob",
      mapOf("emailsSent" to emailsSent.toString()),
      null,
    )
  }

  fun recordDeactivateProgressionLicencesJobEvent(licencesDeactivated: Int = 0) {
    telemetryClient.trackEvent(
      "DeactivateProgressionLicencesJob",
      mapOf("licences" to licencesDeactivated.toString()),
      null,
    )
  }

  fun recordTimeOutLicenceJobEvent(licencesTimedOut: Int = 0) {
    telemetryClient.trackEvent(
      "TimeOutLicencesJob",
      mapOf("licences" to licencesTimedOut.toString()),
      null,
    )
  }

  fun <T> recordCaseloadLoad(
    caseLoadType: CaseloadType<T>,
    context: Collection<String>,
    items: List<T>,
  ) {
    val itemCounts =
      items.map {
        val kind = caseLoadType.kindExtractor(it)
        val type = if (caseLoadType.isUnstarted(it)) "UNSTARTED" else "STARTED"
        "CASES_${kind}_$type"
      }
        .groupingBy { it }
        .eachCount()
        .mapValues { it.value.toString() }

    val properties =
      mapOf("caseloadType" to caseLoadType.name, "context" to context.joinToString(", ")) + itemCounts
    telemetryClient.trackEvent("CaseLoadRequest", properties, null)
  }
}
