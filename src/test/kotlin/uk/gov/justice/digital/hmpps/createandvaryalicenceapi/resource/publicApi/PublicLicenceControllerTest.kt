package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi

import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.reset
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ControllerAdvice
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService

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
    val result = mvc.perform(get("/public/licence/id").accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk)
      .andReturn()

    assertThat(result.response.contentAsString)
      .isEqualTo("")
  }
}
