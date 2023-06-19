package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AuditRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AuditEventType
import java.time.LocalDateTime
import java.util.Optional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AuditEvent as EntityAuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence as EntityLicence

class AuditServiceTest {
  private val licenceRepository = mock<LicenceRepository>()
  private val auditEventRepository = mock<AuditEventRepository>()

  private val service = AuditService(auditEventRepository, licenceRepository)

  @BeforeEach
  fun reset() {
    reset(auditEventRepository, licenceRepository)
  }

  @Test
  fun `records an audit event`() {
    whenever(auditEventRepository.save(transform(anEvent))).thenReturn(transform(anEvent))
    service.recordAuditEvent(anEvent)
    verify(auditEventRepository, times(1)).save(transform(anEvent))
  }

  @Test
  fun `gets audit events relating to a specific licence`() {
    val aUserRequest = aRequest.copy(username = null, licenceId = 1L)

    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))

    whenever(
      auditEventRepository
        .findAllByLicenceIdAndEventTimeBetweenOrderByEventTimeDesc(
          aUserRequest.licenceId!!,
          aUserRequest.startTime,
          aUserRequest.endTime,
        ),
    ).thenReturn(aListOfEntities)

    val response = service.getAuditEvents(aUserRequest)

    assertThat(response).hasSize(3)
    assertThat(response[0].summary).isEqualTo("Summary1")

    verify(licenceRepository, times(1)).findById(1L)
    verify(auditEventRepository, times(1)).findAllByLicenceIdAndEventTimeBetweenOrderByEventTimeDesc(
      aUserRequest.licenceId!!,
      aUserRequest.startTime,
      aUserRequest.endTime,
    )
  }

  @Test
  fun `get audit events relating to a specific user`() {
    val aUserRequest = aRequest.copy(licenceId = null)
    whenever(
      auditEventRepository
        .findAllByUsernameAndEventTimeBetweenOrderByEventTimeDesc(
          aUserRequest.username!!,
          aUserRequest.startTime,
          aUserRequest.endTime,
        ),
    ).thenReturn(aListOfEntities)

    val response = service.getAuditEvents(aUserRequest)

    assertThat(response).hasSize(3)
    assertThat(response[0].summary).isEqualTo("Summary1")

    verify(auditEventRepository, times(1)).findAllByUsernameAndEventTimeBetweenOrderByEventTimeDesc(
      aUserRequest.username!!,
      aUserRequest.startTime,
      aUserRequest.endTime,
    )
  }

  @Test
  fun `get all audit events`() {
    val aUserRequest = aRequest.copy(username = null, licenceId = null)
    whenever(
      auditEventRepository
        .findAllByEventTimeBetweenOrderByEventTimeDesc(
          aUserRequest.startTime,
          aUserRequest.endTime,
        ),
    ).thenReturn(aListOfEntities)

    val response = service.getAuditEvents(aUserRequest)

    assertThat(response).hasSize(3)
    assertThat(response[0].summary).isEqualTo("Summary1")

    verify(auditEventRepository, times(1)).findAllByEventTimeBetweenOrderByEventTimeDesc(
      aUserRequest.startTime,
      aUserRequest.endTime,
    )
  }

  companion object {
    val anEvent = AuditEvent(
      licenceId = 1L,
      eventTime = LocalDateTime.now(),
      username = "USER",
      fullName = "Forename Surname",
      eventType = AuditEventType.USER_EVENT,
      summary = "Summary description",
      detail = "Detail description",
    )

    val aRequest = AuditRequest(
      username = "USER",
      licenceId = 1L,
      startTime = LocalDateTime.now().minusMonths(1),
      endTime = LocalDateTime.now(),
    )

    val aLicenceEntity = EntityLicence(id = 1L)

    val aListOfEntities = listOf(
      EntityAuditEvent(
        id = 1L,
        licenceId = 1L,
        eventTime = LocalDateTime.now().minusDays(1L),
        username = "USER",
        fullName = "First Last",
        eventType = AuditEventType.USER_EVENT,
        summary = "Summary1",
        detail = "Detail1",
      ),
      EntityAuditEvent(
        id = 2L,
        licenceId = 1L,
        eventTime = LocalDateTime.now().minusDays(2L),
        username = "USER",
        fullName = "First Last",
        eventType = AuditEventType.USER_EVENT,
        summary = "Summary2",
        detail = "Detail2",
      ),
      EntityAuditEvent(
        id = 3L,
        licenceId = 1L,
        eventTime = LocalDateTime.now().minusDays(3L),
        username = "CUSER",
        fullName = "First Last",
        eventType = AuditEventType.SYSTEM_EVENT,
        summary = "Summary3",
        detail = "Detail3",
      ),
    )
  }
}
