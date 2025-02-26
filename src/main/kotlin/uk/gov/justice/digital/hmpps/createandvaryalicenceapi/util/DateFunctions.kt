package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util

import java.time.LocalDate

/**
 * Safely compare if two nullable dates are different.
 * Returns true if either date is different or one date is null
 */
fun nullableDatesDiffer(date1: LocalDate?, date2: LocalDate?): Boolean = date1?.let { it != date2 } ?: (date2 != null)
