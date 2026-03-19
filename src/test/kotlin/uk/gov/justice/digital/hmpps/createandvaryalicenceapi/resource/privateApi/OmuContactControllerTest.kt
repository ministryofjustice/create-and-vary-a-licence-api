package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.privateApi

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ControllerAdvice
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.NotSecuredWebMvcTest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.OmuContact
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateOmuEmailRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.OmuService
import java.time.LocalDateTime

@NotSecuredWebMvcTest(controllers = [OmuContactController::class])
class OmuContactControllerTest {
  @MockitoBean
  private lateinit var omuService: OmuService

  @Autowired
  private lateinit var mvc: MockMvc

  @Autowired
  private lateinit var mapper: ObjectMapper

  @BeforeEach
  fun reset() {
    reset(omuService)

    mvc = MockMvcBuilders
      .standaloneSetup(OmuContactController(omuService))
      .setControllerAdvice(ControllerAdvice())
      .build()
  }

  @Test
  fun `get OMU email contact`() {
    val expectedOmuEmail = OmuContact(email = "test@test.com", prisonCode = "LEI", dateCreated = LocalDateTime.now())
    whenever(omuService.getOmuContactEmail("LEI")).thenReturn(expectedOmuEmail)

    val request = get("/omu/LEI/contact/email")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)

    mvc.perform(request).andExpect(status().isOk)

    verify(omuService, times(1)).getOmuContactEmail("LEI")
  }

  @Test
  fun `update OMU email contact`() {
    val body = UpdateOmuEmailRequest(email = "test@test.com")

    val expectedOmuEmail = OmuContact(email = "test@test.com", prisonCode = "LEI", dateCreated = LocalDateTime.now())
    whenever(omuService.updateOmuEmail("LEI", body)).thenReturn(expectedOmuEmail)

    val request = put("/omu/LEI/contact/email")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .content(mapper.writeValueAsBytes(body))

    mvc.perform(request).andExpect(status().isOk)

    verify(omuService, times(1)).updateOmuEmail("LEI", body)
  }

  @Test
  fun `delete OMU email contact`() {
    val expectedOmuEmail = OmuContact(email = "test@test.com", prisonCode = "LEI", dateCreated = LocalDateTime.now())
    whenever(omuService.getOmuContactEmail("LEI")).thenReturn(expectedOmuEmail)

    val request = delete("/omu/LEI/contact/email")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)

    mvc.perform(request).andExpect(status().isOk)

    verify(omuService, times(1)).deleteOmuEmail("LEI")
  }
}
