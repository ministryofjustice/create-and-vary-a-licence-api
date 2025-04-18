package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.CacheManager
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.GovUkMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.workingDays.GovUkApiClient
import java.time.LocalDate

class BankHolidaysIntegrationTest : IntegrationTestBase() {

  @MockitoSpyBean
  private lateinit var govUkApiClient: GovUkApiClient

  @Autowired
  private lateinit var cacheManager: CacheManager

  @BeforeEach
  @AfterEach
  fun clearCache() {
    cacheManager.getCache("bank-holidays")?.clear()
  }

  @Test
  fun `retrieve bank holidays for England and Wales`() {
    govUkApiMockServer.stubGetBankHolidaysForEnglandAndWales()
    val resultList = webTestClient.get()
      .uri("/bank-holidays")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(LocalDate::class.java)
      .returnResult().responseBody!!

    assertThat(resultList).isEqualTo(
      listOf(
        LocalDate.parse("2018-01-01"),
        LocalDate.parse("2018-03-30"),
        LocalDate.parse("2018-04-02"),
        LocalDate.parse("2018-05-07"),
      ),
    )
  }

  @Test
  fun `cached version of bank holidays are returned when retrieving the bank holidays a second time`() {
    govUkApiMockServer.stubGetBankHolidaysForEnglandAndWales()
    var resultList = webTestClient.get()
      .uri("/bank-holidays")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(LocalDate::class.java)
      .returnResult().responseBody!!

    assertThat(resultList).hasSize(4)

    resultList = webTestClient.get()
      .uri("/bank-holidays")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(LocalDate::class.java)
      .returnResult().responseBody!!

    assertThat(resultList).hasSize(4)

    verify(govUkApiClient, times(1)).getBankHolidaysForEnglandAndWales()
  }

  private companion object {
    val govUkApiMockServer = GovUkMockServer()

    @JvmStatic
    @BeforeAll
    fun startMocks() {
      govUkApiMockServer.start()
    }

    @JvmStatic
    @AfterAll
    fun stopMocks() {
      govUkApiMockServer.stop()
    }
  }
}
