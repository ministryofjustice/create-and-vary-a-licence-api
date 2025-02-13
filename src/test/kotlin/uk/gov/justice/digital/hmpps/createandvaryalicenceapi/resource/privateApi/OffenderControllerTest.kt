package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.privateApi

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ControllerAdvice
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.UpdateComRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateOffenderDetailsRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateProbationTeamRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.OffenderService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.PrisonerSearchService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.StaffService
import java.time.LocalDate

@ExtendWith(SpringExtension::class)
@ActiveProfiles("test")
@WebMvcTest(controllers = [OffenderController::class])
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = [OffenderController::class])
@WebAppConfiguration
class OffenderControllerTest {

  @MockitoBean
  private lateinit var offenderService: OffenderService

  @MockitoBean
  private lateinit var prisonerSearchService: PrisonerSearchService

  @MockitoBean
  private lateinit var staffService: StaffService

  @Autowired
  private lateinit var mvc: MockMvc

  @Autowired
  private lateinit var mapper: ObjectMapper

  @BeforeEach
  fun reset() {
    reset(offenderService, prisonerSearchService, staffService)

    mvc = MockMvcBuilders
      .standaloneSetup(OffenderController(offenderService, prisonerSearchService, staffService))
      .setControllerAdvice(ControllerAdvice())
      .build()
  }

  @Test
  fun `update offender with new offender manager`() {
    val body = UpdateComRequest(
      staffIdentifier = 2000,
      staffUsername = "joebloggs",
      staffEmail = "joebloggs@probation.gov.uk",
      firstName = "Joseph",
      lastName = "Bloggs",
    )

    val expectedCom = CommunityOffenderManager(
      staffIdentifier = 2000,
      username = "joebloggs",
      email = "joebloggs@probation.gov.uk",
      firstName = "Joseph",
      lastName = "Bloggs",
    )
    whenever(staffService.updateComDetails(any())).thenReturn(expectedCom)

    val request = put("/offender/crn/exampleCrn/responsible-com")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .content(mapper.writeValueAsBytes(body))

    mvc.perform(request).andExpect(status().isOk)

    verify(staffService, times(1)).updateComDetails(body)
    verify(offenderService, times(1)).updateOffenderWithResponsibleCom("exampleCrn", expectedCom)
  }

  @Test
  fun `update offender with new probation region`() {
    val body = UpdateProbationTeamRequest(
      probationAreaCode = "N02",
      probationPduCode = "PDU2",
      probationLauCode = "LAU2",
      probationTeamCode = "TEAM2",
    )

    val request = put("/offender/crn/exampleCrn/probation-team")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .content(mapper.writeValueAsBytes(body))

    mvc.perform(request).andExpect(status().isOk)

    verify(offenderService, times(1)).updateProbationTeam("exampleCrn", body)
  }

  @Test
  fun `update offender details`() {
    val body = UpdateOffenderDetailsRequest(
      forename = "Alex",
      middleNames = "Brian Cameron",
      surname = "David-Edgar",
      dateOfBirth = LocalDate.parse("1970-01-01"),
    )

    val request = put("/offender/nomisid/exampleNomisId/update-offender-details")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .content(mapper.writeValueAsBytes(body))

    mvc.perform(request).andExpect(status().isOk)

    verify(offenderService, times(1)).updateOffenderDetails("exampleNomisId", body)
  }

  @Test
  fun `get ineligibility reasons`() {
    whenever(prisonerSearchService.getIneligibilityReasons("A1234AA")).thenReturn(listOf("A Reason"))

    val request = get("/offender/nomisid/A1234AA/ineligibility-reasons")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)

    val response = mvc.perform(request).andExpect(status().isOk).andReturn().response.contentAsString

    assertThat(mapper.readValue(response, Array<String>::class.java)).isEqualTo(arrayOf("A Reason"))
  }

  @Test
  fun `get is-91 status`() {
    whenever(prisonerSearchService.getIS91Status("A1234AA")).thenReturn(true)

    val request = get("/offender/nomisid/A1234AA/is-91-status")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)

    val response = mvc.perform(request).andExpect(status().isOk).andReturn().response.contentAsString

    assertThat(mapper.readValue(response, String::class.java)).isEqualTo("true")
  }
}
