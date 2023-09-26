package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.gov

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.gov.bankHolidays.BankHoliday
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.gov.bankHolidays.BankHolidayEvent

@Service
class GovUkApiClient(@Qualifier("govUkWebClient") val govUkApiClient: WebClient) {

  fun getBankHolidaysForEnglandAndWales(): List<BankHolidayEvent> {
    val response = govUkApiClient
      .get()
      .uri("/bank-holidays.json")
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono(BankHoliday::class.java)
      .block()

    return response?.bankHolidayResult?.events ?: error("Unexpected null response from API")
  }
}
