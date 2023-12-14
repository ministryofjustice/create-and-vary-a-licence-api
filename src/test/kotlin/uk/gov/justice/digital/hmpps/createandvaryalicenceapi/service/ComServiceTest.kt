package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple.tuple
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.UpdateComRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.ProbationUserSearchRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.CommunityOffenderManagerRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerHdcStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.CaseloadResult
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.CommunityApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.Identifiers
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.Manager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.Name
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ProbationSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.Team
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.request.ProbationSearchSortByRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.ProbationSearchSortBy
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.SearchDirection
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.SearchField
import java.time.LocalDate

class ComServiceTest {
  private val communityOffenderManagerRepository = mock<CommunityOffenderManagerRepository>()
  private val licenceRepository = mock<LicenceRepository>()
  private val communityApiClient = mock<CommunityApiClient>()
  private val probationSearchApiClient = mock<ProbationSearchApiClient>()
  private val prisonerSearchApiClient = mock<PrisonerSearchApiClient>()
  private val prisonApiClient = mock<PrisonApiClient>()
  private val eligibilityService = mock<EligibilityService>()

  private val service =
    ComService(
      communityOffenderManagerRepository,
      licenceRepository,
      communityApiClient,
      probationSearchApiClient,
      prisonerSearchApiClient,
      prisonApiClient,
      eligibilityService,
    )

  @BeforeEach
  fun reset() {
    reset(
      communityOffenderManagerRepository,
      licenceRepository,
      communityApiClient,
      probationSearchApiClient,
      prisonerSearchApiClient,
      prisonApiClient,
      eligibilityService,
    )
  }

  @Test
  fun `updates existing COM with new details`() {
    val expectedCom = CommunityOffenderManager(
      staffIdentifier = 3000,
      username = "JBLOGGS",
      email = "jbloggs123@probation.gov.uk",
      firstName = "X",
      lastName = "Y",
    )

    val comCaptor = ArgumentCaptor.forClass(CommunityOffenderManager::class.java)

    whenever(communityOffenderManagerRepository.findByStaffIdentifierOrUsernameIgnoreCase(any(), any()))
      .thenReturn(
        listOf(
          CommunityOffenderManager(
            staffIdentifier = 2000,
            username = "joebloggs",
            email = "jbloggs@probation.gov.uk",
            firstName = "A",
            lastName = "B",
          ),
        ),
      )

    whenever(communityOffenderManagerRepository.saveAndFlush(any())).thenReturn(expectedCom)

    val comDetails = UpdateComRequest(
      staffIdentifier = 3000,
      staffUsername = "jbloggs",
      staffEmail = "jbloggs123@probation.gov.uk",
      firstName = "X",
      lastName = "Y",
    )

    service.updateComDetails(comDetails)

    verify(communityOffenderManagerRepository, times(1)).findByStaffIdentifierOrUsernameIgnoreCase(3000, "jbloggs")
    verify(communityOffenderManagerRepository, times(1)).saveAndFlush(comCaptor.capture())

    assertThat(comCaptor.value).usingRecursiveComparison().ignoringFields("lastUpdatedTimestamp").isEqualTo(expectedCom)
  }

  @Test
  fun `does not update COM with same details`() {
    val expectedCom = CommunityOffenderManager(
      staffIdentifier = 2000,
      username = "joebloggs",
      email = "jbloggs123@probation.gov.uk",
      firstName = "X",
      lastName = "Y",
    )

    whenever(communityOffenderManagerRepository.findByStaffIdentifierOrUsernameIgnoreCase(any(), any()))
      .thenReturn(
        listOf(
          CommunityOffenderManager(
            staffIdentifier = 2000,
            username = "joebloggs",
            email = "jbloggs123@probation.gov.uk",
            firstName = "X",
            lastName = "Y",
          ),
        ),
      )

    whenever(communityOffenderManagerRepository.saveAndFlush(any())).thenReturn(expectedCom)

    val comDetails = UpdateComRequest(
      staffIdentifier = 2000,
      staffUsername = "JOEBLOGGS",
      staffEmail = "jbloggs123@probation.gov.uk",
      firstName = "X",
      lastName = "Y",
    )

    service.updateComDetails(comDetails)

    verify(communityOffenderManagerRepository, times(1)).findByStaffIdentifierOrUsernameIgnoreCase(2000, "JOEBLOGGS")
    verify(communityOffenderManagerRepository, times(0)).saveAndFlush(any())
  }

  @Test
  fun `adds a new existing COM if it doesnt exist`() {
    val expectedCom = CommunityOffenderManager(
      staffIdentifier = 3000,
      username = "JBLOGGS",
      email = "jbloggs123@probation.gov.uk",
      firstName = "X",
      lastName = "Y",
    )

    whenever(
      communityOffenderManagerRepository.findByStaffIdentifierOrUsernameIgnoreCase(
        any(),
        any(),
      ),
    ).thenReturn(null)
    whenever(communityOffenderManagerRepository.saveAndFlush(any())).thenReturn(expectedCom)

    val comDetails = UpdateComRequest(
      staffIdentifier = 3000,
      staffUsername = "jbloggs",
      staffEmail = "jbloggs123@probation.gov.uk",
      firstName = "X",
      lastName = "Y",
    )

    service.updateComDetails(comDetails)

    verify(communityOffenderManagerRepository, times(1)).findByStaffIdentifierOrUsernameIgnoreCase(3000, "jbloggs")
    verify(communityOffenderManagerRepository, times(1)).saveAndFlush(expectedCom)
  }

  @Test
  fun `updates existing COM with new username and forces it to be uppercase`() {
    val expectedCom = CommunityOffenderManager(
      staffIdentifier = 2000,
      username = "JBLOGGSNEW1",
      email = "jbloggs123@probation.gov.uk",
      firstName = "X",
      lastName = "Y",
    )

    val comCaptor = ArgumentCaptor.forClass(CommunityOffenderManager::class.java)

    whenever(communityOffenderManagerRepository.findByStaffIdentifierOrUsernameIgnoreCase(any(), any()))
      .thenReturn(
        listOf(
          CommunityOffenderManager(
            staffIdentifier = 2000,
            username = "JOEBLOGGS",
            email = "jbloggs@probation.gov.uk",
            firstName = "A",
            lastName = "B",
          ),
        ),
      )

    whenever(communityOffenderManagerRepository.saveAndFlush(any())).thenReturn(expectedCom)

    val comDetails = UpdateComRequest(
      staffIdentifier = 2000,
      staffUsername = "jbloggsnew1",
      staffEmail = "jbloggs123@probation.gov.uk",
      firstName = "X",
      lastName = "Y",
    )

    service.updateComDetails(comDetails)

    verify(communityOffenderManagerRepository, times(1)).findByStaffIdentifierOrUsernameIgnoreCase(2000, "jbloggsnew1")
    verify(communityOffenderManagerRepository, times(1)).saveAndFlush(comCaptor.capture())

    assertThat(comCaptor.value).usingRecursiveComparison().ignoringFields("lastUpdatedTimestamp").isEqualTo(expectedCom)
  }

  @Test
  fun `updates existing COM with new staffIdentifier`() {
    val expectedCom = CommunityOffenderManager(
      staffIdentifier = 2001,
      username = "JOEBLOGGS",
      email = "jbloggs123@probation.gov.uk",
      firstName = "X",
      lastName = "Y",
    )

    val comCaptor = ArgumentCaptor.forClass(CommunityOffenderManager::class.java)

    whenever(communityOffenderManagerRepository.findByStaffIdentifierOrUsernameIgnoreCase(any(), any()))
      .thenReturn(
        listOf(
          CommunityOffenderManager(
            staffIdentifier = 2000,
            username = "JOEBLOGGS",
            email = "jbloggs@probation.gov.uk",
            firstName = "A",
            lastName = "B",
          ),
        ),
      )

    whenever(communityOffenderManagerRepository.saveAndFlush(any())).thenReturn(expectedCom)

    val comDetails = UpdateComRequest(
      staffIdentifier = 2001,
      staffUsername = "JOEBLOGGS",
      staffEmail = "jbloggs123@probation.gov.uk",
      firstName = "X",
      lastName = "Y",
    )

    service.updateComDetails(comDetails)

    verify(communityOffenderManagerRepository, times(1)).findByStaffIdentifierOrUsernameIgnoreCase(2001, "JOEBLOGGS")
    verify(communityOffenderManagerRepository, times(1)).saveAndFlush(comCaptor.capture())

    assertThat(comCaptor.value).usingRecursiveComparison().ignoringFields("lastUpdatedTimestamp").isEqualTo(expectedCom)
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
            Team("A01B02", "Test Team"),
          ),
          "2023/05/24",
        ),
      ),
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn((listOf(aLicenceEntity)))

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
    verifyNoInteractions(prisonApiClient)

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
            Team("A01B02", "Test Team"),
          ),
          "2023/05/24",
        ),
      ),
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn((listOf(aLicenceEntity)))

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

    verifyNoInteractions(eligibilityService)
    verifyNoInteractions(prisonApiClient)

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
            Team("A01B02", "Test Team"),
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
    verifyNoInteractions(prisonApiClient)

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
            Team("A01B02", "Test Team"),
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
    verifyNoInteractions(prisonApiClient)

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
            Team("A01B02", "Test Team"),
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
    verifyNoInteractions(prisonApiClient)

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
            Team("A01B02", "Test Team"),
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
            Team("A01B02", "Test Team"),
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

    verify(prisonerSearchApiClient).searchPrisonersByNomisIds(
      emptyList(),
    )

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
            Team("A01B02", "Test Team"),
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
            Team("A01B02", "Test Team"),
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
          topUpSupervisionExpiryDate = null,
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
            Team("A01B02", "Test Team"),
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
          topUpSupervisionExpiryDate = LocalDate.parse("2024-09-14"),
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
            Team("A01B02", "Test Team"),
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
          topUpSupervisionExpiryDate = LocalDate.parse("2024-10-14"),
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
            Team("A01B02", "Test Team"),
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
            Team("A01B02", "Test Team"),
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
            Team("A01B02", "Test Team"),
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
            Team("A01B02", "Test Team"),
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

    whenever(prisonApiClient.getHdcStatuses(listOf(aPrisonerSearchResult.bookingId.toLong()))).thenReturn(
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
      listOf(aPrisonerSearchResult.bookingId.toLong()),
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
            Team("A01B02", "Test Team"),
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

    whenever(prisonApiClient.getHdcStatuses(listOf(aPrisonerSearchResult.bookingId.toLong()))).thenReturn(
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
            Team("A01B02", "Test Team"),
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
    whenever(prisonApiClient.getHdcStatuses(listOf(aPrisonerSearchResult.bookingId.toLong()))).thenReturn(
      listOf(approvedHdc),
    )

    val reasons = service.getIneligibilityReasons("A1234AA")
    assertThat(reasons).containsExactly("A reason", "Approved for HDC")
  }

  private companion object {
    val aLicenceEntity = TestData.createCrdLicence()

    val aPrisonerSearchResult = PrisonerSearchPrisoner(
      prisonerNumber = "A1234AA",
      bookingId = "123456",
      status = "ACTIVE IN",
      mostSeriousOffence = "Robbery",
      licenceExpiryDate = LocalDate.parse("2024-09-14"),
      topUpSupervisionExpiryDate = LocalDate.parse("2024-09-14"),
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
