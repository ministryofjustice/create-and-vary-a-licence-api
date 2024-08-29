package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.privateApi

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ControllerAdvice
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.HdcService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.CurfewAddress
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.CurfewHours
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.FirstNight
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.HdcLicenceData

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

  @Autowired
  private lateinit var mapper: ObjectMapper

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
    whenever(hdcService.getHdcLicenceData("123456")).thenReturn(someHdcLicenceData)

    val request = get("/hdc/curfew/bookingId/123456")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)

    val result = mvc.perform(request)
      .andExpect(status().isOk)
      .andExpect(content().contentType(MediaType.APPLICATION_JSON))
      .andReturn()

    assertThat(result.response.contentAsString)
      .isEqualTo(mapper.writeValueAsString(someHdcLicenceData))

    verify(hdcService, times(1)).getHdcLicenceData("123456")
  }

  private companion object {
    val aCurfewAddress = CurfewAddress(
      "1 Test Street",
      "Test Area",
      "Test Town",
      "AB1 2CD",
    )

    val aSetOfFirstNightCurfewHours = FirstNight(
      "16:00",
      "08:00",
    )

    val aSetOfCurfewHours = CurfewHours(
      "20:00",
      "08:00",
      "20:00",
      "08:00",
      "20:00",
      "08:00",
      "20:00",
      "08:00",
      "20:00",
      "08:00",
      "20:00",
      "08:00",
      "20:00",
      "08:00",
    )

    val someHdcLicenceData = HdcLicenceData(
      aCurfewAddress,
      aSetOfFirstNightCurfewHours,
      aSetOfCurfewHours,
    )
  }
}
