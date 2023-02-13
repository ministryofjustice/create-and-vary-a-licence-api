package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.reset
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.json.JsonContent
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.core.ResolvableType
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ControllerAdvice
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicencePolicyService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import java.util.Objects

@ExtendWith(SpringExtension::class)
@ActiveProfiles("test")
@WebMvcTest(controllers = [LicencePolicyController::class])
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = [LicencePolicyController::class])
@WebAppConfiguration
class LicencePolicyControllerTest {

  @MockBean
  private lateinit var licenceService: LicenceService

  @SpyBean
  private lateinit var licencePolicyService: LicencePolicyService

  @Autowired
  private lateinit var mvc: MockMvc

  @Autowired
  private lateinit var mapper: ObjectMapper

  @BeforeEach
  fun reset() {
    reset(licenceService)

    mvc = MockMvcBuilders.standaloneSetup(LicencePolicyController(licencePolicyService, licenceService))
      .setControllerAdvice(ControllerAdvice()).build()
  }

  @Test
  fun `get a licence policy V1_0`() {
    val result = mvc.perform(get("/licence-policy/version/1.0").accept(APPLICATION_JSON)).andExpect(status().isOk)
      .andExpect(content().contentType(APPLICATION_JSON)).andReturn()

    assertThatJson(result).isStrictlyEqualToJson("v1.0.json")
  }

  @Test
  fun `get a licence policy V2_0`() {
    val result = mvc.perform(get("/licence-policy/version/2.0").accept(APPLICATION_JSON)).andExpect(status().isOk)
      .andExpect(content().contentType(APPLICATION_JSON)).andReturn()

    assertThatJson(result).isStrictlyEqualToJson("v2.0.json")
  }

  @Test
  fun `get a licence policy V2_1`() {
    val result = mvc.perform(get("/licence-policy/version/2.1").accept(APPLICATION_JSON)).andExpect(status().isOk)
      .andExpect(content().contentType(APPLICATION_JSON)).andReturn()

    assertThatJson(result).isStrictlyEqualToJson("v2.1.json")
  }

  private fun assertThatJson(response: MvcResult) =
    Assertions.assertThat(
      JsonContent<Any>(
        javaClass,
        ResolvableType.forType(String::class.java),
        Objects.requireNonNull(response.response.contentAsString)
      )
    )
}
