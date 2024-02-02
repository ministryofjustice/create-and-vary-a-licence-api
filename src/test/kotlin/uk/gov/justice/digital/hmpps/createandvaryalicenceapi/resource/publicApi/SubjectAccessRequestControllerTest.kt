package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi

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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ControllerAdvice
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalConditionData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.BespokeCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.StandardCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.Content
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.SarContent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.publicApi.SubjectAccessRequestService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AuditEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.LocalDateTime

@ExtendWith(SpringExtension::class)
@ActiveProfiles("test")
@WebMvcTest(controllers = [SubjectAccessRequestController::class])
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = [SubjectAccessRequestController::class])
@WebAppConfiguration
class SubjectAccessRequestControllerTest {

  @MockBean
  private lateinit var subjectAccessRequestService: SubjectAccessRequestService

  @Autowired
  private lateinit var mvc: MockMvc

  @BeforeEach
  fun reset() {
    reset(subjectAccessRequestService)

    mvc = MockMvcBuilders
      .standaloneSetup(SubjectAccessRequestController(subjectAccessRequestService))
      .setControllerAdvice(ControllerAdvice())
      .build()
  }

  private fun serializedSarContent() =
    this.javaClass.getResourceAsStream("/test_data/sar_content/serializedSarContent.json")!!.bufferedReader(
      StandardCharsets.UTF_8,
    ).readText()

  @Test
  fun `get a Sar Content by id returns ok and have a response`() {
    whenever(subjectAccessRequestService.getSarRecordsById("G4169UO")).thenReturn(sarContentResponse)
    mvc.perform(get("/public/subject-access-request?prn=G4169UO").accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk)
      .andExpect(content().json(serializedSarContent(), true))
      .andReturn()
  }

  @Test
  fun `500 when pass both prn and crn`() {
    val result =
      mvc.perform(get("/public/subject-access-request?prn=G4169UO&crn=Z265290").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isInternalServerError)
        .andReturn()

    assertThat(result.response.contentAsString).contains("Only supports search by single identifier.")

    verify(subjectAccessRequestService, times(0)).getSarRecordsById("G4169UO")
  }

  @Test
  fun `209 when pass crn and but not prn`() {
    val result =
      mvc.perform(get("/public/subject-access-request?crn=Z265290").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().`is`(209))
        .andReturn()

    assertThat(result.response.contentAsString).contains("Search by crn is not supported.")

    verify(subjectAccessRequestService, times(0)).getSarRecordsById("G4169UO")
  }

  @Test
  fun `204 when pass prn but no records found`() {
    whenever(subjectAccessRequestService.getSarRecordsById("G4169UO")).thenReturn(null)
    val result =
      mvc.perform(get("/public/subject-access-request?prn=G4169UO").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNoContent)
        .andReturn()

    assertThat(result.response.contentAsString).contains("No records found for the prn.")

    verify(subjectAccessRequestService, times(1)).getSarRecordsById("G4169UO")
  }

  private companion object {
    val someStandardConditions = listOf(
      StandardCondition(
        id = 1,
        code = "goodBehaviour",
        sequence = 1,
        text = "Be of good behaviour",
      ),
      StandardCondition(
        id = 2,
        code = "notBreakLaw",
        sequence = 1,
        text = "Do not break any law",
      ),
      StandardCondition(
        id = 3,
        code = "attendMeetings",
        sequence = 1,
        text = "Attend meetings",
      ),
    )

    val someAssociationData = listOf(
      AdditionalConditionData(
        id = 1,
        field = "field1",
        value = "value1",
        sequence = 1,
      ),
      AdditionalConditionData(
        id = 2,
        field = "numberOfCurfews",
        value = "value2",
        sequence = 2,
      ),
    )

    val someAdditionalConditions = listOf(
      AdditionalCondition(
        id = 1,
        code = "associateWith",
        sequence = 1,
        text = "Do not associate with [NAME] for a period of [TIME PERIOD]",
        expandedText = "Do not associate with value1 for a period of value2",
        data = someAssociationData,
        readyToSubmit = true,
      ),
    )

    val someBespokeConditions = listOf(
      BespokeCondition(
        id = 1,
        sequence = 1,
        text = "Bespoke one text",
      ),
      BespokeCondition(
        id = 2,
        sequence = 2,
        text = "Bespoke two text",
      ),
    )

    val aListOfModelLicences = listOf(
      CrdLicence(
        id = 1,
        typeCode = LicenceType.AP,
        version = "2.1",
        statusCode = LicenceStatus.IN_PROGRESS,
        nomsId = "A1234AA",
        bookingNo = "123456",
        bookingId = 987654,
        crn = "A12345",
        pnc = "2019/123445",
        cro = "12345",
        prisonCode = "MDI",
        prisonDescription = "Moorland (HMP)",
        forename = "Bob",
        surname = "Mortimer",
        approvedByUsername = "TestApprover",
        approvedDate = LocalDateTime.of(2023, 10, 11, 12, 0),
        dateOfBirth = LocalDate.of(1985, 12, 28),
        conditionalReleaseDate = LocalDate.of(2021, 10, 22),
        actualReleaseDate = LocalDate.of(2021, 10, 22),
        sentenceStartDate = LocalDate.of(2018, 10, 22),
        sentenceEndDate = LocalDate.of(2021, 10, 22),
        licenceStartDate = LocalDate.of(2021, 10, 22),
        licenceExpiryDate = LocalDate.of(2021, 10, 22),
        topupSupervisionStartDate = LocalDate.of(2021, 10, 22),
        topupSupervisionExpiryDate = LocalDate.of(2021, 10, 22),
        dateCreated = LocalDateTime.of(2023, 10, 11, 11, 30),
        dateLastUpdated = LocalDateTime.of(2023, 10, 11, 11, 30),

        comUsername = "X12345",
        comStaffId = 12345,
        comEmail = "stephen.mills@nps.gov.uk",
        probationAreaCode = "N01",
        probationAreaDescription = "Wales",
        probationPduCode = "N01A",
        probationPduDescription = "Cardiff",
        probationLauCode = "N01A2",
        probationLauDescription = "Cardiff South",
        probationTeamCode = "NA01A2-A",
        probationTeamDescription = "Cardiff South Team A",
        createdByUsername = "TestCreator",
        standardLicenceConditions = someStandardConditions,
        standardPssConditions = someStandardConditions,
        additionalLicenceConditions = someAdditionalConditions,
        additionalPssConditions = someAdditionalConditions,
        bespokeConditions = someBespokeConditions,
        licenceVersion = "1.4",
        updatedByUsername = "TestUpdater",
      ),
    )

    val aListOfAuditEvents = listOf(
      AuditEvent(
        id = 1L,
        licenceId = 1L,
        eventTime = LocalDateTime.of(2023, 10, 11, 12, 0),
        username = "USER",
        fullName = "First Last",
        eventType = AuditEventType.USER_EVENT,
        summary = "Summary1",
        detail = "Detail1",
      ),
      AuditEvent(
        id = 2L,
        licenceId = 1L,
        eventTime = LocalDateTime.of(2023, 10, 11, 12, 1),
        username = "USER",
        fullName = "First Last",
        eventType = AuditEventType.USER_EVENT,
        summary = "Summary2",
        detail = "Detail2",
      ),
      AuditEvent(
        id = 3L,
        licenceId = 1L,
        eventTime = LocalDateTime.of(2023, 10, 11, 12, 2),
        username = "CUSER",
        fullName = "First Last",
        eventType = AuditEventType.SYSTEM_EVENT,
        summary = "Summary3",
        detail = "Detail3",
      ),
    )

    val aListOfLicenceEvents = listOf(
      LicenceEvent(
        licenceId = 1,
        eventType = LicenceEventType.SUBMITTED,
        username = "smills",
        forenames = "Stephen",
        surname = "Mills",
        eventDescription = "Licence submitted for approval",
        eventTime = LocalDateTime.of(2023, 10, 11, 12, 3),
      ),
    )

    val sarContentResponse = SarContent(
      Content(
        licences = aListOfModelLicences,
        auditEvents = aListOfAuditEvents,
        licencesEvents = aListOfLicenceEvents,
      ),
    )
  }
}
