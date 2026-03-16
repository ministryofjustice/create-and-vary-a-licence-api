package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.privateApi

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ControllerAdvice
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.NotSecuredWebMvcTest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.EligibilityAssessment
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.support.SupportService

@NotSecuredWebMvcTest(controllers = [SupportController::class])
class SupportControllerTest {

  @MockitoBean
  private lateinit var supportService: SupportService

  @Autowired
  private lateinit var mvc: MockMvc

  @Autowired
  private lateinit var mapper: ObjectMapper

  @BeforeEach
  fun reset() {
    reset(supportService)

    mvc = MockMvcBuilders
      .standaloneSetup(SupportController(supportService))
      .setControllerAdvice(ControllerAdvice())
      .build()
  }

  @Test
  fun `get ineligibility reasons`() {
    whenever(supportService.getIneligibilityReasons("A1234AA")).thenReturn(
      EligibilityAssessment(
        genericIneligibilityReasons = listOf("A Reason"),
        crdIneligibilityReasons = emptyList(),
        prrdIneligibilityReasons = emptyList(),
      ),
    )

    val request = get("/offender/nomisid/A1234AA/ineligibility-reasons")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)

    val response = mvc.perform(request).andExpect(status().isOk).andReturn().response.contentAsString

    assertThat(
      mapper.readValue(
        response,
        EligibilityAssessment::class.java,
      ).ineligibilityReasons,
    ).isEqualTo(listOf("A Reason"))
  }

  @Test
  fun `get is-91 status`() {
    whenever(supportService.getIS91Status("A1234AA")).thenReturn(true)

    val request = get("/offender/nomisid/A1234AA/is-91-status")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)

    val response = mvc.perform(request).andExpect(status().isOk).andReturn().response.contentAsString

    assertThat(mapper.readValue(response, String::class.java)).isEqualTo("true")
  }
}
