package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.documents

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
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
        part("file", ByteArrayResource(file)).filename(documentUuid.toString())
        part("metadata", metadata)
      }.build(),
    )
    .accept(MediaType.APPLICATION_JSON)
    .retrieve()
    .onStatus(HttpStatusCode::isError) { response ->
      response.bodyToMono<Map<String, String>>().map { json ->
        error("Error during uploading document (UUID=$documentUuid, StatusCode=${response.statusCode().value()}, Response=$json)")
      }
    }
    .bodyToMono(Document::class.java)
    .block() ?: error("Error during uploading document (UUID=$documentUuid) - Null response")
}
