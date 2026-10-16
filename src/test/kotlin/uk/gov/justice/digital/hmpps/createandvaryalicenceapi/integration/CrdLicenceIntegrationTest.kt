package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.ElectronicMonitoringProviderStatus

class CrdLicenceIntegrationTest : IntegrationTestBase() {

  @Test
  @Sql("classpath:test_data/seed-licence-id-1.sql")
  fun `Get a CRD licence by ID`() {
    // Given
    val uri = "/licence/id/1"
    val roles = cvlRoles()

    // When
    val response = webTestClient.get()
      .uri(uri)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = roles))
      .exchange()

    // Then
    val result = response
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(CrdLicence::class.java)
      .returnResult().responseBody

    assertThat(result).isNotNull
    assertThat(result?.id).isEqualTo(1L)
    assertThat(result?.kind).isEqualTo("CRD")
    assertThat(result?.standardLicenceConditions).hasSize(2)
      .extracting("code")
      .containsExactly("goodBehaviour", "notBreakLaw")
    assertThat(result?.standardPssConditions).hasSize(1)
      .extracting("code")
      .containsExactly("attendMeetings")
    assertThat(result?.responsibleComFullName).isEqualTo("Test Client")
    assertThat(result?.electronicMonitoringProviderStatus).isEqualTo(ElectronicMonitoringProviderStatus.COMPLETE)
    assertThat(result?.hardStopDate).isNotNull
    assertThat(result?.hardStopWarningDate).isNotNull
    assertThat(result?.isInHardStopPeriod).isTrue
    assertThat(result?.isDueToBeReleasedInTheNextTwoWorkingDays).isTrue
    assertThat(result?.isReplacementForOptedOutLicence).isFalse
    assertThat(result?.isReplacementForRefusedLicence).isFalse
  }

  @Test
  @Sql("classpath:test_data/seed-crd-licence-with-opt-out-and-refused-links.sql")
  fun `Get a CRD licence includes opted out and refused replacement flags`() {
    // Given
    val uri = "/licence/id/2"
    val roles = cvlRoles()

    // When
    val response = webTestClient.get()
      .uri(uri)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = roles))
      .exchange()

    // Then
    val result = response
      .expectStatus().isOk
      .expectBody(CrdLicence::class.java)
      .returnResult().responseBody

    assertThat(result?.isReplacementForOptedOutLicence).isTrue
    assertThat(result?.isReplacementForRefusedLicence).isTrue
  }
}
