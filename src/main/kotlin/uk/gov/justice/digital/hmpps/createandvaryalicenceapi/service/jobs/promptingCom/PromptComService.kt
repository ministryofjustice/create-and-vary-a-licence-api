package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs.promptingCom

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.NotifyService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import java.time.Clock
import java.time.DayOfWeek.MONDAY
import java.time.LocalDate

@Service
class PromptComService(
  private val promptComListBuilder: PromptComListBuilder,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val notifyService: NotifyService,
  private val telemetryClient: TelemetryClient,
) {

  @Async
  fun runJob(clock: Clock = Clock.systemDefaultZone()) {
    log.info("Running job")

    val cases = getCases(clock)

    notifyService.sendInitialLicenceCreateEmails(cases)
    telemetryClient.trackEvent("PromptComJob", mapOf("cases" to cases.size.toString()), null)
  }

  fun getCases(clock: Clock = Clock.systemDefaultZone()): List<PromptComNotification> {
    log.info("Getting cases")

    val (earliestReleaseDate, latestReleaseDate) = fromNowToTheNext4Weeks(clock)
    log.info("Gathering prisoners with release dates between {} and {}", earliestReleaseDate, latestReleaseDate)

    val candidates = prisonerSearchApiClient.getAllByReleaseDate(earliestReleaseDate, latestReleaseDate)

    log.info("Found {} prisoners with release dates within the next 4 weeks ", candidates.size)
    return promptComListBuilder.gatherEmails(candidates, earliestReleaseDate, latestReleaseDate)
  }

  private fun PromptComListBuilder.gatherEmails(
    candidates: List<PrisonerSearchPrisoner>,
    earliestReleaseDate: LocalDate,
    latestReleaseDate: LocalDate,
  ): List<PromptComNotification> {
    val eligible = excludeIneligibleCases(candidates)
    log.info("{}/{} prisoners eligible", eligible.size, candidates.size)

    val withoutLicences = excludeInflightLicences(eligible)
    log.info("{}/{} + without licences", withoutLicences.size, eligible.size)

    val withoutHdc = excludePrisonersWithHdc(withoutLicences)
    log.info("{}/{} + without allowed HDC", withoutHdc.size, withoutLicences.size)

    val withDeliusDetails = enrichWithDeliusData(withoutHdc)
    log.info("{}/{} + with delius information", withDeliusDetails.size, withoutHdc.size)

    val withComEmails = enrichWithComEmail(withDeliusDetails)
    log.info("{}/{} + with com email", withComEmails.size, withDeliusDetails.size)

    val withStartDates = enrichWithLicenceStartDates(withComEmails)
    log.info("{}/{} + with start date", withStartDates.size, withComEmails.size)

    val withDateInRange = excludeOutOfRangeDates(withStartDates, earliestReleaseDate, latestReleaseDate)
    log.info("{}/{} + start date in range", withDateInRange.size, withStartDates.size)

    val casesNotInHardStop = excludeInHardStop(withDateInRange)
    log.info("{}/{} + not in hard stop", casesNotInHardStop.size, withDateInRange.size)

    val emails = buildEmailsToSend(casesNotInHardStop)
    log.info("{}/{} = emails", emails.size, casesNotInHardStop.size)

    return emails
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)

    fun fromNowToTheNext4Weeks(clock: Clock) =
      LocalDate.now(clock).with(MONDAY) to LocalDate.now(clock).plusWeeks(4).with(MONDAY)
  }
}
