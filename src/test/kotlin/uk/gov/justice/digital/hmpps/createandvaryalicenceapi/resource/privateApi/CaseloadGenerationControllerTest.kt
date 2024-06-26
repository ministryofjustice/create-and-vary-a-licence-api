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
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ControllerAdvice
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ApprovalCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ApproverCaseloadService

@ExtendWith(SpringExtension::class)
@ActiveProfiles("test")
@WebMvcTest(controllers = [CaseloadGenerationController::class])
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = [CaseloadGenerationController::class])
@WebAppConfiguration
class CaseloadGenerationControllerTest {

  @MockBean
  private lateinit var approverCaseloadService: ApproverCaseloadService

  @Autowired
  private lateinit var mvc: MockMvc

  @Autowired
  private lateinit var mapper: ObjectMapper

  @BeforeEach
  fun reset() {
    reset(approverCaseloadService)

    mvc = MockMvcBuilders
      .standaloneSetup(CaseloadGenerationController(approverCaseloadService))
      .setControllerAdvice(ControllerAdvice())
      .build()
  }

  @Test
  fun `get licences for approval`() {
    val request = listOf(
      "MDI",
      "ABC",
    )

    val approvalCase = ApprovalCase(
      licenceId = 1L,
    )

    whenever(approverCaseloadService.getApprovalNeeded(request)).thenReturn(listOf(approvalCase))

    val response = mvc.perform(
      post("/caseload/get-approval-needed")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsBytes(request)),
    )
      .andExpect(status().isOk)
      .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
      .andReturn().response.contentAsString

    assertThat(mapper.readValue(response, Array<ApprovalCase>::class.java)).isEqualTo(arrayOf(approvalCase))
    verify(approverCaseloadService, times(1)).getApprovalNeeded(request)
  }
}
