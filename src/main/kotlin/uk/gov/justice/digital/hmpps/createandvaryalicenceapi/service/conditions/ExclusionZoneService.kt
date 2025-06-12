package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionUploadDetail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionUploadSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionUploadDetailRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import javax.imageio.ImageIO

@Service
class ExclusionZoneService(
  private val licenceRepository: LicenceRepository,
  private val additionalConditionRepository: AdditionalConditionRepository,
  private val additionalConditionUploadDetailRepository: AdditionalConditionUploadDetailRepository,
) {

  init {
    ImageIO.scanForPlugins()
  }

  @Transactional
  fun uploadExclusionZoneFile(licenceId: Long, conditionId: Long, file: MultipartFile) {
    val licenceEntity = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }

    val additionalCondition = additionalConditionRepository
      .findById(conditionId)
      .orElseThrow { EntityNotFoundException("$conditionId") }

    // Remove any existing upload detail rows manually - intentionally not linked to the additionalCondition entity
    // There can only be one uploaded exclusion map on this licence/condition
    additionalCondition.additionalConditionUploadSummary.map { it.uploadDetailId }.forEach {
      additionalConditionUploadDetailRepository.findById(it).ifPresent { detail ->
        additionalConditionUploadDetailRepository.delete(detail)
      }
    }

    log.info("uploadExclusionZoneFile:  Name ${file.name} Type ${file.contentType} Original ${file.originalFilename}, Size ${file.size}")

    if (file.isEmpty) {
      log.error("uploadExclusion:  Empty file uploaded, Name ${file.name} Type ${file.contentType} Orig. Name ${file.originalFilename}, Size ${file.size}")
      throw ValidationException("Exclusion zone - file was empty.")
    }

    // Process the MapMaker PDF file to get the fullSizeImage from page 1, descriptive text on page 2 and generate a thumbnail
    val uploadFile = ExclusionZoneUploadFile(file)
    val fullSizeImage = uploadFile.fullSizeImage
    val description = uploadFile.description
    val thumbnailImage = uploadFile.thumbnailImage

    // Validate that we were able to extract meaningful data from the uploaded file
    if (fullSizeImage == null || thumbnailImage == null) {
      log.error("uploadExclusion:  Could not extract images from file, Name ${file.name} Type ${file.contentType} Orig. Name ${file.originalFilename}, Size ${file.size}")
      throw ValidationException("Exclusion zone - failed to extract the expected image map")
    }

    val uploadDetail = AdditionalConditionUploadDetail(
      licenceId = licenceEntity.id,
      additionalConditionId = additionalCondition.id,
      originalData = file.bytes,
      fullSizeImage = fullSizeImage,
    )

    val savedDetail = additionalConditionUploadDetailRepository.saveAndFlush(uploadDetail)

    val uploadSummary = AdditionalConditionUploadSummary(
      additionalCondition = additionalCondition,
      filename = file.originalFilename,
      fileType = file.contentType,
      fileSize = file.size.toInt(),
      description = description,
      thumbnailImage = thumbnailImage,
      uploadDetailId = savedDetail.id,
    )

    val updatedAdditionalCondition = additionalCondition.copy(additionalConditionUploadSummary = listOf(uploadSummary))

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

    // Remove uploadDetail rows manually - intentionally not linked to the additionalCondition entity
    additionalCondition.additionalConditionUploadSummary.map { it.uploadDetailId }.forEach {
      additionalConditionUploadDetailRepository.findById(it).ifPresent { detail ->
        additionalConditionUploadDetailRepository.delete(detail)
      }
    }

    // Remove the additionalConditionData item for 'outOfBoundFilename'
    val updatedAdditionalConditionData = additionalCondition
      .additionalConditionData
      .filter { !it.dataField.equals("outOfBoundFilename") }

    // Update summary and data via the additionalCondition lists
    val updatedAdditionalCondition = additionalCondition.copy(
      additionalConditionData = updatedAdditionalConditionData,
      additionalConditionUploadSummary = emptyList(),
    )

    additionalConditionRepository.saveAndFlush(updatedAdditionalCondition)
  }

  fun getExclusionZoneImage(licenceId: Long, conditionId: Long): ByteArray? {
    licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }

    val additionalCondition = additionalConditionRepository
      .findById(conditionId)
      .orElseThrow { EntityNotFoundException("$conditionId") }

    val uploadIds = additionalCondition.additionalConditionUploadSummary.map { it.uploadDetailId }
    if (uploadIds.isEmpty()) {
      throw EntityNotFoundException("$conditionId")
    }

    val upload = additionalConditionUploadDetailRepository
      .findById(uploadIds.first())
      .orElseThrow { EntityNotFoundException("$conditionId") }

    return upload.fullSizeImage
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
