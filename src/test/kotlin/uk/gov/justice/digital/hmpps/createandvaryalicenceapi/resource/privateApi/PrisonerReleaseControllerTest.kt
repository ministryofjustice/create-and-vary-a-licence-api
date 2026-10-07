package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.privateApi

import com.fasterxml.jackson.databind.ObjectMapper
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ControllerAdvice
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.NotSecuredWebMvcTest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.privateApi.PrisonerReleaseController.PrisonerToReleaseRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.PrisonerReleasedHandler

@NotSecuredWebMvcTest(controllers = [PrisonerReleaseController::class])
class PrisonerReleaseControllerTest {

  @MockitoBean
  private lateinit var prisonerReleasedHandler: PrisonerReleasedHandler

  @Autowired
  private lateinit var mvc: MockMvc

  @Autowired
  private lateinit var mapper: ObjectMapper

  @BeforeEach
  fun reset() {
    reset(prisonerReleasedHandler)

    mvc = MockMvcBuilders
      .standaloneSetup(PrisonerReleaseController(prisonerReleasedHandler, handlerEnabled = false))
      .setControllerAdvice(ControllerAdvice())
      .build()
  }

  @Test
  fun `Trigger release process`() {
    mvc.perform(
      post("/licence/trigger-release-prisoner")
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        .content(mapper.writeValueAsBytes(PrisonerToReleaseRequest(prisonNumber = "A1234AA"))),
    )
      .andExpect(status().isOk)

    verify(prisonerReleasedHandler, times(1)).processLicencesOnRelease("A1234AA")
  }

  @Test
  fun `Bad request when no prison number provided`() {
    mvc.perform(
      post("/licence/trigger-release-prisoner")
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        .content("{}"),
    )
      .andExpect(status().isBadRequest)
      .andExpect(jsonPath("$.developerMessage").value(containsString("must not be blank")))
  }

  @Test
  fun `Request not processed when handler disabled `() {
    mvc = MockMvcBuilders
      .standaloneSetup(PrisonerReleaseController(prisonerReleasedHandler, handlerEnabled = true))
      .setControllerAdvice(ControllerAdvice())
      .build()

    mvc.perform(
      post("/licence/trigger-release-prisoner")
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        .content(mapper.writeValueAsBytes(PrisonerToReleaseRequest(prisonNumber = "A1234AA"))),
    )
      .andExpect(status().isOk)

    verifyNoInteractions(prisonerReleasedHandler)
  }
}
