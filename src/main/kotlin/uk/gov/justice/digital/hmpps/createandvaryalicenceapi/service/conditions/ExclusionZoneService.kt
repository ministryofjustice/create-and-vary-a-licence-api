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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.ExclusionZoneUpload
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionUploadDetailRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.ExclusionZoneUploadsRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.documents.DocumentService
import javax.imageio.ImageIO

@Service
class ExclusionZoneService(
  private val licenceRepository: LicenceRepository,
  private val additionalConditionRepository: AdditionalConditionRepository,
  private val additionalConditionUploadDetailRepository: AdditionalConditionUploadDetailRepository,
  private val documentService: DocumentService,
  private val exclusionZoneUploadsRepository: ExclusionZoneUploadsRepository,
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

    removeExistingDbRows(additionalCondition)

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

    uploadExclusionMapDocumentToService(uploadFile, licenceEntity, additionalCondition)

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

  private fun uploadExclusionMapDocumentToService(
    uploadFile: ExclusionZoneUploadFile,
    licenceEntity: Licence,
    additionalCondition: AdditionalCondition,
  ) {
    val metadata = mapOf(
      "licenceId" to licenceEntity.id.toString(),
      "additionalConditionId" to additionalCondition.id.toString(),
    )
    val pdf = documentService
      .uploadDocument(file = uploadFile.bytes, metadata = metadata + mapOf("kind" to "pdf"))
    val image = documentService
      .uploadDocument(file = uploadFile.fullSizeImage!!, metadata = metadata + mapOf("kind" to "fullSizeImage"))
    val thumb = documentService
      .uploadDocument(file = uploadFile.thumbnailImage!!, metadata = metadata + mapOf("kind" to "thumbnail"))

    exclusionZoneUploadsRepository.save(
      ExclusionZoneUpload(
        licence = licenceEntity,
        additionalCondition = additionalCondition,
        pdfId = pdf,
        fullImageId = image,
        thumbnailId = thumb,
      ),
    )
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
