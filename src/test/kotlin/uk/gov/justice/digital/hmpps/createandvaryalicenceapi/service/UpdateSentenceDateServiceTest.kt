package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence.Companion.SYSTEM_USER
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StaffRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.aPrisonApiPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createHdcLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createVariationLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.SentenceDetail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.ACTIVE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.APPROVED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.INACTIVE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.IN_PROGRESS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.SUBMITTED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.TIMED_OUT
import java.time.LocalDate
import java.util.Optional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AuditEvent as EntityAuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence as EntityLicence

class UpdateSentenceDateServiceTest {
  private val licenceRepository = mock<LicenceRepository>()
  private val auditEventRepository = mock<AuditEventRepository>()
  private val notifyService = mock<NotifyService>()
  private val prisonApiClient = mock<PrisonApiClient>()
  private val hdcService = mock<HdcService>()
  private val staffRepository = mock<StaffRepository>()
  private val releaseDateService = mock<ReleaseDateService>()
  private val licenceService = mock<LicenceService>()

  val aCrdLicenceEntity = TestData.createCrdLicence()
  val aHdcLicenceEntity = createHdcLicence()
  val aCom = TestData.com()
  val aPreviousUser = CommunityOffenderManager(
    staffIdentifier = 4000,
    username = "test",
    email = "test@test.com",
    firstName = "Test",
    lastName = "Test",
  )

  private val service = UpdateSentenceDateService(
    licenceRepository,
    auditEventRepository,
    notifyService,
    prisonApiClient,
    hdcService,
    staffRepository,
    releaseDateService,
    licenceService,
  )

  @BeforeEach
  fun reset() {
    val authentication = mock<Authentication>()
    val securityContext = mock<SecurityContext>()

    whenever(authentication.name).thenReturn("tcom")
    whenever(securityContext.authentication).thenReturn(authentication)
    SecurityContextHolder.setContext(securityContext)

    reset(
      licenceRepository,
      auditEventRepository,
      notifyService,
      prisonApiClient,
      hdcService,
      staffRepository,
      releaseDateService,
      licenceService,
    )
  }

  @BeforeEach
  fun beforeEach() {
    whenever(releaseDateService.getLicenceStartDate(any(), any())).thenReturn(LocalDate.of(2023, 9, 11))
    whenever(staffRepository.findByUsernameIgnoreCase("tcom")).thenReturn(aCom)
  }

  @Test
  fun `update sentence dates persists the updated entity`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aCrdLicenceEntity))
    whenever(hdcService.isApprovedForHdc(any(), any())).thenReturn(false)
    whenever(prisonApiClient.getPrisonerDetail(any())).thenReturn(
      aPrisonApiPrisoner().copy(
        sentenceDetail = SentenceDetail(
          conditionalReleaseDate = LocalDate.parse("2023-09-11"),
          confirmedReleaseDate = LocalDate.parse("2023-09-11"),
          sentenceStartDate = LocalDate.parse("2021-09-11"),
          sentenceExpiryDate = LocalDate.parse("2024-09-11"),
          licenceExpiryDate = LocalDate.parse("2024-09-11"),
          topupSupervisionStartDate = LocalDate.parse("2024-09-11"),
          topupSupervisionExpiryDate = LocalDate.parse("2025-09-11"),
          postRecallReleaseDate = LocalDate.parse("2025-09-11"),
        ),
      ),
    )

    service.updateSentenceDates(1L)

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(auditEventRepository, times(1)).saveAndFlush(any())

    assertThat(licenceCaptor.value)
      .extracting(
        "conditionalReleaseDate",
        "actualReleaseDate",
        "sentenceStartDate",
        "sentenceEndDate",
        "licenceStartDate",
        "licenceExpiryDate",
        "topupSupervisionStartDate",
        "topupSupervisionExpiryDate",
        "postRecallReleaseDate",
        "updatedByUsername",
        "updatedBy",
      )
      .isEqualTo(
        listOf(
          LocalDate.parse("2023-09-11"),
          LocalDate.parse("2023-09-11"),
          LocalDate.parse("2021-09-11"),
          LocalDate.parse("2024-09-11"),
          LocalDate.parse("2023-09-11"),
          LocalDate.parse("2024-09-11"),
          LocalDate.parse("2024-09-11"),
          LocalDate.parse("2025-09-11"),
          LocalDate.parse("2025-09-11"),
          aCom.username,
          aCom,
        ),
      )

    verify(notifyService, times(1)).sendDatesChangedEmail(
      "1",
      aCrdLicenceEntity.responsibleCom.email,
      "${aCrdLicenceEntity.responsibleCom.firstName} ${aCrdLicenceEntity.responsibleCom.lastName}",
      "${aCrdLicenceEntity.forename} ${aCrdLicenceEntity.surname}",
      aCrdLicenceEntity.crn,
      listOf(
        "Release date has changed to 11 September 2023",
        "Licence end date has changed to 11 September 2024",
        "Sentence end date has changed to 11 September 2024",
        "Top up supervision start date has changed to 11 September 2024",
        "Top up supervision end date has changed to 11 September 2025",
        "Post recall release date has changed to 11 September 2025",
      ),
    )

    verify(staffRepository, times(1)).findByUsernameIgnoreCase(aCom.username)
  }

  @Test
  fun `specific date changes are added to the audit`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aCrdLicenceEntity))
    whenever(hdcService.isApprovedForHdc(any(), any())).thenReturn(false)
    whenever(prisonApiClient.getPrisonerDetail(any())).thenReturn(
      aPrisonApiPrisoner().copy(
        sentenceDetail = SentenceDetail(
          conditionalReleaseDate = LocalDate.parse("2023-09-11"),
          confirmedReleaseDate = LocalDate.parse("2023-09-11"),
          sentenceStartDate = LocalDate.parse("2021-09-11"),
          sentenceExpiryDate = LocalDate.parse("2024-09-11"),
          licenceExpiryDate = LocalDate.parse("2024-09-11"),
          topupSupervisionStartDate = LocalDate.parse("2024-09-11"),
          topupSupervisionExpiryDate = LocalDate.parse("2025-09-11"),
          postRecallReleaseDate = LocalDate.parse("2025-09-11"),
        ),
      ),
    )

    service.updateSentenceDates(1L)

    argumentCaptor<EntityAuditEvent>().apply {
      verify(auditEventRepository, times(1)).saveAndFlush(capture())
      assertThat(firstValue)
        .extracting("licenceId", "username", "fullName", "summary", "changes")
        .isEqualTo(
          listOf(
            1L,
            "SYSTEM",
            "SYSTEM",
            "Sentence dates updated for ${aCrdLicenceEntity.forename} ${aCrdLicenceEntity.surname}",
            mapOf(
              "CRD" to mapOf("from" to "2021-10-22", "to" to "2023-09-11"),
              "ARD" to mapOf("from" to "2021-10-22", "to" to "2023-09-11"),
              "LED" to mapOf("from" to "2021-10-22", "to" to "2024-09-11"),
              "LSD" to mapOf("from" to "2021-10-22", "to" to "2023-09-11"),
              "PRRD" to mapOf("from" to null, "to" to "2025-09-11"),
              "SSD" to mapOf("from" to "2018-10-22", "to" to "2021-09-11"),
              "SED" to mapOf("from" to "2021-10-22", "to" to "2024-09-11"),
              "TUSED" to mapOf("from" to "2021-10-22", "to" to "2025-09-11"),
              "TUSSD" to mapOf("from" to "2021-10-22", "to" to "2024-09-11"),
            ),
          ),
        )
    }
  }

  @Test
  fun `update sentence dates persists the updated HDCAD if HDC licence`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aHdcLicenceEntity))
    whenever(hdcService.isApprovedForHdc(any(), any())).thenReturn(true)
    whenever(prisonApiClient.getPrisonerDetail(any())).thenReturn(
      aPrisonApiPrisoner().copy(
        sentenceDetail = SentenceDetail(
          conditionalReleaseDate = LocalDate.parse("2023-09-11"),
          confirmedReleaseDate = LocalDate.parse("2023-09-11"),
          sentenceStartDate = LocalDate.parse("2021-09-11"),
          sentenceExpiryDate = LocalDate.parse("2024-09-11"),
          licenceExpiryDate = LocalDate.parse("2024-09-11"),
          topupSupervisionStartDate = LocalDate.parse("2024-09-11"),
          topupSupervisionExpiryDate = LocalDate.parse("2025-09-11"),
          postRecallReleaseDate = LocalDate.parse("2025-09-11"),
          homeDetentionCurfewActualDate = LocalDate.parse("2025-09-11"),
        ),
      ),
    )

    service.updateSentenceDates(1L)

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())

    assertThat(licenceCaptor.value)
      .extracting(
        "conditionalReleaseDate",
        "actualReleaseDate",
        "sentenceStartDate",
        "sentenceEndDate",
        "licenceStartDate",
        "licenceExpiryDate",
        "topupSupervisionStartDate",
        "topupSupervisionExpiryDate",
        "postRecallReleaseDate",
        "homeDetentionCurfewActualDate",
        "updatedByUsername",
        "updatedBy",
      )
      .isEqualTo(
        listOf(
          LocalDate.parse("2023-09-11"),
          LocalDate.parse("2023-09-11"),
          LocalDate.parse("2021-09-11"),
          LocalDate.parse("2024-09-11"),
          LocalDate.parse("2023-09-11"),
          LocalDate.parse("2024-09-11"),
          LocalDate.parse("2024-09-11"),
          LocalDate.parse("2025-09-11"),
          LocalDate.parse("2025-09-11"),
          LocalDate.parse("2025-09-11"),
          aCom.username,
          aCom,
        ),
      )
  }

  @Test
  fun `update sentence dates emails if HDC licence is HDC Approved`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aHdcLicenceEntity))
    whenever(hdcService.isApprovedForHdc(any(), any())).thenReturn(true)
    whenever(prisonApiClient.getPrisonerDetail(any())).thenReturn(
      aPrisonApiPrisoner().copy(
        sentenceDetail = SentenceDetail(
          conditionalReleaseDate = LocalDate.parse("2023-09-11"),
          confirmedReleaseDate = LocalDate.parse("2023-09-11"),
          sentenceStartDate = LocalDate.parse("2021-09-11"),
          sentenceExpiryDate = LocalDate.parse("2024-09-11"),
          licenceExpiryDate = LocalDate.parse("2024-09-11"),
          topupSupervisionStartDate = LocalDate.parse("2024-09-11"),
          topupSupervisionExpiryDate = LocalDate.parse("2025-09-11"),
          homeDetentionCurfewActualDate = LocalDate.parse("2023-09-11"),
          homeDetentionCurfewEndDate = LocalDate.parse("2023-09-12"),
        ),
      ),
    )

    service.updateSentenceDates(1L)

    verify(notifyService, times(1)).sendDatesChangedEmail(
      "1",
      aHdcLicenceEntity.responsibleCom.email,
      "${aHdcLicenceEntity.responsibleCom.firstName} ${aHdcLicenceEntity.responsibleCom.lastName}",
      "${aHdcLicenceEntity.forename} ${aHdcLicenceEntity.surname}",
      aHdcLicenceEntity.crn,
      listOf(
        "Release date has changed to 11 September 2023",
        "Licence end date has changed to 11 September 2024",
        "Sentence end date has changed to 11 September 2024",
        "Top up supervision start date has changed to 11 September 2024",
        "Top up supervision end date has changed to 11 September 2025",
        "HDC actual date has changed to 11 September 2023",
        "HDC end date has changed to 12 September 2023",
      ),
    )
  }

  @Test
  fun `update sentence dates persists the updated entity with null dates`() {
    val licence = aCrdLicenceEntity.copy(sentenceStartDate = null, licenceExpiryDate = null)
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(licence))
    whenever(hdcService.isApprovedForHdc(any(), any())).thenReturn(false)
    whenever(prisonApiClient.getPrisonerDetail(any())).thenReturn(
      aPrisonApiPrisoner().copy(
        sentenceDetail = SentenceDetail(
          conditionalReleaseDate = LocalDate.parse("2023-09-11"),
          confirmedReleaseDate = null,
          sentenceStartDate = LocalDate.parse("2018-10-22"),
          sentenceExpiryDate = LocalDate.parse("2024-09-11"),
          licenceExpiryDate = LocalDate.parse("2024-09-11"),
          topupSupervisionStartDate = LocalDate.parse("2024-09-11"),
          topupSupervisionExpiryDate = null,
          postRecallReleaseDate = null,
        ),
      ),
    )

    service.updateSentenceDates(1L)

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())

    assertThat(licenceCaptor.value)
      .extracting(
        "conditionalReleaseDate",
        "actualReleaseDate",
        "sentenceStartDate",
        "sentenceEndDate",
        "licenceStartDate",
        "licenceExpiryDate",
        "topupSupervisionStartDate",
        "topupSupervisionExpiryDate",
        "postRecallReleaseDate",
        "updatedByUsername",
        "updatedBy",
      )
      .isEqualTo(
        listOf(
          LocalDate.parse("2023-09-11"),
          null,
          LocalDate.parse("2018-10-22"),
          LocalDate.parse("2024-09-11"),
          LocalDate.parse("2023-09-11"),
          LocalDate.parse("2024-09-11"),
          LocalDate.parse("2024-09-11"),
          null,
          null,
          aCom.username,
          aCom,
        ),
      )

    verify(notifyService, times(1)).sendDatesChangedEmail(
      "1",
      licence.responsibleCom.email,
      "${licence.responsibleCom.firstName} ${licence.responsibleCom.lastName}",
      "${licence.forename} ${licence.surname}",
      licence.crn,
      listOf(
        "Release date has changed to 11 September 2023",
        "Licence end date has changed to 11 September 2024",
        "Sentence end date has changed to 11 September 2024",
        "Top up supervision start date has changed to 11 September 2024",
        "Top up supervision end date has changed to null",
      ),
    )
  }

  @Test
  fun `should set the license status to inactive when the offender has a new future conditional release date`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aCrdLicenceEntity.copy(statusCode = ACTIVE)))
    whenever(hdcService.isApprovedForHdc(any(), any())).thenReturn(false)
    whenever(prisonApiClient.getPrisonerDetail(any())).thenReturn(
      aPrisonApiPrisoner().copy(
        sentenceDetail = SentenceDetail(
          conditionalReleaseDate = LocalDate.now().plusDays(5),
          confirmedReleaseDate = null,
          sentenceStartDate = LocalDate.parse("2018-10-22"),
          sentenceExpiryDate = LocalDate.parse("2024-09-11"),
          licenceExpiryDate = LocalDate.parse("2024-09-11"),
          topupSupervisionStartDate = LocalDate.parse("2024-09-11"),
          topupSupervisionExpiryDate = null,
        ),
      ),
    )

    service.updateSentenceDates(1L)

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    assertThat(licenceCaptor.value)
      .extracting("statusCode", "updatedByUsername", "updatedBy")
      .isEqualTo(listOf(INACTIVE, aCom.username, aCom))

    verify(notifyService, times(1)).sendDatesChangedEmail(
      "1",
      aCrdLicenceEntity.responsibleCom.email,
      "${aCrdLicenceEntity.responsibleCom.firstName} ${aCrdLicenceEntity.responsibleCom.lastName}",
      "${aCrdLicenceEntity.forename} ${aCrdLicenceEntity.surname}",
      aCrdLicenceEntity.crn,
      listOf(
        "Release date has changed to 11 September 2023",
        "Licence end date has changed to 11 September 2024",
        "Sentence end date has changed to 11 September 2024",
        "Top up supervision start date has changed to 11 September 2024",
        "Top up supervision end date has changed to null",
      ),
    )
  }

  @Test
  fun `should set the license status to inactive when the offender has a new future actual release date`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aCrdLicenceEntity.copy(statusCode = ACTIVE)))
    whenever(hdcService.isApprovedForHdc(any(), any())).thenReturn(false)
    whenever(prisonApiClient.getPrisonerDetail(any())).thenReturn(
      aPrisonApiPrisoner().copy(
        sentenceDetail = SentenceDetail(
          conditionalReleaseDate = null,
          confirmedReleaseDate = LocalDate.now().plusDays(2),
          sentenceStartDate = LocalDate.parse("2018-10-22"),
          sentenceExpiryDate = LocalDate.parse("2024-09-11"),
          licenceExpiryDate = LocalDate.parse("2024-09-11"),
          topupSupervisionStartDate = LocalDate.parse("2024-09-11"),
          topupSupervisionExpiryDate = null,
        ),
      ),
    )

    service.updateSentenceDates(1L)

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    assertThat(licenceCaptor.value)
      .extracting("statusCode", "updatedByUsername", "updatedBy")
      .isEqualTo(listOf(INACTIVE, aCom.username, aCom))

    verify(notifyService, times(1)).sendDatesChangedEmail(
      "1",
      aCrdLicenceEntity.responsibleCom.email,
      "${aCrdLicenceEntity.responsibleCom.firstName} ${aCrdLicenceEntity.responsibleCom.lastName}",
      "${aCrdLicenceEntity.forename} ${aCrdLicenceEntity.surname}",
      aCrdLicenceEntity.crn,
      listOf(
        "Release date has changed to 11 September 2023",
        "Licence end date has changed to 11 September 2024",
        "Sentence end date has changed to 11 September 2024",
        "Top up supervision start date has changed to 11 September 2024",
        "Top up supervision end date has changed to null",
      ),
    )
  }

  @Test
  fun `should not set the license status to inactive if existing license is not active`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aCrdLicenceEntity.copy(statusCode = IN_PROGRESS)))
    whenever(hdcService.isApprovedForHdc(any(), any())).thenReturn(false)
    whenever(prisonApiClient.getPrisonerDetail(any())).thenReturn(
      aPrisonApiPrisoner().copy(
        sentenceDetail = SentenceDetail(
          conditionalReleaseDate = LocalDate.now().plusDays(5),
          confirmedReleaseDate = LocalDate.now().plusDays(2),
          sentenceStartDate = LocalDate.parse("2018-10-22"),
          sentenceExpiryDate = LocalDate.parse("2024-09-11"),
          licenceExpiryDate = LocalDate.parse("2024-09-11"),
          topupSupervisionStartDate = LocalDate.parse("2024-09-11"),
          topupSupervisionExpiryDate = null,
        ),
      ),
    )

    service.updateSentenceDates(1L)

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    assertThat(licenceCaptor.value)
      .extracting("statusCode", "updatedByUsername", "updatedBy")
      .isEqualTo(listOf(IN_PROGRESS, aCom.username, aCom))

    verify(notifyService, times(1)).sendDatesChangedEmail(
      "1",
      aCrdLicenceEntity.responsibleCom.email,
      "${aCrdLicenceEntity.responsibleCom.firstName} ${aCrdLicenceEntity.responsibleCom.lastName}",
      "${aCrdLicenceEntity.forename} ${aCrdLicenceEntity.surname}",
      aCrdLicenceEntity.crn,
      listOf(
        "Release date has changed to 11 September 2023",
        "Licence end date has changed to 11 September 2024",
        "Sentence end date has changed to 11 September 2024",
        "Top up supervision start date has changed to 11 September 2024",
        "Top up supervision end date has changed to null",
      ),
    )
  }

  @Test
  fun `should set the license status to inactive even if conditionalReleaseDate is before today`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aCrdLicenceEntity.copy(statusCode = ACTIVE)))
    whenever(hdcService.isApprovedForHdc(any(), any())).thenReturn(false)
    whenever(prisonApiClient.getPrisonerDetail(any())).thenReturn(
      aPrisonApiPrisoner().copy(
        sentenceDetail = SentenceDetail(
          conditionalReleaseDate = LocalDate.now().minusDays(1),
          confirmedReleaseDate = LocalDate.now().plusDays(2),
          sentenceStartDate = LocalDate.parse("2018-10-22"),
          sentenceExpiryDate = LocalDate.parse("2024-09-11"),
          licenceExpiryDate = LocalDate.parse("2024-09-11"),
          topupSupervisionStartDate = LocalDate.parse("2024-09-11"),
          topupSupervisionExpiryDate = null,
        ),
      ),
    )

    service.updateSentenceDates(1L)

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    assertThat(licenceCaptor.value)
      .extracting("statusCode", "updatedByUsername", "updatedBy")
      .isEqualTo(listOf(INACTIVE, aCom.username, aCom))

    verify(notifyService, times(1)).sendDatesChangedEmail(
      "1",
      aCrdLicenceEntity.responsibleCom.email,
      "${aCrdLicenceEntity.responsibleCom.firstName} ${aCrdLicenceEntity.responsibleCom.lastName}",
      "${aCrdLicenceEntity.forename} ${aCrdLicenceEntity.surname}",
      aCrdLicenceEntity.crn,
      listOf(
        "Release date has changed to 11 September 2023",
        "Licence end date has changed to 11 September 2024",
        "Sentence end date has changed to 11 September 2024",
        "Top up supervision start date has changed to 11 September 2024",
        "Top up supervision end date has changed to null",
      ),
    )
  }

  @Test
  fun `should set the license status to inactive even if actualReleaseDate is before today`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aCrdLicenceEntity.copy(statusCode = ACTIVE)))
    whenever(hdcService.isApprovedForHdc(any(), any())).thenReturn(false)
    whenever(prisonApiClient.getPrisonerDetail(any())).thenReturn(
      aPrisonApiPrisoner().copy(
        sentenceDetail = SentenceDetail(
          conditionalReleaseDate = LocalDate.now().plusDays(5),
          confirmedReleaseDate = LocalDate.now().minusDays(2),
          sentenceStartDate = LocalDate.parse("2018-10-22"),
          sentenceExpiryDate = LocalDate.parse("2024-09-11"),
          licenceExpiryDate = LocalDate.parse("2024-09-11"),
          topupSupervisionStartDate = LocalDate.parse("2024-09-11"),
          topupSupervisionExpiryDate = null,
        ),
      ),
    )

    service.updateSentenceDates(1L)

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    assertThat(licenceCaptor.value)
      .extracting("statusCode", "updatedByUsername", "updatedBy")
      .isEqualTo(listOf(INACTIVE, aCom.username, aCom))

    verify(notifyService, times(1)).sendDatesChangedEmail(
      "1",
      aCrdLicenceEntity.responsibleCom.email,
      "${aCrdLicenceEntity.responsibleCom.firstName} ${aCrdLicenceEntity.responsibleCom.lastName}",
      "${aCrdLicenceEntity.forename} ${aCrdLicenceEntity.surname}",
      aCrdLicenceEntity.crn,
      listOf(
        "Release date has changed to 11 September 2023",
        "Licence end date has changed to 11 September 2024",
        "Sentence end date has changed to 11 September 2024",
        "Top up supervision start date has changed to 11 September 2024",
        "Top up supervision end date has changed to null",
      ),
    )
  }

  @Test
  fun `updating user is retained and username is set to SYSTEM_USER when a staff member cannot be found`() {
    whenever(licenceRepository.findById(1L)).thenReturn(
      Optional.of(
        aCrdLicenceEntity.copy(
          updatedBy = aPreviousUser,
        ),
      ),
    )
    whenever(hdcService.isApprovedForHdc(any(), any())).thenReturn(false)
    whenever(staffRepository.findByUsernameIgnoreCase("tcom")).thenReturn(null)
    whenever(prisonApiClient.getPrisonerDetail(any())).thenReturn(
      aPrisonApiPrisoner().copy(
        sentenceDetail = SentenceDetail(
          conditionalReleaseDate = LocalDate.parse("2023-09-11"),
          confirmedReleaseDate = LocalDate.parse("2023-09-11"),
          sentenceStartDate = LocalDate.parse("2021-09-11"),
          sentenceExpiryDate = LocalDate.parse("2024-09-11"),
          licenceExpiryDate = LocalDate.parse("2024-09-11"),
          topupSupervisionStartDate = LocalDate.parse("2024-09-11"),
          topupSupervisionExpiryDate = LocalDate.parse("2025-09-11"),
        ),
      ),
    )

    service.updateSentenceDates(1L)

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())

    assertThat(licenceCaptor.value)
      .extracting(
        "updatedByUsername",
        "updatedBy",
      )
      .isEqualTo(
        listOf(
          SYSTEM_USER,
          aPreviousUser,
        ),
      )
  }

  @Test
  fun `Recalculates Licence Start Date rather than reading from the request`() {
    whenever(releaseDateService.getLicenceStartDate(any(), any())).thenReturn(LocalDate.of(2024, 1, 1))
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aCrdLicenceEntity))
    whenever(hdcService.isApprovedForHdc(any(), any())).thenReturn(false)
    whenever(prisonApiClient.getPrisonerDetail(any())).thenReturn(
      aPrisonApiPrisoner().copy(
        sentenceDetail = SentenceDetail(
          conditionalReleaseDate = LocalDate.parse("2023-09-11"),
          confirmedReleaseDate = LocalDate.parse("2023-09-11"),
          sentenceStartDate = LocalDate.parse("2021-09-11"),
          sentenceExpiryDate = LocalDate.parse("2024-09-11"),
          licenceExpiryDate = LocalDate.parse("2024-09-11"),
          topupSupervisionStartDate = LocalDate.parse("2024-09-11"),
          topupSupervisionExpiryDate = LocalDate.parse("2025-09-11"),
          postRecallReleaseDate = LocalDate.parse("2025-09-11"),
        ),
      ),
    )

    service.updateSentenceDates(1L)

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())

    assertThat(licenceCaptor.value)
      .extracting(
        "conditionalReleaseDate",
        "actualReleaseDate",
        "sentenceStartDate",
        "sentenceEndDate",
        "licenceStartDate",
        "licenceExpiryDate",
        "topupSupervisionStartDate",
        "topupSupervisionExpiryDate",
        "postRecallReleaseDate",
        "updatedByUsername",
        "updatedBy",
      )
      .isEqualTo(
        listOf(
          LocalDate.parse("2023-09-11"),
          LocalDate.parse("2023-09-11"),
          LocalDate.parse("2021-09-11"),
          LocalDate.parse("2024-09-11"),
          LocalDate.parse("2024-01-01"),
          LocalDate.parse("2024-09-11"),
          LocalDate.parse("2024-09-11"),
          LocalDate.parse("2025-09-11"),
          LocalDate.parse("2025-09-11"),
          aCom.username,
          aCom,
        ),
      )

    verify(notifyService, times(1)).sendDatesChangedEmail(
      "1",
      aCrdLicenceEntity.responsibleCom.email,
      "${aCrdLicenceEntity.responsibleCom.firstName} ${aCrdLicenceEntity.responsibleCom.lastName}",
      "${aCrdLicenceEntity.forename} ${aCrdLicenceEntity.surname}",
      aCrdLicenceEntity.crn,
      listOf(
        "Release date has changed to 01 January 2024",
        "Licence end date has changed to 11 September 2024",
        "Sentence end date has changed to 11 September 2024",
        "Top up supervision start date has changed to 11 September 2024",
        "Top up supervision end date has changed to 11 September 2025",
        "Post recall release date has changed to 11 September 2025",
      ),
    )
  }

  @Nested
  inner class `Timing out licences` {
    @Test
    fun `should time out if the licence is now in hard stop period but previously was not`() {
      whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aCrdLicenceEntity))
      whenever(hdcService.isApprovedForHdc(any(), any())).thenReturn(false)
      whenever(releaseDateService.isInHardStopPeriod(any(), anyOrNull())).thenReturn(false, true)
      whenever(prisonApiClient.getPrisonerDetail(any())).thenReturn(
        aPrisonApiPrisoner().copy(
          sentenceDetail = SentenceDetail(
            conditionalReleaseDate = LocalDate.now().plusDays(5),
            confirmedReleaseDate = LocalDate.now().minusDays(2),
            sentenceStartDate = LocalDate.parse("2018-10-22"),
            sentenceExpiryDate = LocalDate.parse("2024-09-11"),
            licenceExpiryDate = LocalDate.parse("2024-09-11"),
            topupSupervisionStartDate = LocalDate.parse("2024-09-11"),
            topupSupervisionExpiryDate = null,
          ),
        ),
      )

      service.updateSentenceDates(1L)

      verify(licenceService, times(1)).timeout(any(), any())
    }

    @Test
    fun `should not time out if the licence is not an in progress licence`() {
      whenever(licenceRepository.findById(1L)).thenReturn(
        Optional.of(
          aCrdLicenceEntity.copy(
            statusCode = SUBMITTED,
          ),
        ),
      )
      whenever(hdcService.isApprovedForHdc(any(), any())).thenReturn(false)

      whenever(releaseDateService.isInHardStopPeriod(any(), anyOrNull())).thenReturn(false, true)
      whenever(prisonApiClient.getPrisonerDetail(any())).thenReturn(
        aPrisonApiPrisoner().copy(
          sentenceDetail = SentenceDetail(
            conditionalReleaseDate = LocalDate.now().plusDays(5),
            confirmedReleaseDate = LocalDate.now().minusDays(2),
            sentenceStartDate = LocalDate.parse("2018-10-22"),
            sentenceExpiryDate = LocalDate.parse("2024-09-11"),
            licenceExpiryDate = LocalDate.parse("2024-09-11"),
            topupSupervisionStartDate = LocalDate.parse("2024-09-11"),
            topupSupervisionExpiryDate = null,
          ),
        ),
      )

      service.updateSentenceDates(1L)
      verify(licenceService, times(0)).timeout(any(), any())
    }

    @Test
    fun `should not time out if the licence is in hard stop period but is not a CRD licence`() {
      whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(createVariationLicence()))
      whenever(hdcService.isApprovedForHdc(any(), any())).thenReturn(false)
      whenever(releaseDateService.isInHardStopPeriod(any(), anyOrNull())).thenReturn(false, true)
      whenever(prisonApiClient.getPrisonerDetail(any())).thenReturn(
        aPrisonApiPrisoner().copy(
          sentenceDetail = SentenceDetail(
            conditionalReleaseDate = LocalDate.parse("2023-09-11"),
            confirmedReleaseDate = LocalDate.parse("2023-09-11"),
            sentenceStartDate = LocalDate.parse("2021-09-11"),
            sentenceExpiryDate = LocalDate.parse("2024-09-11"),
            licenceExpiryDate = LocalDate.parse("2024-09-11"),
            topupSupervisionStartDate = LocalDate.parse("2024-09-11"),
            topupSupervisionExpiryDate = LocalDate.parse("2025-09-11"),
          ),
        ),
      )

      service.updateSentenceDates(1L)

      verify(licenceService, times(0)).timeout(any(), any())
    }

    @Test
    fun `should inactivate a licence if the licence was in hard stop period but is no longer in hard stop period`() {
      val inHardStopLicence = aCrdLicenceEntity.copy(
        statusCode = TIMED_OUT,
        conditionalReleaseDate = LocalDate.now(),
        actualReleaseDate = LocalDate.now(),
        sentenceStartDate = LocalDate.now().minusYears(2),
        sentenceEndDate = LocalDate.now().plusYears(1),
        licenceStartDate = LocalDate.now(),
        licenceExpiryDate = LocalDate.now().plusYears(1),
        topupSupervisionStartDate = LocalDate.now().plusYears(1),
        topupSupervisionExpiryDate = LocalDate.now().plusYears(2),
      )

      val noLongerInHardStopLicence = inHardStopLicence.copy(
        conditionalReleaseDate = LocalDate.now().plusWeeks(1),
        actualReleaseDate = LocalDate.now().plusWeeks(1),
        licenceStartDate = LocalDate.now().plusWeeks(1),
      )

      whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(inHardStopLicence))
      whenever(hdcService.isApprovedForHdc(any(), any())).thenReturn(false)

      whenever(releaseDateService.isInHardStopPeriod(any(), anyOrNull())).thenReturn(true, false)
      whenever(
        licenceRepository.findAllByBookingIdAndStatusCodeInAndKindIn(
          inHardStopLicence.bookingId!!,
          listOf(
            IN_PROGRESS,
            SUBMITTED,
            APPROVED,
            TIMED_OUT,
          ),
          listOf(LicenceKind.CRD, LicenceKind.HARD_STOP),
        ),
      ).thenReturn(listOf(noLongerInHardStopLicence))
      whenever(prisonApiClient.getPrisonerDetail(any())).thenReturn(
        aPrisonApiPrisoner().copy(
          sentenceDetail = SentenceDetail(
            conditionalReleaseDate = LocalDate.now().plusWeeks(1),
            confirmedReleaseDate = LocalDate.now().plusWeeks(1),
            sentenceStartDate = LocalDate.now().minusYears(2),
            sentenceExpiryDate = LocalDate.now().plusYears(1),
            licenceExpiryDate = LocalDate.now().plusYears(1),
            topupSupervisionStartDate = LocalDate.now().plusYears(1),
            topupSupervisionExpiryDate = LocalDate.now().plusYears(2),
          ),
        ),
      )

      service.updateSentenceDates(1L)

      verify(licenceService, times(1)).inactivateLicences(
        listOf(noLongerInHardStopLicence),
        UpdateSentenceDateService.LICENCE_DEACTIVATION_HARD_STOP,
      )

      verify(licenceService, times(0)).timeout(any(), any())
    }
  }

  @Test
  fun `should not time out if the licence is in hard stop period but is a HDC licence`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(createHdcLicence()))
    whenever(hdcService.isApprovedForHdc(any(), any())).thenReturn(true)
    whenever(releaseDateService.isInHardStopPeriod(any(), anyOrNull())).thenReturn(false, true)
    whenever(prisonApiClient.getPrisonerDetail(any())).thenReturn(
      aPrisonApiPrisoner().copy(
        sentenceDetail = SentenceDetail(
          conditionalReleaseDate = LocalDate.parse("2023-09-11"),
          confirmedReleaseDate = LocalDate.parse("2023-09-11"),
          sentenceStartDate = LocalDate.parse("2021-09-11"),
          sentenceExpiryDate = LocalDate.parse("2024-09-11"),
          licenceExpiryDate = LocalDate.parse("2024-09-11"),
          topupSupervisionStartDate = LocalDate.parse("2024-09-11"),
          topupSupervisionExpiryDate = LocalDate.parse("2025-09-11"),
        ),
      ),
    )

    service.updateSentenceDates(1L)

    verify(licenceService, times(0)).timeout(any(), any())
  }
}
