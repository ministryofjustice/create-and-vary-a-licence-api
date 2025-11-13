package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.timeserved

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.timeserved.CommunicationMethod
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.timeserved.ContactStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.timeserved.TimeServedProbationConfirmContactRequest
import java.time.Duration
import java.time.LocalDateTime

private const val TIME_SERVED_PROBATION_CONTACT_URI = "/licences/time-served"

class TimeServedProbationConfirmContactIntegrationTest : IntegrationTestBase() {

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql",
  )
  fun `Create new probation contact record successfully`() {
    // Given
    val licenceId = 1L
    val request = TimeServedProbationConfirmContactRequest(
      ContactStatus.ALREADY_CONTACTED,
      communicationMethods = listOf(CommunicationMethod.EMAIL, CommunicationMethod.PHONE),
      otherCommunicationDetail = null,
    )

    // When
    val response = putTimeServedProbationConfirmContact(licenceId, request)

    // Then
    response.expectStatus().isNoContent
    assertConfirmProbationEntity(
      licenceId,
      ContactStatus.ALREADY_CONTACTED,
      setOf(CommunicationMethod.EMAIL, CommunicationMethod.PHONE),
    )
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql",
  )
  @Sql(
    "classpath:test_data/seed-probation-confirm-id-1.sql",
  )
  fun `Update existing probation contact record`() {
    // Given
    val licenceId = 1L
    val request = TimeServedProbationConfirmContactRequest(
      ContactStatus.WILL_CONTACT_SOON,
      communicationMethods = listOf(CommunicationMethod.TEAMS, CommunicationMethod.OTHER),
      otherCommunicationDetail = "In person",
    )

    // When
    val response = putTimeServedProbationConfirmContact(licenceId, request)

    // Then
    response.expectStatus().isNoContent
    assertConfirmProbationEntity(
      licenceId,
      ContactStatus.WILL_CONTACT_SOON,
      setOf(CommunicationMethod.TEAMS, CommunicationMethod.OTHER),
      expectedOtherDetail = "In person",
      update = true,
    )
  }

  @Test
  fun `Validation fails when contacted is null`() {
    // Given
    val licenceId = 1L
    val request = mapOf("communicationMethods" to listOf("EMAIL"))

    // When
    val response = putTimeServedProbationConfirmContact(licenceId, request)

    // Then
    val errorResponse = getErrorResponse(response)
    assertThat(errorResponse!!.userMessage).contains("Validation failed for one or more fields.")
    assertThat(errorResponse.developerMessage).contains("Confirm if you have contacted the probation team")
  }

  @Test
  fun `Validation fails when communication is empty`() {
    // Given
    val licenceId = 1L
    val request = TimeServedProbationConfirmContactRequest(
      ContactStatus.ALREADY_CONTACTED,
      communicationMethods = emptyList(),
      otherCommunicationDetail = null,
    )

    // When
    val response = putTimeServedProbationConfirmContact(licenceId, request)

    // Then
    val errorResponse = getErrorResponse(response)
    assertThat(errorResponse!!.userMessage).contains("Validation failed for one or more fields.")
    assertThat(errorResponse.developerMessage).contains("Choose a form of communication")
  }

  @Test
  fun `Validation fails when other selected but otherDetail is null`() {
    // Given
    val licenceId = 1L
    val request = TimeServedProbationConfirmContactRequest(
      ContactStatus.CANNOT_CONTACT,
      communicationMethods = listOf(CommunicationMethod.OTHER),
      otherCommunicationDetail = null,
    )

    // When
    val response = putTimeServedProbationConfirmContact(licenceId, request)

    // Then
    val errorResponse = getErrorResponse(response)
    assertThat(errorResponse!!.userMessage).contains("Validation failed for one or more fields.")
    assertThat(errorResponse.developerMessage).contains("Enter a form of communication")
  }

  private fun putTimeServedProbationConfirmContact(licenceId: Long, requestBody: Any) = webTestClient.put()
    .uri("$TIME_SERVED_PROBATION_CONTACT_URI/$licenceId/confirm/probation-contact")
    .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
    .contentType(APPLICATION_JSON)
    .bodyValue(requestBody)
    .exchange()

  private fun assertConfirmProbationEntity(
    licenceId: Long,
    expectedContacted: ContactStatus,
    expectedCommunication: Set<CommunicationMethod>,
    expectedOtherDetail: String? = null,
    update: Boolean = false,
  ) {
    val entity = testRepository.findTimeServedProbationConfirmContact(licenceId)
    assertThat(entity).isNotNull
    entity?.let {
      assertThat(entity.contactStatus).isEqualTo(expectedContacted)
      assertThat(entity.communicationMethods).containsExactlyInAnyOrderElementsOf(expectedCommunication)
      assertThat(entity.otherDetail).isEqualTo(expectedOtherDetail)
      assertThat(entity.otherDetail).isEqualTo(expectedOtherDetail)
      assertThat(entity.confirmedByUsername).isEqualTo("test-client")

      val now = LocalDateTime.now()
      val tolerance = Duration.ofSeconds(5) // allow 5 seconds difference
      if (update) {
        assertThat(it.dateCreated).isBefore(it.dateLastUpdated)
        assertThat(it.dateLastUpdated).isCloseTo(now, within(tolerance))
      } else {
        assertThat(it.dateCreated).isCloseTo(now, within(tolerance))
        assertThat(it.dateLastUpdated).isCloseTo(now, within(tolerance))
      }
    }
  }

  private fun getErrorResponse(
    result: WebTestClient.ResponseSpec,
    exceptedStatus: HttpStatusCode = HttpStatusCode.valueOf(400),
  ): ErrorResponse? {
    result.expectStatus().isEqualTo(exceptedStatus)
    val errorResponse = result.expectBody(ErrorResponse::class.java).returnResult().responseBody
    assertThat(errorResponse).isNotNull
    return errorResponse
  }
}
