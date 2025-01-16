package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import LsdRecalculationService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createCrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate
import java.time.LocalDateTime

class LsdRecalculationServiceTest {
  private val licenceRepository = mock<LicenceRepository>()
  private val releaseDateService = mock<ReleaseDateService>()
  private val prisonerSearchApiClient = mock<PrisonerSearchApiClient>()
  private val auditEventRepository = mock<AuditEventRepository>()

  private val service = LsdRecalculationService(
    licenceRepository,
    releaseDateService,
    prisonerSearchApiClient,
    auditEventRepository,
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
      prisonerSearchApiClient,
      auditEventRepository,
    )
  }

  @Test
  fun `it returns -1 if no licences are found`() {
    whenever(licenceRepository.findLicencesToBatchUpdateLsd(any(), any())).thenReturn(emptyList())

    assertThat(service.batchUpdateLicenceStartDate(10, 0)).isEqualTo(-1)
  }

  @Test
  fun `it returns the highest id of the processed licences`() {
    whenever(licenceRepository.findLicencesToBatchUpdateLsd(any(), any())).thenReturn(
      listOf(
        aLicenceEntity.copy(id = 1, nomsId = "A1234BC"),
        aLicenceEntity.copy(id = 2, nomsId = "B5678CD"),
        aLicenceEntity.copy(id = 3, nomsId = "C9012DE"),
      ),
    )

    assertThat(service.batchUpdateLicenceStartDate(10, 0)).isEqualTo(3)
  }

  @Test
  fun `it returns the highest id without processing licences if there are no associated NOMIS IDs`() {
    whenever(licenceRepository.findLicencesToBatchUpdateLsd(any(), any())).thenReturn(
      listOf(
        aLicenceEntity.copy(id = 1, nomsId = null),
      ),
    )

    assertThat(service.batchUpdateLicenceStartDate(10, 0)).isEqualTo(1)

    verify(licenceRepository, times(0)).saveAndFlush(any())
    verify(auditEventRepository, times(0)).saveAndFlush(any())
  }

  @Test
  fun `it calls to update a licence if the LSD has changed`() {
    whenever(licenceRepository.findLicencesToBatchUpdateLsd(any(), any())).thenReturn(
      listOf(
        aLicenceEntity.copy(id = 1, nomsId = "A1234BC", licenceStartDate = LocalDate.of(2021, 10, 20)),
        aLicenceEntity.copy(id = 2, nomsId = "B5678CD", licenceStartDate = LocalDate.of(2021, 10, 22)),
        aLicenceEntity.copy(id = 3, nomsId = "C9012DE", licenceStartDate = LocalDate.of(2021, 10, 23)),
      ),
    )

    whenever(releaseDateService.getLicenceStartDates(any())).thenReturn(
      mapOf(
        "A1234BC" to LocalDate.of(2021, 10, 22),
        "B5678CD" to LocalDate.of(2021, 10, 22),
        "C9012DE" to LocalDate.of(2021, 10, 22),
      ),
    )

    val licenceCaptor = ArgumentCaptor.forClass(Licence::class.java)

    service.batchUpdateLicenceStartDate(10, 0)

    verify(licenceRepository, times(2)).saveAndFlush(licenceCaptor.capture())

    assertThat(
      licenceCaptor.allValues.all {
        it.licenceStartDate == LocalDate.of(2021, 10, 22)
      },
    )
  }

  @Test
  fun `it does not call to update a licence if the LSD has not changed`() {
    whenever(licenceRepository.findLicencesToBatchUpdateLsd(any(), any())).thenReturn(
      listOf(
        aLicenceEntity.copy(id = 1, nomsId = "A1234BC"),
        aLicenceEntity.copy(id = 2, nomsId = "B5678CD"),
        aLicenceEntity.copy(id = 3, nomsId = "C9012DE"),
      ),
    )

    whenever(releaseDateService.getLicenceStartDates(any())).thenReturn(
      mapOf(
        "A1234BC" to LocalDate.of(2021, 10, 22),
        "B5678CD" to LocalDate.of(2021, 10, 22),
        "C9012DE" to LocalDate.of(2021, 10, 22),
      ),
    )

    service.batchUpdateLicenceStartDate(10, 0)

    verify(licenceRepository, times(0)).saveAndFlush(any())
  }

  @Test
  fun `it creates audit events for any changes to licence start dates`() {
    whenever(licenceRepository.findLicencesToBatchUpdateLsd(any(), any())).thenReturn(
      listOf(
        aLicenceEntity.copy(id = 1, nomsId = "A1234BC", licenceStartDate = LocalDate.of(2021, 10, 20)),
        aLicenceEntity.copy(id = 2, nomsId = "B5678CD", licenceStartDate = LocalDate.of(2021, 10, 22)),
        aLicenceEntity.copy(id = 3, nomsId = "C9012DE", licenceStartDate = LocalDate.of(2021, 10, 23)),
      ),
    )

    whenever(releaseDateService.getLicenceStartDates(any())).thenReturn(
      mapOf(
        "A1234BC" to LocalDate.of(2021, 10, 22),
        "B5678CD" to LocalDate.of(2021, 10, 22),
        "C9012DE" to LocalDate.of(2021, 10, 22),
      ),
    )

    val auditCaptor = ArgumentCaptor.forClass(AuditEvent::class.java)

    service.batchUpdateLicenceStartDate(10, 0)

    verify(auditEventRepository, times(2)).saveAndFlush(auditCaptor.capture())

    assertThat(auditCaptor.allValues).hasSize(2)
    assertThat(auditCaptor.allValues[0]).extracting("detail", "summary")
      .isEqualTo(listOf("ID 1 type AP status IN_PROGRESS version 1.1", "Licence Start Date recalculated from 2021-10-20 to 2021-10-22 for Bob Mortimer"))
    assertThat(auditCaptor.allValues[1]).extracting("detail", "summary")
      .isEqualTo(listOf("ID 3 type AP status IN_PROGRESS version 1.1", "Licence Start Date recalculated from 2021-10-23 to 2021-10-22 for Bob Mortimer"))
  }

  @Test
  fun `it does not create audit events if no LSDs were changed`() {
    whenever(licenceRepository.findLicencesToBatchUpdateLsd(any(), any())).thenReturn(
      listOf(
        aLicenceEntity.copy(id = 1, nomsId = "A1234BC"),
        aLicenceEntity.copy(id = 2, nomsId = "B5678CD"),
        aLicenceEntity.copy(id = 3, nomsId = "C9012DE"),
      ),
    )

    whenever(releaseDateService.getLicenceStartDates(any())).thenReturn(
      mapOf(
        "A1234BC" to LocalDate.of(2021, 10, 22),
        "B5678CD" to LocalDate.of(2021, 10, 22),
        "C9012DE" to LocalDate.of(2021, 10, 22),
      ),
    )

    service.batchUpdateLicenceStartDate(10, 0)

    verify(auditEventRepository, times(0)).saveAndFlush(any())
  }

  private companion object {
    val aLicenceEntity = createCrdLicence().copy(
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
    )
  }
}
