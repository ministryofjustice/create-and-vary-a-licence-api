package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.caseload

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.DeliusMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.PrisonApiMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.PrisonerSearchMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response.LastMinuteHandoverCaseResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.typeReference
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.CommunityManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.Detail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.Name
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ProbationCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.TeamDetail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.time.LocalDate

private const val GET_LAST_MINUTE_CASES = "offender/support/report/last-minute-handover-cases"

class LastMinuteHandoverCasesIntegrationTest : IntegrationTestBase() {

  @BeforeEach
  fun setup() {
    webTestClient = webTestClient.mutate().build()
  }

  @Test
  fun `Successfully retrieve last minute handover cases that have not started`() {
    // Given
    val workingDays = prisonerSearchMockServer.nextWorkingDates()
    val workingDay = workingDays.elementAt(3)

    val prisoner1 = createPrisonerSearchResult(
      prisonerNumber = "A1234AA",
      conditionalReleaseDate = workingDay,
      confirmedReleaseDate = workingDay,
      prisonId = "BAI",
      firstName = "John",
      lastName = "Smith",
    )
    val prisoners = listOf(prisoner1)
    val managers = listOf(createCommunityManager(1, prisoner1.prisonerNumber))

    stubClients(prisoners, managers)

    // When
    val result = webTestClient.get()
      .uri(GET_LAST_MINUTE_CASES)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .accept(APPLICATION_JSON)
      .exchange()

    // Then
    val response = result.expectStatus().isOk
      .expectHeader().contentType(APPLICATION_JSON)
      .expectBody(typeReference<List<LastMinuteHandoverCaseResponse>>())
      .returnResult().responseBody!!

    assertThat(response).hasSize(1)
    with(response[0]) {
      assertThat(prisonerNumber).isEqualTo(prisoner1.prisonerNumber)
      assertThat(prisonerName).isEqualTo("${prisoner1.firstName} ${prisoner1.lastName}")
      assertThat(prisonCode).isEqualTo(prisoner1.prisonId)
      assertThat(prisonName).isEqualTo(prisoner1.prisonName)
      assertThat(releaseDate).isEqualTo(prisoner1.conditionalReleaseDate)
      assertThat(probationRegion).isEqualTo("probationArea-description-1")
      assertThat(status).isEqualTo(LicenceStatus.NOT_STARTED)
      assertThat(crn).isEqualTo("A12345")
      assertThat(probationPractitioner).isEqualTo("Test1 Middle1 Test1")
    }
  }

  @Sql(
    "classpath:test_data/seed-licence-id-1.sql",
  )
  @Test
  fun `Successfully retrieve last minute handover cases that are in process`() {
    // Given
    val workingDays = prisonerSearchMockServer.nextWorkingDates()
    val workingDay = workingDays.elementAt(2)

    val prisoner1 = createPrisonerSearchResult(
      prisonerNumber = "A1234AA",
      conditionalReleaseDate = workingDay,
      confirmedReleaseDate = workingDay,
      prisonId = "BAI",
      firstName = "John",
      lastName = "Smith",
      bookingId = "12345",
    )
    val prisoners = listOf(prisoner1)
    val managers = listOf(createCommunityManager(1, prisoner1.prisonerNumber))

    stubClients(prisoners, managers)

    // When
    val result = webTestClient.get()
      .uri(GET_LAST_MINUTE_CASES)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .accept(APPLICATION_JSON)
      .exchange()

    // Then
    val response = result.expectStatus().isOk
      .expectHeader().contentType(APPLICATION_JSON)
      .expectBody(typeReference<List<LastMinuteHandoverCaseResponse>>())
      .returnResult().responseBody!!

    assertThat(response).hasSize(1)
    with(response[0]) {
      assertThat(prisonerNumber).isEqualTo(prisoner1.prisonerNumber)
      assertThat(prisonerName).isEqualTo("${prisoner1.firstName} ${prisoner1.lastName}")
      assertThat(prisonCode).isEqualTo(prisoner1.prisonId)
      assertThat(prisonName).isEqualTo(prisoner1.prisonName)
      assertThat(releaseDate).isEqualTo(prisoner1.conditionalReleaseDate)
      assertThat(probationRegion).isEqualTo("probationArea-description-1")
      assertThat(status).isEqualTo(LicenceStatus.IN_PROGRESS)
      assertThat(crn).isEqualTo("A12345")
      assertThat(probationPractitioner).isEqualTo("Test1 Middle1 Test1")
    }
  }

  @Test
  fun `Retrieve no cases when CRD is in the past`() {
    // Given
    val yesterday = LocalDate.now().minusDays(1)
    val prisoner = createPrisonerSearchResult(
      prisonerNumber = "A9999AA",
      conditionalReleaseDate = yesterday,
      confirmedReleaseDate = yesterday,
    )
    val prisoners = listOf(prisoner)
    val managers = listOf(createCommunityManager(1, prisoner.prisonerNumber))

    stubClients(prisoners, managers)

    // When
    val result = webTestClient.get()
      .uri(GET_LAST_MINUTE_CASES)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .accept(APPLICATION_JSON)
      .exchange()

    // Then
    val response = result.expectStatus().isOk
      .expectBody(typeReference<List<LastMinuteHandoverCaseResponse>>())
      .returnResult().responseBody!!

    assertThat(response).isEmpty()
  }

  @Test
  fun `Retrieve no cases when CRD is more than a week away`() {
    // Given
    val workingDays = prisonerSearchMockServer.nextWorkingDates()
    val workingDay = workingDays.elementAt(8)

    val prisoner = createPrisonerSearchResult(
      prisonerNumber = "A9999AA",
      conditionalReleaseDate = workingDay,
      confirmedReleaseDate = workingDay,
    )
    val prisoners = listOf(prisoner)
    val managers = listOf(createCommunityManager(1, prisoner.prisonerNumber))

    stubClients(prisoners, managers)

    // When
    val result = webTestClient.get()
      .uri(GET_LAST_MINUTE_CASES)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .accept(APPLICATION_JSON)
      .exchange()

    // Then
    val response = result.expectStatus().isOk
      .expectBody(typeReference<List<LastMinuteHandoverCaseResponse>>())
      .returnResult().responseBody!!

    assertThat(response).isEmpty()
  }

  @Test
  fun `Retrieve no cases when all prisoners are HDC eligible`() {
    // Given
    val workingDays = prisonerSearchMockServer.nextWorkingDates()
    val workingDay = workingDays.elementAt(3)

    val prisoner = createPrisonerSearchResult(
      prisonerNumber = "A8888AA",
      conditionalReleaseDate = workingDay,
      confirmedReleaseDate = workingDay,
      homeDetentionCurfewEligibilityDate = LocalDate.now().minusDays(1), // HDC eligible
    )
    val prisoners = listOf(prisoner)
    val managers = listOf(createCommunityManager(1, prisoner.prisonerNumber))

    stubClients(prisoners, managers)
    prisonApiMockServer.getHdcStatuses(
      listOf(
        prisoner.bookingId!! to true,
      ),
    )

    // When
    val result = webTestClient.get()
      .uri(GET_LAST_MINUTE_CASES)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .accept(APPLICATION_JSON)
      .exchange()

    // Then
    val response = result.expectStatus().isOk
      .expectBody(typeReference<List<LastMinuteHandoverCaseResponse>>())
      .returnResult().responseBody!!

    assertThat(response).isEmpty()
  }

  @Test
  fun `Get forbidden (403) when incorrect role supplied`() {
    val result = webTestClient.get()
      .uri(GET_LAST_MINUTE_CASES)
      .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
      .accept(APPLICATION_JSON)
      .exchange()

    val responseBody = result.expectStatus().isForbidden
      .expectStatus().isEqualTo(FORBIDDEN.value())
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    assertThat(responseBody?.userMessage).contains("Access Denied")
  }

  @Test
  fun `Unauthorized (401) when no token is supplied`() {
    val result = webTestClient.get()
      .uri(GET_LAST_MINUTE_CASES)
      .accept(APPLICATION_JSON)
      .exchange()

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
    deliusMockServer.stubGetManagers(managers)
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

  companion object {
    val prisonerSearchMockServer = PrisonerSearchMockServer()
    val deliusMockServer = DeliusMockServer()
    val prisonApiMockServer = PrisonApiMockServer()

    @JvmStatic
    @BeforeAll
    fun startMocks() {
      prisonerSearchMockServer.start()
      deliusMockServer.start()
      prisonApiMockServer.start()
    }

    @JvmStatic
    @AfterAll
    fun stopMocks() {
      prisonerSearchMockServer.stop()
      deliusMockServer.stop()
      prisonApiMockServer.stop()
    }
  }
}
