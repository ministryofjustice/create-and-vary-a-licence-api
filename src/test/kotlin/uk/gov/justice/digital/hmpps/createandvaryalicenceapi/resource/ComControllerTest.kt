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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ControllerAdvice
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.UpdateComRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.ComService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.NotifyService

@ExtendWith(SpringExtension::class)
@ActiveProfiles("test")
@WebMvcTest(controllers = [ComController::class])
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = [ComController::class])
@WebAppConfiguration
class ComControllerTest {

  @MockBean
  private lateinit var notifyService: NotifyService

  @MockBean
  private lateinit var comService: ComService

  @Autowired
  private lateinit var mvc: MockMvc

  @Autowired
  private lateinit var mapper: ObjectMapper

  @BeforeEach
  fun reset() {
    reset(comService, notifyService)

    mvc = MockMvcBuilders
      .standaloneSetup(ComController(comService, notifyService))
      .setControllerAdvice(ControllerAdvice())
      .build()
  }

  @Test
  fun `update com with new contact details`() {
    val body = UpdateComRequest(staffIdentifier = 2000, staffUsername = "joebloggs", staffEmail = "joebloggs@probation.gov.uk", firstName = "Joseph", lastName = "Bloggs")

    val expectedCom = CommunityOffenderManager(staffIdentifier = 2000, username = "joebloggs", email = "joebloggs@probation.gov.uk", firstName = "Joseph", lastName = "Bloggs")
    whenever(comService.updateComDetails(any())).thenReturn(expectedCom)

    val request = put("/com/update")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .content(mapper.writeValueAsBytes(body))

    mvc.perform(request).andExpect(status().isOk)

    verify(comService, times(1)).updateComDetails(body)
  }
}
