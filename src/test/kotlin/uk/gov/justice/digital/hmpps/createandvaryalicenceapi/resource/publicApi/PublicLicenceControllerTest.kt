package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.hamcrest.Matchers.contains
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
import org.springframework.core.io.ClassPathResource
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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.ApConditions
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.BespokeCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.Conditions
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.LicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.LicenceType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.PssConditions
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.additionalConditions.GenericAdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licencePolicy.StandardCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.PolicyVersion
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.publicApi.PublicLicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.time.LocalDate
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
  fun `get a licence by id returns ok and have a response`() {
    val result = mvc.perform(get("/public/licences/id/1234").accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk)
      .andReturn()

    assertThat(result.response.contentAsString)
      .isEqualTo("")
  }

  @Test
  fun `get licences by prison number`() {
    whenever(publicLicenceService.getAllLicencesByPrisonNumber("A1234AA")).thenReturn(listOf(aLicenceSummary))

    val result = mvc.perform(get("/public/licence-summaries/prison-number/A1234AA").accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk)
      .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
      .andExpect(
        jsonPath(
          "\$..createdDateTime",
          contains("2023-10-11T11:00:00"),
        ),
      )
      .andExpect(
        jsonPath(
          "\$..approvedDateTime",
          contains("2023-10-11T12:00:00"),
        ),
      )
      .andExpect(
        jsonPath(
          "\$..updatedDateTime",
          contains("2023-10-11T11:30:00"),
        ),
      )
      .andReturn()

    assertThat(result.response.contentAsString)
      .isEqualTo(mapper.writeValueAsString(listOf(aLicenceSummary)))

    verify(publicLicenceService, times(1)).getAllLicencesByPrisonNumber("A1234AA")
  }

  @Test
  fun `get licences by crn`() {
    whenever(publicLicenceService.getAllLicencesByCrn("A12345")).thenReturn(listOf(aLicenceSummary))

    val result = mvc.perform(get("/public/licence-summaries/crn/A12345").accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk)
      .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
      .andExpect(
        jsonPath(
          "\$..createdDateTime",
          contains("2023-10-11T11:00:00"),
        ),
      )
      .andExpect(
        jsonPath(
          "\$..approvedDateTime",
          contains("2023-10-11T12:00:00"),
        ),
      )
      .andExpect(
        jsonPath(
          "\$..updatedDateTime",
          contains("2023-10-11T11:30:00"),
        ),
      )
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

  @Test
  fun `get a full-size image for an exclusion zone`() {
    whenever(publicLicenceService.getImageUpload(1, 1)).thenReturn(aFullSizeMapImage)

    val result = mvc.perform(get("/public/licences/1/conditions/1/image-upload").accept(MediaType.IMAGE_JPEG))
      .andExpect(status().isOk)
      .andExpect(MockMvcResultMatchers.content().contentType(MediaType.IMAGE_JPEG))
      .andReturn()

    assertThat(result.response.contentAsByteArray).isEqualTo(aFullSizeMapImage)

    verify(publicLicenceService, times(1)).getImageUpload(1, 1)
  }

  @Test
  fun `get licence by licence id`() {
    val id: Long = 12345
    whenever(publicLicenceService.getLicenceById(id)).thenReturn(licence)

    val result = mvc.perform(get("/public/licences/id/12345").accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk)
      .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
      .andExpect(
        jsonPath(
          "\$..createdDateTime",
          contains("2023-10-11T11:00:00"),
        ),
      )
      .andExpect(
        jsonPath(
          "\$..approvedDateTime",
          contains("2023-10-11T12:00:00"),
        ),
      )
      .andExpect(
        jsonPath(
          "\$..updatedDateTime",
          contains("2023-10-11T11:30:00"),
        ),
      )
      .andReturn()

    assertThat(result.response.contentAsString)
      .isEqualTo(mapper.writeValueAsString(licence))

    verify(publicLicenceService, times(1)).getLicenceById(12345)
  }

  private companion object {
    private val bespokeCondition = listOf(BespokeCondition("You should not visit Y"))
    private val standardConditions = listOf(StandardCondition("fda24aa9-a2b0-4d49-9c87-23b0a7be4013", " as reasonably required by your supervisor, to give a sample of oral fluid"))
    private val additionalConditions = listOf(
      GenericAdditionalCondition(
        type = "STANDARD",
        id = 3568,
        category = "Drug testing",
        code = "fda24aa9-a2b0-4d49-9c87-23b0a7be4013",
        text = "Attend [INSERT NAME AND ADDRESS], as reasonably required by your supervisor, to give a sample of oral fluid / urine in order to test whether you have any specified Class A or specified Class B drugs in your body, for the purpose of ensuring that you are complying with the requirement of your supervision period requiring you to be of good behaviour.",

      ),
    )
    private val pssConditions = PssConditions(standardConditions, additionalConditions)
    private val apConditions = ApConditions(
      standard = standardConditions,
      additional = additionalConditions,
      bespoke = bespokeCondition,
    )
    val licenceConditions = Conditions(
      apConditions,
      pssConditions,
    )

    val aLicenceSummary = LicenceSummary(
      id = 1,
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

    val aFullSizeMapImage = ClassPathResource("test_map.jpg").inputStream.readAllBytes()
    val licence = Licence(
      id = 1,
      licenceType = LicenceType.AP,
      policyVersion = PolicyVersion.V1_0,
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
      licenceStartDate = LocalDate.of(2023, 10, 11),
      isInPssPeriod = false,
      conditions = licenceConditions,
    )
  }
}
