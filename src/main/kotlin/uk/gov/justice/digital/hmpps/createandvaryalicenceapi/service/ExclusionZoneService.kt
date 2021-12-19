package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
// import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionUploadDetail
// import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalConditionUploadSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionUploadDetailRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import javax.persistence.EntityNotFoundException

@Service
class ExclusionZoneService(
  private val licenceRepository: LicenceRepository,
  private val additionalConditionRepository: AdditionalConditionRepository,
  private val additionalConditionUploadDetailRepository: AdditionalConditionUploadDetailRepository,
) {

  @Transactional
  fun uploadExclusionZoneFile(licenceId: Long, conditionId: Long, file: MultipartFile) {
    val licenceEntity = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }

    val additionalCondition = additionalConditionRepository
      .findById(conditionId)
      .orElseThrow { EntityNotFoundException("$conditionId") }

    // Remove upload detail rows manually if present - intentionally not linked to the additionalCondition entity
    additionalCondition.additionalConditionUploadSummary.map { it.uploadDetailId }.forEach {
      additionalConditionUploadDetailRepository.findById(it).ifPresent { detail ->
        additionalConditionUploadDetailRepository.delete(detail)
      }
    }

    // Replace any upload summary items with the new version via the additional condition entity
    var updatedAdditionalCondition = additionalCondition.copy(additionalConditionUploadSummary = emptyList())

    // Get the inputStream from the MultiPart file
    // Get the PDF bytes
    // Extract the image from page 1
    // Extract the text from page 2
    // Create a thumbnail and full-size image

    // val newSummary = AdditionalConditionUploadSummary()
    // val newDetail = AdditionalConditionUploadDetail()

    additionalConditionRepository.saveAndFlush(updatedAdditionalCondition)
  }

  @Transactional
  fun removeExclusionZoneFile(licenceId: Long, conditionId: Long) {
    licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }

    val additionalCondition = additionalConditionRepository
      .findById(conditionId)
      .orElseThrow { EntityNotFoundException("$conditionId") }

    // Remove upload detail rows manually if present - intentionally not linked to the additionalCondition entity
    additionalCondition.additionalConditionUploadSummary.map { it.uploadDetailId }.forEach {
      additionalConditionUploadDetailRepository.findById(it).ifPresent {
        additionalConditionUploadDetailRepository.delete(it)
      }
    }

    // Remove the uploadSummary via the additionalCondition entity
    val updatedAdditionalCondition = additionalCondition.copy(additionalConditionUploadSummary = emptyList())
    additionalConditionRepository.saveAndFlush(updatedAdditionalCondition)
  }
}
