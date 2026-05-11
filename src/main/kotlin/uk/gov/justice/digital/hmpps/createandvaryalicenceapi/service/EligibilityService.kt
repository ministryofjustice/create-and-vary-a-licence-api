package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.EligibilityAssessment
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.HdcStatuses
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.BookingSentenceAndRecallTypes
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.EligibleKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.isOnOrAfter
import java.time.Clock
import java.time.LocalDate

@Service
class EligibilityService(
  private val prisonApiClient: PrisonApiClient,
  private val releaseDateService: ReleaseDateService,
  private val clock: Clock,
  @param:Value("\${feature.toggle.hdc.enabled}") private val hdcEnabled: Boolean = false,
  @param:Value("\${feature.toggle.restrictedPatients.enabled:false}") private val restrictedPatientsEnabled: Boolean = false,
) {

  fun getEligibilityAssessment(prisoner: PrisonerSearchPrisoner, hdcStatuses: HdcStatuses): EligibilityAssessment {
    val assessments = getEligibilityAssessments(listOf(prisoner), hdcStatuses)
    return assessments.values.first()
  }

  fun getEligibilityAssessments(
    prisoners: List<PrisonerSearchPrisoner>,
    hdcStatuses: HdcStatuses,
  ): Map<String, EligibilityAssessment> {
    val nomisIdsToEligibilityAssessments = prisoners.map { prisoner ->
      if (prisoner.bookingId == null) {
        return@map prisoner.prisonerNumber to buildAssessment(
          genericIneligibilityReasons = listOf("no active booking"),
        )
      }
      val isExpectedHdcRelease = hdcStatuses.isExpectedHdcRelease(prisoner.bookingId.toLong())
      val genericIneligibilityReasons = getGenericIneligibilityReasons(prisoner)
      val crdIneligibilityReasons = getCrdIneligibilityReasons(prisoner, isExpectedHdcRelease)
      val prrdIneligibilityReasons = getPrrdIneligibilityReasons(prisoner, isExpectedHdcRelease)
      val hdcIneligibilityReasons = getHdcIneligibilityReasons(prisoner, isExpectedHdcRelease)

      return@map prisoner.prisonerNumber to buildAssessment(
        genericIneligibilityReasons,
        crdIneligibilityReasons,
        prrdIneligibilityReasons,
        hdcIneligibilityReasons,
      )
    }.toMap()

    val unidentifiableRecallsExcluded = overrideUnidentifiableRecalls(prisoners, nomisIdsToEligibilityAssessments)
    return unidentifiableRecallsExcluded
  }

  fun getGenericIneligibilityReasons(prisoner: PrisonerSearchPrisoner): List<String> {
    val eligibilityCriteria = listOf(
      !isPersonParoleEligible(prisoner) to "is eligible for parole",
      !isDead(prisoner) to "has died",
      !isOnIndeterminateSentence(prisoner) to "is on indeterminate sentence",
      hasEligiblePrisonStatus(prisoner) to "does not have eligible prison status",
      !isBreachOfTopUpSupervision(prisoner) to "is breach of top up supervision case",
      !isPssOnly(prisoner) to "PSS licences no longer supported",
    )

    return eligibilityCriteria.mapNotNull { (test, message) -> if (!test) message else null }
  }

  fun getCrdIneligibilityReasons(prisoner: PrisonerSearchPrisoner, isExpectedHdcRelease: Boolean): List<String> {
    val eligibilityCriteria = listOf(
      hasConditionalReleaseDate(prisoner) to "has no conditional release date",
      hasCrdTodayOrInTheFutureOrIsTimeServed(prisoner) to "CRD in the past and not eligible for time served",
      isEligibleIfOnAnExtendedDeterminateSentence(prisoner) to "is on non-eligible EDS",
      !isRecallCase(prisoner) to "is a recall case",
      !isExpectedHdcRelease to "is expected to be released on HDC",
    )

    return eligibilityCriteria.mapNotNull { (test, message) -> if (!test) message else null }
  }

  fun getPrrdIneligibilityReasons(prisoner: PrisonerSearchPrisoner, isExpectedHdcRelease: Boolean): List<String> {
    val eligibilityCriteria = listOf(
      hasPostRecallReleaseDate(prisoner) to "has no post recall release date",
      hasPrrdTodayOrInTheFuture(prisoner) to "post recall release date is in the past",
      !isBeingReleasedAtSled(prisoner) to "is being released at SLED",
      !isExpectedHdcRelease to "is expected to be released on HDC",
    )

    return eligibilityCriteria.mapNotNull { (test, message) -> if (!test) message else null }
  }

  fun getHdcIneligibilityReasons(prisoner: PrisonerSearchPrisoner, isExpectedHdcRelease: Boolean): List<String> {
    if (!hdcEnabled) return listOf("HDC licences not currently supported in CVL")

    val eligibilityCriteria = listOf(
      hasConditionalReleaseDate(prisoner) to "has no conditional release date",
      hasHomeDetentionCurfewActualDate(prisoner) to "has no home detention curfew actual date",
      isTenOrMoreDaysToCrd(prisoner) to "has CRD fewer than 10 days in the future",
      isExpectedHdcRelease to "is not expected to be released on HDC",
    )

    return eligibilityCriteria.mapNotNull { (test, message) -> if (!test) message else null }
  }

  // CRD-specific eligibility rules

  private fun hasCrdTodayOrInTheFutureOrIsTimeServed(prisoner: PrisonerSearchPrisoner): Boolean = prisoner.conditionalReleaseDate == null ||
    (
      dateIsTodayOrFuture(prisoner.conditionalReleaseDate) ||
        releaseDateService.isTimeServed(
          prisoner,
        )
      )

  private fun isRecallCase(prisoner: PrisonerSearchPrisoner): Boolean {
    // If a CRD but no PRRD it should NOT be treated as a recall
    if (prisoner.conditionalReleaseDate != null && prisoner.postRecallReleaseDate == null) {
      return false
    }

    if (prisoner.conditionalReleaseDate != null) {
      // If the PRRD > CRD - it should be treated as a recall otherwise it is not treated as a recall
      return prisoner.postRecallReleaseDate!!.isAfter(prisoner.conditionalReleaseDate)
    }

    // Trust the Nomis recall flag as a fallback position - the above rules should always override
    if (prisoner.recall == null) {
      log.warn("${prisoner.prisonerNumber} missing recall flag")
    }
    return prisoner.recall ?: false
  }

  private fun isEligibleIfOnAnExtendedDeterminateSentence(prisoner: PrisonerSearchPrisoner): Boolean {
    // If you don’t have a PED, you automatically pass this check as you’re not an EDS case
    if (prisoner.paroleEligibilityDate == null) {
      return true
    }

    // if ARD is not between CRD - 4 days and CRD inclusive (to account for bank holidays and weekends), not eligible
    if (prisoner.confirmedReleaseDate != null && prisoner.conditionalReleaseDate != null) {
      val dateStart = prisoner.conditionalReleaseDate.minusDays(4)
      if (prisoner.confirmedReleaseDate.isBefore(dateStart) || prisoner.confirmedReleaseDate.isAfter(prisoner.conditionalReleaseDate)) {
        return false
      }
    }

    // an APD with a PED in the past means they were a successful parole applicant on a later attempt, so not eligible
    if (prisoner.actualParoleDate != null) {
      return false
    }

    return true
  }

  // PRRD-specific eligibility rules
  private fun hasPostRecallReleaseDate(prisoner: PrisonerSearchPrisoner): Boolean = prisoner.postRecallReleaseDate != null

  private fun hasPrrdTodayOrInTheFuture(prisoner: PrisonerSearchPrisoner): Boolean = prisoner.postRecallReleaseDate == null || dateIsTodayOrFuture(prisoner.postRecallReleaseDate)

  private fun isBeingReleasedAtSled(prisoner: PrisonerSearchPrisoner): Boolean = when {
    prisoner.postRecallReleaseDate == null -> false
    prisoner.licenceExpiryDate == null -> false

    else -> {
      val releaseDate = releaseDateService.calculatePrrdLicenceStartDate(prisoner)
      releaseDateService.isReleaseAtLed(releaseDate, prisoner.licenceExpiryDate)
    }
  }

  // HDC-specific eligibility rules
  private fun hasHomeDetentionCurfewActualDate(prisoner: PrisonerSearchPrisoner): Boolean = prisoner.homeDetentionCurfewActualDate != null

  private fun isTenOrMoreDaysToCrd(prisoner: PrisonerSearchPrisoner): Boolean = prisoner.conditionalReleaseDate == null ||
    prisoner.conditionalReleaseDate.isOnOrAfter(
      LocalDate.now(clock).plusDays(MINIMUM_HDC_WINDOW_DAYS),
    )

  // Shared eligibility rules
  private fun hasConditionalReleaseDate(prisoner: PrisonerSearchPrisoner): Boolean = prisoner.conditionalReleaseDate != null

  private fun isPersonParoleEligible(prisoner: PrisonerSearchPrisoner): Boolean {
    if (prisoner.paroleEligibilityDate != null) {
      if (prisoner.paroleEligibilityDate.isAfter(LocalDate.now(clock))) {
        return true
      }
    }
    return false
  }

  private fun isPssOnly(prisoner: PrisonerSearchPrisoner): Boolean = prisoner.licenceExpiryDate == null && prisoner.topupSupervisionExpiryDate != null

  private fun isDead(prisoner: PrisonerSearchPrisoner): Boolean = prisoner.legalStatus == "DEAD"

  private fun isOnIndeterminateSentence(prisoner: PrisonerSearchPrisoner): Boolean {
    if (prisoner.indeterminateSentence == null) {
      log.warn("${prisoner.prisonerNumber} missing indeterminateSentence")
    }
    return prisoner.indeterminateSentence ?: false
  }

  private fun hasEligiblePrisonStatus(prisoner: PrisonerSearchPrisoner): Boolean {
    val isRestrictedPatient = restrictedPatientsEnabled && prisoner.isRestrictedPatient()
    val isEligibleStatus = prisoner.status?.let { it.startsWith("ACTIVE") || it == "INACTIVE TRN" } ?: false
    return isRestrictedPatient || isEligibleStatus
  }

  private fun isBreachOfTopUpSupervision(prisoner: PrisonerSearchPrisoner): Boolean = prisoner.imprisonmentStatus == "BOTUS"

  private fun dateIsTodayOrFuture(date: LocalDate?): Boolean {
    if (date == null) return false
    return date.isEqual(LocalDate.now(clock)) || date.isAfter(LocalDate.now(clock))
  }

  // Miscellaneous functions
  private fun overrideUnidentifiableRecalls(
    prisoners: List<PrisonerSearchPrisoner>,
    nomisIdsToEligibilityAssessments: Map<String, EligibilityAssessment>,
  ): Map<String, EligibilityAssessment> {
    val nonRecallCases = mutableMapOf<String, EligibilityAssessment>()
    val recallCases = mutableMapOf<String, EligibilityAssessment>()

    nomisIdsToEligibilityAssessments.forEach { (nomisId, eligibilityAssessment) ->
      if (eligibilityAssessment.eligibleKind?.isRecall() == true) {
        recallCases[nomisId] = eligibilityAssessment
      } else {
        nonRecallCases[nomisId] = eligibilityAssessment
      }
    }

    val overriddenRecallCases = overrideEligibilityForUnidentifiableRecalls(prisoners, recallCases)

    return nonRecallCases + overriddenRecallCases
  }

  private fun overrideEligibilityForUnidentifiableRecalls(
    prisoners: List<PrisonerSearchPrisoner>,
    recallCases: Map<String, EligibilityAssessment>,
  ): Map<String, EligibilityAssessment> {
    val nomisIdsToBookingIds = recallCases.keys.associate { nomisId ->
      val prisoner = prisoners.first { it.prisonerNumber == nomisId }
      return@associate nomisId to prisoner.bookingId!!.toLong()
    }
    val bookingsSentenceAndRecallTypes = prisonApiClient.getSentenceAndRecallTypes(nomisIdsToBookingIds.values.toList())
    return recallCases.map { (nomisId, eligibilityAssessment) ->
      val bookingId = nomisIdsToBookingIds[nomisId]!!
      val case = bookingsSentenceAndRecallTypes.firstOrNull { it.bookingId == bookingId }
      when {
        case == null -> {
          nomisId to buildAssessment(
            genericIneligibilityReasons = eligibilityAssessment.genericIneligibilityReasons,
            crdIneligibilityReasons = eligibilityAssessment.crdIneligibilityReasons,
            prrdIneligibilityReasons = eligibilityAssessment.prrdIneligibilityReasons + "does not have any active sentences",
            hdcIneligibilityReasons = eligibilityAssessment.hdcIneligibilityReasons,
          )
        }

        case.isStandardRecall() || case.isFixedTermRecall() -> nomisId to buildAssessment(
          genericIneligibilityReasons = eligibilityAssessment.genericIneligibilityReasons,
          crdIneligibilityReasons = eligibilityAssessment.crdIneligibilityReasons,
          prrdIneligibilityReasons = eligibilityAssessment.prrdIneligibilityReasons,
          hdcIneligibilityReasons = eligibilityAssessment.hdcIneligibilityReasons,
          case = case,
        )

        else -> {
          nomisId to buildAssessment(
            genericIneligibilityReasons = eligibilityAssessment.genericIneligibilityReasons,
            crdIneligibilityReasons = eligibilityAssessment.crdIneligibilityReasons,
            prrdIneligibilityReasons = eligibilityAssessment.prrdIneligibilityReasons + "is on an unidentified non-fixed term recall",
            hdcIneligibilityReasons = eligibilityAssessment.hdcIneligibilityReasons,
          )
        }
      }
    }.toMap()
  }

  private fun BookingSentenceAndRecallTypes?.isStandardRecall(): Boolean = this?.sentenceTypeRecallTypes?.any { it.recallType.isStandardRecall } == true

  private fun BookingSentenceAndRecallTypes?.isFixedTermRecall(): Boolean = this?.sentenceTypeRecallTypes?.any { it.recallType.isFixedTermRecall } == true

  private fun buildAssessment(
    genericIneligibilityReasons: List<String> = emptyList(),
    crdIneligibilityReasons: List<String> = emptyList(),
    prrdIneligibilityReasons: List<String> = emptyList(),
    hdcIneligibilityReasons: List<String> = emptyList(),
    case: BookingSentenceAndRecallTypes? = null,
  ): EligibilityAssessment {
    val isEligible = genericIneligibilityReasons.isEmpty() &&
      (crdIneligibilityReasons.isEmpty() || prrdIneligibilityReasons.isEmpty() || hdcIneligibilityReasons.isEmpty())

    val eligibleKind = when {
      !isEligible -> null
      crdIneligibilityReasons.isEmpty() -> EligibleKind.CRD
      hdcIneligibilityReasons.isEmpty() -> EligibleKind.HDC
      prrdIneligibilityReasons.isEmpty() -> if (case.isStandardRecall()) EligibleKind.STANDARD else EligibleKind.FIXED_TERM
      else -> null
    }

    return EligibilityAssessment(
      genericIneligibilityReasons = genericIneligibilityReasons,
      crdIneligibilityReasons = crdIneligibilityReasons,
      prrdIneligibilityReasons = prrdIneligibilityReasons,
      hdcIneligibilityReasons = hdcIneligibilityReasons,
      isEligible = isEligible,
      eligibleKind = eligibleKind,
      ineligibilityReasons = genericIneligibilityReasons + crdIneligibilityReasons + prrdIneligibilityReasons,
    )
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
    private const val MINIMUM_HDC_WINDOW_DAYS = 10L
  }
}
