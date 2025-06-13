package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionUploadDetail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionUploadSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionUploadDetailRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.documents.DocumentService
import javax.imageio.ImageIO

@Service
class ExclusionZoneService(
  private val licenceRepository: LicenceRepository,
  private val additionalConditionRepository: AdditionalConditionRepository,
  private val additionalConditionUploadDetailRepository: AdditionalConditionUploadDetailRepository,
  private val documentService: DocumentService,
) {

  init {
    ImageIO.scanForPlugins()
  }

  @Transactional
  fun uploadExclusionZoneFile(licenceId: Long, conditionId: Long, file: MultipartFile) {
    val licenceEntity = licenceRepository.findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }
    val additionalCondition = additionalConditionRepository.findById(conditionId)
      .orElseThrow { EntityNotFoundException("$conditionId") }

    log.info("uploadExclusionZoneFile:  Name ${file.name} Type ${file.contentType} Original ${file.originalFilename}, Size ${file.size}")

    if (file.isEmpty) {
      log.error("uploadExclusion:  Empty file uploaded, Name ${file.name} Type ${file.contentType} Orig. Name ${file.originalFilename}, Size ${file.size}")
      throw ValidationException("Exclusion zone - file was empty.")
    }

    // Process the MapMaker PDF file to get the fullSizeImage from page 1, descriptive text on page 2 and generate a thumbnail
    val uploadFile = ExclusionZoneUploadFile(file)

    // Validate that we were able to extract meaningful data from the uploaded file
    if (uploadFile.fullSizeImage == null || uploadFile.thumbnailImage == null) {
      log.error("uploadExclusion:  Could not extract images from file, Name ${file.name} Type ${file.contentType} Orig. Name ${file.originalFilename}, Size ${file.size}")
      throw ValidationException("Exclusion zone - failed to extract the expected image map")
    }

    saveExclusionMapToDb(uploadFile, licenceEntity, additionalCondition)
    uploadExclusionMapDocumentToService(uploadFile, licenceEntity, additionalCondition)
  }

  private fun uploadExclusionMapDocumentToService(
    uploadFile: ExclusionZoneUploadFile,
    licenceEntity: Licence,
    additionalCondition: AdditionalCondition,
  ) {
    mapOf(
      "PDF" to uploadFile.bytes,
      "THUMBNAIL" to uploadFile.thumbnailImage!!,
      "FULL_IMAGE" to uploadFile.fullSizeImage!!,
    ).forEach { (type, file) ->
      documentService.uploadDocument(
        file = file,
        metadata = mapOf(
          "licenceId" to licenceEntity.id.toString(),
          "additionalConditionId" to additionalCondition.id.toString(),
          "type" to type,
        ),
      )
    }
  }

  private fun saveExclusionMapToDb(
    uploadFile: ExclusionZoneUploadFile,
    licenceEntity: Licence,
    additionalCondition: AdditionalCondition,
  ) {
    removeExistingDbRows(additionalCondition)

    val uploadDetail = AdditionalConditionUploadDetail(
      licenceId = licenceEntity.id,
      additionalConditionId = additionalCondition.id,
      originalData = uploadFile.bytes,
      fullSizeImage = uploadFile.fullSizeImage,
    )

    val savedDetail = additionalConditionUploadDetailRepository.saveAndFlush(uploadDetail)

    val uploadSummary = AdditionalConditionUploadSummary(
      additionalCondition = additionalCondition,
      filename = uploadFile.filename,
      fileType = uploadFile.contentType,
      fileSize = uploadFile.fileSize,
      description = uploadFile.description,
      thumbnailImage = uploadFile.thumbnailImage,
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

    removeExistingDbRows(additionalCondition)

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

  private fun removeExistingDbRows(additionalCondition: AdditionalCondition) {
    additionalCondition.additionalConditionUploadSummary.map { it.uploadDetailId }.forEach {
      additionalConditionUploadDetailRepository.findById(it).ifPresent { detail ->
        additionalConditionUploadDetailRepository.delete(detail)
      }
    }
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
