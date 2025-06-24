package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
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
import java.util.UUID
import javax.imageio.ImageIO

@Service
class ExclusionZoneService(
  private val licenceRepository: LicenceRepository,
  private val additionalConditionRepository: AdditionalConditionRepository,
  private val additionalConditionUploadDetailRepository: AdditionalConditionUploadDetailRepository,
  private val documentService: DocumentService,
) {

  @Value("\${hmpps.document.api.enabled:false}")
  private val documentServiceEnabled: Boolean = false

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

    log.info("uploadExclusionZoneFile: Name ${file.name} Type ${file.contentType} Original ${file.originalFilename}, Size ${file.size}")

    val pdfExtract = ExclusionZonePdfExtract.fromMultipartFile(file)

    if (pdfExtract == null) {
      val reason = if (file.isEmpty) "Empty file" else "failed to extract the expected image map"
      throw ValidationException("Exclusion zone upload document - $reason")
    }

    logAndUploadExclusionZoneFile(file, pdfExtract, licenceEntity, additionalCondition)
  }

  fun logAndUploadExclusionZoneFile(
    originalFile: MultipartFile,
    pdfExtract: ExclusionZonePdfExtract,
    licence: Licence,
    additionalCondition: AdditionalCondition,
  ) {
    val (originalDataDsUuid, fullSizeImageDsUuid, thumbnailImageDsUuid) =
      uploadDocuments(originalFile, pdfExtract, licence, additionalCondition)

    val uploadDetail = AdditionalConditionUploadDetail(
      licenceId = licence.id,
      additionalConditionId = additionalCondition.id,
      originalData = originalFile.bytes,
      originalDataDsUuid = originalDataDsUuid?.toString(),
      fullSizeImage = pdfExtract.fullSizeImage,
      fullSizeImageDsUuid = fullSizeImageDsUuid?.toString(),
    )

    val savedDetail = additionalConditionUploadDetailRepository.saveAndFlush(uploadDetail)

    val uploadSummary = AdditionalConditionUploadSummary(
      additionalCondition = additionalCondition,
      filename = originalFile.originalFilename,
      fileType = originalFile.contentType,
      fileSize = originalFile.size.toInt(),
      description = pdfExtract.description,
      thumbnailImage = pdfExtract.thumbnailImage,
      thumbnailImageDsUuid = thumbnailImageDsUuid?.toString(),
      uploadDetailId = savedDetail.id,
    )

    val updatedAdditionalCondition = additionalCondition.copy(additionalConditionUploadSummary = listOf(uploadSummary))

    additionalConditionRepository.saveAndFlush(updatedAdditionalCondition)
  }

  private fun uploadDocuments(
    originalFile: MultipartFile,
    pdfExtract: ExclusionZonePdfExtract,
    licence: Licence,
    additionalCondition: AdditionalCondition,
  ): Triple<UUID?, UUID?, UUID?> {
    if (!documentServiceEnabled) {
      return Triple(null, null, null)
    }

    val metadata = mapOf(
      "licenceId" to licence.id.toString(),
      "additionalConditionId" to additionalCondition.id.toString(),
    )

    val originalDataDsUuid = documentService
      .uploadDocument(file = originalFile.bytes, metadata = metadata + mapOf("kind" to "pdf"))
    val fullSizeImageDsUuid = documentService
      .uploadDocument(file = pdfExtract.fullSizeImage, metadata = metadata + mapOf("kind" to "fullSizeImage"))
    val thumbnailImageDsUuid = documentService
      .uploadDocument(file = pdfExtract.thumbnailImage, metadata = metadata + mapOf("kind" to "thumbnail"))

    return Triple(originalDataDsUuid, fullSizeImageDsUuid, thumbnailImageDsUuid)
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
