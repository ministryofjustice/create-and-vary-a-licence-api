package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import jakarta.validation.ValidationException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.LicenceEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.OverrideLicenceDatesRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AuditEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate
import java.util.Optional

class LicenceOverrideServiceTest {

  private val licenceRepository = mock<LicenceRepository>()
  private val auditEventRepository = mock<AuditEventRepository>()
  private val licenceEventRepository = mock<LicenceEventRepository>()
  private val licenceOverrideService =
    LicenceOverrideService(licenceRepository, auditEventRepository, licenceEventRepository)

  @BeforeEach
  fun reset() {
    val authentication = mock<Authentication>()
    val securityContext = mock<SecurityContext>()

    whenever(authentication.name).thenReturn("smills")
    whenever(securityContext.authentication).thenReturn(authentication)
    SecurityContextHolder.setContext(securityContext)

    reset(licenceRepository)
  }

  @Test
  fun `Override status fails if another licence for the same offender already has requested status`() {
    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(
      listOf(inactiveLicenceA, approvedLicenceA, approvedLicenceB),
    )

    whenever(licenceRepository.findById(approvedLicenceB.id)).thenReturn(Optional.of(approvedLicenceB))

    val exception = assertThrows<ValidationException> {
      licenceOverrideService.changeStatus(approvedLicenceB.id, LicenceStatus.APPROVED, "Test Exception")
    }

    assertThat(exception).isInstanceOf(ValidationException::class.java)
    assertThat(exception.message).isEqualTo("${LicenceStatus.APPROVED} is already in use for this offender on another licence")
  }

  @Test
  fun `Override status should not fail when changing to INACTIVE regardless of other licences`() {
    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(
      listOf(inactiveLicenceA, inactiveLicenceB, approvedLicenceA, approvedLicenceB),
    )

    whenever(licenceRepository.findById(approvedLicenceB.id)).thenReturn(Optional.of(approvedLicenceB))

    val reasonForChange = "Test override from ${LicenceStatus.APPROVED} to ${LicenceStatus.INACTIVE}"

    licenceOverrideService.changeStatus(
      approvedLicenceB.id,
      LicenceStatus.INACTIVE,
      reasonForChange,
    )

    val licenceCaptor = ArgumentCaptor.forClass(Licence::class.java)
    val auditCaptor = ArgumentCaptor.forClass(AuditEvent::class.java)
    val licenceEventCaptor = ArgumentCaptor.forClass(LicenceEvent::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())
    verify(licenceEventRepository, times(1)).saveAndFlush(licenceEventCaptor.capture())

    assertThat(licenceCaptor.value)
      .extracting("statusCode", "updatedByUsername", "licenceActivatedDate")
      .isEqualTo(listOf(LicenceStatus.INACTIVE, "smills", null))

    assertThat(auditCaptor.value).extracting("licenceId", "username", "eventType", "summary")
      .isEqualTo(
        listOf(
          approvedLicenceB.id,
          "smills",
          AuditEventType.USER_EVENT,
          "Licence status overridden to ${LicenceStatus.INACTIVE} for Robin Smith: $reasonForChange",
        ),
      )

    assertThat(licenceEventCaptor.value)
      .extracting("licenceId", "username", "eventType", "eventDescription")
      .isEqualTo(listOf(approvedLicenceB.id, "smills", LicenceEventType.INACTIVE, reasonForChange))
  }

  @Test
  fun `Override status updates licence as expected for unused status code`() {
    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(
      listOf(inactiveLicenceA, approvedLicenceA, approvedLicenceB),
    )

    whenever(licenceRepository.findById(approvedLicenceA.id)).thenReturn(Optional.of(approvedLicenceA))

    val reasonForChange = "Test override from ${LicenceStatus.APPROVED} to ${LicenceStatus.SUBMITTED}"

    licenceOverrideService.changeStatus(
      approvedLicenceA.id,
      LicenceStatus.SUBMITTED,
      reasonForChange,
    )

    val licenceCaptor = ArgumentCaptor.forClass(Licence::class.java)
    val auditCaptor = ArgumentCaptor.forClass(AuditEvent::class.java)
    val licenceEventCaptor = ArgumentCaptor.forClass(LicenceEvent::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())
    verify(licenceEventRepository, times(1)).saveAndFlush(licenceEventCaptor.capture())

    assertThat(licenceCaptor.value)
      .extracting("statusCode", "updatedByUsername", "licenceActivatedDate")
      .isEqualTo(listOf(LicenceStatus.SUBMITTED, "smills", null))

    assertThat(auditCaptor.value).extracting("licenceId", "username", "eventType", "summary")
      .isEqualTo(
        listOf(
          approvedLicenceA.id,
          "smills",
          AuditEventType.USER_EVENT,
          "Licence status overridden to ${LicenceStatus.SUBMITTED} for Robin Smith: $reasonForChange",
        ),
      )

    assertThat(licenceEventCaptor.value)
      .extracting("licenceId", "username", "eventType", "eventDescription")
      .isEqualTo(listOf(approvedLicenceA.id, "smills", LicenceEventType.SUBMITTED, reasonForChange))
  }

  @Test
  fun `update licenceActivatedDate when licence status is ACTIVE`() {
    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(
      listOf(inactiveLicenceA, approvedLicenceA, approvedLicenceB),
    )

    whenever(licenceRepository.findById(approvedLicenceA.id)).thenReturn(Optional.of(approvedLicenceA))

    val reasonForChange = "Test licenceActivatedDate when licence is made ${LicenceStatus.ACTIVE}"

    licenceOverrideService.changeStatus(
      approvedLicenceA.id,
      LicenceStatus.ACTIVE,
      reasonForChange,
    )

    val licenceCaptor = ArgumentCaptor.forClass(Licence::class.java)
    val auditCaptor = ArgumentCaptor.forClass(AuditEvent::class.java)
    val licenceEventCaptor = ArgumentCaptor.forClass(LicenceEvent::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())
    verify(licenceEventRepository, times(1)).saveAndFlush(licenceEventCaptor.capture())

    assertThat(licenceCaptor.value.licenceActivatedDate).isNotNull()

    assertThat(licenceCaptor.value)
      .extracting("statusCode", "updatedByUsername", "licenceActivatedDate")
      .isEqualTo(listOf(LicenceStatus.ACTIVE, "smills", licenceCaptor.value.licenceActivatedDate))

    assertThat(auditCaptor.value).extracting("licenceId", "username", "eventType", "summary")
      .isEqualTo(
        listOf(
          approvedLicenceA.id,
          "smills",
          AuditEventType.USER_EVENT,
          "Licence status overridden to ${LicenceStatus.ACTIVE} for Robin Smith: $reasonForChange",
        ),
      )

    assertThat(licenceEventCaptor.value)
      .extracting("licenceId", "username", "eventType", "eventDescription")
      .isEqualTo(listOf(approvedLicenceA.id, "smills", LicenceEventType.ACTIVATED, reasonForChange))
  }

  @Test
  fun `Override dates updates licence dates`() {
    whenever(licenceRepository.findById(approvedLicenceA.id)).thenReturn(Optional.of(approvedLicenceA))

    val request = OverrideLicenceDatesRequest(
      conditionalReleaseDate = LocalDate.now(),
      actualReleaseDate = LocalDate.now(),
      sentenceStartDate = LocalDate.now(),
      sentenceEndDate = LocalDate.now(),
      licenceStartDate = LocalDate.now(),
      licenceExpiryDate = LocalDate.now(),
      topupSupervisionStartDate = LocalDate.now(),
      topupSupervisionExpiryDate = LocalDate.now(),
      reason = "Test override dates",
    )

    licenceOverrideService.changeDates(
      approvedLicenceA.id,
      request,
    )

    val licenceCaptor = ArgumentCaptor.forClass(Licence::class.java)
    val auditCaptor = ArgumentCaptor.forClass(AuditEvent::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())

    assertThat(licenceCaptor.value)
      .extracting(
        "conditionalReleaseDate", "actualReleaseDate",
        "sentenceStartDate", "sentenceEndDate", "licenceStartDate", "licenceExpiryDate",
        "topupSupervisionStartDate", "topupSupervisionExpiryDate", "updatedByUsername",
      )
      .isEqualTo(
        listOf(
          request.conditionalReleaseDate, request.actualReleaseDate, request.sentenceStartDate,
          request.sentenceEndDate, request.licenceStartDate, request.licenceExpiryDate,
          request.topupSupervisionStartDate, request.topupSupervisionExpiryDate, "smills",
        ),
      )

    assertThat(auditCaptor.value).extracting("licenceId", "username", "eventType", "summary")
      .isEqualTo(
        listOf(
          approvedLicenceA.id,
          "smills",
          AuditEventType.USER_EVENT,
          "Sentence dates overridden for Robin Smith: ${request.reason}",
        ),
      )
  }

  private companion object {
    val inactiveLicenceA = Licence(
      id = 1L,
      crn = "exampleCrn",
      forename = "Robin",
      surname = "Smith",
      typeCode = LicenceType.AP,
      statusCode = LicenceStatus.INACTIVE,
      version = "1.0",
      probationAreaCode = "N01",
      probationAreaDescription = "N01 Region",
      probationPduCode = "PDU1",
      probationPduDescription = "PDU1 Pdu",
      probationLauCode = "LAU1",
      probationLauDescription = "LAU1 Lau",
      probationTeamCode = "TEAM1",
      probationTeamDescription = "TEAM1 probation team",
    )
    val inactiveLicenceB = Licence(
      id = 2L,
      crn = "exampleCrn",
      forename = "Robin",
      surname = "Smith",
      typeCode = LicenceType.AP,
      statusCode = LicenceStatus.INACTIVE,
      version = "1.0",
      probationAreaCode = "N01",
      probationAreaDescription = "N01 Region",
      probationPduCode = "PDU1",
      probationPduDescription = "PDU1 Pdu",
      probationLauCode = "LAU1",
      probationLauDescription = "LAU1 Lau",
      probationTeamCode = "TEAM1",
      probationTeamDescription = "TEAM1 probation team",
    )
    val approvedLicenceA = Licence(
      id = 3L,
      crn = "exampleCrn",
      forename = "Robin",
      surname = "Smith",
      typeCode = LicenceType.AP,
      statusCode = LicenceStatus.APPROVED,
      version = "1.0",
      probationAreaCode = "N01",
      probationAreaDescription = "N01 Region",
      probationPduCode = "PDU1",
      probationPduDescription = "PDU1 Pdu",
      probationLauCode = "LAU1",
      probationLauDescription = "LAU1 Lau",
      probationTeamCode = "TEAM1",
      probationTeamDescription = "TEAM1 probation team",
    )
    val approvedLicenceB = Licence(
      id = 4L,
      crn = "exampleCrn",
      forename = "Robin",
      surname = "Smith",
      typeCode = LicenceType.AP,
      statusCode = LicenceStatus.APPROVED,
      version = "1.0",
      probationAreaCode = "N01",
      probationAreaDescription = "N01 Region",
      probationPduCode = "PDU1",
      probationPduDescription = "PDU1 Pdu",
      probationLauCode = "LAU1",
      probationLauDescription = "LAU1 Lau",
      probationTeamCode = "TEAM1",
      probationTeamDescription = "TEAM1 probation team",
    )
  }
}
