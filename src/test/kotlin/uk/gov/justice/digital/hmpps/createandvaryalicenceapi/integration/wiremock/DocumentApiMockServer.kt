package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aMultipart
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.binaryEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.delete
import com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import net.minidev.json.JSONObject
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.documents.DocumentType
import java.util.UUID

class DocumentApiMockServer : WireMockServer(8097) {
  fun stubUploadDocument() {
    stubFor(
      post(urlMatching("/documents/EXCLUSION_ZONE_MAP/[a-z0-9A-Z|-]{36}")).willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(happyResponse)
          .withStatus(201),
      ),
    )
  }

  fun stubDeleteDocuments() {
    stubFor(
      delete(urlMatching("/documents/[a-z0-9A-Z|-]{36}")).willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withStatus(204),
      ),
    )
  }

  fun verifyUploadedDocument(
    didHappenXTimes: Int = 1,
    withUuid: String = "[a-z0-9A-Z|-]{36}",
    withType: DocumentType = DocumentType.EXCLUSION_ZONE_MAP,
    fileWasUploaded: ByteArray = ByteArray(0),
    withMetadata: Map<String, String> = mapOf(),
  ): UUID? {
    val request = postRequestedFor(urlMatching("/documents/$withType/$withUuid"))
      .withRequestBodyPart(aMultipart("file").withBody(binaryEqualTo(fileWasUploaded)).build())
      .withRequestBodyPart(aMultipart("metadata").withBody(equalToJson(JSONObject(withMetadata).toString())).build())

    verify(didHappenXTimes, request)

    return UUID.fromString(
      findAll(request).first().url.substringAfter("/documents/$withType/"),
    )
  }

  fun verifyDelete(withUuid: String) {
    val request = deleteRequestedFor(urlEqualTo("/documents/$withUuid"))
    verify(request)
  }

  private var happyResponse = """
    {
      "documentUuid": "e2487a03-7cf9-4a9c-85e4-1d51efd7b3f1",
      "documentType": "EXCLUSION_ZONE_MAP",
      "documentFilename": "warrant_for_remand",
      "filename": "warrant_for_remand",
      "fileExtension": "pdf",
      "fileSize": 48243,
      "fileHash": "d58e3582afa99040e27b92b13c8f2280",
      "mimeType": "pdf",
      "metadata": {
        "prisonCode": "KMI",
        "prisonNumber": "C3456DE",
        "court": "Birmingham Magistrates",
        "warrantDate": "2023-11-14"
      },
      "createdTime": "2025-06-03T13:04:03.393Z",
      "createdByServiceName": "Remand and Sentencing",
      "createdByUsername": "AAA01U"
    }
  """.trimMargin()
}
