package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.SentenceDateHolder
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate

val ALT_OUTCOME_CODES = listOf("IMMIGRATION_DETAINEE", "REMAND", "CONVICTED_UNSENTENCED")

@Service
class ReleaseDateService(
  private val clock: Clock,
  private val workingDaysService: WorkingDaysService,
  private val iS91DeterminationService: IS91DeterminationService,
  @Value("\${maxNumberOfWorkingDaysAllowedForEarlyRelease:3}") private val maxNumberOfWorkingDaysAllowedForEarlyRelease: Int = 3,
  @Value("\${maxNumberOfWorkingDaysToTriggerAllocationWarningEmail:5}") private val maxNumberOfWorkingDaysToTriggerAllocationWarningEmail: Int = 5,
) {
  fun isInHardStopPeriod(sentenceDateHolder: SentenceDateHolder, overrideClock: Clock? = null): Boolean {
    val now = overrideClock ?: clock
    val hardStopDate = getHardStopDate(sentenceDateHolder)
    val today = LocalDate.now(now)

    if (hardStopDate == null || sentenceDateHolder.licenceStartDate == null) {
      return false
    }

    return today >= hardStopDate && today <= sentenceDateHolder.licenceStartDate
  }

  fun isDueForEarlyRelease(sentenceDateHolder: SentenceDateHolder): Boolean {
    val actualReleaseDate = sentenceDateHolder.actualReleaseDate
    val conditionalReleaseDate = sentenceDateHolder.conditionalReleaseDate

    if (actualReleaseDate == null || conditionalReleaseDate == null) {
      return false
    }
    return actualReleaseDate < 1.workingDaysBefore(conditionalReleaseDate)
  }

  fun isDueToBeReleasedInTheNextTwoWorkingDays(sentenceDateHolder: SentenceDateHolder): Boolean {
    val actualReleaseDate = sentenceDateHolder.actualReleaseDate
    val conditionalReleaseDate = sentenceDateHolder.conditionalReleaseDate
    val earliestReleaseDate = listOfNotNull(actualReleaseDate, conditionalReleaseDate).minOrNull() ?: return false
    return LocalDate.now(clock) >= 2.workingDaysBefore(earliestReleaseDate) && LocalDate.now(clock) <= earliestReleaseDate
  }

  fun getEarliestReleaseDate(sentenceDateHolder: SentenceDateHolder): LocalDate? {
    val releaseDate = sentenceDateHolder.actualReleaseDate ?: sentenceDateHolder.conditionalReleaseDate ?: return null
    return when {
      isEligibleForEarlyRelease(sentenceDateHolder) -> getEarliestDateBefore(
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
    val releaseDate = sentenceDateHolder.actualReleaseDate ?: sentenceDateHolder.conditionalReleaseDate
    return releaseDate != null && isEligibleForEarlyRelease(releaseDate)
  }

  /** Friday is also considered as weekend */
  fun isEligibleForEarlyRelease(releaseDate: LocalDate): Boolean {
    if (releaseDate.dayOfWeek in listOf(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)) {
      return true
    }
    return workingDaysService.isNonWorkingDay(releaseDate)
  }

  fun getHardStopDate(sentenceDateHolder: SentenceDateHolder): LocalDate? {
    val actualReleaseDate = sentenceDateHolder.actualReleaseDate
    val conditionalReleaseDate = sentenceDateHolder.conditionalReleaseDate

    return when {
      conditionalReleaseDate == null -> null
      actualReleaseDate != null && isExcludedFromHardstop(actualReleaseDate, conditionalReleaseDate) -> null
      else -> calculateHardStopDate(conditionalReleaseDate)
    }
  }

  fun getLicenceStartDate(nomisRecord: PrisonerSearchPrisoner): LocalDate? {
    return if (
      ALT_OUTCOME_CODES.contains(nomisRecord.legalStatus) ||
      nomisRecord.paroleEligibilityDate != null && nomisRecord.paroleEligibilityDate.isBefore(LocalDate.now()) ||
      iS91DeterminationService.isIS91Case(nomisRecord)
    ) {
      nomisRecord.determineAltLicenceStartDate()
    } else {
      nomisRecord.determineLicenceStartDate()
    }
  }

  private fun calculateHardStopDate(conditionalReleaseDate: LocalDate): LocalDate {
    val date =
      if (conditionalReleaseDate.isNonWorkingDay()) 1.workingDaysBefore(conditionalReleaseDate) else conditionalReleaseDate
    return 2.workingDaysBefore(date)
  }

  private fun isExcludedFromHardstop(actualReleaseDate: LocalDate, conditionalReleaseDate: LocalDate): Boolean {
    if (conditionalReleaseDate.isNonWorkingDay()) {
      return actualReleaseDate != 1.workingDaysBefore(conditionalReleaseDate)
    }
    return actualReleaseDate != conditionalReleaseDate
  }

  fun getHardStopWarningDate(sentenceDateHolder: SentenceDateHolder): LocalDate? {
    val hardStopDate = getHardStopDate(sentenceDateHolder) ?: return null
    return 2.workingDaysBefore(hardStopDate)
  }

  private fun Int.workingDaysBefore(date: LocalDate) = workingDaysService.workingDaysBefore(date).take(this).last()

  private fun Int.workingDaysAfter(date: LocalDate) = workingDaysService.workingDaysAfter(date).take(this).last()

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

    val workingDayCrd = getWorkingDayCrd()

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
      getWorkingDayCrd()
    } else {
      confirmedReleaseDate
    }
  }

  private fun PrisonerSearchPrisoner.getWorkingDayCrd(): LocalDate? {
    if (conditionalReleaseDate == null) return null

    return if (conditionalReleaseDate.isNonWorkingDay()) {
      1.workingDaysBefore(conditionalReleaseDate)
    } else {
      conditionalReleaseDate
    }
  }
}
