package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Prisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner

enum class LicenceType {
  AP,
  AP_PSS,
  PSS,
  ;

  companion object {
    fun getLicenceType(nomisRecord: PrisonerSearchPrisoner) = when {
      nomisRecord.licenceExpiryDate == null -> PSS
      nomisRecord.topupSupervisionExpiryDate == null || nomisRecord.topupSupervisionExpiryDate <= nomisRecord.licenceExpiryDate -> AP
      else -> AP_PSS
    }

    fun getLicenceType(nomisRecord: Prisoner) = when {
      nomisRecord.licenceExpiryDate == null -> PSS
      nomisRecord.topupSupervisionExpiryDate == null || nomisRecord.topupSupervisionExpiryDate <= nomisRecord.licenceExpiryDate -> AP
      else -> AP_PSS
    }
  }
}
