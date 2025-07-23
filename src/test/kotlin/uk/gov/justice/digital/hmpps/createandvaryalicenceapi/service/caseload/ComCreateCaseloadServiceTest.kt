package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.kotlin.any
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CaseloadItem
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ComCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CvlFields
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Prisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ProbationPractitioner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CaseloadService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.EligibilityService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.HdcService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.HdcService.HdcStatuses
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceCreationService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ManagedOffenderCrn
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.Name
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ProbationCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.StaffDetail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.User
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.toPrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month

class ComCreateCaseloadServiceTest {
  private val caseloadService = mock<CaseloadService>()
  private val deliusApiClient = mock<DeliusApiClient>()
  private val licenceService = mock<LicenceService>()
  private val hdcService = mock<HdcService>()
  private val eligibilityService = mock<EligibilityService>()
  private val licenceCreationService = mock<LicenceCreationService>()

  private val service = ComCreateCaseloadService(
    caseloadService,
    deliusApiClient,
    licenceService,
    eligibilityService,
    hdcService,
    licenceCreationService,
  )

  private val elevenDaysFromNow = LocalDate.now().plusDays(11)
  private val tenDaysFromNow = LocalDate.now().plusDays(10)
  private val nineDaysFromNow = LocalDate.now().plusDays(9)
  private val yesterday = LocalDate.now().minusDays(1)
  private val deliusStaffIdentifier = 213L

  @BeforeEach
  fun reset() {
    reset(deliusApiClient, licenceService, hdcService, eligibilityService)
    whenever(licenceCreationService.determineLicenceKind(any())).thenReturn(LicenceKind.CRD)
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
    verify(eligibilityService, never()).isEligibleForCvl(any())
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
      TestData.managedOffenderCrn() to CaseloadItem(
        prisoner = Prisoner(
          prisonerNumber = "ABC123",
          firstName = "Person",
          lastName = "One",
          dateOfBirth = LocalDate.of(1970, 1, 1),
          conditionalReleaseDate = tenDaysFromNow,
        ),
        cvl = CvlFields(
          hardStopDate = LocalDate.of(2023, Month.FEBRUARY, 3),
          hardStopWarningDate = LocalDate.of(2023, Month.FEBRUARY, 1),
          licenceType = LicenceType.PSS,
        ),
      ),
    )

    val casesAndLicences = service.mapCasesToLicences(cases)

    assertThat(casesAndLicences).hasSize(1)
    assertThat(casesAndLicences["X12348"]).hasSize(1)
    assertThat(casesAndLicences["X12348"]!!.first().licenceStatus).isEqualTo(LicenceStatus.NOT_STARTED)
    verify(licenceService).findLicencesForCrnsAndStatuses(any(), any())
  }

  @Test
  fun `it sets not started licences to timed out when in the hard stop period`() {
    val cases = mapOf(
      aManagedOffenderCrn() to CaseloadItem(
        prisoner = Prisoner(
          prisonerNumber = "ABC123",
          firstName = "Person",
          lastName = "One",
          dateOfBirth = LocalDate.of(1970, 1, 1),
        ),
        cvl = CvlFields(isInHardStopPeriod = true, licenceType = LicenceType.AP),
      ),
    )

    val caseAndLicences = service.mapCasesToLicences(cases)

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
        createCaseloadItem("AB1234E", tenDaysFromNow, bookingId = "1", licenceStartDate = tenDaysFromNow),
      ),
    )
    val prisonersToLicenceStartDates = mapOf("AB1234E" to tenDaysFromNow)
    whenever(eligibilityService.isEligibleForCvl(any())).thenReturn(true)
    whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(emptySet()))

    val caseload = service.getStaffCreateCaseload(deliusStaffIdentifier)

    assertThat(caseload).hasSize(1)
    verifyCase(
      caseload.first(),
      "X12348",
      "AB1234E",
      LicenceStatus.NOT_STARTED,
      LicenceType.AP,
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
      createCaseloadItem("AB1234E", nineDaysFromNow, paroleEligibilityDate = yesterday, bookingId = "1", licenceStartDate = nineDaysFromNow),
      createCaseloadItem("AB1234F", tenDaysFromNow, paroleEligibilityDate = tenDaysFromNow, bookingId = "2", licenceStartDate = tenDaysFromNow),
      createCaseloadItem("AB1234G", tenDaysFromNow, legalStatus = "DEAD", bookingId = "3", licenceStartDate = tenDaysFromNow),
      createCaseloadItem("AB1234H", tenDaysFromNow, indeterminateSentence = true, bookingId = "4", licenceStartDate = tenDaysFromNow),
      createCaseloadItem("AB1234I", tenDaysFromNow, bookingId = "5", licenceStartDate = tenDaysFromNow),
      createCaseloadItem("AB1234J", tenDaysFromNow, bookingId = "6", licenceStartDate = tenDaysFromNow),
      createCaseloadItem("AB1234K", tenDaysFromNow, bookingId = "123", licenceStartDate = tenDaysFromNow),
      createCaseloadItem("AB1234L", nineDaysFromNow, bookingId = "123", licenceStartDate = nineDaysFromNow),
      // This case tests that recalls are overridden if the PRRD < the conditionalReleaseDate - so NOT_STARTED
      createCaseloadItem(
        "AB1234M",
        tenDaysFromNow,
        postRecallReleaseDate = nineDaysFromNow,
        recall = true,
        bookingId = "7",
        licenceStartDate = tenDaysFromNow,
      ),
      createCaseloadItem(
        prisonerNumber = "AB1234N",
        conditionalReleaseDate = tenDaysFromNow,
        postRecallReleaseDate = elevenDaysFromNow,
        recall = true,
        bookingId = "8",
        licenceStartDate = tenDaysFromNow,
      ),
      // This case tests that recalls are overridden if the PRRD is equal to the conditionalReleaseDate - so NOT_STARTED
      createCaseloadItem(
        prisonerNumber = "AB1234P",
        conditionalReleaseDate = nineDaysFromNow,
        postRecallReleaseDate = nineDaysFromNow,
        recall = true,
        bookingId = "9",
        licenceStartDate = nineDaysFromNow,
      ),
      // This case tests that recalls are overridden if no PRRD exists and there is only the conditionalReleaseDate - so NOT_STARTED
      createCaseloadItem("AB1234Q", nineDaysFromNow, recall = true, bookingId = "10", licenceStartDate = nineDaysFromNow),
      // This case tests that the case is included when the status is INACTIVE TRN
      createCaseloadItem("AB1234R", nineDaysFromNow, status = "INACTIVE TRN", bookingId = "11", licenceStartDate = nineDaysFromNow),
    )

    whenever(caseloadService.getPrisonersByNumber(any())).thenReturn(caseloadItems)

    whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(emptySet()))
    whenever(eligibilityService.isEligibleForCvl(any())).thenReturn(true)
    whenever(eligibilityService.isEligibleForCvl(caseloadItems[1].prisoner.toPrisonerSearchPrisoner())).thenReturn(
      false,
    )
    whenever(eligibilityService.isEligibleForCvl(caseloadItems[2].prisoner.toPrisonerSearchPrisoner())).thenReturn(
      false,
    )
    whenever(eligibilityService.isEligibleForCvl(caseloadItems[9].prisoner.toPrisonerSearchPrisoner())).thenReturn(
      false,
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
    )
    verifyCase(
      case = caseload[1],
      expectedCrn = "X12351",
      expectedPrisonerNumber = "AB1234L",
      expectedLicenceStatus = LicenceStatus.NOT_STARTED,
      expectedLicenceType = LicenceType.AP,
      expectedReleaseDate = nineDaysFromNow,
    )
    verifyCase(
      case = caseload[2],
      expectedCrn = "X12354",
      expectedPrisonerNumber = "AB1234P",
      expectedLicenceStatus = LicenceStatus.NOT_STARTED,
      expectedLicenceType = LicenceType.AP,
      expectedReleaseDate = nineDaysFromNow,
    )
    verifyCase(
      case = caseload[3],
      expectedCrn = "X12355",
      expectedPrisonerNumber = "AB1234Q",
      expectedLicenceStatus = LicenceStatus.NOT_STARTED,
      expectedLicenceType = LicenceType.AP,
      expectedReleaseDate = nineDaysFromNow,
    )
    verifyCase(
      case = caseload[4],
      expectedCrn = "X12356",
      expectedPrisonerNumber = "AB1234R",
      expectedLicenceStatus = LicenceStatus.NOT_STARTED,
      expectedLicenceType = LicenceType.AP,
      expectedReleaseDate = nineDaysFromNow,
    )
    verifyCase(
      case = caseload[5],
      expectedCrn = "X12352",
      expectedPrisonerNumber = "AB1234M",
      expectedLicenceStatus = LicenceStatus.NOT_STARTED,
      expectedLicenceType = LicenceType.AP,
      expectedReleaseDate = tenDaysFromNow,
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
        createCaseloadItem(
          bookingId = "1",
          prisonerNumber = "AB1234E",
          conditionalReleaseDate = tenDaysFromNow,
          releaseDate = tenDaysFromNow,
          licenceExpiryDate = LocalDate.of(
            2022,
            Month.DECEMBER,
            26,
          ),
        ),
        createCaseloadItem(
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

    whenever(licenceService.findLicencesForCrnsAndStatuses(any(), any())).thenReturn(
      listOf(
        createLicenceSummary(
          crn = "X12348",
          nomisId = "AB1234E",
          kind = LicenceKind.CRD,
          licenceType = LicenceType.AP_PSS,
          licenceStatus = LicenceStatus.SUBMITTED,
          licenceExpiryDate = elevenDaysFromNow,
          comUsername = "johndoe",
          licenceStartDate = LocalDate.now().minusDays(2),
        ),
        createLicenceSummary(
          crn = "X12349",
          nomisId = "AB1234F",
          kind = LicenceKind.CRD,
          licenceType = LicenceType.AP_PSS,
          licenceStatus = LicenceStatus.SUBMITTED,
          licenceExpiryDate = elevenDaysFromNow,
          comUsername = "johndoe",
          licenceStartDate = tenDaysFromNow,
        ),
      ),
    )
    whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(emptySet()))
    whenever(eligibilityService.isEligibleForCvl(any())).thenReturn(true)
    whenever(deliusApiClient.getStaffDetailsByUsername(any())).thenReturn(
      listOf(
        User(
          username = "johndoe",
          code = "X54321",
          name = Name(forename = "John", surname = "Doe"),
          teams = emptyList(),
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

    val case1 = createCaseloadItem(
      bookingId = "1",
      prisonerNumber = "AB1234E",
      conditionalReleaseDate = tenDaysFromNow,
      releaseDate = tenDaysFromNow,
      licenceExpiryDate = LocalDate.of(
        2022,
        Month.DECEMBER,
        26,
      ),
      licenceStartDate = tenDaysFromNow,
    )
    val case2 = createCaseloadItem(
      bookingId = "2",
      prisonerNumber = "AB1234F",
      conditionalReleaseDate = tenDaysFromNow,
      releaseDate = tenDaysFromNow,
      status = "INACTIVE OUT",
      licenceStartDate = tenDaysFromNow,
    )
    val case3 = createCaseloadItem(
      bookingId = "3",
      prisonerNumber = "AB1234G",
      conditionalReleaseDate = tenDaysFromNow,
      releaseDate = tenDaysFromNow,
      status = "INACTIVE OUT",
      licenceStartDate = tenDaysFromNow,
    )
    val case4 = createCaseloadItem(
      bookingId = "4",
      prisonerNumber = "AB1234H",
      conditionalReleaseDate = tenDaysFromNow,
      releaseDate = tenDaysFromNow,
      topupSupervisionExpiryDate = LocalDate.of(2023, Month.JUNE, 22),
      licenceStartDate = tenDaysFromNow,
    )
    val case5 = createCaseloadItem(
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
    whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(emptySet()))

    whenever(eligibilityService.isEligibleForCvl(case1.prisoner.toPrisonerSearchPrisoner())).thenReturn(true)
    whenever(eligibilityService.isEligibleForCvl(case2.prisoner.toPrisonerSearchPrisoner())).thenReturn(false)
    whenever(eligibilityService.isEligibleForCvl(case3.prisoner.toPrisonerSearchPrisoner())).thenReturn(false)
    whenever(eligibilityService.isEligibleForCvl(case4.prisoner.toPrisonerSearchPrisoner())).thenReturn(true)
    whenever(eligibilityService.isEligibleForCvl(case5.prisoner.toPrisonerSearchPrisoner())).thenReturn(true)

    whenever(licenceService.findLicencesForCrnsAndStatuses(any(), any())).thenReturn(
      listOf(
        createLicenceSummary(
          crn = "X12352",
          nomisId = "AB1234I",
          kind = LicenceKind.CRD,
          licenceType = LicenceType.AP_PSS,
          licenceStatus = LicenceStatus.SUBMITTED,
          licenceExpiryDate = elevenDaysFromNow,
          comUsername = "johndoe",
          licenceStartDate = elevenDaysFromNow,
        ),
      ),
    )

    whenever(deliusApiClient.getStaffDetailsByUsername(any())).thenReturn(
      listOf(
        User(
          username = "johndoe",
          code = "X54321",
          name = Name(forename = "John", surname = "Doe"),
          teams = emptyList(),
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
    )
    verifyCase(
      case = caseload[1],
      expectedCrn = "X12351",
      expectedPrisonerNumber = "AB1234H",
      expectedLicenceStatus = LicenceStatus.NOT_STARTED,
      expectedLicenceType = LicenceType.PSS,
      expectedReleaseDate = tenDaysFromNow,
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
        createCaseloadItem(
          bookingId = "1",
          prisonerNumber = "AB1234E",
          conditionalReleaseDate = tenDaysFromNow,
          status = "ACTIVE IN",
          licenceStartDate = tenDaysFromNow,
        ),
        createCaseloadItem(
          bookingId = "2",
          prisonerNumber = "AB1234F",
          conditionalReleaseDate = tenDaysFromNow,
          topupSupervisionExpiryDate = LocalDate.of(2022, Month.DECEMBER, 26),
          licenceStartDate = tenDaysFromNow,
        ),
      ),
    )

    whenever(eligibilityService.isEligibleForCvl(any())).thenReturn(true)
    whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(emptySet()))

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
    )
  }

  @Test
  fun `PRRD licences will be mapped to offenders and caseloads for team will be created`() {
    // Given
    val selectedTeam = "team c"
    val prisonerNumber = "AB1234E"
    val managedOffender = aManagedOffenderCrn(prisonerNumber)
    val managedOffenders = listOf(managedOffender)
    val caseLoadItem = createCaseloadItem(
      bookingId = "1",
      prisonerNumber = prisonerNumber,
      conditionalReleaseDate = null,
      postRecallReleaseDate = LocalDate.now(),
      licenceStartDate = LocalDate.now(),
    )

    whenever(deliusApiClient.getManagedOffendersByTeam(selectedTeam)).thenReturn(managedOffenders)
    whenever(caseloadService.getPrisonersByNumber(any())).thenReturn(listOf(caseLoadItem))
    whenever(eligibilityService.isEligibleForCvl(any())).thenReturn(true)
    whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(emptySet()))

    // When
    val caseload = service.getTeamCreateCaseload(listOf(), listOf(selectedTeam))

    // Then
    assertThat(caseload).hasSize(1)
  }

  @Test
  fun `PRRD licences will be mapped to offenders and caseloads for staff will be created`() {
    // Given
    val prisonerNumber = "AB1234E"
    val managedOffender = aManagedOffenderCrn(prisonerNumber)
    val managedOffenders = listOf(managedOffender)
    val caseLoadItem = createCaseloadItem(
      bookingId = "1",
      prisonerNumber = prisonerNumber,
      conditionalReleaseDate = null,
      postRecallReleaseDate = LocalDate.now(),
      licenceStartDate = LocalDate.now(),
    )

    whenever(deliusApiClient.getManagedOffenders(deliusStaffIdentifier)).thenReturn(managedOffenders)
    whenever(caseloadService.getPrisonersByNumber(any())).thenReturn(listOf(caseLoadItem))
    whenever(eligibilityService.isEligibleForCvl(any())).thenReturn(true)
    whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(emptySet()))

    // When
    val caseload = service.getStaffCreateCaseload(deliusStaffIdentifier)

    // Then
    assertThat(caseload).hasSize(1)
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
      createCaseloadItem(
        bookingId = "1",
        prisonerNumber = "AB1234E",
        conditionalReleaseDate = tenDaysFromNow,
        releaseDate = tenDaysFromNow,
        postRecallReleaseDate = tenDaysFromNow,
        recall = true,
        licenceStartDate = tenDaysFromNow,
      ),
    )
    whenever(caseloadService.getPrisonersByNumber(any())).thenReturn(caseloadItems)
    whenever(eligibilityService.isEligibleForCvl(caseloadItems[0].prisoner.toPrisonerSearchPrisoner())).thenReturn(true)
    whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(setOf(caseloadItems[0].prisoner.bookingId?.toLong()!!)))

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
        createCaseloadItem("AB1234E", yesterday, bookingId = "5", postRecallReleaseDate = tenDaysFromNow, licenceStartDate = tenDaysFromNow),
      ),
    )

    whenever(licenceCreationService.determineLicenceKind(any())).thenReturn(LicenceKind.PRRD)
    whenever(eligibilityService.isEligibleForCvl(any())).thenReturn(false)

    val caseload = service.getStaffCreateCaseload(deliusStaffIdentifier)

    assertThat(caseload).hasSize(0)
    verify(eligibilityService, times(1)).isEligibleForCvl(any())
  }

  @Test
  fun `it bypasses eligibility checks for existing PRRD licences`() {
    whenever(
      deliusApiClient.getManagedOffenders(deliusStaffIdentifier),
    ).thenReturn(
      listOf(
        ManagedOffenderCrn(
          crn = "X12348",
          nomisId = "AB1234E",
          staff = StaffDetail(name = Name(forename = "Joe", surname = "Bloggs"), code = "X1234"),
        ),
      ),
    )

    whenever(caseloadService.getPrisonersByNumber(any())).thenReturn(
      listOf(
        createCaseloadItem("AB1234E", yesterday, bookingId = "5", postRecallReleaseDate = tenDaysFromNow, licenceStartDate = tenDaysFromNow),
      ),
    )

    whenever(licenceCreationService.determineLicenceKind(any())).thenReturn(LicenceKind.PRRD)
    whenever(eligibilityService.isEligibleForCvl(any())).thenReturn(false)
    whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(emptySet()))
    whenever(licenceService.findLicencesForCrnsAndStatuses(any(), any())).thenReturn(
      listOf(
        createLicenceSummary(
          crn = "X12348",
          nomisId = "AB1234E",
          kind = LicenceKind.PRRD,
          licenceType = LicenceType.AP_PSS,
          licenceStatus = LicenceStatus.SUBMITTED,
          licenceExpiryDate = elevenDaysFromNow,
          comUsername = "johndoe",
          licenceStartDate = tenDaysFromNow,
        ),
      ),
    )

    val caseload = service.getStaffCreateCaseload(deliusStaffIdentifier)

    assertThat(caseload).hasSize(1)
    verifyCase(
      case = caseload[0],
      expectedCrn = "X12348",
      expectedPrisonerNumber = "AB1234E",
      expectedLicenceStatus = LicenceStatus.SUBMITTED,
      expectedLicenceType = LicenceType.AP_PSS,
      expectedReleaseDate = tenDaysFromNow,
      expectedProbationPractitioner = ProbationPractitioner(
        staffCode = "X1234",
        name = "Joe Bloggs",
      ),
      expectedLicenceKind = LicenceKind.PRRD,
    )
    verify(eligibilityService, never()).isEligibleForCvl(any())
  }

  private fun createCaseloadItem(
    prisonerNumber: String,
    conditionalReleaseDate: LocalDate?,
    status: String = "ACTIVE IN",
    postRecallReleaseDate: LocalDate? = null,
    recall: Boolean = false,
    bookingId: String? = null,
    topupSupervisionExpiryDate: LocalDate? = null,
    licenceExpiryDate: LocalDate? = null,
    releaseDate: LocalDate? = null,
    imprisonmentStatus: String? = null,
    confirmedReleaseDate: LocalDate? = null,
    paroleEligibilityDate: LocalDate? = null,
    legalStatus: String? = null,
    indeterminateSentence: Boolean? = false,
    licenceStartDate: LocalDate? = null,
  ): CaseloadItem = CaseloadItem(
    prisoner = Prisoner(
      prisonerNumber = prisonerNumber,
      conditionalReleaseDate = conditionalReleaseDate,
      firstName = "Person",
      lastName = "One",
      dateOfBirth = LocalDate.of(1970, 1, 1),
      status = status,
      bookingId = bookingId,
      postRecallReleaseDate = postRecallReleaseDate,
      recall = recall,
      topupSupervisionExpiryDate = topupSupervisionExpiryDate,
      licenceExpiryDate = licenceExpiryDate,
      releaseDate = releaseDate,
      imprisonmentStatus = imprisonmentStatus,
      confirmedReleaseDate = confirmedReleaseDate,
      paroleEligibilityDate = paroleEligibilityDate,
      legalStatus = legalStatus,
      indeterminateSentence = indeterminateSentence,
    ),
    cvl = CvlFields(licenceType = LicenceType.PSS, licenceStartDate = licenceStartDate),
  )

  private fun verifyCase(
    case: ComCase,
    expectedCrn: String,
    expectedPrisonerNumber: String,
    expectedLicenceStatus: LicenceStatus,
    expectedLicenceType: LicenceType,
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
    }
  }

  private fun createProbationCase(crn: String, nomsNumber: String? = null) = ProbationCase(
    nomisId = nomsNumber,
    crn = crn,
  )

  private fun createLicenceSummary(
    crn: String,
    nomisId: String,
    licenceType: LicenceType,
    licenceStatus: LicenceStatus,
    kind: LicenceKind = LicenceKind.CRD,
    licenceExpiryDate: LocalDate? = null,
    comUsername: String? = null,
    conditionalReleaseDate: LocalDate? = null,
    confirmedReleaseDate: LocalDate? = null,
    licenceStartDate: LocalDate? = null,
    isReviewNeeded: Boolean = false,
  ): LicenceSummary = LicenceSummary(
    crn = crn,
    nomisId = nomisId,
    kind = kind,
    licenceId = 1,
    licenceType = licenceType,
    licenceStatus = licenceStatus,
    licenceExpiryDate = licenceExpiryDate,
    comUsername = comUsername,
    isReviewNeeded = isReviewNeeded,
    isDueForEarlyRelease = false,
    isInHardStopPeriod = false,
    isDueToBeReleasedInTheNextTwoWorkingDays = false,
    conditionalReleaseDate = conditionalReleaseDate,
    actualReleaseDate = confirmedReleaseDate,
    licenceStartDate = licenceStartDate,
    dateCreated = LocalDateTime.now(),
    updatedByFullName = "X Y",
    bookingId = null,
    dateOfBirth = null,
    forename = null,
    surname = null,
    prisonCode = null,
    prisonDescription = null,
    probationLauCode = null,
    probationPduCode = null,
    probationAreaCode = null,
    probationTeamCode = null,
    probationAreaDescription = null,
    probationTeamDescription = null,
    probationLauDescription = null,
    probationPduDescription = null,
  )

  private fun aManagedOffenderCrn(nomisId: String? = "ABC123"): ManagedOffenderCrn = ManagedOffenderCrn(
    crn = "X12348",
    nomisId,
    staff = StaffDetail(name = Name(forename = "Joe", surname = "Bloggs"), code = "X1234"),
  )
}
