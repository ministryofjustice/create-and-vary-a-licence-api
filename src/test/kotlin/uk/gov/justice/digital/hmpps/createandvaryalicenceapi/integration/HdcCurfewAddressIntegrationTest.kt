package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcVariationLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.address.AddressSource
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.address.hdc.AccommodationType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.AddAddressRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.AddHdcCurfewAddressRequest

class HdcCurfewAddressIntegrationTest : IntegrationTestBase() {

  @Test
  fun `Get forbidden (403) when incorrect roles are supplied`() {
    val result = webTestClient.put()
      .uri("/licence/id/1/hdc/curfew/address")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_VERY_WRONG")))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(buildAddHdcCurfewAddressRequest())
      .exchange()
      .expectStatus().isEqualTo(HttpStatus.FORBIDDEN.value())
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    assertThat(result?.userMessage).contains("Access Denied")
  }

  @Test
  fun `Unauthorized (401) when no token is supplied`() {
    webTestClient.put()
      .uri("/licence/id/1/hdc/curfew/address")
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(buildAddHdcCurfewAddressRequest())
      .exchange()
      .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED.value())
  }

  @Test
  fun `Service throws a 404 and not a 500 when licence is not found`() {
    val exception = webTestClient.put()
      .uri("/licence/id/99999/hdc/curfew/address")
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .bodyValue(buildAddHdcCurfewAddressRequest())
      .exchange()
      .expectStatus().isNotFound
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    assertThat(exception!!.status).isEqualTo(HttpStatus.NOT_FOUND.value())
  }

  @Test
  @Sql(
    "classpath:test_data/seed-hdc-variation-licence-id-1.sql",
  )
  fun `When adding a new HDC curfew address Then everything saves as expected`() {
    assertThat(testRepository.findAllHdcCurfewAddresses().size).isEqualTo(0)
    // Given
    val uri = "/licence/id/1/hdc/curfew/address"
    val request = buildAddHdcCurfewAddressRequest()

    // When
    val result = putRequest(uri, request)

    // Then
    result.expectStatus().isOk

    val licence = getHdcLicence()
    val savedAddress = licence.curfewAddress!!

    assertThat(savedAddress.id).isNotNull()
    assertThat(savedAddress.uprn).isEqualTo(request.address.uprn)
    assertThat(savedAddress.postcode).isEqualTo(request.address.postcode)

    assertThat(savedAddress.postReleaseResidentialChecksCompleted)
      .isEqualTo(request.postReleaseResidentialChecksCompleted)
    assertThat(savedAddress.postReleaseResidentialChecksNotCompletedReason)
      .isEqualTo(request.postReleaseResidentialChecksNotCompletedReason)
    assertThat(savedAddress.accommodationType)
      .isEqualTo(request.accommodationType)

    assertThat(testRepository.findAllHdcCurfewAddresses().size).isEqualTo(1)

    val auditEvent = testRepository.findFirstAuditEvent(licence.id)
    assertThat(auditEvent.changes)
      .containsEntry("field", "createHdcCurfewAddress")
      .containsEntry("value", request.address.toString())
  }

  @Test
  @Sql(
    "classpath:test_data/seed-hdc-licence-id-1-with-curfew-address.sql",
  )
  fun `When updating an existing HDC curfew address Then everything saves as expected`() {
    assertThat(testRepository.findAllHdcCurfewAddresses().size).isEqualTo(1)
    val previousValue = testRepository.findAllHdcCurfewAddresses()[0]
    // Given
    val uri = "/licence/id/1/hdc/curfew/address"
    val request = buildAddHdcCurfewAddressRequest()

    // When
    val result = putRequest(uri, request)

    // Then
    result.expectStatus().isOk

    val licence = getHdcLicence()
    val updatedAddress = licence.curfewAddress!!

    assertThat(updatedAddress.id).isEqualTo(1L)

    assertThat(updatedAddress.uprn).isEqualTo(request.address.uprn)
    assertThat(updatedAddress.postcode).isEqualTo(request.address.postcode)

    assertThat(updatedAddress.postReleaseResidentialChecksCompleted)
      .isEqualTo(request.postReleaseResidentialChecksCompleted)
    assertThat(updatedAddress.accommodationType).isEqualTo(request.accommodationType)
    assertThat(updatedAddress.postReleaseResidentialChecksNotCompletedReason)
      .isEqualTo(request.postReleaseResidentialChecksNotCompletedReason)

    assertThat(testRepository.findAllHdcCurfewAddresses().size).isEqualTo(1)

    val auditEvent = testRepository.findFirstAuditEvent(licence.id)
    assertThat(auditEvent.changes)
      .containsEntry("field", "updateHdcCurfewAddress")
      .containsEntry("previousValue", previousValue.toString())
      .containsEntry("value", request.address.toString())
  }

  private fun putRequest(
    uri: String,
    requestBody: Any,
    roles: List<String> = listOf("ROLE_CVL_ADMIN"),
  ) = webTestClient.put()
    .uri(uri)
    .headers(setAuthorisation(roles = roles))
    .contentType(MediaType.APPLICATION_JSON)
    .bodyValue(requestBody)
    .accept(MediaType.APPLICATION_JSON)
    .exchange()

  fun getHdcLicence(id: Long = 1): HdcVariationLicence {
    val licence = testRepository.findLicence(id) as HdcVariationLicence
    assertThat(licence).isNotNull
    return licence
  }

  private fun buildAddHdcCurfewAddressRequest(
    uprn: String? = null,
    firstLine: String = "1 Test Street",
    secondLine: String? = "Flat 1",
    townOrCity: String = "Test Town",
    county: String = "Test County",
    postcode: String = "TE1 1ST",
    source: AddressSource = AddressSource.MANUAL,
    postReleaseResidentialChecksCompleted: Boolean = true,
    postReleaseResidentialChecksNotCompletedReason: String? = null,
    accommodationType: AccommodationType = AccommodationType.RESIDENTIAL,
  ): AddHdcCurfewAddressRequest = AddHdcCurfewAddressRequest(
    address = AddAddressRequest(
      uprn = uprn,
      firstLine = firstLine,
      secondLine = secondLine,
      townOrCity = townOrCity,
      county = county,
      postcode = postcode,
      source = source,
    ),
    postReleaseResidentialChecksCompleted = postReleaseResidentialChecksCompleted,
    postReleaseResidentialChecksNotCompletedReason = postReleaseResidentialChecksNotCompletedReason,
    accommodationType = accommodationType,
  )
}
