package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner

enum class LicenceType {
  AP,
  AP_PSS,
  PSS,
  ;

  companion object {
    fun getLicenceType(nomisRecord: PrisonerSearchPrisoner) = when {
      nomisRecord.licenceExpiryDate == null -> PSS
      nomisRecord.topUpSupervisionExpiryDate == null || nomisRecord.topUpSupervisionExpiryDate <= nomisRecord.licenceExpiryDate -> AP
      else -> AP_PSS
    }
  }
}
