package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.variations

import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.VariationLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.EditVariationRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.HdcLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.HdcVariationLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceKinds
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ModelVariation
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response.VariationChangeResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StaffRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.AuditService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions.LicenceConditionService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcVariationLicence as HdcVariationLicenceEntity
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence as EntityLicence

@Service
class VariationService(
  private val auditService: AuditService,
  private val licenceConditionService: LicenceConditionService,
  private val licenceRepository: LicenceRepository,
  private val licenceService: LicenceService,
  private val staffRepository: StaffRepository,
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

  @Transactional
  fun editVariation(variationId: Long, request: EditVariationRequest) {
    val variation = getVariationLicence(variationId)
    require(variation.statusCode == LicenceStatus.VARIATION_SUBMITTED) { "Only submitted variations can be edited." }

    val staffMember = staffRepository.findByUsernameIgnoreCase(request.username)
    variation.updateStatus(
      statusCode = LicenceStatus.VARIATION_IN_PROGRESS,
      staffMember = staffMember,
      approvedByUsername = variation.approvedByUsername,
      approvedByName = variation.approvedByName,
      approvedDate = variation.approvedDate,
      supersededDate = null,
      submittedDate = variation.submittedDate,
      licenceActivatedDate = variation.licenceActivatedDate,
    )
    licenceRepository.saveAndFlush(variation)
    auditService.recordAuditEventVariationEdited(variation, staffMember)

    licenceConditionService.updateLicencePolicy(variationId)
  }

  private fun getVariationLicence(licenceId: Long): EntityLicence {
    val licence = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }
    require(licence is VariationLicence || licence is HdcVariationLicenceEntity) { "licence with id: $licenceId is not a variation" }
    return licence
  }

  private fun Licence.isHdcLicence() = kind == LicenceKinds.HDC || kind == LicenceKinds.HDC_VARIATION

  private fun Licence.isVariation() = kind == LicenceKinds.VARIATION || kind == LicenceKinds.HDC_VARIATION
}
