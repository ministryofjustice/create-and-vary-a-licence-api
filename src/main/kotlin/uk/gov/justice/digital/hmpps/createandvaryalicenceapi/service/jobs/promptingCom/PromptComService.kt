package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs.promptingCom

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import java.time.Clock
import java.time.DayOfWeek.MONDAY
import java.time.LocalDate

@Service
class PromptComService(
  private val promptComListBuilder: PromptComListBuilder,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
) {

  fun runJob(clock: Clock = Clock.systemDefaultZone()): List<Com> {
    log.info("Running job")

    val (earliestReleaseDate, latestReleaseDate) = fromNowToTheNext4Weeks(clock)

    val candidates = prisonerSearchApiClient.getAllByReleaseDate(earliestReleaseDate, latestReleaseDate)

    log.info("Found {} prisoners with release dates within the next 4 weeks ", candidates.size)
    return promptComListBuilder.gatherEmails(candidates)
  }

  private fun PromptComListBuilder.gatherEmails(candidates: List<PrisonerSearchPrisoner>): List<Com> {
    val eligible = excludeIneligibleCases(candidates)
    log.info("{}/{} prisoners eligible", eligible.size, candidates.size)

    val withoutLicences = excludeInflightLicences(eligible)
    log.info("{}/{} + without licences", withoutLicences.size, eligible.size)

    val withoutHdc = excludePrisonersWithHdc(withoutLicences)
    log.info("{}/{} + without allowed HDC", withoutHdc.size, withoutLicences.size)

    val withDeliusDetails = enrichWithDeliusData(withoutHdc)
    log.info("{}/{} + with delius information", withDeliusDetails.size, withoutHdc.size)

    val casesWithComEmails = enrichWithComEmail(withDeliusDetails)
    log.info("{}/{} + with com email", casesWithComEmails.size, withDeliusDetails.size)

    val casesWithStartDates = enrichWithLicenceStartDates(casesWithComEmails)
    log.info("{}/{} + with start date", casesWithStartDates.size, casesWithComEmails.size)

    val casesNotInHardStop = excludeInHardStop(casesWithStartDates)
    log.info("{}/{} + not in hard stop", casesNotInHardStop.size, casesWithComEmails.size)

    val emails = buildEmailsToSend(casesNotInHardStop)
    log.info("{}/{} = emails", emails.size, casesWithStartDates.size)

    return emails
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)

    fun fromNowToTheNext4Weeks(clock: Clock) =
      LocalDate.now(clock).with(MONDAY) to LocalDate.now(clock).plusWeeks(4).with(MONDAY)
  }
}
