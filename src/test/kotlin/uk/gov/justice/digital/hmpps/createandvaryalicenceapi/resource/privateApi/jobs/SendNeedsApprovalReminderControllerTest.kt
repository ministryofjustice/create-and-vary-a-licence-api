package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.privateApi.jobs

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ControllerAdvice
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.NotSecuredWebMvcTest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs.SendNeedsApprovalReminderService

@NotSecuredWebMvcTest(controllers = [SendNeedsApprovalReminderController::class])
class SendNeedsApprovalReminderControllerTest {
  @MockitoBean
  private lateinit var sendNeedsApprovalReminderService: SendNeedsApprovalReminderService

  @Autowired
  private lateinit var mvc: MockMvc

  @BeforeEach
  fun reset() {
    reset(sendNeedsApprovalReminderService)

    mvc = MockMvcBuilders
      .standaloneSetup(SendNeedsApprovalReminderController(sendNeedsApprovalReminderService))
      .setControllerAdvice(ControllerAdvice())
      .build()
  }

  @Test
  fun `send email to probation practitioner`() {
    mvc.perform(
      post("/jobs/notify-probation-of-unapproved-licences")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON),
    )
      .andExpect(status().isOk)
    verify(sendNeedsApprovalReminderService, times(1)).sendEmailsToProbationPractitioner()
  }
}
