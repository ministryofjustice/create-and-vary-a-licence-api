package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.privateApi

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ControllerAdvice
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.privateApi.jobs.SendNeedsApprovalReminderController
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.UnapprovedLicenceService

@ExtendWith(SpringExtension::class)
@ActiveProfiles("test")
@WebMvcTest(controllers = [SendNeedsApprovalReminderController::class])
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = [SendNeedsApprovalReminderController::class])
@WebAppConfiguration
class SendNeedsApprovalReminderControllerTest {
  @MockBean
  private lateinit var unapprovedLicenceService: UnapprovedLicenceService

  @Autowired
  private lateinit var mvc: MockMvc

  @BeforeEach
  fun reset() {
    reset(unapprovedLicenceService)

    mvc = MockMvcBuilders
      .standaloneSetup(SendNeedsApprovalReminderController(unapprovedLicenceService))
      .setControllerAdvice(ControllerAdvice())
      .build()
  }

  @Test
  fun `send email to probation practitioner`() {
    mvc.perform(
      MockMvcRequestBuilders.post("/notify-probation-of-unapproved-licences")
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON),
    )
      .andExpect(status().isOk)
    verify(unapprovedLicenceService, times(1)).sendEmailsToProbationPractitioner()
  }
}
