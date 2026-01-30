package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions.upload

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.apache.pdfbox.Loader
import org.apache.pdfbox.io.RandomAccessReadBuffer
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionUpload
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionUploadRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions.upload.pdf.UploadPdfExtract
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions.upload.pdf.UploadPdfExtractBuilder
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.documents.DocumentService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.toBase64
import java.util.UUID
import javax.imageio.ImageIO
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Licence as LicenceModel

private const val IMAGE_TYPE = "image/png"

@Service
class UploadFileConditionsService(
  private val licenceRepository: LicenceRepository,
  private val additionalConditionRepository: AdditionalConditionRepository,
  private val additionalConditionUploadRepository: AdditionalConditionUploadRepository,
  private val documentService: DocumentService,
) {

  init {
    ImageIO.scanForPlugins()
  }

  @Transactional
  fun uploadFile(licenceId: Long, conditionId: Long, file: MultipartFile) {
    log.info(
      "uploadFile - Licence id={}, AdditionalCondition id={}, File(Name={}, OriginalFileName={}, Type={}, Size={})",
      licenceId,
      conditionId,
      file.name,
      file.originalFilename,
      file.contentType,
      file.size,
    )

    val licenceEntity = licence(licenceId)
    val additionalCondition = additionalCondition(conditionId)
    // get deletableDocumentUuids before data is changed on the DB
    val deletableDocumentUuids = getDeletableDocumentUuids(listOf(additionalCondition))

    val multipleUploadPdfExtract = fromMultipartFile(file)

    val (originalDataDsUuid, fullSizeImageDsUuid, thumbnailImageDsUuid) =
      uploadDocuments(file, multipleUploadPdfExtract, licenceEntity, additionalCondition)

    val uploadSummary = AdditionalConditionUpload(
      additionalCondition = additionalCondition,
      filename = file.originalFilename,
      fileType = file.contentType,
      fileSize = file.size.toInt(),
      imageType = IMAGE_TYPE,
      imageSize = multipleUploadPdfExtract.fullSizeImage.size,
      description = multipleUploadPdfExtract.description,
      thumbnailImageDsUuid = thumbnailImageDsUuid?.toString(),
      originalDataDsUuid = originalDataDsUuid?.toString(),
      fullSizeImageDsUuid = fullSizeImageDsUuid?.toString(),
    )

    additionalCondition.additionalConditionUpload.clear()
    additionalCondition.additionalConditionUpload.add(uploadSummary)

    // Delete Documents after all above work is done, just encase exception is thrown before now!
    deleteDocuments(deletableDocumentUuids)
  }

  fun fromMultipartFile(file: MultipartFile): UploadPdfExtract = runCatching {
    Loader.loadPDF(RandomAccessReadBuffer(file.inputStream)).use { pdfDoc ->
      UploadPdfExtractBuilder(pdfDoc).build()
    }
  }.getOrElse { throw ValidationException(it) }

  fun getImage(licenceId: Long, conditionId: Long): ByteArray? {
    val licence = licence(licenceId)
    if (!licence.additionalConditions.any { it.id == conditionId }) {
      throw EntityNotFoundException("Unable to find condition $conditionId on licence $licenceId")
    }
    val uploadDetail = additionalCondition(conditionId).additionalConditionUpload.first()
    return uploadDetail.fullSizeImageDsUuid?.let { uuid -> documentService.downloadDocument(UUID.fromString(uuid)) }
  }

  fun getThumbnailForImages(
    entityLicence: Licence,
    licence: LicenceModel,
  ) {
    val uploadThumbNailUuids = entityLicence.additionalConditions
      .flatMap { it.additionalConditionUpload }
      .filter { it.thumbnailImageDsUuid != null }
      .associate { it.id!! to UUID.fromString(it.thumbnailImageDsUuid!!) }

    licence.additionalLicenceConditions.forEach { condition ->
      condition.uploadSummary.forEach { summary ->
        uploadThumbNailUuids[summary.id]?.let { uuid ->
          try {
            val base64 = documentService.downloadDocument(uuid).toBase64()
            summary.thumbnailImage = base64
          } catch (e: Exception) {
            log.warn("Failed to download thumbnail for upload Id={}", summary.id, e)
            throw e
          }
        }
      }
    }
  }

  @Transactional
  fun getDeletableDocumentUuids(additionalConditions: List<AdditionalCondition>): List<UUID> {
    val additionalConditionIds = additionalConditions.map { it.id!! }
    log.info("Deleting documents for AdditionalConditions with id in ({})", additionalConditionIds)
    val documentUuids = additionalConditionUploadRepository.findDocumentUuidsFor(additionalConditionIds)
    return documentUuids.filter {
      additionalConditionUploadRepository.hasOnlyOneUpload(it)
    }
      .map { UUID.fromString(it) }
  }

  fun deleteDocuments(documentUuids: List<UUID>) {
    log.info("Deleting documents for uuids in ({})", documentUuids)
    documentUuids.forEach { documentService.deleteDocument(it) }
  }

  @Transactional
  fun deleteDocumentsForConditions(additionalConditions: List<AdditionalCondition>) {
    deleteDocuments(getDeletableDocumentUuids(additionalConditions))
  }

  private fun uploadDocuments(
    originalFile: MultipartFile,
    pdfExtract: UploadPdfExtract,
    licenceEntity: Licence,
    additionalCondition: AdditionalCondition,
  ): Triple<UUID?, UUID?, UUID?> {
    val metadataFor = { kind: String ->
      mapOf(
        "licenceId" to licenceEntity.id.toString(),
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

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
