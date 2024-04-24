package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple.tuple
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.ProbationUserSearchRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createCrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createHardStopLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerHdcStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.CaseloadResult
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.CommunityApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.Detail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.Identifiers
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.Manager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.Name
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ProbationSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.request.ProbationSearchSortByRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.ProbationSearchSortBy
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.SearchDirection
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.SearchField
import java.time.LocalDate

class PrisonerSearchServiceTest {
  private val licenceRepository = mock<LicenceRepository>()
  private val communityApiClient = mock<CommunityApiClient>()
  private val probationSearchApiClient = mock<ProbationSearchApiClient>()
  private val prisonerSearchApiClient = mock<PrisonerSearchApiClient>()
  private val prisonApiClient = mock<PrisonApiClient>()
  private val eligibilityService = mock<EligibilityService>()
  private val releaseDateService = mock<ReleaseDateService>()

  private val service =
    PrisonerSearchService(
      licenceRepository,
      communityApiClient,
      probationSearchApiClient,
      prisonerSearchApiClient,
      prisonApiClient,
      eligibilityService,
      releaseDateService,
      hardStopEnabled = true,
    )

  @BeforeEach
  fun reset() {
    reset(
      licenceRepository,
      communityApiClient,
      probationSearchApiClient,
      prisonerSearchApiClient,
      prisonApiClient,
      eligibilityService,
      releaseDateService,
    )
  }

  @Test
  fun `search for offenders in prison on a staff member's caseload`() {
    whenever(communityApiClient.getTeamsCodesForUser(2000)).thenReturn(
      listOf(
        "A01B02",
      ),
    )
    whenever(probationSearchApiClient.searchLicenceCaseloadByTeam("Test", listOf("A01B02"))).thenReturn(
      listOf(
        CaseloadResult(
          Name("Test", "Surname"),
          Identifiers("X12345", "A1234AA"),
          Manager(
            "A01B02C",
            Name("Staff", "Surname"),
            Detail("A01B02", "Test Team"),
          ),
          "2023/05/24",
        ),
      ),
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn((listOf(aLicenceEntity)))

    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(aPrisonerSearchResult.prisonerNumber))).thenReturn(
      listOf(
        aPrisonerSearchResult,
      ),
    )

    whenever(eligibilityService.isEligibleForCvl(aPrisonerSearchResult)).thenReturn(
      true,
    )

    val request = ProbationUserSearchRequest(
      "Test",
      2000,
    )

    val result = service.searchForOffenderOnStaffCaseload(request)

    verify(probationSearchApiClient).searchLicenceCaseloadByTeam(
      request.query,
      communityApiClient.getTeamsCodesForUser(request.staffIdentifier),
    )

    verify(eligibilityService).isEligibleForCvl(
      aPrisonerSearchResult,
    )

    verify(prisonApiClient).getHdcStatuses(
      emptyList(),
    )

    val resultsList = result.results
    val offender = resultsList.first()
    val inPrisonCount = result.inPrisonCount
    val onProbationCount = result.onProbationCount

    assertThat(resultsList).isNotEmpty
    assertThat(resultsList.size).isEqualTo(1)

    assertThat(offender)
      .extracting {
        tuple(
          it.name, it.crn, it.nomisId, it.comName, it.comStaffCode, it.teamName, it.releaseDate,
          it.licenceId, it.licenceType, it.licenceStatus, it.isOnProbation,
        )
      }
      .isEqualTo(
        tuple(
          "Test Surname",
          "X12345",
          "A1234AA",
          "Staff Surname",
          "A01B02C",
          "Test Team",
          LocalDate.parse("2021-10-22"),
          1L,
          LicenceType.AP,
          LicenceStatus.IN_PROGRESS,
          false,
        ),
      )

    assertThat(inPrisonCount).isEqualTo(1)
    assertThat(onProbationCount).isEqualTo(0)
  }

  @Test
  fun `search for offenders in prison on a staff member's caseload with results sorted`() {
    whenever(communityApiClient.getTeamsCodesForUser(2000)).thenReturn(
      listOf(
        "A01B02",
      ),
    )
    whenever(
      probationSearchApiClient.searchLicenceCaseloadByTeam(
        "Test",
        listOf("A01B02"),
        listOf(
          ProbationSearchSortByRequest(SearchField.SURNAME.probationSearchApiSortType, "asc"),
          ProbationSearchSortByRequest(SearchField.COM_FORENAME.probationSearchApiSortType, "desc"),
        ),
      ),
    ).thenReturn(
      listOf(
        CaseloadResult(
          Name("Test", "Surname"),
          Identifiers("A123456", "A1234AA"),
          Manager(
            "A01B02C",
            Name("Staff", "Surname"),
            Detail("A01B02", "Test Team"),
          ),
          "2023/05/24",
        ),
      ),
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn((listOf(aLicenceEntity)))

    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(aPrisonerSearchResult.prisonerNumber))).thenReturn(
      listOf(
        aPrisonerSearchResult,
      ),
    )

    whenever(eligibilityService.isEligibleForCvl(aPrisonerSearchResult)).thenReturn(
      true,
    )

    val request = ProbationUserSearchRequest(
      "Test",
      2000,
      listOf(
        ProbationSearchSortBy(SearchField.SURNAME, SearchDirection.ASC),
        ProbationSearchSortBy(SearchField.COM_FORENAME, SearchDirection.DESC),
      ),
    )

    val result = service.searchForOffenderOnStaffCaseload(request)

    verify(probationSearchApiClient).searchLicenceCaseloadByTeam(
      request.query,
      communityApiClient.getTeamsCodesForUser(request.staffIdentifier),
      listOf(
        ProbationSearchSortByRequest(SearchField.SURNAME.probationSearchApiSortType, "asc"),
        ProbationSearchSortByRequest(SearchField.COM_FORENAME.probationSearchApiSortType, "desc"),
      ),
    )

    verify(eligibilityService).isEligibleForCvl(
      aPrisonerSearchResult,
    )

    verify(prisonApiClient).getHdcStatuses(
      emptyList(),
    )

    val resultsList = result.results
    val offender = resultsList.first()

    assertThat(resultsList.size).isEqualTo(1)
    assertThat(offender)
      .extracting { tuple(it.name, it.comName, it.teamName) }
      .isEqualTo(
        tuple("Test Surname", "Staff Surname", "Test Team"),
      )
  }

  @Test
  fun `search for offenders in prison on a staff member's caseload with latest licence selected`() {
    whenever(communityApiClient.getTeamsCodesForUser(2000)).thenReturn(
      listOf(
        "A01B02",
      ),
    )
    whenever(probationSearchApiClient.searchLicenceCaseloadByTeam("Test", listOf("A01B02"))).thenReturn(
      listOf(
        CaseloadResult(
          Name("Test", "Surname"),
          Identifiers("A123456", "A1234AA"),
          Manager(
            "A01B02C",
            Name("Staff", "Surname"),
            Detail("A01B02", "Test Team"),
          ),
          "2023/05/24",
        ),
      ),
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any()))
      .thenReturn(
        (
          listOf(
            aLicenceEntity.copy(
              statusCode = LicenceStatus.ACTIVE,
            ),
            aLicenceEntity.copy(
              statusCode = LicenceStatus.APPROVED,
            ),
          )
          ),
      )
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(
      listOf(
        aPrisonerSearchResult,
      ),
    )
    whenever(eligibilityService.isEligibleForCvl(any())).thenReturn(
      true,
    )

    val request = ProbationUserSearchRequest(
      "Test",
      2000,
    )

    val result = service.searchForOffenderOnStaffCaseload(request)

    verify(probationSearchApiClient).searchLicenceCaseloadByTeam(
      request.query,
      communityApiClient.getTeamsCodesForUser(request.staffIdentifier),
    )
    val resultsList = result.results
    val offender = resultsList.first()
    val inPrisonCount = result.inPrisonCount
    val onProbationCount = result.onProbationCount

    assertThat(resultsList).isNotEmpty
    assertThat(resultsList.size).isEqualTo(1)

    assertThat(offender)
      .extracting {
        tuple(
          it.name, it.crn, it.nomisId, it.comName, it.comStaffCode, it.teamName, it.releaseDate,
          it.licenceId, it.licenceType, it.licenceStatus, it.isOnProbation,
        )
      }
      .isEqualTo(
        tuple(
          "Test Surname",
          "X12345",
          "A1234AA",
          "Staff Surname",
          "A01B02C",
          "Test Team",
          LocalDate.parse("2021-10-22"),
          1L,
          LicenceType.AP,
          LicenceStatus.APPROVED,
          false,
        ),
      )

    assertThat(inPrisonCount).isEqualTo(1)
    assertThat(onProbationCount).isEqualTo(0)
  }

  @Test
  fun `search for offenders on probation on a staff member's caseload with latest licence selected`() {
    whenever(communityApiClient.getTeamsCodesForUser(2000)).thenReturn(
      listOf(
        "A01B02",
      ),
    )
    whenever(probationSearchApiClient.searchLicenceCaseloadByTeam("Test", listOf("A01B02"))).thenReturn(
      listOf(
        CaseloadResult(
          Name("Test", "Surname"),
          Identifiers("X12345", "A1234AA"),
          Manager(
            "A01B02C",
            Name("Staff", "Surname"),
            Detail("A01B02", "Test Team"),
          ),
          "2023/05/24",
        ),
      ),
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any()))
      .thenReturn(
        (
          listOf(
            aLicenceEntity.copy(
              statusCode = LicenceStatus.ACTIVE,
            ),
            aLicenceEntity.copy(
              statusCode = LicenceStatus.VARIATION_SUBMITTED,
            ),
          )
          ),
      )

    whenever(eligibilityService.isEligibleForCvl(aPrisonerSearchResult)).thenReturn(
      true,
    )

    val request = ProbationUserSearchRequest(
      "Test",
      2000,
    )

    val result = service.searchForOffenderOnStaffCaseload(request)

    verify(probationSearchApiClient).searchLicenceCaseloadByTeam(
      request.query,
      communityApiClient.getTeamsCodesForUser(request.staffIdentifier),
    )
    verifyNoInteractions(prisonApiClient)
    verifyNoInteractions(prisonerSearchApiClient)
    verifyNoInteractions(eligibilityService)

    val resultsList = result.results
    val offender = resultsList.first()
    val inPrisonCount = result.inPrisonCount
    val onProbationCount = result.onProbationCount

    assertThat(resultsList).isNotEmpty
    assertThat(resultsList.size).isEqualTo(1)

    assertThat(offender)
      .extracting {
        tuple(
          it.name, it.crn, it.nomisId, it.comName, it.comStaffCode, it.teamName, it.releaseDate,
          it.licenceId, it.licenceType, it.licenceStatus, it.isOnProbation,
        )
      }
      .isEqualTo(
        tuple(
          "Test Surname",
          "X12345",
          "A1234AA",
          "Staff Surname",
          "A01B02C",
          "Test Team",
          LocalDate.parse("2021-10-22"),
          1L,
          LicenceType.AP,
          LicenceStatus.VARIATION_SUBMITTED,
          true,
        ),
      )

    assertThat(inPrisonCount).isEqualTo(0)
    assertThat(onProbationCount).isEqualTo(1)
  }

  @Test
  fun `search for offenders on probation on a staff member's caseload with no CRD should use ARD`() {
    whenever(communityApiClient.getTeamsCodesForUser(2000)).thenReturn(
      listOf(
        "A01B02",
      ),
    )
    whenever(probationSearchApiClient.searchLicenceCaseloadByTeam("Test", listOf("A01B02"))).thenReturn(
      listOf(
        CaseloadResult(
          Name("Test", "Surname"),
          Identifiers("A123456", "A1234AA"),
          Manager(
            "A01B02C",
            Name("Staff", "Surname"),
            Detail("A01B02", "Test Team"),
          ),
          "2023/05/24",
        ),
      ),
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any()))
      .thenReturn(
        (
          listOf(
            aLicenceEntity.copy(
              statusCode = LicenceStatus.ACTIVE,
            ),
            aLicenceEntity.copy(
              statusCode = LicenceStatus.VARIATION_IN_PROGRESS,
              conditionalReleaseDate = null,
              actualReleaseDate = LocalDate.parse("2023-07-27"),
            ),
          )
          ),
      )

    val request = ProbationUserSearchRequest(
      "Test",
      2000,
    )

    val result = service.searchForOffenderOnStaffCaseload(request)

    verify(probationSearchApiClient).searchLicenceCaseloadByTeam(
      request.query,
      communityApiClient.getTeamsCodesForUser(request.staffIdentifier),
    )

    verifyNoInteractions(eligibilityService)

    val resultsList = result.results
    val offender = resultsList.first()
    val inPrisonCount = result.inPrisonCount
    val onProbationCount = result.onProbationCount

    assertThat(resultsList).isNotEmpty
    assertThat(resultsList.size).isEqualTo(1)

    assertThat(offender)
      .extracting {
        tuple(
          it.name, it.crn, it.nomisId, it.comName, it.comStaffCode, it.teamName, it.releaseDate,
          it.licenceId, it.licenceType, it.licenceStatus, it.isOnProbation,
        )
      }
      .isEqualTo(
        tuple(
          "Test Surname",
          "X12345",
          "A1234AA",
          "Staff Surname",
          "A01B02C",
          "Test Team",
          LocalDate.parse("2023-07-27"),
          1L,
          LicenceType.AP,
          LicenceStatus.VARIATION_IN_PROGRESS,
          true,
        ),
      )

    assertThat(inPrisonCount).isEqualTo(0)
    assertThat(onProbationCount).isEqualTo(1)
  }

  @Test
  fun `search for offenders in prison on a staff member's caseload without a licence (NOT_STARTED)`() {
    whenever(communityApiClient.getTeamsCodesForUser(2000)).thenReturn(
      listOf(
        "A01B02",
      ),
    )
    whenever(probationSearchApiClient.searchLicenceCaseloadByTeam("Test", listOf("A01B02"))).thenReturn(
      listOf(
        CaseloadResult(
          Name("Test", "Surname"),
          Identifiers("A123456", "A1234AA"),
          Manager(
            "A01B02C",
            Name("Staff", "Surname"),
            Detail("A01B02", "Test Team"),
          ),
          "2023/05/24",
        ),
      ),
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any()))
      .thenReturn(
        emptyList(),
      )

    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(aPrisonerSearchResult.prisonerNumber))).thenReturn(
      listOf(
        aPrisonerSearchResult,
      ),
    )

    whenever(eligibilityService.isEligibleForCvl(aPrisonerSearchResult)).thenReturn(
      true,
    )

    val request = ProbationUserSearchRequest(
      "Test",
      2000,
    )

    val result = service.searchForOffenderOnStaffCaseload(request)

    verify(probationSearchApiClient).searchLicenceCaseloadByTeam(
      request.query,
      communityApiClient.getTeamsCodesForUser(request.staffIdentifier),
    )

    verify(prisonerSearchApiClient).searchPrisonersByNomisIds(
      listOf(aPrisonerSearchResult.prisonerNumber),
    )

    verify(eligibilityService).isEligibleForCvl(
      aPrisonerSearchResult,
    )

    verify(prisonApiClient).getHdcStatuses(
      emptyList(),
    )

    val resultsList = result.results
    val offender = resultsList.first()
    val inPrisonCount = result.inPrisonCount
    val onProbationCount = result.onProbationCount

    assertThat(resultsList).isNotEmpty
    assertThat(resultsList.size).isEqualTo(1)

    assertThat(offender)
      .extracting {
        tuple(
          it.name, it.crn, it.nomisId, it.comName, it.comStaffCode, it.teamName, it.releaseDate,
          it.licenceId, it.licenceType, it.licenceStatus, it.isOnProbation,
        )
      }
      .isEqualTo(
        tuple(
          "Test Surname",
          "A123456",
          "A1234AA",
          "Staff Surname",
          "A01B02C",
          "Test Team",
          LocalDate.parse("2023-09-14"),
          null,
          LicenceType.AP,
          LicenceStatus.NOT_STARTED,
          false,
        ),
      )

    assertThat(inPrisonCount).isEqualTo(1)
    assertThat(onProbationCount).isEqualTo(0)
  }

  @Test
  fun `search for offenders in prison on a staff member's caseload without a licence where NOMIS ID is not populated`() {
    whenever(communityApiClient.getTeamsCodesForUser(2000)).thenReturn(
      listOf(
        "A01B02",
      ),
    )
    whenever(probationSearchApiClient.searchLicenceCaseloadByTeam("Test", listOf("A01B02"))).thenReturn(
      listOf(
        CaseloadResult(
          Name("Test", "Surname"),
          Identifiers("A123456", null),
          Manager(
            "A01B02C",
            Name("Staff", "Surname"),
            Detail("A01B02", "Test Team"),
          ),
          "2023/05/24",
        ),
      ),
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any()))
      .thenReturn(
        emptyList(),
      )

    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(aPrisonerSearchResult.prisonerNumber))).thenReturn(
      listOf(
        aPrisonerSearchResult,
      ),
    )

    val request = ProbationUserSearchRequest(
      "Test",
      2000,
    )

    val result = service.searchForOffenderOnStaffCaseload(request)

    verify(probationSearchApiClient).searchLicenceCaseloadByTeam(
      request.query,
      communityApiClient.getTeamsCodesForUser(request.staffIdentifier),
    )

    verifyNoInteractions(prisonerSearchApiClient)
    verifyNoInteractions(eligibilityService)
    verifyNoInteractions(prisonApiClient)

    val resultsList = result.results
    val inPrisonCount = result.inPrisonCount
    val onProbationCount = result.onProbationCount

    assertThat(resultsList).isEmpty()
    assertThat(resultsList.size).isEqualTo(0)
    assertThat(inPrisonCount).isEqualTo(0)
    assertThat(onProbationCount).isEqualTo(0)
  }

  @Test
  fun `search for offenders in prison on a staff member's caseload without a licence sets PSS licence type correctly `() {
    whenever(communityApiClient.getTeamsCodesForUser(2000)).thenReturn(
      listOf(
        "A01B02",
      ),
    )
    whenever(probationSearchApiClient.searchLicenceCaseloadByTeam("Test", listOf("A01B02"))).thenReturn(
      listOf(
        CaseloadResult(
          Name("Test", "Surname"),
          Identifiers("A123456", "A1234AA"),
          Manager(
            "A01B02C",
            Name("Staff", "Surname"),
            Detail("A01B02", "Test Team"),
          ),
          "2023/05/24",
        ),
      ),
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any()))
      .thenReturn(
        emptyList(),
      )

    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(aPrisonerSearchResult.prisonerNumber))).thenReturn(
      listOf(
        aPrisonerSearchResult.copy(
          licenceExpiryDate = null,
        ),
      ),
    )

    whenever(eligibilityService.isEligibleForCvl(any())).thenReturn(
      true,
    )

    val request = ProbationUserSearchRequest(
      "Test",
      2000,
    )

    val result = service.searchForOffenderOnStaffCaseload(request)

    verify(probationSearchApiClient).searchLicenceCaseloadByTeam(
      request.query,
      communityApiClient.getTeamsCodesForUser(request.staffIdentifier),
    )

    verify(prisonerSearchApiClient).searchPrisonersByNomisIds(
      listOf(aPrisonerSearchResult.prisonerNumber),
    )

    verify(eligibilityService).isEligibleForCvl(any())
    verify(prisonApiClient).getHdcStatuses(
      emptyList(),
    )

    val resultsList = result.results
    val offender = resultsList.first()
    val inPrisonCount = result.inPrisonCount
    val onProbationCount = result.onProbationCount

    assertThat(resultsList).isNotEmpty
    assertThat(resultsList.size).isEqualTo(1)

    assertThat(offender)
      .extracting {
        tuple(
          it.name, it.crn, it.nomisId, it.comName, it.comStaffCode, it.teamName, it.releaseDate,
          it.licenceId, it.licenceType, it.licenceStatus, it.isOnProbation,
        )
      }
      .isEqualTo(
        tuple(
          "Test Surname",
          "A123456",
          "A1234AA",
          "Staff Surname",
          "A01B02C",
          "Test Team",
          LocalDate.parse("2023-09-14"),
          null,
          LicenceType.PSS,
          LicenceStatus.NOT_STARTED,
          false,
        ),
      )

    assertThat(inPrisonCount).isEqualTo(1)
    assertThat(onProbationCount).isEqualTo(0)
  }

  @Test
  fun `search for offenders in prison on a staff member's caseload without a licence sets AP licence type correctly where there is no TUSED`() {
    whenever(communityApiClient.getTeamsCodesForUser(2000)).thenReturn(
      listOf(
        "A01B02",
      ),
    )
    whenever(probationSearchApiClient.searchLicenceCaseloadByTeam("Test", listOf("A01B02"))).thenReturn(
      listOf(
        CaseloadResult(
          Name("Test", "Surname"),
          Identifiers("A123456", "A1234AA"),
          Manager(
            "A01B02C",
            Name("Staff", "Surname"),
            Detail("A01B02", "Test Team"),
          ),
          "2023/05/24",
        ),
      ),
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any()))
      .thenReturn(
        emptyList(),
      )

    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(aPrisonerSearchResult.prisonerNumber))).thenReturn(
      listOf(
        aPrisonerSearchResult.copy(
          topupSupervisionExpiryDate = null,
        ),
      ),
    )

    whenever(eligibilityService.isEligibleForCvl(any())).thenReturn(
      true,
    )

    val request = ProbationUserSearchRequest(
      "Test",
      2000,
    )

    val result = service.searchForOffenderOnStaffCaseload(request)

    verify(probationSearchApiClient).searchLicenceCaseloadByTeam(
      request.query,
      communityApiClient.getTeamsCodesForUser(request.staffIdentifier),
    )

    verify(prisonerSearchApiClient).searchPrisonersByNomisIds(
      listOf(aPrisonerSearchResult.prisonerNumber),
    )

    verify(eligibilityService).isEligibleForCvl(
      any(),
    )

    verify(prisonApiClient).getHdcStatuses(
      emptyList(),
    )

    val resultsList = result.results
    val offender = resultsList.first()
    val inPrisonCount = result.inPrisonCount
    val onProbationCount = result.onProbationCount

    assertThat(resultsList).isNotEmpty
    assertThat(resultsList.size).isEqualTo(1)

    assertThat(offender)
      .extracting {
        tuple(
          it.name, it.crn, it.nomisId, it.comName, it.comStaffCode, it.teamName, it.releaseDate,
          it.licenceId, it.licenceType, it.licenceStatus, it.isOnProbation,
        )
      }
      .isEqualTo(
        tuple(
          "Test Surname",
          "A123456",
          "A1234AA",
          "Staff Surname",
          "A01B02C",
          "Test Team",
          LocalDate.parse("2023-09-14"),
          null,
          LicenceType.AP,
          LicenceStatus.NOT_STARTED,
          false,
        ),
      )

    assertThat(inPrisonCount).isEqualTo(1)
    assertThat(onProbationCount).isEqualTo(0)
  }

  @Test
  fun `search for offenders in prison on a staff member's caseload without a licence sets AP licence type correctly where TUSED before LED`() {
    whenever(communityApiClient.getTeamsCodesForUser(2000)).thenReturn(
      listOf(
        "A01B02",
      ),
    )
    whenever(probationSearchApiClient.searchLicenceCaseloadByTeam("Test", listOf("A01B02"))).thenReturn(
      listOf(
        CaseloadResult(
          Name("Test", "Surname"),
          Identifiers("A123456", "A1234AA"),
          Manager(
            "A01B02C",
            Name("Staff", "Surname"),
            Detail("A01B02", "Test Team"),
          ),
          "2023/05/24",
        ),
      ),
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any()))
      .thenReturn(
        emptyList(),
      )

    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(aPrisonerSearchResult.prisonerNumber))).thenReturn(
      listOf(
        aPrisonerSearchResult.copy(
          licenceExpiryDate = LocalDate.parse("2024-09-15"),
          topupSupervisionExpiryDate = LocalDate.parse("2024-09-14"),
        ),
      ),
    )

    whenever(eligibilityService.isEligibleForCvl(any())).thenReturn(
      true,
    )

    val request = ProbationUserSearchRequest(
      "Test",
      2000,
    )

    val result = service.searchForOffenderOnStaffCaseload(request)

    verify(probationSearchApiClient).searchLicenceCaseloadByTeam(
      request.query,
      communityApiClient.getTeamsCodesForUser(request.staffIdentifier),
    )

    verify(prisonerSearchApiClient).searchPrisonersByNomisIds(
      listOf(aPrisonerSearchResult.prisonerNumber),
    )

    verify(eligibilityService).isEligibleForCvl(
      any(),
    )

    verify(prisonApiClient).getHdcStatuses(
      emptyList(),
    )

    val resultsList = result.results
    val offender = resultsList.first()
    val inPrisonCount = result.inPrisonCount
    val onProbationCount = result.onProbationCount

    assertThat(resultsList).isNotEmpty
    assertThat(resultsList.size).isEqualTo(1)

    assertThat(offender)
      .extracting {
        tuple(
          it.name, it.crn, it.nomisId, it.comName, it.comStaffCode, it.teamName, it.releaseDate,
          it.licenceId, it.licenceType, it.licenceStatus, it.isOnProbation,
        )
      }
      .isEqualTo(
        tuple(
          "Test Surname",
          "A123456",
          "A1234AA",
          "Staff Surname",
          "A01B02C",
          "Test Team",
          LocalDate.parse("2023-09-14"),
          null,
          LicenceType.AP,
          LicenceStatus.NOT_STARTED,
          false,
        ),
      )

    assertThat(inPrisonCount).isEqualTo(1)
    assertThat(onProbationCount).isEqualTo(0)
  }

  @Test
  fun `search for offenders in prison on a staff member's caseload without a licence sets AP_PSS licence type correctly`() {
    whenever(communityApiClient.getTeamsCodesForUser(2000)).thenReturn(
      listOf(
        "A01B02",
      ),
    )
    whenever(probationSearchApiClient.searchLicenceCaseloadByTeam("Test", listOf("A01B02"))).thenReturn(
      listOf(
        CaseloadResult(
          Name("Test", "Surname"),
          Identifiers("A123456", "A1234AA"),
          Manager(
            "A01B02C",
            Name("Staff", "Surname"),
            Detail("A01B02", "Test Team"),
          ),
          "2023/05/24",
        ),
      ),
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any()))
      .thenReturn(
        emptyList(),
      )

    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(aPrisonerSearchResult.prisonerNumber))).thenReturn(
      listOf(
        aPrisonerSearchResult.copy(
          licenceExpiryDate = LocalDate.parse("2024-09-15"),
          topupSupervisionExpiryDate = LocalDate.parse("2024-10-14"),
        ),
      ),
    )

    whenever(eligibilityService.isEligibleForCvl(any())).thenReturn(
      true,
    )

    val request = ProbationUserSearchRequest(
      "Test",
      2000,
    )

    val result = service.searchForOffenderOnStaffCaseload(request)

    verify(probationSearchApiClient).searchLicenceCaseloadByTeam(
      request.query,
      communityApiClient.getTeamsCodesForUser(request.staffIdentifier),
    )

    verify(prisonerSearchApiClient).searchPrisonersByNomisIds(
      listOf(aPrisonerSearchResult.prisonerNumber),
    )

    verify(eligibilityService).isEligibleForCvl(
      any(),
    )

    verify(prisonApiClient).getHdcStatuses(
      emptyList(),
    )

    val resultsList = result.results
    val offender = resultsList.first()
    val inPrisonCount = result.inPrisonCount
    val onProbationCount = result.onProbationCount

    assertThat(resultsList).isNotEmpty
    assertThat(resultsList.size).isEqualTo(1)

    assertThat(offender)
      .extracting {
        tuple(
          it.name, it.crn, it.nomisId, it.comName, it.comStaffCode, it.teamName, it.releaseDate,
          it.licenceId, it.licenceType, it.licenceStatus, it.isOnProbation,
        )
      }
      .isEqualTo(
        tuple(
          "Test Surname",
          "A123456",
          "A1234AA",
          "Staff Surname",
          "A01B02C",
          "Test Team",
          LocalDate.parse("2023-09-14"),
          null,
          LicenceType.AP_PSS,
          LicenceStatus.NOT_STARTED,
          false,
        ),
      )

    assertThat(inPrisonCount).isEqualTo(1)
    assertThat(onProbationCount).isEqualTo(0)
  }

  @Test
  fun `search for offenders in prison on a staff member's caseload without a licence without any release date data should be ignored`() {
    whenever(communityApiClient.getTeamsCodesForUser(2000)).thenReturn(
      listOf(
        "A01B02",
      ),
    )
    whenever(probationSearchApiClient.searchLicenceCaseloadByTeam("Test", listOf("A01B02"))).thenReturn(
      listOf(
        CaseloadResult(
          Name("Test", "Surname"),
          Identifiers("A123456", "A1234AA"),
          Manager(
            "A01B02C",
            Name("Staff", "Surname"),
            Detail("A01B02", "Test Team"),
          ),
          "2023/05/24",
        ),
      ),
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any()))
      .thenReturn(
        emptyList(),
      )

    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(aPrisonerSearchResult.prisonerNumber))).thenReturn(
      listOf(
        aPrisonerSearchResult.copy(
          releaseDate = null,
          confirmedReleaseDate = null,
        ),
      ),
    )

    whenever(eligibilityService.isEligibleForCvl(aPrisonerSearchResult)).thenReturn(
      false,
    )

    val request = ProbationUserSearchRequest(
      "Test",
      2000,
    )

    val result = service.searchForOffenderOnStaffCaseload(request)

    verify(probationSearchApiClient).searchLicenceCaseloadByTeam(
      request.query,
      communityApiClient.getTeamsCodesForUser(request.staffIdentifier),
    )

    verify(prisonerSearchApiClient).searchPrisonersByNomisIds(
      listOf(aPrisonerSearchResult.prisonerNumber),
    )

    verify(eligibilityService).isEligibleForCvl(
      any(),
    )

    verify(prisonApiClient).getHdcStatuses(
      emptyList(),
    )

    val resultsList = result.results
    val inPrisonCount = result.inPrisonCount
    val onProbationCount = result.onProbationCount

    assertThat(resultsList).isEmpty()
    assertThat(resultsList.size).isEqualTo(0)
    assertThat(inPrisonCount).isEqualTo(0)
    assertThat(onProbationCount).isEqualTo(0)
  }

  @Test
  fun `search for offenders in prison on a staff member's caseload without a licence with no CRD should use release date`() {
    whenever(communityApiClient.getTeamsCodesForUser(2000)).thenReturn(
      listOf(
        "A01B02",
      ),
    )
    whenever(probationSearchApiClient.searchLicenceCaseloadByTeam("Test", listOf("A01B02"))).thenReturn(
      listOf(
        CaseloadResult(
          Name("Test", "Surname"),
          Identifiers("A123456", "A1234AA"),
          Manager(
            "A01B02C",
            Name("Staff", "Surname"),
            Detail("A01B02", "Test Team"),
          ),
          "2023/05/24",
        ),
      ),
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any()))
      .thenReturn(
        emptyList(),
      )

    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(aPrisonerSearchResult.prisonerNumber))).thenReturn(
      listOf(
        aPrisonerSearchResult.copy(
          confirmedReleaseDate = null,
        ),
      ),
    )

    whenever(eligibilityService.isEligibleForCvl(any())).thenReturn(
      true,
    )

    val request = ProbationUserSearchRequest(
      "Test",
      2000,
    )

    val result = service.searchForOffenderOnStaffCaseload(request)

    verify(probationSearchApiClient).searchLicenceCaseloadByTeam(
      request.query,
      communityApiClient.getTeamsCodesForUser(request.staffIdentifier),
    )

    verify(prisonerSearchApiClient).searchPrisonersByNomisIds(
      listOf(aPrisonerSearchResult.prisonerNumber),
    )

    verify(eligibilityService).isEligibleForCvl(
      any(),
    )

    verify(prisonApiClient).getHdcStatuses(
      emptyList(),
    )

    val resultsList = result.results
    val offender = resultsList.first()
    val inPrisonCount = result.inPrisonCount
    val onProbationCount = result.onProbationCount

    assertThat(resultsList).isNotEmpty
    assertThat(resultsList.size).isEqualTo(1)

    assertThat(offender)
      .extracting {
        tuple(
          it.name, it.crn, it.nomisId, it.comName, it.comStaffCode, it.teamName, it.releaseDate,
          it.licenceId, it.licenceType, it.licenceStatus, it.isOnProbation,
        )
      }
      .isEqualTo(
        tuple(
          "Test Surname",
          "A123456",
          "A1234AA",
          "Staff Surname",
          "A01B02C",
          "Test Team",
          LocalDate.parse("2023-09-14"),
          null,
          LicenceType.AP,
          LicenceStatus.NOT_STARTED,
          false,
        ),
      )

    assertThat(inPrisonCount).isEqualTo(1)
    assertThat(onProbationCount).isEqualTo(0)
  }

  @Test
  fun `search for offenders in prison on a staff member's caseload without a licence and ineligible for CVL`() {
    whenever(communityApiClient.getTeamsCodesForUser(2000)).thenReturn(
      listOf(
        "A01B02",
      ),
    )
    whenever(probationSearchApiClient.searchLicenceCaseloadByTeam("Test", listOf("A01B02"))).thenReturn(
      listOf(
        CaseloadResult(
          Name("Test", "Surname"),
          Identifiers("A123456", "A1234AA"),
          Manager(
            "A01B02C",
            Name("Staff", "Surname"),
            Detail("A01B02", "Test Team"),
          ),
          "2023/05/24",
        ),
      ),
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any()))
      .thenReturn(
        emptyList(),
      )

    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(aPrisonerSearchResult.prisonerNumber))).thenReturn(
      listOf(
        aPrisonerSearchResult,
      ),
    )

    whenever(eligibilityService.isEligibleForCvl(aPrisonerSearchResult)).thenReturn(
      false,
    )

    val request = ProbationUserSearchRequest(
      "Test",
      2000,
    )

    val result = service.searchForOffenderOnStaffCaseload(request)

    verify(probationSearchApiClient).searchLicenceCaseloadByTeam(
      request.query,
      communityApiClient.getTeamsCodesForUser(request.staffIdentifier),
    )

    verify(prisonerSearchApiClient).searchPrisonersByNomisIds(
      listOf(aPrisonerSearchResult.prisonerNumber),
    )

    verify(eligibilityService).isEligibleForCvl(
      aPrisonerSearchResult,
    )

    verify(prisonApiClient).getHdcStatuses(
      emptyList(),
    )

    val resultsList = result.results
    val inPrisonCount = result.inPrisonCount
    val onProbationCount = result.onProbationCount

    assertThat(resultsList).isEmpty()
    assertThat(resultsList.size).isEqualTo(0)
    assertThat(inPrisonCount).isEqualTo(0)
    assertThat(onProbationCount).isEqualTo(0)
  }

  @Test
  fun `search for offenders in prison on a staff member's caseload without a licence, eligible for CVL and is a non approved HDC case`() {
    whenever(communityApiClient.getTeamsCodesForUser(2000)).thenReturn(
      listOf(
        "A01B02",
      ),
    )
    whenever(probationSearchApiClient.searchLicenceCaseloadByTeam("Test", listOf("A01B02"))).thenReturn(
      listOf(
        CaseloadResult(
          Name("Test", "Surname"),
          Identifiers("A123456", "A1234AA"),
          Manager(
            "A01B02C",
            Name("Staff", "Surname"),
            Detail("A01B02", "Test Team"),
          ),
          "2023/05/24",
        ),
      ),
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any()))
      .thenReturn(
        emptyList(),
      )

    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(aPrisonerSearchResult.prisonerNumber))).thenReturn(
      listOf(
        aPrisonerSearchResult.copy(
          homeDetentionCurfewEligibilityDate = LocalDate.parse("2023-09-14"),
        ),
      ),
    )

    whenever(eligibilityService.isEligibleForCvl(any())).thenReturn(
      true,
    )

    whenever(prisonApiClient.getHdcStatuses(listOf(aPrisonerSearchResult.bookingId!!.toLong()))).thenReturn(
      listOf(
        aPrisonerHdcStatus,
      ),
    )

    val request = ProbationUserSearchRequest(
      "Test",
      2000,
    )

    val result = service.searchForOffenderOnStaffCaseload(request)

    verify(probationSearchApiClient).searchLicenceCaseloadByTeam(
      request.query,
      communityApiClient.getTeamsCodesForUser(request.staffIdentifier),
    )

    verify(prisonerSearchApiClient).searchPrisonersByNomisIds(
      listOf(aPrisonerSearchResult.prisonerNumber),
    )

    verify(eligibilityService).isEligibleForCvl(
      any(),
    )

    verify(prisonApiClient).getHdcStatuses(
      listOf(aPrisonerSearchResult.bookingId!!.toLong()),
    )

    val resultsList = result.results
    val offender = resultsList.first()
    val inPrisonCount = result.inPrisonCount
    val onProbationCount = result.onProbationCount

    assertThat(resultsList).isNotEmpty
    assertThat(resultsList.size).isEqualTo(1)

    assertThat(offender)
      .extracting {
        tuple(
          it.name, it.crn, it.nomisId, it.comName, it.comStaffCode, it.teamName, it.releaseDate,
          it.licenceId, it.licenceType, it.licenceStatus, it.isOnProbation,
        )
      }
      .isEqualTo(
        tuple(
          "Test Surname",
          "A123456",
          "A1234AA",
          "Staff Surname",
          "A01B02C",
          "Test Team",
          LocalDate.parse("2023-09-14"),
          null,
          LicenceType.AP,
          LicenceStatus.NOT_STARTED,
          false,
        ),
      )

    assertThat(inPrisonCount).isEqualTo(1)
    assertThat(onProbationCount).isEqualTo(0)
  }

  @Test
  fun `search for offenders in prison on a staff member's caseload without a licence, eligible for CVL and is an approved HDC case`() {
    whenever(communityApiClient.getTeamsCodesForUser(2000)).thenReturn(
      listOf(
        "A01B02",
      ),
    )
    whenever(probationSearchApiClient.searchLicenceCaseloadByTeam("Test", listOf("A01B02"))).thenReturn(
      listOf(
        CaseloadResult(
          Name("Test", "Surname"),
          Identifiers("A123456", "A1234AA"),
          Manager(
            "A01B02C",
            Name("Staff", "Surname"),
            Detail("A01B02", "Test Team"),
          ),
          "2023/05/24",
        ),
      ),
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any()))
      .thenReturn(
        emptyList(),
      )

    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(aPrisonerSearchResult.prisonerNumber))).thenReturn(
      listOf(
        aPrisonerSearchResult.copy(
          homeDetentionCurfewEligibilityDate = LocalDate.parse("2023-09-14"),
        ),
      ),
    )

    whenever(eligibilityService.isEligibleForCvl(any())).thenReturn(
      true,
    )

    whenever(prisonApiClient.getHdcStatuses(listOf(aPrisonerSearchResult.bookingId!!.toLong()))).thenReturn(
      listOf(
        aPrisonerHdcStatus.copy(
          approvalStatus = "APPROVED",
        ),
      ),
    )

    val request = ProbationUserSearchRequest(
      "Test",
      2000,
    )

    val result = service.searchForOffenderOnStaffCaseload(request)

    verify(probationSearchApiClient).searchLicenceCaseloadByTeam(
      request.query,
      communityApiClient.getTeamsCodesForUser(request.staffIdentifier),
    )

    verify(prisonerSearchApiClient).searchPrisonersByNomisIds(
      listOf(aPrisonerSearchResult.prisonerNumber),
    )

    verify(eligibilityService).isEligibleForCvl(
      any(),
    )

    verify(prisonApiClient).getHdcStatuses(
      any(),
    )

    val resultsList = result.results
    val inPrisonCount = result.inPrisonCount
    val onProbationCount = result.onProbationCount

    assertThat(resultsList).isEmpty()
    assertThat(resultsList.size).isEqualTo(0)
    assertThat(inPrisonCount).isEqualTo(0)
    assertThat(onProbationCount).isEqualTo(0)
  }

  @Test
  fun `search for offenders in prison on a staff member's caseload without a licence, eligible for CVL, HDC case but without HDCED`() {
    whenever(communityApiClient.getTeamsCodesForUser(2000)).thenReturn(
      listOf(
        "A01B02",
      ),
    )
    whenever(probationSearchApiClient.searchLicenceCaseloadByTeam("Test", listOf("A01B02"))).thenReturn(
      listOf(
        CaseloadResult(
          Name("Test", "Surname"),
          Identifiers("A123456", "A1234AA"),
          Manager(
            "A01B02C",
            Name("Staff", "Surname"),
            Detail("A01B02", "Test Team"),
          ),
          "2023/05/24",
        ),
      ),
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any()))
      .thenReturn(
        emptyList(),
      )

    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(aPrisonerSearchResult.prisonerNumber))).thenReturn(
      listOf(
        aPrisonerSearchResult,
      ),
    )

    whenever(eligibilityService.isEligibleForCvl(any())).thenReturn(
      true,
    )

    val request = ProbationUserSearchRequest(
      "Test",
      2000,
    )

    val result = service.searchForOffenderOnStaffCaseload(request)

    verify(probationSearchApiClient).searchLicenceCaseloadByTeam(
      request.query,
      communityApiClient.getTeamsCodesForUser(request.staffIdentifier),
    )

    verify(prisonerSearchApiClient).searchPrisonersByNomisIds(
      listOf(aPrisonerSearchResult.prisonerNumber),
    )

    verify(eligibilityService).isEligibleForCvl(
      aPrisonerSearchResult,
    )

    verify(prisonApiClient).getHdcStatuses(
      emptyList(),
    )

    val resultsList = result.results
    val offender = resultsList.first()
    val inPrisonCount = result.inPrisonCount
    val onProbationCount = result.onProbationCount

    assertThat(resultsList).isNotEmpty
    assertThat(resultsList.size).isEqualTo(1)

    assertThat(offender)
      .extracting {
        tuple(
          it.name, it.crn, it.nomisId, it.comName, it.comStaffCode, it.teamName, it.releaseDate,
          it.licenceId, it.licenceType, it.licenceStatus, it.isOnProbation,
        )
      }
      .isEqualTo(
        tuple(
          "Test Surname",
          "A123456",
          "A1234AA",
          "Staff Surname",
          "A01B02C",
          "Test Team",
          LocalDate.parse("2023-09-14"),
          null,
          LicenceType.AP,
          LicenceStatus.NOT_STARTED,
          false,
        ),
      )

    assertThat(inPrisonCount).isEqualTo(1)
    assertThat(onProbationCount).isEqualTo(0)
  }

  @Test
  fun `hard stop dates are populated for non-started licences`() {
    whenever(communityApiClient.getTeamsCodesForUser(2000)).thenReturn(
      listOf(
        "A01B02",
      ),
    )
    whenever(probationSearchApiClient.searchLicenceCaseloadByTeam("Test", listOf("A01B02"))).thenReturn(
      listOf(
        CaseloadResult(
          Name("Test", "Surname"),
          Identifiers("A123456", "A1234AA"),
          Manager(
            "A01B02C",
            Name("Staff", "Surname"),
            Detail("A01B02", "Test Team"),
          ),
          "2023/05/24",
        ),
      ),
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(emptyList())

    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(aPrisonerSearchResult.prisonerNumber))).thenReturn(
      listOf(
        aPrisonerSearchResult,
      ),
    )

    whenever(eligibilityService.isEligibleForCvl(any())).thenReturn(true)

    whenever(releaseDateService.isInHardStopPeriod(any(), anyOrNull())).thenReturn(true)
    whenever(releaseDateService.getHardStopDate(any())).thenReturn(LocalDate.of(2023, 2, 12))
    whenever(releaseDateService.getHardStopWarningDate(any())).thenReturn(LocalDate.of(2023, 3, 14))
    whenever(releaseDateService.isDueForEarlyRelease(any())).thenReturn(true)
    whenever(releaseDateService.isDueToBeReleasedInTheNextTwoWorkingDays(any())).thenReturn(true)

    val result = service.searchForOffenderOnStaffCaseload(ProbationUserSearchRequest("Test", 2000))

    with(result.results.first()) {
      assertThat(licenceStatus).isEqualTo(LicenceStatus.TIMED_OUT)
      assertThat(isInHardStopPeriod).isEqualTo(true)
      assertThat(hardStopDate).isEqualTo(LocalDate.of(2023, 2, 12))
      assertThat(hardStopWarningDate).isEqualTo(LocalDate.of(2023, 3, 14))
      assertThat(isDueForEarlyRelease).isEqualTo(true)
      assertThat(isDueToBeReleasedInTheNextTwoWorkingDays).isEqualTo(true)
    }
  }

  @Test
  fun `hard stop dates are populated for started licences`() {
    whenever(communityApiClient.getTeamsCodesForUser(2000)).thenReturn(
      listOf(
        "A01B02",
      ),
    )
    whenever(probationSearchApiClient.searchLicenceCaseloadByTeam("Test", listOf("A01B02"))).thenReturn(
      listOf(
        CaseloadResult(
          Name("Test", "Surname"),
          Identifiers("A123456", "A1234AA"),
          Manager(
            "A01B02C",
            Name("Staff", "Surname"),
            Detail("A01B02", "Test Team"),
          ),
          "2023/05/24",
        ),
      ),
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any()))
      .thenReturn(
        (
          listOf(
            aLicenceEntity.copy(
              statusCode = LicenceStatus.ACTIVE,
            ),
            aLicenceEntity.copy(
              statusCode = LicenceStatus.APPROVED,
            ),
          )
          ),
      )
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(
      listOf(
        aPrisonerSearchResult,
      ),
    )
    whenever(eligibilityService.isEligibleForCvl(any())).thenReturn(
      true,
    )

    whenever(releaseDateService.isInHardStopPeriod(any(), anyOrNull())).thenReturn(true)
    whenever(releaseDateService.getHardStopDate(any())).thenReturn(LocalDate.of(2023, 2, 12))
    whenever(releaseDateService.getHardStopWarningDate(any())).thenReturn(LocalDate.of(2023, 3, 14))
    whenever(releaseDateService.isDueForEarlyRelease(any())).thenReturn(true)
    whenever(releaseDateService.isDueToBeReleasedInTheNextTwoWorkingDays(any())).thenReturn(true)

    val result = service.searchForOffenderOnStaffCaseload(ProbationUserSearchRequest("Test", 2000))

    with(result.results.first()) {
      assertThat(licenceStatus).isEqualTo(LicenceStatus.APPROVED)
      assertThat(isInHardStopPeriod).isEqualTo(true)
      assertThat(hardStopDate).isEqualTo(LocalDate.of(2023, 2, 12))
      assertThat(hardStopWarningDate).isEqualTo(LocalDate.of(2023, 3, 14))
      assertThat(isDueForEarlyRelease).isEqualTo(true)
      assertThat(isDueToBeReleasedInTheNextTwoWorkingDays).isEqualTo(true)
    }
  }

  @Test
  fun `Kind and version of are populated for started CRD licences`() {
    whenever(communityApiClient.getTeamsCodesForUser(2000)).thenReturn(
      listOf(
        "A01B02",
      ),
    )
    whenever(probationSearchApiClient.searchLicenceCaseloadByTeam("Test", listOf("A01B02"))).thenReturn(
      listOf(
        CaseloadResult(
          Name("Test", "Surname"),
          Identifiers("A123456", "A1234AA"),
          Manager(
            "A01B02C",
            Name("Staff", "Surname"),
            Detail("A01B02", "Test Team"),
          ),
          "2023/05/24",
        ),
      ),
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any()))
      .thenReturn(listOf(createCrdLicence().copy(versionOfId = 2L)))

    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any()))
      .thenReturn(listOf(aPrisonerSearchResult))
    whenever(eligibilityService.isEligibleForCvl(any())).thenReturn(true)

    val result = service.searchForOffenderOnStaffCaseload(ProbationUserSearchRequest("Test", 2000))

    with(result.results.first()) {
      assertThat(kind).isEqualTo(LicenceKind.CRD)
      assertThat(versionOf).isEqualTo(2L)
    }
  }

  @Test
  fun `Release date label and is review needed are populated for not started CRD licences`() {
    whenever(communityApiClient.getTeamsCodesForUser(2000)).thenReturn(
      listOf(
        "A01B02",
      ),
    )
    whenever(probationSearchApiClient.searchLicenceCaseloadByTeam("Test", listOf("A01B02"))).thenReturn(
      listOf(
        CaseloadResult(
          Name("Test", "Surname"),
          Identifiers("A123456", "A1234AA"),
          Manager(
            "A01B02C",
            Name("Staff", "Surname"),
            Detail("A01B02", "Test Team"),
          ),
          "2023/05/24",
        ),
      ),
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(emptyList())

    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any()))
      .thenReturn(listOf(aPrisonerSearchResult))
    whenever(eligibilityService.isEligibleForCvl(any())).thenReturn(true)

    val result = service.searchForOffenderOnStaffCaseload(ProbationUserSearchRequest("Test", 2000))

    with(result.results.first()) {
      assertThat(releaseDateLabel).isEqualTo("Confirmed release date")
      assertThat(isReviewNeeded).isFalse()
    }
  }

  @Test
  fun `Release date label reads 'CRD' when no confirmed release date is provided by NOMIS for not started CRD licences`() {
    whenever(communityApiClient.getTeamsCodesForUser(2000)).thenReturn(
      listOf(
        "A01B02",
      ),
    )
    whenever(probationSearchApiClient.searchLicenceCaseloadByTeam("Test", listOf("A01B02"))).thenReturn(
      listOf(
        CaseloadResult(
          Name("Test", "Surname"),
          Identifiers("A123456", "A1234AA"),
          Manager(
            "A01B02C",
            Name("Staff", "Surname"),
            Detail("A01B02", "Test Team"),
          ),
          "2023/05/24",
        ),
      ),
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(emptyList())

    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any()))
      .thenReturn(
        listOf(
          aPrisonerSearchResult.copy(
            confirmedReleaseDate = null,
          ),
        ),
      )
    whenever(eligibilityService.isEligibleForCvl(any())).thenReturn(true)

    val result = service.searchForOffenderOnStaffCaseload(ProbationUserSearchRequest("Test", 2000))

    with(result.results.first()) {
      assertThat(releaseDateLabel).isEqualTo("CRD")
      assertThat(isReviewNeeded).isFalse
    }
  }

  @Test
  fun `Release date label and is review needed are populated for started hard stop licence`() {
    whenever(communityApiClient.getTeamsCodesForUser(2000)).thenReturn(
      listOf(
        "A01B02",
      ),
    )
    whenever(probationSearchApiClient.searchLicenceCaseloadByTeam("Test", listOf("A01B02"))).thenReturn(
      listOf(
        CaseloadResult(
          Name("Test", "Surname"),
          Identifiers("A123456", "A1234AA"),
          Manager(
            "A01B02C",
            Name("Staff", "Surname"),
            Detail("A01B02", "Test Team"),
          ),
          "2023/05/24",
        ),
      ),
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any()))
      .thenReturn(
        listOf(
          createHardStopLicence().copy(
            statusCode = LicenceStatus.ACTIVE,
            reviewDate = null,
          ),
        ),
      )

    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any()))
      .thenReturn(listOf(aPrisonerSearchResult))
    whenever(eligibilityService.isEligibleForCvl(any())).thenReturn(true)

    val result = service.searchForOffenderOnStaffCaseload(ProbationUserSearchRequest("Test", 2000))

    with(result.results.first()) {
      assertThat(releaseDateLabel).isEqualTo("Confirmed release date")
      assertThat(isReviewNeeded).isTrue()
    }
  }

  @Test
  fun `get ineligibility reasons for absent offender`() {
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(listOf("A1234AA"))).thenReturn(emptyList())

    val exception = assertThrows<IllegalStateException> {
      service.getIneligibilityReasons("A1234AA")
    }

    assertThat(exception.message).isEqualTo("Found 0 prisoners for: A1234AA")
  }

  @Test
  fun `get ineligibility reasons for present offender`() {
    val hdcPrisoner = aPrisonerSearchResult.copy(homeDetentionCurfewEligibilityDate = LocalDate.now())
    val approvedHdc = aPrisonerHdcStatus.copy(approvalStatus = "APPROVED")

    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(listOf("A1234AA"))).thenReturn(listOf(hdcPrisoner))
    whenever(eligibilityService.getIneligibilityReasons(hdcPrisoner)).thenReturn(listOf("A reason"))
    whenever(prisonApiClient.getHdcStatuses(listOf(aPrisonerSearchResult.bookingId!!.toLong()))).thenReturn(
      listOf(approvedHdc),
    )

    val reasons = service.getIneligibilityReasons("A1234AA")
    assertThat(reasons).containsExactly("A reason", "Approved for HDC")
  }

  private companion object {
    val aLicenceEntity = createCrdLicence()

    val aPrisonerSearchResult = PrisonerSearchPrisoner(
      prisonerNumber = "A1234AA",
      bookingId = "123456",
      status = "ACTIVE IN",
      mostSeriousOffence = "Robbery",
      licenceExpiryDate = LocalDate.parse("2024-09-14"),
      topupSupervisionExpiryDate = LocalDate.parse("2024-09-14"),
      homeDetentionCurfewEligibilityDate = null,
      releaseDate = LocalDate.parse("2023-09-14"),
      confirmedReleaseDate = LocalDate.parse("2023-09-14"),
      conditionalReleaseDate = LocalDate.parse("2023-09-14"),
      paroleEligibilityDate = null,
      actualParoleDate = null,
      postRecallReleaseDate = null,
      legalStatus = "SENTENCED",
      indeterminateSentence = false,
      recall = false,
      prisonId = "ABC",
      locationDescription = "HMP Moorland",
      bookNumber = "12345A",
      firstName = "Jane",
      middleNames = null,
      lastName = "Doe",
      dateOfBirth = LocalDate.parse("1985-01-01"),
      conditionalReleaseDateOverrideDate = null,
      sentenceStartDate = LocalDate.parse("2023-09-14"),
      sentenceExpiryDate = LocalDate.parse("2024-09-14"),
      topupSupervisionStartDate = null,
      croNumber = null,
    )

    val aPrisonerHdcStatus = PrisonerHdcStatus(
      approvalStatusDate = null,
      approvalStatus = "REJECTED",
      refusedReason = null,
      checksPassedDate = null,
      bookingId = 123456,
      passed = true,
    )
  }
}
