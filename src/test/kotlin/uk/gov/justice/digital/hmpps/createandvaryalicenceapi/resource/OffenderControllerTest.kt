package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource

import com.fasterxml.jackson.databind.ObjectMapper
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ControllerAdvice
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.UpdateResponsibleComRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.OffenderService

@ExtendWith(SpringExtension::class)
@ActiveProfiles("test")
@WebMvcTest(controllers = [OffenderController::class])
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = [OffenderController::class])
@WebAppConfiguration
class OffenderControllerTest {

  @MockBean
  private lateinit var offenderService: OffenderService

  @Autowired
  private lateinit var mvc: MockMvc

  @Autowired
  private lateinit var mapper: ObjectMapper

  @BeforeEach
  fun reset() {
    reset(offenderService)

    mvc = MockMvcBuilders
      .standaloneSetup(OffenderController(offenderService))
      .setControllerAdvice(ControllerAdvice())
      .build()
  }

  @Test
  fun `update offender with new offender manager`() {
    val body = UpdateResponsibleComRequest(staffIdentifier = 2000, staffUsername = "joebloggs", staffEmail = "joebloggs@probation.gov.uk")
    val request = put("/offender/crn/exampleCrn/responsible-com")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .content(mapper.writeValueAsBytes(body))

    mvc.perform(request).andExpect(status().isOk)

    verify(offenderService, times(1)).updateOffenderWithResponsibleCom("exampleCrn", body)
  }
}
