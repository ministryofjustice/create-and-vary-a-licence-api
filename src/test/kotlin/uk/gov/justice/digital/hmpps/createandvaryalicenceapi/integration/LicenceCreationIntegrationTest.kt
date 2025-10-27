package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.EntityAlreadyExistsResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HardStopLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.TimeServedLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.DeliusMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.GovUkMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.HdcApiMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.PrisonApiMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.PrisonerSearchMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceCreationResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.CreateLicenceRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.LicenceType.CRD
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.LicenceType.HARD_STOP
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.LicenceType.HDC
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.HdcCurfewAddressRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StandardConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate

class LicenceCreationIntegrationTest : IntegrationTestBase() {

  @Autowired
  lateinit var standardConditionRepository: StandardConditionRepository

  @Autowired
  lateinit var additionalConditionRepository: AdditionalConditionRepository

  @Autowired
  lateinit var auditEventRepository: AuditEventRepository

  @Autowired
  lateinit var hdcCurfewAddressRepository: HdcCurfewAddressRepository

  @BeforeEach
  fun reset() {
    govUkApiMockServer.stubGetBankHolidaysForEnglandAndWales()
  }

  @Test
  fun `Create a PRRD licence`() {
    // Given
    val nomisPostRecallReleaseDate = LocalDate.now().plusDays(1)
    prisonApiMockServer.stubGetPrison()
    prisonApiMockServer.stubGetCourtOutcomes()
    prisonApiMockServer.stubGetSentenceAndRecallTypes()
    prisonerSearchMockServer.stubSearchPrisonersByNomisIds(postRecallReleaseDate = nomisPostRecallReleaseDate)
    deliusMockServer.stubGetProbationCase()
    deliusMockServer.stubGetOffenderManager(regionCode = "REGION1")

    assertThat(testRepository.countLicence()).isEqualTo(0)
    assertThat(standardConditionRepository.count()).isEqualTo(0)
    assertThat(auditEventRepository.count()).isEqualTo(0)

    val licenceCreationResponse = webTestClient.post()
      .uri("/licence/create")
      .bodyValue(CreateLicenceRequest(nomsId = "NOMSID"))
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(LicenceCreationResponse::class.java)
      .returnResult().responseBody

    assertThat(licenceCreationResponse?.licenceId).isGreaterThan(0L)
    val licences = testRepository.findAllLicence()
    assertThat(licences.count()).isEqualTo(1)

    val licence = licences.first()
    assertThat(licence.kind).isEqualTo(LicenceKind.PRRD)
    assertThat(licence.responsibleCom!!.username).isEqualTo("AAA")
    assertThat(licence.typeCode).isEqualTo(LicenceType.AP)
    assertThat(licence.statusCode).isEqualTo(LicenceStatus.IN_PROGRESS)
    assertThat(licence.postRecallReleaseDate).isEqualTo(nomisPostRecallReleaseDate)
    assertThat(standardConditionRepository.count()).isEqualTo(9)
    assertThat(additionalConditionRepository.count()).isEqualTo(0)
    assertThat(auditEventRepository.count()).isEqualTo(1)
  }

  @Test
  fun `Create a CRD licence`() {
    prisonApiMockServer.stubGetPrison()
    prisonApiMockServer.stubGetCourtOutcomes()
    prisonerSearchMockServer.stubSearchPrisonersByNomisIds()
    deliusMockServer.stubGetProbationCase()
    deliusMockServer.stubGetOffenderManager()

    assertThat(testRepository.countLicence()).isEqualTo(0)
    assertThat(standardConditionRepository.count()).isEqualTo(0)
    assertThat(auditEventRepository.count()).isEqualTo(0)

    val result = webTestClient.post()
      .uri("/licence/create")
      .bodyValue(CreateLicenceRequest(nomsId = "NOMSID"))
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(LicenceCreationResponse::class.java)
      .returnResult().responseBody

    log.info("Expect OK: Result returned ${mapper.writeValueAsString(result)}")

    assertThat(result?.licenceId).isGreaterThan(0L)

    val licences = testRepository.findAllLicence()
    assertThat(licences).hasSize(1)
    val licence = licences.first() as CrdLicence
    assertThat(licence.getCom().username).isEqualTo("AAA")
    assertThat(licence.typeCode).isEqualTo(LicenceType.AP)
    assertThat(licence.statusCode).isEqualTo(LicenceStatus.IN_PROGRESS)

    assertThat(standardConditionRepository.count()).isEqualTo(9)
    assertThat(additionalConditionRepository.count()).isEqualTo(0)
    assertThat(auditEventRepository.count()).isEqualTo(1)
  }

  @Test
  @Sql(
    "classpath:test_data/seed-prison-case-administrator.sql",
  )
  fun `Cannot create two inflight licences`() {
    prisonApiMockServer.stubGetPrison()
    prisonApiMockServer.stubGetCourtOutcomes()
    prisonerSearchMockServer.stubSearchPrisonersByNomisIds()
    deliusMockServer.stubGetProbationCase()
    deliusMockServer.stubGetOffenderManager()

    val result = webTestClient.post()
      .uri("/licence/create")
      .bodyValue(CreateLicenceRequest(nomsId = "A1234AA", type = CRD))
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(LicenceCreationResponse::class.java)
      .returnResult().responseBody!!

    val secondAttempt = webTestClient.post()
      .uri("/licence/create")
      .bodyValue(CreateLicenceRequest(nomsId = "A1234AA", type = CRD))
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isEqualTo(HttpStatus.CONFLICT)
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(EntityAlreadyExistsResponse::class.java)
      .returnResult().responseBody!!

    assertThat(result.licenceId).isEqualTo(secondAttempt.existingResourceId)
    assertThat(testRepository.countLicence()).isEqualTo(1)
  }

  @Test
  fun `Unauthorized (401) for creating CRD Licence when no token is supplied`() {
    webTestClient.post()
      .uri("/licence/create")
      .bodyValue(CreateLicenceRequest(nomsId = "NOMSID"))
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED.value())

    assertThat(testRepository.countLicence()).isEqualTo(0)
    assertThat(standardConditionRepository.count()).isEqualTo(0)
  }

  @Test
  fun `Get forbidden (403) for creating CRD Licence when incorrect roles are supplied`() {
    val result = webTestClient.post()
      .uri("/licence/create")
      .bodyValue(CreateLicenceRequest(nomsId = "NOMSID"))
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_VERY_WRONG")))
      .exchange()
      .expectStatus().isEqualTo(HttpStatus.FORBIDDEN.value())
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    assertThat(result?.userMessage).contains("Access Denied")
    assertThat(testRepository.countLicence()).isEqualTo(0)
    assertThat(standardConditionRepository.count()).isEqualTo(0)
  }

  @Test
  @Sql(
    "classpath:test_data/seed-prison-case-administrator.sql",
  )
  fun `Create a Hard Stop licence`() {
    prisonApiMockServer.stubGetPrison()
    prisonApiMockServer.stubGetCourtOutcomes()
    prisonerSearchMockServer.stubSearchPrisonersByNomisIds(conditionalReleaseDateOverrideDate = LocalDate.now().plusDays(10))
    deliusMockServer.stubGetProbationCase()
    deliusMockServer.stubGetOffenderManager()

    assertThat(testRepository.countLicence()).isEqualTo(0)
    assertThat(standardConditionRepository.count()).isEqualTo(0)
    assertThat(auditEventRepository.count()).isEqualTo(0)

    val result = webTestClient.post()
      .uri("/licence/create")
      .bodyValue(CreateLicenceRequest(nomsId = "NOMSID", type = HARD_STOP))
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(user = "pca", roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(LicenceCreationResponse::class.java)
      .returnResult().responseBody!!

    log.info("Expect OK: Result returned ${mapper.writeValueAsString(result)}")

    assertThat(result.licenceId).isGreaterThan(0L)

    val licences = testRepository.findAllLicence()
    assertThat(licences).hasSize(1)
    val licence = licences.first() as HardStopLicence
    assertThat(licence.kind).isEqualTo(LicenceKind.HARD_STOP)
    assertThat(licence.typeCode).isEqualTo(LicenceType.AP)
    assertThat(licence.statusCode).isEqualTo(LicenceStatus.IN_PROGRESS)
    assertThat(licence.getCom().username).isEqualTo("AAA")
    assertThat(licence.createdBy!!.id).isEqualTo(9L)
    assertThat(standardConditionRepository.count()).isEqualTo(9)
    assertThat(auditEventRepository.count()).isEqualTo(1)
  }

  @Test
  @Sql(
    "classpath:test_data/seed-prison-case-administrator.sql",
  )
  fun `Create a Timeserved licence`() {
    prisonApiMockServer.stubGetPrison()
    prisonApiMockServer.stubGetCourtOutcomes()
    prisonerSearchMockServer.stubSearchPrisonersByNomisIds()
    deliusMockServer.stubGetProbationCase()
    deliusMockServer.stubGetOffenderManager()

    assertThat(testRepository.countLicence()).isEqualTo(0)
    assertThat(testRepository.getStandardConditionCount()).isEqualTo(0)
    assertThat(testRepository.getAuditEventCount()).isEqualTo(0)

    val result = webTestClient.post()
      .uri("/licence/create")
      .bodyValue(CreateLicenceRequest(nomsId = "NOMSID", type = HARD_STOP))
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(user = "pca", roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(LicenceCreationResponse::class.java)
      .returnResult().responseBody!!

    log.info("Expect OK: Result returned ${mapper.writeValueAsString(result)}")

    assertThat(result.licenceId).isGreaterThan(0L)

    val licences = testRepository.findAllLicence()
    assertThat(licences).hasSize(1)
    val licence = licences.first() as TimeServedLicence
    assertThat(licence.kind).isEqualTo(LicenceKind.TIME_SERVED)
    assertThat(licence.typeCode).isEqualTo(LicenceType.AP)
    assertThat(licence.statusCode).isEqualTo(LicenceStatus.IN_PROGRESS)
    assertThat(licence.responsibleCom?.username).isEqualTo("AAA")
    assertThat(licence.createdBy!!.id).isEqualTo(9L)
    assertThat(testRepository.getStandardConditionCount()).isEqualTo(9)
    assertThat(testRepository.getAuditEventCount()).isEqualTo(1)
  }

  @Test
  @Sql(
    "classpath:test_data/seed-prison-case-administrator.sql",
    "classpath:test_data/seed-timed-out-licence.sql",
  )
  fun `Create a Hard Stop licence which is replacing timed out licence`() {
    prisonApiMockServer.stubGetPrison()
    prisonerSearchMockServer.stubSearchPrisonersByNomisIds(conditionalReleaseDateOverrideDate = LocalDate.now().plusDays(1))
    prisonApiMockServer.stubGetCourtOutcomes()
    deliusMockServer.stubGetProbationCase()
    deliusMockServer.stubGetOffenderManager()

    assertThat(testRepository.countLicence()).isEqualTo(1)
    assertThat(standardConditionRepository.count()).isEqualTo(0)
    assertThat(auditEventRepository.count()).isEqualTo(0)

    val result = webTestClient.post()
      .uri("/licence/create")
      .bodyValue(CreateLicenceRequest(nomsId = "NOMSID", type = HARD_STOP))
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(user = "pca", roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(LicenceCreationResponse::class.java)
      .returnResult().responseBody!!

    log.info("Expect OK: Result returned ${mapper.writeValueAsString(result)}")

    assertThat(result.licenceId).isGreaterThan(0L)

    val licences = testRepository.findAllLicence()
    assertThat(licences).hasSize(2)
    val crdLicence = licences.find { it.kind == LicenceKind.CRD } as CrdLicence
    val hardStopLicence = licences.find { it.kind == LicenceKind.HARD_STOP } as HardStopLicence

    assertThat(hardStopLicence.getCom().username).isEqualTo("AAA")
    assertThat(hardStopLicence.createdBy!!.id).isEqualTo(9L)
    assertThat(hardStopLicence.substituteOfId).isEqualTo(crdLicence.id)
    assertThat(hardStopLicence.kind).isEqualTo(LicenceKind.HARD_STOP)
    assertThat(hardStopLicence.typeCode).isEqualTo(LicenceType.AP)
    assertThat(hardStopLicence.statusCode).isEqualTo(LicenceStatus.IN_PROGRESS)

    assertThat(standardConditionRepository.count()).isEqualTo(9)
    assertThat(additionalConditionRepository.count()).isEqualTo(1)

    assertThat(auditEventRepository.count()).isEqualTo(1)
  }

  @Test
  fun `Unauthorized (401) for creating Hard Stop Licence when no token is supplied`() {
    webTestClient.post()
      .uri("/licence/create")
      .bodyValue(CreateLicenceRequest(nomsId = "NOMSID", type = HARD_STOP))
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED.value())

    assertThat(testRepository.countLicence()).isEqualTo(0)
    assertThat(standardConditionRepository.count()).isEqualTo(0)
  }

  @Test
  fun `Get forbidden (403) for creating Hard Stop Licence when incorrect roles are supplied`() {
    val result = webTestClient.post()
      .uri("/licence/create")
      .bodyValue(CreateLicenceRequest(nomsId = "NOMSID", type = HARD_STOP))
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_VERY_WRONG")))
      .exchange()
      .expectStatus().isEqualTo(HttpStatus.FORBIDDEN.value())
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    assertThat(result?.userMessage).contains("Access Denied")
    assertThat(testRepository.countLicence()).isEqualTo(0)
    assertThat(standardConditionRepository.count()).isEqualTo(0)
  }

  @Nested
  inner class CreateHdcLicences {
    @Test
    fun `Create a AP HDC licence`() {
      prisonApiMockServer.stubGetPrison()
      prisonApiMockServer.stubGetHdcLatest(123, "APPROVED")
      prisonerSearchMockServer.stubSearchPrisonersByNomisIdsHDCAPResult()
      deliusMockServer.stubGetProbationCase()
      deliusMockServer.stubGetOffenderManager()
      hdcApiMockServer.stubGetHdcLicenceData(123)

      assertThat(testRepository.countLicence()).isEqualTo(0)
      assertThat(standardConditionRepository.count()).isEqualTo(0)
      assertThat(auditEventRepository.count()).isEqualTo(0)
      assertThat(hdcCurfewAddressRepository.count()).isEqualTo(0)

      val result = webTestClient.post()
        .uri("/licence/create")
        .bodyValue(CreateLicenceRequest(nomsId = "NOMSID", type = HDC))
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
        .exchange()
        .expectStatus().isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody(LicenceCreationResponse::class.java)
        .returnResult().responseBody

      log.info("Expect OK: Result returned ${mapper.writeValueAsString(result)}")

      assertThat(result?.licenceId).isGreaterThan(0L)

      val licences = testRepository.findAllLicence()
      assertThat(licences).hasSize(1)
      val licence = licences.first() as HdcLicence
      assertThat(licence.getCom().username).isEqualTo("AAA")
      assertThat(licence.kind).isEqualTo(LicenceKind.HDC)
      assertThat(licence.typeCode).isEqualTo(LicenceType.AP)
      assertThat(licence.statusCode).isEqualTo(LicenceStatus.IN_PROGRESS)

      assertThat(standardConditionRepository.count()).isEqualTo(9)
      assertThat(auditEventRepository.count()).isEqualTo(1)
      assertThat(hdcCurfewAddressRepository.count()).isEqualTo(1)
    }

    @Test
    fun `Create a AP_PSS HDC licence`() {
      prisonApiMockServer.stubGetPrison()
      prisonApiMockServer.stubGetHdcLatest(123, "APPROVED")
      prisonerSearchMockServer.stubSearchPrisonersByNomisIdsHDCAPPSSResult()
      deliusMockServer.stubGetProbationCase()
      deliusMockServer.stubGetOffenderManager()
      hdcApiMockServer.stubGetHdcLicenceData(123)

      assertThat(testRepository.countLicence()).isEqualTo(0)
      assertThat(standardConditionRepository.count()).isEqualTo(0)
      assertThat(auditEventRepository.count()).isEqualTo(0)
      assertThat(hdcCurfewAddressRepository.count()).isEqualTo(0)

      val result = webTestClient.post()
        .uri("/licence/create")
        .bodyValue(CreateLicenceRequest(nomsId = "NOMSID", type = HDC))
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
        .exchange()
        .expectStatus().isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody(LicenceCreationResponse::class.java)
        .returnResult().responseBody

      log.info("Expect OK: Result returned ${mapper.writeValueAsString(result)}")

      assertThat(result?.licenceId).isGreaterThan(0L)

      val licences = testRepository.findAllLicence()
      assertThat(licences).hasSize(1)
      val licence = licences.first() as HdcLicence
      assertThat(licence.getCom().username).isEqualTo("AAA")
      assertThat(licence.kind).isEqualTo(LicenceKind.HDC)
      assertThat(licence.typeCode).isEqualTo(LicenceType.AP_PSS)
      assertThat(licence.statusCode).isEqualTo(LicenceStatus.IN_PROGRESS)

      assertThat(standardConditionRepository.count()).isEqualTo(17)
      assertThat(auditEventRepository.count()).isEqualTo(1)
      assertThat(hdcCurfewAddressRepository.count()).isEqualTo(1)
    }

    @Test
    fun `Service throws error when PSS HDC licence`() {
      prisonerSearchMockServer.stubSearchPrisonersByNomisIdsHDCPSSResult()
      hdcApiMockServer.stubGetHdcLicenceData(123)

      val exception = webTestClient.post()
        .uri("/licence/create")
        .bodyValue(CreateLicenceRequest(nomsId = "NOMSID", type = HDC))
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
        .exchange()
        .expectStatus().is5xxServerError
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody

      assertThat(exception!!.status).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value())
      assertThat(exception.userMessage).isEqualTo("Unexpected error: HDC Licence for A1234AA can not be of type PSS")
      assertThat(exception.developerMessage).isEqualTo("HDC Licence for A1234AA can not be of type PSS")
    }

    @Test
    fun `Unauthorized (401) for creating CRD Licence when no token is supplied`() {
      webTestClient.post()
        .uri("/licence/create")
        .bodyValue(CreateLicenceRequest(nomsId = "NOMSID", type = HDC))
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED.value())

      assertThat(testRepository.countLicence()).isEqualTo(0)
      assertThat(standardConditionRepository.count()).isEqualTo(0)
      assertThat(hdcCurfewAddressRepository.count()).isEqualTo(0)
    }

    @Test
    fun `Get forbidden (403) for creating CRD Licence when incorrect roles are supplied`() {
      val result = webTestClient.post()
        .uri("/licence/create")
        .bodyValue(CreateLicenceRequest(nomsId = "NOMSID", type = HDC))
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_CVL_VERY_WRONG")))
        .exchange()
        .expectStatus().isEqualTo(HttpStatus.FORBIDDEN.value())
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody

      assertThat(result?.userMessage).contains("Access Denied")
      assertThat(testRepository.countLicence()).isEqualTo(0)
      assertThat(standardConditionRepository.count()).isEqualTo(0)
      assertThat(hdcCurfewAddressRepository.count()).isEqualTo(0)
    }
  }

  private companion object {
    val govUkApiMockServer = GovUkMockServer()
    val prisonApiMockServer = PrisonApiMockServer()
    val prisonerSearchMockServer = PrisonerSearchMockServer()
    val deliusMockServer = DeliusMockServer()
    val hdcApiMockServer = HdcApiMockServer()

    @JvmStatic
    @BeforeAll
    fun startMocks() {
      prisonApiMockServer.start()
      govUkApiMockServer.start()
      prisonerSearchMockServer.start()
      deliusMockServer.start()
      hdcApiMockServer.start()
    }

    @JvmStatic
    @AfterAll
    fun stopMocks() {
      prisonApiMockServer.stop()
      govUkApiMockServer.stop()
      prisonerSearchMockServer.stop()
      deliusMockServer.stop()
      hdcApiMockServer.stop()
    }
  }
}
