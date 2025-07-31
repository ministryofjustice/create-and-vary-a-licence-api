package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.workingDays

import com.fasterxml.jackson.annotation.JsonProperty

class BankHoliday(
  @field:JsonProperty("england-and-wales")
  val bankHolidayResult: BankHolidayResult,
)
