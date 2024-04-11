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
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence.Companion.SYSTEM_USER
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateSentenceDatesRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StaffRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerHdcStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AuditEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.time.LocalDate
import java.util.Optional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AuditEvent as EntityAuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence as EntityLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.LicenceEvent as EntityLicenceEvent

class UpdateSentenceDateServiceTest {
  private val licenceRepository = mock<LicenceRepository>()
  private val auditEventRepository = mock<AuditEventRepository>()
  private val notifyService = mock<NotifyService>()
  private val prisonApiClient = mock<PrisonApiClient>()
  private val staffRepository = mock<StaffRepository>()
  private val releaseDateService = mock<ReleaseDateService>()
  private val licenceEventRepository = mock<LicenceEventRepository>()

  private val service = UpdateSentenceDateService(
    licenceRepository,
    auditEventRepository,
    notifyService,
    prisonApiClient,
    staffRepository,
    releaseDateService,
    licenceEventRepository,
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
      staffRepository,
      releaseDateService,
      licenceEventRepository,
    )
  }

  @Test
  fun `update sentence dates persists the updated entity`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))
    whenever(prisonApiClient.getHdcStatus(any())).thenReturn(
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
    whenever(staffRepository.findByUsernameIgnoreCase("smills")).thenReturn(aCom)

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
          aCom.username,
          aCom,
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
        "Release date has changed to 11 September 2023" to true,
        "Licence end date has changed to 11 September 2024" to true,
        "Sentence end date has changed to 11 September 2024" to true,
        "Top up supervision start date has changed to 11 September 2024" to true,
        "Top up supervision end date has changed to 11 September 2025" to true,
      ),
    )

    verify(staffRepository, times(1)).findByUsernameIgnoreCase(aCom.username)
  }

  @Test
  fun `update sentence dates still sends email if HDC licence is not found`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))
    whenever(prisonApiClient.getHdcStatus(any())).thenReturn(Mono.empty())

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
    whenever(prisonApiClient.getHdcStatus(any())).thenReturn(
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
    whenever(prisonApiClient.getHdcStatus(any())).thenReturn(
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
    whenever(prisonApiClient.getHdcStatus(any())).thenReturn(
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
    whenever(staffRepository.findByUsernameIgnoreCase("smills")).thenReturn(aCom)

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
          aCom.username,
          aCom,
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
        "Release date has changed to 11 September 2023" to true,
        "Licence end date has changed to 11 September 2024" to true,
        "Sentence end date has changed to 11 September 2024" to true,
        "Top up supervision start date has changed to 11 September 2024" to true,
        "Top up supervision end date has changed to null" to true,
      ),
    )

    verify(staffRepository, times(1)).findByUsernameIgnoreCase(aCom.username)
  }

  @Test
  fun `should set the license status to inactive when the offender has a new future conditional release date`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity.copy(statusCode = LicenceStatus.ACTIVE)))
    whenever(prisonApiClient.getHdcStatus(any())).thenReturn(
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
    whenever(staffRepository.findByUsernameIgnoreCase("smills")).thenReturn(aCom)

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
    assertThat(licenceCaptor.value)
      .extracting("statusCode", "updatedByUsername", "updatedBy")
      .isEqualTo(listOf(LicenceStatus.INACTIVE, aCom.username, aCom))

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
        "Release date has changed to 11 September 2023" to true,
        "Licence end date has changed to 11 September 2024" to true,
        "Sentence end date has changed to 11 September 2024" to true,
        "Top up supervision start date has changed to 11 September 2024" to true,
        "Top up supervision end date has changed to null" to true,
      ),
    )
    verify(staffRepository, times(1)).findByUsernameIgnoreCase(aCom.username)
  }

  @Test
  fun `should set the license status to inactive when the offender has a new future actual release date`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity.copy(statusCode = LicenceStatus.ACTIVE)))
    whenever(prisonApiClient.getHdcStatus(any())).thenReturn(
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
    whenever(staffRepository.findByUsernameIgnoreCase("smills")).thenReturn(aCom)

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
    assertThat(licenceCaptor.value)
      .extracting("statusCode", "updatedByUsername", "updatedBy")
      .isEqualTo(listOf(LicenceStatus.INACTIVE, aCom.username, aCom))

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
        "Release date has changed to 11 September 2023" to true,
        "Licence end date has changed to 11 September 2024" to true,
        "Sentence end date has changed to 11 September 2024" to true,
        "Top up supervision start date has changed to 11 September 2024" to true,
        "Top up supervision end date has changed to null" to true,
      ),
    )
    verify(staffRepository, times(1)).findByUsernameIgnoreCase(aCom.username)
  }

  @Test
  fun `should not set the license status to inactive if existing license is not active`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity.copy(statusCode = LicenceStatus.IN_PROGRESS)))
    whenever(prisonApiClient.getHdcStatus(any())).thenReturn(
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
    whenever(staffRepository.findByUsernameIgnoreCase("smills")).thenReturn(aCom)

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
    assertThat(licenceCaptor.value)
      .extracting("statusCode", "updatedByUsername", "updatedBy")
      .isEqualTo(listOf(LicenceStatus.IN_PROGRESS, aCom.username, aCom))
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
        "Release date has changed to 11 September 2023" to true,
        "Licence end date has changed to 11 September 2024" to true,
        "Sentence end date has changed to 11 September 2024" to true,
        "Top up supervision start date has changed to 11 September 2024" to true,
        "Top up supervision end date has changed to null" to true,
      ),
    )
    verify(staffRepository, times(1)).findByUsernameIgnoreCase(aCom.username)
  }

  @Test
  fun `should set the license status to inactive even if conditionalReleaseDate is before today`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity.copy(statusCode = LicenceStatus.ACTIVE)))
    whenever(prisonApiClient.getHdcStatus(any())).thenReturn(
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
    whenever(staffRepository.findByUsernameIgnoreCase("smills")).thenReturn(aCom)

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
    assertThat(licenceCaptor.value)
      .extracting("statusCode", "updatedByUsername", "updatedBy")
      .isEqualTo(listOf(LicenceStatus.INACTIVE, aCom.username, aCom))

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
        "Release date has changed to 11 September 2023" to true,
        "Licence end date has changed to 11 September 2024" to true,
        "Sentence end date has changed to 11 September 2024" to true,
        "Top up supervision start date has changed to 11 September 2024" to true,
        "Top up supervision end date has changed to null" to true,
      ),
    )
    verify(staffRepository, times(1)).findByUsernameIgnoreCase(aCom.username)
  }

  @Test
  fun `should set the license status to inactive even if actualReleaseDate is before today`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity.copy(statusCode = LicenceStatus.ACTIVE)))
    whenever(prisonApiClient.getHdcStatus(any())).thenReturn(
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
    whenever(staffRepository.findByUsernameIgnoreCase("smills")).thenReturn(aCom)

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
    assertThat(licenceCaptor.value)
      .extracting("statusCode", "updatedByUsername", "updatedBy")
      .isEqualTo(listOf(LicenceStatus.INACTIVE, aCom.username, aCom))

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
        "Release date has changed to 11 September 2023" to true,
        "Licence end date has changed to 11 September 2024" to true,
        "Sentence end date has changed to 11 September 2024" to true,
        "Top up supervision start date has changed to 11 September 2024" to true,
        "Top up supervision end date has changed to null" to true,
      ),
    )
    verify(staffRepository, times(1)).findByUsernameIgnoreCase(aCom.username)
  }

  @Test
  fun `updating user is retained and username is set to SYSTEM_USER when a staff member cannot be found`() {
    whenever(licenceRepository.findById(1L)).thenReturn(
      Optional.of(
        aLicenceEntity.copy(
          updatedBy = aPreviousUser,
        ),
      ),
    )
    whenever(prisonApiClient.getHdcStatus(any())).thenReturn(
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
    whenever(staffRepository.findByUsernameIgnoreCase("smills")).thenReturn(null)

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
          SYSTEM_USER,
          aPreviousUser,
        ),
      )

    verify(staffRepository, times(1)).findByUsernameIgnoreCase(aCom.username)
  }

  @Test
  fun `should set the license status to timed out if the licence is in hard stop period`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))
    whenever(prisonApiClient.getHdcStatus(any())).thenReturn(
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
    whenever(staffRepository.findByUsernameIgnoreCase("smills")).thenReturn(aCom)
    whenever(releaseDateService.isInHardStopPeriod(aLicenceEntity)).thenReturn(true)

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
    val eventCaptor = ArgumentCaptor.forClass(EntityLicenceEvent::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    assertThat(licenceCaptor.value)
      .extracting("statusCode", "updatedByUsername", "updatedBy")
      .isEqualTo(listOf(LicenceStatus.TIMED_OUT, "SYSTEM", aCom))

    verify(auditEventRepository, times(2)).saveAndFlush(auditCaptor.capture())

    assertThat(auditCaptor.allValues[0])
      .extracting("licenceId", "username", "fullName", "summary", "eventType")
      .isEqualTo(
        listOf(
          1L,
          "SYSTEM",
          "SYSTEM",
          "Sentence dates updated for ${aLicenceEntity.forename} ${aLicenceEntity.surname}",
          AuditEventType.SYSTEM_EVENT,
        ),
      )

    assertThat(auditCaptor.allValues[1])
      .extracting("licenceId", "username", "fullName", "summary", "eventType")
      .isEqualTo(
        listOf(
          1L,
          "SYSTEM",
          "SYSTEM",
          "Licence automatically timed out after sentence dates update for ${aLicenceEntity.forename} ${aLicenceEntity.surname}",
          AuditEventType.SYSTEM_EVENT,
        ),
      )

    verify(licenceEventRepository, times(1)).saveAndFlush(eventCaptor.capture())

    assertThat(eventCaptor.value)
      .extracting("licenceId", "eventType", "username", "forenames", "surname", "eventDescription")
      .isEqualTo(
        listOf(
          1L,
          LicenceEventType.TIMED_OUT,
          "SYSTEM",
          "SYSTEM",
          "SYSTEM",
          "Licence automatically timed out after sentence dates update for ${aLicenceEntity.forename} ${aLicenceEntity.surname}",
        ),
      )

    verify(notifyService, times(1)).sendDatesChangedEmail(
      "1",
      aLicenceEntity.responsibleCom?.email,
      "${aLicenceEntity.responsibleCom?.firstName} ${aLicenceEntity.responsibleCom?.lastName}",
      "${aLicenceEntity.forename} ${aLicenceEntity.surname}",
      aLicenceEntity.crn,
      mapOf(
        "Release date has changed to 11 September 2023" to true,
        "Licence end date has changed to 11 September 2024" to true,
        "Sentence end date has changed to 11 September 2024" to true,
        "Top up supervision start date has changed to 11 September 2024" to true,
        "Top up supervision end date has changed to null" to true,
      ),
    )

    verify(staffRepository, times(1)).findByUsernameIgnoreCase(aCom.username)
  }

  @Test
  fun `should not time out if the licence is in hard stop period but is not a CRD licence`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(TestData.createVariationLicence()))
    whenever(prisonApiClient.getHdcStatus(any())).thenReturn(
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
    whenever(staffRepository.findByUsernameIgnoreCase("smills")).thenReturn(aCom)
    whenever(releaseDateService.isInHardStopPeriod(TestData.createVariationLicence())).thenReturn(true)

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
        "updatedBy",
        "statusCode",
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
          aCom.username,
          aCom,
          LicenceStatus.VARIATION_IN_PROGRESS,
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
        "Release date has changed to 11 September 2023" to true,
        "Licence end date has changed to 11 September 2024" to true,
        "Sentence end date has changed to 11 September 2024" to true,
        "Top up supervision start date has changed to 11 September 2024" to true,
        "Top up supervision end date has changed to 11 September 2025" to true,
      ),
    )

    verify(staffRepository, times(1)).findByUsernameIgnoreCase(aCom.username)
    verifyNoInteractions(licenceEventRepository)
  }

  private companion object {
    val aLicenceEntity = TestData.createCrdLicence()
    val aCom = TestData.com()
    val aPreviousUser = CommunityOffenderManager(
      staffIdentifier = 4000,
      username = "test",
      email = "test@test.com",
      firstName = "Test",
      lastName = "Test",
    )
  }
}
