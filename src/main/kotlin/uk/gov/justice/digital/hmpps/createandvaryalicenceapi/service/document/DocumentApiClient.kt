package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.document

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.document.CreateDocumentRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.document.Document
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.document.DocumentMetaData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.typeReference

@Service
class DocumentApiClient(
  @Qualifier("oauthDocumentApiClient") val documentApiWebClient: WebClient,
) {
  fun postDocument(
    documentUuid: String,
    file: String,
    metadata: DocumentMetaData,
    documentType: String,
  ): Document? {
    return documentApiWebClient
      .post()
      .uri("/documents/$documentType/$documentUuid")
      .accept(MediaType.APPLICATION_JSON)
      .bodyValue(CreateDocumentRequest(file, metadata))
      .retrieve()
      .bodyToMono(typeReference<Document>())
      .block()
  }
}
