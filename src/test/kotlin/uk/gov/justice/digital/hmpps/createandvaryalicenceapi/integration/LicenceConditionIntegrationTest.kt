package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.assertj.core.groups.Tuple.tuple
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.GovUkMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalConditionData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalConditionRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalConditionsRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.BespokeConditionRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.StandardCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.UpdateAdditionalConditionDataRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.UpdateStandardConditionDataRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.AddAdditionalConditionRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType

private const val CONDITION_CODE = "db2d7e24-b130-4c7e-a1bf-6bb5f3036c02"

private const val EM_CONDITION_CODE = "fd129172-bdd3-4d97-a4a0-efd7b47a49d4"

class LicenceConditionIntegrationTest : IntegrationTestBase() {

  @BeforeEach
  fun reset() {
    govUkApiMockServer.stubGetBankHolidaysForEnglandAndWales()
  }

  @Autowired
  lateinit var additionalConditionRepository: AdditionalConditionRepository

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql",
  )
  fun `Update the standard conditions`() {
    webTestClient.put()
      .uri("/licence/id/1/standard-conditions")
      .bodyValue(anUpdateStandardConditionRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk

    val result = webTestClient.get()
      .uri("/licence/id/1")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(Licence::class.java)
      .returnResult().responseBody

    assertThat(result?.standardLicenceConditions)
      .extracting<Tuple> { tuple(it.code, it.text, it.sequence) }
      .containsAll(
        listOf(
          tuple("code1", "text", 0),
          tuple("code2", "text", 1),
          tuple("code3", "text", 2),
        ),
      )
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql",
  )
  fun `Add an additional condition`() {
    webTestClient.post()
      .uri("/licence/id/1/additional-condition/AP")
      .bodyValue(anAddAdditionalConditionRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk

    val result = webTestClient.get()
      .uri("/licence/id/1")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(Licence::class.java)
      .returnResult().responseBody

    assertThat(result?.typeCode).isEqualTo(LicenceType.AP)
    assertThat(result?.additionalLicenceConditions?.get(0)?.code).isEqualTo(anAddAdditionalConditionRequest.conditionCode)
    assertThat(result?.additionalLicenceConditions?.get(0)?.category).isEqualTo(anAddAdditionalConditionRequest.conditionCategory)
    assertThat(result?.additionalLicenceConditions?.get(0)?.text).isEqualTo(anAddAdditionalConditionRequest.conditionText)
    assertThat(result?.additionalLicenceConditions?.get(0)?.expandedText).isEqualTo(anAddAdditionalConditionRequest.expandedText)
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql",
  )
  fun `Update the list of additional conditions`() {
    webTestClient.put()
      .uri("/licence/id/1/additional-conditions")
      .bodyValue(anAdditionalConditionsRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk

    val result = webTestClient.get()
      .uri("/licence/id/1")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(Licence::class.java)
      .returnResult().responseBody

    assertThat(result?.additionalLicenceConditions)
      .extracting<Tuple> { tuple(it.code, it.category, it.text, it.sequence) }
      .containsAll(
        listOf(
          tuple(CONDITION_CODE, "category", "text", 0),
          tuple(EM_CONDITION_CODE, "category", "text", 1),
          tuple(CONDITION_CODE, "category", "text", 2),
          tuple(CONDITION_CODE, "category", "text", 3),
        ),
      )
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-5.sql",
  )
  fun `create electronic monitoring provider if not exists`() {
    webTestClient.put()
      .uri("/licence/id/1/additional-conditions")
      .bodyValue(anAdditionalConditionsRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk

    val result = testRepository.findLicence() as CrdLicence
    assertThat(result.electronicMonitoringProvider)
      .isNotNull
      .extracting("isToBeTaggedForProgramme", "programmeName")
      .containsExactly(null, null)
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-6.sql",
  )
  fun `don't create electronic monitoring provider if exists`() {
    webTestClient.put()
      .uri("/licence/id/1/additional-conditions")
      .bodyValue(anAdditionalConditionsRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk

    val result = testRepository.findLicence() as CrdLicence
    assertThat(result.electronicMonitoringProvider)
      .isNotNull
      .extracting("isToBeTaggedForProgramme", "programmeName")
      .containsExactly(true, "Test Programme")
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-7.sql",
  )
  fun `clear electronic monitoring provider`() {
    val result = testRepository.findLicence() as CrdLicence
    assertThat(result.electronicMonitoringProvider)
      .isNotNull
      .extracting("isToBeTaggedForProgramme", "programmeName")
      .containsExactly(true, "Test Programme")

    webTestClient.delete()
      .uri("/licence/id/1/additional-condition/id/1")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isNoContent()

    val result1 = testRepository.findLicence() as CrdLicence
    assertThat(result1.electronicMonitoringProvider).isNull()
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-6.sql",
  )
  fun `don't clear electronic monitoring provider`() {
    val result = testRepository.findLicence() as CrdLicence
    assertThat(result.electronicMonitoringProvider)
      .isNotNull
      .extracting("isToBeTaggedForProgramme", "programmeName")
      .containsExactly(true, "Test Programme")

    webTestClient.delete()
      .uri("/licence/id/1/additional-condition/id/1")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isNoContent()

    val result1 = testRepository.findLicence() as CrdLicence
    assertThat(result1.electronicMonitoringProvider)
      .isNotNull
      .extracting("isToBeTaggedForProgramme", "programmeName")
      .containsExactly(true, "Test Programme")
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql",
  )
  fun `Update the bespoke conditions`() {
    govUkApiMockServer.stubGetBankHolidaysForEnglandAndWales()
    webTestClient.put()
      .uri("/licence/id/1/bespoke-conditions")
      .bodyValue(aBespokeConditionRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk

    val result = webTestClient.get()
      .uri("/licence/id/1")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(Licence::class.java)
      .returnResult().responseBody

    assertThat(result?.bespokeConditions)
      .extracting("text")
      .containsAll(listOf("Condition 1", "Condition 2", "Condition 3"))
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql",
  )
  fun `Update the data associated with an additional condition`() {
    // Given
    webTestClient.put()
      .uri("/licence/id/1/additional-conditions")
      .bodyValue(anAdditionalConditionsRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk

    // The condition ids will depend upon the order in which tests run so find these dynamically
    val conditions = testRepository.getAdditionalConditions(1)
    assertThat(conditions).isNotEmpty
    val conditionId = conditions.first().id

    // When
    val result = webTestClient.put()
      .uri("/licence/id/1/additional-conditions/condition/$conditionId")
      .bodyValue(anAdditionalConditionDataRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()

    // Then
    result.expectStatus().isOk

    val licence = testRepository.findLicence()
    assertThat(licence.additionalConditions).isNotEmpty
    val firstCondition = licence.additionalConditions.first()

    assertThat(firstCondition)
      .extracting("conditionVersion", "expandedConditionText")
      .containsExactly(
        "1.0",
        "Notify your supervising officer of any developing intimate relationships with women or men.",
      )

    assertThat(firstCondition.additionalConditionData).isNotEmpty
    assertThat(firstCondition.additionalConditionData).hasSize(1)
    val additionalConditionData = firstCondition.additionalConditionData[0]

    assertThat(additionalConditionData.id).isEqualTo(1)
    assertThat(additionalConditionData.dataField).isEqualTo("gender")
    assertThat(additionalConditionData.dataValue).isEqualTo("women or men")
    assertThat(additionalConditionData.dataSequence).isEqualTo(0)
  }

  private companion object {
    val anUpdateStandardConditionRequest = UpdateStandardConditionDataRequest(
      standardLicenceConditions = listOf(
        StandardCondition(id = 1, code = "code1", sequence = 0, text = "text"),
        StandardCondition(id = 2, code = "code2", sequence = 1, text = "text"),
        StandardCondition(id = 3, code = "code3", sequence = 2, text = "text"),
      ),
    )

    val anAddAdditionalConditionRequest = AddAdditionalConditionRequest(
      conditionCode = CONDITION_CODE,
      conditionType = "AP",
      conditionCategory = "category",
      sequence = 4,
      conditionText = "text",
      expandedText = "some more text",
    )

    val aBespokeConditionRequest = BespokeConditionRequest(
      conditions = listOf("Condition 1", "Condition 2", "Condition 3"),
    )

    val anAdditionalConditionsRequest = AdditionalConditionsRequest(
      additionalConditions = listOf(
        AdditionalConditionRequest(code = CONDITION_CODE, category = "category", sequence = 0, text = "text"),
        AdditionalConditionRequest(code = EM_CONDITION_CODE, category = "category", sequence = 1, text = "text"),
        AdditionalConditionRequest(code = CONDITION_CODE, category = "category", sequence = 2, text = "text"),
        AdditionalConditionRequest(code = CONDITION_CODE, category = "category", sequence = 3, text = "text"),
      ),
      conditionType = "AP",
    )

    val anAdditionalConditionDataRequest = UpdateAdditionalConditionDataRequest(
      data = listOf(
        AdditionalConditionData(id = 1, field = "gender", value = "women or men", sequence = 0),
      ),
    )

    val govUkApiMockServer = GovUkMockServer()

    @JvmStatic
    @BeforeAll
    fun startMocks() {
      govUkApiMockServer.start()
    }

    @JvmStatic
    @AfterAll
    fun stopMocks() {
      govUkApiMockServer.stop()
    }
  }
}
