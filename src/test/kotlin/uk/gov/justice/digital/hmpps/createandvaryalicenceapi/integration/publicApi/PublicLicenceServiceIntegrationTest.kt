package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.publicApi

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.LicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.LicenceType

class PublicLicenceServiceIntegrationTest : IntegrationTestBase() {

  @Autowired
  lateinit var licenceRepository: LicenceRepository

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql",
  )
  fun `Get licences by CRN`() {
    val resultList = webTestClient.get()
      .uri("/public/licence-summaries/crn/CRN1")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_VIEW_LICENCES")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(LicenceSummary::class.java)
      .returnResult().responseBody

    assertThat(resultList?.size).isEqualTo(1)

    val result = resultList?.first()

    assertThat(result?.id).isEqualTo(1L)
    assertThat(result?.licenceType).isEqualTo(LicenceType.AP)
    assertThat(result?.policyVersion).isEqualTo("1.0")
    assertThat(result?.version).isEqualTo("1.0")
    assertThat(result?.statusCode).isEqualTo(LicenceStatus.IN_PROGRESS)
    assertThat(result?.prisonNumber).isEqualTo("A1234AA")
    assertThat(result?.bookingId).isEqualTo(12345L)
    assertThat(result?.crn).isEqualTo("CRN1")
    assertThat(result?.createdByUsername).isEqualTo("test-client")
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql",
  )
  fun `Get forbidden (403) when incorrect role is supplied`() {
    val result = webTestClient.get()
      .uri("/public/licence-summaries/crn/CRN1")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_VERY_WRONG")))
      .exchange()
      .expectStatus().isEqualTo(HttpStatus.FORBIDDEN.value())
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    assertThat(result?.userMessage).contains("Access Denied")
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql",
  )
  fun `Get licences by prisoner number`() {
    val resultList = webTestClient.get()
      .uri("/public/licence-summaries/prison-number/A1234AA")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_VIEW_LICENCES")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(LicenceSummary::class.java)
      .returnResult().responseBody

    assertThat(resultList?.size).isEqualTo(1)

    val result = resultList?.first()

    assertThat(result?.id).isEqualTo(1L)
    assertThat(result?.licenceType).isEqualTo(LicenceType.AP)
    assertThat(result?.policyVersion).isEqualTo("1.0")
    assertThat(result?.version).isEqualTo("1.0")
    assertThat(result?.statusCode).isEqualTo(LicenceStatus.IN_PROGRESS)
    assertThat(result?.prisonNumber).isEqualTo("A1234AA")
    assertThat(result?.bookingId).isEqualTo(12345L)
    assertThat(result?.crn).isEqualTo("CRN1")
    assertThat(result?.createdByUsername).isEqualTo("test-client")
  }
}
