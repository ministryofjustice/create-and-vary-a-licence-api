package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.cache.CacheManager
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.GovUkMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.gov.GovUkApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.gov.bankHolidays.BankHolidayEvent
import java.time.LocalDate

class BankHolidaysIntegrationTest : IntegrationTestBase() {

  @SpyBean
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
      .expectBodyList(BankHolidayEvent::class.java)
      .returnResult().responseBody

    assertThat(resultList?.size).isEqualTo(4)
    assertThat(resultList)
      .extracting<Tuple> {
        Tuple.tuple(it.date)
      }
      .contains(
        Tuple.tuple(LocalDate.parse("2018-01-01")),
        Tuple.tuple(LocalDate.parse("2018-03-30")),
        Tuple.tuple(LocalDate.parse("2018-04-02")),
        Tuple.tuple(LocalDate.parse("2018-05-07")),
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
      .expectBodyList(BankHolidayEvent::class.java)
      .returnResult().responseBody

    assertThat(resultList?.size).isEqualTo(4)

    resultList = webTestClient.get()
      .uri("/bank-holidays")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(BankHolidayEvent::class.java)
      .returnResult().responseBody

    assertThat(resultList?.size).isEqualTo(4)

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
