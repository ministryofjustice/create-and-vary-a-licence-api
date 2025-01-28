package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.workingDays

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.time.LocalDate

@Service
class GovUkApiClient(@Qualifier("govUkWebClient") val govUkApiClient: WebClient) {

  fun getBankHolidaysForEnglandAndWales(): List<LocalDate> {
    val response = govUkApiClient
      .get()
      .uri("/bank-holidays.json")
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono(BankHoliday::class.java)
      .block()

    return response?.bankHolidayResult?.events?.map { it.date }
      ?: error("Unexpected null response from API")
  }
}
