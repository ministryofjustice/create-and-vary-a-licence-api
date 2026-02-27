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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ProbationPractitioner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.ApproverSearchRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.PrisonUserSearchRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.ProbationUserSearchRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response.ApproverSearchResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response.ComSearchResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response.FoundComCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CaseService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ApproverCaseloadService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.VaryApproverCaseloadService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ca.CaCaseloadService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.com.ComCaseloadSearchService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.com.ComCreateCaseloadService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.com.ComVaryCaseloadService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.request.VaryApproverCaseloadSearchRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.response.VaryApproverCaseloadSearchResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate

@ExtendWith(SpringExtension::class)
@ActiveProfiles("test")
@WebMvcTest(controllers = [CaseloadController::class])
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = [CaseloadController::class])
@WebAppConfiguration
class CaseloadControllerTest {

  @MockitoBean
  private lateinit var caseService: CaseService

  @MockitoBean
  private lateinit var approverCaseloadService: ApproverCaseloadService

  @MockitoBean
  private lateinit var caCaseloadService: CaCaseloadService

  @MockitoBean
  private lateinit var comCreateCaseloadService: ComCreateCaseloadService

  @MockitoBean
  private lateinit var comVaryCaseloadService: ComVaryCaseloadService

  @MockitoBean
  private lateinit var varyApproverCaseloadService: VaryApproverCaseloadService

  @MockitoBean
  private lateinit var comCaseloadSearchService: ComCaseloadSearchService

  @Autowired
  private lateinit var mvc: MockMvc

  @Autowired
  private lateinit var mapper: ObjectMapper

  @BeforeEach
  fun reset() {
    reset(caseService, approverCaseloadService, comCaseloadSearchService)

    mvc = MockMvcBuilders
      .standaloneSetup(
        CaseloadController(
          caseService,
          approverCaseloadService,
          caCaseloadService,
          comCreateCaseloadService,
          comVaryCaseloadService,
          varyApproverCaseloadService,
          comCaseloadSearchService,
        ),
      )
      .setControllerAdvice(ControllerAdvice())
      .build()
  }

  @Test
  fun `Get licences for approval`() {
    // Given
    val request = listOf(
      "MDI",
      "ABC",
    )

    val approvalCase = ApprovalCase(
      licenceId = 1L,
      probationPractitioner = ProbationPractitioner(allocated = true),
    )

    whenever(approverCaseloadService.getApprovalNeeded(request)).thenReturn(listOf(approvalCase))

    // When
    val result = mvc.perform(
      post("/caseload/prison-approver/approval-needed")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsBytes(request)),
    )

    // Then
    result
      .andExpect(status().isOk)
      .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))

    val response = result.andReturn().response.contentAsString
    assertThat(mapper.readValue(response, Array<ApprovalCase>::class.java)).isEqualTo(arrayOf(approvalCase))
    verify(approverCaseloadService, times(1)).getApprovalNeeded(request)
  }

  @Test
  fun `Get recently approved licences`() {
    // Given
    val request = listOf(
      "MDI",
      "ABC",
    )

    val approvalCase = ApprovalCase(
      licenceId = 1L,
      probationPractitioner = ProbationPractitioner(allocated = true),
    )

    whenever(approverCaseloadService.getRecentlyApproved(request)).thenReturn(listOf(approvalCase))

    // When
    val result = mvc.perform(
      post("/caseload/prison-approver/recently-approved")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsBytes(request)),
    )

    // Then
    result
      .andExpect(status().isOk)
      .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))

    val response = result.andReturn().response.contentAsString
    assertThat(mapper.readValue(response, Array<ApprovalCase>::class.java)).isEqualTo(arrayOf(approvalCase))
    verify(approverCaseloadService, times(1)).getRecentlyApproved(request)
  }

  @Test
  fun `Search for offender on prison case admin caseload`() {
    // Given
    val request = PrisonUserSearchRequest(
      "ABC",
      setOf("MDI"),
    )

    val result =
      PrisonCaseAdminSearchResult(listOf(TestData.caCase().copy(prisonerNumber = "ABC")), emptyList(), emptyList())

    whenever(caCaseloadService.searchForOffenderOnPrisonCaseAdminCaseload(request)).thenReturn(result)

    // When
    val mvcResult = mvc.perform(
      post("/caseload/case-admin/case-search")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsBytes(request)),
    )

    // Then
    mvcResult
      .andExpect(status().isOk)
      .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))

    val response = mvcResult.andReturn().response.contentAsString
    assertThat(mapper.readValue(response, PrisonCaseAdminSearchResult::class.java)).isEqualTo(result)
    verify(caCaseloadService, times(1)).searchForOffenderOnPrisonCaseAdminCaseload(request)
  }

  @Test
  fun `Search for offender on approver caseload`() {
    // Given
    val request = ApproverSearchRequest(
      listOf("MDI"),
      "ABC",
    )

    val result = ApproverSearchResponse(listOf(TestData.approvalCase().copy(prisonerNumber = "ABC")), emptyList())

    whenever(approverCaseloadService.searchForOffenderOnApproverCaseload(request)).thenReturn(result)

    // When
    val mvcResult = mvc.perform(
      post("/caseload/prison-approver/case-search")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsBytes(request)),
    )

    // Then
    mvcResult
      .andExpect(status().isOk)
      .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))

    val response = mvcResult.andReturn().response.contentAsString
    assertThat(mapper.readValue(response, ApproverSearchResponse::class.java)).isEqualTo(result)
    verify(approverCaseloadService, times(1)).searchForOffenderOnApproverCaseload(request)
  }

  @Test
  fun `Search for offender on vary approver caseload`() {
    // Given
    val request = VaryApproverCaseloadSearchRequest(
      listOf("N55PDV"),
      null,
      "ABC",
    )

    val result =
      VaryApproverCaseloadSearchResponse(listOf(TestData.varyApprovalCase().copy(crnNumber = "ABC")), emptyList())

    whenever(varyApproverCaseloadService.searchForOffenderOnVaryApproverCaseload(request)).thenReturn(result)

    // When
    val mvcResult = mvc.perform(
      post("/caseload/vary-approver/case-search")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsBytes(request)),
    )

    // Then
    mvcResult
      .andExpect(status().isOk)
      .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))

    val response = mvcResult.andReturn().response.contentAsString
    assertThat(mapper.readValue(response, VaryApproverCaseloadSearchResponse::class.java)).isEqualTo(result)
    verify(varyApproverCaseloadService, times(1)).searchForOffenderOnVaryApproverCaseload(request)
  }

  @Test
  fun `Search for offenders on a given staff member's caseload`() {
    // Given
    val body = ProbationUserSearchRequest(query = "Test", staffIdentifier = 2000)

    whenever(comCaseloadSearchService.searchForOffenderOnProbationUserCaseload(body)).thenReturn(aFoundProbationRecord)

    val request = post("/caseload/com/case-search")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .content(mapper.writeValueAsBytes(body))

    // When
    val mvcResult = mvc.perform(request)

    // Then
    mvcResult
      .andExpect(status().isOk)
      .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))

    val result = mvcResult.andReturn()
    assertThat(result.response.contentAsString)
      .isEqualTo(mapper.writeValueAsString(aFoundProbationRecord))

    verify(comCaseloadSearchService, times(1)).searchForOffenderOnProbationUserCaseload(body)
  }

  private companion object {
    val aFoundProbationRecord = ComSearchResponse(
      listOf(
        FoundComCase(
          kind = LicenceKind.CRD,
          name = "Test Surname",
          crn = "CRN1",
          nomisId = "NOMS1",
          comName = "Staff Surname",
          comStaffCode = "A01B02C",
          probationPractitioner = ProbationPractitioner("A01B02C", "Staff Surname", true),
          teamName = "Test Team",
          releaseDate = LocalDate.of(2021, 10, 22),
          licenceId = 1L,
          licenceType = LicenceType.AP,
          licenceStatus = LicenceStatus.IN_PROGRESS,
          isOnProbation = false,
          isRestricted = false,
        ),
      ),
      1,
      0,
    )
  }
}
