package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.privateApi

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ControllerAdvice
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.NotSecuredWebMvcTest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.privateApi.timeserved.TimeServedCasesController
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.timeServed.TimeServedCaseload
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.timeServed.TimeServedCaseloadService

@NotSecuredWebMvcTest(controllers = [TimeServedCasesController::class])
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
