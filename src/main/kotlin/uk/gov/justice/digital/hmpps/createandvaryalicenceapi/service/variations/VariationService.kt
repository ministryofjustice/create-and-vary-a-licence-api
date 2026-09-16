package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.variations

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.HdcLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.HdcVariationLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceKinds
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ModelVariation
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response.VariationChangeResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService

@Service
class VariationService(
  private val licenceService: LicenceService,
) {
  @Transactional
  fun calculateDiffFromOriginal(variationId: Long): VariationChangeResponse {
    val variationLicence = licenceService.getLicenceById(variationId)

    require(variationLicence.isVariation()) { "Licence with id $variationId is not a variation" }

    val originalLicence = licenceService.getLicenceById((variationLicence as ModelVariation).variationOf!!)

    val variedConditions = compareLicenceConditions(originalLicence, variationLicence)

    var updatedCurfewAddress = false
    var updatedCurfewHours = false
    if (variationLicence.isHdcLicence() && originalLicence.isHdcLicence()) {
      val variation = variationLicence as HdcVariationLicence
      val original = originalLicence as HdcLicence
      updatedCurfewAddress = hasUpdatedCurfewAddress(original.curfewAddress, variation.curfewAddress)
      updatedCurfewHours = hasUpdatedCurfewHours(originalLicence.weeklyCurfewTimes, variation.weeklyCurfewTimes)
    }
    return VariationChangeResponse(
      licenceConditionsAdded = variedConditions.licenceConditionsAdded,
      licenceConditionsRemoved = variedConditions.licenceConditionsRemoved,
      licenceConditionsAmended = variedConditions.licenceConditionsAmended,
      hasUpdatedCurfewAddress = updatedCurfewAddress,
      hasUpdatedCurfewHours = updatedCurfewHours,
    )
  }

  private fun Licence.isHdcLicence() = kind == LicenceKinds.HDC || kind == LicenceKinds.HDC_VARIATION

  private fun Licence.isVariation() = kind == LicenceKinds.VARIATION || kind == LicenceKinds.HDC_VARIATION
}
