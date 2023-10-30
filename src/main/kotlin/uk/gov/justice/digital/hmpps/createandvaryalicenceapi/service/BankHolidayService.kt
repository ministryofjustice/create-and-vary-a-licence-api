package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.gov.GovUkApiClient

@Service
class BankHolidayService(
  private val govUkApiClient: GovUkApiClient,
) {

  @Cacheable("bank-holidays")
  fun getBankHolidaysForEnglandAndWales() = govUkApiClient.getBankHolidaysForEnglandAndWales()
}
