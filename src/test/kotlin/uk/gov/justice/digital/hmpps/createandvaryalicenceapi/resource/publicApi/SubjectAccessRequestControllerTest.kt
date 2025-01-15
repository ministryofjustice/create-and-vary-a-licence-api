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
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.json.JsonCompareMode.STRICT
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ControllerAdvice
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.LicenceEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest.Content
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest.SarContent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest.transformToSarAuditEvents
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest.transformToSarLicenceEvents
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.publicApi.SubjectAccessRequestService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AuditEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceEventType
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime

@ExtendWith(SpringExtension::class)
@ActiveProfiles("test")
@WebMvcTest(controllers = [SubjectAccessRequestController::class])
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = [SubjectAccessRequestController::class])
@WebAppConfiguration
class SubjectAccessRequestControllerTest {

  @MockitoBean
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

  private fun serializedSarContent(licence: String) =
    this.javaClass.getResourceAsStream("/test_data/sar_content/$licence.json")!!.bufferedReader(
      StandardCharsets.UTF_8,
    ).readText()

  @Test
  fun `get a Subject Access Request Content for CRD Licence`() {
    val licences = listOf(TestData.createSarLicence())
    val auditEvents = aListOfAuditEvents.transformToSarAuditEvents()
    val licencesEvents = aListOfLicenceEvents.transformToSarLicenceEvents()
    whenever(subjectAccessRequestService.getSarRecordsById("G4169UO")).thenReturn(
      SarContent(
        Content(
          licences,
          auditEvents,
          licencesEvents,
        ),
      ),
    )

    mvc.perform(get("/subject-access-request?prn=G4169UO").accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk)
      .andExpect(content().json(serializedSarContent("sarLicence"), STRICT))
      .andReturn()
  }

  @Test
  fun `500 when pass both prn and crn`() {
    val result =
      mvc.perform(get("/subject-access-request?prn=G4169UO&crn=Z265290").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isInternalServerError)
        .andReturn()

    assertThat(result.response.contentAsString).contains("Only supports search by single identifier.")

    verify(subjectAccessRequestService, times(0)).getSarRecordsById("G4169UO")
  }

  @Test
  fun `209 when pass crn and but not prn`() {
    val result =
      mvc.perform(get("/subject-access-request?crn=Z265290").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().`is`(209))
        .andReturn()

    assertThat(result.response.contentAsString).contains("Search by crn is not supported.")

    verify(subjectAccessRequestService, times(0)).getSarRecordsById("G4169UO")
  }

  @Test
  fun `204 when pass prn but no records found`() {
    whenever(subjectAccessRequestService.getSarRecordsById("G4169UO")).thenReturn(null)
    val result =
      mvc.perform(get("/subject-access-request?prn=G4169UO").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNoContent)
        .andReturn()

    assertThat(result.response.contentAsString).contains("No records found for the prn.")

    verify(subjectAccessRequestService, times(1)).getSarRecordsById("G4169UO")
  }

  private companion object {
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
  }
}
