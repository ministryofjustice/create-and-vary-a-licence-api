package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.privateApi

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ControllerAdvice
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.NotSecuredWebMvcTest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.CurfewTimeRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.FirstNightCurfewTimeRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateFirstNightCurfewTimesRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateWeeklyCurfewTimesRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.HdcService
import java.time.DayOfWeek
import java.time.LocalTime

@NotSecuredWebMvcTest(controllers = [HdcCurfewTimesController::class])
class HdcCurfewTimesControllerTest {

  @MockitoBean
  private lateinit var hdcService: HdcService

  @Autowired
  private lateinit var mvc: MockMvc

  @Autowired
  private lateinit var mapper: ObjectMapper

  @BeforeEach
  fun reset() {
    reset(hdcService)

    mvc = MockMvcBuilders
      .standaloneSetup(HdcCurfewTimesController(hdcService))
      .setControllerAdvice(ControllerAdvice())
      .build()
  }

  @Test
  fun `update HDC curfew times by licence ID`() {
    mvc.perform(
      put("/licence/id/123456/hdc-weekly-curfew-times")
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        .content(mapper.writeValueAsBytes(anWeeklyCurfewTimesRequest)),
    )
      .andExpect(status().isOk)

    verify(hdcService, times(1)).updateWeeklyCurfewTimes(123456, anWeeklyCurfewTimesRequest)
  }

  @Test
  fun `update HDC first night curfew times by licence ID`() {
    mvc.perform(
      put("/licence/id/123456/hdc-first-night-curfew-times")
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        .content(mapper.writeValueAsBytes(anFirstNightCurfewTimesRequest)),
    )
      .andExpect(status().isOk)

    verify(hdcService, times(1)).updateFirstNightCurfewTimes(123456, anFirstNightCurfewTimesRequest)
  }

  private companion object {
    val anWeeklyCurfewTimesRequest = UpdateWeeklyCurfewTimesRequest(
      listOf(
        CurfewTimeRequest(
          1,
          DayOfWeek.MONDAY,
          LocalTime.of(20, 0),
          DayOfWeek.TUESDAY,
        ),
        CurfewTimeRequest(
          2,
          DayOfWeek.TUESDAY,
          LocalTime.of(20, 0),
          DayOfWeek.WEDNESDAY,
          LocalTime.of(8, 0),
        ),
        CurfewTimeRequest(
          3,
          DayOfWeek.WEDNESDAY,
          LocalTime.of(20, 0),
          DayOfWeek.THURSDAY,
          LocalTime.of(8, 0),
        ),
        CurfewTimeRequest(
          4,
          DayOfWeek.THURSDAY,
          LocalTime.of(20, 0),
          DayOfWeek.FRIDAY,
          LocalTime.of(8, 0),
        ),
        CurfewTimeRequest(
          5,
          DayOfWeek.FRIDAY,
          LocalTime.of(20, 0),
          DayOfWeek.SATURDAY,
          LocalTime.of(8, 0),
        ),
        CurfewTimeRequest(
          6,
          DayOfWeek.SATURDAY,
          LocalTime.of(20, 0),
          DayOfWeek.SUNDAY,
          LocalTime.of(8, 0),
        ),
        CurfewTimeRequest(
          7,
          DayOfWeek.SUNDAY,
          LocalTime.of(20, 0),
          DayOfWeek.MONDAY,
          LocalTime.of(8, 0),
        ),
      ),
    )

    val anFirstNightCurfewTimesRequest = UpdateFirstNightCurfewTimesRequest(
      FirstNightCurfewTimeRequest(
        fromTime = LocalTime.of(20, 0),
        untilTime = LocalTime.of(8, 0),
      ),
    )
  }
}
