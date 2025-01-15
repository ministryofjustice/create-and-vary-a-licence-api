package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Prisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner

enum class LicenceType {
  AP {
    override fun conditionTypes() = setOf(AP.name)
  },
  AP_PSS {
    override fun conditionTypes() = setOf(AP.name, PSS.name)
  },
  PSS {
    override fun conditionTypes() = setOf(PSS.name)
  },
  ;

  abstract fun conditionTypes(): Set<String>

  companion object {
    fun getLicenceType(nomisRecord: PrisonerSearchPrisoner) = when {
      nomisRecord.licenceExpiryDate == null && nomisRecord.topupSupervisionExpiryDate == null -> AP
      nomisRecord.licenceExpiryDate == null -> PSS
      nomisRecord.topupSupervisionExpiryDate == null || nomisRecord.topupSupervisionExpiryDate <= nomisRecord.licenceExpiryDate -> AP
      else -> AP_PSS
    }

    fun getLicenceType(nomisRecord: Prisoner) = when {
      nomisRecord.licenceExpiryDate == null && nomisRecord.topupSupervisionExpiryDate == null -> AP
      nomisRecord.licenceExpiryDate == null -> PSS
      nomisRecord.topupSupervisionExpiryDate == null || nomisRecord.topupSupervisionExpiryDate <= nomisRecord.licenceExpiryDate -> AP
      else -> AP_PSS
    }
  }
}
