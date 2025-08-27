package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.DeliusMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.GovUkMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.UpdateComRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateProbationTeamRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StaffRepository

class OffenderIntegrationTest : IntegrationTestBase() {

  @Autowired
  lateinit var licenceRepository: LicenceRepository

  @Autowired
  lateinit var staffRepository: StaffRepository

  @Test
  fun `Get forbidden (403) when incorrect roles are supplied`() {
    val requestBody = UpdateComRequest(
      staffIdentifier = 2000,
      staffUsername = "joebloggs",
      staffEmail = "joebloggs@probation.gov.uk",
      firstName = "X",
      lastName = "Y",
    )

    val result = webTestClient.put()
      .uri("/offender/crn/CRN1/responsible-com")
      .bodyValue(requestBody)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_WRONG ROLE")))
      .exchange()
      .expectStatus().isForbidden
      .expectStatus().isEqualTo(HttpStatus.FORBIDDEN.value())
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    assertThat(result?.userMessage).contains("Access Denied")
  }

  @Test
  fun `Unauthorized (401) when no token is supplied`() {
    webTestClient.put()
      .uri("/offender/crn/CRN1/responsible-com")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED.value())
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql",
  )
  fun `Update an offender's inflight licences with new COM details`() {
    // Given
    val requestBody = UpdateComRequest(
      staffIdentifier = 2000,
      staffUsername = "test-client",
      staffEmail = "joebloggs@probation.gov.uk",
      firstName = "Joseph",
      lastName = "Bloggs",
    )
    govUkApiMockServer.stubGetBankHolidaysForEnglandAndWales()

    // When
    val result = webTestClient.put()
      .uri("/offender/crn/CRN1/responsible-com")
      .bodyValue(requestBody)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()

    // Then
    result.expectStatus().isOk

    val licence = licenceRepository.findById(1L).orElseThrow()
    assertThat(licence.responsibleCom)
      .extracting("staffIdentifier", "username", "email", "firstName", "lastName")
      .isEqualTo(listOf(2000L, "TEST-CLIENT", "joebloggs@probation.gov.uk", "Joseph", "Bloggs"))
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql",
  )
  fun `Update an offender's inflight licences with new probation team`() {
    val requestBody = UpdateProbationTeamRequest(
      probationAreaCode = "N02",
      probationPduCode = "PDU2",
      probationLauCode = "LAU2",
      probationTeamCode = "TEAM2",
    )

    webTestClient.put()
      .uri("/offender/crn/CRN1/probation-team")
      .bodyValue(requestBody)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk

    val licence = licenceRepository.findById(1L).orElseThrow()
    assertThat(licence.probationAreaCode).isEqualTo("N02")
    assertThat(licence.probationPduCode).isEqualTo("PDU2")
    assertThat(licence.probationLauCode).isEqualTo("LAU2")
    assertThat(licence.probationTeamCode).isEqualTo("TEAM2")
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-3.sql",
  )
  fun `Synchronises COM allocation info with Delius`() {
    val crn = "CRN1"
    val userName = "username"
    val emailAddress = "emailAddress@Delius"
    val staffIdentifier = 123L
    val firstName = "forename"
    val lastName = "surname"

    deliusMockServer.stubGetOffenderManager(
      crn,
      userName = userName,
      emailAddress,
      staffIdentifier,
      firstName = firstName,
      lastName = lastName,
    )
    deliusMockServer.stubAssignDeliusRole(userName = userName.uppercase())

    webTestClient.put()
      .uri("/offender/sync-com/crn/$crn")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk

    val com = staffRepository.findByStaffIdentifier(staffIdentifier)
    assertThat(com).isNotNull
    assertThat(com!!.username).isEqualTo(userName.uppercase())
    assertThat(com.email).isEqualTo(emailAddress)
    assertThat(com.firstName).isEqualTo(firstName)
    assertThat(com.lastName).isEqualTo(lastName)

    val licence = licenceRepository.findById(3L).orElseThrow()
    assertThat(licence.responsibleCom.id).isEqualTo(com.id)
  }

  private companion object {
    val deliusMockServer = DeliusMockServer()
    val govUkApiMockServer = GovUkMockServer()

    @JvmStatic
    @BeforeAll
    fun startMocks() {
      deliusMockServer.start()
      govUkApiMockServer.start()
    }

    @JvmStatic
    @AfterAll
    fun stopMocks() {
      deliusMockServer.stop()
      govUkApiMockServer.stop()
    }
  }
}
