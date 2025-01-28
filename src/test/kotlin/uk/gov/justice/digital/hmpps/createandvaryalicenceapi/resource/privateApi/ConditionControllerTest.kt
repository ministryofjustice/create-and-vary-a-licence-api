package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.privateApi

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ControllerAdvice
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalConditionData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalConditionRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalConditionsRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.BespokeConditionRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.StandardCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.UpdateAdditionalConditionDataRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.UpdateStandardConditionDataRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.AddAdditionalConditionRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions.LicenceConditionService

@ExtendWith(SpringExtension::class)
@ActiveProfiles("test")
@WebMvcTest(controllers = [ConditionController::class])
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = [ConditionController::class])
@WebAppConfiguration
class ConditionControllerTest {

  @MockitoBean
  private lateinit var licenceConditionService: LicenceConditionService

  @Autowired
  private lateinit var mvc: MockMvc

  @Autowired
  private lateinit var mapper: ObjectMapper

  @BeforeEach
  fun reset() {
    mvc = MockMvcBuilders
      .standaloneSetup(
        ConditionController(
          licenceConditionService,
        ),
      )
      .setControllerAdvice(ControllerAdvice())
      .build()
  }

  @Test
  fun `update bespoke conditions`() {
    mvc.perform(
      put("/licence/id/4/bespoke-conditions")
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        .content(mapper.writeValueAsBytes(aBespokeConditionsRequest)),
    )
      .andExpect(status().isOk)

    verify(licenceConditionService, times(1)).updateBespokeConditions(4, aBespokeConditionsRequest)
  }

  @Test
  fun `update bespoke conditions with an empty request removes previous bespoke conditions`() {
    mvc.perform(
      put("/licence/id/4/bespoke-conditions")
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        .content(mapper.writeValueAsBytes(BespokeConditionRequest())),
    )
      .andExpect(status().isOk)

    verify(licenceConditionService, times(1)).updateBespokeConditions(4, BespokeConditionRequest())
  }

  @Test
  fun `Add additional condition`() {
    mvc.perform(
      post("/licence/id/4/additional-condition/AP")
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        .content(mapper.writeValueAsBytes(anAddAdditionalConditionRequest)),
    )
      .andExpect(status().isOk)

    verify(licenceConditionService, times(1)).addAdditionalCondition(4, anAddAdditionalConditionRequest)
  }

  @Test
  fun `Delete additional condition`() {
    mvc.perform(
      delete("/licence/id/4/additional-condition/id/1")
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON),
    )
      .andExpect(status().isNoContent)

    verify(licenceConditionService, times(1)).deleteAdditionalCondition(4, 1)
  }

  @Test
  fun `update the list of additional conditions`() {
    mvc.perform(
      put("/licence/id/4/additional-conditions")
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        .content(mapper.writeValueAsBytes(anUpdateAdditionalConditionsListRequest)),
    )
      .andExpect(status().isOk)

    verify(licenceConditionService, times(1)).updateAdditionalConditions(4, anUpdateAdditionalConditionsListRequest)
  }

  @Test
  fun `update the list of standard conditions`() {
    mvc.perform(
      put("/licence/id/4/standard-conditions")
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        .content(mapper.writeValueAsBytes(anUpdateStandardConditionRequest)),
    )
      .andExpect(status().isOk)

    verify(licenceConditionService, times(1)).updateStandardConditions(4, anUpdateStandardConditionRequest)
  }

  @Test
  fun `update the data associated with an additional condition`() {
    mvc.perform(
      put("/licence/id/4/additional-conditions/condition/1")
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        .content(mapper.writeValueAsBytes(anUpdateAdditionalConditionsDataRequest)),
    )
      .andExpect(status().isOk)

    verify(licenceConditionService, times(1)).updateAdditionalConditionData(
      4,
      1,
      anUpdateAdditionalConditionsDataRequest,
    )
  }

  private companion object {

    val anUpdateStandardConditionRequest = UpdateStandardConditionDataRequest(
      standardLicenceConditions = listOf(
        StandardCondition(code = "code1", sequence = 0, text = "text"),
        StandardCondition(code = "code2", sequence = 1, text = "text"),
        StandardCondition(code = "code3", sequence = 2, text = "text"),
      ),
    )

    val anAddAdditionalConditionRequest = AddAdditionalConditionRequest(
      conditionCode = "code",
      conditionType = "AP",
      conditionCategory = "category",
      sequence = 4,
      conditionText = "text",
      expandedText = "some more text",
    )

    val aBespokeConditionsRequest = BespokeConditionRequest(conditions = listOf("Bespoke 1", "Bespoke 2"))

    val anUpdateAdditionalConditionsListRequest = AdditionalConditionsRequest(
      additionalConditions = listOf(
        AdditionalConditionRequest(code = "code", category = "category", sequence = 0, text = "text"),
      ),
      conditionType = "AP",
    )

    val anUpdateAdditionalConditionsDataRequest = UpdateAdditionalConditionDataRequest(
      data = listOf(AdditionalConditionData(field = "field1", value = "value1", sequence = 0)),
    )
  }
}
