package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs.promptingCom

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Case
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.EligibilityService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.SentenceDateHolderAdapter.toSentenceDateHolder
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ProbationSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.time.LocalDate

@Service
class PromptComListBuilder(
  private val licenceRepository: LicenceRepository,
  private val probationSearchApiClient: ProbationSearchApiClient,
  private val eligibilityService: EligibilityService,
  private val releaseDateService: ReleaseDateService,
  private val prisonApiClient: PrisonApiClient,
  private val deliusApiClient: DeliusApiClient,
) {

  fun excludeIneligibleCases(candidates: List<PrisonerSearchPrisoner>): List<PrisonerSearchPrisoner> = candidates.filter(eligibilityService::isEligibleForCvl)

  fun excludePrisonersWithHdc(prisoners: List<PrisonerSearchPrisoner>): List<PrisonerSearchPrisoner> {
    val bookingIds =
      prisoners.filter { it.homeDetentionCurfewEligibilityDate != null }.map { it.bookingId!!.toLong() }

    val approvedForHdc = prisonApiClient.getHdcStatuses(bookingIds).filter { it.approvalStatus == "APPROVED" }
      .mapNotNull { it.bookingId?.toString() }.toSet()

    return prisoners.filter { it.bookingId !in approvedForHdc }
  }

  fun excludeInflightLicences(prisoners: List<PrisonerSearchPrisoner>): List<PrisonerSearchPrisoner> {
    val chunks = prisoners.map { it.prisonerNumber }.chunked(LICENCE_LOOKUP_BATCH_SIZE)

    val inflightLicences = chunks.map {
      licenceRepository.findBookingIdsForLicencesInState(it, LicenceStatus.IN_FLIGHT_LICENCES)
    }.flatten().mapTo(mutableSetOf()) { it.toString() }

    return prisoners.filter { it.bookingId !in inflightLicences }
  }

  fun enrichWithDeliusData(prisoners: List<PrisonerSearchPrisoner>): List<PromptCase> {
    val probationRecords = probationSearchApiClient.searchForPeopleByNomsNumber(prisoners.map { it.prisonerNumber })
      .associateBy { it.otherIds.nomsNumber!! }

    return prisoners.mapNotNull {
      val probationRecord = probationRecords[it.prisonerNumber]
      val offenderManager = probationRecord?.offenderManagers?.find { it.active }
      if (probationRecord == null || offenderManager == null) {
        return@mapNotNull null
      }
      PromptCase(
        prisoner = it,
        crn = probationRecord.otherIds.crn,
        comStaffCode = offenderManager.staffDetail.code,
        comName = "${offenderManager.staffDetail.forenames} ${offenderManager.staffDetail.surname}",
        comAllocationDate = offenderManager.fromDate,
      )
    }
  }

  fun enrichWithComEmail(cases: List<PromptCase>): List<CaseWithEmail> {
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

  fun enrichWithLicenceStartDates(cases: List<CaseWithEmail>): List<CaseWithEmailAndStartDate> {
    val lsd = releaseDateService.getLicenceStartDates(cases.map { it.first.prisoner })
    return cases.mapNotNull { (case, email) ->
      val licenceStartDate = lsd[case.prisoner.prisonerNumber]
      if (licenceStartDate == null) {
        return@mapNotNull null
      }
      case to email to licenceStartDate
    }
  }

  fun CaseWithEmailAndStartDate.isNotInHardStop(): Boolean = !releaseDateService.isInHardStopPeriod(this.first.prisoner.toSentenceDateHolder(this.third))

  fun excludeInHardStop(cases: List<CaseWithEmailAndStartDate>) = cases.filter { it.isNotInHardStop() }

  fun excludeOutOfRangeDates(cases: List<CaseWithEmailAndStartDate>, startDate: LocalDate, endDate: LocalDate) = cases.filter { (_, _, releaseDate) -> releaseDate in startDate..endDate }

  fun buildEmailsToSend(cases: List<CaseWithEmailAndStartDate>): List<PromptComNotification> = cases.groupBy { (case, _, _) -> case.comStaffCode }.values.map { cases ->
    val (com, email, _) = cases.first()
    PromptComNotification(
      comName = com.comName,
      email = email,
      initialPromptCases = cases.map { (case, _, startDate) ->
        Case(
          crn = case.crn,
          name = case.prisoner.firstName + " " + case.prisoner.lastName,
          releaseDate = startDate,
        )
      },
    )
  }

  private companion object {
    private const val LICENCE_LOOKUP_BATCH_SIZE = 500
  }
}

typealias CaseWithEmail = Pair<PromptCase, String>
typealias CaseWithEmailAndStartDate = Triple<PromptCase, String, LocalDate>

infix fun <A, B, C> Pair<A, B>.to(that: C): Triple<A, B, C> = Triple(this.first, this.second, that)

data class PromptCase(
  val prisoner: PrisonerSearchPrisoner,
  val crn: String,
  val comStaffCode: String,
  val comName: String,
  val comAllocationDate: LocalDate?,
)
