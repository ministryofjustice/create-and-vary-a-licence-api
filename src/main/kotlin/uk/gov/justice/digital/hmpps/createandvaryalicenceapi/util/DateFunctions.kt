package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util

import java.time.LocalDate

/**
 * Safely compare if two nullable dates are different.
 * Returns true if either date is different or one date is null
 */
fun LocalDate?.hasChanged(otherDate: LocalDate?): Boolean = this?.let { it != otherDate } ?: (otherDate != null)
