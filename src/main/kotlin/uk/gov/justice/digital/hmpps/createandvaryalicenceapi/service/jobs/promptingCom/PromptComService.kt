package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs.promptingCom

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.EligibilityService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.SentenceDateHolderAdapter.toSentenceDateHolder
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ProbationSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.time.Clock
import java.time.DayOfWeek.MONDAY
import java.time.LocalDate

@Service
class PromptComService(
  private val licenceRepository: LicenceRepository,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val probationSearchApiClient: ProbationSearchApiClient,
  private val eligibilityService: EligibilityService,
  private val releaseDateService: ReleaseDateService,
  private val prisonApiClient: PrisonApiClient,
  private val deliusApiClient: DeliusApiClient,
) {
  private companion object {
    val log = LoggerFactory.getLogger(this::class.java)
    private const val LICENCE_LOOKUP_BATCH_SIZE = 500
  }

  fun runJob(clock: Clock = Clock.systemDefaultZone()): List<Com> {
    log.info("Running job")

    val (earliestReleaseDate, latestReleaseDate) = fromNowToTheNext4Weeks(clock)

    val candidates = prisonerSearchApiClient.getAllByReleaseDate(earliestReleaseDate, latestReleaseDate)
    log.info("Found {} prisoners with release dates within the next 4 weeks ", candidates.size)

    val eligible = candidates.filter(eligibilityService::isEligibleForCvl)
    log.info("{}/{} prisoners eligible", eligible.size, candidates.size)

    val withoutHdc = excludePrisonersWithHdc(eligible)
    log.info("{}/{} + without allowed HDC", withoutHdc.size, eligible.size)

    val withoutLicences = excludeInflightLicences(withoutHdc)
    log.info("{}/{} + without licences", withoutLicences.size, withoutHdc.size)

    val withDeliusDetails = enrichWithDeliusData(withoutLicences)
    log.info("{}/{} + with delius information", withDeliusDetails.size, withoutLicences.size)

    val casesWithComEmails = enrichWithComEmail(withDeliusDetails)
    log.info("{}/{} + with com email", casesWithComEmails.size, withDeliusDetails.size)

    val casesWithStartDates = enrichWithLicenceStartDates(casesWithComEmails)
    log.info("{}/{} + with start date", casesWithStartDates.size, casesWithComEmails.size)

    val casesNotInHardstop = casesWithStartDates.filter { it.isNotInHardStop() }
    log.info("{}/{} + not in hard stop", casesNotInHardstop.size, casesWithComEmails.size)

    val emails = buildEmailsToSend(casesNotInHardstop)
    log.info("{}/{} = emails", emails.size, casesWithStartDates.size)

    return emails
  }

  fun fromNowToTheNext4Weeks(clock: Clock) =
    LocalDate.now(clock).with(MONDAY) to LocalDate.now(clock).plusWeeks(4).with(MONDAY)

  private fun CaseWithEmailAndStartDate.isNotInHardStop(): Boolean =
    !releaseDateService.isInHardStopPeriod(this.first.prisoner.toSentenceDateHolder(this.third))

  fun excludePrisonersWithHdc(prisoners: List<PrisonerSearchPrisoner>): List<PrisonerSearchPrisoner> {
    val bookingIds = prisoners
      .filter { it.homeDetentionCurfewEligibilityDate != null }
      .map { it.bookingId!!.toLong() }

    val approvedForHdc = prisonApiClient.getHdcStatuses(bookingIds)
      .filter { it.approvalStatus == "APPROVED" }
      .mapNotNull { it.bookingId?.toString() }
      .toSet()

    return prisoners.filter { it.bookingId !in approvedForHdc }
  }

  fun excludeInflightLicences(prisoners: List<PrisonerSearchPrisoner>): List<PrisonerSearchPrisoner> {
    val chunks = prisoners.map { it.prisonerNumber }.chunked(LICENCE_LOOKUP_BATCH_SIZE)

    val inflightLicences = chunks.map {
      licenceRepository.findBookingIdsForLicencesInState(it, LicenceStatus.IN_FLIGHT_LICENCES)
    }.flatten().mapTo(mutableSetOf()) { it.toString() }

    return prisoners.filter { it.bookingId !in inflightLicences }
  }

  private fun enrichWithDeliusData(prisoners: List<PrisonerSearchPrisoner>): List<Case> {
    val probationRecords = probationSearchApiClient.searchForPeopleByNomsNumber(prisoners.map { it.prisonerNumber })
      .associateBy { it.otherIds.nomsNumber!! }

    return prisoners.mapNotNull {
      val probationRecord = probationRecords[it.prisonerNumber]
      val offenderManager = probationRecord?.offenderManagers?.find { it.active }
      if (probationRecord == null || offenderManager == null) {
        return@mapNotNull null
      }
      Case(
        prisoner = it,
        crn = probationRecord.otherIds.crn,
        comStaffCode = offenderManager.staffDetail.code,
        comName = "${offenderManager.staffDetail.forenames} ${offenderManager.staffDetail.surname}",
        comAllocationDate = offenderManager.fromDate,
      )
    }
  }

  private fun enrichWithComEmail(cases: List<Case>): List<CaseWithEmail> {
    val crns = cases.map { it.crn }.distinct()
    val staffEmails = deliusApiClient.getStaffEmails(crns).associateBy { it.code }

    return cases.mapNotNull {
      val staffEmail = staffEmails[it.comStaffCode]
      if (staffEmail == null || staffEmail.email == null) {
        return@mapNotNull null
      }
      it to staffEmail.email
    }
  }

  private fun enrichWithLicenceStartDates(cases: List<CaseWithEmail>): List<CaseWithEmailAndStartDate> {
    val lsd = releaseDateService.getLicenceStartDates(cases.map { it.first.prisoner })
    return cases.mapNotNull { (case, email) ->
      val licenceStartDate = lsd[case.prisoner.prisonerNumber]
      if (licenceStartDate == null) {
        return@mapNotNull null
      }
      case to email to licenceStartDate
    }
  }

  private fun buildEmailsToSend(cases: List<CaseWithEmailAndStartDate>): List<Com> {
    return cases
      .groupBy { (case, _, _) -> case.comStaffCode }
      .values
      .map { cases ->
        val (com, email, _) = cases.first()
        Com(
          comName = com.comName,
          email = email,
          subjects = cases.map { (case, _, startDate) ->
            Subject(
              prisonerNumber = case.prisoner.prisonerNumber,
              crn = case.crn,
              name = case.prisoner.firstName + " " + case.prisoner.lastName,
              releaseDate = startDate,
            )
          },
        )
      }
  }
}

typealias CaseWithEmail = Pair<Case, String>
typealias CaseWithEmailAndStartDate = Triple<Case, String, LocalDate>

infix fun <A, B, C> Pair<A, B>.to(that: C): Triple<A, B, C> = Triple(this.first, this.second, that)

data class Case(
  val prisoner: PrisonerSearchPrisoner,
  val crn: String,
  val comStaffCode: String,
  val comName: String,
  val comAllocationDate: LocalDate?,
)

data class Com(
  val email: String,
  val comName: String,
  val subjects: List<Subject>,
)

data class Subject(
  val prisonerNumber: String,
  val crn: String,
  val name: String,
  val releaseDate: LocalDate,
)
