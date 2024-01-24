package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.privateApi

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ControllerAdvice
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.BankHolidayService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.ReleaseDateService
import java.time.LocalDate

@ExtendWith(SpringExtension::class)
@ActiveProfiles("test")
@WebMvcTest(controllers = [DatesController::class])
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = [DatesController::class])
@WebAppConfiguration
class DatesControllerTest {

  @MockBean
  private lateinit var bankHolidayService: BankHolidayService

  @MockBean
  private lateinit var releaseDateService: ReleaseDateService

  @Autowired
  private lateinit var mvc: MockMvc

  @BeforeEach
  fun reset() {
    reset(bankHolidayService, releaseDateService)

    mvc = MockMvcBuilders
      .standaloneSetup(DatesController(bankHolidayService, releaseDateService))
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

  @Test
  fun `get hard stop cut-off date for licence to time out`() {
    val expectedCutoffDate = LocalDate.parse("2023-12-05")
    whenever(releaseDateService.getCutOffDateForLicenceTimeOut()).thenReturn(
      expectedCutoffDate,
    )

    val request = get("/current-hard-stop-cutoff-date")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)

    mvc.perform(request).andExpect(status().isOk)
      .andExpect(
        jsonPath("\$.cutoffDate").value("05/12/2023"),
      )

    verify(releaseDateService, times(1)).getCutOffDateForLicenceTimeOut()
  }
}
