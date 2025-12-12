package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.privateApi

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
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ControllerAdvice
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.UpdateComRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.NotifyService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.StaffService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.communityOffenderManager

@ExtendWith(SpringExtension::class)
@ActiveProfiles("test")
@WebMvcTest(controllers = [StaffController::class])
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = [StaffController::class])
@WebAppConfiguration
class StaffControllerTest {

  @MockitoBean
  private lateinit var notifyService: NotifyService

  @MockitoBean
  private lateinit var staffService: StaffService

  @Autowired
  private lateinit var mvc: MockMvc

  @Autowired
  private lateinit var mapper: ObjectMapper

  @BeforeEach
  fun reset() {
    reset(notifyService, staffService)

    mvc = MockMvcBuilders
      .standaloneSetup(StaffController(staffService))
      .setControllerAdvice(ControllerAdvice())
      .build()
  }

  @Test
  fun `update com with new contact details`() {
    // Given
    val body = UpdateComRequest(
      staffIdentifier = 2000,
      staffUsername = "joebloggs",
      staffEmail = "joebloggs@probation.gov.uk",
      firstName = "Joseph",
      lastName = "Bloggs",
    )

    val expectedCom = communityOffenderManager()
    whenever(staffService.updateComDetails(any())).thenReturn(expectedCom)

    val request = put("/com/update")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .content(mapper.writeValueAsBytes(body))

    // When
    val mvcResult = mvc.perform(request)

    // Then
    mvcResult.andExpect(status().isOk)
    verify(staffService, times(1)).updateComDetails(body)
  }
}
