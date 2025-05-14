package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceSummaryApproverView
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.ca
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.com
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createHdcVariationLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createVariationLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.StandardCondition as EntityStandardCondition

class PrisonApproverServiceTest {
  private val licenceRepository = mock<LicenceRepository>()
  private val releaseDateService = mock<ReleaseDateService>()

  private val service =
    PrisonApproverService(
      licenceRepository,
      releaseDateService,
    )

  @BeforeEach
  fun reset() {
    val authentication = mock<Authentication>()
    val securityContext = mock<SecurityContext>()

    whenever(authentication.name).thenReturn("smills")
    whenever(securityContext.authentication).thenReturn(authentication)
    SecurityContextHolder.setContext(securityContext)

    reset(
      licenceRepository,
      releaseDateService,
    )
  }

  @Test
  fun `find recently approved licences matching criteria - returns recently approved licences`() {
    whenever(licenceRepository.getRecentlyApprovedLicences(any(), any())).thenReturn(
      listOf(
        aRecentlyApprovedLicence.copy(
          statusCode = LicenceStatus.APPROVED,
          submittedDate = LocalDateTime.of(2023, 1, 2, 3, 40),
          submittedBy = com(),
          updatedBy = com(),
        ),
      ),
    )

    val licenceSummaries = service.findRecentlyApprovedLicences(emptyList())

    assertThat(licenceSummaries).isEqualTo(
      listOf(
        aRecentlyApprovedLicenceSummary.copy(
          licenceStatus = LicenceStatus.APPROVED,
          submittedDate = LocalDateTime.of(2023, 1, 2, 3, 40),
        ),
      ),
    )
    verify(licenceRepository, times(1)).getRecentlyApprovedLicences(
      anyList(),
      any<LocalDate>(),
    )
  }

  @Test
  fun `find recently approved licences matching criteria - returns the original licence for an active variation`() {
    val aRecentlyApprovedLicence = TestData.createCrdLicence().copy(
      id = 1,
      actualReleaseDate = LocalDate.now().minusDays(1),
      conditionalReleaseDate = LocalDate.now(),
      approvedByName = "jim smith",
      statusCode = LicenceStatus.INACTIVE,
      approvedDate = LocalDateTime.of(2023, 9, 19, 16, 38, 42),
      submittedBy = com(),
      updatedBy = com(),
      submittedDate = LocalDateTime.of(2023, 9, 19, 16, 38, 42),
    )

    val activeVariationLicence = createVariationLicence().copy(
      id = aRecentlyApprovedLicence.id + 1,
      statusCode = LicenceStatus.ACTIVE,
      variationOfId = aRecentlyApprovedLicence.id,
    )

    whenever(licenceRepository.findById(aRecentlyApprovedLicence.id)).thenReturn(
      Optional.of(
        aRecentlyApprovedLicence,
      ),
    )
    whenever(
      licenceRepository.getRecentlyApprovedLicences(
        anyList(),
        any<LocalDate>(),
      ),
    ).thenReturn(
      listOf(
        activeVariationLicence,
      ),
    )

    val licenceSummaries = service.findRecentlyApprovedLicences(emptyList())

    assertThat(licenceSummaries).isEqualTo(
      listOf(
        aRecentlyApprovedLicenceSummary.copy(
          surname = activeVariationLicence.surname,
          forename = activeVariationLicence.forename,
          licenceStatus = LicenceStatus.INACTIVE,
          submittedByFullName = com().fullName,
          updatedByFullName = com().fullName,
          submittedDate = LocalDateTime.of(2023, 9, 19, 16, 38, 42),
        ),
      ),
    )
    verify(licenceRepository, times(1)).getRecentlyApprovedLicences(anyList(), any<LocalDate>())
  }

  @Test
  fun `find recently approved licences matching criteria - returns the original licence for an hardstop`() {
    val aRecentlyApprovedLicence = TestData.createHardStopLicence().copy(
      id = 1,
      actualReleaseDate = LocalDate.now().minusDays(1),
      conditionalReleaseDate = LocalDate.now(),
      approvedByName = "jim smith",
      statusCode = LicenceStatus.INACTIVE,
      approvedDate = LocalDateTime.of(2023, 9, 19, 16, 38, 42),
      submittedBy = ca(),
      updatedBy = com(),
      submittedDate = LocalDateTime.of(2023, 9, 19, 16, 38, 42),
    )

    val activeVariationLicence = createVariationLicence().copy(
      id = aRecentlyApprovedLicence.id + 1,
      statusCode = LicenceStatus.ACTIVE,
      variationOfId = aRecentlyApprovedLicence.id,
    )

    whenever(licenceRepository.findById(aRecentlyApprovedLicence.id)).thenReturn(
      Optional.of(
        aRecentlyApprovedLicence,
      ),
    )
    whenever(
      licenceRepository.getRecentlyApprovedLicences(
        anyList(),
        any<LocalDate>(),
      ),
    ).thenReturn(
      listOf(
        activeVariationLicence,
      ),
    )

    val licenceSummaries = service.findRecentlyApprovedLicences(emptyList())

    assertThat(licenceSummaries).isEqualTo(
      listOf(
        aRecentlyApprovedLicenceSummary.copy(
          kind = LicenceKind.HARD_STOP,
          surname = activeVariationLicence.surname,
          forename = activeVariationLicence.forename,
          licenceStatus = LicenceStatus.INACTIVE,
          submittedByFullName = com().fullName,
          updatedByFullName = com().fullName,
          submittedDate = LocalDateTime.of(2023, 9, 19, 16, 38, 42),
        ),
      ),
    )
    verify(licenceRepository, times(1)).getRecentlyApprovedLicences(anyList(), any<LocalDate>())
  }

  @Test
  fun `find recently approved licences matching criteria - returns the original licence for a HDC licence`() {
    val aRecentlyApprovedLicence = TestData.createHdcLicence().copy(
      id = 1,
      actualReleaseDate = LocalDate.now().minusDays(1),
      conditionalReleaseDate = LocalDate.now(),
      approvedByName = "jim smith",
      statusCode = LicenceStatus.INACTIVE,
      approvedDate = LocalDateTime.of(2023, 9, 19, 16, 38, 42),
      submittedBy = com(),
      updatedBy = com(),
      submittedDate = LocalDateTime.of(2023, 9, 19, 16, 38, 42),
    )

    val activeVariationLicence = createHdcVariationLicence().copy(
      id = aRecentlyApprovedLicence.id + 1,
      statusCode = LicenceStatus.ACTIVE,
      variationOfId = aRecentlyApprovedLicence.id,
    )

    whenever(licenceRepository.findById(aRecentlyApprovedLicence.id)).thenReturn(
      Optional.of(
        aRecentlyApprovedLicence,
      ),
    )
    whenever(
      licenceRepository.getRecentlyApprovedLicences(
        anyList(),
        any<LocalDate>(),
      ),
    ).thenReturn(
      listOf(
        activeVariationLicence,
      ),
    )

    val licenceSummaries = service.findRecentlyApprovedLicences(emptyList())

    assertThat(licenceSummaries).isEqualTo(
      listOf(
        aRecentlyApprovedLicenceSummary.copy(
          kind = LicenceKind.HDC,
          surname = activeVariationLicence.surname,
          forename = activeVariationLicence.forename,
          licenceStatus = LicenceStatus.INACTIVE,
          submittedByFullName = com().fullName,
          updatedByFullName = com().fullName,
          submittedDate = LocalDateTime.of(2023, 9, 19, 16, 38, 42),
        ),
      ),
    )
    verify(licenceRepository, times(1)).getRecentlyApprovedLicences(anyList(), any<LocalDate>())
  }

  @Nested
  inner class `getting licences for approval` {

    @Test
    fun `Get licences for approval returns correct approved licence summary`() {
      val prisons = listOf("MDI")

      val aLicence = aLicenceEntity.copy(
        statusCode = LicenceStatus.SUBMITTED,
        submittedBy = aCom,
        submittedDate = LocalDateTime.of(2022, 7, 27, 15, 0, 0),
        updatedBy = aCom,
      )

      whenever(licenceRepository.getLicencesReadyForApproval(prisons)).thenReturn(listOf(aLicence))

      val approvedLicenceSummaries = service.getLicencesForApproval(prisons)

      verify(licenceRepository).getLicencesReadyForApproval(prisons)

      assertThat(approvedLicenceSummaries).hasSize(1)
      assertThat(approvedLicenceSummaries[0]).isEqualTo(aLicenceSummaryApproverView)
    }

    @Test
    fun `Get licences for approval are sorted correctly`() {
      val prisons = listOf("MDI", "ABC")

      val licences = listOf(
        aLicenceEntity.copy(
          statusCode = LicenceStatus.SUBMITTED,
          submittedBy = aCom,
          updatedBy = aCom,
          licenceStartDate = null,
        ),
        aLicenceEntity.copy(
          id = 2L,
          prisonCode = "ABC",
          statusCode = LicenceStatus.SUBMITTED,
          submittedBy = aCom,
          licenceStartDate = LocalDate.of(2024, 3, 11),
        ),
        aLicenceEntity.copy(
          id = 3L,
          prisonCode = "ABC",
          statusCode = LicenceStatus.SUBMITTED,
          submittedBy = aCom,
          licenceStartDate = LocalDate.of(2024, 3, 14),
        ),
        aLicenceEntity.copy(
          id = 4L,
          prisonCode = "MDI",
          statusCode = LicenceStatus.SUBMITTED,
          submittedBy = aCom,
          updatedBy = aPreviousUser,
          licenceStartDate = LocalDate.of(2024, 3, 12),
        ),
        aLicenceEntity.copy(
          id = 5L,
          prisonCode = "MDI",
          statusCode = LicenceStatus.SUBMITTED,
          submittedBy = aCom,
          updatedBy = aPreviousUser,
          licenceStartDate = LocalDate.of(2024, 3, 10),
        ),
      )

      whenever(licenceRepository.getLicencesReadyForApproval(prisons)).thenReturn(licences)

      val approvedLicenceSummaries = service.getLicencesForApproval(prisons)

      verify(licenceRepository).getLicencesReadyForApproval(prisons)
      assertThat(approvedLicenceSummaries).hasSize(5)
      assertThat(approvedLicenceSummaries).extracting<Long> { it.licenceId }.containsExactly(5, 2, 4, 3, 1)
    }

    @Test
    fun `No prison codes when getting licences for approval returns early`() {
      val response = service.getLicencesForApproval(emptyList())
      verifyNoInteractions(licenceRepository)
      assertThat(response).isEmpty()
    }

    @Test
    fun `Derived fields are populated`() {
      whenever(releaseDateService.isInHardStopPeriod(any(), anyOrNull())).thenReturn(true)
      whenever(releaseDateService.isDueForEarlyRelease(any())).thenReturn(true)
      whenever(releaseDateService.getHardStopDate(any())).thenReturn(LocalDate.of(2022, 1, 3))
      whenever(releaseDateService.getHardStopWarningDate(any())).thenReturn(LocalDate.of(2022, 1, 1))

      whenever(licenceRepository.getLicencesReadyForApproval(listOf("MDI"))).thenReturn(listOf(aLicenceEntity))

      val approvedLicenceSummaries = service.getLicencesForApproval(listOf("MDI"))

      assertThat(approvedLicenceSummaries).hasSize(1)
      with(approvedLicenceSummaries.first()) {
        assertThat(isInHardStopPeriod).isTrue()
        assertThat(isDueForEarlyRelease).isTrue()
        assertThat(hardStopDate).isEqualTo(LocalDate.of(2022, 1, 3))
        assertThat(hardStopWarningDate).isEqualTo(LocalDate.of(2022, 1, 1))
      }
    }
  }

  private companion object {
    val aCom = com()

    val aLicenceEntity = TestData.createCrdLicence().copy(
      id = 1,
      typeCode = LicenceType.AP,
      version = "1.1",
      statusCode = LicenceStatus.IN_PROGRESS,
      nomsId = "A1234AA",
      bookingNo = "123456",
      bookingId = 54321,
      crn = "X12345",
      pnc = "2019/123445",
      cro = "12345",
      prisonCode = "MDI",
      prisonDescription = "Moorland (HMP)",
      forename = "Bob",
      surname = "Mortimer",
      dateOfBirth = LocalDate.of(1985, 12, 28),
      conditionalReleaseDate = LocalDate.of(2021, 10, 22),
      actualReleaseDate = LocalDate.of(2021, 10, 22),
      sentenceStartDate = LocalDate.of(2018, 10, 22),
      sentenceEndDate = LocalDate.of(2021, 10, 22),
      licenceStartDate = LocalDate.of(2021, 10, 22),
      licenceExpiryDate = LocalDate.of(2021, 10, 22),
      topupSupervisionStartDate = LocalDate.of(2021, 10, 22),
      topupSupervisionExpiryDate = LocalDate.of(2021, 10, 22),
      probationAreaCode = "N01",
      probationAreaDescription = "Wales",
      probationPduCode = "N01A",
      probationPduDescription = "Cardiff",
      probationLauCode = "N01A2",
      probationLauDescription = "Cardiff South",
      probationTeamCode = "NA01A2-A",
      probationTeamDescription = "Cardiff South Team A",
      dateCreated = LocalDateTime.of(2022, 7, 27, 15, 0, 0),
      standardConditions = emptyList(),
      responsibleCom = CommunityOffenderManager(
        staffIdentifier = 2000,
        username = "smills",
        email = "testemail@probation.gov.uk",
        firstName = "X",
        lastName = "Y",
      ),
      createdBy = CommunityOffenderManager(
        staffIdentifier = 2000,
        username = "smills",
        email = "testemail@probation.gov.uk",
        firstName = "X",
        lastName = "Y",
      ),
      approvedByName = "jim smith",
      approvedDate = LocalDateTime.of(2023, 9, 19, 16, 38, 42),
    ).let {
      it.copy(
        standardConditions = listOf(
          EntityStandardCondition(
            id = 1,
            conditionCode = "goodBehaviour",
            conditionSequence = 1,
            conditionText = "Be of good behaviour",
            licence = it,
          ),
          EntityStandardCondition(
            id = 2,
            conditionCode = "notBreakLaw",
            conditionSequence = 2,
            conditionText = "Do not break any law",
            licence = it,
          ),
          EntityStandardCondition(
            id = 3,
            conditionCode = "attendMeetings",
            conditionSequence = 3,
            conditionText = "Attend meetings",
            licence = it,
          ),
        ),
      )
    }

    val aLicenceSummaryApproverView = LicenceSummaryApproverView(
      licenceId = 1,
      forename = "Bob",
      surname = "Mortimer",
      dateOfBirth = LocalDate.of(1985, 12, 28),
      licenceStatus = LicenceStatus.SUBMITTED,
      kind = LicenceKind.CRD,
      licenceType = LicenceType.AP,
      nomisId = "A1234AA",
      crn = "X12345",
      bookingId = 54321,
      prisonCode = "MDI",
      prisonDescription = "Moorland (HMP)",
      probationAreaCode = "N01",
      probationAreaDescription = "Wales",
      probationPduCode = "N01A",
      probationPduDescription = "Cardiff",
      probationLauCode = "N01A2",
      probationLauDescription = "Cardiff South",
      probationTeamCode = "NA01A2-A",
      probationTeamDescription = "Cardiff South Team A",
      comUsername = "smills",
      conditionalReleaseDate = LocalDate.of(2021, 10, 22),
      actualReleaseDate = LocalDate.of(2021, 10, 22),
      sentenceStartDate = LocalDate.of(2018, 10, 22),
      sentenceEndDate = LocalDate.of(2021, 10, 22),
      licenceStartDate = LocalDate.of(2021, 10, 22),
      licenceExpiryDate = LocalDate.of(2021, 10, 22),
      topupSupervisionStartDate = LocalDate.of(2021, 10, 22),
      topupSupervisionExpiryDate = LocalDate.of(2021, 10, 22),
      dateCreated = LocalDateTime.of(2022, 7, 27, 15, 0, 0),
      submittedDate = LocalDateTime.of(2022, 7, 27, 15, 0, 0),
      approvedDate = LocalDateTime.of(2023, 9, 19, 16, 38, 42),
      approvedByName = "jim smith",
      licenceVersion = "1.0",
      versionOf = null,
      isReviewNeeded = false,
      updatedByFullName = "X Y",
      submittedByFullName = "X Y",
    )

    val aRecentlyApprovedLicence = aLicenceEntity.copy(
      actualReleaseDate = LocalDate.now().minusDays(1),
      conditionalReleaseDate = LocalDate.now(),
    )

    val aRecentlyApprovedLicenceSummary = aLicenceSummaryApproverView.copy(
      actualReleaseDate = LocalDate.now().minusDays(1),
      conditionalReleaseDate = LocalDate.now(),
    )

    val aPreviousUser = CommunityOffenderManager(
      staffIdentifier = 4000,
      username = "test",
      email = "test@test.com",
      firstName = "Test",
      lastName = "Test",
    )
  }
}
