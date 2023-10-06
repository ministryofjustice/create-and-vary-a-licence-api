package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi

import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ControllerAdvice

@ExtendWith(SpringExtension::class)
@ActiveProfiles("test")
@WebMvcTest(controllers = [PublicLicenceController::class])
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = [PublicLicenceController::class])
@WebAppConfiguration
class PublicLicenceControllerTest {

  @Autowired
  private lateinit var mvc: MockMvc

  @BeforeEach
  fun reset() {
    mvc = MockMvcBuilders
      .standaloneSetup(PublicLicenceController())
      .setControllerAdvice(ControllerAdvice())
      .build()
  }

  @Test
  fun `get a licence by id`() {
    val result = mvc.perform(get("/public/licences/id/1234").accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk)
      .andReturn()

    assertThat(result.response.contentAsString)
      .isEqualTo("")
  }

  @Test
  fun `get licences by prison number`() {
    mvc.perform(get("/public/licences/prison-number/A1234AA").accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk)
      .andExpect(jsonPath("$", `is`(emptyList<Any>())))
  }
}
