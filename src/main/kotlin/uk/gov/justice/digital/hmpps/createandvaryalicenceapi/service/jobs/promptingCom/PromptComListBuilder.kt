package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs.promptingCom

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Case
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.EligibilityService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.HdcService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.SentenceDateHolderAdapter.toSentenceDateHolder
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.fullName
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.time.LocalDate

@Service
class PromptComListBuilder(
  private val licenceRepository: LicenceRepository,
  private val eligibilityService: EligibilityService,
  private val releaseDateService: ReleaseDateService,
  private val hdcService: HdcService,
  private val deliusApiClient: DeliusApiClient,
) {

  fun excludeIneligibleCases(candidates: List<PrisonerSearchPrisoner>): List<PrisonerSearchPrisoner> = candidates.filter(eligibilityService::isEligibleForCvl)

  fun excludePrisonersWithHdc(prisoners: List<PrisonerSearchPrisoner>): List<PrisonerSearchPrisoner> {
    val hdcStatuses = hdcService.getHdcStatus(prisoners)
    return prisoners.filterNot { hdcStatuses.isApprovedForHdc(it.bookingId?.toLong()!!) }
  }

  fun excludeInflightLicences(prisoners: List<PrisonerSearchPrisoner>): List<PrisonerSearchPrisoner> {
    val chunks = prisoners.map { it.prisonerNumber }.chunked(LICENCE_LOOKUP_BATCH_SIZE)

    val inflightLicences = chunks.map {
      licenceRepository.findBookingIdsForLicencesInState(it, LicenceStatus.IN_FLIGHT_LICENCES)
    }.flatten().mapTo(mutableSetOf()) { it.toString() }

    return prisoners.filter { it.bookingId !in inflightLicences }
  }

  fun enrichWithDeliusData(prisoners: List<PrisonerSearchPrisoner>): List<PromptCase> {
    val coms =
      deliusApiClient.getOffenderManagers(prisoners.map { it.prisonerNumber }).filter { it.case.nomisId != null }
        .associateBy { it.case.nomisId!! }

    return prisoners.mapNotNull {
      val offenderManager = coms[it.prisonerNumber]
      if (offenderManager == null) {
        return@mapNotNull null
      }
      PromptCase(
        prisoner = it,
        crn = offenderManager.case.crn,
        comStaffCode = offenderManager.code,
        comName = offenderManager.name.fullName(),
        comAllocationDate = offenderManager.allocationDate,
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

  fun CaseWithEmailAndStartDate.isNotInHardStop(): Boolean = !releaseDateService.isInHardStopPeriod(this.first.prisoner.toSentenceDateHolder(this.third).licenceStartDate)

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
