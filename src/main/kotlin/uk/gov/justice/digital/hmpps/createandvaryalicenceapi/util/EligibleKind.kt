package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util

enum class EligibleKind(val licenceKind: LicenceKind) {
  CRD(LicenceKind.CRD),
  HDC(LicenceKind.HDC),
  FIXED_TERM(LicenceKind.PRRD) {
    override fun isRecall() = true
  },
  ;

  open fun isRecall(): Boolean = false
}
