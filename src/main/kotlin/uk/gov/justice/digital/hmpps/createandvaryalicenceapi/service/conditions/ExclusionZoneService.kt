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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.DocumentCountsRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.documents.DocumentService
import java.util.UUID
import javax.imageio.ImageIO

private const val IMAGE_TYPE = "image/png"

@Service
class ExclusionZoneService(
  private val licenceRepository: LicenceRepository,
  private val additionalConditionRepository: AdditionalConditionRepository,
  private val additionalConditionUploadDetailRepository: AdditionalConditionUploadDetailRepository,
  private val documentService: DocumentService,
  private val documentCountsRepository: DocumentCountsRepository,
) {

  init {
    ImageIO.scanForPlugins()
  }

  @Transactional
  fun uploadExclusionZoneFile(licenceId: Long, conditionId: Long, file: MultipartFile) {
    log.info(
      "uploadExclusionZoneFile - Licence id={}, AdditionalCondition id={}, File(Name={}, OriginalFileName={}, Type={}, Size={})",
      licenceId,
      conditionId,
      file.name,
      file.originalFilename,
      file.contentType,
      file.size,
    )

    val licenceEntity = licence(licenceId)
    val additionalCondition = additionalCondition(conditionId)

    deleteDocumentsFor(listOf(additionalCondition))

    val pdfExtract = ExclusionZonePdfExtract
      .runCatching { fromMultipartFile(file) }
      .getOrElse { throw ValidationException(it) }

    val (originalDataDsUuid, fullSizeImageDsUuid, thumbnailImageDsUuid) =
      uploadExtractedExclusionZoneParts(file, pdfExtract, licenceEntity, additionalCondition)

    val uploadDetail = AdditionalConditionUploadDetail(
      licenceId = licenceEntity.id,
      additionalConditionId = additionalCondition.id!!,
      originalData = file.bytes,
      originalDataDsUuid = originalDataDsUuid?.toString(),
      fullSizeImage = pdfExtract.fullSizeImage,
      fullSizeImageDsUuid = fullSizeImageDsUuid?.toString(),
    )

    val savedDetail = additionalConditionUploadDetailRepository.saveAndFlush(uploadDetail)

    val uploadSummary = AdditionalConditionUploadSummary(
      additionalCondition = additionalCondition,
      filename = file.originalFilename,
      fileType = file.contentType,
      fileSize = file.size.toInt(),
      imageType = IMAGE_TYPE,
      imageSize = file.size.toInt(),
      description = pdfExtract.description,
      thumbnailImage = pdfExtract.thumbnailImage,
      thumbnailImageDsUuid = thumbnailImageDsUuid?.toString(),
      uploadDetailId = savedDetail.id!!,
    )

    val updatedAdditionalCondition = additionalCondition.copy(additionalConditionUploadSummary = mutableListOf(uploadSummary))

    additionalConditionRepository.saveAndFlush(updatedAdditionalCondition)
  }

  fun getExclusionZoneImage(licenceId: Long, conditionId: Long): ByteArray? {
    licence(licenceId)

    val uploadDetail = additionalConditionUploadDetail(
      additionalCondition(conditionId).additionalConditionUploadSummary.first().uploadDetailId,
    )

    return if (uploadDetail.fullSizeImageDsUuid != null) {
      documentService.downloadDocument(UUID.fromString(uploadDetail.fullSizeImageDsUuid))
    } else {
      uploadDetail.fullSizeImage
    }
  }

  fun preloadThumbnailsFor(licence: Licence) {
    licence.additionalConditions
      .flatMap { it.additionalConditionUploadSummary }
      .filterNot { it.thumbnailImageDsUuid == null }
      .forEach {
        it.preloadedThumbnailImage = documentService.downloadDocument(UUID.fromString(it.thumbnailImageDsUuid))
      }
  }

  fun deleteDocumentsFor(licence: Licence) {
    log.info("Deleting documents for Licence id={}", licence.id)

    deleteDocumentsFor(licence.additionalConditions)
  }

  fun deleteDocumentsFor(additionalConditions: List<AdditionalCondition>) {
    val additionalConditionIds = additionalConditions.map { it.id!! }

    log.info("Deleting documents for AdditionalConditions with id in ({})", additionalConditionIds)

    // Remove AdditionalConditionUploadDetail records so they don't get orphaned
    additionalConditionUploadDetailRepository.deleteAllByIdInBatch(
      additionalConditions
        .flatMap { it.additionalConditionUploadSummary }
        .map { it.uploadDetailId },
    )

    documentCountsRepository.countsOfDocumentsRelatedTo(additionalConditionIds)
      .filter { it.count == 1 }
      .also { log.info("Found {} documents to delete...", it.size) }
      .forEach { documentService.deleteDocument(UUID.fromString(it.uuid)) }
  }

  private fun uploadExtractedExclusionZoneParts(
    originalFile: MultipartFile,
    pdfExtract: ExclusionZonePdfExtract,
    licence: Licence,
    additionalCondition: AdditionalCondition,
  ): Triple<UUID?, UUID?, UUID?> {
    val metadataFor = { kind: String ->
      mapOf(
        "licenceId" to licence.id.toString(),
        "additionalConditionId" to additionalCondition.id.toString(),
        "kind" to kind,
      )
    }

    return Triple(
      documentService.uploadDocument(originalFile.bytes, metadataFor("pdf")),
      documentService.uploadDocument(pdfExtract.fullSizeImage, metadataFor("fullSizeImage")),
      documentService.uploadDocument(pdfExtract.thumbnailImage, metadataFor("thumbnail")),
    )
  }

  private fun licence(id: Long) = licenceRepository.findById(id)
    .orElseThrow { EntityNotFoundException("Unable to find Licence id=$id") }

  private fun additionalCondition(id: Long) = additionalConditionRepository.findById(id)
    .orElseThrow { EntityNotFoundException("Unable to find AdditionalCondition id=$id") }

  private fun additionalConditionUploadDetail(id: Long) = additionalConditionUploadDetailRepository.findById(id)
    .orElseThrow { EntityNotFoundException("Unable to find AdditionalConditionUploadDetail id=$id") }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
