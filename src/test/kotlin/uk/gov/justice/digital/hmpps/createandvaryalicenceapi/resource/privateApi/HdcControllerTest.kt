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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ControllerAdvice
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.HdcService

@ExtendWith(SpringExtension::class)
@ActiveProfiles("test")
@WebMvcTest(controllers = [HdcController::class])
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = [HdcController::class])
@WebAppConfiguration
class HdcControllerTest {

  @MockBean
  private lateinit var hdcService: HdcService

  @Autowired
  private lateinit var mvc: MockMvc

  @BeforeEach
  fun reset() {
    reset(hdcService)

    mvc = MockMvcBuilders
      .standaloneSetup(HdcController(hdcService))
      .setControllerAdvice(ControllerAdvice())
      .build()
  }

  @Test
  fun `get HDC licence data by booking ID`() {
    mvc.perform(
      get("/hdc/curfew/bookingId/123456")
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
    )
      .andExpect(status().isOk)

    verify(hdcService, times(1)).getHdcLicenceData(123456)
  }
}
