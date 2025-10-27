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

class ComVaryCaseloadServiceTest {
  private val deliusApiClient = mock<DeliusApiClient>()
  private val licenceService = mock<LicenceService>()
  private val releaseDateService = mock<ReleaseDateService>()

  private val service = ComVaryCaseloadService(deliusApiClient, licenceService, releaseDateService)

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
          licenceStatus = LicenceStatus.VARIATION_IN_PROGRESS,
          comUsername = "johndoe",
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
          licenceStatus = LicenceStatus.ACTIVE,
          comUsername = "johndoe",
        ),
        createLicenceSummary(
          crn = "X12348",
          nomisId = "AB1234E",
          kind = LicenceKind.CRD,
          licenceStatus = LicenceStatus.VARIATION_IN_PROGRESS,
          comUsername = "johndoe",
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
          licenceStatus = LicenceStatus.ACTIVE,
          licenceStartDate = tenDaysFromNow,
          comUsername = "johndoe",
          isReviewNeeded = true,
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
      ManagedOffenderCrn(
        crn = "X12350",
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
          forename = "ABCX XYZ",
        ),
        createLicenceSummary(
          crn = "X12349",
          nomisId = "AB1234F",
          kind = LicenceKind.VARIATION,
          licenceStatus = LicenceStatus.VARIATION_IN_PROGRESS,
          licenceExpiryDate = elevenDaysFromNow,
          comUsername = "johndoe",
          licenceStartDate = tenDaysFromNow,
          forename = "ABC XYZ",
        ),
        createLicenceSummary(
          crn = "X12350",
          nomisId = "AB1234G",
          kind = LicenceKind.VARIATION,
          licenceStatus = LicenceStatus.VARIATION_IN_PROGRESS,
          licenceExpiryDate = elevenDaysFromNow,
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
        StaffNameResponse(
          username = "joebloggs",
          code = "X1234",
          name = Name(forename = "Joe", surname = "Bloggs"),
          id = Long.MIN_VALUE,
        ),
      ),
    )

    val caseload = service.getTeamVaryCaseload(listOf("team A", "team B"), listOf(selectedTeam))

    verify(deliusApiClient).getManagedOffendersByTeam("team C")
    assertThat(caseload).hasSize(3)
    verifyCase(
      caseload[0],
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
    verifyCase(
      caseload[1],
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
      caseload[2],
      expectedCrn = "X12350",
      expectedPrisonerNumber = "AB1234G",
      expectedLicenceType = LicenceType.AP,
      expectedLicenceStatus = LicenceStatus.VARIATION_IN_PROGRESS,
      expectedReleaseDate = elevenDaysFromNow,
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
        StaffNameResponse(
          username = "johndoe",
          code = "X54321",
          name = Name(forename = "John", surname = "Doe"),
          id = Long.MIN_VALUE,
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
    licenceType: LicenceType = LicenceType.AP,
    licenceStatus: LicenceStatus,
    kind: LicenceKind = LicenceKind.CRD,
    licenceExpiryDate: LocalDate? = null,
    comUsername: String? = null,
    conditionalReleaseDate: LocalDate? = null,
    licenceStartDate: LocalDate? = null,
    isReviewNeeded: Boolean = false,
    forename: String? = null,
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
    conditionalReleaseDate = conditionalReleaseDate,
    actualReleaseDate = null,
    licenceStartDate = licenceStartDate,
    dateCreated = LocalDateTime.now(),
    updatedByFullName = "X Y",
    bookingId = null,
    dateOfBirth = null,
    forename = forename,
    surname = null,
    prisonCode = null,
    prisonDescription = null,
    probationLauCode = null,
    probationPduCode = null,
    probationAreaCode = null,
    probationTeamCode = null,
  )
}
