package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util

import kotlin.enums.enumEntries

enum class EligibileKind(val licenceKind: LicenceKind) {
  CRD(LicenceKind.CRD),
  HDC(LicenceKind.HDC),
  FIXED_TERM(LicenceKind.PRRD),
  ;


  companion object {
    fun findByLicenceKind(licenceKind: LicenceKind): EligibileKind? {
      return enumEntries<EligibileKind>().find { it.licenceKind == licenceKind }
    }
  }
}
