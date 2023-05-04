package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.*
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.AddAdditionalConditionRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType

class LicenceConditionIntegrationTest : IntegrationTestBase() {

  @Autowired
  lateinit var licenceRepository: LicenceRepository

  @Autowired
  lateinit var additionalConditionRepository: AdditionalConditionRepository

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql"
  )/**/
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
      .extracting<Tuple> { Tuple.tuple(it.code, it.text, it.sequence) }
      .containsAll(
        listOf(
          Tuple.tuple("code1", "text", 0),
          Tuple.tuple("code2", "text", 1),
          Tuple.tuple("code3", "text", 2),
        )
      )
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql"
  )/**/
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
    "classpath:test_data/seed-licence-id-1.sql"
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
      .extracting<Tuple> { Tuple.tuple(it.code, it.category, it.text, it.sequence) }
      .containsAll(
        listOf(
          Tuple.tuple("code1", "category", "text", 0),
          Tuple.tuple("code2", "category", "text", 1),
          Tuple.tuple("code3", "category", "text", 2),
          Tuple.tuple("code4", "category", "text", 3)
        )
      )
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql"
  )/**/
  fun `Update the bespoke conditions`() {
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
    "classpath:test_data/seed-licence-id-1.sql"
  )
  fun `Update the data associated with an additional condition`() {
    webTestClient.put()
      .uri("/licence/id/1/additional-conditions")
      .bodyValue(anAdditionalConditionsRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk

    // The condition ids will depend upon the order in which tests run so find these dynamically
    val conditions =
      additionalConditionRepository.findAll().toMutableList().filter { condition -> condition.licence.id == 1L }
    assertThat(conditions).isNotEmpty
    val conditionId = conditions.first().id

    webTestClient.put()
      .uri("/licence/id/1/additional-conditions/condition/$conditionId")
      .bodyValue(anAdditionalConditionDataRequest)
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

    assertThat(result?.additionalLicenceConditions?.get(0)?.expandedText).isEqualTo("expanded text")

    assertThat(result?.additionalLicenceConditions?.get(0)?.data)
      .extracting<Tuple> { Tuple.tuple(it.field, it.value, it.sequence) }
      .containsAll(
        listOf(
          Tuple.tuple("field1", "value1", 0),
          Tuple.tuple("field2", "value2", 1),
          Tuple.tuple("field3", "value3", 2),
        )
      )
  }

  private companion object {
    val anUpdateStandardConditionRequest = UpdateStandardConditionDataRequest(
      standardLicenceConditions = listOf(
        StandardCondition(code = "code1", sequence = 0, text = "text"),
        StandardCondition(code = "code2", sequence = 1, text = "text"),
        StandardCondition(code = "code3", sequence = 2, text = "text")
      )
    )

    val anAddAdditionalConditionRequest = AddAdditionalConditionRequest(
      conditionCode = "code",
      conditionType = "AP",
      conditionCategory = "category",
      sequence = 4,
      conditionText = "text",
      expandedText = "some more text"
    )

    val aBespokeConditionRequest = BespokeConditionRequest(
      conditions = listOf("Condition 1", "Condition 2", "Condition 3")
    )

    val anAdditionalConditionsRequest = AdditionalConditionsRequest(
      additionalConditions = listOf(
        AdditionalCondition(code = "code1", category = "category", sequence = 0, text = "text"),
        AdditionalCondition(code = "code2", category = "category", sequence = 1, text = "text"),
        AdditionalCondition(code = "code3", category = "category", sequence = 2, text = "text"),
        AdditionalCondition(code = "code4", category = "category", sequence = 3, text = "text")
      ),
      conditionType = "AP"
    )

    val anAdditionalConditionDataRequest = UpdateAdditionalConditionDataRequest(
      data = listOf(
        AdditionalConditionData(field = "field1", value = "value1", sequence = 0),
        AdditionalConditionData(field = "field2", value = "value2", sequence = 1),
        AdditionalConditionData(field = "field3", value = "value3", sequence = 2),
      ),
      expandedConditionText = "expanded text"
    )
  }
}