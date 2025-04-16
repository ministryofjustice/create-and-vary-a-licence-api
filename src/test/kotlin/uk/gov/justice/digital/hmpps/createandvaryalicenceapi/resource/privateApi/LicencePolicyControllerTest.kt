package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.privateApi

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ControllerAdvice
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.aModelHdcVariation
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.aModelLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.aModelVariation
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.someOldModelAdditionalConditions
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.ConditionChangeType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.LicenceConditionChanges
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.LicencePolicyService

@ExtendWith(SpringExtension::class)
@ActiveProfiles("test")
@WebMvcTest(controllers = [LicencePolicyController::class])
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = [LicencePolicyController::class])
@WebAppConfiguration
class LicencePolicyControllerTest {

  @MockitoBean
  private lateinit var licencePolicyService: LicencePolicyService

  @MockitoBean
  private lateinit var licenceService: LicenceService

  @Autowired
  private lateinit var mvc: MockMvc

  @Autowired
  private lateinit var mapper: ObjectMapper

  @BeforeEach
  fun reset() {
    reset(licenceService, licencePolicyService)

    mvc = MockMvcBuilders
      .standaloneSetup(LicencePolicyController(licencePolicyService, licenceService))
      .setControllerAdvice(ControllerAdvice())
      .build()
  }

  @Test
  fun `Returns an empty list if the licence is not a ModelVariation`() {
    whenever(licenceService.getLicenceById(any())).thenReturn(
      aModelLicence().copy(
        additionalLicenceConditions = someOldModelAdditionalConditions(),
      ),
    )

    val result = mvc.perform(
      get("/licence-policy/compare/2.1/licence/1").accept(APPLICATION_JSON),
    )
      .andExpect(status().isOk)
      .andExpect(content().contentType(APPLICATION_JSON))
      .andReturn()

    assertThat(result.response.contentAsString).isEqualTo(mapper.writeValueAsString(emptyList<LicenceConditionChanges>()))
  }

  @Test
  fun `Returns an list of LicenceConditionChanges`() {
    whenever(licenceService.getLicenceById(any())).thenReturn(
      aModelVariation().copy(
        additionalLicenceConditions = someOldModelAdditionalConditions(),
      ),
    )

    whenever(licencePolicyService.compareLicenceWithPolicy(any(), any(), any())).thenReturn(conditionChanges)

    val result = mvc.perform(
      get("/licence-policy/compare/2.1/licence/1").accept(APPLICATION_JSON),
    )
      .andExpect(status().isOk)
      .andExpect(content().contentType(APPLICATION_JSON))
      .andReturn()

    assertThat(result.response.contentAsString).isEqualTo(mapper.writeValueAsString(conditionChanges))
  }

  @Test
  fun `Returns an list of LicenceConditionChanges for Hdc Variations`() {
    whenever(licenceService.getLicenceById(any())).thenReturn(
      aModelHdcVariation().copy(
        additionalLicenceConditions = someOldModelAdditionalConditions(),
      ),
    )

    whenever(licencePolicyService.compareLicenceWithPolicy(any(), any(), any())).thenReturn(conditionChanges)

    val result = mvc.perform(
      get("/licence-policy/compare/2.1/licence/1").accept(APPLICATION_JSON),
    )
      .andExpect(status().isOk)
      .andExpect(content().contentType(APPLICATION_JSON))
      .andReturn()

    assertThat(result.response.contentAsString).isEqualTo(mapper.writeValueAsString(conditionChanges))
  }

  private companion object {
    val conditionChanges = listOf(
      LicenceConditionChanges(
        changeType = ConditionChangeType.TEXT_CHANGE,
        code = "ABC123",
        sequence = 0,
        previousText = "Previous text",
        currentText = "Current text",
        addedInputs = emptyList(),
        removedInputs = emptyList(),
      ),
    )
  }
}
