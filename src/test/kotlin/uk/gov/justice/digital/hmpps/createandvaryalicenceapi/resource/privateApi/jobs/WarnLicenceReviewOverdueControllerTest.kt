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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ControllerAdvice
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.NotSecuredWebMvcTest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs.LicenceReviewOverdueService

@NotSecuredWebMvcTest(controllers = [WarnLicenceReviewOverdueController::class])
class WarnLicenceReviewOverdueControllerTest {
  @MockitoBean
  private lateinit var licenceReviewOverdueService: LicenceReviewOverdueService

  @Autowired
  private lateinit var mvc: MockMvc

  @BeforeEach
  fun reset() {
    reset(licenceReviewOverdueService)

    mvc = MockMvcBuilders
      .standaloneSetup(WarnLicenceReviewOverdueController(licenceReviewOverdueService))
      .setControllerAdvice(ControllerAdvice())
      .build()
  }

  @Test
  fun `send email to COM`() {
    mvc.perform(
      MockMvcRequestBuilders.post("/jobs/warn-licence-review-overdue")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON),
    )
      .andExpect(MockMvcResultMatchers.status().isOk)
    verify(licenceReviewOverdueService, times(1)).sendComReviewEmail()
  }
}
