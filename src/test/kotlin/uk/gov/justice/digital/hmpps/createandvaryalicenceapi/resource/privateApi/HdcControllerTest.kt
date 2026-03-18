package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.privateApi

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ControllerAdvice
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.NotSecuredWebMvcTest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.HdcService

@NotSecuredWebMvcTest(controllers = [HdcController::class])
class HdcControllerTest {

  @MockitoBean
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
  fun `get HDC licence data by licence ID`() {
    mvc.perform(
      get("/hdc/curfew/licenceId/123456")
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON),
    )
      .andExpect(status().isOk)

    verify(hdcService, times(1)).getHdcLicenceData(123456)
  }
}
