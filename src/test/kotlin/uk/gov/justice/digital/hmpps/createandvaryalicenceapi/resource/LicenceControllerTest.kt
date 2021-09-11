package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource

import com.fasterxml.jackson.databind.ObjectMapper
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.reset
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ControllerAdvice
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalConditionData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.BespokeCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CreateLicenceRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CreateLicenceResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.StandardCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate
import java.time.LocalDateTime
import javax.persistence.EntityNotFoundException
import javax.validation.ValidationException

@ExtendWith(SpringExtension::class)
@ActiveProfiles("test")
@WebMvcTest(controllers = [LicenceController::class])
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = [LicenceController::class])
@WebAppConfiguration
class LicenceControllerTest {

  @MockBean
  private lateinit var licenceService: LicenceService

  @Autowired
  private lateinit var mvc: MockMvc

  @Autowired
  private lateinit var mapper: ObjectMapper

  @BeforeEach
  fun reset() {
    reset(licenceService)

    mvc = MockMvcBuilders
      .standaloneSetup(LicenceController(licenceService))
      .setControllerAdvice(ControllerAdvice())
      .build()
  }

  @Test
  fun `get a licence by id`() {
    whenever(licenceService.getLicenceById(1)).thenReturn(aLicence)

    val result = mvc.perform(get("/licence/id/1").accept(APPLICATION_JSON))
      .andExpect(status().isOk)
      .andExpect(content().contentType(APPLICATION_JSON))
      .andReturn()

    assertThat(result.response.contentAsString)
      .isEqualTo(mapper.writeValueAsString(aLicence))

    verify(licenceService, times(1)).getLicenceById(1)
  }

  @Test
  fun `404 licence not found`() {
    whenever(licenceService.getLicenceById(1)).thenThrow(EntityNotFoundException(""))

    val result = mvc.perform(get("/licence/id/1").accept(APPLICATION_JSON))
      .andExpect(status().isNotFound)
      .andExpect(content().contentType(APPLICATION_JSON))
      .andReturn()

    assertThat(result.response.contentAsString).contains("Not found")

    verify(licenceService, times(1)).getLicenceById(1)
  }

  @Test
  fun `create a licence`() {
    whenever(licenceService.createLicence(aCreateLicenceRequest)).thenReturn(aCreateLicenceResponse)

    val result = mvc.perform(
      post("/licence/create")
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        .content(mapper.writeValueAsBytes(aCreateLicenceRequest))
    )
      .andExpect(status().isOk)
      .andExpect(content().contentType(APPLICATION_JSON))
      .andReturn()

    assertThat(result.response.contentAsString).isEqualTo(mapper.writeValueAsString(aCreateLicenceResponse))

    verify(licenceService, times(1)).createLicence(aCreateLicenceRequest)
  }

  @Test
  fun `create a licence where another is in progress`() {
    whenever(licenceService.createLicence(aCreateLicenceRequest))
      .thenThrow(ValidationException("A licence already exists for this person"))

    val result = mvc.perform(
      post("/licence/create")
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        .content(mapper.writeValueAsBytes(aCreateLicenceRequest))
    )
      .andExpect(status().is4xxClientError)
      .andExpect(content().contentType(APPLICATION_JSON))
      .andReturn()

    assertThat(result.response.contentAsString).contains("A licence already exists for this person")

    verify(licenceService, times(1)).createLicence(aCreateLicenceRequest)
  }

  private companion object {

    val someStandardConditions = listOf(
      StandardCondition(id = 1, code = "goodBehaviour", sequence = 1, text = "Be of good behaviour"),
      StandardCondition(id = 2, code = "notBreakLaw", sequence = 1, text = "Do not break any law"),
      StandardCondition(id = 3, code = "attendMeetings", sequence = 1, text = "Attend meetings"),
    )

    val someAssociationData = listOf(
      AdditionalConditionData(id = 1, sequence = 1, description = "association", "TEXT", "Peter Smith"),
      AdditionalConditionData(id = 2, sequence = 2, description = "howLong", "TEXT", "6 months"),
    )

    val someAdditionalConditions = listOf(
      AdditionalCondition(
        id = 1,
        code = "associateWith",
        sequence = 1,
        text = "Do not associate with [NAME] for a period of [TIME PERIOD]",
        data = someAssociationData,
      ),
    )

    val someBespokeConditions = listOf(
      BespokeCondition(id = 1, sequence = 1, text = "Bespoke one text"),
      BespokeCondition(id = 2, sequence = 2, text = "Bespoke two text"),
    )

    val aLicence = Licence(
      id = 1,
      typeCode = LicenceType.AP,
      version = "1.1",
      statusCode = LicenceStatus.IN_PROGRESS,
      nomsId = "A1234AA",
      bookingNo = "123456",
      bookingId = 54321,
      crn = "X12345",
      pnc = "2019/123445",
      cro = "12345",
      prisonCode = "MDI",
      prisonDescription = "Moorland (HMP)",
      forename = "Bob",
      surname = "Mortimer",
      dateOfBirth = LocalDate.of(1985, 12, 28),
      conditionalReleaseDate = LocalDate.of(2021, 10, 22),
      actualReleaseDate = LocalDate.of(2021, 10, 22),
      sentenceStartDate = LocalDate.of(2018, 10, 22),
      sentenceEndDate = LocalDate.of(2021, 10, 22),
      licenceStartDate = LocalDate.of(2021, 10, 22),
      licenceExpiryDate = LocalDate.of(2021, 10, 22),
      comFirstName = "Stephen",
      comLastName = "Mills",
      comUsername = "X12345",
      comStaffId = 12345,
      comEmail = "stephen.mills@nps.gov.uk",
      comTelephone = "0116 2788777",
      probationAreaCode = "N01",
      probationLduCode = "LDU1",
      dateCreated = LocalDateTime.now(),
      createByUsername = "X12345",
      standardConditions = someStandardConditions,
      additionalConditions = someAdditionalConditions,
      bespokeConditions = someBespokeConditions,
    )

    val aCreateLicenceRequest = CreateLicenceRequest(
      typeCode = LicenceType.AP,
      version = "1.4",
      nomsId = "NOMSID",
      bookingNo = "BOOKINGNO",
      bookingId = 1L,
      crn = "CRN1",
      pnc = "PNC1",
      cro = "CRO1",
      prisonCode = "MDI",
      prisonDescription = "Moorland (HMP)",
      forename = "Mike",
      surname = "Myers",
      dateOfBirth = LocalDate.of(2001, 10, 1),
      conditionalReleaseDate = LocalDate.of(2021, 10, 22),
      actualReleaseDate = LocalDate.of(2021, 10, 22),
      sentenceStartDate = LocalDate.of(2018, 10, 22),
      sentenceEndDate = LocalDate.of(2021, 10, 22),
      licenceStartDate = LocalDate.of(2021, 10, 22),
      licenceExpiryDate = LocalDate.of(2021, 10, 22),
      comFirstName = "Stephen",
      comLastName = "Mills",
      comUsername = "X12345",
      comStaffId = 12345,
      comEmail = "stephen.mills@nps.gov.uk",
      comTelephone = "0116 2788777",
      probationAreaCode = "N01",
      probationLduCode = "LDU1",
      standardConditions = someStandardConditions.map { it.text!! }
    )

    val aCreateLicenceResponse = CreateLicenceResponse(
      licenceId = 99,
      licenceType = LicenceType.AP,
      licenceStatus = LicenceStatus.IN_PROGRESS,
    )
  }

  // Other tests possible
  // - create a licence with an invalid type
  // - create a PSS licence type
}
