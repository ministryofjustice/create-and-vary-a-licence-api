package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple.tuple
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.ProbationUserSearchRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.EligibilityService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.HdcService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.HdcService.HdcStatuses
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceCreationService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.caseloadResult
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createCrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createHardStopLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createHdcLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createPrrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.CaseloadResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.util.ReleaseDateLabelFactory
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.workingDays.WorkingDaysService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType.AP_PSS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.ProbationSearchSortBy
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.SearchField
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class ComCaseloadSearchServiceTest {
  private val licenceRepository = mock<LicenceRepository>()
  private val deliusApiClient = mock<DeliusApiClient>()
  private val prisonerSearchApiClient = mock<PrisonerSearchApiClient>()
  private val hdcService = mock<HdcService>()
  private val eligibilityService = mock<EligibilityService>()
  private val releaseDateService = mock<ReleaseDateService>()
  private val licenceCreationService = mock<LicenceCreationService>()
  private val workingDaysService = mock<WorkingDaysService>()

  private val releaseDateLabelFactory = ReleaseDateLabelFactory(workingDaysService)

  private val service = ComCaseloadSearchService(
    licenceRepository,
    deliusApiClient,
    prisonerSearchApiClient,
    hdcService,
    eligibilityService,
    releaseDateService,
    clock,
    releaseDateLabelFactory,
  )

  @BeforeEach
  fun reset() {
    reset(
      licenceRepository,
      deliusApiClient,
      prisonerSearchApiClient,
      hdcService,
      eligibilityService,
      releaseDateService,
      licenceCreationService,
    )

    whenever(deliusApiClient.getTeamManagedOffenders(2000, "Test"))
      .thenReturn(CaseloadResponse(listOf(caseloadResult())))
    whenever(licenceCreationService.determineLicenceKind(any())).thenReturn(LicenceKind.CRD)
  }

  @Test
  fun `search for offenders in prison on a staff member's caseload`() {
    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn((listOf(aLicenceEntity)))
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(listOf(aPrisonerSearchResult))
    whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(emptySet()))
    whenever(eligibilityService.isEligibleForCvl(eq(aPrisonerSearchResult), anyOrNull())).thenReturn(true)

    val result = service.searchForOffenderOnStaffCaseload(request)

    assertThat(result.results.size).isEqualTo(1)
    assertThat(result.inPrisonCount).isEqualTo(1)
    assertThat(result.onProbationCount).isEqualTo(0)

    assertThat(result.results.first()).extracting {
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
  }

  @Test
  fun `search for offenders in prison with results sorted`() {
    whenever(
      deliusApiClient.getTeamManagedOffenders(
        2000,
        "Test",
        PageRequest.of(
          0,
          2000,
          Sort.by(
            Sort.Order(Sort.Direction.ASC, SearchField.SURNAME.probationSearchApiSortType),
            Sort.Order(Sort.Direction.DESC, SearchField.COM_FORENAME.probationSearchApiSortType),
          ),
        ),
      ),
    ).thenReturn(CaseloadResponse(listOf(caseloadResult())))

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn((listOf(aLicenceEntity)))
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(listOf(aPrisonerSearchResult))
    whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(emptySet()))
    whenever(eligibilityService.isEligibleForCvl(eq(aPrisonerSearchResult), anyOrNull())).thenReturn(true)

    val request = request.copy(
      sortBy = listOf(
        ProbationSearchSortBy(SearchField.SURNAME, Sort.Direction.ASC),
        ProbationSearchSortBy(SearchField.COM_FORENAME, Sort.Direction.DESC),
      ),
    )

    val result = service.searchForOffenderOnStaffCaseload(request)

    verify(deliusApiClient).getTeamManagedOffenders(
      2000,
      request.query,
      PageRequest.of(
        0,
        2000,
        Sort.by(
          Sort.Order(Sort.Direction.ASC, SearchField.SURNAME.probationSearchApiSortType),
          Sort.Order(Sort.Direction.DESC, SearchField.COM_FORENAME.probationSearchApiSortType),
        ),
      ),
    )

    assertThat(result.results.size).isEqualTo(1)
    assertThat(result.results.first()).extracting { tuple(it.name, it.comName, it.teamName) }.isEqualTo(
      tuple("Test Surname", "Staff Surname", "Test Team"),
    )
  }

  @Test
  fun `search for offenders in prison with latest licence selected`() {
    val licences = listOf(
      aLicenceEntity.copy(
        id = 1,
        statusCode = LicenceStatus.APPROVED,
        versionOfId = null,
        licenceVersion = "1.0",
      ),
      aLicenceEntity.copy(
        id = 2,
        statusCode = LicenceStatus.IN_PROGRESS,
        versionOfId = 1,
        licenceVersion = "1.1",
      ),
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(licences)
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(listOf(aPrisonerSearchResult))
    whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(emptySet()))
    whenever(eligibilityService.isEligibleForCvl(any(), anyOrNull())).thenReturn(true)

    val result = service.searchForOffenderOnStaffCaseload(request)

    assertThat(result.results.size).isEqualTo(1)
    assertThat(result.inPrisonCount).isEqualTo(1)
    assertThat(result.onProbationCount).isEqualTo(0)

    assertThat(result.results.first()).extracting {
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
        2L,
        LicenceType.AP,
        LicenceStatus.IN_PROGRESS,
        false,
      ),
    )
  }

  @Test
  fun `search for offenders on probation with latest licence selected`() {
    val licences = listOf(
      aLicenceEntity.copy(
        id = 1,
        statusCode = LicenceStatus.ACTIVE,
        licenceVersion = "1.0",
      ),
      aLicenceEntity.copy(
        id = 2,
        statusCode = LicenceStatus.VARIATION_SUBMITTED,
        licenceVersion = "2.0",
      ),
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(licences)

    val result = service.searchForOffenderOnStaffCaseload(request)

    // No checks take place for released prisoners
    verifyNoInteractions(hdcService)
    verifyNoInteractions(eligibilityService)

    assertThat(result.results.size).isEqualTo(1)
    assertThat(result.inPrisonCount).isEqualTo(0)
    assertThat(result.onProbationCount).isEqualTo(1)

    assertThat(result.results.first()).extracting {
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
        2L,
        LicenceType.AP,
        LicenceStatus.VARIATION_SUBMITTED,
        true,
      ),
    )
  }

  @Test
  fun `search for offenders in prison without an unstarted licence`() {
    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(emptyList())
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(listOf(aPrisonerSearchResult))
    whenever(releaseDateService.getLicenceStartDates(any())).thenReturn(mapOf("A1234AA" to LocalDate.of(2023, 9, 14)))
    whenever(eligibilityService.isEligibleForCvl(eq(aPrisonerSearchResult), anyOrNull())).thenReturn(true)
    whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(emptySet()))

    val result = service.searchForOffenderOnStaffCaseload(request)

    assertThat(result.results.size).isEqualTo(1)
    assertThat(result.inPrisonCount).isEqualTo(1)
    assertThat(result.onProbationCount).isEqualTo(0)

    assertThat(result.results.first()).extracting {
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
  }

  @Test
  fun `search for offenders in prison without a licence where NOMIS ID is not populated`() {
    whenever(deliusApiClient.getTeamManagedOffenders(2000, "Test"))
      .thenReturn(CaseloadResponse(listOf(caseloadResult().copy(crn = "X123456", nomisId = null))))

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(emptyList())

    val result = service.searchForOffenderOnStaffCaseload(request)

    verifyNoInteractions(prisonerSearchApiClient)
    verifyNoInteractions(eligibilityService)
    verifyNoInteractions(hdcService)

    assertThat(result.results.size).isEqualTo(0)
    assertThat(result.inPrisonCount).isEqualTo(0)
    assertThat(result.onProbationCount).isEqualTo(0)
  }

  @Test
  fun `search for offenders in prison without a licence sets PSS licence type correctly `() {
    val prisoner = aPrisonerSearchResult.copy(licenceExpiryDate = null)

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(emptyList())
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(listOf(prisoner))
    whenever(releaseDateService.getLicenceStartDates(any())).thenReturn(mapOf("A1234AA" to LocalDate.of(2023, 9, 14)))
    whenever(eligibilityService.isEligibleForCvl(any(), anyOrNull())).thenReturn(true)
    whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(emptySet()))

    val result = service.searchForOffenderOnStaffCaseload(request)

    assertThat(result.results.size).isEqualTo(1)
    assertThat(result.inPrisonCount).isEqualTo(1)
    assertThat(result.onProbationCount).isEqualTo(0)

    assertThat(result.results.first().licenceType).isEqualTo(LicenceType.PSS)
  }

  @Test
  fun `search for offenders in prison without a licence sets AP licence type correctly where there is no TUSED`() {
    val prisoner = aPrisonerSearchResult.copy(
      topupSupervisionExpiryDate = null,
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(emptyList())
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(listOf(prisoner))
    whenever(releaseDateService.getLicenceStartDates(any())).thenReturn(mapOf("A1234AA" to LocalDate.of(2023, 9, 14)))
    whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(emptySet()))
    whenever(eligibilityService.isEligibleForCvl(any(), anyOrNull())).thenReturn(true)

    val result = service.searchForOffenderOnStaffCaseload(request)

    assertThat(result.results.size).isEqualTo(1)
    assertThat(result.inPrisonCount).isEqualTo(1)
    assertThat(result.onProbationCount).isEqualTo(0)

    assertThat(result.results.first().licenceType).isEqualTo(LicenceType.AP)
  }

  @Test
  fun `search for offenders in prison without a licence sets AP licence type correctly where TUSED before LED`() {
    val prisoner = aPrisonerSearchResult.copy(
      licenceExpiryDate = LocalDate.parse("2024-09-15"),
      topupSupervisionExpiryDate = LocalDate.parse("2024-09-14"),
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(emptyList())
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(listOf(prisoner))
    whenever(releaseDateService.getLicenceStartDates(any())).thenReturn(mapOf("A1234AA" to LocalDate.of(2023, 9, 14)))
    whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(emptySet()))
    whenever(eligibilityService.isEligibleForCvl(any(), anyOrNull())).thenReturn(true)

    val result = service.searchForOffenderOnStaffCaseload(request)

    assertThat(result.results.size).isEqualTo(1)
    assertThat(result.inPrisonCount).isEqualTo(1)
    assertThat(result.onProbationCount).isEqualTo(0)

    assertThat(result.results.first().licenceType).isEqualTo(LicenceType.AP)
  }

  @Test
  fun `search for offenders in prison without a licence sets AP_PSS licence type correctly`() {
    val prisoner = aPrisonerSearchResult.copy(
      licenceExpiryDate = LocalDate.parse("2024-09-15"),
      topupSupervisionExpiryDate = LocalDate.parse("2024-10-14"),
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(emptyList())
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(listOf(prisoner))
    whenever(releaseDateService.getLicenceStartDates(any())).thenReturn(mapOf("A1234AA" to LocalDate.of(2023, 9, 14)))
    whenever(eligibilityService.isEligibleForCvl(any(), anyOrNull())).thenReturn(true)
    whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(emptySet()))

    val result = service.searchForOffenderOnStaffCaseload(request)

    assertThat(result.results.size).isEqualTo(1)
    assertThat(result.inPrisonCount).isEqualTo(1)
    assertThat(result.onProbationCount).isEqualTo(0)

    assertThat(result.results.first().licenceType).isEqualTo(AP_PSS)
  }

  @Test
  fun `search for offenders in prison without a licence without any release date data should be ignored`() {
    val prisoner = aPrisonerSearchResult.copy(
      releaseDate = null,
      confirmedReleaseDate = null,
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(emptyList())
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(listOf(prisoner))
    whenever(eligibilityService.isEligibleForCvl(eq(aPrisonerSearchResult), anyOrNull())).thenReturn(false)

    val result = service.searchForOffenderOnStaffCaseload(request)

    assertThat(result.results).isEmpty()
    assertThat(result.inPrisonCount).isEqualTo(0)
    assertThat(result.onProbationCount).isEqualTo(0)
  }

  @Test
  fun `search for offenders in prison without a licence with no CRD should use release date`() {
    val prisoner = aPrisonerSearchResult.copy(confirmedReleaseDate = null)

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(emptyList())
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(listOf(prisoner))
    whenever(releaseDateService.getLicenceStartDates(any())).thenReturn(mapOf("A1234AA" to LocalDate.of(2023, 9, 14)))
    whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(emptySet()))
    whenever(eligibilityService.isEligibleForCvl(any(), anyOrNull())).thenReturn(true)

    val result = service.searchForOffenderOnStaffCaseload(request)

    assertThat(result.results.size).isEqualTo(1)
    assertThat(result.inPrisonCount).isEqualTo(1)
    assertThat(result.onProbationCount).isEqualTo(0)

    assertThat(result.results.first().releaseDate).isEqualTo(LocalDate.parse("2023-09-14"))
  }

  @Test
  fun `search for offenders in prison without a licence and ineligible for CVL`() {
    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(emptyList())
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(listOf(aPrisonerSearchResult))
    whenever(eligibilityService.isEligibleForCvl(eq(aPrisonerSearchResult), anyOrNull())).thenReturn(false)

    val result = service.searchForOffenderOnStaffCaseload(request)

    assertThat(result.results).isEmpty()
    assertThat(result.inPrisonCount).isEqualTo(0)
    assertThat(result.onProbationCount).isEqualTo(0)
  }

  @Test
  fun `search for offenders in prison without a licence, eligible for CVL and is a non approved HDC case`() {
    val prisoner = aPrisonerSearchResult.copy(
      homeDetentionCurfewEligibilityDate = LocalDate.parse("2023-09-14"),
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(emptyList())
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(listOf(prisoner))
    whenever(releaseDateService.getLicenceStartDates(any())).thenReturn(mapOf("A1234AA" to LocalDate.of(2023, 9, 14)))
    whenever(eligibilityService.isEligibleForCvl(any(), anyOrNull())).thenReturn(true)
    whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(emptySet()))

    val result = service.searchForOffenderOnStaffCaseload(request)

    assertThat(result.inPrisonCount).isEqualTo(1)
    assertThat(result.onProbationCount).isEqualTo(0)
    assertThat(result.results.first()).extracting {
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
  }

  @ParameterizedTest
  @EnumSource(
    value = LicenceStatus::class,
    names = ["NOT_STARTED", "IN_PROGRESS", "SUBMITTED", "APPROVED"],
  )
  fun `search for offenders in prison with a CRD licence, eligible for CVL and is an approved HDC case`(status: LicenceStatus) {
    val prisoner = aPrisonerSearchResult.copy(
      homeDetentionCurfewEligibilityDate = LocalDate.parse("2023-09-14"),
    )
    val crdLicence = createCrdLicence().copy(
      bookingId = prisoner.bookingId!!.toLong(),
      versionOfId = 2L,
      statusCode = status,
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(listOf(crdLicence))
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(listOf(prisoner))
    whenever(eligibilityService.isEligibleForCvl(any(), anyOrNull())).thenReturn(true)
    whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(setOf(prisoner.bookingId.toLong())))

    val result = service.searchForOffenderOnStaffCaseload(request)

    verify(hdcService).getHdcStatus(listOf(prisoner))

    assertThat(result.results).hasSize(0)
    assertThat(result.inPrisonCount).isEqualTo(0)
    assertThat(result.onProbationCount).isEqualTo(0)
  }

  @ParameterizedTest
  @EnumSource(
    value = LicenceStatus::class,
    names = ["NOT_STARTED", "IN_PROGRESS", "SUBMITTED", "APPROVED"],
  )
  fun `search for offenders in prison with a CRD licence but not in progress, eligible for CVL and is an approved HDC case`(
    status: LicenceStatus,
  ) {
    val prisoner = aPrisonerSearchResult.copy(
      homeDetentionCurfewEligibilityDate = LocalDate.parse("2023-09-14"),
    )
    val crdLicence = createCrdLicence().copy(
      bookingId = prisoner.bookingId!!.toLong(),
      versionOfId = 2L,
      statusCode = status,
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(listOf(crdLicence))
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(listOf(prisoner))
    whenever(eligibilityService.isEligibleForCvl(any(), anyOrNull())).thenReturn(true)
    whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(setOf(prisoner.bookingId.toLong())))

    val result = service.searchForOffenderOnStaffCaseload(request)

    verify(hdcService).getHdcStatus(listOf(prisoner))

    assertThat(result.results).hasSize(0)
    assertThat(result.inPrisonCount).isEqualTo(0)
    assertThat(result.onProbationCount).isEqualTo(0)
  }

  @ParameterizedTest
  @EnumSource(
    value = LicenceStatus::class,
    names = ["NOT_STARTED", "IN_PROGRESS", "SUBMITTED", "APPROVED"],
  )
  fun `search for offenders in prison with an in progress HDC licence, eligible for CVL and is an approved HDC case`(
    status: LicenceStatus,
  ) {
    val prisoner = aPrisonerSearchResult.copy(
      homeDetentionCurfewEligibilityDate = LocalDate.parse("2023-09-14"),
    )
    val crdLicence = createHdcLicence().copy(
      bookingId = prisoner.bookingId!!.toLong(),
      versionOfId = 2L,
      statusCode = status,
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(listOf(crdLicence))
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(listOf(prisoner))
    whenever(eligibilityService.isEligibleForCvl(any(), anyOrNull())).thenReturn(true)
    whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(setOf(prisoner.bookingId.toLong())))

    val result = service.searchForOffenderOnStaffCaseload(request)

    verify(hdcService).getHdcStatus(listOf(prisoner))

    assertThat(result.results).hasSize(1)
    assertThat(result.inPrisonCount).isEqualTo(1)
    assertThat(result.onProbationCount).isEqualTo(0)
  }

  @Test
  fun `search for offenders in prison without a licence, eligible for CVL and is an approved HDC case`() {
    val prisoner = aPrisonerSearchResult.copy(
      homeDetentionCurfewEligibilityDate = LocalDate.parse("2023-09-14"),
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(emptyList())
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(listOf(prisoner))
    whenever(eligibilityService.isEligibleForCvl(any(), anyOrNull())).thenReturn(true)
    whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(setOf(prisoner.bookingId!!.toLong())))

    val result = service.searchForOffenderOnStaffCaseload(request)

    assertThat(result.results).isEmpty()
    assertThat(result.inPrisonCount).isEqualTo(0)
    assertThat(result.onProbationCount).isEqualTo(0)
  }

  @Test
  fun `search for offenders in prison without a licence, eligible for CVL, HDC case but without HDCED`() {
    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(emptyList())
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(listOf(aPrisonerSearchResult))
    whenever(releaseDateService.getLicenceStartDates(any())).thenReturn(mapOf("A1234AA" to LocalDate.of(2023, 9, 14)))
    whenever(eligibilityService.isEligibleForCvl(any(), anyOrNull())).thenReturn(true)
    whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(emptySet()))

    val result = service.searchForOffenderOnStaffCaseload(request)

    assertThat(result.results.size).isEqualTo(1)
    assertThat(result.inPrisonCount).isEqualTo(1)
    assertThat(result.onProbationCount).isEqualTo(0)

    assertThat(result.results.first()).extracting {
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
  }

  @Test
  fun `hard stop dates are populated for non-started licences`() {
    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(emptyList())
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(listOf(aPrisonerSearchResult))
    whenever(eligibilityService.isEligibleForCvl(any(), anyOrNull())).thenReturn(true)
    whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(emptySet()))
    whenever(releaseDateService.getLicenceStartDates(any())).thenReturn(mapOf("A1234AA" to LocalDate.of(2023, 2, 14)))

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
    val licences = listOf(
      aLicenceEntity.copy(
        id = 1,
        statusCode = LicenceStatus.ACTIVE,
        licenceVersion = "1.0",
      ),
      aLicenceEntity.copy(
        id = 2,
        statusCode = LicenceStatus.APPROVED,
        licenceVersion = "2.0",
      ),
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(licences)
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(listOf(aPrisonerSearchResult))
    whenever(eligibilityService.isEligibleForCvl(any(), anyOrNull())).thenReturn(true)
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
    val crdLicence = createCrdLicence().copy(
      versionOfId = 2L,
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(listOf(crdLicence))
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(listOf(aPrisonerSearchResult))
    whenever(eligibilityService.isEligibleForCvl(any(), anyOrNull())).thenReturn(true)
    whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(emptySet()))

    val result = service.searchForOffenderOnStaffCaseload(request)

    with(result.results.first()) {
      assertThat(kind).isEqualTo(LicenceKind.CRD)
      assertThat(versionOf).isEqualTo(2L)
    }
  }

  @Test
  fun `Release date is LSD`() {
    val hardStopLicence = createHardStopLicence().copy(
      conditionalReleaseDate = LocalDate.of(2024, 4, 29),
      actualReleaseDate = LocalDate.of(2024, 4, 28),
      licenceStartDate = LocalDate.of(2024, 4, 27),
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(listOf(hardStopLicence))
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(listOf(aPrisonerSearchResult))
    whenever(eligibilityService.isEligibleForCvl(any(), anyOrNull())).thenReturn(true)
    whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(emptySet()))

    val result = service.searchForOffenderOnStaffCaseload(request)

    assertThat(result.results.first().releaseDate).isEqualTo(LocalDate.of(2024, 4, 27))
  }

  @Test
  fun `Release date label and is review needed are populated for not started CRD licences`() {
    val prisoner = aPrisonerSearchResult.copy(
      confirmedReleaseDate = LocalDate.of(2023, 9, 14),
      conditionalReleaseDate = LocalDate.of(2023, 9, 15),
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(emptyList())
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(listOf(prisoner))
    whenever(releaseDateService.getLicenceStartDates(any())).thenReturn(mapOf("A1234AA" to LocalDate.of(2023, 9, 14)))
    whenever(eligibilityService.isEligibleForCvl(any(), anyOrNull())).thenReturn(true)
    whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(emptySet()))

    val result = service.searchForOffenderOnStaffCaseload(request)

    with(result.results.first()) {
      assertThat(releaseDateLabel).isEqualTo("Confirmed release date")
      assertThat(isReviewNeeded).isFalse()
    }
  }

  @Test
  fun `Release date label reads 'CRD' when licence start date matches CRD`() {
    val prisoner = aPrisonerSearchResult.copy(
      confirmedReleaseDate = null,
      conditionalReleaseDate = LocalDate.of(2023, 9, 14),
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(emptyList())
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(listOf(prisoner))
    whenever(releaseDateService.getLicenceStartDates(any())).thenReturn(mapOf("A1234AA" to LocalDate.of(2023, 9, 14)))
    whenever(eligibilityService.isEligibleForCvl(any(), anyOrNull())).thenReturn(true)
    whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(emptySet()))

    val result = service.searchForOffenderOnStaffCaseload(request)

    assertThat(result.results.first().releaseDateLabel).isEqualTo("CRD")
  }

  @Test
  fun `Release date label reads 'Post-recall release date (PRRD)' when licence start date matches PRRD`() {
    // Given

    val licenceStartDate = LocalDate.of(2023, 9, 14)

    val prisoner = aPrisonerSearchResult.copy(
      confirmedReleaseDate = null,
      postRecallReleaseDate = licenceStartDate,
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(emptyList())
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(listOf(prisoner))
    whenever(releaseDateService.getLicenceStartDates(any())).thenReturn(mapOf("A1234AA" to licenceStartDate))
    whenever(eligibilityService.isEligibleForCvl(any(), anyOrNull())).thenReturn(true)
    whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(emptySet()))
    whenever(workingDaysService.getLastWorkingDay(prisoner.postRecallReleaseDate)).thenReturn(prisoner.postRecallReleaseDate)

    // When
    val result = service.searchForOffenderOnStaffCaseload(request)

    // Then
    assertThat(result.results.first().releaseDateLabel).isEqualTo("Post-recall release date (PRRD)")
    verify(eligibilityService).isEligibleForCvl(any(), anyOrNull())
  }

  @Test
  fun `for offenders in prison with a PRRD licence the release date label reads 'Post-recall release date (PRRD)' when licence start date matches PRRD`() {
    // Given

    val licenceStartDate = LocalDate.of(2023, 9, 14)
    val prrdLicence = createPrrdLicence().copy(
      versionOfId = 2L,
      licenceStartDate = licenceStartDate,
      postRecallReleaseDate = licenceStartDate,
    )
    val prisoner = aPrisonerSearchResult.copy(
      confirmedReleaseDate = null,
      postRecallReleaseDate = licenceStartDate,
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(emptyList())
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(listOf(prisoner))
    whenever(releaseDateService.getLicenceStartDates(any())).thenReturn(mapOf("A1234AA" to licenceStartDate))
    whenever(eligibilityService.isEligibleForCvl(any(), anyOrNull())).thenReturn(true)
    whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(emptySet()))
    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(listOf(prrdLicence))
    whenever(workingDaysService.getLastWorkingDay(prisoner.postRecallReleaseDate)).thenReturn(licenceStartDate)

    // When
    val result = service.searchForOffenderOnStaffCaseload(request)

    // Then
    assertThat(result.results.first().releaseDateLabel).isEqualTo("Post-recall release date (PRRD)")
  }

  @Test
  fun `Ensures search for offenders in prison with a PRRD licence do not check eligibility for Cvl if new PRRD licence`() {
    // Given

    val licenceStartDate = LocalDate.of(2023, 9, 14)
    val prrdLicence = createPrrdLicence().copy(
      versionOfId = 2L,
    )
    val prisoner = aPrisonerSearchResult.copy(
      confirmedReleaseDate = null,
      postRecallReleaseDate = licenceStartDate,
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(emptyList())
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(listOf(prisoner))
    whenever(releaseDateService.getLicenceStartDates(any())).thenReturn(mapOf("A1234AA" to licenceStartDate))
    whenever(eligibilityService.isEligibleForCvl(any(), anyOrNull())).thenReturn(true)
    whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(emptySet()))
    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(listOf(prrdLicence))

    // When
    service.searchForOffenderOnStaffCaseload(request)

    // Then
    verify(eligibilityService, never()).isEligibleForCvl(any(), anyOrNull())
  }

  @Test
  fun `Release date label reads 'Confirmed release date' when licence start date matches ARD`() {
    val prisoner = aPrisonerSearchResult.copy(
      confirmedReleaseDate = LocalDate.of(2023, 9, 14),
      conditionalReleaseDate = LocalDate.of(2023, 9, 15),
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(emptyList())
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(listOf(prisoner))
    whenever(releaseDateService.getLicenceStartDates(any())).thenReturn(mapOf("A1234AA" to LocalDate.of(2023, 9, 14)))
    whenever(eligibilityService.isEligibleForCvl(any(), anyOrNull())).thenReturn(true)
    whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(emptySet()))

    val result = service.searchForOffenderOnStaffCaseload(request)

    assertThat(result.results.first().releaseDateLabel).isEqualTo("Confirmed release date")
  }

  @Test
  fun `Release date label reads 'CRD' when licence start date does not match either ARD or CRD or PRRD`() {
    val prisoner = aPrisonerSearchResult.copy(
      confirmedReleaseDate = LocalDate.of(2023, 9, 14),
      conditionalReleaseDate = LocalDate.of(2023, 9, 15),
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(emptyList())
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(listOf(prisoner))
    whenever(releaseDateService.getLicenceStartDates(any())).thenReturn(mapOf("A1234AA" to LocalDate.of(2023, 9, 16)))
    whenever(eligibilityService.isEligibleForCvl(any(), anyOrNull())).thenReturn(true)
    whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(emptySet()))

    val result = service.searchForOffenderOnStaffCaseload(request)

    assertThat(result.results.first().releaseDateLabel).isEqualTo("CRD")
  }

  @Test
  fun `Release date label and is review needed are populated for started hard stop licence`() {
    val hardStopLicence = createHardStopLicence().copy(
      statusCode = LicenceStatus.ACTIVE,
      reviewDate = null,
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(listOf(hardStopLicence))
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(listOf(aPrisonerSearchResult))
    whenever(eligibilityService.isEligibleForCvl(any(), anyOrNull())).thenReturn(true)
    whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(emptySet()))

    val result = service.searchForOffenderOnStaffCaseload(request)

    with(result.results.first()) {
      assertThat(releaseDateLabel).isEqualTo("Confirmed release date")
      assertThat(isReviewNeeded).isTrue()
    }
  }

  @Test
  fun `Licences with a null licence start date are filtered out`() {
    val licence = aLicenceEntity.copy(
      licenceStartDate = null,
    )
    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(listOf(licence))
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(listOf(aPrisonerSearchResult))
    whenever(eligibilityService.isEligibleForCvl(any(), anyOrNull())).thenReturn(true)
    whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(emptySet()))

    val result = service.searchForOffenderOnStaffCaseload(request)

    assertThat(result.results.size).isEqualTo(0)
    assertThat(result.inPrisonCount).isEqualTo(0)
    assertThat(result.onProbationCount).isEqualTo(0)
  }

  @Test
  fun `Licences with a past licence start date are filtered out`() {
    val licence = aLicenceEntity.copy(
      licenceStartDate = LocalDate.now(clock).minusDays(1),
    )
    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(listOf(licence))
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(listOf(aPrisonerSearchResult))
    whenever(eligibilityService.isEligibleForCvl(any(), anyOrNull())).thenReturn(true)
    whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(emptySet()))

    val result = service.searchForOffenderOnStaffCaseload(request)

    assertThat(result.results.size).isEqualTo(0)
    assertThat(result.inPrisonCount).isEqualTo(0)
    assertThat(result.onProbationCount).isEqualTo(0)
  }

  @Test
  fun `Active licences with a past licence start date are not filtered out`() {
    val licence = aLicenceEntity.copy(
      licenceStartDate = LocalDate.now(clock).minusDays(1),
      statusCode = LicenceStatus.ACTIVE,
    )
    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(listOf(licence))
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(listOf(aPrisonerSearchResult))
    whenever(eligibilityService.isEligibleForCvl(any(), anyOrNull())).thenReturn(true)
    whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(emptySet()))

    val result = service.searchForOffenderOnStaffCaseload(request)

    assertThat(result.results.size).isEqualTo(1)
    assertThat(result.inPrisonCount).isEqualTo(0)
    assertThat(result.onProbationCount).isEqualTo(1)
  }

  private companion object {
    val aLicenceEntity = createCrdLicence()
    val aPrisonerSearchResult = TestData.prisonerSearchResult()
    val request = ProbationUserSearchRequest("Test", 2000)
    val clock: Clock = Clock.fixed(Instant.parse("2021-01-01T00:00:00Z"), ZoneId.systemDefault())
  }
}
