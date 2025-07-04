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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ControllerAdvice
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ApprovalCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.PrisonCaseAdminSearchResult
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.PrisonUserSearchRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CaseloadService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ApproverCaseloadService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.CaCaseloadService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ComCaseloadService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.VaryApproverCaseloadService

@ExtendWith(SpringExtension::class)
@ActiveProfiles("test")
@WebMvcTest(controllers = [CaseloadController::class])
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = [CaseloadController::class])
@WebAppConfiguration
class CaseloadControllerTest {

  @MockitoBean
  private lateinit var caseloadService: CaseloadService

  @MockitoBean
  private lateinit var approverCaseloadService: ApproverCaseloadService

  @MockitoBean
  private lateinit var caCaseloadService: CaCaseloadService

  @MockitoBean
  private lateinit var comCaseloadService: ComCaseloadService

  @MockitoBean
  private lateinit var varyApproverCaseloadService: VaryApproverCaseloadService

  @Autowired
  private lateinit var mvc: MockMvc

  @Autowired
  private lateinit var mapper: ObjectMapper

  @BeforeEach
  fun reset() {
    reset(caseloadService, approverCaseloadService)

    mvc = MockMvcBuilders
      .standaloneSetup(
        CaseloadController(
          caseloadService,
          approverCaseloadService,
          caCaseloadService,
          comCaseloadService,
          varyApproverCaseloadService,
        ),
      )
      .setControllerAdvice(ControllerAdvice())
      .build()
  }

  @Test
  fun `Get licences for approval`() {
    val request = listOf(
      "MDI",
      "ABC",
    )

    val approvalCase = ApprovalCase(
      licenceId = 1L,
    )

    whenever(approverCaseloadService.getApprovalNeeded(request)).thenReturn(listOf(approvalCase))

    val response = mvc.perform(
      post("/caseload/prison-approver/approval-needed")
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

  @Test
  fun `Get recently approved licences`() {
    val request = listOf(
      "MDI",
      "ABC",
    )

    val approvalCase = ApprovalCase(
      licenceId = 1L,
    )

    whenever(approverCaseloadService.getRecentlyApproved(request)).thenReturn(listOf(approvalCase))

    val response = mvc.perform(
      post("/caseload/prison-approver/recently-approved")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsBytes(request)),
    )
      .andExpect(status().isOk)
      .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
      .andReturn().response.contentAsString

    assertThat(mapper.readValue(response, Array<ApprovalCase>::class.java)).isEqualTo(arrayOf(approvalCase))
    verify(approverCaseloadService, times(1)).getRecentlyApproved(request)
  }

  @Test
  fun `Search for offender on prison case admin caseload`() {
    val request = PrisonUserSearchRequest(
      "ABC",
      setOf("MDI"),
    )

    val result = PrisonCaseAdminSearchResult(listOf(TestData.caCase().copy(prisonerNumber = "ABC")), emptyList())

    whenever(caCaseloadService.searchForOffenderOnPrisonCaseAdminCaseload(request)).thenReturn(result)

    val response = mvc.perform(
      post("/caseload/case-admin/case-search")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsBytes(request)),
    )
      .andExpect(status().isOk)
      .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
      .andReturn().response.contentAsString

    assertThat(mapper.readValue(response, PrisonCaseAdminSearchResult::class.java)).isEqualTo(result)
    verify(caCaseloadService, times(1)).searchForOffenderOnPrisonCaseAdminCaseload(request)
  }
}
