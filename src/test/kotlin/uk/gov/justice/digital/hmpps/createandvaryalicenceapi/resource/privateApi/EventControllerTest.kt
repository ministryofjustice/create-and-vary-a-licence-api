package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.privateApi

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.AssertionsForClassTypes
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ControllerAdvice
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.EventQueryObject
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.EventService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceEventType
import java.time.LocalDateTime

@ExtendWith(SpringExtension::class)
@ActiveProfiles("test")
@WebMvcTest(controllers = [EventController::class])
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = [EventController::class])
@WebAppConfiguration
class EventControllerTest {

  @MockitoBean
  private lateinit var eventService: EventService

  @Autowired
  private lateinit var mvc: MockMvc

  @Autowired
  private lateinit var mapper: ObjectMapper

  @BeforeEach
  fun reset() {
    reset(eventService)

    mvc = MockMvcBuilders
      .standaloneSetup(EventController(eventService))
      .setControllerAdvice(ControllerAdvice())
      .build()
  }

  @Test
  fun `match events by licence ID and event type`() {
    val eventQueryObject = EventQueryObject(licenceId = 1, eventTypes = listOf(LicenceEventType.SUBMITTED))
    whenever(eventService.findEventsMatchingCriteria(eventQueryObject)).thenReturn(listOf(aLicenceEvent))

    val result = mvc.perform(
      MockMvcRequestBuilders.get("/events/match?licenceId=1&eventType=SUBMITTED").accept(MediaType.APPLICATION_JSON),
    )
      .andExpect(MockMvcResultMatchers.status().isOk)
      .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
      .andReturn()

    AssertionsForClassTypes.assertThat(result.response.contentAsString)
      .isEqualTo(mapper.writeValueAsString(listOf(aLicenceEvent)))

    verify(eventService, times(1)).findEventsMatchingCriteria(eventQueryObject)
  }

  private companion object {
    val aLicenceEvent = LicenceEvent(
      licenceId = 1,
      eventType = LicenceEventType.SUBMITTED,
      username = "smills",
      forenames = "Stephen",
      surname = "Mills",
      eventDescription = "Licence submitted for approval",
      eventTime = LocalDateTime.now(),
    )
  }
}
