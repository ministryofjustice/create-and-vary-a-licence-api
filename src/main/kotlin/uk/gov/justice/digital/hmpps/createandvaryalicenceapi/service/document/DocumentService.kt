package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.document

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.http.MediaType.APPLICATION_PDF
import org.springframework.http.MediaType.IMAGE_JPEG
import org.springframework.stereotype.Service
import java.util.UUID

const val DOCUMENT_TYPE = "EXCLUSION_ZONE_MAP"

enum class LicenceDocumentType(
  val fileType: MediaType,
) {
  EXCLUSION_ZONE_MAP_PDF(APPLICATION_PDF),
  EXCLUSION_ZONE_MAP_FULL_IMG(IMAGE_JPEG),
  EXCLUSION_ZONE_MAP_THUMBNAIL(IMAGE_JPEG),
}

@Service
class DocumentService(
  val documentApiClient: DocumentApiClient,
  @Value("\${hmpps.document.api.enabled}") private val enabled: Boolean,
) {
  fun getDocument(documentUUID: String): ByteArray? {
    if (!enabled) {
      log.warn("Document Service is disabled.")
      return null
    }
    return documentApiClient.getDocument(documentUUID)
  }

  fun uploadExclusionZoneFile(
    type: LicenceDocumentType,
    licenceId: Long,
    additionalConditionId: Long,
    file: ByteArray?,
  ): String? {
    log.info(
      "Posting $type for licence: $licenceId, condition: $additionalConditionId",
    )
    return postFile(
      file = file,
      fileType = type.fileType,
      metadata =
      DocumentMetaData(
        subType = type,
        licenceId = licenceId.toString(),
        additionalConditionId = additionalConditionId.toString(),
      ),
    )
  }

  private fun postFile(
    file: ByteArray?,
    fileType: MediaType,
    metadata: DocumentMetaData,
  ): String? {
    if (!enabled) {
      log.warn("Document Service is disabled: Didn't send document to document service.")
      return null
    }

    if (file == null || file.isEmpty()) return null

    val documentUuid = UUID.randomUUID().toString()
    documentApiClient.postDocument(DOCUMENT_TYPE, documentUuid, fileType, file, metadata)
    return documentUuid
  }

  companion object {
    private val log = LoggerFactory.getLogger(DocumentService::class.java)
  }
}
