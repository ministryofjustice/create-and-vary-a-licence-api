package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util

import java.time.LocalDate

fun determineReleaseDateKind(prrd: LocalDate?, crd: LocalDate?): LicenceKind = when {
  // hdcad?.isTodayOrInTheFuture() == true -> LicenceKind.HDC
  prrd?.isTodayOrInTheFuture() == true && (crd == null || prrd.isAfter(crd)) -> LicenceKind.PRRD
  else -> LicenceKind.CRD
}
