package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.document

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
    metadata: DocumentMetaData,
    documentType: String,
  ): Document? {
    val contentsAsResource: ByteArrayResource =
      object : ByteArrayResource(file) {
        override fun getFilename(): String {
          return documentUuid
        }
      }
    return documentApiWebClient
      .post()
      .uri("/documents/$documentType/$documentUuid")
      .header("Service-Name", "create-and-vary-a-licence-api")
      .bodyValue(
        MultipartBodyBuilder().apply {
          part("file", contentsAsResource, MediaType.IMAGE_JPEG)
          part("metadata", metadata)
        }.build(),
      )
      .retrieve()
      .bodyToMono(typeReference<Document>())
      .block()
  }
}
