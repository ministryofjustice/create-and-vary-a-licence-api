package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.documents

import com.github.tomakehurst.wiremock.client.WireMock.aMultipart
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.binaryEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.core.io.ClassPathResource
import org.springframework.web.reactive.function.client.WebClient
import java.util.UUID

class DocumentApiClientUploadTest {
  private lateinit var documentApiClient: DocumentApiClient
  private lateinit var file: ClassPathResource
  private val uuid: UUID = UUID.fromString("e2487a03-7cf9-4a9c-85e4-1d51efd7b3f1")

  private companion object {
    @RegisterExtension
    val wiremock: WireMockExtension = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build()
  }

  @BeforeEach
  fun setup() {
    val webClient: WebClient = WebClient.builder().baseUrl("http://localhost:${wiremock.port}").build()
    documentApiClient = DocumentApiClient(webClient)
    file = ClassPathResource("Test_map_2021-12-06_112550.pdf")
  }

  @AfterEach
  fun verifyApiBeingCalled() {
    wiremock.verify(
      1,
      postRequestedFor(urlEqualTo("/documents/EXCLUSION_ZONE_MAP/$uuid"))
        .withRequestBodyPart(aMultipart("file").withBody(binaryEqualTo(file.contentAsByteArray)).build())
        .withRequestBodyPart(aMultipart("metadata").withBody(equalToJson("""{"aKey":"1","bKey":"2","cKey":"3"}""")).build()),
    )
  }

  @Test
  fun `Returns the document response when successfully uploading a document to the remote service`() {
    givenDocumentApiRespondsWith(status = 201, responseBody = happyResponse)

    val result = uploadDocument()

    assertThat(result)
      .extracting("documentUuid", "documentType")
      .isEqualTo(listOf(uuid, DocumentType.EXCLUSION_ZONE_MAP))
  }

  @ParameterizedTest
  @CsvSource("400", "401", "403", "409", "500")
  fun `Throws an exception when the request is not successful`(responseStatusCode: Int) {
    givenDocumentApiRespondsWith(status = responseStatusCode, responseBody = errorResponse)

    assertThatThrownBy { uploadDocument() }
      .isInstanceOf(IllegalStateException::class.java)
      .hasMessageContaining(
        "Error during uploading document (UUID=%s, StatusCode=%d, Response=%s)".format(
          uuid,
          responseStatusCode,
          errorResponse,
        ),
      )
  }

  @Test
  fun `Throws an exception when the document is not successful and no response could be parsed`() {
    givenDocumentApiRespondsWith(status = 401, responseBody = "")

    assertThatThrownBy { uploadDocument() }
      .isInstanceOf(IllegalStateException::class.java)
      .hasMessageContaining("Error during uploading document (UUID=$uuid)")
  }

  private fun uploadDocument() = documentApiClient.uploadDocument(
    file = file.contentAsByteArray,
    documentUuid = uuid,
    documentType = DocumentType.EXCLUSION_ZONE_MAP,
    metadata = mapOf("aKey" to "1", "bKey" to "2", "cKey" to "3"),
  )

  private var happyResponse = """
    {
      "documentUuid": "$uuid",
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

  private var errorResponse = """
    {
      "userMessage": "string",
      "developerMessage": "string",
      "errorCode": 1073741824,
      "moreInfo": "string",
      "status": 1073741824
    }
  """.trimMargin()

  private fun givenDocumentApiRespondsWith(status: Int = 201, responseBody: String = happyResponse) {
    wiremock.stubFor(
      post(urlEqualTo("/documents/EXCLUSION_ZONE_MAP/$uuid")).willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(responseBody)
          .withStatus(status),
      ),
    )
  }
}
