package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Prisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner

@Schema(description = "Type of Licence")
enum class LicenceType {
  @Schema(description = "All purpose")
  AP {
    override fun conditionTypes() = setOf(AP.name)
  },

  @Schema(description = "All purpose & post sentence supervision")
  AP_PSS {
    override fun conditionTypes() = setOf(AP.name, PSS.name)
  },

  @Schema(description = "post sentence supervision")
  PSS {
    override fun conditionTypes() = setOf(PSS.name)
  },
  ;

  abstract fun conditionTypes(): Set<String>

  companion object {
    fun getLicenceType(nomisRecord: PrisonerSearchPrisoner): LicenceType {
      val topupSupervisionExpiryDate = nomisRecord.topupSupervisionExpiryDate
      return when {
        nomisRecord.licenceExpiryDate == null && topupSupervisionExpiryDate == null -> AP
        nomisRecord.licenceExpiryDate == null -> PSS
        topupSupervisionExpiryDate == null || topupSupervisionExpiryDate <= nomisRecord.licenceExpiryDate -> AP
        else -> AP_PSS
      }
    }

    fun getLicenceType(nomisRecord: Prisoner): LicenceType {
      val topupSupervisionExpiryDate = nomisRecord.topupSupervisionExpiryDate
      return when {
        nomisRecord.licenceExpiryDate == null && topupSupervisionExpiryDate == null -> AP
        nomisRecord.licenceExpiryDate == null -> PSS
        topupSupervisionExpiryDate == null || topupSupervisionExpiryDate <= nomisRecord.licenceExpiryDate -> AP
        else -> AP_PSS
      }
    }
  }
}
