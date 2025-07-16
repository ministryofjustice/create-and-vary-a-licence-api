package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.documents

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.web.reactive.function.client.WebClient
import java.util.UUID

class DocumentApiClientDownloadTest {

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
  fun `returns the requested document file as a ByteArray`() {
    givenDocumentApiRespondsWith(status = 200)
    assertThat(documentApiClient.downloadDocumentFile(uuid)).isEqualTo(documentFile)
  }

  @ParameterizedTest
  @CsvSource("400", "401", "403", "409", "500")
  fun `Throws an exception when the request is not successful`(responseStatusCode: Int) {
    givenDocumentApiRespondsWith(status = responseStatusCode, responseBody = errorResponse)

    assertThatThrownBy { documentApiClient.downloadDocumentFile(uuid) }
      .isInstanceOf(IllegalStateException::class.java)
      .hasMessageContaining(
        "Error downloading document (UUID=%s, StatusCode=%d, Response=%s)".format(
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

  private fun givenDocumentApiRespondsWith(status: Int = 200, responseBody: String = documentFile.toString(Charsets.UTF_8)) {
    wiremock.stubFor(
      get(urlEqualTo("/documents/$uuid/file")).willReturn(
        aResponse()
          .withHeader("Content-Type", "application/pdf")
          .withBody(responseBody)
          .withStatus(status),
      ),
    )
  }
}
