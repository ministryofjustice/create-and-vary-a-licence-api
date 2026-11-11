package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.publicApi.v2

import net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.NullAndEmptySource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.v2.model.licence.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.v2.model.licence.additionalConditions.CurfewAdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.v2.model.licence.additionalConditions.CurfewTime
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.v2.model.licence.additionalConditions.ElectronicMonitoringAdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.v2.model.licence.additionalConditions.ElectronicMonitoringAdditionalConditionWithRestriction
import java.time.LocalTime

class PublicElectronicMonitoringIntegrationTest : IntegrationTestBase() {
  @Autowired
  private lateinit var jdbcTemplate: JdbcTemplate

  @Test
  @Sql(
    "classpath:test_data/seed-crd-licence-id-1-with-conditions.sql",
    "classpath:test_data/public-api/v2/seed-curfew-conditions-v3.sql",
  )
  fun `v2 exposes curfew condition times from v3 policy fields`() {
    // Given
    val request = webTestClient.get().uri("/public/v2/licences/id/1")
      .headers(setAuthorisation(roles = listOf("ROLE_VIEW_LICENCES")))

    // When
    val result = request.exchange()

    // Then
    result.expectStatus().isOk.expectHeader().contentType(APPLICATION_JSON)
    val body = result.expectBody<String>().returnResult().responseBody
    val licence = mapper.readValue(body, Licence::class.java)

    val conditions = licence.conditions.apConditions.additional
    assertThat(conditions).hasSize(1)
    val curfewCondition = conditions.single() as CurfewAdditionalCondition
    assertThat(curfewCondition.curfewTimes).containsExactly(
      CurfewTime(startTime = LocalTime.of(19, 0), endTime = LocalTime.of(19, 0)),
      CurfewTime(startTime = LocalTime.of(8, 30), endTime = LocalTime.of(21, 45)),
      CurfewTime(startTime = LocalTime.of(6, 15), endTime = LocalTime.of(22, 30)),
    )
  }

  @Test
  @Sql(
    "classpath:test_data/seed-crd-licence-id-1-with-conditions.sql",
    "classpath:test_data/public-api/v2/seed-curfew-conditions-v4.sql",
  )
  fun `v2 exposes curfew condition times from v4 policy fields`() {
    // Given
    val request = webTestClient.get().uri("/public/v2/licences/id/1")
      .headers(setAuthorisation(roles = listOf("ROLE_VIEW_LICENCES")))

    // When
    val result = request.exchange()

    // Then
    result.expectStatus().isOk.expectHeader().contentType(APPLICATION_JSON)
    val body = result.expectBody<String>().returnResult().responseBody
    val licence = mapper.readValue(body, Licence::class.java)

    val conditions = licence.conditions.apConditions.additional
    assertThat(conditions).hasSize(3).allMatch { it is CurfewAdditionalCondition }
    val curfewConditions = conditions.filterIsInstance<CurfewAdditionalCondition>()
    assertThat(curfewConditions[0].curfewTimes).containsExactly(
      CurfewTime(startTime = LocalTime.of(1, 0), endTime = LocalTime.of(14, 0)),
    )
    assertThat(curfewConditions[1].curfewTimes).containsExactly(
      CurfewTime(startTime = LocalTime.of(3, 15), endTime = LocalTime.of(16, 45)),
      CurfewTime(startTime = LocalTime.of(5, 30), endTime = LocalTime.of(18, 45)),
    )
    assertThat(curfewConditions[2].curfewTimes).containsExactly(
      CurfewTime(startTime = LocalTime.of(7, 10), endTime = LocalTime.of(8, 20)),
      CurfewTime(startTime = LocalTime.of(9, 30), endTime = LocalTime.of(10, 40)),
      CurfewTime(startTime = LocalTime.of(11, 50), endTime = LocalTime.of(12, 55)),
    )
  }

  @ParameterizedTest
  @CsvSource("4.0,CRD", "4.1,CRD", "4.1,HDC", "4.1,HDC_VARIATION")
  @Sql(
    "classpath:test_data/seed-crd-licence-id-1-with-conditions.sql",
    "classpath:test_data/public-api/v2/seed-em-conditions.sql",
  )
  fun `v2 returns end dates and correct types for all three EM conditions`(policyVersion: String, kind: String) {
    // Given
    jdbcTemplate.update("update licence set version = ?, kind = ? where id = 1", policyVersion, kind)
    val request = webTestClient.get().uri("/public/v2/licences/id/1")
      .headers(setAuthorisation(roles = listOf("ROLE_VIEW_LICENCES")))

    // When
    val response = request.exchange()

    // Then
    val body = response.expectStatus().isOk.expectHeader().contentType(APPLICATION_JSON)
      .expectBody(String::class.java).returnResult().responseBody!!
    assertThatJson(body).inPath("conditions.AP.additional").isEqualTo(expectedJson("em-conditions"))
    assertThatJson(body).inPath("conditions.PSS.additional").isEqualTo(expectedJson("pss-em-conditions"))
    val conditions = mapper.readValue(body, Licence::class.java).conditions.apConditions.additional
    assertThat(conditions[0]).isInstanceOf(ElectronicMonitoringAdditionalConditionWithRestriction::class.java)
      .isInstanceOf(ElectronicMonitoringAdditionalCondition::class.java)
    assertThat(conditions[1]).isExactlyInstanceOf(ElectronicMonitoringAdditionalCondition::class.java)
    assertThat(conditions[2]).isExactlyInstanceOf(ElectronicMonitoringAdditionalCondition::class.java)
  }

  @ParameterizedTest
  @ValueSource(strings = ["1.0", "2.0", "2.1", "3.0"])
  @Sql(
    "classpath:test_data/seed-crd-licence-id-1-with-conditions.sql",
    "classpath:test_data/public-api/v2/seed-em-conditions.sql",
  )
  fun `v2 returns a null end date for historical 14a conditions`(policyVersion: String) {
    // Given
    jdbcTemplate.update("update licence set version = ? where id = 1", policyVersion)
    jdbcTemplate.update("delete from additional_condition_data where additional_condition_id = 1 and data_field = 'endDate'")
    val request = webTestClient.get().uri("/public/v2/licences/id/1")
      .headers(setAuthorisation(roles = listOf("ROLE_VIEW_LICENCES")))

    // When
    val response = request.exchange()

    // Then
    val body = response.expectStatus().isOk.expectBody(String::class.java).returnResult().responseBody!!
    assertThatJson(body).inPath("conditions.AP.additional").isEqualTo(expectedJson("historical-em-conditions"))
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = [" "])
  @Sql(
    "classpath:test_data/seed-crd-licence-id-1-with-conditions.sql",
    "classpath:test_data/public-api/v2/seed-em-conditions.sql",
  )
  fun `v2 returns null end dates for null or blank EM dates`(endDate: String?) {
    // Given
    jdbcTemplate.update("update additional_condition_data set data_value = ? where data_field = 'endDate'", endDate)
    val request = webTestClient.get().uri("/public/v2/licences/id/1")
      .headers(setAuthorisation(roles = listOf("ROLE_VIEW_LICENCES")))

    // When
    val response = request.exchange()

    // Then
    val body = response.expectStatus().isOk.expectBody(String::class.java).returnResult().responseBody!!
    assertThatJson(body).inPath("conditions.AP.additional").isEqualTo(expectedJson("em-conditions-without-dates"))
  }

  @Test
  @Sql(
    "classpath:test_data/seed-crd-licence-id-1-with-conditions.sql",
    "classpath:test_data/public-api/v2/seed-em-conditions.sql",
  )
  fun `v2 returns null end dates when EM date inputs are absent`() {
    // Given
    jdbcTemplate.update("delete from additional_condition_data where data_field = 'endDate'")
    val request = webTestClient.get().uri("/public/v2/licences/id/1")
      .headers(setAuthorisation(roles = listOf("ROLE_VIEW_LICENCES")))

    // When
    val response = request.exchange()

    // Then
    val body = response.expectStatus().isOk.expectBody(String::class.java).returnResult().responseBody!!
    assertThatJson(body).inPath("conditions.AP.additional").isEqualTo(expectedJson("em-conditions-without-dates"))
  }

  @Test
  @Sql(
    "classpath:test_data/seed-crd-licence-id-1-with-conditions.sql",
    "classpath:test_data/public-api/v2/seed-em-conditions.sql",
  )
  fun `original public API retains its existing EM condition representation`() {
    // Given
    val request = webTestClient.get().uri("/public/licences/id/1")
      .headers(setAuthorisation(roles = listOf("ROLE_VIEW_LICENCES")))

    // When
    val response = request.exchange()

    // Then
    val body = response.expectStatus().isOk.expectBody(String::class.java).returnResult().responseBody!!
    assertThatJson(body).inPath("conditions.AP.additional").isEqualTo(expectedJson("legacy-em-conditions"))
  }

  @ParameterizedTest
  @ValueSource(ints = [1, 2, 3])
  @Sql(
    "classpath:test_data/seed-crd-licence-id-1-with-conditions.sql",
    "classpath:test_data/public-api/v2/seed-em-conditions.sql",
  )
  fun `v2 reports invalid EM dates rather than returning null`(conditionId: Int) {
    // Given
    jdbcTemplate.update(
      "update additional_condition_data set data_value = 'not-a-date' where additional_condition_id = ? and data_field = 'endDate'",
      conditionId,
    )
    val request = webTestClient.get().uri("/public/v2/licences/id/1")
      .headers(setAuthorisation(roles = listOf("ROLE_VIEW_LICENCES")))

    // When
    val response = request.exchange()

    // Then
    val body = response.expectStatus().is5xxServerError.expectBody(String::class.java).returnResult().responseBody!!
    assertThatJson(body).isEqualTo(expectedJson("invalid-em-date-error"))
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-2.sql",
    "classpath:test_data/add-upload-to-licence-id-2.sql",
    "classpath:test_data/add-event-exclusion-to-licence-id-2.sql",
  )
  fun `v2 preserves non EM upload condition fields`() {
    // Given
    val request = webTestClient.get().uri("/public/v2/licences/id/2")
      .headers(setAuthorisation(roles = listOf("ROLE_VIEW_LICENCES")))

    // When
    val response = request.exchange()

    // Then
    val body = response.expectStatus().isOk.expectBody(String::class.java).returnResult().responseBody!!
    assertThatJson(body).inPath("conditions.AP.additional").isEqualTo(expectedJson("upload-conditions"))
  }

  private fun expectedJson(name: String): String = javaClass
    .getResource("/test_data/public-api/v2/$name.json")!!.readText()
}
