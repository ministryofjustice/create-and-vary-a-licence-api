package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.TimeServedExternalRecord
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.ExternalTimeServedRecordRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StaffRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.TimeServedExternalRecordsRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.communityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.prisonUser

private const val BOOKING_ID = 12345L
private const val PRISON_NUMBER = "A1234BC"
private const val USERNAME = "tcom1"
private const val REQUEST_PRISON_CODE = "MDI"
private const val REQUEST_REASON = "Some reason"

private const val PREVIOUS_PRISON_CODE = "LEI"
private const val PREVIOUS_REASON = "old reason"

class TimeServedExternalRecordsReasonServiceTest {

  private val licenceRepository = mock<TimeServedExternalRecordsRepository>()
  private val staffRepository = mock<StaffRepository>()
  private val auditEventRepository = mock<AuditEventRepository>()

  private val service = TimeServedExternalRecordService(
    licenceRepository,
    staffRepository,
    auditEventRepository,
  )

  @BeforeEach
  fun reset() {
    reset(
      licenceRepository,
      staffRepository,
      auditEventRepository,
    )
    val authentication = mock<Authentication>()
    val securityContext = mock<SecurityContext>()

    whenever(authentication.name).thenReturn(USERNAME)
    whenever(securityContext.authentication).thenReturn(authentication)
    SecurityContextHolder.setContext(securityContext)
  }

  @Nested
  inner class `Setting Record Tests` {
    @Test
    fun `it throws ValidationException if staff username not found`() {
      // Given
      val request = ExternalTimeServedRecordRequest(reason = REQUEST_REASON, prisonCode = REQUEST_PRISON_CODE)

      whenever(staffRepository.findByUsernameIgnoreCase(USERNAME)).thenReturn(null)

      // When
      val exception = assertThrows<IllegalStateException> {
        service.setTimeServedExternalRecord(PRISON_NUMBER, BOOKING_ID, request)
      }

      // Then
      assertThat(exception)
        .isInstanceOf(IllegalStateException::class.java)
        .hasMessage("Staff with username $USERNAME not found")

      verify(licenceRepository, times(0)).saveAndFlush(any())
      verify(auditEventRepository, times(0)).saveAndFlush(any())
    }

    @Test
    fun `successful creation`() {
      // Given
      val request = ExternalTimeServedRecordRequest(reason = REQUEST_REASON, prisonCode = REQUEST_PRISON_CODE)
      val staff = prisonUser().copy(username = USERNAME)

      whenever(staffRepository.findByUsernameIgnoreCase(USERNAME)).thenReturn(staff)
      whenever(licenceRepository.findByNomsIdAndBookingId(PRISON_NUMBER, BOOKING_ID)).thenReturn(null)
      whenever(licenceRepository.saveAndFlush(any())).thenAnswer { it.arguments[0] }

      // When
      service.setTimeServedExternalRecord(PRISON_NUMBER, BOOKING_ID, request)

      // Then
      argumentCaptor<TimeServedExternalRecord> {
        verify(licenceRepository).saveAndFlush(capture())
        with(firstValue) {
          assertThat(nomsId).isEqualTo(PRISON_NUMBER)
          assertThat(bookingId).isEqualTo(BOOKING_ID)
          assertThat(reason).isEqualTo(REQUEST_REASON)
          assertThat(prisonCode).isEqualTo(REQUEST_PRISON_CODE)
          assertThat(updatedByCa).isEqualTo(staff)
          assertThat(dateCreated).isNotNull()
          assertThat(dateLastUpdated).isNotNull()
        }
      }
      argumentCaptor<AuditEvent> {
        verify(auditEventRepository).saveAndFlush(capture())
        with(firstValue) {
          assertThat(summary).isEqualTo("TimeServed External Record Reason created")
          assertThat(username).isEqualTo(USERNAME)
          assertThat(changes).containsEntry("nomsId", PRISON_NUMBER)
          assertThat(changes).containsEntry("bookingId", BOOKING_ID)
          assertThat(changes).containsEntry("reason", REQUEST_REASON)
          assertThat(changes).containsEntry("prisonCode", REQUEST_PRISON_CODE)
        }
      }
    }

    @Test
    fun `successful update`() {
      // Given
      val request = ExternalTimeServedRecordRequest(reason = REQUEST_REASON, prisonCode = REQUEST_PRISON_CODE)
      val staff = prisonUser().copy(username = USERNAME)
      val existingRecord = TimeServedExternalRecord(
        nomsId = PRISON_NUMBER,
        bookingId = BOOKING_ID,
        reason = PREVIOUS_REASON,
        prisonCode = PREVIOUS_PRISON_CODE,
        updatedByCa = communityOffenderManager(),
      )

      whenever(staffRepository.findByUsernameIgnoreCase(USERNAME)).thenReturn(staff)
      whenever(licenceRepository.findByNomsIdAndBookingId(PRISON_NUMBER, BOOKING_ID)).thenReturn(existingRecord)
      whenever(licenceRepository.saveAndFlush(any())).thenAnswer { it.arguments[0] }

      // When
      service.setTimeServedExternalRecord(PRISON_NUMBER, BOOKING_ID, request)

      // Then
      argumentCaptor<TimeServedExternalRecord> {
        verify(licenceRepository).saveAndFlush(capture())
        with(firstValue) {
          assertThat(nomsId).isEqualTo(PRISON_NUMBER)
          assertThat(bookingId).isEqualTo(BOOKING_ID)
          assertThat(reason).isEqualTo(REQUEST_REASON)
          assertThat(prisonCode).isEqualTo(REQUEST_PRISON_CODE)
          assertThat(updatedByCa).isEqualTo(staff)
          assertThat(dateCreated).isNotNull()
          assertThat(dateLastUpdated).isNotNull()
        }
      }
      argumentCaptor<AuditEvent> {
        verify(auditEventRepository).saveAndFlush(capture())
        with(firstValue) {
          assertThat(summary).isEqualTo("TimeServed External Record Reason updated")
          assertThat(username).isEqualTo(USERNAME)
          assertThat(changes).containsEntry("nomsId", PRISON_NUMBER)
          assertThat(changes).containsEntry("bookingId", BOOKING_ID)
          assertThat(changes).containsEntry("reason (old)", PREVIOUS_REASON)
          assertThat(changes).containsEntry("reason (new)", REQUEST_REASON)
          assertThat(changes).containsEntry("prisonCode (old)", PREVIOUS_PRISON_CODE)
          assertThat(changes).containsEntry("prisonCode (new)", REQUEST_PRISON_CODE)
        }
      }
    }
  }
}
