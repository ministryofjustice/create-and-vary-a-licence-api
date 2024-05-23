package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder.setContext
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.StandardCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.NotifyAttentionNeededLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.NotifyService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.writeCsv
import java.time.LocalDate
import java.time.LocalDateTime

class NotifyAttentionNeededLicencesServiceTest {
  private val licenceRepository = mock<LicenceRepository>()
  private val prisonerSearchApiClient = mock<PrisonerSearchApiClient>()
  private val notifyService = mock<NotifyService>()

  private val service = NotifyAttentionNeededLicencesService(
    emailAddress,
    licenceRepository,
    prisonerSearchApiClient,
    notifyService,
  )

  @BeforeEach
  fun reset() {
    val authentication = mock<Authentication>()
    val securityContext = mock<SecurityContext>()

    whenever(authentication.name).thenReturn("smills")
    whenever(securityContext.authentication).thenReturn(authentication)
    setContext(securityContext)

    reset(
      licenceRepository,
      prisonerSearchApiClient,
      notifyService,
    )
  }

  @Test
  fun `notify attention needed licences job should return if there are no licences in attention needed tab`() {
    whenever(licenceRepository.getAttentionNeededLicences()).thenReturn(listOf())

    service.notifyAttentionNeededLicences()

    verify(prisonerSearchApiClient, times(0)).searchPrisonersByNomisIds(emptyList())
    verify(notifyService, times(0)).sendAttentionNeededLicencesEmail(emailAddress, ByteArray(0), fileName)
  }

  @Test
  fun `notify attention needed licences job should send email if there are licences in attention needed tab`() {
    whenever(licenceRepository.getAttentionNeededLicences()).thenReturn(listOf(aLicenceEntity))
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(aLicenceEntity.nomsId.toString()))).thenReturn(
      listOf(aPrisonerSearchPrisoner),
    )

    service.notifyAttentionNeededLicences()
    val fileContent = writeCsv(
      listOf(
        NotifyAttentionNeededLicence(
          aLicenceEntity.nomsId,
          aPrisonerSearchPrisoner.prisonName,
          aPrisonerSearchPrisoner.legalStatus,
          aLicenceEntity.conditionalReleaseDate,
          aLicenceEntity.actualReleaseDate,
          aLicenceEntity.licenceStartDate,
        ),
      ),
    )
    val cvsData =
      "Noms ID,Prison Name,Noms Legal Status,ARD,CRD,Licence Start Date\r\nA1234AA,null,SENTENCED,2021-10-22,2021-10-22,2021-10-22\r\n"

    assertThat(fileContent).isEqualTo(cvsData)
    verify(prisonerSearchApiClient, times(1)).searchPrisonersByNomisIds(listOf(aLicenceEntity.nomsId.toString()))
    verify(notifyService, times(1)).sendAttentionNeededLicencesEmail(
      emailAddress,
      fileContent.toByteArray(),
      fileName,
    )
  }

  private companion object {
    val emailAddress = "testemail@probation.gov.uk"
    val fileName = "attentionNeededLicences_" + LocalDate.now() + ".csv"
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
          StandardCondition(
            id = 1,
            conditionCode = "goodBehaviour",
            conditionSequence = 1,
            conditionText = "Be of good behaviour",
            licence = it,
          ),
          StandardCondition(
            id = 2,
            conditionCode = "notBreakLaw",
            conditionSequence = 2,
            conditionText = "Do not break any law",
            licence = it,
          ),
          StandardCondition(
            id = 3,
            conditionCode = "attendMeetings",
            conditionSequence = 3,
            conditionText = "Attend meetings",
            licence = it,
          ),
        ),
      )
    }
    val aPrisonerSearchPrisoner = PrisonerSearchPrisoner(
      prisonerNumber = "A1234AA",
      bookingId = "54321",
      status = "INACTIVE OUT",
      mostSeriousOffence = "Robbery",
      licenceExpiryDate = LocalDate.parse("2024-09-14"),
      topupSupervisionExpiryDate = LocalDate.parse("2024-09-14"),
      homeDetentionCurfewEligibilityDate = null,
      releaseDate = LocalDate.parse("2023-09-14"),
      confirmedReleaseDate = LocalDate.parse("2023-09-14"),
      conditionalReleaseDate = LocalDate.parse("2023-09-14"),
      paroleEligibilityDate = null,
      actualParoleDate = null,
      postRecallReleaseDate = null,
      legalStatus = "SENTENCED",
      indeterminateSentence = false,
      recall = false,
      prisonId = "ABC",
      locationDescription = "HMP Moorland",
      bookNumber = "12345A",
      firstName = "Jane",
      middleNames = null,
      lastName = "Doe",
      dateOfBirth = LocalDate.parse("1985-01-01"),
      conditionalReleaseDateOverrideDate = null,
      sentenceStartDate = LocalDate.parse("2023-09-14"),
      sentenceExpiryDate = LocalDate.parse("2024-09-14"),
      topupSupervisionStartDate = null,
      croNumber = null,
    )
  }
}
