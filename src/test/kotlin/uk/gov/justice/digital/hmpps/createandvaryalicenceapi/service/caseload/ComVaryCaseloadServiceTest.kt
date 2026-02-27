package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ComVaryCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ProbationPractitioner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceCaseRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.model.LicenceComCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CaseloadType.ComVaryStaffCaseload
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CaseloadType.ComVaryTeamCaseload
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TelemetryService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.com.ComVaryCaseloadService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ManagedOffenderCrn
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.Name
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.StaffDetail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.response.CaseAccessResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.VARIATION
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.VARIATION_IN_PROGRESS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate
import java.time.LocalDateTime

class ComVaryCaseloadServiceTest {
  private val deliusApiClient = mock<DeliusApiClient>()
  private val licenceCaseRepository = mock<LicenceCaseRepository>()
  private val telemetryService = mock<TelemetryService>()

  private val service =
    ComVaryCaseloadService(deliusApiClient, licenceCaseRepository, telemetryService)

  private val elevenDaysFromNow = LocalDate.now().plusDays(11)
  private val tenDaysFromNow = LocalDate.now().plusDays(10)
  private val deliusStaffIdentifier = 213L

  @BeforeEach
  fun reset() {
    val authentication = org.mockito.kotlin.mock<Authentication>()
    val securityContext = org.mockito.kotlin.mock<SecurityContext>()

    whenever(authentication.name).thenReturn("Test Username")
    whenever(securityContext.authentication).thenReturn(authentication)
    SecurityContextHolder.setContext(securityContext)

    reset(deliusApiClient, licenceCaseRepository)
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

    whenever(licenceCaseRepository.findLicenceCasesForCom(any(), any())).thenReturn(
      listOf(
        createLicenceComCase(
          crn = "X12348",
          nomisId = "AB1234E",
          kind = LicenceKind.CRD,
          licenceStatus = VARIATION_IN_PROGRESS,
        ),
      ),
    )

    val caseload = service.getStaffVaryCaseload(deliusStaffIdentifier)
    assertThat(caseload).hasSize(1)
    verifyCase(
      caseload[0],
      expectedCrn = "X12348",
      expectedPrisonerNumber = "AB1234E",
      expectedLicenceStatus = VARIATION_IN_PROGRESS,
      expectedLicenceType = LicenceType.AP,
      expectedProbationPractitioner = ProbationPractitioner(
        staffCode = "X1234",
        name = "Joe Bloggs",
        allocated = true,
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

    whenever(licenceCaseRepository.findLicenceCasesForCom(any(), any())).thenReturn(
      listOf(
        createLicenceComCase(
          crn = "X12348",
          nomisId = "AB1234E",
          kind = LicenceKind.CRD,
          licenceStatus = LicenceStatus.ACTIVE,
        ),
        createLicenceComCase(
          crn = "X12348",
          nomisId = "AB1234E",
          kind = LicenceKind.CRD,
          licenceStatus = VARIATION_IN_PROGRESS,
        ),
      ),
    )

    val caseload = service.getStaffVaryCaseload(deliusStaffIdentifier)
    assertThat(caseload).hasSize(1)
    verifyCase(
      caseload[0],
      expectedCrn = "X12348",
      expectedPrisonerNumber = "AB1234E",
      expectedLicenceStatus = VARIATION_IN_PROGRESS,
      expectedLicenceType = LicenceType.AP,
      expectedProbationPractitioner = ProbationPractitioner(
        staffCode = "X1234",
        name = "Joe Bloggs",
        allocated = true,
      ),
    )
  }

  @Test
  fun `telemetry is captured for staff`() {
    val managedOffenders = listOf(
      ManagedOffenderCrn(
        crn = "X12348",
        staff = StaffDetail(code = "X1234", name = Name(forename = "Joe", surname = "Bloggs")),
      ),
    )

    whenever(deliusApiClient.getManagedOffenders(deliusStaffIdentifier)).thenReturn(managedOffenders)

    whenever(licenceCaseRepository.findLicenceCasesForCom(any(), any())).thenReturn(
      listOf(
        createLicenceComCase(
          crn = "X12348",
          nomisId = "AB1234E",
          kind = LicenceKind.CRD,
          licenceStatus = LicenceStatus.ACTIVE,
        ),
        createLicenceComCase(
          crn = "X12348",
          nomisId = "AB1234E",
          kind = LicenceKind.CRD,
          licenceStatus = VARIATION_IN_PROGRESS,
        ),
      ),
    )

    val caseload = service.getStaffVaryCaseload(deliusStaffIdentifier)

    verify(telemetryService).recordCaseloadLoad(
      eq(ComVaryStaffCaseload),
      eq(setOf(deliusStaffIdentifier.toString())),
      eq(caseload),
    )
  }

  @Test
  fun `telemetry is captured for teams`() {
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

    whenever(licenceCaseRepository.findLicenceCasesForCom(any(), any())).thenReturn(
      listOf(
        createLicenceComCase(
          crn = "X12348",
          nomisId = "AB1234E",
          kind = VARIATION,
          licenceStatus = VARIATION_IN_PROGRESS,
          licenceStartDate = tenDaysFromNow,
          forename = "ABCX XYZ",
        ),
        createLicenceComCase(
          crn = "X12349",
          nomisId = "AB1234F",
          kind = VARIATION,
          licenceStatus = VARIATION_IN_PROGRESS,
          licenceStartDate = tenDaysFromNow,
          forename = "ABC XYZ",
        ),
        createLicenceComCase(
          crn = "X12350",
          nomisId = "AB1234G",
          kind = VARIATION,
          licenceStatus = VARIATION_IN_PROGRESS,
          licenceStartDate = elevenDaysFromNow,
        ),
      ),
    )

    val caseload = service.getTeamVaryCaseload(listOf("team A", "team B"), listOf(selectedTeam))

    verify(telemetryService).recordCaseloadLoad(
      eq(ComVaryTeamCaseload),
      eq(setOf(selectedTeam)),
      eq(caseload),
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

    whenever(licenceCaseRepository.findLicenceCasesForCom(any(), any())).thenReturn(
      listOf(
        createLicenceComCase(
          crn = "X12348",
          nomisId = "AB1234E",
          kind = LicenceKind.HARD_STOP,
          licenceStatus = LicenceStatus.ACTIVE,
          licenceStartDate = tenDaysFromNow,
          reviewDate = null,
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
        staffCode = "X1234",
        name = "Joe Bloggs",
        allocated = true,
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

    whenever(licenceCaseRepository.findLicenceCasesForCom(any(), any())).thenReturn(
      listOf(
        createLicenceComCase(
          crn = "X12348",
          nomisId = "AB1234E",
          kind = VARIATION,
          typeCode = LicenceType.PSS,
          licenceStatus = VARIATION_IN_PROGRESS,
          licenceStartDate = tenDaysFromNow,
          forename = "ABCX XYZ",
        ),
        createLicenceComCase(
          crn = "X12349",
          nomisId = "AB1234F",
          kind = VARIATION,
          typeCode = LicenceType.AP,
          licenceStatus = VARIATION_IN_PROGRESS,
          licenceStartDate = tenDaysFromNow,
          forename = "ABC XYZ",
        ),
        createLicenceComCase(
          crn = "X12350",
          nomisId = "AB1234G",
          kind = VARIATION,
          licenceStatus = VARIATION_IN_PROGRESS,
          licenceStartDate = elevenDaysFromNow,
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
      expectedLicenceStatus = VARIATION_IN_PROGRESS,
      expectedReleaseDate = tenDaysFromNow,
      expectedProbationPractitioner = ProbationPractitioner(
        staffCode = "X54321",
        name = "John Doe",
        allocated = true,
      ),
    )
    verifyCase(
      caseload[1],
      expectedCrn = "X12348",
      expectedPrisonerNumber = "AB1234E",
      expectedLicenceType = LicenceType.PSS,
      expectedLicenceStatus = VARIATION_IN_PROGRESS,
      expectedReleaseDate = tenDaysFromNow,
      expectedProbationPractitioner = ProbationPractitioner(
        staffCode = "X1234",
        name = "Joe Bloggs",
        allocated = true,
      ),
    )
    verifyCase(
      caseload[2],
      expectedCrn = "X12350",
      expectedPrisonerNumber = "AB1234G",
      expectedLicenceType = LicenceType.AP,
      expectedLicenceStatus = VARIATION_IN_PROGRESS,
      expectedReleaseDate = elevenDaysFromNow,
      expectedProbationPractitioner = ProbationPractitioner(
        staffCode = "X54321",
        name = "John Doe",
        allocated = true,
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

    whenever(licenceCaseRepository.findLicenceCasesForCom(any(), any())).thenReturn(
      listOf(
        createLicenceComCase(
          crn = "X12348",
          nomisId = "AB1234E",
          kind = LicenceKind.HARD_STOP,
          licenceStatus = LicenceStatus.ACTIVE,
          licenceStartDate = LocalDate.now(),
          reviewDate = null,
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
        staffCode = "X1234",
        name = "Joe Bloggs",
        allocated = true,
      ),
      expectedReviewNeeded = true,
    )
  }

  @Test
  fun `does not check Delius user access when laoEnabled is false`() {
    val managedOffenders = listOf(
      ManagedOffenderCrn(
        crn = "X12348",
        staff = StaffDetail(code = "X1234", name = Name(forename = "Joe", surname = "Bloggs")),
      ),
    )

    whenever(deliusApiClient.getManagedOffenders(deliusStaffIdentifier)).thenReturn(managedOffenders)

    whenever(licenceCaseRepository.findLicenceCasesForCom(any(), any())).thenReturn(
      listOf(
        createLicenceComCase(
          crn = "X12348",
          nomisId = "AB1234E",
          kind = VARIATION,
          licenceStatus = VARIATION_IN_PROGRESS,
        ),
      ),
    )

    val caseload = service.getStaffVaryCaseload(deliusStaffIdentifier)

    assertThat(caseload).hasSize(1)
    assertThat(caseload[0].isRestricted).isFalse()
    verify(deliusApiClient, times(0)).getCheckUserAccess(any(), any(), any())
  }

  @Test
  fun `LAO cases are returned as restricted in the caseload when they are excluded`() {
    val managedOffenders = listOf(
      ManagedOffenderCrn(
        crn = "X12348",
        staff = StaffDetail(code = "X1234", name = Name(forename = "Joe", surname = "Bloggs")),
      ),
    )

    val serviceWithLaoEnabled = ComVaryCaseloadService(
      deliusApiClient,
      licenceCaseRepository,
      telemetryService,
      laoEnabled = true,
    )

    whenever(deliusApiClient.getManagedOffenders(deliusStaffIdentifier)).thenReturn(managedOffenders)

    whenever(licenceCaseRepository.findLicenceCasesForCom(any(), any())).thenReturn(
      listOf(
        createLicenceComCase(
          crn = "X12348",
          nomisId = "AB1234E",
          kind = VARIATION,
          licenceStatus = VARIATION_IN_PROGRESS,
        ),
      ),
    )

    whenever(deliusApiClient.getCheckUserAccess(any(), eq(listOf("X12348")), any())).thenReturn(
      listOf(
        CaseAccessResponse(crn = "X12348", userExcluded = true, userRestricted = false),
      ),
    )

    val caseload = serviceWithLaoEnabled.getStaffVaryCaseload(deliusStaffIdentifier)

    assertThat(caseload).hasSize(1)
    assertThat(caseload[0].isRestricted).isTrue()
    assertThat(caseload[0].probationPractitioner).isEqualTo(ProbationPractitioner.restrictedView())
  }

  @Test
  fun `LAO cases are returned as restricted in the caseload when they are restricted`() {
    val managedOffenders = listOf(
      ManagedOffenderCrn(
        crn = "X12348",
        staff = StaffDetail(code = "X1234", name = Name(forename = "Joe", surname = "Bloggs")),
      ),
    )

    val serviceWithLaoEnabled = ComVaryCaseloadService(
      deliusApiClient,
      licenceCaseRepository,
      telemetryService,
      laoEnabled = true,
    )

    whenever(deliusApiClient.getManagedOffenders(deliusStaffIdentifier)).thenReturn(managedOffenders)

    whenever(licenceCaseRepository.findLicenceCasesForCom(any(), any())).thenReturn(
      listOf(
        createLicenceComCase(
          crn = "X12348",
          nomisId = "AB1234E",
          kind = VARIATION,
          licenceStatus = VARIATION_IN_PROGRESS,
        ),
      ),
    )

    whenever(deliusApiClient.getCheckUserAccess(any(), eq(listOf("X12348")), any())).thenReturn(
      listOf(
        CaseAccessResponse(crn = "X12348", userExcluded = false, userRestricted = true),
      ),
    )

    val caseload = serviceWithLaoEnabled.getStaffVaryCaseload(deliusStaffIdentifier)

    assertThat(caseload).hasSize(1)
    assertThat(caseload[0].isRestricted).isTrue()
    assertThat(caseload[0].probationPractitioner).isEqualTo(ProbationPractitioner.restrictedView())
  }

  @Test
  fun `Cases are not returned as restricted where they are not excluded or restricted`() {
    val managedOffenders = listOf(
      ManagedOffenderCrn(
        crn = "X12348",
        staff = StaffDetail(code = "X1234", name = Name(forename = "Joe", surname = "Bloggs")),
      ),
    )

    val serviceWithLaoEnabled = ComVaryCaseloadService(
      deliusApiClient,
      licenceCaseRepository,
      telemetryService,
      laoEnabled = true,
    )

    whenever(deliusApiClient.getManagedOffenders(deliusStaffIdentifier)).thenReturn(managedOffenders)

    whenever(licenceCaseRepository.findLicenceCasesForCom(any(), any())).thenReturn(
      listOf(
        createLicenceComCase(
          crn = "X12348",
          nomisId = "AB1234E",
          kind = VARIATION,
          licenceStatus = VARIATION_IN_PROGRESS,
        ),
      ),
    )

    whenever(deliusApiClient.getCheckUserAccess(any(), eq(listOf("X12348")), any())).thenReturn(
      listOf(
        CaseAccessResponse(crn = "X12348", userExcluded = false, userRestricted = false),
      ),
    )

    val caseload = serviceWithLaoEnabled.getStaffVaryCaseload(deliusStaffIdentifier)

    assertThat(caseload).hasSize(1)
    assertThat(caseload[0].isRestricted).isFalse()
    assertThat(caseload[0].probationPractitioner).isEqualTo(
      ProbationPractitioner(
        staffCode = "X1234",
        name = "Joe Bloggs",
        allocated = true,
      ),
    )
  }

  @Test
  fun `Team caseload with LAO restrictions is handled correctly`() {
    val selectedTeam = "team C"

    val managedOffenders = listOf(
      ManagedOffenderCrn(
        crn = "X12348",
        staff = StaffDetail(code = "X1234", name = Name(forename = "Joe", surname = "Bloggs")),
      ),
    )

    val serviceWithLaoEnabled = ComVaryCaseloadService(
      deliusApiClient,
      licenceCaseRepository,
      telemetryService,
      laoEnabled = true,
    )

    whenever(deliusApiClient.getManagedOffendersByTeam(selectedTeam)).thenReturn(managedOffenders)

    whenever(licenceCaseRepository.findLicenceCasesForCom(any(), any())).thenReturn(
      listOf(
        createLicenceComCase(
          crn = "X12348",
          nomisId = "AB1234E",
          kind = VARIATION,
          licenceStatus = VARIATION_IN_PROGRESS,
        ),
      ),
    )

    whenever(deliusApiClient.getCheckUserAccess(any(), eq(listOf("X12348")), any())).thenReturn(
      listOf(
        CaseAccessResponse(crn = "X12348", userExcluded = true, userRestricted = false),
      ),
    )

    val caseload = serviceWithLaoEnabled.getTeamVaryCaseload(listOf("team A"), listOf(selectedTeam))

    assertThat(caseload).hasSize(1)
    assertThat(caseload[0].isRestricted).isTrue()
  }

  private fun verifyCase(
    case: ComVaryCase,
    expectedCrn: String,
    expectedPrisonerNumber: String,
    expectedLicenceStatus: LicenceStatus,
    expectedLicenceType: LicenceType,
    expectedReleaseDate: LocalDate? = null,
    expectedProbationPractitioner: ProbationPractitioner = ProbationPractitioner("X1234", "Joe Bloggs", true),
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

  private fun createLicenceComCase(
    crn: String,
    nomisId: String,
    typeCode: LicenceType = LicenceType.AP,
    licenceStatus: LicenceStatus,
    kind: LicenceKind = LicenceKind.CRD,
    prisonCode: String = "MDI",
    sentenceStartDate: LocalDate? = null,
    conditionalReleaseDate: LocalDate? = null,
    confirmedReleaseDate: LocalDate? = null,
    licenceStartDate: LocalDate? = null,
    versionOfId: Long? = null,
    reviewDate: LocalDateTime? = LocalDateTime.now(),
    forename: String = "forename",
    surname: String = "surname",
  ) = LicenceComCase(
    crn = crn,
    prisonNumber = nomisId,
    kind = kind,
    licenceId = 1,
    typeCode = typeCode,
    statusCode = licenceStatus,
    comUsername = null,
    sentenceStartDate = sentenceStartDate,
    conditionalReleaseDate = conditionalReleaseDate,
    actualReleaseDate = confirmedReleaseDate,
    licenceStartDate = licenceStartDate,
    forename = forename,
    surname = surname,
    versionOfId = versionOfId,
    postRecallReleaseDate = LocalDate.now(),
    homeDetentionCurfewActualDate = LocalDate.now(),
    updatedByFirstName = "firstName",
    updatedByLastName = "lastName",
    reviewDate = reviewDate,
    prisonCode = prisonCode,
  )
}
