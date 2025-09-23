package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util

import java.time.LocalDate

@TimeServedConsiderations("Any consideration here for time served licences when determining kind")
fun determineReleaseDateKind(prrdDate: LocalDate?, crdDate: LocalDate?): LicenceKind {
  val usePrrd = prrdDate?.isTodayOrInTheFuture() == true && (crdDate == null || prrdDate.isAfter(crdDate))
  if (usePrrd) {
    return LicenceKind.PRRD
  }
  return LicenceKind.CRD
}
