package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.DeliusMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.GovUkMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.PrisonApiMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.PrisonerSearchMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.ProbationSearchMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceCreationResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.AddAdditionalConditionRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.CreateLicenceRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.OverrideLicenceTypeRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateSentenceDatesRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StandardConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate

class ChangeLicenceTypeIntegrationTest : IntegrationTestBase() {

  @Autowired
  lateinit var licenceRepository: LicenceRepository

  @Autowired
  lateinit var standardConditionRepository: StandardConditionRepository

  @Autowired
  lateinit var additionalConditionRepository: AdditionalConditionRepository

  @Autowired
  lateinit var auditEventRepository: AuditEventRepository

  @BeforeEach
  fun reset() {
    govUkApiMockServer.stubGetBankHolidaysForEnglandAndWales()
  }

  @Test
  fun `Change licence type`() {
    prisonApiMockServer.stubGetPrison()
    prisonApiMockServer.stubGetCourtOutcomes()
    prisonApiMockServer.stubGetPrisonerDetail()
    probationSearchMockServer.stubSearchForPersonOnProbation()
    deliusMockServer.stubGetOffenderManager()
    prisonerSearchMockServer.stubSearchPrisonersByNomisIds()

    assertThat(licenceRepository.count()).isEqualTo(0)
    assertThat(standardConditionRepository.count()).isEqualTo(0)
    assertThat(auditEventRepository.count()).isEqualTo(0)

    val result = webTestClient.post().uri("/licence/create").bodyValue(CreateLicenceRequest(nomsId = "A1234AA"))
      .accept(MediaType.APPLICATION_JSON).headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN"))).exchange()
      .expectStatus().isOk.expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(LicenceCreationResponse::class.java).returnResult().responseBody!!

    webTestClient.post().uri("/licence/id/1/additional-condition/AP").bodyValue(
      AddAdditionalConditionRequest(
        conditionCode = "db2d7e24-b130-4c7e-a1bf-6bb5f3036c02",
        conditionType = "AP",
        conditionCategory = "category",
        sequence = 4,
        conditionText = "text",
        expandedText = "some more text",
      ),
    ).accept(MediaType.APPLICATION_JSON).headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN"))).exchange()
      .expectStatus().isOk

    log.info("Expect OK: Result returned ${mapper.writeValueAsString(result)}")

    assertThat(result.licenceId).isGreaterThan(0L)

    val standardConditions = standardConditionRepository.findAll()
    assertThat(standardConditions).hasSize(9)
    assertThat(standardConditions.map { it.conditionType }).allMatch { it == "AP" }
    val additionalConditions = additionalConditionRepository.findAll()
    assertThat(additionalConditions).hasSize(1)
    assertThat(additionalConditions.map { it.conditionType }).allMatch { it == "AP" }

    // Override prisoner search response to trigger LSD change
    mockPrisonerSearchResponse(LocalDate.parse("2023-09-11"))

    webTestClient.put().uri("/licence/id/${result.licenceId}/sentence-dates").bodyValue(
      UpdateSentenceDatesRequest(
        conditionalReleaseDate = LocalDate.parse("2023-09-11"),
        actualReleaseDate = LocalDate.parse("2023-09-11"),
        sentenceStartDate = LocalDate.parse("2021-09-11"),
        sentenceEndDate = LocalDate.parse("2024-09-11"),
        licenceExpiryDate = null,
        topupSupervisionStartDate = LocalDate.parse("2024-09-11"),
        topupSupervisionExpiryDate = LocalDate.now().plusDays(1),
      ),
    ).accept(MediaType.APPLICATION_JSON).headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN"))).exchange()
      .expectStatus().isOk

    webTestClient.post().uri("/licence/id/${result.licenceId}/override/type")
      .bodyValue(OverrideLicenceTypeRequest(licenceType = LicenceType.PSS, reason = "Some Reason"))
      .accept(MediaType.APPLICATION_JSON).headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN"))).exchange()
      .expectStatus().isAccepted

    val standardConditionsAfterUpdate = standardConditionRepository.findAll()
    assertThat(standardConditionsAfterUpdate).hasSize(8)
    assertThat(standardConditionsAfterUpdate.map { it.conditionType }).allMatch { it == "PSS" }

    assertThat(additionalConditionRepository.count()).isEqualTo(0)
    assertThat(auditEventRepository.count()).isEqualTo(5)

    assertThat(licenceRepository.count()).isEqualTo(1)
    val licence = licenceRepository.findAll().first()
    assertThat(licence.typeCode).isEqualTo(LicenceType.PSS)
  }

  @Test
  fun `Fail to change licence type due to invalid dates`() {
    prisonApiMockServer.stubGetPrison()
    prisonApiMockServer.stubGetCourtOutcomes()
    prisonerSearchMockServer.stubSearchPrisonersByNomisIds()
    probationSearchMockServer.stubSearchForPersonOnProbation()
    deliusMockServer.stubGetOffenderManager()

    assertThat(licenceRepository.count()).isEqualTo(0)
    assertThat(standardConditionRepository.count()).isEqualTo(0)
    assertThat(auditEventRepository.count()).isEqualTo(0)

    val result = webTestClient.post().uri("/licence/create").bodyValue(CreateLicenceRequest(nomsId = "NOMSID"))
      .accept(MediaType.APPLICATION_JSON).headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN"))).exchange()
      .expectStatus().isOk.expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(LicenceCreationResponse::class.java).returnResult().responseBody!!

    val response = webTestClient.post().uri("/licence/id/${result.licenceId}/override/type")
      .bodyValue(OverrideLicenceTypeRequest(licenceType = LicenceType.PSS, reason = "Some Reason"))
      .accept(MediaType.APPLICATION_JSON).headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN"))).exchange()
      .expectStatus().isBadRequest.expectBody(ProblemDetail::class.java).returnResult().responseBody!!

    assertThat(response.title).isEqualTo("Incorrect dates for new licence type: PSS")
    assertThat(response.properties).isEqualTo(
      mapOf(
        "fieldErrors" to mapOf(
          "LED" to "IS_PRESENT",
        ),
      ),
    )
  }

  @Test
  fun `Unauthorized (401) when no token is supplied`() {
    webTestClient.post().uri("/licence/id/1/override/type")
      .bodyValue(OverrideLicenceTypeRequest(licenceType = LicenceType.AP, reason = "Some Reason"))
      .accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED.value())

    assertThat(licenceRepository.count()).isEqualTo(0)
    assertThat(standardConditionRepository.count()).isEqualTo(0)
  }

  @Test
  fun `Get forbidden (403) when incorrect roles are supplied`() {
    val result = webTestClient.post().uri("/licence/id/1/override/type")
      .bodyValue(OverrideLicenceTypeRequest(licenceType = LicenceType.AP, reason = "Some Reason"))
      .accept(MediaType.APPLICATION_JSON).headers(setAuthorisation(roles = listOf("ROLE_CVL_VERY_WRONG")))
      .exchange().expectStatus().isEqualTo(HttpStatus.FORBIDDEN.value()).expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    assertThat(result?.userMessage).contains("Access Denied")
    assertThat(licenceRepository.count()).isEqualTo(0)
    assertThat(standardConditionRepository.count()).isEqualTo(0)
  }

  private fun mockPrisonerSearchResponse(releaseDate: LocalDate?) {
    prisonerSearchMockServer.stubSearchPrisonersByNomisIds(
      """[
            {
              "prisonerNumber": "A1234AA",
              "bookingId": "123",
              "status": "ACTIVE",
              "mostSeriousOffence": "Robbery",
              "licenceExpiryDate": "${LocalDate.now().plusYears(1)}",
              "topupSupervisionExpiryDate": "${LocalDate.now().plusYears(1)}",
              "homeDetentionCurfewEligibilityDate": null,
              "releaseDate": "$releaseDate",
              "confirmedReleaseDate": "$releaseDate",
              "conditionalReleaseDate": "$releaseDate",
              "paroleEligibilityDate": null,
              "actualParoleDate" : null,
              "postRecallReleaseDate": null,
              "homeDetentionCurfewActualDate": "2024-08-01",
              "legalStatus": "SENTENCED",
              "indeterminateSentence": false,
              "recall": false,
              "prisonId": "ABC",
              "bookNumber": "12345A",
              "firstName": "Test1",
              "lastName": "Person1",
              "dateOfBirth": "1985-01-01"
           }]
      """.trimIndent(),
    )
  }

  private companion object {
    val govUkApiMockServer = GovUkMockServer()
    val prisonApiMockServer = PrisonApiMockServer()
    val prisonerSearchMockServer = PrisonerSearchMockServer()
    val probationSearchMockServer = ProbationSearchMockServer()
    val deliusMockServer = DeliusMockServer()

    @JvmStatic
    @BeforeAll
    fun startMocks() {
      prisonApiMockServer.start()
      govUkApiMockServer.start()
      prisonerSearchMockServer.start()
      probationSearchMockServer.start()
      deliusMockServer.start()
    }

    @JvmStatic
    @AfterAll
    fun stopMocks() {
      prisonApiMockServer.stop()
      govUkApiMockServer.stop()
      prisonerSearchMockServer.stop()
      probationSearchMockServer.stop()
      deliusMockServer.stop()
    }
  }
}
