package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.CommunityApiMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.User

class ComIntegrationTest : IntegrationTestBase() {

  @Value("\${hmpps.community.api.url}")
  val communityApiWiremockUrl: String = ""

  @Test
  fun `Get team codes for user with valid staff identifier`() {
    communityApiMockServer.stubGetTeamCodesForUser()

    val result = webTestClient.get()
      .uri("$communityApiWiremockUrl/secure/staff/staffIdentifier/123456")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(User::class.java)
      .returnResult().responseBody

    assertThat(result?.teams?.size).isEqualTo(1)
    assertThat(result?.teams)
      .extracting("code")
      .containsExactly("A01B02")
  }

  private companion object {
    val communityApiMockServer = CommunityApiMockServer()

    @JvmStatic
    @BeforeAll
    fun startMocks() {
      communityApiMockServer.start()
    }

    @JvmStatic
    @AfterAll
    fun stopMocks() {
      communityApiMockServer.stop()
    }
  }
}
