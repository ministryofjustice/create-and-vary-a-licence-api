package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.documents

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.delete
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.web.reactive.function.client.WebClient
import java.util.UUID

class DocumentApiClientDeleteTest {

  private lateinit var documentApiClient: DocumentApiClient
  private val uuid: UUID = UUID.fromString("425a4764-99fb-4874-bc3f-278fe5ac9f91")
  private val documentFile = byteArrayOf(1, 2, 3, 4)

  private companion object {
    @RegisterExtension
    val wiremock: WireMockExtension = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build()
  }

  @BeforeEach
  fun setup() {
    val webClient: WebClient = WebClient.builder().baseUrl("http://localhost:${wiremock.port}").build()
    documentApiClient = DocumentApiClient(webClient)
  }

  @Test
  fun `deletes the requested document file`() {
    givenDocumentApiRespondsWith(status = 204)
    assertDoesNotThrow { documentApiClient.deleteDocument(uuid) }
  }

  @ParameterizedTest
  @CsvSource("400", "401", "403")
  fun `Throws an exception when the request is not successful`(responseStatusCode: Int) {
    givenDocumentApiRespondsWith(status = responseStatusCode, responseBody = errorResponse)

    assertThatThrownBy { documentApiClient.deleteDocument(uuid) }
      .isInstanceOf(IllegalStateException::class.java)
      .hasMessageContaining(
        "Error deleting document (UUID=%s, StatusCode=%d, Response=%s)".format(
          uuid,
          responseStatusCode,
          errorResponse,
        ),
      )
  }

  private var errorResponse = """
    {
      "userMessage": "string",
      "developerMessage": "string",
      "errorCode": 1073741824,
      "moreInfo": "string",
      "status": 1073741824
    }
  """.trimMargin()

  private fun givenDocumentApiRespondsWith(status: Int = 200, responseBody: String = "") {
    wiremock.stubFor(
      delete(urlEqualTo("/documents/$uuid")).willReturn(
        aResponse()
          .withHeader("Content-Type", "application/pdf")
          .withBody(responseBody)
          .withStatus(status),
      ),
    )
  }
}
