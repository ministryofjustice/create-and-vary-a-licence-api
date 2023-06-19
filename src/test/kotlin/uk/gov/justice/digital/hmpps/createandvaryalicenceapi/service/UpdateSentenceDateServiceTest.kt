package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

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
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateSentenceDatesRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerHdcStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AuditEvent as EntityAuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence as EntityLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.StandardCondition as EntityStandardCondition

class UpdateSentenceDateServiceTest {
  private val licenceRepository = mock<LicenceRepository>()
  private val auditEventRepository = mock<AuditEventRepository>()
  private val notifyService = mock<NotifyService>()
  private val prisonApiClient = mock<PrisonApiClient>()

  private val service = UpdateSentenceDateService(
    licenceRepository,
    auditEventRepository,
    notifyService,
    prisonApiClient,
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
      auditEventRepository,
      notifyService,
      prisonApiClient,
    )
  }

  @Test
  fun `update sentence dates persists the updated entity`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))
    whenever(prisonApiClient.hdcStatus(any())).thenReturn(
      Mono.just(
        PrisonerHdcStatus(
          approvalStatusDate = null,
          approvalStatus = "REJECTED",
          refusedReason = null,
          checksPassedDate = null,
          bookingId = aLicenceEntity.bookingId!!,
          passed = true,
        ),
      ),
    )

    service.updateSentenceDates(
      1L,
      UpdateSentenceDatesRequest(
        conditionalReleaseDate = LocalDate.parse("2023-09-11"),
        actualReleaseDate = LocalDate.parse("2023-09-11"),
        sentenceStartDate = LocalDate.parse("2021-09-11"),
        sentenceEndDate = LocalDate.parse("2024-09-11"),
        licenceStartDate = LocalDate.parse("2023-09-11"),
        licenceExpiryDate = LocalDate.parse("2024-09-11"),
        topupSupervisionStartDate = LocalDate.parse("2024-09-11"),
        topupSupervisionExpiryDate = LocalDate.parse("2025-09-11"),
      ),
    )

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())

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
        "updatedByUsername",
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
          "smills",
        ),
      )

    assertThat(auditCaptor.value)
      .extracting("licenceId", "username", "fullName", "summary")
      .isEqualTo(
        listOf(
          1L,
          "SYSTEM",
          "SYSTEM",
          "Sentence dates updated for ${aLicenceEntity.forename} ${aLicenceEntity.surname}",
        ),
      )

    verify(notifyService, times(1)).sendDatesChangedEmail(
      "1",
      aLicenceEntity.responsibleCom?.email,
      "${aLicenceEntity.responsibleCom?.firstName} ${aLicenceEntity.responsibleCom?.lastName}",
      "${aLicenceEntity.forename} ${aLicenceEntity.surname}",
      aLicenceEntity.crn,
      mapOf(
        "Release date" to true,
        "Licence end date" to true,
        "Sentence end date" to true,
        "Top up supervision start date" to true,
        "Top up supervision end date" to true,
      ),
    )
  }

  @Test
  fun `update sentence dates still sends email if HDC licence is not found`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))
    whenever(prisonApiClient.hdcStatus(any())).thenReturn(Mono.empty())

    service.updateSentenceDates(
      1L,
      UpdateSentenceDatesRequest(
        conditionalReleaseDate = LocalDate.parse("2023-09-11"),
        actualReleaseDate = LocalDate.parse("2023-09-11"),
        sentenceStartDate = LocalDate.parse("2021-09-11"),
        sentenceEndDate = LocalDate.parse("2024-09-11"),
        licenceStartDate = LocalDate.parse("2023-09-11"),
        licenceExpiryDate = LocalDate.parse("2024-09-11"),
        topupSupervisionStartDate = LocalDate.parse("2024-09-11"),
        topupSupervisionExpiryDate = LocalDate.parse("2025-09-11"),
      ),
    )

    verify(notifyService, times(1)).sendDatesChangedEmail(any(), any(), any(), any(), any(), any())
  }

  @Test
  fun `update sentence dates does not email if HDC licence is Approved`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))
    whenever(prisonApiClient.hdcStatus(any())).thenReturn(
      Mono.just(
        PrisonerHdcStatus(
          approvalStatusDate = null,
          approvalStatus = "APPROVED",
          refusedReason = null,
          checksPassedDate = null,
          bookingId = aLicenceEntity.bookingId!!,
          passed = true,
        ),
      ),
    )

    service.updateSentenceDates(
      1L,
      UpdateSentenceDatesRequest(
        conditionalReleaseDate = LocalDate.parse("2023-09-11"),
        actualReleaseDate = LocalDate.parse("2023-09-11"),
        sentenceStartDate = LocalDate.parse("2021-09-11"),
        sentenceEndDate = LocalDate.parse("2024-09-11"),
        licenceStartDate = LocalDate.parse("2023-09-11"),
        licenceExpiryDate = LocalDate.parse("2024-09-11"),
        topupSupervisionStartDate = LocalDate.parse("2024-09-11"),
        topupSupervisionExpiryDate = LocalDate.parse("2025-09-11"),
      ),
    )

    verify(notifyService, times(0)).sendDatesChangedEmail(any(), any(), any(), any(), any(), any())
  }

  @Test
  fun `update sentence dates does not email if Licence BookingId is missing`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity.copy(bookingId = null)))
    whenever(prisonApiClient.hdcStatus(any())).thenReturn(
      Mono.just(
        PrisonerHdcStatus(
          approvalStatusDate = null,
          approvalStatus = "REJECTED",
          refusedReason = null,
          checksPassedDate = null,
          bookingId = aLicenceEntity.bookingId!!,
          passed = false,
        ),
      ),
    )

    service.updateSentenceDates(
      1L,
      UpdateSentenceDatesRequest(
        conditionalReleaseDate = LocalDate.parse("2023-09-11"),
        actualReleaseDate = LocalDate.parse("2023-09-11"),
        sentenceStartDate = LocalDate.parse("2021-09-11"),
        sentenceEndDate = LocalDate.parse("2024-09-11"),
        licenceStartDate = LocalDate.parse("2023-09-11"),
        licenceExpiryDate = LocalDate.parse("2024-09-11"),
        topupSupervisionStartDate = LocalDate.parse("2024-09-11"),
        topupSupervisionExpiryDate = LocalDate.parse("2025-09-11"),
      ),
    )

    verify(notifyService, times(0)).sendDatesChangedEmail(any(), any(), any(), any(), any(), any())
  }

  @Test
  fun `update sentence dates persists the updated entity with null dates`() {
    val licence = aLicenceEntity.copy(sentenceStartDate = null, licenceExpiryDate = null)
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(licence))
    whenever(prisonApiClient.hdcStatus(any())).thenReturn(
      Mono.just(
        PrisonerHdcStatus(
          approvalStatusDate = null,
          approvalStatus = "REJECTED",
          refusedReason = null,
          checksPassedDate = null,
          bookingId = aLicenceEntity.bookingId!!,
          passed = false,
        ),
      ),
    )

    service.updateSentenceDates(
      1L,
      UpdateSentenceDatesRequest(
        conditionalReleaseDate = LocalDate.parse("2023-09-11"),
        actualReleaseDate = null,
        sentenceStartDate = LocalDate.parse("2018-10-22"),
        sentenceEndDate = LocalDate.parse("2024-09-11"),
        licenceStartDate = LocalDate.parse("2023-09-11"),
        licenceExpiryDate = LocalDate.parse("2024-09-11"),
        topupSupervisionStartDate = LocalDate.parse("2024-09-11"),
        topupSupervisionExpiryDate = null,
      ),
    )

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())

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
        "updatedByUsername",
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
          "smills",
        ),
      )

    assertThat(auditCaptor.value)
      .extracting("licenceId", "username", "fullName", "summary")
      .isEqualTo(listOf(1L, "SYSTEM", "SYSTEM", "Sentence dates updated for ${licence.forename} ${licence.surname}"))

    verify(notifyService, times(1)).sendDatesChangedEmail(
      "1",
      licence.responsibleCom?.email,
      "${licence.responsibleCom?.firstName} ${licence.responsibleCom?.lastName}",
      "${licence.forename} ${licence.surname}",
      licence.crn,
      mapOf(
        "Release date" to true,
        "Licence end date" to true,
        "Sentence end date" to true,
        "Top up supervision start date" to true,
        "Top up supervision end date" to true,
      ),
    )
  }

  @Test
  fun `should set the license status to inactive when the offender has a new future conditional release date`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity.copy(statusCode = LicenceStatus.ACTIVE)))
    whenever(prisonApiClient.hdcStatus(any())).thenReturn(
      Mono.just(
        PrisonerHdcStatus(
          approvalStatusDate = null,
          approvalStatus = "REJECTED",
          refusedReason = null,
          checksPassedDate = null,
          bookingId = aLicenceEntity.bookingId!!,
          passed = true,
        ),
      ),
    )
    service.updateSentenceDates(
      1L,
      UpdateSentenceDatesRequest(
        conditionalReleaseDate = LocalDate.now().plusDays(5),
        actualReleaseDate = null,
        sentenceStartDate = LocalDate.parse("2018-10-22"),
        sentenceEndDate = LocalDate.parse("2024-09-11"),
        licenceStartDate = LocalDate.parse("2023-09-11"),
        licenceExpiryDate = LocalDate.parse("2024-09-11"),
        topupSupervisionStartDate = LocalDate.parse("2024-09-11"),
        topupSupervisionExpiryDate = null,
      ),
    )

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    assertThat(licenceCaptor.value).extracting("statusCode").isEqualTo(LicenceStatus.INACTIVE)

    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())
    assertThat(auditCaptor.value).extracting("licenceId", "username", "fullName", "summary")
      .isEqualTo(
        listOf(
          1L,
          "SYSTEM",
          "SYSTEM",
          "Sentence dates updated for ${aLicenceEntity.forename} ${aLicenceEntity.surname}",
        ),
      )
    verify(notifyService, times(1)).sendDatesChangedEmail(
      "1",
      aLicenceEntity.responsibleCom?.email,
      "${aLicenceEntity.responsibleCom?.firstName} ${aLicenceEntity.responsibleCom?.lastName}",
      "${aLicenceEntity.forename} ${aLicenceEntity.surname}",
      aLicenceEntity.crn,
      mapOf(
        "Release date" to true,
        "Licence end date" to true,
        "Sentence end date" to true,
        "Top up supervision start date" to true,
        "Top up supervision end date" to true,
      ),
    )
  }

  @Test
  fun `should set the license status to inactive when the offender has a new future actual release date`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity.copy(statusCode = LicenceStatus.ACTIVE)))
    whenever(prisonApiClient.hdcStatus(any())).thenReturn(
      Mono.just(
        PrisonerHdcStatus(
          approvalStatusDate = null,
          approvalStatus = "REJECTED",
          refusedReason = null,
          checksPassedDate = null,
          bookingId = aLicenceEntity.bookingId!!,
          passed = true,
        ),
      ),
    )
    service.updateSentenceDates(
      1L,
      UpdateSentenceDatesRequest(
        conditionalReleaseDate = null,
        actualReleaseDate = LocalDate.now().plusDays(2),
        sentenceStartDate = LocalDate.parse("2018-10-22"),
        sentenceEndDate = LocalDate.parse("2024-09-11"),
        licenceStartDate = LocalDate.parse("2023-09-11"),
        licenceExpiryDate = LocalDate.parse("2024-09-11"),
        topupSupervisionStartDate = LocalDate.parse("2024-09-11"),
        topupSupervisionExpiryDate = null,
      ),
    )

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    assertThat(licenceCaptor.value).extracting("statusCode").isEqualTo(LicenceStatus.INACTIVE)

    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())
    assertThat(auditCaptor.value).extracting("licenceId", "username", "fullName", "summary")
      .isEqualTo(
        listOf(
          1L,
          "SYSTEM",
          "SYSTEM",
          "Sentence dates updated for ${aLicenceEntity.forename} ${aLicenceEntity.surname}",
        ),
      )
    verify(notifyService, times(1)).sendDatesChangedEmail(
      "1",
      aLicenceEntity.responsibleCom?.email,
      "${aLicenceEntity.responsibleCom?.firstName} ${aLicenceEntity.responsibleCom?.lastName}",
      "${aLicenceEntity.forename} ${aLicenceEntity.surname}",
      aLicenceEntity.crn,
      mapOf(
        "Release date" to true,
        "Licence end date" to true,
        "Sentence end date" to true,
        "Top up supervision start date" to true,
        "Top up supervision end date" to true,
      ),
    )
  }

  @Test
  fun `should not set the license status to inactive if existing license is not active`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity.copy(statusCode = LicenceStatus.IN_PROGRESS)))
    whenever(prisonApiClient.hdcStatus(any())).thenReturn(
      Mono.just(
        PrisonerHdcStatus(
          approvalStatusDate = null,
          approvalStatus = "REJECTED",
          refusedReason = null,
          checksPassedDate = null,
          bookingId = aLicenceEntity.bookingId!!,
          passed = true,
        ),
      ),
    )
    service.updateSentenceDates(
      1L,
      UpdateSentenceDatesRequest(
        conditionalReleaseDate = LocalDate.now().plusDays(5),
        actualReleaseDate = LocalDate.now().plusDays(2),
        sentenceStartDate = LocalDate.parse("2018-10-22"),
        sentenceEndDate = LocalDate.parse("2024-09-11"),
        licenceStartDate = LocalDate.parse("2023-09-11"),
        licenceExpiryDate = LocalDate.parse("2024-09-11"),
        topupSupervisionStartDate = LocalDate.parse("2024-09-11"),
        topupSupervisionExpiryDate = null,
      ),
    )

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    assertThat(licenceCaptor.value).extracting("statusCode").isEqualTo(LicenceStatus.IN_PROGRESS)

    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())
    assertThat(auditCaptor.value).extracting("licenceId", "username", "fullName", "summary")
      .isEqualTo(
        listOf(
          1L,
          "SYSTEM",
          "SYSTEM",
          "Sentence dates updated for ${aLicenceEntity.forename} ${aLicenceEntity.surname}",
        ),
      )

    verify(notifyService, times(1)).sendDatesChangedEmail(
      "1",
      aLicenceEntity.responsibleCom?.email,
      "${aLicenceEntity.responsibleCom?.firstName} ${aLicenceEntity.responsibleCom?.lastName}",
      "${aLicenceEntity.forename} ${aLicenceEntity.surname}",
      aLicenceEntity.crn,
      mapOf(
        "Release date" to true,
        "Licence end date" to true,
        "Sentence end date" to true,
        "Top up supervision start date" to true,
        "Top up supervision end date" to true,
      ),
    )
  }

  @Test
  fun `should set the license status to inactive even if conditionalReleaseDate is before today`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity.copy(statusCode = LicenceStatus.ACTIVE)))
    whenever(prisonApiClient.hdcStatus(any())).thenReturn(
      Mono.just(
        PrisonerHdcStatus(
          approvalStatusDate = null,
          approvalStatus = "REJECTED",
          refusedReason = null,
          checksPassedDate = null,
          bookingId = aLicenceEntity.bookingId!!,
          passed = true,
        ),
      ),
    )
    service.updateSentenceDates(
      1L,
      UpdateSentenceDatesRequest(
        conditionalReleaseDate = LocalDate.now().minusDays(1),
        actualReleaseDate = LocalDate.now().plusDays(2),
        sentenceStartDate = LocalDate.parse("2018-10-22"),
        sentenceEndDate = LocalDate.parse("2024-09-11"),
        licenceStartDate = LocalDate.parse("2023-09-11"),
        licenceExpiryDate = LocalDate.parse("2024-09-11"),
        topupSupervisionStartDate = LocalDate.parse("2024-09-11"),
        topupSupervisionExpiryDate = null,
      ),
    )

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    assertThat(licenceCaptor.value).extracting("statusCode").isEqualTo(LicenceStatus.INACTIVE)

    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())
    assertThat(auditCaptor.value).extracting("licenceId", "username", "fullName", "summary")
      .isEqualTo(
        listOf(
          1L,
          "SYSTEM",
          "SYSTEM",
          "Sentence dates updated for ${aLicenceEntity.forename} ${aLicenceEntity.surname}",
        ),
      )

    verify(notifyService, times(1)).sendDatesChangedEmail(
      "1",
      aLicenceEntity.responsibleCom?.email,
      "${aLicenceEntity.responsibleCom?.firstName} ${aLicenceEntity.responsibleCom?.lastName}",
      "${aLicenceEntity.forename} ${aLicenceEntity.surname}",
      aLicenceEntity.crn,
      mapOf(
        "Release date" to true,
        "Licence end date" to true,
        "Sentence end date" to true,
        "Top up supervision start date" to true,
        "Top up supervision end date" to true,
      ),
    )
  }

  @Test
  fun `should set the license status to inactive even if actualReleaseDate is before today`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity.copy(statusCode = LicenceStatus.ACTIVE)))
    whenever(prisonApiClient.hdcStatus(any())).thenReturn(
      Mono.just(
        PrisonerHdcStatus(
          approvalStatusDate = null,
          approvalStatus = "REJECTED",
          refusedReason = null,
          checksPassedDate = null,
          bookingId = aLicenceEntity.bookingId!!,
          passed = true,
        ),
      ),
    )

    service.updateSentenceDates(
      1L,
      UpdateSentenceDatesRequest(
        conditionalReleaseDate = LocalDate.now().plusDays(5),
        actualReleaseDate = LocalDate.now().minusDays(2),
        sentenceStartDate = LocalDate.parse("2018-10-22"),
        sentenceEndDate = LocalDate.parse("2024-09-11"),
        licenceStartDate = LocalDate.parse("2023-09-11"),
        licenceExpiryDate = LocalDate.parse("2024-09-11"),
        topupSupervisionStartDate = LocalDate.parse("2024-09-11"),
        topupSupervisionExpiryDate = null,
      ),
    )

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    assertThat(licenceCaptor.value).extracting("statusCode").isEqualTo(LicenceStatus.INACTIVE)

    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())
    assertThat(auditCaptor.value).extracting("licenceId", "username", "fullName", "summary")
      .isEqualTo(
        listOf(
          1L,
          "SYSTEM",
          "SYSTEM",
          "Sentence dates updated for ${aLicenceEntity.forename} ${aLicenceEntity.surname}",
        ),
      )

    verify(notifyService, times(1)).sendDatesChangedEmail(
      "1",
      aLicenceEntity.responsibleCom?.email,
      "${aLicenceEntity.responsibleCom?.firstName} ${aLicenceEntity.responsibleCom?.lastName}",
      "${aLicenceEntity.forename} ${aLicenceEntity.surname}",
      aLicenceEntity.crn,
      mapOf(
        "Release date" to true,
        "Licence end date" to true,
        "Sentence end date" to true,
        "Top up supervision start date" to true,
        "Top up supervision end date" to true,
      ),
    )
  }

  private companion object {
    val someEntityStandardConditions = listOf(
      EntityStandardCondition(
        id = 1,
        conditionCode = "goodBehaviour",
        conditionSequence = 1,
        conditionText = "Be of good behaviour",
        licence = mock(),
      ),
      EntityStandardCondition(
        id = 2,
        conditionCode = "notBreakLaw",
        conditionSequence = 2,
        conditionText = "Do not break any law",
        licence = mock(),
      ),
      EntityStandardCondition(
        id = 3,
        conditionCode = "attendMeetings",
        conditionSequence = 3,
        conditionText = "Attend meetings",
        licence = mock(),
      ),
    )

    val aLicenceEntity = EntityLicence(
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
      standardConditions = someEntityStandardConditions,
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
    )
  }
}
