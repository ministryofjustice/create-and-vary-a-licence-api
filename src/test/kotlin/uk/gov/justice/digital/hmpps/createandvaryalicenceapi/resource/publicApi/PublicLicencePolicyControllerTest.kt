package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.EntityNotFoundException
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ControllerAdvice
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licencePolicy.ConditionTypes
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licencePolicy.LicencePolicy
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licencePolicy.LicencePolicyAdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licencePolicy.LicencePolicyConditions
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licencePolicy.StandardCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.PolicyVersion
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.publicApi.PublicLicencePolicyService

@ExtendWith(SpringExtension::class)
@ActiveProfiles("test")
@WebMvcTest(controllers = [PublicLicencePolicyController::class])
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = [PublicLicencePolicyController::class])
@WebAppConfiguration
class PublicLicencePolicyControllerTest {

  @MockBean
  private lateinit var publicLicencePolicyService: PublicLicencePolicyService

  @Autowired
  private lateinit var mvc: MockMvc

  @Autowired
  private lateinit var mapper: ObjectMapper

  @BeforeEach
  fun reset() {
    reset(publicLicencePolicyService)

    mvc = MockMvcBuilders
      .standaloneSetup(PublicLicencePolicyController(publicLicencePolicyService))
      .setControllerAdvice(ControllerAdvice())
      .build()
  }

  @Test
  fun `get policy by version number`() {
    whenever(publicLicencePolicyService.getLicencePolicyByVersionNumber("2.1")).thenReturn(aLicencePolicy)

    val result = mvc.perform(get("/public/policy/2.1").accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk)
      .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
      .andReturn()

    assertThat(result.response.contentAsString)
      .isEqualTo(mapper.writeValueAsString(aLicencePolicy))

    verify(publicLicencePolicyService, times(1)).getLicencePolicyByVersionNumber("2.1")
  }

  @Test
  fun `404 policy not found by version number`() {
    whenever(publicLicencePolicyService.getLicencePolicyByVersionNumber("0"))
      .thenThrow(EntityNotFoundException("Policy version 0 not found"))

    val result = mvc.perform(get("/public/policy/0").accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isNotFound)
      .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
      .andReturn()

    assertThat(result.response.contentAsString).contains("Not found")

    verify(publicLicencePolicyService, times(1)).getLicencePolicyByVersionNumber("0")
  }

  private companion object {

    val someApStandardConditions = listOf(
      StandardCondition(code = "goodBehaviour", text = "Be of good behaviour"),
      StandardCondition(code = "notCommitOffence", text = "Do not commit any offence"),
      StandardCondition(code = "keepInTouch", text = "Keep in touch with supervising officer"),
    )

    val someApAdditionalConditions = listOf(
      LicencePolicyAdditionalCondition(
        code = "code1",
        text = "Do not associate with [NAME] for a period of [TIME PERIOD]",
        category = "Maintaining contact with a person",
        categoryShort = "Contact with a person",
        requiresUserInput = true,
      ),
      LicencePolicyAdditionalCondition(
        code = "code2",
        text = "Engage with Integrated Offender Management team",
        category = "Participate in activities",
        categoryShort = "Programmes or activities",
        requiresUserInput = false,
      ),
    )

    val somePssStandardConditions = listOf(
      StandardCondition(code = "goodBehaviour", text = "Be of good behaviour"),
      StandardCondition(code = "notCommitOffence", text = "Do not commit any offence"),
      StandardCondition(code = "keepInTouch", text = "Keep in touch with supervising officer"),
    )

    val somePssAdditionalConditions = listOf(
      LicencePolicyAdditionalCondition(
        code = "code1",
        text = "Attend [INSERT APPOINTMENT TIME DATE AND ADDRESS]",
        category = "Appointment",
        categoryShort = null,
        requiresUserInput = true,
      ),
      LicencePolicyAdditionalCondition(
        code = "code2",
        text = "Attend [INSERT NAME AND ADDRESS] as reasonably required by your supervisor",
        category = "Testing",
        categoryShort = null,
        requiresUserInput = true,
      ),
    )

    val someApConditions = LicencePolicyConditions(
      standard = someApStandardConditions,
      additional = someApAdditionalConditions,
    )

    val somePssConditions = LicencePolicyConditions(
      standard = somePssStandardConditions,
      additional = somePssAdditionalConditions,
    )

    val someConditionTypes = ConditionTypes(
      apConditions = someApConditions,
      pssConditions = somePssConditions,
    )

    val aLicencePolicy = LicencePolicy(
      PolicyVersion.V2_1,
      someConditionTypes,
    )
  }

  @Test
  fun `given a policy exist when get latest policy then return latest policy`() {
    whenever(publicLicencePolicyService.getLatestLicencePolicy()).thenReturn(aLicencePolicy)

    val result = mvc.perform(get("/public/policy/latest").accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk)
      .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
      .andReturn()

    assertThat(result.response.contentAsString)
      .isEqualTo(mapper.writeValueAsString(aLicencePolicy))

    verify(publicLicencePolicyService, times(1)).getLatestLicencePolicy()
  }
}
