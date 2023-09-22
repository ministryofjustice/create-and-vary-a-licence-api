package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource

import com.fasterxml.jackson.databind.ObjectMapper
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
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ControllerAdvice
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AuditRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.privateApi.AuditController
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.AuditService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AuditEventType
import java.time.LocalDateTime

@ExtendWith(SpringExtension::class)
@ActiveProfiles("test")
@WebMvcTest(controllers = [AuditController::class])
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = [AuditController::class])
@WebAppConfiguration
class AuditControllerTest {

  @MockBean
  private lateinit var auditService: AuditService

  @Autowired
  private lateinit var mvc: MockMvc

  @Autowired
  private lateinit var mapper: ObjectMapper

  @BeforeEach
  fun reset() {
    reset(auditService)

    mvc = MockMvcBuilders
      .standaloneSetup(AuditController(auditService))
      .setControllerAdvice(ControllerAdvice())
      .build()
  }

  @Test
  fun `record an audit event`() {
    val body = AuditEvent(
      licenceId = 1L,
      eventTime = LocalDateTime.now(),
      username = "USER",
      fullName = "Forename Surname",
      eventType = AuditEventType.USER_EVENT,
      summary = "Summary description",
      detail = "Detail description",
    )

    mvc.put("/audit/save") {
      accept = MediaType.APPLICATION_JSON
      content = mapper.writeValueAsBytes(body)
      contentType = MediaType.APPLICATION_JSON
    }.andExpect {
      status { isOk() }
    }
    verify(auditService, times(1)).recordAuditEvent(any())
  }

  @Test
  fun `query for audit events for a licence`() {
    val body = AuditRequest(
      licenceId = 1L,
      startTime = LocalDateTime.now().minusMonths(1),
      endTime = LocalDateTime.now(),
    )

    whenever(auditService.getAuditEvents(body)).thenReturn(aListOfAuditEvents)

    mvc.post("/audit/retrieve") {
      accept = MediaType.APPLICATION_JSON
      content = mapper.writeValueAsString(body)
      contentType = MediaType.APPLICATION_JSON
    }.andExpect {
      status { isOk() }
      content { contentType(MediaType.APPLICATION_JSON) }
      content { mapper.writeValueAsString(aListOfAuditEvents) }
    }

    verify(auditService, times(1)).getAuditEvents(any())
  }

  @Test
  fun `query for audit events for a user`() {
    val body = AuditRequest(
      username = "USER",
      startTime = LocalDateTime.now().minusMonths(1),
      endTime = LocalDateTime.now(),
    )

    whenever(auditService.getAuditEvents(body)).thenReturn(aListOfAuditEvents)

    mvc.post("/audit/retrieve") {
      accept = MediaType.APPLICATION_JSON
      content = mapper.writeValueAsString(body)
      contentType = MediaType.APPLICATION_JSON
    }.andExpect {
      status { isOk() }
      content { contentType(MediaType.APPLICATION_JSON) }
      content { mapper.writeValueAsString(aListOfAuditEvents) }
    }

    verify(auditService, times(1)).getAuditEvents(any())
  }

  companion object {
    val aListOfAuditEvents = listOf(
      AuditEvent(
        id = 1L,
        licenceId = 1L,
        eventTime = LocalDateTime.now().minusDays(1L),
        username = "USER",
        fullName = "First Last",
        eventType = AuditEventType.USER_EVENT,
        summary = "Summary1",
        detail = "Detail1",
      ),
      AuditEvent(
        id = 2L,
        licenceId = 1L,
        eventTime = LocalDateTime.now().minusDays(2L),
        username = "USER",
        fullName = "First Last",
        eventType = AuditEventType.USER_EVENT,
        summary = "Summary2",
        detail = "Detail2",
      ),
      AuditEvent(
        id = 3L,
        licenceId = 1L,
        eventTime = LocalDateTime.now().minusDays(3L),
        username = "CUSER",
        fullName = "First Last",
        eventType = AuditEventType.SYSTEM_EVENT,
        summary = "Summary3",
        detail = "Detail3",
      ),
    )
  }
}
