package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceCaseRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.model.LicenceApproverCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.time.LocalDate
import java.time.LocalDateTime

class PrisonApproverServiceTest {

  private val licenceService = mock<LicenceService>()
  private val licenceCaseRepository = mock<LicenceCaseRepository>()

  private val service = PrisonApproverService(
    licenceService,
    licenceCaseRepository
  )

  @BeforeEach
  fun reset() {
    val authentication = mock<Authentication>()
    val securityContext = mock<SecurityContext>()

    whenever(authentication.name).thenReturn("tcom")
    whenever(securityContext.authentication).thenReturn(authentication)
    SecurityContextHolder.setContext(securityContext)

    reset(licenceService, licenceCaseRepository)
  }

  @Test
  fun `find recently approved licences matching criteria - returns recently approved licences`() {
    val case = licenceApproverCase(
      licenceStatus = LicenceStatus.APPROVED,
      approvedByName = "Jim Smith",
      approvedDate = LocalDateTime.of(2023, 1, 2, 3, 40),
    )

    whenever(licenceCaseRepository.findRecentlyApprovedLicenceCasesAfter(anyList(), any()))
      .thenReturn(listOf(case))

    val licenceSummaries = service.findRecentlyApprovedLicenceCases(emptyList())

    assertThat(licenceSummaries).hasSize(1)
    val summary = licenceSummaries.first()
    assertThat(summary.licenceStatus).isEqualTo(LicenceStatus.APPROVED)
    assertThat(summary.approvedDate).isEqualTo(LocalDateTime.of(2023, 1, 2, 3, 40))
    verify(licenceCaseRepository).findRecentlyApprovedLicenceCasesAfter(anyList(), any())
  }

  @Test
  fun `find recently approved PRRD licence - returns recently approved PRRD licences`() {
    val case = licenceApproverCase(
      kind = LicenceKind.PRRD,
      licenceStatus = LicenceStatus.APPROVED,
      approvedByName = "Jim Smith",
      approvedDate = LocalDateTime.of(2023, 1, 2, 3, 40),
    )

    whenever(licenceCaseRepository.findRecentlyApprovedLicenceCasesAfter(anyList(), any()))
      .thenReturn(listOf(case))

    val licenceSummaries = service.findRecentlyApprovedLicenceCases(emptyList())

    assertThat(licenceSummaries).isNotEmpty
    val licence = licenceSummaries.first()
    assertThat(licence.kind).isEqualTo(LicenceKind.PRRD)
    assertThat(licence.licenceStatus).isEqualTo(LicenceStatus.APPROVED)
  }

  @Nested
  inner class `getting licences for approval` {

    @Test
    fun `Get licences for approval returns correct approved licence summary`() {
      val prisons = listOf("MDI")
      val case = licenceApproverCase(
        licenceStatus = LicenceStatus.SUBMITTED,
        licenceStartDate = LocalDate.of(2022, 7, 27),
      )

      whenever(licenceCaseRepository.findLicenceCasesReadyForApproval(prisons)).thenReturn(listOf(case))

      val approvedLicenceSummaries = service.getLicenceCasesReadyForApproval(prisons)

      verify(licenceCaseRepository).findLicenceCasesReadyForApproval(prisons)
      assertThat(approvedLicenceSummaries).hasSize(1)
      val summary = approvedLicenceSummaries.first()
      assertThat(summary.licenceStatus).isEqualTo(LicenceStatus.SUBMITTED)
      assertThat(summary.prisonCode).isEqualTo("MDI")
    }

    @Test
    fun `No prison codes when getting licences for approval returns early`() {
      val response = service.getLicenceCasesReadyForApproval(emptyList())
      verifyNoInteractions(licenceCaseRepository)
      assertThat(response).isEmpty()
    }
  }

  private fun licenceApproverCase(
    licenceStartDate: LocalDate? = LocalDate.now(),
    kind: LicenceKind = LicenceKind.CRD,
    licenceId: Long = 1L,
    versionOfId: Long? = null,
    licenceStatus: LicenceStatus = LicenceStatus.SUBMITTED,
    prisonNumber: String = "A1234AA",
    surname: String? = "Smith",
    forename: String? = "John",
    updatedByFirstName: String? = "X",
    updatedByLastName: String? = "Y",
    comUsername: String? = "tcom",
    conditionalReleaseDate: LocalDate? = LocalDate.now(),
    actualReleaseDate: LocalDate? = LocalDate.now(),
    postRecallReleaseDate: LocalDate? = null,
    approvedByName: String? = null,
    approvedDate: LocalDateTime? = null,
    submittedByFullName: String? = "X Y",
    prisonCode: String? = "MDI",
    prisonDescription: String? = "Moorland (HMP)",
    variationOfId: Long? = null,
  ): LicenceApproverCase {
    val licenceApproverCase = LicenceApproverCase(
      licenceStartDate = licenceStartDate,
      kind = kind,
      licenceId = licenceId,
      versionOfId = versionOfId,
      licenceStatus = licenceStatus,
      prisonNumber = prisonNumber,
      surname = surname,
      forename = forename,
      updatedByFirstName = updatedByFirstName,
      updatedByLastName = updatedByLastName,
      comUsername = comUsername,
      conditionalReleaseDate = conditionalReleaseDate,
      actualReleaseDate = actualReleaseDate,
      postRecallReleaseDate = postRecallReleaseDate,
      approvedByName = approvedByName,
      approvedDate = approvedDate,
      prisonCode = prisonCode,
      prisonDescription = prisonDescription,
      variationOfId = variationOfId,
    )
    licenceApproverCase.submittedByFullName = submittedByFullName
    return licenceApproverCase
  }
}
