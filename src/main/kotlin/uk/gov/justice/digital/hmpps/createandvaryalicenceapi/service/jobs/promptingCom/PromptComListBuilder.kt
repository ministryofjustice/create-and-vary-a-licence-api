package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs.promptingCom

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Case
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CvlRecord
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.SentenceDateHolderAdapter.toSentenceDateHolder
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.CommunityManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.fullName
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.time.LocalDate
import kotlin.collections.List

@Service
class PromptComListBuilder(
  private val licenceRepository: LicenceRepository,
  private val releaseDateService: ReleaseDateService,
  private val deliusApiClient: DeliusApiClient,
) {

  fun excludeIneligibleCases(
    candidates: Map<PrisonerSearchPrisoner, CommunityManager>,
    cvlRecords: List<CvlRecord>,
  ): Map<PrisonerSearchPrisoner, CommunityManager> {
    return candidates.filter { (nomisRecord, _) ->
      val cvlRecord = cvlRecords.first { cvlRecord -> cvlRecord.nomisId == nomisRecord.prisonerNumber }
      return@filter cvlRecord.isEligible
    }
  }

  fun excludeInflightLicences(prisoners: List<PrisonerSearchPrisoner>): List<PrisonerSearchPrisoner> {
    val chunks = prisoners.map { it.prisonerNumber }.chunked(LICENCE_LOOKUP_BATCH_SIZE)

    val inflightLicences = chunks.map {
      licenceRepository.findBookingIdsForLicencesInState(it, LicenceStatus.IN_FLIGHT_LICENCES)
    }.flatten().mapTo(mutableSetOf()) { it.toString() }

    return prisoners.filter { it.bookingId !in inflightLicences }
  }

  fun enrichWithDeliusData(prisoners: List<PrisonerSearchPrisoner>): Map<PrisonerSearchPrisoner, CommunityManager> {
    val coms =
      deliusApiClient.getOffenderManagers(prisoners.map { it.prisonerNumber }).filter { it.case.nomisId != null }
        .associateBy { it.case.nomisId!! }

    return prisoners.mapNotNull {
      val offenderManager = coms[it.prisonerNumber]
      if (offenderManager == null) {
        return@mapNotNull null
      }
      it to offenderManager
    }.toMap()
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

  fun enrichWithLicenceStartDates(
    cases: List<CaseWithEmail>,
    cvlRecords: List<CvlRecord>,
  ): List<CaseWithEmailAndStartDate> {
    return cases.mapNotNull { (case, email) ->
      val cvlRecord = cvlRecords.find { cvlRecord -> cvlRecord.nomisId == case.prisoner.prisonerNumber }
      val licenceStartDate = cvlRecord?.licenceStartDate
      if (licenceStartDate == null) {
        return@mapNotNull null
      }
      case to email to licenceStartDate
    }
  }

  fun CaseWithEmailAndStartDate.isNotInHardStop(): Boolean = !releaseDateService.isInHardStopPeriod(this.first.prisoner.toSentenceDateHolder(this.third).licenceStartDate)

  fun excludeInHardStop(cases: List<CaseWithEmailAndStartDate>) = cases.filter { it.isNotInHardStop() }

  fun excludeOutOfRangeDates(cases: List<CaseWithEmailAndStartDate>, startDate: LocalDate, endDate: LocalDate) = cases.filter { (_, _, releaseDate) -> releaseDate in startDate..endDate }

  fun buildEmailsToSend(
    cases: List<CaseWithEmailAndStartDate>,
    cvlRecords: List<CvlRecord>,
  ): List<PromptComNotification> = cases.groupBy { (case, _, _) -> case.comStaffCode }.values.map { cases ->
    val (com, email, _) = cases.first()
    PromptComNotification(
      comName = com.comName,
      email = email,
      initialPromptCases = cases.map { (case, _, startDate) ->
        val cvlRecord = cvlRecords.first { cvlRecord -> cvlRecord.nomisId == case.prisoner.prisonerNumber }
        Case(
          crn = case.crn,
          name = case.prisoner.firstName + " " + case.prisoner.lastName,
          licenceStartDate = startDate,
          kind = cvlRecord.eligibleKind,
        )
      }.sortedBy { it.licenceStartDate },
    )
  }

  fun transformToPromptCases(
    cases: Map<PrisonerSearchPrisoner, CommunityManager>,
  ): List<PromptCase> = cases.map { (nomisRecord, offenderManager) ->
    PromptCase(
      prisoner = nomisRecord,
      crn = offenderManager.case.crn,
      comStaffCode = offenderManager.code,
      comName = offenderManager.name.fullName(),
      comAllocationDate = offenderManager.allocationDate,
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
