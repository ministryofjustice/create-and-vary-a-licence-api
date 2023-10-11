package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.hamcrest.Matchers.`is`
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ControllerAdvice
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.LicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.LicenceType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.PublicLicenceService
import java.time.LocalDateTime

@ExtendWith(SpringExtension::class)
@ActiveProfiles("test")
@WebMvcTest(controllers = [PublicLicenceController::class])
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = [PublicLicenceController::class])
@WebAppConfiguration
class PublicLicenceControllerTest {

  @MockBean
  private lateinit var publicLicenceService: PublicLicenceService

  @Autowired
  private lateinit var mvc: MockMvc

  @Autowired
  private lateinit var mapper: ObjectMapper

  @BeforeEach
  fun reset() {
    reset(publicLicenceService)

    mvc = MockMvcBuilders
      .standaloneSetup(PublicLicenceController(publicLicenceService))
      .setControllerAdvice(ControllerAdvice())
      .build()
  }

  @Test
  fun `get a licence by id`() {
    val result = mvc.perform(get("/public/licences/id/1234").accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk)
      .andReturn()

    assertThat(result.response.contentAsString)
      .isEqualTo("")
  }

  @Test
  fun `get licences by prison number`() {
    mvc.perform(get("/public/licences/prison-number/A1234AA").accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk)
      .andExpect(jsonPath("$", `is`(emptyList<Any>())))
  }

  @Test
  fun `get licences by crn`() {
    whenever(publicLicenceService.getAllLicencesByCrn("A12345")).thenReturn(listOf(aLicenceSummary))

    val result = mvc.perform(get("/public/licence-summaries/crn/A12345").accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk)
      .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
      .andReturn()

    assertThat(result.response.contentAsString)
      .isEqualTo(mapper.writeValueAsString(listOf(aLicenceSummary)))

    verify(publicLicenceService, times(1)).getAllLicencesByCrn("A12345")
  }

  @Test
  fun `404 licence not found by crn`() {
    whenever(publicLicenceService.getAllLicencesByCrn("A12345")).thenThrow(EntityNotFoundException(""))

    val result = mvc.perform(get("/public/licence-summaries/crn/A12345").accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isNotFound)
      .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
      .andReturn()

    assertThat(result.response.contentAsString).contains("Not found")

    verify(publicLicenceService, times(1)).getAllLicencesByCrn("A12345")
  }

  private companion object {
    val aLicenceSummary = LicenceSummary(
      id = 1,
      licenceType = LicenceType.AP,
      policyVersion = "2.1",
      version = "1.4",
      statusCode = LicenceStatus.IN_PROGRESS,
      prisonNumber = "A1234AA",
      bookingId = 987654L,
      crn = "A12345",
      approvedByUsername = "TestApprover",
      approvedDateTime = LocalDateTime.of(2023, 10, 11, 12, 0, 0),
      createdByUsername = "TestCreator",
      createdDateTime = LocalDateTime.of(2023, 10, 11, 11, 0, 0),
      updatedByUsername = "TestUpdater",
      updatedDateTime = LocalDateTime.of(2023, 10, 11, 11, 30, 0),
      isInPssPeriod = false,
    )
  }
}
