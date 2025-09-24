package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.privateApi

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
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
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ControllerAdvice
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.timeServed.TimeServedCaseload
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.timeServed.TimeServedCaseloadService

@ExtendWith(SpringExtension::class)
@ActiveProfiles("test")
@WebMvcTest(controllers = [TimeServedCasesController::class])
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = [TimeServedCasesController::class])
@WebAppConfiguration
class TimeServedCasesControllerTest {

  @MockitoBean
  private lateinit var timeServedCaseloadService: TimeServedCaseloadService

  @Autowired
  private lateinit var mvc: MockMvc

  @Autowired
  private lateinit var mapper: ObjectMapper

  @BeforeEach
  fun reset() {
    reset(timeServedCaseloadService)

    mvc = MockMvcBuilders
      .standaloneSetup(
        TimeServedCasesController(
          timeServedCaseloadService,
        ),
      )
      .setControllerAdvice(ControllerAdvice())
      .build()
  }

  @Test
  fun `Get timeserved cases`() {
    val result =
      TimeServedCaseload(emptyList(), emptyList())

    whenever(timeServedCaseloadService.getCases("MDI")).thenReturn(result)

    val response = mvc.perform(
      post("/cases/time-served/MDI")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON),
    )
      .andExpect(status().isOk)
      .andExpect(content().contentType(MediaType.APPLICATION_JSON))
      .andReturn().response.contentAsString

    assertThat(mapper.readValue(response, TimeServedCaseload::class.java)).isEqualTo(result)
    verify(timeServedCaseloadService, times(1)).getCases("MDI")
  }
}
