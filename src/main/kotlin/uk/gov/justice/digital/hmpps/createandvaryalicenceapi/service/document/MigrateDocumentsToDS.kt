package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.document

import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionDocuments
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.document.DocumentMetaData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.document.LicenceDocumentType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionDocumentsRepository

@Service
class MigrateDocumentsToDS(
  val documentService: DocumentService,
  val additionalConditionDocumentsRepository: AdditionalConditionDocumentsRepository,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  fun migrateDocuments(count: Int) {
    log.info("Start - Copying documents to document service. Max count is: $count")
    val exclusionZoneMaps = getExclusionZoneMaps(count)
    log.info("Found " + exclusionZoneMaps.size + " maps to copy to document service")
    exclusionZoneMaps.forEach {
      migrateExclusionZoneMaps(it)
    }
  }

  fun getExclusionZoneMaps(max: Int): List<AdditionalConditionDocuments> {
    return additionalConditionDocumentsRepository.getFilesWhichAreNotCopiedToDocumentService(Pageable.ofSize(max))
  }

  private fun migrateExclusionZoneMaps(additionalCond: AdditionalConditionDocuments) {
    // JPEG image
    val fullSizeImgUuid =
      postDocument(
        additionalCond.fullSizeImage,
        additionalCond,
        MediaType.IMAGE_JPEG,
        LicenceDocumentType.EXCLUSION_ZONE_MAP_FULL_IMG,
      )
    // PDF version
    val pdfUuid =
      postDocument(
        additionalCond.originalData,
        additionalCond,
        MediaType.APPLICATION_PDF,
        LicenceDocumentType.EXCLUSION_ZONE_MAP_PDF,
      )
    // thumbnail
    val thumbnailUuid =
      postDocument(
        additionalCond.thumbnailImage,
        additionalCond,
        MediaType.IMAGE_JPEG,
        LicenceDocumentType.EXCLUSION_ZONE_MAP_THUMBNAIL,
      )
    additionalCond.thumbnailImageDsUuid = thumbnailUuid
    additionalCond.originalDataDsUuid = pdfUuid
    additionalCond.fullSizeImageDsUuid = fullSizeImgUuid
  }

  private fun postDocument(
    file: ByteArray?,
    additionalCond: AdditionalConditionDocuments,
    mediaType: MediaType,
    licenceDocumentType: LicenceDocumentType,
  ): String? {
    log.info("Posting " + licenceDocumentType + " for additionalCond.additionalConditionId: " + additionalCond.additionalConditionId)

    return documentService.postDocumentToDocumentService(
      file,
      mediaType,
      metadata =
        DocumentMetaData(
          licenceId = additionalCond.licenceId.toString(),
          additionalConditionId = additionalCond.additionalConditionId.toString(),
          documentType = licenceDocumentType.toString(),
        ),
      documentType = LicenceDocumentType.EXCLUSION_ZONE_MAP.toString(),
    )
  }
}
