package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ComCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ProbationPractitioner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ManagedOffenderCrn
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.Name
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.StaffDetail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.User
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate
import java.time.LocalDateTime

class ComVaryCaseloadServiceTest {
  private val deliusApiClient = mock<DeliusApiClient>()
  private val licenceService = mock<LicenceService>()

  private val service = ComVaryCaseloadService(deliusApiClient, licenceService)

  private val elevenDaysFromNow = LocalDate.now().plusDays(11)
  private val tenDaysFromNow = LocalDate.now().plusDays(10)
  private val deliusStaffIdentifier = 213L

  @BeforeEach
  fun reset() {
    reset(deliusApiClient, licenceService)
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

    whenever(licenceService.findLicencesForCrnsAndStatuses(any(), any())).thenReturn(
      listOf(
        createLicenceSummary(
          crn = "X12348",
          nomisId = "AB1234E",
          kind = LicenceKind.CRD,
          licenceType = LicenceType.AP,
          licenceStatus = LicenceStatus.VARIATION_IN_PROGRESS,
          comUsername = "johndoe",
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

    val caseload = service.getStaffVaryCaseload(deliusStaffIdentifier)
    assertThat(caseload).hasSize(1)
    verifyCase(
      caseload[0],
      expectedCrn = "X12348",
      expectedPrisonerNumber = "AB1234E",
      expectedLicenceStatus = LicenceStatus.VARIATION_IN_PROGRESS,
      expectedLicenceType = LicenceType.AP,
      expectedProbationPractitioner = ProbationPractitioner(
        staffCode = "X54321",
        name = "John Doe",
        staffUsername = "johndoe",
      ),
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

    whenever(licenceService.findLicencesForCrnsAndStatuses(any(), any())).thenReturn(
      listOf(
        createLicenceSummary(
          crn = "X12348",
          nomisId = "AB1234E",
          kind = LicenceKind.CRD,
          licenceType = LicenceType.AP,
          licenceStatus = LicenceStatus.ACTIVE,
          comUsername = "johndoe",
        ),
        createLicenceSummary(
          crn = "X12348",
          nomisId = "AB1234E",
          kind = LicenceKind.CRD,
          licenceType = LicenceType.AP,
          licenceStatus = LicenceStatus.VARIATION_IN_PROGRESS,
          comUsername = "johndoe",
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

    val caseload = service.getStaffVaryCaseload(deliusStaffIdentifier)
    assertThat(caseload).hasSize(1)
    verifyCase(
      caseload[0],
      expectedCrn = "X12348",
      expectedPrisonerNumber = "AB1234E",
      expectedLicenceStatus = LicenceStatus.VARIATION_IN_PROGRESS,
      expectedLicenceType = LicenceType.AP,
      expectedProbationPractitioner = ProbationPractitioner(
        staffCode = "X54321",
        name = "John Doe",
        staffUsername = "johndoe",
      ),
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

    whenever(licenceService.findLicencesForCrnsAndStatuses(any(), any())).thenReturn(
      listOf(
        createLicenceSummary(
          crn = "X12348",
          nomisId = "AB1234E",
          kind = LicenceKind.HARD_STOP,
          licenceType = LicenceType.AP,
          licenceStatus = LicenceStatus.ACTIVE,
          licenceStartDate = tenDaysFromNow,
          comUsername = "johndoe",
          isReviewNeeded = true,
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

    val caseload = service.getStaffVaryCaseload(deliusStaffIdentifier)

    assertThat(caseload).hasSize(1)
    verifyCase(
      caseload[0],
      expectedCrn = "X12348",
      expectedPrisonerNumber = "AB1234E",
      expectedLicenceStatus = LicenceStatus.ACTIVE,
      expectedLicenceType = LicenceType.AP,
      expectedReleaseDate = tenDaysFromNow,
      expectedProbationPractitioner = ProbationPractitioner(
        staffCode = "X54321",
        name = "John Doe",
        staffUsername = "johndoe",
      ),
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
        staff = StaffDetail(name = Name(forename = "John", surname = "Doe"), code = "X54321"),
      ),
    )

    whenever(
      deliusApiClient.getManagedOffendersByTeam(selectedTeam),
    ).thenReturn(managedOffenders)

    whenever(licenceService.findLicencesForCrnsAndStatuses(any(), any())).thenReturn(
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
          comUsername = "johndoe",
          licenceStartDate = tenDaysFromNow,
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
      expectedProbationPractitioner = ProbationPractitioner(
        staffCode = "X1234",
        name = "Joe Bloggs",
        staffUsername = "joebloggs",
      ),
    )
    verifyCase(
      caseload[1],
      expectedCrn = "X12349",
      expectedPrisonerNumber = "AB1234F",
      expectedLicenceType = LicenceType.AP,
      expectedLicenceStatus = LicenceStatus.VARIATION_IN_PROGRESS,
      expectedReleaseDate = tenDaysFromNow,
      expectedProbationPractitioner = ProbationPractitioner(
        staffCode = "X54321",
        name = "John Doe",
        staffUsername = "johndoe",
      ),
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

    whenever(licenceService.findLicencesForCrnsAndStatuses(any(), any())).thenReturn(
      listOf(
        createLicenceSummary(
          crn = "X12348",
          nomisId = "AB1234E",
          kind = LicenceKind.HARD_STOP,
          licenceType = LicenceType.AP,
          licenceStatus = LicenceStatus.ACTIVE,
          licenceExpiryDate = elevenDaysFromNow,
          comUsername = "johndoe",
          licenceStartDate = LocalDate.now(),
          isReviewNeeded = true,
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

    val caseload = service.getStaffVaryCaseload(deliusStaffIdentifier)

    assertThat(caseload).hasSize(1)
    verifyCase(
      caseload[0],
      expectedCrn = "X12348",
      expectedPrisonerNumber = "AB1234E",
      expectedLicenceType = LicenceType.AP,
      expectedLicenceStatus = LicenceStatus.ACTIVE,
      expectedReleaseDate = LocalDate.now(),
      expectedProbationPractitioner = ProbationPractitioner(
        staffCode = "X54321",
        name = "John Doe",
        staffUsername = "johndoe",
      ),
      expectedReviewNeeded = true,
    )
  }

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
