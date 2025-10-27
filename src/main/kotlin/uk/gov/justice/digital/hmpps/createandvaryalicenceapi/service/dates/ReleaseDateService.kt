package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HardStopData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.SentenceDateHolder
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.IS91DeterminationService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.workingDays.WorkingDaysService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.CRD
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.HDC
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.PRRD
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.TimeServedConsiderations
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
  @param:Value("\${feature.toggle.timeServed.enabled:false}")
  private val isTimeServedEnabled: Boolean = false,
) {
  fun isInHardStopPeriod(licenceStartDate: LocalDate?, overrideClock: Clock? = null): Boolean {
    val now = overrideClock ?: clock
    val hardStopDate = getHardStopDate(licenceStartDate)
    val today = LocalDate.now(now)
    if (hardStopDate == null || licenceStartDate == null) {
      return false
    }

    return today >= hardStopDate && today <= licenceStartDate
  }

  fun isDueToBeReleasedInTheNextTwoWorkingDays(sentenceDateHolder: SentenceDateHolder): Boolean {
    val licenceStartDate = sentenceDateHolder.licenceStartDate ?: return false
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

  fun getHardStopDate(licenceStartDate: LocalDate?): LocalDate? = when {
    licenceStartDate == null -> null
    else -> calculateHardStopDate(licenceStartDate)
  }

  @TimeServedConsiderations("For time served licences, take it there will be special logic to use as licence start date in the future?")
  fun getLicenceStartDate(
    nomisRecord: PrisonerSearchPrisoner,
    licenceKind: LicenceKind?,
  ): LocalDate? = when (licenceKind) {
    HDC -> nomisRecord.homeDetentionCurfewActualDate
    PRRD -> nomisRecord.calculatePrrdLicenceStartDate()
    CRD -> calculateCrdLicenceStartDate(
      nomisRecord,
      iS91DeterminationService.isIS91Case(nomisRecord),
    )
    else -> null
  }

  @TimeServedConsiderations("For time served licences, take it there will be special logic to use as licence start date in the future?")
  fun getLicenceStartDates(prisoners: List<PrisonerSearchPrisoner>, nomisIdsToKinds: Map<String, LicenceKind?>): Map<String, LocalDate?> {
    val iS91BookingIds = iS91DeterminationService.getIS91AndExtraditionBookingIds(prisoners)
    return prisoners.associate {
      it.prisonerNumber to when (nomisIdsToKinds[it.prisonerNumber]) {
        HDC -> it.homeDetentionCurfewActualDate
        PRRD -> it.calculatePrrdLicenceStartDate()
        CRD -> calculateCrdLicenceStartDate(it, iS91BookingIds.contains(it.bookingId?.toLong()))
        else -> null
      }
    }
  }

  private fun isTimeServed(
    sentenceStartDate: LocalDate?,
    confirmedReleaseDate: LocalDate?,
    conditionalReleaseDate: LocalDate?,
    overrideClock: Clock? = null,
  ): Boolean {
    val clockToUse = overrideClock ?: clock
    val today = LocalDate.now(clockToUse)

    return isTimeServedEnabled &&
      sentenceStartDate == today &&
      confirmedReleaseDate == today &&
      conditionalReleaseDate == today
  }

  private fun determineHardStopKind(
    licenceStartDate: LocalDate?,
    sentenceStartDate: LocalDate?,
    confirmedReleaseDate: LocalDate?,
    conditionalReleaseDate: LocalDate?,
    overrideClock: Clock? = null,
  ): LicenceKind? {
    if (licenceStartDate == null || !isInHardStopPeriod(licenceStartDate, overrideClock)) {
      return null
    }

    return if (isTimeServed(sentenceStartDate, confirmedReleaseDate, conditionalReleaseDate, overrideClock)) {
      LicenceKind.TIME_SERVED
    } else {
      LicenceKind.HARD_STOP
    }
  }

  fun getHardStopKind(data: HardStopData, overrideClock: Clock? = null): LicenceKind? {
    return determineHardStopKind(
      data.licenceStartDate,
      data.sentenceStartDate,
      data.actualReleaseDate,
      data.conditionalReleaseDate,
      overrideClock,
    )
  }


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

  fun getHardStopWarningDate(licenceStartDate: LocalDate?): LocalDate? {
    val hardStopDate = getHardStopDate(licenceStartDate) ?: return null
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

  private fun PrisonerSearchPrisoner.calculatePrrdLicenceStartDate(): LocalDate? = when {
    postRecallReleaseDate == null -> null
    confirmedReleaseDate == null -> getLastWorkingDay(postRecallReleaseDate)
    confirmedReleaseDate.isAfter(postRecallReleaseDate) -> getLastWorkingDay(postRecallReleaseDate)
    confirmedReleaseDate.isOnOrBefore(conditionalReleaseDate) -> getLastWorkingDay(postRecallReleaseDate)
    else -> confirmedReleaseDate
  }

  private fun getLastWorkingDay(date: LocalDate?): LocalDate? = when {
    date == null -> null
    date.isNonWorkingDay() -> 1.workingDaysBefore(date)
    else -> date
  }
}
