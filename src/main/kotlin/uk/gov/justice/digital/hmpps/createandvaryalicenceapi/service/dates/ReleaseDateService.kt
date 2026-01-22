package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.SentenceDateHolder
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.IS91DeterminationService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.workingDays.WorkingDaysService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.CRD
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.HDC
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.PRRD
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.TIME_SERVED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.isOnOrBefore
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate

val ALT_OUTCOME_CODES = listOf("IMMIGRATION_DETAINEE", "REMAND", "CONVICTED_UNSENTENCED")

@Service
class ReleaseDateService(
  private val clock: Clock,
  private val workingDaysService: WorkingDaysService,
  private val iS91DeterminationService: IS91DeterminationService,
  @param:Value("\${maxNumberOfWorkingDaysAllowedForEarlyRelease:3}") private val maxNumberOfWorkingDaysAllowedForEarlyRelease: Int = 3,
  @param:Value("\${maxNumberOfWorkingDaysToTriggerAllocationWarningEmail:5}") private val maxNumberOfWorkingDaysToTriggerAllocationWarningEmail: Int = 5,
  @param:Value("\${timeserved.max.days.crd.before.today:14}") private val maxNumberOfDaysBeforeTodayForCrdTimeserved: Long = 14,
  @param:Value("\${feature.toggle.timeServed.enabled:false}")
  private val isTimeServedEnabled: Boolean = false,
  @param:Value("\${feature.toggle.timeServed.prisons}")
  private val timeServedEnabledPrisons: List<String>? = emptyList(),
) {
  fun isInHardStopPeriod(
    licenceStartDate: LocalDate?,
    kind: LicenceKind? = null,
    overrideClock: Clock? = null,
  ): Boolean {
    val now = overrideClock ?: clock
    val hardStopDate = getHardStopDate(licenceStartDate, kind)
    val today = LocalDate.now(now)
    if (hardStopDate == null || licenceStartDate == null) {
      return false
    }

    return today >= hardStopDate && today <= licenceStartDate
  }

  fun isDueToBeReleasedInTheNextTwoWorkingDays(licenceStartDate: LocalDate?): Boolean {
    if (licenceStartDate == null) return false
    return LocalDate.now(clock) >= 2.workingDaysBefore(licenceStartDate) && LocalDate.now(clock) <= licenceStartDate
  }

  fun getEarliestReleaseDate(sentenceDateHolder: SentenceDateHolder): LocalDate? {
    val releaseDate = sentenceDateHolder.licenceStartDate ?: return null
    return when {
      releaseDate == sentenceDateHolder.homeDetentionCurfewActualDate -> releaseDate

      isEligibleForEarlyRelease(releaseDate) -> getEarliestDateBefore(
        maxNumberOfWorkingDaysAllowedForEarlyRelease,
        releaseDate,
      )

      else -> releaseDate
    }
  }

  fun isLateAllocationWarningRequired(releaseDate: LocalDate?): Boolean {
    if (releaseDate === null || releaseDate.isBefore(LocalDate.now(clock))) return false
    val warningThreshold = getEarliestDateBefore(
      maxNumberOfWorkingDaysToTriggerAllocationWarningEmail,
      releaseDate,
    )
    return LocalDate.now(clock) >= warningThreshold
  }

  fun isEligibleForEarlyRelease(sentenceDateHolder: SentenceDateHolder): Boolean {
    val releaseDate = sentenceDateHolder.licenceStartDate

    // Temporary check to prevent HDC licences being marked as early release until we can get kind before creation
    if (releaseDate == sentenceDateHolder.homeDetentionCurfewActualDate) return false

    return releaseDate != null && isEligibleForEarlyRelease(releaseDate)
  }

  /** Friday is also considered as weekend */
  fun isEligibleForEarlyRelease(releaseDate: LocalDate): Boolean {
    if (releaseDate.dayOfWeek in listOf(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)) {
      return true
    }
    return workingDaysService.isNonWorkingDay(releaseDate)
  }

  fun getHardStopDate(licenceStartDate: LocalDate?, kind: LicenceKind? = null): LocalDate? = when {
    licenceStartDate == null || kind == TIME_SERVED -> null
    else -> calculateHardStopDate(licenceStartDate)
  }

  fun getLicenceStartDate(
    nomisRecord: PrisonerSearchPrisoner,
    licenceKind: LicenceKind?,
  ): LocalDate? = getLicenceStartDates(
    listOf(nomisRecord),
    mapOf(nomisRecord.prisonerNumber to licenceKind),
  )[nomisRecord.prisonerNumber]

  fun getLicenceStartDates(
    prisoners: List<PrisonerSearchPrisoner>,
    nomisIdsToKinds: Map<String, LicenceKind?>,
  ): Map<String, LocalDate?> {
    val iS91BookingIds = iS91DeterminationService.getIS91AndExtraditionBookingIds(prisoners)
    return prisoners.associate {
      it.prisonerNumber to when (nomisIdsToKinds[it.prisonerNumber]) {
        PRRD -> calculatePrrdLicenceStartDate(it)
        CRD -> calculateCrdLicenceStartDate(it, iS91BookingIds.contains(it.bookingId?.toLong()))
        HDC -> it.homeDetentionCurfewActualDate
        else -> null
      }
    }
  }

  fun isReleaseAtLed(releaseDate: LocalDate?, licenceExpiryDate: LocalDate?): Boolean {
    if (releaseDate == null) return false

    return releaseDate == licenceExpiryDate
  }

  fun isTimeServed(prisoner: PrisonerSearchPrisoner): Boolean = isTimeServed(
    prisoner.sentenceStartDate,
    prisoner.conditionalReleaseDate,
    prisoner.prisonId,
  )

  private fun isTimeServed(
    sentenceStartDate: LocalDate?,
    conditionalReleaseDate: LocalDate?,
    prisonCode: String?,
  ): Boolean = isTimeServedEnabled &&
    timeServedEnabledPrisons?.contains(prisonCode) == true &&
    sentenceStartDate == conditionalReleaseDate &&
    conditionalReleaseDate?.isAfter(LocalDate.now(clock).minusDays(maxNumberOfDaysBeforeTodayForCrdTimeserved)) ?: false

  private fun calculateCrdLicenceStartDate(
    nomisRecord: PrisonerSearchPrisoner,
    isIs91Case: Boolean,
  ): LocalDate? = if (
    ALT_OUTCOME_CODES.contains(nomisRecord.legalStatus) ||
    isIs91Case
  ) {
    nomisRecord.determineAltLicenceStartDate()
  } else {
    nomisRecord.determineLicenceStartDate()
  }

  private fun calculateHardStopDate(licenceStartDate: LocalDate): LocalDate {
    val adjustedLsd =
      if (licenceStartDate.isNonWorkingDay()) 1.workingDaysBefore(licenceStartDate) else licenceStartDate
    return 2.workingDaysBefore(adjustedLsd)
  }

  fun getHardStopWarningDate(licenceStartDate: LocalDate?, kind: LicenceKind? = null): LocalDate? {
    val hardStopDate = getHardStopDate(licenceStartDate, kind) ?: return null
    return 2.workingDaysBefore(hardStopDate)
  }

  fun Int.workingDaysBefore(date: LocalDate) = workingDaysService.workingDaysBefore(date).take(this).last()

  private fun getEarliestDateBefore(
    days: Int,
    releaseDate: LocalDate,
  ): LocalDate = workingDaysService.workingDaysBefore(releaseDate)
    .filterNot { it.dayOfWeek == DayOfWeek.FRIDAY }
    .take(days)
    .last()

  private fun LocalDate.isNonWorkingDay() = workingDaysService.isNonWorkingDay(this)

  private fun PrisonerSearchPrisoner.determineAltLicenceStartDate(): LocalDate? {
    if (conditionalReleaseDate == null) return null

    val workingDayCrd = getLastWorkingDay(conditionalReleaseDate)

    return if (
      confirmedReleaseDate == null ||
      confirmedReleaseDate.isBefore(workingDayCrd) ||
      confirmedReleaseDate.isAfter(conditionalReleaseDate)
    ) {
      workingDayCrd
    } else {
      confirmedReleaseDate
    }
  }

  private fun PrisonerSearchPrisoner.determineLicenceStartDate(): LocalDate? {
    if (conditionalReleaseDate == null) return null

    return if (confirmedReleaseDate == null || confirmedReleaseDate.isAfter(conditionalReleaseDate)) {
      getLastWorkingDay(conditionalReleaseDate)
    } else {
      confirmedReleaseDate
    }
  }

  fun calculatePrrdLicenceStartDate(prisoner: PrisonerSearchPrisoner): LocalDate? = with(prisoner) {
    when {
      postRecallReleaseDate == null -> null
      confirmedReleaseDate == null -> getLastWorkingDay(prisoner.postRecallReleaseDate)
      confirmedReleaseDate.isAfter(prisoner.postRecallReleaseDate) -> getLastWorkingDay(prisoner.postRecallReleaseDate)
      confirmedReleaseDate.isOnOrBefore(prisoner.conditionalReleaseDate) -> getLastWorkingDay(prisoner.postRecallReleaseDate)
      else -> confirmedReleaseDate
    }
  }

  private fun getLastWorkingDay(date: LocalDate?): LocalDate? = when {
    date == null -> null
    date.isNonWorkingDay() -> 1.workingDaysBefore(date)
    else -> date
  }
}
