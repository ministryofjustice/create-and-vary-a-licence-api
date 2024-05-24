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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.NotifyAttentionNeededLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.NotifyService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.writeCsv
import java.time.LocalDate

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
      nomsId = "A1234AA",
      conditionalReleaseDate = LocalDate.of(2021, 10, 22),
      actualReleaseDate = LocalDate.of(2021, 10, 22),
      licenceStartDate = LocalDate.of(2021, 10, 22),
    )

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
