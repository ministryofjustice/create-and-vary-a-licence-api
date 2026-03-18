package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.privateApi

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ControllerAdvice
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.NotSecuredWebMvcTest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.workingDays.BankHolidayService
import java.time.LocalDate

@NotSecuredWebMvcTest(controllers = [DatesController::class])
class DatesControllerTest {

  @MockitoBean
  private lateinit var bankHolidayService: BankHolidayService

  @Autowired
  private lateinit var mvc: MockMvc

  @BeforeEach
  fun reset() {
    reset(bankHolidayService)

    mvc = MockMvcBuilders
      .standaloneSetup(DatesController(bankHolidayService))
      .setControllerAdvice(ControllerAdvice())
      .build()
  }

  @Test
  fun `retrieve bank holidays for England and Wales`() {
    val expectedBankHolidays = listOf(
      LocalDate.parse("2023-09-21"),
    )
    whenever(bankHolidayService.getBankHolidaysForEnglandAndWales()).thenReturn(expectedBankHolidays)

    val request = get("/bank-holidays")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)

    mvc.perform(request).andExpect(status().isOk)

    verify(bankHolidayService, times(1)).getBankHolidaysForEnglandAndWales()
  }
}
