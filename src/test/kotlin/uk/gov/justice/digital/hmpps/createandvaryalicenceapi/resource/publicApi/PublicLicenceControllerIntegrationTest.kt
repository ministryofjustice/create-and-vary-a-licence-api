package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.nullValue
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
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.MediaType.IMAGE_JPEG
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.json.JsonCompareMode.STRICT
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ControllerAdvice
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.LicenceType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.PublicLicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createCrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.someModelAdditionalConditions
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.PolicyVersion
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.publicApi.PublicLicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.toCrd
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.transformToPublicLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.nio.charset.StandardCharsets.UTF_8
import java.time.LocalDate
import java.time.LocalDateTime

@ExtendWith(SpringExtension::class)
@ActiveProfiles("test")
@WebMvcTest(controllers = [PublicLicenceController::class])
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = [PublicLicenceController::class])
@WebAppConfiguration
class PublicLicenceControllerIntegrationTest {

  @MockitoBean
  private lateinit var publicLicenceService: PublicLicenceService

  @Autowired
  private lateinit var mvc: MockMvc

  @BeforeEach
  fun reset() {
    reset(publicLicenceService)

    mvc = MockMvcBuilders
      .standaloneSetup(PublicLicenceController(publicLicenceService))
      .setControllerAdvice(ControllerAdvice())
      .build()
  }

  @Test
  fun `get a licence by id returns ok and have a response`() {
    whenever(publicLicenceService.getLicenceById(1234)).thenReturn(aLicence)

    mvc.perform(get("/public/licences/id/1234").accept(APPLICATION_JSON))
      .andExpect(status().isOk)
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(content().json(serialisedContent("licence-by-id"), STRICT))
      .andReturn()
  }

  @Test
  fun `get a licence by id handles missing created dates`() {
    whenever(publicLicenceService.getLicenceById(1234)).thenReturn(aLicence.copy(createdDateTime = null))

    mvc.perform(get("/public/licences/id/1234").accept(APPLICATION_JSON))
      .andExpect(status().isOk)
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(jsonPath("\$..createdDateTime", contains(nullValue())))
      .andReturn()
  }

  @Test
  fun `get licences by prison number`() {
    whenever(publicLicenceService.getAllLicencesByPrisonNumber("A1234AA")).thenReturn(listOf(aLicenceSummary))

    mvc.perform(get("/public/licence-summaries/prison-number/A1234AA").accept(APPLICATION_JSON))
      .andExpect(status().isOk)
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(jsonPath("\$..createdDateTime", contains("2023-10-11T11:00:00")))
      .andExpect(jsonPath("\$..approvedDateTime", contains("2023-10-11T12:00:00")))
      .andExpect(jsonPath("\$..updatedDateTime", contains("2023-10-11T11:30:00")))
      .andExpect(content().json(serialisedContent("licences"), STRICT))
      .andReturn()

    verify(publicLicenceService, times(1)).getAllLicencesByPrisonNumber("A1234AA")
  }

  @Test
  fun `get licences by crn`() {
    whenever(publicLicenceService.getAllLicencesByCrn("A12345")).thenReturn(listOf(aLicenceSummary))

    mvc.perform(get("/public/licence-summaries/crn/A12345").accept(APPLICATION_JSON))
      .andExpect(status().isOk)
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(jsonPath("\$..createdDateTime", contains("2023-10-11T11:00:00")))
      .andExpect(jsonPath("\$..approvedDateTime", contains("2023-10-11T12:00:00")))
      .andExpect(jsonPath("\$..updatedDateTime", contains("2023-10-11T11:30:00")))
      .andExpect(content().json(serialisedContent("licences"), STRICT))
      .andReturn()

    verify(publicLicenceService, times(1)).getAllLicencesByCrn("A12345")
  }

  @Test
  fun `handles null created date`() {
    val licence = aLicenceSummary.copy(createdDateTime = null)
    whenever(publicLicenceService.getAllLicencesByCrn("A12345")).thenReturn(listOf(licence))

    mvc.perform(get("/public/licence-summaries/crn/A12345").accept(APPLICATION_JSON))
      .andExpect(status().isOk)
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(jsonPath("\$..createdDateTime", contains(nullValue())))
      .andReturn()

    verify(publicLicenceService, times(1)).getAllLicencesByCrn("A12345")
  }

  @Test
  fun `404 licence not found by crn`() {
    whenever(publicLicenceService.getAllLicencesByCrn("A12345")).thenThrow(EntityNotFoundException(""))

    mvc.perform(get("/public/licence-summaries/crn/A12345").accept(APPLICATION_JSON))
      .andExpect(status().isNotFound)
      .andExpect(content().contentType(APPLICATION_JSON))
      .andReturn()

    verify(publicLicenceService, times(1)).getAllLicencesByCrn("A12345")
  }

  @Test
  fun `get a full-size image for an exclusion zone`() {
    val aFullSizeMapImage = this::class.java.getResourceAsStream("/test_map.jpg")!!.readAllBytes()

    whenever(publicLicenceService.getImageUpload(1, 1)).thenReturn(aFullSizeMapImage)

    val result = mvc.perform(get("/public/licences/1/conditions/1/image-upload").accept(IMAGE_JPEG))
      .andExpect(status().isOk)
      .andExpect(content().contentType(IMAGE_JPEG))
      .andReturn()

    assertThat(result.response.contentAsByteArray).isEqualTo(aFullSizeMapImage)

    verify(publicLicenceService, times(1)).getImageUpload(1, 1)
  }

  private companion object {
    val aLicence = toCrd(
      licence = createCrdLicence().copy(id = 1234, version = "2.1"),
      earliestReleaseDate = LocalDate.of(2024, 1, 3),
      isEligibleForEarlyRelease = true,
      hardStopDate = LocalDate.of(2024, 1, 1),
      hardStopWarningDate = LocalDate.of(2023, 12, 28),
      isInHardStopPeriod = true,
      isDueToBeReleasedInTheNextTwoWorkingDays = true,
      conditionPolicyData = emptyMap(),
    ).copy(additionalLicenceConditions = someModelAdditionalConditions()).transformToPublicLicence()

    val aLicenceSummary = PublicLicenceSummary(
      id = 1,
      kind = LicenceKind.CRD,
      licenceType = LicenceType.AP,
      policyVersion = PolicyVersion.V2_0,
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

    private fun serialisedContent(name: String) = this::class.java.getResourceAsStream("/test_data/public_api/$name.json")!!.bufferedReader(
      UTF_8,
    ).readText()
  }
}
