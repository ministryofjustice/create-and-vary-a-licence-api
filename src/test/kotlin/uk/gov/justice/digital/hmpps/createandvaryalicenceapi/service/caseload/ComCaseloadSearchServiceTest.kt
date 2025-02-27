package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple.tuple
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.ProbationUserSearchRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.EligibilityService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.HdcService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.HdcService.HdcStatuses
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.caseloadResult
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createCrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createHardStopLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.Identifiers
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ProbationSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.request.ProbationSearchSortByRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.ProbationSearchSortBy
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.SearchDirection
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.SearchField
import java.time.LocalDate

class ComCaseloadSearchServiceTest {
  private val licenceRepository = mock<LicenceRepository>()
  private val deliusApiClient = mock<DeliusApiClient>()
  private val probationSearchApiClient = mock<ProbationSearchApiClient>()
  private val prisonerSearchApiClient = mock<PrisonerSearchApiClient>()
  private val hdcService = mock<HdcService>()
  private val eligibilityService = mock<EligibilityService>()
  private val releaseDateService = mock<ReleaseDateService>()

  private val service = ComCaseloadSearchService(
    licenceRepository,
    deliusApiClient,
    probationSearchApiClient,
    prisonerSearchApiClient,
    hdcService,
    eligibilityService,
    releaseDateService,
  )

  @BeforeEach
  fun reset() {
    reset(
      licenceRepository,
      deliusApiClient,
      probationSearchApiClient,
      prisonerSearchApiClient,
      hdcService,
      eligibilityService,
      releaseDateService,
    )
  }

  @Test
  fun `search for offenders in prison on a staff member's caseload`() {
    whenever(deliusApiClient.getTeamsCodesForUser(2000)).thenReturn(
      listOf(
        "A01B02",
      ),
    )
    whenever(probationSearchApiClient.searchLicenceCaseloadByTeam("Test", listOf("A01B02"))).thenReturn(
      listOf(caseloadResult()),
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn((listOf(aLicenceEntity)))

    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(aPrisonerSearchResult.prisonerNumber))).thenReturn(
      listOf(
        aPrisonerSearchResult,
      ),
    )

    whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(emptySet()))
    whenever(eligibilityService.isEligibleForCvl(aPrisonerSearchResult)).thenReturn(true)

    val result = service.searchForOffenderOnStaffCaseload(request)

    verify(probationSearchApiClient).searchLicenceCaseloadByTeam(
      request.query,
      deliusApiClient.getTeamsCodesForUser(request.staffIdentifier),
    )

    verify(eligibilityService).isEligibleForCvl(aPrisonerSearchResult)
    verify(hdcService).getHdcStatus(listOf(aPrisonerSearchResult))

    val resultsList = result.results
    val offender = resultsList.first()
    val inPrisonCount = result.inPrisonCount
    val onProbationCount = result.onProbationCount

    assertThat(resultsList).isNotEmpty
    assertThat(resultsList.size).isEqualTo(1)

    assertThat(offender).extracting {
      tuple(
        it.name, it.crn, it.nomisId, it.comName, it.comStaffCode, it.teamName, it.releaseDate,
        it.licenceId, it.licenceType, it.licenceStatus, it.isOnProbation,
      )
    }.isEqualTo(
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
    whenever(deliusApiClient.getTeamsCodesForUser(2000)).thenReturn(listOf("A01B02"))
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
      listOf(caseloadResult()),
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn((listOf(aLicenceEntity)))

    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(aPrisonerSearchResult.prisonerNumber))).thenReturn(
      listOf(
        aPrisonerSearchResult,
      ),
    )
    whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(emptySet()))
    whenever(eligibilityService.isEligibleForCvl(aPrisonerSearchResult)).thenReturn(true)

    val request = request.copy(
      sortBy = listOf(
        ProbationSearchSortBy(SearchField.SURNAME, SearchDirection.ASC),
        ProbationSearchSortBy(SearchField.COM_FORENAME, SearchDirection.DESC),
      ),
    )

    val result = service.searchForOffenderOnStaffCaseload(request)

    verify(probationSearchApiClient).searchLicenceCaseloadByTeam(
      request.query,
      deliusApiClient.getTeamsCodesForUser(request.staffIdentifier),
      listOf(
        ProbationSearchSortByRequest(SearchField.SURNAME.probationSearchApiSortType, "asc"),
        ProbationSearchSortByRequest(SearchField.COM_FORENAME.probationSearchApiSortType, "desc"),
      ),
    )

    verify(eligibilityService).isEligibleForCvl(aPrisonerSearchResult)
    verify(hdcService).getHdcStatus(listOf(aPrisonerSearchResult))

    val resultsList = result.results
    val offender = resultsList.first()

    assertThat(resultsList.size).isEqualTo(1)
    assertThat(offender).extracting { tuple(it.name, it.comName, it.teamName) }.isEqualTo(
      tuple("Test Surname", "Staff Surname", "Test Team"),
    )
  }

  @Test
  fun `search for offenders in prison on a staff member's caseload with latest licence selected`() {
    whenever(deliusApiClient.getTeamsCodesForUser(2000)).thenReturn(
      listOf(
        "A01B02",
      ),
    )
    whenever(probationSearchApiClient.searchLicenceCaseloadByTeam("Test", listOf("A01B02"))).thenReturn(
      listOf(caseloadResult()),
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(
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
    whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(emptySet()))
    whenever(eligibilityService.isEligibleForCvl(any())).thenReturn(true)

    val result = service.searchForOffenderOnStaffCaseload(request)

    verify(probationSearchApiClient).searchLicenceCaseloadByTeam(
      request.query,
      deliusApiClient.getTeamsCodesForUser(request.staffIdentifier),
    )
    val resultsList = result.results
    val offender = resultsList.first()
    val inPrisonCount = result.inPrisonCount
    val onProbationCount = result.onProbationCount

    assertThat(resultsList).isNotEmpty
    assertThat(resultsList.size).isEqualTo(1)

    assertThat(offender).extracting {
      tuple(
        it.name, it.crn, it.nomisId, it.comName, it.comStaffCode, it.teamName, it.releaseDate,
        it.licenceId, it.licenceType, it.licenceStatus, it.isOnProbation,
      )
    }.isEqualTo(
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
    whenever(deliusApiClient.getTeamsCodesForUser(2000)).thenReturn(
      listOf(
        "A01B02",
      ),
    )
    whenever(probationSearchApiClient.searchLicenceCaseloadByTeam("Test", listOf("A01B02"))).thenReturn(
      listOf(caseloadResult()),
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(
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

    whenever(eligibilityService.isEligibleForCvl(aPrisonerSearchResult)).thenReturn(true)

    val result = service.searchForOffenderOnStaffCaseload(request)

    verify(probationSearchApiClient).searchLicenceCaseloadByTeam(
      request.query,
      deliusApiClient.getTeamsCodesForUser(request.staffIdentifier),
    )
    verifyNoInteractions(hdcService)
    verifyNoInteractions(prisonerSearchApiClient)
    verifyNoInteractions(eligibilityService)

    val resultsList = result.results
    val offender = resultsList.first()
    val inPrisonCount = result.inPrisonCount
    val onProbationCount = result.onProbationCount

    assertThat(resultsList).isNotEmpty
    assertThat(resultsList.size).isEqualTo(1)

    assertThat(offender).extracting {
      tuple(
        it.name, it.crn, it.nomisId, it.comName, it.comStaffCode, it.teamName, it.releaseDate,
        it.licenceId, it.licenceType, it.licenceStatus, it.isOnProbation,
      )
    }.isEqualTo(
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
  fun `search for offenders in prison on a staff member's caseload without a licence (NOT_STARTED)`() {
    whenever(deliusApiClient.getTeamsCodesForUser(2000)).thenReturn(
      listOf(
        "A01B02",
      ),
    )
    whenever(probationSearchApiClient.searchLicenceCaseloadByTeam("Test", listOf("A01B02"))).thenReturn(
      listOf(caseloadResult()),
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(emptyList())

    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(aPrisonerSearchResult.prisonerNumber))).thenReturn(
      listOf(aPrisonerSearchResult),
    )

    whenever(releaseDateService.getLicenceStartDates(any())).thenReturn(mapOf("A1234AA" to LocalDate.of(2023, 9, 14)))

    whenever(eligibilityService.isEligibleForCvl(aPrisonerSearchResult)).thenReturn(true)
    whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(emptySet()))

    val result = service.searchForOffenderOnStaffCaseload(request)

    verify(probationSearchApiClient).searchLicenceCaseloadByTeam(
      request.query,
      deliusApiClient.getTeamsCodesForUser(request.staffIdentifier),
    )

    verify(prisonerSearchApiClient).searchPrisonersByNomisIds(
      listOf(aPrisonerSearchResult.prisonerNumber),
    )

    verify(eligibilityService).isEligibleForCvl(aPrisonerSearchResult)
    verify(hdcService).getHdcStatus(listOf(aPrisonerSearchResult))

    val resultsList = result.results
    val offender = resultsList.first()
    val inPrisonCount = result.inPrisonCount
    val onProbationCount = result.onProbationCount

    assertThat(resultsList).isNotEmpty
    assertThat(resultsList.size).isEqualTo(1)

    assertThat(offender).extracting {
      tuple(
        it.name, it.crn, it.nomisId, it.comName, it.comStaffCode, it.teamName, it.releaseDate,
        it.licenceId, it.licenceType, it.licenceStatus, it.isOnProbation,
      )
    }.isEqualTo(
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
    whenever(deliusApiClient.getTeamsCodesForUser(2000)).thenReturn(
      listOf(
        "A01B02",
      ),
    )
    whenever(probationSearchApiClient.searchLicenceCaseloadByTeam("Test", listOf("A01B02"))).thenReturn(
      listOf(caseloadResult().copy(identifiers = Identifiers(crn = "X123456", noms = null))),
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(
      emptyList(),
    )

    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(aPrisonerSearchResult.prisonerNumber))).thenReturn(
      listOf(
        aPrisonerSearchResult,
      ),
    )

    val result = service.searchForOffenderOnStaffCaseload(request)

    verify(probationSearchApiClient).searchLicenceCaseloadByTeam(
      request.query,
      deliusApiClient.getTeamsCodesForUser(request.staffIdentifier),
    )

    verifyNoInteractions(prisonerSearchApiClient)
    verifyNoInteractions(eligibilityService)
    verifyNoInteractions(hdcService)

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
    whenever(deliusApiClient.getTeamsCodesForUser(2000)).thenReturn(
      listOf(
        "A01B02",
      ),
    )
    whenever(probationSearchApiClient.searchLicenceCaseloadByTeam("Test", listOf("A01B02"))).thenReturn(
      listOf(caseloadResult()),
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(
      emptyList(),
    )

    val prisoner = aPrisonerSearchResult.copy(licenceExpiryDate = null)
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(aPrisonerSearchResult.prisonerNumber))).thenReturn(
      listOf(prisoner),
    )

    whenever(releaseDateService.getLicenceStartDates(any())).thenReturn(mapOf("A1234AA" to LocalDate.of(2023, 9, 14)))

    whenever(eligibilityService.isEligibleForCvl(any())).thenReturn(true)
    whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(emptySet()))

    val result = service.searchForOffenderOnStaffCaseload(request)

    verify(probationSearchApiClient).searchLicenceCaseloadByTeam(
      request.query,
      deliusApiClient.getTeamsCodesForUser(request.staffIdentifier),
    )

    verify(prisonerSearchApiClient).searchPrisonersByNomisIds(
      listOf(aPrisonerSearchResult.prisonerNumber),
    )

    verify(eligibilityService).isEligibleForCvl(any())
    verify(hdcService).getHdcStatus(listOf(prisoner))

    val resultsList = result.results
    val offender = resultsList.first()
    val inPrisonCount = result.inPrisonCount
    val onProbationCount = result.onProbationCount

    assertThat(resultsList).isNotEmpty
    assertThat(resultsList.size).isEqualTo(1)

    assertThat(offender).extracting {
      tuple(
        it.name, it.crn, it.nomisId, it.comName, it.comStaffCode, it.teamName, it.releaseDate,
        it.licenceId, it.licenceType, it.licenceStatus, it.isOnProbation,
      )
    }.isEqualTo(
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
    whenever(deliusApiClient.getTeamsCodesForUser(2000)).thenReturn(listOf("A01B02"))

    val prisoner = aPrisonerSearchResult.copy(
      topupSupervisionExpiryDate = null,
    )

    whenever(probationSearchApiClient.searchLicenceCaseloadByTeam("Test", listOf("A01B02"))).thenReturn(
      listOf(caseloadResult()),
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(emptyList())

    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(prisoner.prisonerNumber))).thenReturn(
      listOf(
        prisoner,
      ),
    )

    whenever(releaseDateService.getLicenceStartDates(any())).thenReturn(mapOf("A1234AA" to LocalDate.of(2023, 9, 14)))
    whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(emptySet()))
    whenever(eligibilityService.isEligibleForCvl(any())).thenReturn(true)

    val result = service.searchForOffenderOnStaffCaseload(request)

    verify(probationSearchApiClient).searchLicenceCaseloadByTeam(
      request.query,
      deliusApiClient.getTeamsCodesForUser(request.staffIdentifier),
    )

    verify(prisonerSearchApiClient).searchPrisonersByNomisIds(
      listOf(aPrisonerSearchResult.prisonerNumber),
    )

    verify(eligibilityService).isEligibleForCvl(
      any(),
    )

    verify(hdcService).getHdcStatus(listOf(prisoner))

    val resultsList = result.results
    val offender = resultsList.first()
    val inPrisonCount = result.inPrisonCount
    val onProbationCount = result.onProbationCount

    assertThat(resultsList).isNotEmpty
    assertThat(resultsList.size).isEqualTo(1)

    assertThat(offender).extracting {
      tuple(
        it.name, it.crn, it.nomisId, it.comName, it.comStaffCode, it.teamName, it.releaseDate,
        it.licenceId, it.licenceType, it.licenceStatus, it.isOnProbation,
      )
    }.isEqualTo(
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
    whenever(deliusApiClient.getTeamsCodesForUser(2000)).thenReturn(
      listOf(
        "A01B02",
      ),
    )
    whenever(probationSearchApiClient.searchLicenceCaseloadByTeam("Test", listOf("A01B02"))).thenReturn(
      listOf(caseloadResult()),
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(
      emptyList(),
    )

    val prisoner = aPrisonerSearchResult.copy(
      licenceExpiryDate = LocalDate.parse("2024-09-15"),
      topupSupervisionExpiryDate = LocalDate.parse("2024-09-14"),
    )
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(aPrisonerSearchResult.prisonerNumber))).thenReturn(
      listOf(prisoner),
    )

    whenever(releaseDateService.getLicenceStartDates(any())).thenReturn(mapOf("A1234AA" to LocalDate.of(2023, 9, 14)))
    whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(emptySet()))
    whenever(eligibilityService.isEligibleForCvl(any())).thenReturn(true)

    val result = service.searchForOffenderOnStaffCaseload(request)

    verify(probationSearchApiClient).searchLicenceCaseloadByTeam(
      request.query,
      deliusApiClient.getTeamsCodesForUser(request.staffIdentifier),
    )

    verify(prisonerSearchApiClient).searchPrisonersByNomisIds(
      listOf(aPrisonerSearchResult.prisonerNumber),
    )

    verify(eligibilityService).isEligibleForCvl(any())
    verify(hdcService).getHdcStatus(listOf(prisoner))

    val resultsList = result.results
    val offender = resultsList.first()
    val inPrisonCount = result.inPrisonCount
    val onProbationCount = result.onProbationCount

    assertThat(resultsList).isNotEmpty
    assertThat(resultsList.size).isEqualTo(1)

    assertThat(offender).extracting {
      tuple(
        it.name, it.crn, it.nomisId, it.comName, it.comStaffCode, it.teamName, it.releaseDate,
        it.licenceId, it.licenceType, it.licenceStatus, it.isOnProbation,
      )
    }.isEqualTo(
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
    whenever(deliusApiClient.getTeamsCodesForUser(2000)).thenReturn(
      listOf(
        "A01B02",
      ),
    )
    whenever(probationSearchApiClient.searchLicenceCaseloadByTeam("Test", listOf("A01B02"))).thenReturn(
      listOf(caseloadResult()),
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(
      emptyList(),
    )

    val prisoner = aPrisonerSearchResult.copy(
      licenceExpiryDate = LocalDate.parse("2024-09-15"),
      topupSupervisionExpiryDate = LocalDate.parse("2024-10-14"),
    )
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(aPrisonerSearchResult.prisonerNumber))).thenReturn(
      listOf(prisoner),
    )

    whenever(releaseDateService.getLicenceStartDates(any())).thenReturn(mapOf("A1234AA" to LocalDate.of(2023, 9, 14)))
    whenever(eligibilityService.isEligibleForCvl(any())).thenReturn(true)
    whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(emptySet()))

    val result = service.searchForOffenderOnStaffCaseload(request)

    verify(probationSearchApiClient).searchLicenceCaseloadByTeam(
      request.query,
      deliusApiClient.getTeamsCodesForUser(request.staffIdentifier),
    )

    verify(prisonerSearchApiClient).searchPrisonersByNomisIds(
      listOf(aPrisonerSearchResult.prisonerNumber),
    )

    verify(eligibilityService).isEligibleForCvl(any())
    verify(hdcService).getHdcStatus(listOf(prisoner))

    val resultsList = result.results
    val offender = resultsList.first()
    val inPrisonCount = result.inPrisonCount
    val onProbationCount = result.onProbationCount

    assertThat(resultsList).isNotEmpty
    assertThat(resultsList.size).isEqualTo(1)

    assertThat(offender).extracting {
      tuple(
        it.name, it.crn, it.nomisId, it.comName, it.comStaffCode, it.teamName, it.releaseDate,
        it.licenceId, it.licenceType, it.licenceStatus, it.isOnProbation,
      )
    }.isEqualTo(
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
    whenever(deliusApiClient.getTeamsCodesForUser(2000)).thenReturn(
      listOf(
        "A01B02",
      ),
    )
    whenever(probationSearchApiClient.searchLicenceCaseloadByTeam("Test", listOf("A01B02"))).thenReturn(
      listOf(caseloadResult()),
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(
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

    whenever(eligibilityService.isEligibleForCvl(aPrisonerSearchResult)).thenReturn(false)

    val result = service.searchForOffenderOnStaffCaseload(request)

    verify(probationSearchApiClient).searchLicenceCaseloadByTeam(
      request.query,
      deliusApiClient.getTeamsCodesForUser(request.staffIdentifier),
    )

    verify(prisonerSearchApiClient).searchPrisonersByNomisIds(
      listOf(aPrisonerSearchResult.prisonerNumber),
    )

    verify(eligibilityService).isEligibleForCvl(
      any(),
    )

    verify(hdcService).getHdcStatus(emptyList())

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
    whenever(deliusApiClient.getTeamsCodesForUser(2000)).thenReturn(
      listOf(
        "A01B02",
      ),
    )
    whenever(probationSearchApiClient.searchLicenceCaseloadByTeam("Test", listOf("A01B02"))).thenReturn(
      listOf(caseloadResult()),
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(
      emptyList(),
    )

    val prisoner = aPrisonerSearchResult.copy(confirmedReleaseDate = null)
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(prisoner.prisonerNumber))).thenReturn(
      listOf(prisoner),
    )

    whenever(releaseDateService.getLicenceStartDates(any())).thenReturn(mapOf("A1234AA" to LocalDate.of(2023, 9, 14)))
    whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(emptySet()))
    whenever(eligibilityService.isEligibleForCvl(any())).thenReturn(true)

    val request = ProbationUserSearchRequest("Test", 2000)

    val result = service.searchForOffenderOnStaffCaseload(request)

    verify(probationSearchApiClient).searchLicenceCaseloadByTeam(
      request.query,
      deliusApiClient.getTeamsCodesForUser(request.staffIdentifier),
    )

    verify(prisonerSearchApiClient).searchPrisonersByNomisIds(
      listOf(prisoner.prisonerNumber),
    )

    verify(eligibilityService).isEligibleForCvl(any())
    verify(hdcService).getHdcStatus(listOf(prisoner))

    val resultsList = result.results
    val offender = resultsList.first()
    val inPrisonCount = result.inPrisonCount
    val onProbationCount = result.onProbationCount

    assertThat(resultsList).isNotEmpty
    assertThat(resultsList.size).isEqualTo(1)

    assertThat(offender).extracting {
      tuple(
        it.name, it.crn, it.nomisId, it.comName, it.comStaffCode, it.teamName, it.releaseDate,
        it.licenceId, it.licenceType, it.licenceStatus, it.isOnProbation,
      )
    }.isEqualTo(
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
    whenever(deliusApiClient.getTeamsCodesForUser(2000)).thenReturn(
      listOf(
        "A01B02",
      ),
    )
    whenever(probationSearchApiClient.searchLicenceCaseloadByTeam("Test", listOf("A01B02"))).thenReturn(
      listOf(caseloadResult()),
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(
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

    val result = service.searchForOffenderOnStaffCaseload(request)

    verify(probationSearchApiClient).searchLicenceCaseloadByTeam(
      request.query,
      deliusApiClient.getTeamsCodesForUser(request.staffIdentifier),
    )

    verify(prisonerSearchApiClient).searchPrisonersByNomisIds(
      listOf(aPrisonerSearchResult.prisonerNumber),
    )

    verify(eligibilityService).isEligibleForCvl(
      aPrisonerSearchResult,
    )

    verify(hdcService).getHdcStatus(emptyList())

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
    whenever(deliusApiClient.getTeamsCodesForUser(2000)).thenReturn(
      listOf(
        "A01B02",
      ),
    )
    whenever(probationSearchApiClient.searchLicenceCaseloadByTeam("Test", listOf("A01B02"))).thenReturn(
      listOf(caseloadResult()),
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(emptyList())

    val prisoner = aPrisonerSearchResult.copy(
      homeDetentionCurfewEligibilityDate = LocalDate.parse("2023-09-14"),
    )
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(prisoner.prisonerNumber))).thenReturn(
      listOf(prisoner),
    )

    whenever(releaseDateService.getLicenceStartDates(any())).thenReturn(mapOf("A1234AA" to LocalDate.of(2023, 9, 14)))
    whenever(eligibilityService.isEligibleForCvl(any())).thenReturn(true)
    whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(emptySet()))

    val result = service.searchForOffenderOnStaffCaseload(request)

    verify(probationSearchApiClient).searchLicenceCaseloadByTeam(
      request.query,
      deliusApiClient.getTeamsCodesForUser(request.staffIdentifier),
    )

    verify(prisonerSearchApiClient).searchPrisonersByNomisIds(listOf(prisoner.prisonerNumber))

    verify(eligibilityService).isEligibleForCvl(any())
    verify(hdcService).getHdcStatus(listOf(prisoner))

    val resultsList = result.results
    val offender = resultsList.first()
    val inPrisonCount = result.inPrisonCount
    val onProbationCount = result.onProbationCount

    assertThat(resultsList).isNotEmpty
    assertThat(resultsList.size).isEqualTo(1)

    assertThat(offender).extracting {
      tuple(
        it.name, it.crn, it.nomisId, it.comName, it.comStaffCode, it.teamName, it.releaseDate,
        it.licenceId, it.licenceType, it.licenceStatus, it.isOnProbation,
      )
    }.isEqualTo(
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
    whenever(deliusApiClient.getTeamsCodesForUser(2000)).thenReturn(
      listOf(
        "A01B02",
      ),
    )
    whenever(probationSearchApiClient.searchLicenceCaseloadByTeam("Test", listOf("A01B02"))).thenReturn(
      listOf(caseloadResult()),
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(emptyList())

    val prisoner = aPrisonerSearchResult.copy(
      homeDetentionCurfewEligibilityDate = LocalDate.parse("2023-09-14"),
    )

    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(prisoner.prisonerNumber))).thenReturn(
      listOf(prisoner),
    )

    whenever(eligibilityService.isEligibleForCvl(any())).thenReturn(true)

    whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(setOf(prisoner.bookingId!!.toLong())))

    val result = service.searchForOffenderOnStaffCaseload(request)

    verify(probationSearchApiClient).searchLicenceCaseloadByTeam(
      request.query,
      deliusApiClient.getTeamsCodesForUser(request.staffIdentifier),
    )

    verify(prisonerSearchApiClient).searchPrisonersByNomisIds(listOf(prisoner.prisonerNumber))

    verify(eligibilityService).isEligibleForCvl(any())

    verify(hdcService).getHdcStatus(listOf(prisoner))

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
    whenever(deliusApiClient.getTeamsCodesForUser(2000)).thenReturn(listOf("A01B02"))
    whenever(probationSearchApiClient.searchLicenceCaseloadByTeam("Test", listOf("A01B02"))).thenReturn(
      listOf(caseloadResult()),
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(emptyList())

    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(aPrisonerSearchResult.prisonerNumber))).thenReturn(
      listOf(
        aPrisonerSearchResult,
      ),
    )

    whenever(releaseDateService.getLicenceStartDates(any())).thenReturn(mapOf("A1234AA" to LocalDate.of(2023, 9, 14)))
    whenever(eligibilityService.isEligibleForCvl(any())).thenReturn(true)
    whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(emptySet()))

    val result = service.searchForOffenderOnStaffCaseload(request)

    verify(probationSearchApiClient).searchLicenceCaseloadByTeam(
      request.query,
      deliusApiClient.getTeamsCodesForUser(request.staffIdentifier),
    )

    verify(prisonerSearchApiClient).searchPrisonersByNomisIds(listOf(aPrisonerSearchResult.prisonerNumber))
    verify(eligibilityService).isEligibleForCvl(aPrisonerSearchResult)
    verify(hdcService).getHdcStatus(listOf(aPrisonerSearchResult))

    val resultsList = result.results
    val offender = resultsList.first()
    val inPrisonCount = result.inPrisonCount
    val onProbationCount = result.onProbationCount

    assertThat(resultsList).isNotEmpty
    assertThat(resultsList.size).isEqualTo(1)

    assertThat(offender).extracting {
      tuple(
        it.name, it.crn, it.nomisId, it.comName, it.comStaffCode, it.teamName, it.releaseDate,
        it.licenceId, it.licenceType, it.licenceStatus, it.isOnProbation,
      )
    }.isEqualTo(
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
    whenever(deliusApiClient.getTeamsCodesForUser(2000)).thenReturn(listOf("A01B02"))
    whenever(probationSearchApiClient.searchLicenceCaseloadByTeam("Test", listOf("A01B02"))).thenReturn(
      listOf(caseloadResult()),
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(emptyList())

    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(aPrisonerSearchResult.prisonerNumber))).thenReturn(
      listOf(
        aPrisonerSearchResult,
      ),
    )

    whenever(eligibilityService.isEligibleForCvl(any())).thenReturn(true)
    whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(emptySet()))
    whenever(releaseDateService.isInHardStopPeriod(any(), anyOrNull())).thenReturn(true)
    whenever(releaseDateService.getHardStopDate(any())).thenReturn(LocalDate.of(2023, 2, 12))
    whenever(releaseDateService.getHardStopWarningDate(any())).thenReturn(LocalDate.of(2023, 3, 14))
    whenever(releaseDateService.isDueForEarlyRelease(any())).thenReturn(true)
    whenever(releaseDateService.isDueToBeReleasedInTheNextTwoWorkingDays(any())).thenReturn(true)

    val result = service.searchForOffenderOnStaffCaseload(request)

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
    whenever(deliusApiClient.getTeamsCodesForUser(2000)).thenReturn(
      listOf(
        "A01B02",
      ),
    )
    whenever(probationSearchApiClient.searchLicenceCaseloadByTeam("Test", listOf("A01B02"))).thenReturn(
      listOf(caseloadResult()),
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(
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
    whenever(eligibilityService.isEligibleForCvl(any())).thenReturn(true)
    whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(emptySet()))

    whenever(releaseDateService.isInHardStopPeriod(any(), anyOrNull())).thenReturn(true)
    whenever(releaseDateService.getHardStopDate(any())).thenReturn(LocalDate.of(2023, 2, 12))
    whenever(releaseDateService.getHardStopWarningDate(any())).thenReturn(LocalDate.of(2023, 3, 14))
    whenever(releaseDateService.isDueForEarlyRelease(any())).thenReturn(true)
    whenever(releaseDateService.isDueToBeReleasedInTheNextTwoWorkingDays(any())).thenReturn(true)

    val result = service.searchForOffenderOnStaffCaseload(request)

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
    whenever(deliusApiClient.getTeamsCodesForUser(2000)).thenReturn(
      listOf(
        "A01B02",
      ),
    )
    whenever(probationSearchApiClient.searchLicenceCaseloadByTeam("Test", listOf("A01B02"))).thenReturn(
      listOf(caseloadResult()),
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(
      listOf(
        createCrdLicence().copy(
          versionOfId = 2L,
        ),
      ),
    )

    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(listOf(aPrisonerSearchResult))
    whenever(eligibilityService.isEligibleForCvl(any())).thenReturn(true)
    whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(emptySet()))

    val result = service.searchForOffenderOnStaffCaseload(request)

    with(result.results.first()) {
      assertThat(kind).isEqualTo(LicenceKind.CRD)
      assertThat(versionOf).isEqualTo(2L)
    }
  }

  @Test
  fun `Release date is LSD`() {
    whenever(deliusApiClient.getTeamsCodesForUser(2000)).thenReturn(
      listOf(
        "A01B02",
      ),
    )
    whenever(probationSearchApiClient.searchLicenceCaseloadByTeam("Test", listOf("A01B02"))).thenReturn(
      listOf(caseloadResult()),
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(
      listOf(
        createHardStopLicence().copy(
          conditionalReleaseDate = LocalDate.of(2024, 4, 29),
          actualReleaseDate = LocalDate.of(2024, 4, 28),
          licenceStartDate = LocalDate.of(2024, 4, 27),
        ),
      ),
    )

    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(listOf(aPrisonerSearchResult))
    whenever(eligibilityService.isEligibleForCvl(any())).thenReturn(true)
    whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(emptySet()))

    val result = service.searchForOffenderOnStaffCaseload(request)

    with(result.results.first()) {
      assertThat(releaseDate).isEqualTo(LocalDate.of(2024, 4, 27))
    }
  }

  @Test
  fun `Release date label and is review needed are populated for not started CRD licences`() {
    whenever(deliusApiClient.getTeamsCodesForUser(2000)).thenReturn(
      listOf(
        "A01B02",
      ),
    )
    whenever(probationSearchApiClient.searchLicenceCaseloadByTeam("Test", listOf("A01B02"))).thenReturn(
      listOf(caseloadResult()),
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(emptyList())

    val prisoner = aPrisonerSearchResult.copy(
      confirmedReleaseDate = LocalDate.of(2023, 9, 14),
      conditionalReleaseDate = LocalDate.of(2023, 9, 15),
    )
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(
      listOf(prisoner),
    )

    whenever(releaseDateService.getLicenceStartDates(any())).thenReturn(mapOf("A1234AA" to LocalDate.of(2023, 9, 14)))

    whenever(eligibilityService.isEligibleForCvl(any())).thenReturn(true)
    whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(emptySet()))

    val result = service.searchForOffenderOnStaffCaseload(request)

    with(result.results.first()) {
      assertThat(releaseDateLabel).isEqualTo("Confirmed release date")
      assertThat(isReviewNeeded).isFalse()
    }
  }

  @Test
  fun `Release date label reads 'CRD' when licence start date matches CRD`() {
    whenever(deliusApiClient.getTeamsCodesForUser(2000)).thenReturn(
      listOf(
        "A01B02",
      ),
    )
    whenever(probationSearchApiClient.searchLicenceCaseloadByTeam("Test", listOf("A01B02"))).thenReturn(
      listOf(caseloadResult()),
    )
    whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(emptySet()))
    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(emptyList())

    val prisoner = aPrisonerSearchResult.copy(
      confirmedReleaseDate = null,
      conditionalReleaseDate = LocalDate.of(2023, 9, 14),
    )
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(listOf(prisoner))

    whenever(releaseDateService.getLicenceStartDates(any())).thenReturn(mapOf("A1234AA" to LocalDate.of(2023, 9, 14)))

    whenever(eligibilityService.isEligibleForCvl(any())).thenReturn(true)

    val result = service.searchForOffenderOnStaffCaseload(request)

    with(result.results.first()) {
      assertThat(releaseDateLabel).isEqualTo("CRD")
    }
  }

  @Test
  fun `Release date label reads 'Confirmed release date' when licence start date matches ARD`() {
    whenever(deliusApiClient.getTeamsCodesForUser(2000)).thenReturn(
      listOf(
        "A01B02",
      ),
    )
    whenever(probationSearchApiClient.searchLicenceCaseloadByTeam("Test", listOf("A01B02"))).thenReturn(
      listOf(caseloadResult()),
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(emptyList())

    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(
      listOf(
        aPrisonerSearchResult.copy(
          confirmedReleaseDate = LocalDate.of(2023, 9, 14),
          conditionalReleaseDate = LocalDate.of(2023, 9, 15),
        ),
      ),
    )

    whenever(releaseDateService.getLicenceStartDates(any())).thenReturn(mapOf("A1234AA" to LocalDate.of(2023, 9, 14)))
    whenever(eligibilityService.isEligibleForCvl(any())).thenReturn(true)
    whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(emptySet()))

    val result = service.searchForOffenderOnStaffCaseload(request)

    with(result.results.first()) {
      assertThat(releaseDateLabel).isEqualTo("Confirmed release date")
    }
  }

  @Test
  fun `Release date label reads 'CRD' when licence start date does not match either ARD or CRD`() {
    whenever(deliusApiClient.getTeamsCodesForUser(2000)).thenReturn(
      listOf(
        "A01B02",
      ),
    )
    whenever(probationSearchApiClient.searchLicenceCaseloadByTeam("Test", listOf("A01B02"))).thenReturn(
      listOf(caseloadResult()),
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(emptyList())

    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(
      listOf(
        aPrisonerSearchResult.copy(
          confirmedReleaseDate = LocalDate.of(2023, 9, 14),
          conditionalReleaseDate = LocalDate.of(2023, 9, 15),
        ),
      ),
    )

    whenever(releaseDateService.getLicenceStartDates(any())).thenReturn(mapOf("A1234AA" to LocalDate.of(2023, 9, 16)))
    whenever(eligibilityService.isEligibleForCvl(any())).thenReturn(true)
    whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(emptySet()))

    val result = service.searchForOffenderOnStaffCaseload(request)

    with(result.results.first()) {
      assertThat(releaseDateLabel).isEqualTo("CRD")
    }
  }

  @Test
  fun `Release date label and is review needed are populated for started hard stop licence`() {
    whenever(deliusApiClient.getTeamsCodesForUser(2000)).thenReturn(
      listOf(
        "A01B02",
      ),
    )
    whenever(probationSearchApiClient.searchLicenceCaseloadByTeam("Test", listOf("A01B02"))).thenReturn(
      listOf(caseloadResult()),
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(
      listOf(
        createHardStopLicence().copy(
          statusCode = LicenceStatus.ACTIVE,
          reviewDate = null,
        ),
      ),
    )

    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(listOf(aPrisonerSearchResult))
    whenever(eligibilityService.isEligibleForCvl(any())).thenReturn(true)
    whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(emptySet()))

    val result = service.searchForOffenderOnStaffCaseload(request)

    with(result.results.first()) {
      assertThat(releaseDateLabel).isEqualTo("Confirmed release date")
      assertThat(isReviewNeeded).isTrue()
    }
  }

  private companion object {
    val aLicenceEntity = createCrdLicence()
    val aPrisonerSearchResult = TestData.prisonerSearchResult()
    val request = ProbationUserSearchRequest("Test", 2000)
  }
}
