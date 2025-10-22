package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ComCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceCreationType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ProbationPractitioner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceCaseRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceComCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CaseloadService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CvlRecordService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.EligibilityService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.HdcService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.HdcService.HdcStatuses
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.aCvlRecord
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.managedOffenderCrn
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.prisonerSearchResult
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ManagedOffenderCrn
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.Name
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.StaffDetail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.response.StaffNameResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month

class ComCreateCaseloadServiceTest {
  private val caseloadService = mock<CaseloadService>()
  private val deliusApiClient = mock<DeliusApiClient>()
  private val licenceCaseRepository = mock<LicenceCaseRepository>()
  private val hdcService = mock<HdcService>()
  private val eligibilityService = mock<EligibilityService>()
  private val releaseDateService = mock<ReleaseDateService>()
  private val cvlRecordService = mock<CvlRecordService>()

  private val service = ComCreateCaseloadService(
    caseloadService,
    deliusApiClient,
    licenceCaseRepository,
    hdcService,
    releaseDateService,
    cvlRecordService,
  )

  private val elevenDaysFromNow = LocalDate.now().plusDays(11)
  private val tenDaysFromNow = LocalDate.now().plusDays(10)
  private val nineDaysFromNow = LocalDate.now().plusDays(9)
  private val twoDaysFromNow = LocalDate.now().plusDays(2)
  private val yesterday = LocalDate.now().minusDays(1)
  private val deliusStaffIdentifier = 213L

  @BeforeEach
  fun reset() {
    reset(deliusApiClient, licenceCaseRepository, hdcService, eligibilityService)
    whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(emptySet()))
  }

  @Test
  fun `it filters out cases with no NOMIS record`() {
    val managedOffenders = listOf(
      ManagedOffenderCrn(crn = "X12348", nomisId = "AB1234E"),
    )
    whenever(
      deliusApiClient.getManagedOffenders(deliusStaffIdentifier),
    ).thenReturn(managedOffenders)

    whenever(caseloadService.getPrisonersByNumber(any())).thenReturn(emptyList())

    val caseload = service.getStaffCreateCaseload(deliusStaffIdentifier)

    assertThat(caseload).hasSize(0)
    verify(cvlRecordService, times(1)).getCvlRecords(emptyList(), emptyMap())
  }

  @Test
  fun `it filters out cases with no NOMIS ID on their Delius records`() {
    val managedOffenders = listOf(
      ManagedOffenderCrn(crn = "X12348", nomisId = null),
    )
    whenever(
      deliusApiClient.getManagedOffenders(deliusStaffIdentifier),
    ).thenReturn(managedOffenders)

    whenever(caseloadService.getPrisonersByNumber(any())).thenReturn(emptyList())

    val caseload = service.getStaffCreateCaseload(deliusStaffIdentifier)

    assertThat(caseload).hasSize(0)
    verify(caseloadService, never()).getPrisonersByNumber(any())
  }

  @Test
  fun `it calls the licence service when Nomis records are found`() {
    val cases = mapOf(
      managedOffenderCrn() to prisonerSearchResult().copy(
        prisonerNumber = "ABC123",
        firstName = "Person",
        lastName = "One",
        dateOfBirth = LocalDate.of(1970, 1, 1),
        conditionalReleaseDate = tenDaysFromNow,
      ),
    )

    val cvlRecords = listOf(
      aCvlRecord(
        nomsId = "ABC123",
        kind = LicenceKind.CRD,
        licenceStartDate = tenDaysFromNow,
      ),
    )

    val casesAndLicences = service.mapCasesToLicences(cases, cvlRecords)

    assertThat(casesAndLicences).hasSize(1)
    assertThat(casesAndLicences["X12348"]).hasSize(1)
    assertThat(casesAndLicences["X12348"]!!.first().licenceStatus).isEqualTo(LicenceStatus.NOT_STARTED)
    verify(licenceCaseRepository).findLicenceCasesForCom(any(), any())
  }

  @Test
  fun `it sets not started licences to timed out when in the hard stop period`() {
    whenever(releaseDateService.isInHardStopPeriod(any(), anyOrNull())).thenReturn(true)

    val cases = mapOf(
      aManagedOffenderCrn() to prisonerSearchResult().copy(
        prisonerNumber = "ABC123",
        firstName = "Person",
        lastName = "One",
        dateOfBirth = LocalDate.of(1970, 1, 1),
        conditionalReleaseDate = twoDaysFromNow,
      ),
    )

    val cvlRecords = listOf(
      aCvlRecord(
        nomsId = "ABC123",
        kind = LicenceKind.CRD,
        licenceStartDate = twoDaysFromNow,
      ),
    )

    val caseAndLicences = service.mapCasesToLicences(cases, cvlRecords)

    assertThat(caseAndLicences).hasSize(1)
    val licence = caseAndLicences["X12348"]!!.first()
    assertThat(licence.nomisId).isEqualTo("ABC123")
    assertThat(caseAndLicences["X12348"]!!).hasSize(1)
    assertThat(licence.licenceStatus).isEqualTo(LicenceStatus.TIMED_OUT)
    assertThat(licence.licenceType).isEqualTo(LicenceType.AP)
  }

  @Test
  fun `it filters invalid data due to mismatch between delius and nomis`() {
    val managedOffenders = listOf(
      ManagedOffenderCrn(crn = "X12346", nomisId = "AB1234D"),
      ManagedOffenderCrn(crn = "X12347"),
      ManagedOffenderCrn(crn = "X12348", nomisId = "AB1234E"),
    )

    whenever(deliusApiClient.getManagedOffenders(deliusStaffIdentifier)).thenReturn(managedOffenders)

    whenever(caseloadService.getPrisonersByNumber(any())).thenReturn(
      listOf(
        prisonerSearchResult().copy(prisonerNumber = "AB1234E", conditionalReleaseDate = tenDaysFromNow, bookingId = "1"),
      ),
    )
    whenever(cvlRecordService.getCvlRecords(any(), any())).thenReturn(listOf(aCvlRecord(nomsId = "AB1234E", kind = LicenceKind.CRD, licenceStartDate = tenDaysFromNow)))

    val caseload = service.getStaffCreateCaseload(deliusStaffIdentifier)

    assertThat(caseload).hasSize(1)
    verifyCase(
      caseload.first(),
      "X12348",
      "AB1234E",
      LicenceStatus.NOT_STARTED,
      LicenceType.AP,
      LicenceCreationType.LICENCE_NOT_STARTED,
      expectedReleaseDate = tenDaysFromNow,
    )
  }

  @Test
  fun `it filters offenders who are ineligible for a licence`() {
    val managedOffenders = listOf(
      ManagedOffenderCrn(crn = "X12348", nomisId = "AB1234E"),
      ManagedOffenderCrn(crn = "X12349", nomisId = "AB1234F"),
      ManagedOffenderCrn(crn = "X12350", nomisId = "AB1234G"),
      ManagedOffenderCrn(crn = "X12351", nomisId = "AB1234L"),
      ManagedOffenderCrn(crn = "X12352", nomisId = "AB1234M"),
      ManagedOffenderCrn(crn = "X12353", nomisId = "AB1234N"),
      ManagedOffenderCrn(crn = "X12354", nomisId = "AB1234P"),
      ManagedOffenderCrn(crn = "X12355", nomisId = "AB1234Q"),
      ManagedOffenderCrn(crn = "X12356", nomisId = "AB1234R"),
    )

    whenever(
      deliusApiClient.getManagedOffenders(deliusStaffIdentifier),
    ).thenReturn(managedOffenders)

    val caseloadItems = listOf(
      prisonerSearchResult().copy(
        prisonerNumber = "AB1234E",
        conditionalReleaseDate = nineDaysFromNow,
        paroleEligibilityDate = yesterday,
        bookingId = "1",
        firstName = "ABC DEF",
      ),
      prisonerSearchResult().copy(
        prisonerNumber = "AB1234F",
        conditionalReleaseDate = tenDaysFromNow,
        paroleEligibilityDate = tenDaysFromNow,
        bookingId = "2",
      ),
      prisonerSearchResult().copy(
        prisonerNumber = "AB1234G",
        conditionalReleaseDate = tenDaysFromNow,
        legalStatus = "DEAD",
        bookingId = "3",
      ),
      prisonerSearchResult().copy(
        prisonerNumber = "AB1234H",
        conditionalReleaseDate = tenDaysFromNow,
        indeterminateSentence = true,
        bookingId = "4",
      ),
      prisonerSearchResult().copy(prisonerNumber = "AB1234I", conditionalReleaseDate = tenDaysFromNow, bookingId = "5"),
      prisonerSearchResult().copy(prisonerNumber = "AB1234J", conditionalReleaseDate = tenDaysFromNow, bookingId = "6"),
      prisonerSearchResult().copy(prisonerNumber = "AB1234K", conditionalReleaseDate = tenDaysFromNow, bookingId = "123"),
      prisonerSearchResult().copy(
        prisonerNumber = "AB1234L",
        conditionalReleaseDate = nineDaysFromNow,
        bookingId = "123",
        firstName = "ABC XYZ",
      ),
      // This case tests that recalls are overridden if the PRRD < the conditionalReleaseDate - so NOT_STARTED
      prisonerSearchResult().copy(
        prisonerNumber = "AB1234M",
        conditionalReleaseDate = tenDaysFromNow,
        postRecallReleaseDate = nineDaysFromNow,
        recall = true,
        bookingId = "7",
      ),
      prisonerSearchResult().copy(
        prisonerNumber = "AB1234N",
        conditionalReleaseDate = tenDaysFromNow,
        postRecallReleaseDate = elevenDaysFromNow,
        recall = true,
        bookingId = "8",
      ),
      // This case tests that recalls are overridden if the PRRD is equal to the conditionalReleaseDate - so NOT_STARTED
      prisonerSearchResult().copy(
        prisonerNumber = "AB1234P",
        conditionalReleaseDate = nineDaysFromNow,
        postRecallReleaseDate = nineDaysFromNow,
        recall = true,
        bookingId = "9",
        firstName = "G",
      ),
      // This case tests that recalls are overridden if no PRRD exists and there is only the conditionalReleaseDate - so NOT_STARTED
      prisonerSearchResult().copy(
        prisonerNumber = "AB1234Q",
        conditionalReleaseDate = nineDaysFromNow,
        recall = true,
        bookingId = "10",
        firstName = "P",
      ),
      // This case tests that the case is included when the status is INACTIVE TRN
      prisonerSearchResult().copy(
        prisonerNumber = "AB1234R",
        conditionalReleaseDate = nineDaysFromNow,
        status = "INACTIVE TRN",
        bookingId = "11",
        firstName = "S",
      ),
    )

    whenever(caseloadService.getPrisonersByNumber(any())).thenReturn(caseloadItems)

    whenever(cvlRecordService.getCvlRecords(any(), any())).thenReturn(
      listOf(
        aCvlRecord(nomsId = "AB1234E", kind = LicenceKind.CRD, licenceStartDate = nineDaysFromNow),
        aCvlRecord(nomsId = "AB1234F", kind = LicenceKind.CRD, licenceStartDate = tenDaysFromNow).copy(isEligible = false),
        aCvlRecord(nomsId = "AB1234G", kind = LicenceKind.CRD, licenceStartDate = tenDaysFromNow).copy(isEligible = false),
        aCvlRecord(nomsId = "AB1234H", kind = LicenceKind.CRD, licenceStartDate = tenDaysFromNow),
        aCvlRecord(nomsId = "AB1234I", kind = LicenceKind.CRD, licenceStartDate = tenDaysFromNow),
        aCvlRecord(nomsId = "AB1234J", kind = LicenceKind.CRD, licenceStartDate = tenDaysFromNow),
        aCvlRecord(nomsId = "AB1234K", kind = LicenceKind.CRD, licenceStartDate = tenDaysFromNow),
        aCvlRecord(nomsId = "AB1234L", kind = LicenceKind.CRD, licenceStartDate = nineDaysFromNow),
        aCvlRecord(nomsId = "AB1234M", kind = LicenceKind.CRD, licenceStartDate = tenDaysFromNow),
        aCvlRecord(nomsId = "AB1234N", kind = LicenceKind.CRD, licenceStartDate = tenDaysFromNow).copy(isEligible = false),
        aCvlRecord(nomsId = "AB1234P", kind = LicenceKind.CRD, licenceStartDate = nineDaysFromNow),
        aCvlRecord(nomsId = "AB1234Q", kind = LicenceKind.CRD, licenceStartDate = nineDaysFromNow),
        aCvlRecord(nomsId = "AB1234R", kind = LicenceKind.CRD, licenceStartDate = nineDaysFromNow),
      ),
    )

    val caseload = service.getStaffCreateCaseload(deliusStaffIdentifier)

    assertThat(caseload).hasSize(6)
    verifyCase(
      case = caseload[0],
      expectedCrn = "X12348",
      expectedPrisonerNumber = "AB1234E",
      expectedLicenceStatus = LicenceStatus.NOT_STARTED,
      expectedLicenceType = LicenceType.AP,
      expectedReleaseDate = nineDaysFromNow,
      expectedLicenceCreationType = LicenceCreationType.LICENCE_NOT_STARTED,
    )
    verifyCase(
      case = caseload[1],
      expectedCrn = "X12351",
      expectedPrisonerNumber = "AB1234L",
      expectedLicenceStatus = LicenceStatus.NOT_STARTED,
      expectedLicenceType = LicenceType.AP,
      expectedReleaseDate = nineDaysFromNow,
      expectedLicenceCreationType = LicenceCreationType.LICENCE_NOT_STARTED,
    )
    verifyCase(
      case = caseload[2],
      expectedCrn = "X12354",
      expectedPrisonerNumber = "AB1234P",
      expectedLicenceStatus = LicenceStatus.NOT_STARTED,
      expectedLicenceType = LicenceType.AP,
      expectedReleaseDate = nineDaysFromNow,
      expectedLicenceCreationType = LicenceCreationType.LICENCE_NOT_STARTED,
    )
    verifyCase(
      case = caseload[3],
      expectedCrn = "X12355",
      expectedPrisonerNumber = "AB1234Q",
      expectedLicenceStatus = LicenceStatus.NOT_STARTED,
      expectedLicenceType = LicenceType.AP,
      expectedReleaseDate = nineDaysFromNow,
      expectedLicenceCreationType = LicenceCreationType.LICENCE_NOT_STARTED,
    )
    verifyCase(
      case = caseload[4],
      expectedCrn = "X12356",
      expectedPrisonerNumber = "AB1234R",
      expectedLicenceStatus = LicenceStatus.NOT_STARTED,
      expectedLicenceType = LicenceType.AP,
      expectedReleaseDate = nineDaysFromNow,
      expectedLicenceCreationType = LicenceCreationType.LICENCE_NOT_STARTED,
    )
    verifyCase(
      case = caseload[5],
      expectedCrn = "X12352",
      expectedPrisonerNumber = "AB1234M",
      expectedLicenceStatus = LicenceStatus.NOT_STARTED,
      expectedLicenceType = LicenceType.AP,
      expectedReleaseDate = tenDaysFromNow,
      expectedLicenceCreationType = LicenceCreationType.LICENCE_NOT_STARTED,
    )
  }

  // isn't working properly
  @Test
  fun `it filters out cases passed LSD`() {
    val managedOffenders = listOf(
      ManagedOffenderCrn(
        crn = "X12348",
        nomisId = "AB1234E",
        staff = StaffDetail(name = Name(forename = "John", surname = "Doe"), code = "X54321"),
      ),
      ManagedOffenderCrn(
        crn = "X12349",
        nomisId = "AB1234F",
        staff = StaffDetail(name = Name(forename = "John", surname = "Doe"), code = "X54321"),
      ),
    )

    whenever(
      deliusApiClient.getManagedOffenders(deliusStaffIdentifier),
    ).thenReturn(managedOffenders)

    whenever(caseloadService.getPrisonersByNumber(any())).thenReturn(
      listOf(
        prisonerSearchResult().copy(
          bookingId = "1",
          prisonerNumber = "AB1234E",
          conditionalReleaseDate = LocalDate.now().minusDays(2),
          releaseDate = LocalDate.now().minusDays(2),
          licenceExpiryDate = LocalDate.of(
            2022,
            Month.DECEMBER,
            26,
          ),
        ),
        prisonerSearchResult().copy(
          bookingId = "2",
          prisonerNumber = "AB1234F",
          conditionalReleaseDate = tenDaysFromNow,
          releaseDate = tenDaysFromNow,
          licenceExpiryDate = LocalDate.of(
            2022,
            Month.DECEMBER,
            26,
          ),
        ),
      ),
    )

    whenever(licenceCaseRepository.findLicenceCasesForCom(any(), any())).thenReturn(
      listOf(
        createLicenceSummary(
          crn = "X12348",
          nomisId = "AB1234E",
          kind = LicenceKind.CRD,
          typeCode = LicenceType.AP_PSS,
          licenceStatus = LicenceStatus.SUBMITTED,
          comUsername = "johndoe",
          licenceStartDate = LocalDate.now().minusDays(2),
        ),
        createLicenceSummary(
          crn = "X12349",
          nomisId = "AB1234F",
          kind = LicenceKind.CRD,
          typeCode = LicenceType.AP_PSS,
          licenceStatus = LicenceStatus.SUBMITTED,
          comUsername = "johndoe",
          licenceStartDate = tenDaysFromNow,
        ),
      ),
    )
    whenever(cvlRecordService.getCvlRecords(any(), any())).thenReturn(
      listOf(
        aCvlRecord(nomsId = "AB1234E", kind = LicenceKind.CRD, licenceStartDate = LocalDate.now().minusDays(2)),
        aCvlRecord(nomsId = "AB1234F", kind = LicenceKind.CRD, licenceStartDate = tenDaysFromNow),
      ),
    )

    whenever(deliusApiClient.getStaffDetailsByUsername(any())).thenReturn(
      listOf(
        StaffNameResponse(
          username = "johndoe",
          code = "X54321",
          name = Name(forename = "John", surname = "Doe"),
          id = Long.MIN_VALUE,
        ),
      ),
    )

    val caseload = service.getStaffCreateCaseload(deliusStaffIdentifier)
    assertThat(caseload).hasSize(1)
    verifyCase(
      case = caseload[0],
      expectedCrn = "X12349",
      expectedPrisonerNumber = "AB1234F",
      expectedLicenceStatus = LicenceStatus.SUBMITTED,
      expectedLicenceType = LicenceType.AP_PSS,
      expectedReleaseDate = tenDaysFromNow,
      expectedProbationPractitioner = ProbationPractitioner(staffCode = "X54321", name = "John Doe"),
      expectedLicenceCreationType = LicenceCreationType.LICENCE_IN_PROGRESS,

    )
  }

  @Test
  fun `it builds the staff create caseload`() {
    val managedOffenders = listOf(
      ManagedOffenderCrn(
        crn = "X12348",
        nomisId = "AB1234E",
        staff = StaffDetail(name = Name(forename = "Joe", surname = "Bloggs"), code = "X1234"),
      ),
      ManagedOffenderCrn(
        crn = "X12349",
        nomisId = "AB1234F",
        staff = StaffDetail(name = Name(forename = "Joe", surname = "Bloggs"), code = "X1234"),
      ),
      ManagedOffenderCrn(
        crn = "X12350",
        nomisId = "AB1234G",
        staff = StaffDetail(name = Name(forename = "Joe", surname = "Bloggs"), code = "X1234"),
      ),
      ManagedOffenderCrn(
        crn = "X12351",
        nomisId = "AB1234H",
        staff = StaffDetail(name = Name(forename = null, surname = null), code = "X1234", unallocated = true),
      ),
      ManagedOffenderCrn(
        crn = "X12352",
        nomisId = "AB1234I",
        staff = StaffDetail(name = Name(forename = "Joe", surname = "Bloggs"), code = "X1234"),
      ),
    )

    whenever(
      deliusApiClient.getManagedOffenders(deliusStaffIdentifier),
    ).thenReturn(managedOffenders)

    val case1 = prisonerSearchResult().copy(
      bookingId = "1",
      prisonerNumber = "AB1234E",
      conditionalReleaseDate = tenDaysFromNow,
      releaseDate = tenDaysFromNow,
      licenceExpiryDate = LocalDate.of(
        2022,
        Month.DECEMBER,
        26,
      ),
    )
    val case2 = prisonerSearchResult().copy(
      bookingId = "2",
      prisonerNumber = "AB1234F",
      conditionalReleaseDate = tenDaysFromNow,
      releaseDate = tenDaysFromNow,
      status = "INACTIVE OUT",
    )
    val case3 = prisonerSearchResult().copy(
      bookingId = "3",
      prisonerNumber = "AB1234G",
      conditionalReleaseDate = tenDaysFromNow,
      releaseDate = tenDaysFromNow,
      status = "INACTIVE OUT",
    )
    val case4 = prisonerSearchResult().copy(
      bookingId = "4",
      prisonerNumber = "AB1234H",
      conditionalReleaseDate = tenDaysFromNow,
      releaseDate = tenDaysFromNow,
      licenceExpiryDate = null,
      topupSupervisionExpiryDate = LocalDate.of(2023, Month.JUNE, 22),
    )
    val case5 = prisonerSearchResult().copy(
      bookingId = "5",
      prisonerNumber = "AB1234I",
      conditionalReleaseDate = elevenDaysFromNow,
      releaseDate = elevenDaysFromNow,
      topupSupervisionExpiryDate = LocalDate.of(2023, Month.JUNE, 22),
      licenceExpiryDate = elevenDaysFromNow,
    )

    val caseloadItems = listOf(
      case1,
      case2,
      case3,
      case4,
      case5,
    )
    whenever(caseloadService.getPrisonersByNumber(any())).thenReturn(caseloadItems)

    whenever(cvlRecordService.getCvlRecords(any(), any())).thenReturn(
      listOf(
        aCvlRecord(nomsId = "AB1234E", kind = LicenceKind.CRD, licenceStartDate = tenDaysFromNow),
        aCvlRecord(nomsId = "AB1234F", kind = LicenceKind.CRD, licenceStartDate = tenDaysFromNow).copy(isEligible = false),
        aCvlRecord(nomsId = "AB1234G", kind = LicenceKind.CRD, licenceStartDate = tenDaysFromNow).copy(isEligible = false),
        aCvlRecord(nomsId = "AB1234H", kind = LicenceKind.CRD, licenceStartDate = tenDaysFromNow),
        aCvlRecord(nomsId = "AB1234I", kind = LicenceKind.CRD, licenceStartDate = elevenDaysFromNow),
      ),
    )

    whenever(licenceCaseRepository.findLicenceCasesForCom(any(), any())).thenReturn(
      listOf(
        createLicenceSummary(
          crn = "X12352",
          nomisId = "AB1234I",
          kind = LicenceKind.CRD,
          typeCode = LicenceType.AP_PSS,
          licenceStatus = LicenceStatus.SUBMITTED,
          comUsername = "johndoe",
          licenceStartDate = elevenDaysFromNow,
        ),
      ),
    )

    whenever(deliusApiClient.getStaffDetailsByUsername(any())).thenReturn(
      listOf(
        StaffNameResponse(
          username = "johndoe",
          code = "X54321",
          name = Name(forename = "John", surname = "Doe"),
          id = Long.MIN_VALUE,
        ),
      ),
    )

    val caseload = service.getStaffCreateCaseload(deliusStaffIdentifier)
    assertThat(caseload).hasSize(3)
    verifyCase(
      case = caseload[0],
      expectedCrn = "X12348",
      expectedPrisonerNumber = "AB1234E",
      expectedLicenceStatus = LicenceStatus.NOT_STARTED,
      expectedLicenceType = LicenceType.AP,
      expectedReleaseDate = tenDaysFromNow,
      expectedProbationPractitioner = ProbationPractitioner(staffCode = "X1234", name = "Joe Bloggs"),
      expectedLicenceCreationType = LicenceCreationType.LICENCE_NOT_STARTED,
    )
    verifyCase(
      case = caseload[1],
      expectedCrn = "X12351",
      expectedPrisonerNumber = "AB1234H",
      expectedLicenceStatus = LicenceStatus.NOT_STARTED,
      expectedLicenceType = LicenceType.PSS,
      expectedReleaseDate = tenDaysFromNow,
      expectedLicenceCreationType = LicenceCreationType.LICENCE_NOT_STARTED,
    )
    verifyCase(
      case = caseload[2],
      expectedCrn = "X12352",
      expectedPrisonerNumber = "AB1234I",
      expectedLicenceStatus = LicenceStatus.SUBMITTED,
      expectedLicenceType = LicenceType.AP_PSS,
      expectedReleaseDate = elevenDaysFromNow,
      expectedProbationPractitioner = ProbationPractitioner(
        staffCode = "X54321",
        name = "John Doe",
      ),
      expectedLicenceCreationType = LicenceCreationType.LICENCE_IN_PROGRESS,
    )
  }

  @Test
  fun `it builds the team create caseload`() {
    val selectedTeam = "team c"

    val managedOffenders = listOf(
      ManagedOffenderCrn(
        crn = "X12348",
        nomisId = "AB1234E",
        staff = StaffDetail(name = Name(forename = "Joe", surname = "Bloggs"), code = "X1234"),
      ),
      ManagedOffenderCrn(
        crn = "X12349",
        nomisId = "AB1234F",
        staff = StaffDetail(name = Name(forename = "John", surname = "Doe"), code = "X54321"),
      ),
    )

    whenever(
      deliusApiClient.getManagedOffendersByTeam(selectedTeam),
    ).thenReturn(managedOffenders)

    whenever(caseloadService.getPrisonersByNumber(any())).thenReturn(
      listOf(
        prisonerSearchResult().copy(
          bookingId = "1",
          prisonerNumber = "AB1234E",
          conditionalReleaseDate = tenDaysFromNow,
          status = "ACTIVE IN",
        ),
        prisonerSearchResult().copy(
          bookingId = "2",
          prisonerNumber = "AB1234F",
          conditionalReleaseDate = tenDaysFromNow,
          licenceExpiryDate = null,
          topupSupervisionExpiryDate = LocalDate.of(2022, Month.DECEMBER, 26),
        ),
      ),
    )

    whenever(cvlRecordService.getCvlRecords(any(), any())).thenReturn(
      listOf(
        aCvlRecord(nomsId = "AB1234E", kind = LicenceKind.CRD, licenceStartDate = tenDaysFromNow),
        aCvlRecord(nomsId = "AB1234F", kind = LicenceKind.CRD, licenceStartDate = tenDaysFromNow),
      ),
    )

    val caseload = service.getTeamCreateCaseload(listOf("team A", "team B"), listOf(selectedTeam))

    verify(deliusApiClient).getManagedOffendersByTeam(selectedTeam)
    assertThat(caseload).hasSize(2)
    verifyCase(
      case = caseload[0],
      expectedCrn = "X12348",
      expectedPrisonerNumber = "AB1234E",
      expectedLicenceStatus = LicenceStatus.NOT_STARTED,
      expectedLicenceType = LicenceType.AP,
      expectedReleaseDate = tenDaysFromNow,
      expectedProbationPractitioner = ProbationPractitioner(
        staffCode = "X1234",
        name = "Joe Bloggs",
      ),
      expectedLicenceCreationType = LicenceCreationType.LICENCE_NOT_STARTED,
    )

    verifyCase(
      case = caseload[1],
      expectedCrn = "X12349",
      expectedPrisonerNumber = "AB1234F",
      expectedLicenceStatus = LicenceStatus.NOT_STARTED,
      expectedLicenceType = LicenceType.PSS,
      expectedReleaseDate = tenDaysFromNow,
      expectedProbationPractitioner = ProbationPractitioner(
        staffCode = "X54321",
        name = "John Doe",
      ),
      expectedLicenceCreationType = LicenceCreationType.LICENCE_NOT_STARTED,
    )
  }

  @Test
  fun `PRRD licences will be mapped to offenders and caseloads for team will be created`() {
    // Given
    val selectedTeam = "team c"
    val prisonerNumber = "AB1234E"
    val managedOffender = aManagedOffenderCrn(prisonerNumber)
    val managedOffenders = listOf(managedOffender)
    val caseLoadItem = prisonerSearchResult().copy(
      bookingId = "1",
      prisonerNumber = prisonerNumber,
      conditionalReleaseDate = null,
      postRecallReleaseDate = LocalDate.now(),
    )

    whenever(deliusApiClient.getManagedOffendersByTeam(selectedTeam)).thenReturn(managedOffenders)
    whenever(caseloadService.getPrisonersByNumber(any())).thenReturn(listOf(caseLoadItem))
    whenever(cvlRecordService.getCvlRecords(any(), any())).thenReturn(listOf(aCvlRecord(nomsId = prisonerNumber, kind = LicenceKind.PRRD, licenceStartDate = LocalDate.now())))

    // When
    val caseload = service.getTeamCreateCaseload(listOf(), listOf(selectedTeam))

    // Then
    assertThat(caseload).hasSize(1)
    assertThat(caseload.first().kind).isEqualTo(LicenceKind.PRRD)
  }

  @Test
  fun `PRRD licences will be mapped to offenders and caseloads for staff will be created`() {
    // Given
    val prisonerNumber = "AB1234E"
    val managedOffender = aManagedOffenderCrn(prisonerNumber)
    val managedOffenders = listOf(managedOffender)
    val caseLoadItem = prisonerSearchResult().copy(
      bookingId = "1",
      prisonerNumber = prisonerNumber,
      conditionalReleaseDate = null,
      postRecallReleaseDate = LocalDate.now(),
    )

    whenever(deliusApiClient.getManagedOffenders(deliusStaffIdentifier)).thenReturn(managedOffenders)
    whenever(caseloadService.getPrisonersByNumber(any())).thenReturn(listOf(caseLoadItem))
    whenever(cvlRecordService.getCvlRecords(any(), any())).thenReturn(listOf(aCvlRecord(nomsId = prisonerNumber, kind = LicenceKind.PRRD, licenceStartDate = LocalDate.now())))

    // When
    val caseload = service.getStaffCreateCaseload(deliusStaffIdentifier)

    // Then
    assertThat(caseload).hasSize(1)
    assertThat(caseload.first().kind).isEqualTo(LicenceKind.PRRD)
  }

  @Test
  fun `it filters out HDC approved licences on team create caseload`() {
    val selectedTeam = "team C"

    val managedOffenders = listOf(
      ManagedOffenderCrn(
        crn = "X12348",
        nomisId = "AB1234E",
        staff = StaffDetail(name = Name(forename = "Joe", surname = "Bloggs"), code = "X1234"),
      ),
      ManagedOffenderCrn(
        crn = "X12349",
        nomisId = "AB1234F",
        staff = StaffDetail(name = Name(forename = "John", surname = "Doe"), code = "X54321"),
      ),
    )

    whenever(
      deliusApiClient.getManagedOffendersByTeam(selectedTeam),
    ).thenReturn(managedOffenders)

    val caseloadItems = listOf(
      prisonerSearchResult().copy(
        bookingId = "1",
        prisonerNumber = "AB1234E",
        conditionalReleaseDate = tenDaysFromNow,
        releaseDate = tenDaysFromNow,
        postRecallReleaseDate = tenDaysFromNow,
        recall = true,
      ),
    )
    whenever(cvlRecordService.getCvlRecords(any(), any())).thenReturn(
      listOf(
        aCvlRecord(nomsId = "AB1234E", kind = LicenceKind.CRD, licenceStartDate = tenDaysFromNow),
      ),
    )
    whenever(caseloadService.getPrisonersByNumber(any())).thenReturn(caseloadItems)
    whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(setOf(caseloadItems[0].bookingId?.toLong()!!)))

    val caseload = service.getTeamCreateCaseload(listOf("team A", "team B"), listOf("team C"))

    verify(deliusApiClient).getManagedOffendersByTeam("team C")
    assertThat(caseload).isEmpty()
  }

  @Test
  fun `it filters out NOT_STARTED PRRD licences`() {
    whenever(
      deliusApiClient.getManagedOffenders(deliusStaffIdentifier),
    ).thenReturn(listOf(ManagedOffenderCrn(crn = "X12348", nomisId = "AB1234E")))

    whenever(caseloadService.getPrisonersByNumber(any())).thenReturn(
      listOf(
        prisonerSearchResult().copy(
          prisonerNumber = "AB1234E",
          conditionalReleaseDate = yesterday,
          bookingId = "5",
          postRecallReleaseDate = tenDaysFromNow,
        ),
      ),
    )

    whenever(cvlRecordService.getCvlRecords(any(), any())).thenReturn(listOf(aCvlRecord(nomsId = "AB1234E", kind = LicenceKind.CRD, licenceStartDate = tenDaysFromNow).copy(isEligible = false)))

    val caseload = service.getStaffCreateCaseload(deliusStaffIdentifier)

    assertThat(caseload).hasSize(0)
  }

  @Test
  fun `it selects a licence edit over the approved licence`() {
    val managedOffenders = listOf(
      ManagedOffenderCrn(crn = "X12348", nomisId = "AB1234E"),
    )
    whenever(deliusApiClient.getManagedOffenders(deliusStaffIdentifier)).thenReturn(managedOffenders)
    whenever(caseloadService.getPrisonersByNumber(any())).thenReturn(
      listOf(
        prisonerSearchResult().copy(prisonerNumber = "AB1234E", conditionalReleaseDate = tenDaysFromNow, bookingId = "1"),
      ),
    )

    whenever(licenceCaseRepository.findLicenceCasesForCom(any(), any())).thenReturn(
      listOf(
        createLicenceSummary(
          crn = "X12348",
          nomisId = "AB1234E",
          kind = LicenceKind.CRD,
          typeCode = LicenceType.AP_PSS,
          licenceStatus = LicenceStatus.APPROVED,
          comUsername = "johndoe",
          licenceStartDate = tenDaysFromNow,
        ),
        createLicenceSummary(
          crn = "X12348",
          nomisId = "AB1234E",
          kind = LicenceKind.CRD,
          typeCode = LicenceType.AP_PSS,
          licenceStatus = LicenceStatus.IN_PROGRESS,
          comUsername = "johndoe",
          licenceStartDate = tenDaysFromNow,
        ),
      ),
    )
    whenever(cvlRecordService.getCvlRecords(any(), any())).thenReturn(listOf(aCvlRecord(nomsId = "AB1234E", kind = LicenceKind.CRD, licenceStartDate = tenDaysFromNow)))

    val caseload = service.getStaffCreateCaseload(deliusStaffIdentifier)

    assertThat(caseload).hasSize(1)
    verifyCase(
      caseload.first(),
      "X12348",
      "AB1234E",
      LicenceStatus.IN_PROGRESS,
      LicenceType.AP_PSS,
      LicenceCreationType.LICENCE_IN_PROGRESS,
      expectedReleaseDate = tenDaysFromNow,
    )
  }

  @Test
  fun `it selects a hard stop licence over timed out licence`() {
    val managedOffenders = listOf(
      ManagedOffenderCrn(crn = "X12348", nomisId = "AB1234E"),
    )
    whenever(deliusApiClient.getManagedOffenders(deliusStaffIdentifier)).thenReturn(managedOffenders)
    whenever(caseloadService.getPrisonersByNumber(any())).thenReturn(
      listOf(
        prisonerSearchResult().copy(prisonerNumber = "AB1234E", conditionalReleaseDate = twoDaysFromNow, bookingId = "1"),
      ),
    )

    whenever(licenceCaseRepository.findLicenceCasesForCom(any(), any())).thenReturn(
      listOf(
        createLicenceSummary(
          crn = "X12348",
          nomisId = "AB1234E",
          kind = LicenceKind.CRD,
          typeCode = LicenceType.AP_PSS,
          licenceStatus = LicenceStatus.TIMED_OUT,
          comUsername = "johndoe",
          licenceStartDate = twoDaysFromNow,
        ),
        createLicenceSummary(
          crn = "X12348",
          nomisId = "AB1234E",
          kind = LicenceKind.HARD_STOP,
          typeCode = LicenceType.AP_PSS,
          licenceStatus = LicenceStatus.IN_PROGRESS,
          comUsername = "johndoe",
          licenceStartDate = twoDaysFromNow,
        ),
      ),
    )
    whenever(cvlRecordService.getCvlRecords(any(), any())).thenReturn(listOf(aCvlRecord(nomsId = "AB1234E", kind = LicenceKind.CRD, licenceStartDate = twoDaysFromNow)))

    val caseload = service.getStaffCreateCaseload(deliusStaffIdentifier)

    assertThat(caseload).hasSize(1)
    verifyCase(
      caseload.first(),
      "X12348",
      "AB1234E",
      LicenceStatus.TIMED_OUT,
      LicenceType.AP_PSS,
      LicenceCreationType.PRISON_WILL_CREATE_THIS_LICENCE,
      expectedReleaseDate = twoDaysFromNow,
      expectedLicenceKind = LicenceKind.HARD_STOP,
    )
  }

  @Test
  fun `it sets LicenceCreationType to PRISON_WILL_CREATE_THIS_LICENCE if the hard stop licence has not been started`() {
    val managedOffenders = listOf(
      ManagedOffenderCrn(crn = "X12348", nomisId = "AB1234E"),
    )
    whenever(deliusApiClient.getManagedOffenders(deliusStaffIdentifier)).thenReturn(managedOffenders)
    whenever(caseloadService.getPrisonersByNumber(any())).thenReturn(
      listOf(
        prisonerSearchResult().copy(prisonerNumber = "AB1234E", conditionalReleaseDate = twoDaysFromNow, bookingId = "1"),
      ),
    )

    whenever(licenceCaseRepository.findLicenceCasesForCom(any(), any())).thenReturn(
      listOf(
        createLicenceSummary(
          crn = "X12348",
          nomisId = "AB1234E",
          kind = LicenceKind.CRD,
          typeCode = LicenceType.AP_PSS,
          licenceStatus = LicenceStatus.TIMED_OUT,
          comUsername = "johndoe",
          licenceStartDate = twoDaysFromNow,
        ),
      ),
    )
    whenever(cvlRecordService.getCvlRecords(any(), any())).thenReturn(listOf(aCvlRecord(nomsId = "AB1234E", kind = LicenceKind.CRD, licenceStartDate = twoDaysFromNow)))

    val caseload = service.getStaffCreateCaseload(deliusStaffIdentifier)

    assertThat(caseload).hasSize(1)
    verifyCase(
      caseload.first(),
      "X12348",
      "AB1234E",
      LicenceStatus.TIMED_OUT,
      LicenceType.AP_PSS,
      LicenceCreationType.PRISON_WILL_CREATE_THIS_LICENCE,
      expectedReleaseDate = twoDaysFromNow,
      expectedLicenceKind = LicenceKind.CRD,
    )
  }

  @Test
  fun `it sets LicenceCreationType to PRISON_WILL_CREATE_THIS_LICENCE if no licence has been started in the hard stop period`() {
    val managedOffenders = listOf(
      ManagedOffenderCrn(crn = "X12348", nomisId = "AB1234E"),
    )

    whenever(deliusApiClient.getManagedOffenders(deliusStaffIdentifier)).thenReturn(managedOffenders)
    whenever(caseloadService.getPrisonersByNumber(any())).thenReturn(
      listOf(
        prisonerSearchResult().copy(
          prisonerNumber = "AB1234E",
          conditionalReleaseDate = twoDaysFromNow,
          bookingId = "1",
          licenceExpiryDate = tenDaysFromNow,
          topupSupervisionExpiryDate = elevenDaysFromNow,
        ),
      ),
    )

    whenever(licenceCaseRepository.findLicenceCasesForCom(any(), any())).thenReturn(
      emptyList(),
    )
    whenever(cvlRecordService.getCvlRecords(any(), any())).thenReturn(listOf(aCvlRecord(nomsId = "AB1234E", kind = LicenceKind.CRD, licenceStartDate = twoDaysFromNow)))

    whenever(releaseDateService.isInHardStopPeriod(any(), anyOrNull())).thenReturn(true)

    val caseload = service.getStaffCreateCaseload(deliusStaffIdentifier)

    assertThat(caseload).hasSize(1)
    verifyCase(
      caseload.first(),
      "X12348",
      "AB1234E",
      LicenceStatus.TIMED_OUT,
      LicenceType.AP_PSS,
      LicenceCreationType.PRISON_WILL_CREATE_THIS_LICENCE,
      expectedReleaseDate = twoDaysFromNow,
      expectedLicenceKind = LicenceKind.CRD,
    )
  }

  @Test
  fun `it sets LicenceCreationType to LICENCE_CREATED_BY_PRISON if the hard stop licence has been submitted`() {
    val managedOffenders = listOf(
      ManagedOffenderCrn(crn = "X12348", nomisId = "AB1234E"),
    )
    whenever(deliusApiClient.getManagedOffenders(deliusStaffIdentifier)).thenReturn(managedOffenders)
    whenever(caseloadService.getPrisonersByNumber(any())).thenReturn(
      listOf(
        prisonerSearchResult().copy(prisonerNumber = "AB1234E", conditionalReleaseDate = twoDaysFromNow, bookingId = "1"),
      ),
    )

    whenever(licenceCaseRepository.findLicenceCasesForCom(any(), any())).thenReturn(
      listOf(
        createLicenceSummary(
          crn = "X12348",
          nomisId = "AB1234E",
          kind = LicenceKind.HARD_STOP,
          typeCode = LicenceType.AP_PSS,
          licenceStatus = LicenceStatus.SUBMITTED,
          comUsername = "johndoe",
          licenceStartDate = twoDaysFromNow,
        ),
      ),
    )
    whenever(cvlRecordService.getCvlRecords(any(), any())).thenReturn(listOf(aCvlRecord(nomsId = "AB1234E", kind = LicenceKind.CRD, licenceStartDate = twoDaysFromNow)))

    val caseload = service.getStaffCreateCaseload(deliusStaffIdentifier)

    assertThat(caseload).hasSize(1)
    verifyCase(
      caseload.first(),
      "X12348",
      "AB1234E",
      LicenceStatus.TIMED_OUT,
      LicenceType.AP_PSS,
      LicenceCreationType.LICENCE_CREATED_BY_PRISON,
      expectedReleaseDate = twoDaysFromNow,
      expectedLicenceKind = LicenceKind.HARD_STOP,
    )
  }

  @Test
  fun `it sets LicenceCreationType to LICENCE_CHANGES_NOT_APPROVED_IN_TIME if an edit times out`() {
    val managedOffenders = listOf(
      ManagedOffenderCrn(crn = "X12348", nomisId = "AB1234E"),
    )
    whenever(deliusApiClient.getManagedOffenders(deliusStaffIdentifier)).thenReturn(managedOffenders)
    whenever(caseloadService.getPrisonersByNumber(any())).thenReturn(
      listOf(
        prisonerSearchResult().copy(prisonerNumber = "AB1234E", conditionalReleaseDate = twoDaysFromNow, bookingId = "1"),
      ),
    )

    whenever(licenceCaseRepository.findLicenceCasesForCom(any(), any())).thenReturn(
      listOf(
        createLicenceSummary(
          crn = "X12348",
          nomisId = "AB1234E",
          kind = LicenceKind.CRD,
          typeCode = LicenceType.AP_PSS,
          licenceStatus = LicenceStatus.APPROVED,
          comUsername = "johndoe",
          licenceStartDate = twoDaysFromNow,
        ),
        createLicenceSummary(
          crn = "X12348",
          nomisId = "AB1234E",
          kind = LicenceKind.CRD,
          typeCode = LicenceType.AP_PSS,
          licenceStatus = LicenceStatus.TIMED_OUT,
          comUsername = "johndoe",
          licenceStartDate = twoDaysFromNow,
          versionOfId = 1,
        ),
      ),
    )
    whenever(cvlRecordService.getCvlRecords(any(), any())).thenReturn(listOf(aCvlRecord(nomsId = "AB1234E", kind = LicenceKind.CRD, licenceStartDate = twoDaysFromNow)))

    val caseload = service.getStaffCreateCaseload(deliusStaffIdentifier)

    assertThat(caseload).hasSize(1)
    verifyCase(
      caseload.first(),
      "X12348",
      "AB1234E",
      LicenceStatus.TIMED_OUT,
      LicenceType.AP_PSS,
      LicenceCreationType.LICENCE_CHANGES_NOT_APPROVED_IN_TIME,
      expectedReleaseDate = twoDaysFromNow,
    )
  }

  private fun verifyCase(
    case: ComCase,
    expectedCrn: String,
    expectedPrisonerNumber: String,
    expectedLicenceStatus: LicenceStatus,
    expectedLicenceType: LicenceType,
    expectedLicenceCreationType: LicenceCreationType,
    expectedReleaseDate: LocalDate? = null,
    expectedProbationPractitioner: ProbationPractitioner? = null,
    expectedReviewNeeded: Boolean = false,
    expectedLicenceKind: LicenceKind = LicenceKind.CRD,
  ) {
    with(case) {
      assertThat(crnNumber).isEqualTo(expectedCrn)
      assertThat(prisonerNumber).isEqualTo(expectedPrisonerNumber)
      assertThat(licenceStatus).isEqualTo(expectedLicenceStatus)
      assertThat(licenceType).isEqualTo(expectedLicenceType)
      expectedReleaseDate.let { assertThat(releaseDate).isEqualTo(expectedReleaseDate) }
      expectedProbationPractitioner.let { assertThat(probationPractitioner).isEqualTo(expectedProbationPractitioner) }
      assertThat(isReviewNeeded).isEqualTo(expectedReviewNeeded)
      assertThat(kind).isEqualTo(expectedLicenceKind)
      assertThat(licenceCreationType).isEqualTo(expectedLicenceCreationType)
    }
  }

  private fun createLicenceSummary(
    crn: String,
    nomisId: String,
    typeCode: LicenceType,
    licenceStatus: LicenceStatus,
    kind: LicenceKind = LicenceKind.CRD,
    comUsername: String? = null,
    conditionalReleaseDate: LocalDate? = null,
    confirmedReleaseDate: LocalDate? = null,
    licenceStartDate: LocalDate? = null,
    versionOfId: Long? = null,
  ): LicenceComCase = LicenceComCase(
    crn = crn,
    prisonNumber = nomisId,
    kind = kind,
    licenceId = 1,
    typeCode = typeCode,
    licenceStatus = licenceStatus,
    comUsername = comUsername,
    conditionalReleaseDate = conditionalReleaseDate,
    actualReleaseDate = confirmedReleaseDate,
    licenceStartDate = licenceStartDate,
    dateCreated = LocalDateTime.now(),
    forename = null,
    surname = null,
    versionOfId = versionOfId,
    postRecallReleaseDate = LocalDate.now(),
    homeDetentionCurfewActualDate = LocalDate.now(),
    updatedByFirstName = "firstName",
    updatedByLastName = "lastName",
    reviewDate = LocalDateTime.now(),
    approvedByName = "approver name",
    approvedDate = LocalDateTime.now(),
  )

  private fun aManagedOffenderCrn(nomisId: String? = "ABC123"): ManagedOffenderCrn = ManagedOffenderCrn(
    crn = "X12348",
    nomisId,
    staff = StaffDetail(name = Name(forename = "Joe", surname = "Bloggs"), code = "X1234"),
  )
}
