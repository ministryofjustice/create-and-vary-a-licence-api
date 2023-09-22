package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.gov.bankHolidays

import com.fasterxml.jackson.annotation.JsonProperty

class BankHoliday(
  @JsonProperty("england-and-wales")
  val bankHolidayResult: BankHolidayResult,
)
