package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util

import java.time.LocalDate

/**
 * Safely compare if two nullable dates are different.
 * Returns true if either date is different or one date is null
 */
fun LocalDate?.hasChanged(otherDate: LocalDate?): Boolean = this?.let { it != otherDate } ?: (otherDate != null)

/**
 * Safely check if date is today or in the future
 */
fun LocalDate?.isTodayOrInTheFuture() = this == LocalDate.now() || this?.isAfter(LocalDate.now()) ?: false

/**
 * Safely check if a date is before or equal to another date
 */
fun LocalDate.isOnOrBefore(other: LocalDate?): Boolean {
  if (other == null) {
    return false
  }

  return this.isBefore(other) || this == other
}

fun LocalDate?.isOnOrAfter(other: LocalDate?): Boolean {
  if (this == null || other == null) {
    return false
  }

  return this.isAfter(other) || this == other
}
