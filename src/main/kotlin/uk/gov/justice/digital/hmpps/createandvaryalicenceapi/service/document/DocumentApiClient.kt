package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.document

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.document.Document
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.document.DocumentMetaData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.typeReference

@Service
class DocumentApiClient(
  @Qualifier("oauthDocumentApiClient") val documentApiWebClient: WebClient,
) {
  fun postDocument(
    documentUuid: String,
    file: ByteArray,
    fileType: MediaType,
    metadata: DocumentMetaData,
    documentType: String,
  ): Document? {
    val contentsAsResource: ByteArrayResource =
      object : ByteArrayResource(file) {
        override fun getFilename(): String {
          return documentUuid
        }
      }
    log.info(documentUuid)
    return documentApiWebClient
      .post()
      .uri("/documents/$documentType/$documentUuid")
      .header("Service-Name", "create-and-vary-a-licence-api")
      .bodyValue(
        MultipartBodyBuilder().apply {
          part("file", contentsAsResource, fileType)
          part("metadata", metadata)
        }.build(),
      )
      .retrieve()
      .bodyToMono(typeReference<Document>())
      .block()
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
