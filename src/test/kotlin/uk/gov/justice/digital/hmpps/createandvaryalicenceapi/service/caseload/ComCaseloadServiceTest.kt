package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.never
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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.ManagedCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.PrisonerSearchService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ManagedOffenderCrn
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.Name
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.OffenderDetail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.OtherIds
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ProbationSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.StaffDetail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.User
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month

class ComCaseloadServiceTest {
  private val caseloadService = mock<CaseloadService>()
  private val deliusApiClient = mock<DeliusApiClient>()
  private val licenceService = mock<LicenceService>()
  private val probationSearchApiClient = mock<ProbationSearchApiClient>()
  private val prisonerSearchService = mock<PrisonerSearchService>()
  private val releaseDateService = mock<ReleaseDateService>()

  private val service = ComCaseloadService(
    caseloadService,
    deliusApiClient,
    licenceService,
    prisonerSearchService,
    probationSearchApiClient,
    releaseDateService,
  )

  private val elevenDaysFromNow = LocalDate.now().plusDays(11)
  private val tenDaysFromNow = LocalDate.now().plusDays(10)
  private val nineDaysFromNow = LocalDate.now().plusDays(9)
  private val yesterday = LocalDate.now().minusDays(1)
  private val deliusStaffIdentifier = 213L

  @BeforeEach
  fun reset() {
    reset(deliusApiClient, licenceService, prisonerSearchService, probationSearchApiClient)
  }

  @Test
  fun `it filters out cases with no NOMIS record`() {
    val cases = listOf(ManagedCase(nomisRecord = null, cvlFields = CvlFields(LicenceType.AP_PSS)))
    val casesAndLicences = service.mapOffendersToLicences(cases)

    assertThat(casesAndLicences).hasSize(0)
    verify(licenceService, never()).findLicencesMatchingCriteria(any())
  }

  @Test
  fun `it calls the licence service when Nomis records are found`() {
    whenever(releaseDateService.getLicenceStartDates(any())).thenReturn(
      mapOf("ABC123" to tenDaysFromNow),
    )
    val cases = listOf(
      ManagedCase(
        nomisRecord = Prisoner(
          prisonerNumber = "ABC123",
          firstName = "Bob",
          lastName = "Smith",
          dateOfBirth = LocalDate.of(1970, 1, 1),
          conditionalReleaseDate = tenDaysFromNow,
        ),
        cvlFields = CvlFields(
          hardStopDate = LocalDate.of(2023, Month.FEBRUARY, 3),
          hardStopWarningDate = LocalDate.of(2023, Month.FEBRUARY, 1),
          licenceType = LicenceType.PSS,
        ),
      ),
    )

    val casesAndLicences = service.mapOffendersToLicences(cases)

    assertThat(casesAndLicences).hasSize(1)
    with(casesAndLicences.first()) {
      assertThat(licences).hasSize(1)
      assertThat(licences[0].licenceStatus).isEqualTo(LicenceStatus.NOT_STARTED)
    }
    verify(licenceService).findLicencesMatchingCriteria(any())
  }

  @Test
  fun `it sets not started licences to timed out when in the hard stop period`() {
    whenever(releaseDateService.getLicenceStartDates(any())).thenReturn(
      mapOf("ABC123" to tenDaysFromNow),
    )
    val cases = listOf(
      ManagedCase(
        nomisRecord = Prisoner(
          prisonerNumber = "ABC123",
          firstName = "Bob",
          lastName = "Smith",
          dateOfBirth = LocalDate.of(1970, 1, 1),
        ),
        cvlFields = CvlFields(isInHardStopPeriod = true, licenceType = LicenceType.AP),
      ),
    )

    val caseAndLicences = service.mapOffendersToLicences(cases)

    assertThat(caseAndLicences).hasSize(1)
    with(caseAndLicences.first()) {
      assertThat(nomisRecord?.prisonerNumber).isEqualTo("ABC123")
      assertThat(cvlFields.isInHardStopPeriod).isTrue()
      assertThat(licences).hasSize(1)
      assertThat(licences[0].licenceStatus).isEqualTo(LicenceStatus.TIMED_OUT)
      assertThat(licences[0].licenceType).isEqualTo(LicenceType.AP)
    }
  }

  @Test
  fun `it filters invalid data due to mismatch between delius and nomis`() {
    val managedOffenders = listOf(
      ManagedOffenderCrn(crn = "X12346"),
      ManagedOffenderCrn(crn = "X12347"),
      ManagedOffenderCrn(crn = "X12348"),
    )

    whenever(deliusApiClient.getManagedOffenders(deliusStaffIdentifier)).thenReturn(managedOffenders)

    whenever(probationSearchApiClient.getOffendersByCrn(managedOffenders.map { it.crn })).thenReturn(
      listOf(
        createOffenderDetail(id = 1, crn = "X12346", nomsNumber = "AB1234D"),
        createOffenderDetail(id = 2, crn = "X12347"),
        createOffenderDetail(id = 3, crn = "X12348", nomsNumber = "AB1234E"),
      ),
    )

    whenever(caseloadService.getPrisonersByNumber(any())).thenReturn(
      listOf(
        createCaseloadItem("AB1234E", tenDaysFromNow),
      ),
    )

    whenever(releaseDateService.getLicenceStartDates(any())).thenReturn(
      mapOf(
        "AB1234E" to tenDaysFromNow,
      ),
    )

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
      ManagedOffenderCrn(crn = "X12348"),
      ManagedOffenderCrn(crn = "X12349"),
      ManagedOffenderCrn(crn = "X12350"),
      ManagedOffenderCrn(crn = "X12351"),
      ManagedOffenderCrn(crn = "X12352"),
      ManagedOffenderCrn(crn = "X12353"),
      ManagedOffenderCrn(crn = "X12354"),
      ManagedOffenderCrn(crn = "X12355"),
      ManagedOffenderCrn(crn = "X12356"),
    )

    whenever(
      deliusApiClient.getManagedOffenders(deliusStaffIdentifier),
    ).thenReturn(managedOffenders)

    whenever(probationSearchApiClient.getOffendersByCrn(managedOffenders.map { it.crn })).thenReturn(
      listOf(
        createOffenderDetail(1L, nomsNumber = "AB1234E", crn = "X12348"),
        createOffenderDetail(3L, nomsNumber = "AB1234F", crn = "X12349"),
        createOffenderDetail(id = 5L, nomsNumber = "AB1234G", crn = "X12350"),
        createOffenderDetail(id = 6L, nomsNumber = "AB1234L", crn = "X12351"),
        createOffenderDetail(id = 7L, nomsNumber = "AB1234M", crn = "X12352"),
        createOffenderDetail(id = 8L, nomsNumber = "AB1234N", crn = "X12353"),
        createOffenderDetail(id = 9L, nomsNumber = "AB1234P", crn = "X12354"),
        createOffenderDetail(id = 10L, nomsNumber = "AB1234Q", crn = "X12355"),
        createOffenderDetail(id = 11L, nomsNumber = "AB1234R", crn = "X12356"),
      ),
    )

    whenever(releaseDateService.getLicenceStartDates(any())).thenReturn(
      mapOf(
        "AB1234E" to nineDaysFromNow,
        "AB1234L" to nineDaysFromNow,
        "AB1234M" to tenDaysFromNow,
        "AB1234P" to nineDaysFromNow,
        "AB1234Q" to nineDaysFromNow,
        "AB1234R" to nineDaysFromNow,
      ),
    )

    val caseloadItems = listOf(
      createCaseloadItem("AB1234E", nineDaysFromNow, paroleEligibilityDate = yesterday),
      createCaseloadItem("AB1234F", tenDaysFromNow, paroleEligibilityDate = tenDaysFromNow),
      createCaseloadItem("AB1234G", tenDaysFromNow, legalStatus = "DEAD"),
      createCaseloadItem("AB1234H", tenDaysFromNow, indeterminateSentence = true),
      createCaseloadItem("AB1234I", tenDaysFromNow),
      createCaseloadItem("AB1234J", tenDaysFromNow),
      createCaseloadItem("AB1234K", tenDaysFromNow, bookingId = "123"),
      createCaseloadItem("AB1234L", nineDaysFromNow, bookingId = "123"),
      // This case tests that recalls are overridden if the PRRD < the conditionalReleaseDate - so NOT_STARTED
      createCaseloadItem(
        "AB1234M",
        tenDaysFromNow,
        postRecallReleaseDate = nineDaysFromNow,
        recall = true,
      ),
      createCaseloadItem(
        prisonerNumber = "AB1234N",
        conditionalReleaseDate = tenDaysFromNow,
        postRecallReleaseDate = elevenDaysFromNow,
        recall = true,
      ),
      // This case tests that recalls are overridden if the PRRD is equal to the conditionalReleaseDate - so NOT_STARTED
      createCaseloadItem(
        prisonerNumber = "AB1234P",
        conditionalReleaseDate = nineDaysFromNow,
        postRecallReleaseDate = nineDaysFromNow,
        recall = true,
      ),
      // This case tests that recalls are overridden if no PRRD exists and there is only the conditionalReleaseDate - so NOT_STARTED
      createCaseloadItem("AB1234Q", nineDaysFromNow, recall = true),
      // This case tests that the case is included when the status is INACTIVE TRN
      createCaseloadItem("AB1234R", nineDaysFromNow, status = "INACTIVE TRN"),
    )

    whenever(caseloadService.getPrisonersByNumber(any())).thenReturn(caseloadItems)

    whenever(prisonerSearchService.getIneligibilityReasons(caseloadItems[1].prisoner)).thenReturn(listOf("is eligible for parole"))
    whenever(prisonerSearchService.getIneligibilityReasons(caseloadItems[2].prisoner)).thenReturn(listOf("has incorrect legal status"))
    whenever(prisonerSearchService.getIneligibilityReasons(caseloadItems[9].prisoner)).thenReturn(listOf("is a recall case"))

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
        staff = StaffDetail(name = Name(forename = "Sherlock", surname = "Holmes"), code = "X54321"),
      ),
      ManagedOffenderCrn(
        crn = "X12349",
        staff = StaffDetail(name = Name(forename = "Sherlock", surname = "Holmes"), code = "X54321"),
      ),
    )

    whenever(
      deliusApiClient.getManagedOffenders(deliusStaffIdentifier),
    ).thenReturn(managedOffenders)

    whenever(probationSearchApiClient.getOffendersByCrn(managedOffenders.map { it.crn })).thenReturn(
      listOf(
        createOffenderDetail(1L, nomsNumber = "AB1234E", crn = "X12348"),
        createOffenderDetail(1L, nomsNumber = "AB1234F", crn = "X12349"),
      ),
    )

    whenever(caseloadService.getPrisonersByNumber(any())).thenReturn(
      listOf(
        createCaseloadItem(
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

    whenever(licenceService.findLicencesMatchingCriteria(any())).thenReturn(
      listOf(
        createLicenceSummary(
          crn = "X12348",
          nomisId = "AB1234E",
          kind = LicenceKind.CRD,
          licenceType = LicenceType.AP_PSS,
          licenceStatus = LicenceStatus.SUBMITTED,
          licenceExpiryDate = elevenDaysFromNow,
          comUsername = "sherlockholmes",
          licenceStartDate = LocalDate.now().minusDays(2),
        ),
        createLicenceSummary(
          crn = "X12349",
          nomisId = "AB1234F",
          kind = LicenceKind.CRD,
          licenceType = LicenceType.AP_PSS,
          licenceStatus = LicenceStatus.SUBMITTED,
          licenceExpiryDate = elevenDaysFromNow,
          comUsername = "sherlockholmes",
          licenceStartDate = tenDaysFromNow,
        ),
      ),
    )

    whenever(deliusApiClient.getStaffDetailsByUsername(any())).thenReturn(
      listOf(
        User(
          username = "sherlockholmes",
          code = "X54321",
          name = Name(forename = "Sherlock", surname = "Holmes"),
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
      expectedProbationPractitioner = ProbationPractitioner(staffCode = "X54321", name = "Sherlock Holmes"),
    )
  }

  @Test
  fun `it builds the staff create caseload`() {
    val managedOffenders = listOf(
      ManagedOffenderCrn(
        crn = "X12348",
        staff = StaffDetail(name = Name(forename = "Joe", surname = "Bloggs"), code = "X1234"),
      ),
      ManagedOffenderCrn(
        crn = "X12349",
        staff = StaffDetail(name = Name(forename = "Joe", surname = "Bloggs"), code = "X1234"),
      ),
      ManagedOffenderCrn(
        crn = "X12350",
        staff = StaffDetail(name = Name(forename = "Joe", surname = "Bloggs"), code = "X1234"),
      ),
      ManagedOffenderCrn(
        crn = "X12351",
        staff = StaffDetail(name = Name(forename = null, surname = null), code = "X1234", unallocated = true),
      ),
      ManagedOffenderCrn(
        crn = "X12352",
        staff = StaffDetail(name = Name(forename = "Joe", surname = "Bloggs"), code = "X1234"),
      ),
    )

    whenever(
      deliusApiClient.getManagedOffenders(deliusStaffIdentifier),
    ).thenReturn(managedOffenders)

    whenever(probationSearchApiClient.getOffendersByCrn(managedOffenders.map { it.crn })).thenReturn(
      listOf(
        createOffenderDetail(1L, nomsNumber = "AB1234E", crn = "X12348"),
        createOffenderDetail(3L, nomsNumber = "AB1234F", crn = "X12349"),
        createOffenderDetail(id = 5L, nomsNumber = "AB1234G", crn = "X12350"),
        createOffenderDetail(id = 6L, nomsNumber = "AB1234H", crn = "X12351"),
        createOffenderDetail(id = 7L, nomsNumber = "AB1234I", crn = "X12352"),
      ),
    )

    whenever(caseloadService.getPrisonersByNumber(any())).thenReturn(
      listOf(
        createCaseloadItem(
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
          prisonerNumber = "AB1234F",
          conditionalReleaseDate = tenDaysFromNow,
          releaseDate = tenDaysFromNow,
          status = "INACTIVE OUT",
        ),
        createCaseloadItem(
          prisonerNumber = "AB1234G",
          conditionalReleaseDate = tenDaysFromNow,
          releaseDate = tenDaysFromNow,
          status = "INACTIVE OUT",
        ),
        createCaseloadItem(
          prisonerNumber = "AB1234H",
          conditionalReleaseDate = tenDaysFromNow,
          releaseDate = tenDaysFromNow,
          topupSupervisionExpiryDate = LocalDate.of(2023, Month.JUNE, 22),
        ),
        createCaseloadItem(
          prisonerNumber = "AB1234I",
          conditionalReleaseDate = elevenDaysFromNow,
          releaseDate = elevenDaysFromNow,
          topupSupervisionExpiryDate = LocalDate.of(2023, Month.JUNE, 22),
          licenceExpiryDate = elevenDaysFromNow,
        ),
      ),
    )

    whenever(licenceService.findLicencesMatchingCriteria(any())).thenReturn(
      listOf(
        createLicenceSummary(
          crn = "X12352",
          nomisId = "AB1234I",
          kind = LicenceKind.CRD,
          licenceType = LicenceType.AP_PSS,
          licenceStatus = LicenceStatus.SUBMITTED,
          licenceExpiryDate = elevenDaysFromNow,
          comUsername = "sherlockholmes",
          licenceStartDate = elevenDaysFromNow,
        ),
      ),
    )

    whenever(deliusApiClient.getStaffDetailsByUsername(any())).thenReturn(
      listOf(
        User(
          username = "sherlockholmes",
          code = "X54321",
          name = Name(forename = "Sherlock", surname = "Holmes"),
          teams = emptyList(),
        ),
      ),
    )

    whenever(releaseDateService.getLicenceStartDates(any())).thenReturn(
      mapOf(
        "AB1234E" to tenDaysFromNow,
        "AB1234H" to tenDaysFromNow,
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
        name = "Sherlock Holmes",
      ),
    )
  }

  @Test
  fun `it builds the team create caseload`() {
    val selectedTeam = "team c"

    val managedOffenders = listOf(
      ManagedOffenderCrn(
        crn = "X12348",
        staff = StaffDetail(name = Name(forename = "Joe", surname = "Bloggs"), code = "X1234"),
      ),
      ManagedOffenderCrn(
        crn = "X12349",
        staff = StaffDetail(name = Name(forename = "Sherlock", surname = "Holmes"), code = "X54321"),
      ),
    )

    whenever(
      deliusApiClient.getManagedOffendersByTeam(selectedTeam),
    ).thenReturn(managedOffenders)

    whenever(probationSearchApiClient.getOffendersByCrn(managedOffenders.map { it.crn })).thenReturn(
      listOf(
        createOffenderDetail(1L, nomsNumber = "AB1234E", crn = "X12348"),
        createOffenderDetail(3L, nomsNumber = "AB1234F", crn = "X12349"),
      ),
    )

    whenever(caseloadService.getPrisonersByNumber(any())).thenReturn(
      listOf(
        createCaseloadItem(
          prisonerNumber = "AB1234E",
          conditionalReleaseDate = tenDaysFromNow,
          status = "ACTIVE IN",
        ),
        createCaseloadItem(
          prisonerNumber = "AB1234F",
          conditionalReleaseDate = tenDaysFromNow,
          topupSupervisionExpiryDate = LocalDate.of(2022, Month.DECEMBER, 26),
        ),
      ),
    )

    whenever(releaseDateService.getLicenceStartDates(any())).thenReturn(
      mapOf(
        "AB1234E" to tenDaysFromNow,
        "AB1234F" to tenDaysFromNow,
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
        name = "Sherlock Holmes",
      ),
    )
  }

  @Test
  fun `it filters recalls and breach of supervision on team create caseload`() {
    val selectedTeam = "team C"

    val managedOffenders = listOf(
      ManagedOffenderCrn(
        crn = "X12348",
        staff = StaffDetail(name = Name(forename = "Joe", surname = "Bloggs"), code = "X1234"),
      ),
      ManagedOffenderCrn(
        crn = "X12349",
        staff = StaffDetail(name = Name(forename = "Sherlock", surname = "Holmes"), code = "X54321"),
      ),
    )

    whenever(
      deliusApiClient.getManagedOffendersByTeam(selectedTeam),
    ).thenReturn(managedOffenders)

    whenever(probationSearchApiClient.getOffendersByCrn(managedOffenders.map { it.crn })).thenReturn(
      listOf(
        createOffenderDetail(1L, nomsNumber = "AB1234E", crn = "X12348"),
        createOffenderDetail(3L, nomsNumber = "AB1234F", crn = "X12349"),
      ),
    )

    val caseloadItems = listOf(
      createCaseloadItem(
        prisonerNumber = "AB1234E",
        conditionalReleaseDate = tenDaysFromNow,
        releaseDate = tenDaysFromNow,
        postRecallReleaseDate = tenDaysFromNow,
        recall = true,
      ),
      createCaseloadItem(
        prisonerNumber = "AB1234F",
        conditionalReleaseDate = tenDaysFromNow,
        releaseDate = tenDaysFromNow,
        imprisonmentStatus = "BOTUS",
        recall = true,
      ),
    )
    whenever(caseloadService.getPrisonersByNumber(any())).thenReturn(caseloadItems)
    whenever(prisonerSearchService.getIneligibilityReasons(caseloadItems[1].prisoner)).thenReturn(listOf("is breach of top up supervision case"))
    whenever(releaseDateService.getLicenceStartDates(any())).thenReturn(
      mapOf(
        "AB1234E" to tenDaysFromNow,
      ),
    )

    val caseload = service.getTeamCreateCaseload(listOf("team A", "team B"), listOf("team C"))

    verify(deliusApiClient).getManagedOffendersByTeam("team C")
    assertThat(caseload).hasSize(1)
    verifyCase(
      case = caseload[0],
      expectedCrn = "X12348",
      expectedPrisonerNumber = "AB1234E",
      expectedLicenceStatus = LicenceStatus.NOT_STARTED,
      expectedLicenceType = LicenceType.AP,
      expectedReleaseDate = tenDaysFromNow,
      expectedProbationPractitioner = ProbationPractitioner(staffCode = "X1234", name = "Joe Bloggs"),
    )

    verify(deliusApiClient).getManagedOffendersByTeam("team C")
  }

  @Test
  fun `it builds the staff vary caseload where there is a single licence`() {
    val managedOffenders = listOf(
      ManagedOffenderCrn(
        crn = "X12348",
        staff = StaffDetail(code = "X1234", name = Name(forename = "Joe", surname = "Bloggs")),
      ),
    )

    whenever(deliusApiClient.getManagedOffenders(deliusStaffIdentifier)).thenReturn(managedOffenders)

    whenever(
      probationSearchApiClient.getOffendersByCrn(
        managedOffenders.map
          { it.crn },
      ),
    ).thenReturn(
      listOf(
        createOffenderDetail(1L, nomsNumber = "AB1234E", crn = "X12348"),
      ),
    )

    whenever(caseloadService.getPrisonersByNumber(any())).thenReturn(
      listOf(
        createCaseloadItem(
          prisonerNumber = "AB1234E",
          conditionalReleaseDate = null,
          confirmedReleaseDate = tenDaysFromNow,
          licenceExpiryDate = LocalDate.of(
            2022,
            Month.DECEMBER,
            26,
          ),
          status = "INACTIVE OUT",
        ),
      ),
    )

    whenever(licenceService.findLicencesMatchingCriteria(any())).thenReturn(
      listOf(
        createLicenceSummary(
          crn = "X12348",
          nomisId = "AB1234E",
          kind = LicenceKind.CRD,
          licenceType = LicenceType.AP,
          licenceStatus = LicenceStatus.VARIATION_IN_PROGRESS,
          comUsername = "sherlockholmes",
        ),
      ),
    )

    whenever(deliusApiClient.getStaffDetailsByUsername(any())).thenReturn(
      listOf(
        User(
          username = "sherlockholmes",
          code = "X54321",
          name = Name(forename = "Sherlock", surname = "Holmes"),
          teams = emptyList(),
        ),
      ),
    )

    val caseload = service.getStaffVaryCaseload(deliusStaffIdentifier)
    assertThat(caseload).hasSize(1)
    verifyCase(
      caseload[0],
      expectedCrn = "X12348",
      expectedPrisonerNumber = "AB1234E",
      expectedLicenceStatus = LicenceStatus.VARIATION_IN_PROGRESS,
      expectedLicenceType = LicenceType.AP,
      expectedProbationPractitioner = ProbationPractitioner(staffCode = "X54321", name = "Sherlock Holmes"),
    )
  }

  @Test
  fun `it builds the staff vary caseload where there are multiple licences`() {
    val managedOffenders = listOf(
      ManagedOffenderCrn(
        crn = "X12348",
        staff = StaffDetail(code = "X1234", name = Name(forename = "Joe", surname = "Bloggs")),
      ),
    )

    whenever(deliusApiClient.getManagedOffenders(deliusStaffIdentifier)).thenReturn(managedOffenders)

    whenever(
      probationSearchApiClient.getOffendersByCrn(
        managedOffenders.map
          { it.crn },
      ),
    ).thenReturn(
      listOf(
        createOffenderDetail(1L, nomsNumber = "AB1234E", crn = "X12348"),
      ),
    )

    whenever(caseloadService.getPrisonersByNumber(any())).thenReturn(
      listOf(
        createCaseloadItem(
          prisonerNumber = "AB1234E",
          conditionalReleaseDate = null,
          confirmedReleaseDate = tenDaysFromNow,
          licenceExpiryDate = LocalDate.of(
            2022,
            Month.DECEMBER,
            26,
          ),
          status = "INACTIVE OUT",
        ),
      ),
    )

    whenever(licenceService.findLicencesMatchingCriteria(any())).thenReturn(
      listOf(
        createLicenceSummary(
          crn = "X12348",
          nomisId = "AB1234E",
          kind = LicenceKind.CRD,
          licenceType = LicenceType.AP,
          licenceStatus = LicenceStatus.ACTIVE,
          comUsername = "sherlockholmes",
        ),
        createLicenceSummary(
          crn = "X12348",
          nomisId = "AB1234E",
          kind = LicenceKind.CRD,
          licenceType = LicenceType.AP,
          licenceStatus = LicenceStatus.VARIATION_IN_PROGRESS,
          comUsername = "sherlockholmes",
        ),
      ),
    )

    whenever(deliusApiClient.getStaffDetailsByUsername(any())).thenReturn(
      listOf(
        User(
          username = "sherlockholmes",
          code = "X54321",
          name = Name(forename = "Sherlock", surname = "Holmes"),
          teams = emptyList(),
        ),
      ),
    )

    val caseload = service.getStaffVaryCaseload(deliusStaffIdentifier)
    assertThat(caseload).hasSize(1)
    verifyCase(
      caseload[0],
      expectedCrn = "X12348",
      expectedPrisonerNumber = "AB1234E",
      expectedLicenceStatus = LicenceStatus.VARIATION_IN_PROGRESS,
      expectedLicenceType = LicenceType.AP,
      expectedProbationPractitioner = ProbationPractitioner(staffCode = "X54321", name = "Sherlock Holmes"),
    )
  }

  @Test
  fun `it builds the staff vary caseload with Review Needed status`() {
    val managedOffenders = listOf(
      ManagedOffenderCrn(
        crn = "X12348",
        staff = StaffDetail(name = Name(forename = "Joe", surname = "Bloggs"), code = "X1234"),
      ),
    )

    whenever(
      deliusApiClient.getManagedOffenders(deliusStaffIdentifier),
    ).thenReturn(managedOffenders)
    whenever(probationSearchApiClient.getOffendersByCrn(managedOffenders.map { it.crn })).thenReturn(
      listOf(
        createOffenderDetail(1L, nomsNumber = "AB1234E", crn = "X12348"),
      ),
    )

    whenever(caseloadService.getPrisonersByNumber(any())).thenReturn(
      listOf(
        createCaseloadItem(
          prisonerNumber = "AB1234E",
          conditionalReleaseDate = null,
          confirmedReleaseDate = tenDaysFromNow,
          licenceExpiryDate = LocalDate.of(
            2022,
            Month.DECEMBER,
            26,
          ),
          status = "INACTIVE OUT",
        ),
      ),
    )

    whenever(licenceService.findLicencesMatchingCriteria(any())).thenReturn(
      listOf(
        createLicenceSummary(
          crn = "X12348",
          nomisId = "AB1234E",
          kind = LicenceKind.HARD_STOP,
          licenceType = LicenceType.AP,
          licenceStatus = LicenceStatus.ACTIVE,
          licenceStartDate = tenDaysFromNow,
          comUsername = "sherlockholmes",
          isReviewNeeded = true,
        ),
      ),
    )

    whenever(deliusApiClient.getStaffDetailsByUsername(any())).thenReturn(
      listOf(
        User(
          username = "sherlockholmes",
          code = "X54321",
          name = Name(forename = "Sherlock", surname = "Holmes"),
          teams = emptyList(),
        ),
      ),
    )

    val caseload = service.getStaffVaryCaseload(deliusStaffIdentifier)

    assertThat(caseload).hasSize(1)
    verifyCase(
      caseload[0],
      expectedCrn = "X12348",
      expectedPrisonerNumber = "AB1234E",
      expectedLicenceStatus = LicenceStatus.ACTIVE,
      expectedLicenceType = LicenceType.AP,
      expectedReleaseDate = tenDaysFromNow,
      expectedProbationPractitioner = ProbationPractitioner(staffCode = "X54321", name = "Sherlock Holmes"),
      expectedReviewNeeded = true,
    )
  }

  @Test
  fun `it builds the team vary caseload`() {
    val selectedTeam = "team C"

    val managedOffenders = listOf(
      ManagedOffenderCrn(
        crn = "X12348",
        staff = StaffDetail(name = Name(forename = "Joe", surname = "Bloggs"), code = "X1234"),
      ),
      ManagedOffenderCrn(
        crn = "X12349",
        staff = StaffDetail(name = Name(forename = "Sherlock", surname = "Holmes"), code = "X54321"),
      ),
    )

    whenever(
      deliusApiClient.getManagedOffendersByTeam(selectedTeam),
    ).thenReturn(managedOffenders)

    whenever(probationSearchApiClient.getOffendersByCrn(managedOffenders.map { it.crn })).thenReturn(
      listOf(
        createOffenderDetail(1L, nomsNumber = "AB1234E", crn = "X12348"),
        createOffenderDetail(3L, nomsNumber = "AB1234F", crn = "X12349"),
      ),
    )

    whenever(caseloadService.getPrisonersByNumber(any())).thenReturn(
      listOf(
        createCaseloadItem(
          prisonerNumber = "AB1234E",
          conditionalReleaseDate = null,
          confirmedReleaseDate = tenDaysFromNow,
          status = "INACTIVE OUT",
        ),
        createCaseloadItem(
          prisonerNumber = "AB1234F",
          conditionalReleaseDate = null,
          confirmedReleaseDate = tenDaysFromNow,
          status = "INACTIVE OUT",
          licenceExpiryDate = LocalDate.of(2022, Month.DECEMBER, 26),
        ),
      ),
    )

    whenever(licenceService.findLicencesMatchingCriteria(any())).thenReturn(
      listOf(
        createLicenceSummary(
          crn = "X12348",
          nomisId = "AB1234E",
          kind = LicenceKind.VARIATION,
          licenceType = LicenceType.PSS,
          licenceStatus = LicenceStatus.VARIATION_IN_PROGRESS,
          licenceExpiryDate = elevenDaysFromNow,
          comUsername = "joebloggs",
          licenceStartDate = tenDaysFromNow,
        ),
        createLicenceSummary(
          crn = "X12349",
          nomisId = "AB1234F",
          kind = LicenceKind.VARIATION,
          licenceType = LicenceType.AP,
          licenceStatus = LicenceStatus.VARIATION_IN_PROGRESS,
          licenceExpiryDate = elevenDaysFromNow,
          comUsername = "sherlockholmes",
          licenceStartDate = tenDaysFromNow,
        ),
      ),
    )

    whenever(deliusApiClient.getStaffDetailsByUsername(any())).thenReturn(
      listOf(
        User(
          username = "sherlockholmes",
          code = "X54321",
          name = Name(forename = "Sherlock", surname = "Holmes"),
          teams = emptyList(),
        ),
        User(
          username = "joebloggs",
          code = "X1234",
          name = Name(forename = "Joe", surname = "Bloggs"),
          teams = emptyList(),
        ),
      ),
    )

    val caseload = service.getTeamVaryCaseload(listOf("team A", "team B"), listOf(selectedTeam))

    verify(deliusApiClient).getManagedOffendersByTeam("team C")
    assertThat(caseload).hasSize(2)
    verifyCase(
      caseload[0],
      expectedCrn = "X12348",
      expectedPrisonerNumber = "AB1234E",
      expectedLicenceType = LicenceType.PSS,
      expectedLicenceStatus = LicenceStatus.VARIATION_IN_PROGRESS,
      expectedReleaseDate = tenDaysFromNow,
      expectedProbationPractitioner = ProbationPractitioner(staffCode = "X1234", name = "Joe Bloggs"),
    )
    verifyCase(
      caseload[1],
      expectedCrn = "X12349",
      expectedPrisonerNumber = "AB1234F",
      expectedLicenceType = LicenceType.AP,
      expectedLicenceStatus = LicenceStatus.VARIATION_IN_PROGRESS,
      expectedReleaseDate = tenDaysFromNow,
      expectedProbationPractitioner = ProbationPractitioner(staffCode = "X54321", name = "Sherlock Holmes"),
    )
  }

  @Test
  fun `it builds the team vary caseload with Review Needed status`() {
    val managedOffenders = listOf(
      ManagedOffenderCrn(
        crn = "X12348",
        staff = StaffDetail(name = Name(forename = "Joe", surname = "Bloggs"), code = "X1234"),
      ),
    )

    whenever(
      deliusApiClient.getManagedOffenders(deliusStaffIdentifier),
    ).thenReturn(managedOffenders)

    whenever(probationSearchApiClient.getOffendersByCrn(managedOffenders.map { it.crn })).thenReturn(
      listOf(
        createOffenderDetail(1L, nomsNumber = "AB1234E", crn = "X12348"),
      ),
    )

    whenever(caseloadService.getPrisonersByNumber(any())).thenReturn(
      listOf(
        createCaseloadItem(
          prisonerNumber = "AB1234E",
          conditionalReleaseDate = null,
          confirmedReleaseDate = tenDaysFromNow,
          releaseDate = tenDaysFromNow,
          licenceExpiryDate = LocalDate.of(2022, Month.DECEMBER, 26),
          status = "INACTIVE OUT",
        ),
        createCaseloadItem(
          prisonerNumber = "AB1234F",
          conditionalReleaseDate = null,
          confirmedReleaseDate = tenDaysFromNow,
          releaseDate = tenDaysFromNow,
          status = "INACTIVE OUT",
          licenceExpiryDate = LocalDate.of(2022, Month.DECEMBER, 26),
        ),
      ),
    )

    whenever(licenceService.findLicencesMatchingCriteria(any())).thenReturn(
      listOf(
        createLicenceSummary(
          crn = "X12348",
          nomisId = "AB1234E",
          kind = LicenceKind.HARD_STOP,
          licenceType = LicenceType.AP,
          licenceStatus = LicenceStatus.ACTIVE,
          licenceExpiryDate = elevenDaysFromNow,
          comUsername = "sherlockholmes",
          licenceStartDate = LocalDate.now(),
          isReviewNeeded = true,
        ),
      ),
    )

    whenever(deliusApiClient.getStaffDetailsByUsername(any())).thenReturn(
      listOf(
        User(
          username = "sherlockholmes",
          code = "X54321",
          name = Name(forename = "Sherlock", surname = "Holmes"),
          teams = emptyList(),
        ),
      ),
    )

    val caseload = service.getStaffVaryCaseload(deliusStaffIdentifier)

    assertThat(caseload).hasSize(1)
    verifyCase(
      caseload[0],
      expectedCrn = "X12348",
      expectedPrisonerNumber = "AB1234E",
      expectedLicenceType = LicenceType.AP,
      expectedLicenceStatus = LicenceStatus.ACTIVE,
      expectedReleaseDate = LocalDate.now(),
      expectedProbationPractitioner = ProbationPractitioner(staffCode = "X54321", name = "Sherlock Holmes"),
      expectedReviewNeeded = true,
    )
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
  ): CaseloadItem = CaseloadItem(
    prisoner = Prisoner(
      prisonerNumber = prisonerNumber,
      conditionalReleaseDate = conditionalReleaseDate,
      firstName = "Bob",
      lastName = "Smith",
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
    cvl = CvlFields(licenceType = LicenceType.PSS),
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
  ) {
    with(case) {
      assertThat(crnNumber).isEqualTo(expectedCrn)
      assertThat(prisonerNumber).isEqualTo(expectedPrisonerNumber)
      assertThat(licenceStatus).isEqualTo(expectedLicenceStatus)
      assertThat(licenceType).isEqualTo(expectedLicenceType)
      expectedReleaseDate.let { assertThat(releaseDate).isEqualTo(expectedReleaseDate) }
      expectedProbationPractitioner.let { assertThat(probationPractitioner).isEqualTo(expectedProbationPractitioner) }
      assertThat(isReviewNeeded).isEqualTo(expectedReviewNeeded)
    }
  }

  private fun createOffenderDetail(id: Long, crn: String, nomsNumber: String? = null) = OffenderDetail(
    offenderId = id,
    OtherIds(nomsNumber = nomsNumber, crn = crn),
    offenderManagers = emptyList(),
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
}
