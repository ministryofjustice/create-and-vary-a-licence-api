package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.privateApi.jobs

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
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ControllerAdvice
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs.InactivateRecallLicencesService

@ExtendWith(SpringExtension::class)
@ActiveProfiles("test")
@WebMvcTest(controllers = [InactivateRecallLicencesController::class])
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = [InactivateRecallLicencesController::class])
@WebAppConfiguration
class InactivateRecallLicencesControllerTest {
  @MockBean
  private lateinit var inactivateRecallLicencesService: InactivateRecallLicencesService

  @Autowired
  private lateinit var mvc: MockMvc

  @BeforeEach
  fun reset() {
    reset(inactivateRecallLicencesService)

    mvc = MockMvcBuilders
      .standaloneSetup(InactivateRecallLicencesController(inactivateRecallLicencesService))
      .setControllerAdvice(ControllerAdvice())
      .build()
  }

  @Test
  fun `inactivate licences`() {
    mvc.perform(
      MockMvcRequestBuilders.post("/run-inactivate-recall-licences-job")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON),
    )
      .andExpect(MockMvcResultMatchers.status().isOk)
    verify(inactivateRecallLicencesService, times(1)).inactivateLicences()
  }
}
