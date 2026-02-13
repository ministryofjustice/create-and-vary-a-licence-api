package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple.tuple
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Sort
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.DeliusMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.GovUkMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.PrisonApiMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.PrisonerSearchMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.ProbationUserSearchRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response.ComSearchResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.ProbationSearchSortBy
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.SearchField
import java.time.LocalDate

class ComIntegrationTest : IntegrationTestBase() {
  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql",
  )
  fun `Given a staff member and the teams they are in, search for offenders within their teams`() {
    deliusMockServer.stubGetTeamManagedCases()
    deliusMockServer.stubGetCheckUserAccess()
    prisonApiMockServer.stubGetCourtOutcomes()

    val resultObject = webTestClient.post()
      .uri("/caseload/com/case-search")
      .bodyValue(aProbationUserSearchRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ComSearchResponse::class.java)
      .returnResult().responseBody

    val resultsList = resultObject?.results
    val offender = resultsList?.first()
    val inPrisonCount = resultObject?.inPrisonCount
    val onProbationCount = resultObject?.onProbationCount

    assertThat(resultsList?.size).isEqualTo(2)
    assertThat(offender)
      .extracting {
        tuple(
          it?.name, it?.crn, it?.nomisId, it?.comName, it?.comStaffCode, it?.teamName, it?.releaseDate, it?.licenceId,
          it?.licenceType, it?.licenceStatus, it?.isOnProbation,
        )
      }
      .isEqualTo(
        tuple(
          "Test Surname",
          "CRN1",
          "A1234AA",
          "Staff Surname",
          "A01B02C",
          "Test Team",
          LocalDate.now(),
          1L,
          LicenceType.AP,
          LicenceStatus.IN_PROGRESS,
          false,
        ),
      )

    assertThat(inPrisonCount).isEqualTo(2)
    assertThat(onProbationCount).isEqualTo(0)
  }

  @Test
  fun `Given a staff member and the teams they are in, search for offenders within their teams where the offender does not already have a licence`() {
    deliusMockServer.stubGetTeamManagedCases()
    deliusMockServer.stubGetCheckUserAccess()
    prisonerSearchApiMockServer.stubSearchPrisonersByNomisIds()
    prisonApiMockServer.stubGetHdcLatest(123L)
    prisonApiMockServer.stubGetCourtOutcomes()

    val resultObject = webTestClient.post()
      .uri("/caseload/com/case-search")
      .bodyValue(aProbationUserSearchRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ComSearchResponse::class.java)
      .returnResult().responseBody

    val resultsList = resultObject?.results
    val offender = resultsList?.first()
    val inPrisonCount = resultObject?.inPrisonCount
    val onProbationCount = resultObject?.onProbationCount

    assertThat(resultsList?.size).isEqualTo(2)
    assertThat(offender)
      .extracting {
        tuple(
          it?.name, it?.crn, it?.nomisId, it?.comName, it?.comStaffCode, it?.teamName, it?.releaseDate, it?.licenceId,
          it?.licenceType, it?.licenceStatus, it?.isOnProbation,
        )
      }
      .isEqualTo(
        tuple(
          "Test Surname",
          "CRN1",
          "A1234AA",
          "Staff Surname",
          "A01B02C",
          "Test Team",
          prisonerSearchApiMockServer.nextWorkingDate(),
          null,
          LicenceType.AP,
          LicenceStatus.TIMED_OUT,
          false,
        ),
      )

    assertThat(inPrisonCount).isEqualTo(2)
    assertThat(onProbationCount).isEqualTo(0)
  }

  @Test
  fun `Given a staff member has searched for an offender that has served there time and has no licence then the case is time served`() {
    // Given
    deliusMockServer.stubGetTeamManagedCases()
    deliusMockServer.stubGetCheckUserAccess()
    prisonerSearchApiMockServer.stubSearchPrisonersByNomisIds(
      prisonId = "MDI",
      sentenceStartDate = LocalDate.now(),
      confirmedReleaseDate = LocalDate.now(),
      conditionalReleaseDate = LocalDate.now(),
    )
    prisonApiMockServer.stubGetHdcLatest(123L)
    prisonApiMockServer.stubGetCourtOutcomes()

    // When
    val result = webTestClient.post()
      .uri("/caseload/com/case-search")
      .bodyValue(aProbationUserSearchRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()

    // Then
    result.expectStatus().isOk
    result.expectHeader().contentType(MediaType.APPLICATION_JSON)

    val searchResult = result.expectBody(ComSearchResponse::class.java)
      .returnResult().responseBody

    assertThat(searchResult).isNotNull
    val resultsList = searchResult!!.results
    assertThat(resultsList.size).isEqualTo(2)
    assertThat(resultsList[0].kind).isEqualTo(LicenceKind.TIME_SERVED)
    assertThat(resultsList[1].kind).isEqualTo(LicenceKind.HARD_STOP)
  }

  @Test
  fun `Given a COM user has searched for an offender with a not started time served case which is unallocated to a probation practitioner`() {
    // Given
    deliusMockServer.stubGetTeamManagedUnallocatedCases()
    deliusMockServer.stubGetCheckUserAccess()
    prisonApiMockServer.stubGetCourtOutcomes()
    prisonerSearchApiMockServer.stubSearchPrisonersByNomisIds(
      prisonId = "MDI",
      sentenceStartDate = LocalDate.now(),
      confirmedReleaseDate = LocalDate.now(),
      conditionalReleaseDate = LocalDate.now(),
    )

    // When
    val result = webTestClient.post()
      .uri("/caseload/com/case-search")
      .bodyValue(aProbationUserSearchRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()

    // Then
    result.expectStatus().isOk
    result.expectHeader().contentType(MediaType.APPLICATION_JSON)
    val searchResult = result.expectBody(ComSearchResponse::class.java)
      .returnResult().responseBody

    assertThat(searchResult).isNotNull
    val resultsList = searchResult!!.results
    assertThat(resultsList.size).isEqualTo(1)
    assertThat(resultsList[0].kind).isEqualTo(LicenceKind.TIME_SERVED)
    assertThat(resultsList[0].comName).isNull()
    assertThat(resultsList[0].comStaffCode).isNull()
  }

  @Test
  @Sql(
    "classpath:test_data/seed-time-served-licence-id-1.sql",
  )
  fun `Given a staff member has searched for an offender that has served there time and has a licence then the case is time served`() {
    // Given
    deliusMockServer.stubGetTeamManagedCases()
    deliusMockServer.stubGetCheckUserAccess()
    prisonApiMockServer.stubGetCourtOutcomes()
    prisonerSearchApiMockServer.stubSearchPrisonersByNomisIds(
      prisonId = "MDI",
      sentenceStartDate = LocalDate.now(),
      confirmedReleaseDate = LocalDate.now(),
      conditionalReleaseDate = LocalDate.now(),
    )

    // When
    val result = webTestClient.post()
      .uri("/caseload/com/case-search")
      .bodyValue(aProbationUserSearchRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()

    // Then
    result.expectStatus().isOk
    result.expectHeader().contentType(MediaType.APPLICATION_JSON)
    val searchResult = result.expectBody(ComSearchResponse::class.java)
      .returnResult().responseBody

    assertThat(searchResult).isNotNull
    val resultsList = searchResult!!.results
    assertThat(resultsList.size).isEqualTo(2)
    assertThat(resultsList[0].kind).isEqualTo(LicenceKind.TIME_SERVED)
    assertThat(resultsList[1].kind).isEqualTo(LicenceKind.HARD_STOP)
  }

  @Test
  @Sql(
    "classpath:test_data/seed-time-served-licence-id-1.sql",
  )
  fun `Given a COM user has searched for an offender with a started time served case which is unallocated to a probation practitioner`() {
    // Given
    deliusMockServer.stubGetTeamManagedUnallocatedCases()
    deliusMockServer.stubGetCheckUserAccess()
    prisonApiMockServer.stubGetCourtOutcomes()
    prisonerSearchApiMockServer.stubSearchPrisonersByNomisIds(
      prisonId = "MDI",
      sentenceStartDate = LocalDate.now(),
      confirmedReleaseDate = LocalDate.now(),
      conditionalReleaseDate = LocalDate.now(),
    )

    // When
    val result = webTestClient.post()
      .uri("/caseload/com/case-search")
      .bodyValue(aProbationUserSearchRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()

    // Then
    result.expectStatus().isOk
    result.expectHeader().contentType(MediaType.APPLICATION_JSON)
    val searchResult = result.expectBody(ComSearchResponse::class.java)
      .returnResult().responseBody

    assertThat(searchResult).isNotNull
    val resultsList = searchResult!!.results
    assertThat(resultsList.size).isEqualTo(1)
    assertThat(resultsList[0].kind).isEqualTo(LicenceKind.TIME_SERVED)
    assertThat(resultsList[0].comName).isNull()
    assertThat(resultsList[0].comStaffCode).isNull()
  }

  @Test
  fun `Given a staff member and the teams they are in, search for offenders within their teams with no results from team caseload`() {
    prisonApiMockServer.stubGetCourtOutcomes()

    val resultList = webTestClient.post()
      .uri("/caseload/com/case-search")
      .bodyValue(aProbationUserSearchRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ComSearchResponse::class.java)
      .returnResult().responseBody

    assertThat(resultList?.results?.size).isEqualTo(0)
  }

  @Test
  fun `Given a staff member and the teams they are in, search for offenders within their teams with no results from prisoner search`() {
    prisonApiMockServer.stubGetCourtOutcomes()
    deliusMockServer.stubGetTeamManagedCases()
    deliusMockServer.stubGetCheckUserAccess()
    prisonerSearchApiMockServer.stubSearchPrisonersByNomisIdsNoResult()

    val resultList = webTestClient.post()
      .uri("/caseload/com/case-search")
      .bodyValue(aProbationUserSearchRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ComSearchResponse::class.java)
      .returnResult().responseBody

    assertThat(resultList?.results?.size).isEqualTo(0)
  }

  @Test
  fun `Given an offender not on probation and their licences is ACTIVE AND CRD and ARD in future When search for offender Then offender should be part of the search results `() {
    deliusMockServer.stubGetTeamManagedCases()
    deliusMockServer.stubGetCheckUserAccess()
    prisonerSearchApiMockServer.stubSearchPrisonersByNomisIds()
    prisonApiMockServer.stubGetHdcLatest(123L)
    prisonApiMockServer.stubGetCourtOutcomes()

    val resultObject = webTestClient.post()
      .uri("/caseload/com/case-search")
      .bodyValue(aProbationUserSearchRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ComSearchResponse::class.java)
      .returnResult().responseBody

    val resultsList = resultObject?.results
    val offender = resultsList?.first()
    val inPrisonCount = resultObject?.inPrisonCount
    val onProbationCount = resultObject?.onProbationCount

    assertThat(resultsList?.size).isEqualTo(2)
    assertThat(offender)
      .extracting {
        tuple(
          it?.name, it?.crn, it?.nomisId, it?.comName, it?.comStaffCode, it?.teamName, it?.releaseDate, it?.licenceId,
          it?.licenceType, it?.licenceStatus, it?.isOnProbation,
        )
      }
      .isEqualTo(
        tuple(
          "Test Surname",
          "CRN1",
          "A1234AA",
          "Staff Surname",
          "A01B02C",
          "Test Team",
          prisonerSearchApiMockServer.nextWorkingDate(),
          null,
          LicenceType.AP,
          LicenceStatus.TIMED_OUT,
          false,
        ),
      )

    assertThat(inPrisonCount).isEqualTo(2)
    assertThat(onProbationCount).isEqualTo(0)
  }

  @Test
  fun `Given an offender is on probation and their licences is ACTIVE AND CRD and ARD in past When search for offender Then offender should be part of the search results `() {
    deliusMockServer.stubGetTeamManagedCases()
    deliusMockServer.stubGetCheckUserAccess()
    prisonerSearchApiMockServer.stubSearchPrisonersByNomisIds()
    prisonApiMockServer.stubGetHdcLatest(123L)
    prisonApiMockServer.stubGetCourtOutcomes()

    val resultObject = webTestClient.post()
      .uri("/caseload/com/case-search")
      .bodyValue(aProbationUserSearchRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ComSearchResponse::class.java)
      .returnResult().responseBody?.results

    assertThat(resultObject).filteredOn("nomisId", "A1234AD").isNotEmpty
  }

  @Test
  fun `Given an offender is not in jail and licence is INACTIVE When search for offender Then offender should not be part of the search results `() {
    deliusMockServer.stubGetTeamManagedCases()
    deliusMockServer.stubGetCheckUserAccess()
    prisonerSearchApiMockServer.stubSearchPrisonersByNomisIds()
    prisonApiMockServer.stubGetHdcLatest(123L)
    prisonApiMockServer.stubGetCourtOutcomes()

    val resultObject = webTestClient.post()
      .uri("/caseload/com/case-search")
      .bodyValue(aProbationUserSearchRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ComSearchResponse::class.java)
      .returnResult().responseBody?.results
    assertThat(resultObject).filteredOn("nomisId", "A1234AE").isEmpty()
  }

  @Test
  fun `Given an offender is a LAO without a licence, When searching for an offender Then offender should be part of the search results `() {
    aProbationUserSearchRequest = aProbationUserSearchRequest.copy(query = "A123456")
    val accessResponse = """
      {
        "access": [
          {
            "crn": "CRN1",
            "userExcluded": true,
            "userRestricted": false,
            "exclusionMessage": "Access restricted on NDelius"
          },
          {
            "crn": "CRN2",
            "userExcluded": false,
            "userRestricted": false
          }
        ]
      }
    """.trimIndent()
    deliusMockServer.stubGetTeamManagedCases()
    deliusMockServer.stubGetCheckUserAccess(accessResponse)
    prisonerSearchApiMockServer.stubSearchPrisonersByNomisIds()
    prisonApiMockServer.stubGetHdcLatest(123L)
    prisonApiMockServer.stubGetCourtOutcomes()

    val resultObject = webTestClient.post()
      .uri("/caseload/com/case-search")
      .bodyValue(aProbationUserSearchRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()

    resultObject.expectStatus().isOk
    resultObject.expectHeader().contentType(MediaType.APPLICATION_JSON)

    val searchResult = resultObject.expectBody(ComSearchResponse::class.java)
      .returnResult().responseBody

    assertThat(searchResult).isNotNull

    val resultsList = searchResult!!.results
    val inPrisonCount = searchResult.inPrisonCount
    val onProbationCount = searchResult.onProbationCount

    assertThat(resultsList.size).isEqualTo(2)

    with(resultsList.first()) {
      assertThat(name).isEqualTo("Access restricted on NDelius")
      assertThat(crn).isEqualTo("CRN1")
      assertThat(probationPractitioner.name).isEqualTo("Restricted")
      assertThat(probationPractitioner.staffCode).isEqualTo("Restricted")
      assertThat(releaseDate).isNull()
      assertThat(isOnProbation).isFalse()
      assertThat(isLao).isTrue()
    }

    assertThat(inPrisonCount).isEqualTo(2)
    assertThat(onProbationCount).isEqualTo(0)
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql",
  )
  fun `Given an offender is a LAO with a licence, When searching for an offender Then offender should be part of the search results `() {
    aProbationUserSearchRequest = aProbationUserSearchRequest.copy(query = "A123456")
    val accessResponse = """
      {
        "access": [
          {
            "crn": "CRN1",
            "userExcluded": false,
            "userRestricted": true,
            "restrictionMessage": "Access restricted on NDelius"
          },
          {
            "crn": "CRN2",
            "userExcluded": false,
            "userRestricted": false
          }
        ]
      }
    """.trimIndent()
    deliusMockServer.stubGetTeamManagedCases()
    deliusMockServer.stubGetCheckUserAccess(accessResponse)
    prisonerSearchApiMockServer.stubSearchPrisonersByNomisIds(
      prisonId = "MDI",
      sentenceStartDate = LocalDate.now(),
      confirmedReleaseDate = LocalDate.now(),
      conditionalReleaseDate = LocalDate.now(),
    )
    prisonApiMockServer.stubGetHdcLatest(123L)
    prisonApiMockServer.stubGetCourtOutcomes()

    val resultObject = webTestClient.post()
      .uri("/caseload/com/case-search")
      .bodyValue(aProbationUserSearchRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()

    resultObject.expectStatus().isOk
    resultObject.expectHeader().contentType(MediaType.APPLICATION_JSON)

    val searchResult = resultObject.expectBody(ComSearchResponse::class.java)
      .returnResult().responseBody

    assertThat(searchResult).isNotNull

    val resultsList = searchResult!!.results
    val inPrisonCount = searchResult.inPrisonCount
    val onProbationCount = searchResult.onProbationCount

    assertThat(resultsList.size).isEqualTo(2)

    with(resultsList.first()) {
      assertThat(name).isEqualTo("Access restricted on NDelius")
      assertThat(crn).isEqualTo("CRN1")
      assertThat(probationPractitioner.name).isEqualTo("Restricted")
      assertThat(probationPractitioner.staffCode).isEqualTo("Restricted")
      assertThat(releaseDate).isNull()
      assertThat(isOnProbation).isFalse()
      assertThat(isLao).isTrue()
    }

    assertThat(inPrisonCount).isEqualTo(2)
    assertThat(onProbationCount).isEqualTo(0)
  }

  @Test
  fun `Given an offender is a LAO without a licence, When searching for an offender without their CRN, The offender should not be part of the search results `() {
    aProbationUserSearchRequest = aProbationUserSearchRequest.copy(query = "Surname")
    val accessResponse = """
      {
        "access": [
          {
            "crn": "CRN1",
            "userExcluded": true,
            "userRestricted": false,
            "exclusionMessage": "Access restricted on NDelius"
          },
          {
            "crn": "CRN2",
            "userExcluded": false,
            "userRestricted": false
          }
        ]
      }
    """.trimIndent()
    deliusMockServer.stubGetTeamManagedCases()
    deliusMockServer.stubGetCheckUserAccess(accessResponse)
    prisonerSearchApiMockServer.stubSearchPrisonersByNomisIds()
    prisonApiMockServer.stubGetHdcLatest(123L)
    prisonApiMockServer.stubGetCourtOutcomes()

    val resultObject = webTestClient.post()
      .uri("/caseload/com/case-search")
      .bodyValue(aProbationUserSearchRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()

    resultObject.expectStatus().isOk
    resultObject.expectHeader().contentType(MediaType.APPLICATION_JSON)

    val searchResult = resultObject.expectBody(ComSearchResponse::class.java)
      .returnResult().responseBody

    assertThat(searchResult).isNotNull

    val resultsList = searchResult!!.results

    assertThat(resultsList.size).isEqualTo(1)

    assertThat(resultsList).filteredOn("crn", "CRN1").isEmpty()
  }

  private companion object {
    val deliusMockServer = DeliusMockServer()
    val prisonerSearchApiMockServer = PrisonerSearchMockServer()
    val prisonApiMockServer = PrisonApiMockServer()
    val govUkMockServer = GovUkMockServer()

    var aProbationUserSearchRequest = ProbationUserSearchRequest(
      "Surname",
      1L,
      listOf(
        ProbationSearchSortBy(SearchField.FORENAME, Sort.Direction.ASC),
      ),
    )

    @JvmStatic
    @BeforeAll
    fun startMocks() {
      deliusMockServer.start()
      prisonerSearchApiMockServer.start()
      prisonApiMockServer.start()

      govUkMockServer.start()
      govUkMockServer.stubGetBankHolidaysForEnglandAndWales()
    }

    @JvmStatic
    @AfterAll
    fun stopMocks() {
      deliusMockServer.stop()
      prisonerSearchApiMockServer.stop()
      prisonApiMockServer.stop()
      govUkMockServer.stop()
    }
  }
}
