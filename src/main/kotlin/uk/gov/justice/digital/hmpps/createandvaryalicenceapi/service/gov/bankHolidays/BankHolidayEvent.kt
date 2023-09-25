package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.gov.bankHolidays

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDate

class BankHolidayEvent(
  @JsonFormat(pattern = "yyyy-MM-dd")
  val date: LocalDate,
)
