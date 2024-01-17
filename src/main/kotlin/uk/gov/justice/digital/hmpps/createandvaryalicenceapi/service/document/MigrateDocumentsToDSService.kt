package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.document

import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.http.MediaType
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionDocuments
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.document.DocumentMetaData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.document.LicenceDocumentType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionDocumentsRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AuditEventType

@Service
class MigrateDocumentsToDSService(
  val documentService: DocumentService,
  val additionalConditionDocumentsRepository: AdditionalConditionDocumentsRepository,
  private val auditEventRepository: AuditEventRepository,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  fun migrateDocuments(count: Int) {
    log.info("Start - Copying documents to document service. Max count is: $count")
    val exclusionZoneMaps = getExclusionZoneMaps(count)
    log.info("Found " + exclusionZoneMaps.size + " maps to copy to document service")
    exclusionZoneMaps.forEach {
      postDocumentsToDS(it)
    }
  }

  fun removeDocuments(count: Int) {
    log.info("Start - Removing documents which are already copied to document service. Max count is: $count")
    val exclusionZoneMaps = getExclusionZoneMapsAlreadyCopiedToDocumentService(count)
    log.info("Found " + exclusionZoneMaps.size + " maps to remove from database")
    exclusionZoneMaps.forEach {
      removeFiles(it)
    }
  }

  private fun removeFiles(additionalCondDocument: AdditionalConditionDocuments) {
    if (additionalCondDocument.fullSizeImageDsUuid != null) {
      additionalCondDocument.fullSizeImage = null
    }
    if (additionalCondDocument.thumbnailImageDsUuid != null) {
      additionalCondDocument.thumbnailImage = null
    }
    if (additionalCondDocument.originalDataDsUuid != null) {
      additionalCondDocument.originalData = null
    }
    additionalConditionDocumentsRepository.saveAndFlush(additionalCondDocument)
    var userName = "SYSTEM"
    val authentication: Authentication = SecurityContextHolder.getContext().authentication
    if (authentication !is AnonymousAuthenticationToken) {
      userName = authentication.name
    }
    auditEventRepository.saveAndFlush(
      AuditEvent(
        licenceId = additionalCondDocument.licenceId,
        username = userName,
        fullName = userName,
        eventType = AuditEventType.SYSTEM_EVENT,
        summary =
        "Removed full size image, raw data and thumbnail  documents from database for licenceId:" +
          additionalCondDocument.licenceId + ", additionalConditionId:" + additionalCondDocument.additionalConditionId +
          " as these are now copied to document service",
        detail =
        "Removed full size image, raw data and thumbnail  documents from database for licenceId:" +
          additionalCondDocument.licenceId + ", additionalConditionId:" + additionalCondDocument.additionalConditionId +
          " as these are now copied to document service as fullSizeImageDsUuid:" + additionalCondDocument.fullSizeImageDsUuid +
          ", originalDataDsUuid:" + additionalCondDocument.originalDataDsUuid + " additionalCondDocument.thumbnailImageDsUuid:" +
          additionalCondDocument.thumbnailImageDsUuid,
      ),
    )
  }

  private fun getExclusionZoneMaps(max: Int): List<AdditionalConditionDocuments> {
    return additionalConditionDocumentsRepository.getFilesWhichAreNotCopiedToDocumentService(Pageable.ofSize(max))
  }

  private fun getExclusionZoneMapsAlreadyCopiedToDocumentService(max: Int): List<AdditionalConditionDocuments> {
    return additionalConditionDocumentsRepository.getFilesWhichAreAlreadyCopiedToDocumentService(Pageable.ofSize(max))
  }

  fun postDocumentsToDS(additionalCond: AdditionalConditionDocuments) {
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
    if (additionalCond.thumbnailImageDsUuid == null) {
      // thumbnail
      val thumbnailUuid =
        postDocument(
          additionalCond.thumbnailImage,
          additionalCond,
          MediaType.IMAGE_JPEG,
          LicenceDocumentType.EXCLUSION_ZONE_MAP_THUMBNAIL,
        )
      additionalCond.thumbnailImageDsUuid = thumbnailUuid
    }
    additionalCond.originalDataDsUuid = pdfUuid
    additionalCond.fullSizeImageDsUuid = fullSizeImgUuid

    additionalConditionDocumentsRepository.saveAndFlush(additionalCond)
  }

  private fun postDocument(
    file: ByteArray?,
    additionalCond: AdditionalConditionDocuments,
    mediaType: MediaType,
    licenceDocumentType: LicenceDocumentType,
  ): String? {
    log.info(
      "Posting " + licenceDocumentType + " for additionalCond.additionalConditionId: " +
        additionalCond.additionalConditionId,
    )

    return documentService.postFileToDocumentService(
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
