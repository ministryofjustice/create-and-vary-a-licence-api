package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.caseload

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.DeliusMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.GovUkMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.PrisonApiMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.PrisonerSearchMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CaCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.typeReference
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.model.request.CaCaseloadSearch
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.CommunityManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.Detail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.Name
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ProbationCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.TeamDetail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.CaViewCasesTab
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.time.Duration
import java.time.LocalDate

private const val GET_PRISON_CASELOAD = "/caseload/case-admin/prison-view"
private val caCaseloadSearch = CaCaseloadSearch(prisonCodes = setOf("BAI"), searchString = null)

class CaCaseloadPrisonViewIntegrationTest : IntegrationTestBase() {

  @Autowired
  lateinit var licenceRepository: LicenceRepository

  @BeforeEach
  fun setupClient() {
    webTestClient = webTestClient.mutate().responseTimeout(Duration.ofSeconds(60)).build()
    govUkMockServer.stubGetBankHolidaysForEnglandAndWales()
  }

  @Test
  @Sql(
    "classpath:test_data/seed-ca-caseload-licences.sql",
  )
  fun `Successfully retrieve ca caseload with licences`() {
    // Given
    prisonerSearchMockServer.stubSearchPrisonersByReleaseDate(0)
    prisonApiMockServer.getHdcStatuses()
    prisonApiMockServer.stubGetCourtOutcomes()
    prisonerSearchMockServer.stubSearchPrisonersByNomisIds()
    deliusMockServer.stubGetStaffDetailsByUsername()
    deliusMockServer.stubGetManagersForGetApprovalCaseload()

    // When
    val result = webTestClient.post()
      .uri(GET_PRISON_CASELOAD)
      .bodyValue(caCaseloadSearch)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()

    // Then
    val caseload = result.expectStatus().isOk
      .expectHeader().contentType(APPLICATION_JSON)
      .expectBody(typeReference<List<CaCase>>())
      .returnResult().responseBody!!

    assertThat(caseload).hasSize(3)
    with(caseload[0]) {
      assertThat(name).isEqualTo("Person Two")
      assertThat(prisonerNumber).isEqualTo("A1234AB")
      assertThat(licenceStatus).isEqualTo(LicenceStatus.SUBMITTED)
      assertThat(tabType).isEqualTo(CaViewCasesTab.FUTURE_RELEASES)
      assertThat(isInHardStopPeriod).isFalse()
      assertThat(kind).isEqualTo(LicenceKind.CRD)
      assertThat(releaseDate).isEqualTo("2022-01-01")
      assertThat(releaseDateLabel).isEqualTo("CRD")
    }
    with(caseload[1]) {
      assertThat(name).isEqualTo("Person One")
      assertThat(prisonerNumber).isEqualTo("A1234AA")
      assertThat(licenceStatus).isEqualTo(LicenceStatus.APPROVED)
      assertThat(tabType).isEqualTo(CaViewCasesTab.ATTENTION_NEEDED)
      assertThat(isInHardStopPeriod).isFalse()
      assertThat(kind).isEqualTo(LicenceKind.CRD)
      assertThat(releaseDate).isEqualTo("2022-09-29")
      assertThat(releaseDateLabel).isEqualTo("CRD")
    }
    with(caseload[2]) {
      assertThat(name).isEqualTo("Person Three")
      assertThat(prisonerNumber).isEqualTo("A1234AC")
      assertThat(licenceStatus).isEqualTo(LicenceStatus.IN_PROGRESS)
      assertThat(tabType).isEqualTo(CaViewCasesTab.RELEASES_IN_NEXT_TWO_WORKING_DAYS)
      assertThat(isInHardStopPeriod).isTrue()
      assertThat(kind).isEqualTo(LicenceKind.CRD)
      assertThat(releaseDate).isToday()
      assertThat(releaseDateLabel).isEqualTo("CRD")
    }
  }

  @Test
  fun `Successfully retrieve ca caseloads with no licences`() {
    // Given
    val today = LocalDate.now()
    val nextWorking = prisonerSearchMockServer.nextWorkingDate()

    val test1 = createPrisonerSearchResult(
      prisonerNumber = "A1234AB",
      bookingId = "123",
      conditionalReleaseDate = nextWorking,
      confirmedReleaseDate = nextWorking.plusDays(1),
    )

    val test2 = createPrisonerSearchResult(
      prisonerNumber = "A2345AB",
      conditionalReleaseDate = today.minusDays(1),
      confirmedReleaseDate = nextWorking.plusDays(1),
    )

    val test3 = createPrisonerSearchResult(
      prisonerNumber = "A456AB",
      conditionalReleaseDate = nextWorking.plusMonths(1),
      confirmedReleaseDate = nextWorking.plusMonths(1),
    )

    val test4 = createPrisonerSearchResult(
      prisonerNumber = "A5678AB",
      conditionalReleaseDate = nextWorking,
      confirmedReleaseDate = nextWorking,
    )

    val prisoners = listOf(test1, test2, test3, test4)
    val managers = prisoners.mapIndexed { index, prisonerSearchPrisoner ->
      createCommunityManager(index.toLong(), prisonerSearchPrisoner.prisonerNumber)
    }

    stubClients(prisoners, managers)

    // When
    val result = webTestClient.post()
      .uri(GET_PRISON_CASELOAD)
      .bodyValue(caCaseloadSearch)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()

    // Then
    val caseload = result.expectStatus().isOk
      .expectHeader().contentType(APPLICATION_JSON)
      .expectBody(typeReference<List<CaCase>>())
      .returnResult().responseBody!!

    assertThat(caseload).hasSize(3)
    with(caseload[0]) {
      assertThat(prisonerNumber).isEqualTo(test1.prisonerNumber)
      assertThat(licenceStatus).isEqualTo(LicenceStatus.TIMED_OUT)
      assertThat(tabType).isEqualTo(CaViewCasesTab.RELEASES_IN_NEXT_TWO_WORKING_DAYS)
      assertThat(isInHardStopPeriod).isTrue()
      assertThat(releaseDateLabel).isEqualTo("CRD")
      assertThat(releaseDate).isEqualTo(test1.conditionalReleaseDate)
    }
    with(caseload[1]) {
      assertThat(prisonerNumber).isEqualTo(test4.prisonerNumber)
      assertThat(licenceStatus).isEqualTo(LicenceStatus.TIMED_OUT)
      assertThat(tabType).isEqualTo(CaViewCasesTab.RELEASES_IN_NEXT_TWO_WORKING_DAYS)
      assertThat(isInHardStopPeriod).isTrue()
      assertThat(releaseDateLabel).isEqualTo("Confirmed release date")
      assertThat(releaseDate).isEqualTo(test4.conditionalReleaseDate)
    }
    with(caseload[2]) {
      assertThat(prisonerNumber).isEqualTo(test3.prisonerNumber)
      assertThat(licenceStatus).isEqualTo(LicenceStatus.NOT_STARTED)
      assertThat(tabType).isEqualTo(CaViewCasesTab.FUTURE_RELEASES)
      assertThat(isInHardStopPeriod).isFalse()
      assertThat(releaseDateLabel).isEqualTo("Confirmed release date")
      assertThat(releaseDate).isEqualTo(test3.conditionalReleaseDate)
    }
  }

  @Test
  fun `Retrieve no ca caseload when prisoner CRD is in the past`() {
    // Given
    val prisoners = listOf(
      createPrisonerSearchResult(
        prisonerNumber = "A2345AB",
        conditionalReleaseDate = LocalDate.now().minusDays(1),
        confirmedReleaseDate = prisonerSearchMockServer.nextWorkingDate(),
      ),
    )

    val managers = prisoners.mapIndexed { index, prisonerSearchPrisoner ->
      createCommunityManager(index.toLong(), prisonerSearchPrisoner.prisonerNumber)
    }

    stubClients(prisoners, managers)

    // When
    val result = webTestClient.post()
      .uri(GET_PRISON_CASELOAD)
      .bodyValue(caCaseloadSearch)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()

    // Then
    val caseload = result.expectStatus().isOk
      .expectHeader().contentType(APPLICATION_JSON)
      .expectBody(typeReference<List<CaCase>>())
      .returnResult().responseBody!!

    assertThat(caseload).isEmpty()
  }

  @Test
  fun `Get forbidden (403) when incorrect roles are supplied`() {
    // Given
    val invalidRoles = listOf("ROLE_CVL_WRONG ROLE")

    // When
    val result = webTestClient.post()
      .uri(GET_PRISON_CASELOAD)
      .bodyValue(caCaseloadSearch)
      .accept(APPLICATION_JSON)
      .headers(setAuthorisation(roles = invalidRoles))
      .exchange()

    // Then
    val responseBody = result
      .expectStatus().isForbidden
      .expectStatus().isEqualTo(FORBIDDEN.value())
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    assertThat(responseBody?.userMessage).contains("Access Denied")
  }

  @Test
  fun `Unauthorized (401) when no token is supplied`() {
    // Given
    // No token is supplied

    // When
    val result = webTestClient.post()
      .uri(GET_PRISON_CASELOAD)
      .bodyValue(caCaseloadSearch)
      .accept(APPLICATION_JSON)
      .exchange()

    // Then
    result.expectStatus().isEqualTo(UNAUTHORIZED.value())
  }

  private fun stubClients(
    prisoners: List<PrisonerSearchPrisoner>,
    managers: List<CommunityManager>,
  ) {
    prisonerSearchMockServer.stubSearchPrisonersByReleaseDate(prisoners)
    prisonApiMockServer.getHdcStatuses()
    prisonApiMockServer.stubGetCourtOutcomes()
    prisonerSearchMockServer.stubSearchPrisonersByNomisIds()
    deliusMockServer.stubGetStaffDetailsByUsername()
    deliusMockServer.stubGetManagersForGetApprovalCaseload(managers)
  }

  fun createCommunityManager(id: Long, nomisId: String): CommunityManager = CommunityManager(
    id = id,
    code = "staff-code-$id",
    case = ProbationCase(
      crn = "A${id}2345",
      nomisId = nomisId,
    ),
    name = Name(
      forename = "Test$id",
      middleName = "Middle$id",
      surname = "Test$id",
    ),
    allocationDate = LocalDate.of(2022, 1, 2),
    team = TeamDetail(
      code = "team-code-$id",
      description = "staff-description-$id",
      borough = Detail("borough-code-$id", "borough-description-$id"),
      district = Detail("district-code-$id", "district-description-$id"),
      provider = Detail("probationArea-code-$id", "probationArea-description-$id"),
    ),
    provider = Detail("probationArea-code-$id", "probationArea-description-$id"),
    unallocated = false,
    email = "user$id@test.com",
  )

  fun createPrisonerSearchResult(
    today: LocalDate = LocalDate.now(),
    tomorrow: LocalDate = today.plusDays(1),
    nextWorking: LocalDate = prisonerSearchMockServer.nextWorkingDate(),
    prisonerNumber: String = "A1234AA",
    bookingId: String = "123",
    status: String = "ACTIVE",
    prisonId: String = "ABC",
    bookNumber: String = "12345A",
    firstName: String = "Test1",
    lastName: String = "Person1",
    dateOfBirth: LocalDate = LocalDate.of(1985, 1, 1),
    mostSeriousOffence: String = "Robbery",
    licenceExpiryDate: LocalDate? = today.plusYears(1),
    topupSupervisionExpiryDate: LocalDate? = licenceExpiryDate,
    homeDetentionCurfewEligibilityDate: LocalDate? = null,
    releaseDate: LocalDate? = tomorrow,
    confirmedReleaseDate: LocalDate? = nextWorking,
    conditionalReleaseDate: LocalDate? = nextWorking,
    paroleEligibilityDate: LocalDate? = null,
    actualParoleDate: LocalDate? = null,
    postRecallReleaseDate: LocalDate? = null,
    legalStatus: String = "SENTENCED",
    indeterminateSentence: Boolean = false,
    recall: Boolean = false,
  ): PrisonerSearchPrisoner = PrisonerSearchPrisoner(
    prisonerNumber = prisonerNumber,
    bookingId = bookingId,
    status = status,
    mostSeriousOffence = mostSeriousOffence,
    licenceExpiryDate = licenceExpiryDate,
    topupSupervisionExpiryDate = topupSupervisionExpiryDate,
    homeDetentionCurfewEligibilityDate = homeDetentionCurfewEligibilityDate,
    releaseDate = releaseDate,
    confirmedReleaseDate = confirmedReleaseDate,
    conditionalReleaseDate = conditionalReleaseDate,
    paroleEligibilityDate = paroleEligibilityDate,
    actualParoleDate = actualParoleDate,
    postRecallReleaseDate = postRecallReleaseDate,
    legalStatus = legalStatus,
    indeterminateSentence = indeterminateSentence,
    recall = recall,
    prisonId = prisonId,
    bookNumber = bookNumber,
    firstName = firstName,
    lastName = lastName,
    dateOfBirth = dateOfBirth,
  )

  private companion object {
    val govUkMockServer = GovUkMockServer()
    val prisonerSearchMockServer = PrisonerSearchMockServer()
    val deliusMockServer = DeliusMockServer()
    val prisonApiMockServer = PrisonApiMockServer()

    @JvmStatic
    @BeforeAll
    fun startMocks() {
      govUkMockServer.start()
      prisonerSearchMockServer.start()
      deliusMockServer.start()
      prisonApiMockServer.start()
    }

    @JvmStatic
    @AfterAll
    fun stopMocks() {
      prisonerSearchMockServer.stop()
      deliusMockServer.stop()
      govUkMockServer.stop()
      prisonApiMockServer.stop()
    }
  }
}
