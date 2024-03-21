package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aMultipart
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlMatching

class DocumentApiMockServer : WireMockServer(8096) {

  fun stubDocumentUploadRequest(documentType: String, requestPayloadJson: String) {
    stubFor(
      post(urlMatching("/api/documents/$documentType/.*"))
        .withMultipartRequestBody(aMultipart("metadata").withBody(equalToJson(requestPayloadJson)))
        .willReturn(
          aResponse().withHeader(
            "Content-Type",
            "application/json",
          )
            .withBody(
              """{ "documentUuid": "8cdadcf3-b003-4116-9956-c99bd8df6a00" }""",
            ).withStatus(200),
        ),
    )
  }

  fun stubDocumentRetrieval(content: ByteArray) {
    stubFor(
      get(urlMatching("/api/documents/.*?/file"))
        .willReturn(
          aResponse()
            .withBody(content).withStatus(200),
        ),
    )
  }
}
