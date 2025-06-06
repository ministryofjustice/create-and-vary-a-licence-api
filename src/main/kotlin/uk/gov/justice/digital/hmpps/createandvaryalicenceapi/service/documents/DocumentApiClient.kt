package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.documents

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.util.UUID

@Service
class DocumentApiClient(@Qualifier("oauthDocumentApiClient") val documentApiClient: WebClient) {
  // Swagger documentation: https://document-api-dev.hmpps.service.justice.gov.uk/swagger-ui/index.html#/document-controller/uploadDocument
  fun uploadDocument(
    documentUuid: UUID,
    documentType: DocumentType,
    file: ByteArray,
    metadata: Map<String, String> = mapOf(),
  ): Document = documentApiClient.post()
    .uri("/documents/$documentType/$documentUuid")
    .header("Service-Name", "create-and-vary-a-licence-api")
    .bodyValue(
      MultipartBodyBuilder().apply {
        part("file", file, contentTypeFor(documentType))
        part("metadata", metadata)
      }.build(),
    )
    .accept(MediaType.APPLICATION_JSON)
    .retrieve()
    .bodyToMono(Document::class.java)
    .onErrorResume { Mono.empty() }
    .block() ?: error("Unable to upload document: $documentType/$documentUuid")

  private fun contentTypeFor(documentType: DocumentType) = when (documentType) {
    DocumentType.EXCLUSION_ZONE_MAP -> MediaType.APPLICATION_PDF
  }
}
