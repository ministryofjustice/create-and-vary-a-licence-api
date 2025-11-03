package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.NomisTimeServedLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.RecordNomisLicenceReasonRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateNomisLicenceReasonRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.NomisTimeServedLicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StaffRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.communityOffenderManager

class RecordNomisTimeServedLicenceServiceTest {

  private val licenceRepository = mock<NomisTimeServedLicenceRepository>()
  private val staffRepository = mock<StaffRepository>()
  private val auditEventRepository = mock<AuditEventRepository>()

  private val service = RecordNomisTimeServedLicenceService(
    licenceRepository,
    staffRepository,
    auditEventRepository,
  )

  @BeforeEach
  fun reset() {
    org.mockito.kotlin.reset(
      licenceRepository,
      staffRepository,
      auditEventRepository,
    )
    val authentication = org.mockito.kotlin.mock<Authentication>()
    val securityContext = org.mockito.kotlin.mock<SecurityContext>()

    whenever(authentication.name).thenReturn(com.username)
    whenever(securityContext.authentication).thenReturn(authentication)
    SecurityContextHolder.setContext(securityContext)
  }

  @Test
  fun `recordNomisLicenceReason throws ValidationException if staff username not found`() {
    // Arrange
    val request = RecordNomisLicenceReasonRequest(
      nomsId = "A1234BC",
      bookingId = 12345L,
      reason = "Some reason",
      prisonCode = "PRISON1",
    )
    val expectedCom = communityOffenderManager()

    val username = "tcom1"
    whenever(staffRepository.findByUsernameIgnoreCase(username)).thenReturn(null)

    // Act
    val exception = assertThrows<IllegalStateException> {
      service.recordNomisLicenceReason(request)
    }

    // Assert
    assertThat(exception)
      .isInstanceOf(IllegalStateException::class.java)
      .hasMessage("Staff with username $username not found")

    verify(licenceRepository, times(0)).saveAndFlush(any())
    verify(auditEventRepository, times(0)).saveAndFlush(any())
  }

  @Test
  fun `updateNomisLicenceReason throws IllegalStateException if staff username not found`() {
    // Arrange
    val nomsId = "A1234BC"
    val bookingId = 12345L
    val request = UpdateNomisLicenceReasonRequest(reason = "Updated reason")

    val username = "tcom1"

    // Mock existing licence record
    val existingLicence = NomisTimeServedLicence(
      nomsId = nomsId,
      bookingId = bookingId,
      reason = "Old reason",
      prisonCode = "PRISON1",
      updatedByCa = communityOffenderManager(),
    )

    whenever(licenceRepository.findByNomsIdAndBookingId(nomsId, bookingId))
      .thenReturn(existingLicence)

    // Simulate missing staff
    whenever(staffRepository.findByUsernameIgnoreCase(username)).thenReturn(null)

    // Act
    val exception = assertThrows<IllegalStateException> {
      service.updateNomisLicenceReason(nomsId, bookingId, request)
    }

    // Assert
    assertThat(exception)
      .isInstanceOf(IllegalStateException::class.java)
      .hasMessage("Staff with username $username not found")

    verify(licenceRepository, times(0)).saveAndFlush(any())
    verify(auditEventRepository, times(0)).saveAndFlush(any())
  }

  private companion object {
    val com = communityOffenderManager()
  }
}
