package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.privateApi

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ControllerAdvice
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.FoundProbationRecord
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ProbationSearchResult
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.UpdateComRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.ProbationUserSearchRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.NotifyService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.StaffService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.communityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ComCaseloadSearchService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate

@ExtendWith(SpringExtension::class)
@ActiveProfiles("test")
@WebMvcTest(controllers = [StaffController::class])
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = [StaffController::class])
@WebAppConfiguration
class StaffControllerTest {

  @MockitoBean
  private lateinit var notifyService: NotifyService

  @MockitoBean
  private lateinit var comCaseloadSearchService: ComCaseloadSearchService

  @MockitoBean
  private lateinit var staffService: StaffService

  @Autowired
  private lateinit var mvc: MockMvc

  @Autowired
  private lateinit var mapper: ObjectMapper

  @BeforeEach
  fun reset() {
    reset(comCaseloadSearchService, notifyService)

    mvc = MockMvcBuilders
      .standaloneSetup(StaffController(comCaseloadSearchService, staffService))
      .setControllerAdvice(ControllerAdvice())
      .build()
  }

  @Test
  fun `update com with new contact details`() {
    val body = UpdateComRequest(
      staffIdentifier = 2000,
      staffUsername = "joebloggs",
      staffEmail = "joebloggs@probation.gov.uk",
      firstName = "Joseph",
      lastName = "Bloggs",
    )

    val expectedCom = communityOffenderManager()
    whenever(staffService.updateComDetails(any())).thenReturn(expectedCom)

    val request = put("/com/update")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .content(mapper.writeValueAsBytes(body))

    mvc.perform(request).andExpect(status().isOk)

    verify(staffService, times(1)).updateComDetails(body)
  }

  @Test
  fun `search for offenders on a given staff member's caseload`() {
    val body = ProbationUserSearchRequest(query = "Test", staffIdentifier = 2000)

    whenever(comCaseloadSearchService.searchForOffenderOnStaffCaseload(any())).thenReturn(aFoundProbationRecord)

    val request = post("/com/case-search")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .content(mapper.writeValueAsBytes(body))

    val result = mvc.perform(request)
      .andExpect(status().isOk)
      .andExpect(content().contentType(MediaType.APPLICATION_JSON))
      .andReturn()

    assertThat(result.response.contentAsString)
      .isEqualTo(mapper.writeValueAsString(aFoundProbationRecord))

    verify(comCaseloadSearchService, times(1)).searchForOffenderOnStaffCaseload(body)
  }

  private companion object {
    val aFoundProbationRecord = ProbationSearchResult(
      listOf(
        FoundProbationRecord(
          name = "Test Surname",
          crn = "CRN1",
          nomisId = "NOMS1",
          comName = "Staff Surname",
          comStaffCode = "A01B02C",
          teamName = "Test Team",
          releaseDate = LocalDate.of(2021, 10, 22),
          licenceId = 1L,
          licenceType = LicenceType.AP,
          licenceStatus = LicenceStatus.IN_PROGRESS,
          isOnProbation = false,
        ),
      ),
      1,
      0,
    )
  }
}
