package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.LicenceEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.HdcLicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createHdcLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.DomainEventsService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AuditEventType.SYSTEM_EVENT
import java.time.LocalDate
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceEventType.INACTIVE as LicenceEventTypeInactive
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.INACTIVE as LicenceStatusInactive

class DeactivateHdcLicencesServiceTest {
  private val licenceRepository = mock<LicenceRepository>()
  private val hdcLicenceRepository = mock<HdcLicenceRepository>()
  private val auditEventRepository = mock<AuditEventRepository>()
  private val licenceEventRepository = mock<LicenceEventRepository>()
  private val domainEventsService = mock<DomainEventsService>()
  private val telemetryClient = mock<TelemetryClient>()

  private val service = DeactivateHdcLicencesService(
    licenceRepository,
    hdcLicenceRepository,
    auditEventRepository,
    licenceEventRepository,
    domainEventsService,
    telemetryClient,
  )

  @BeforeEach
  fun reset() {
    reset(
      licenceRepository,
      auditEventRepository,
      licenceEventRepository,
    )
  }

  @Test
  fun `return if no licences to deactivate`() {
    whenever(hdcLicenceRepository.getDraftLicencesIneligibleForHdcRelease()).thenReturn(emptyList())

    service.runJob()

    verify(licenceRepository, times(0)).saveAndFlush(any())
    verify(auditEventRepository, times(0)).saveAndFlush(any())
    verify(licenceEventRepository, times(0)).saveAndFlush(any())
    verify(telemetryClient, times(0)).trackEvent(any(), any(), isNull())
  }

  @Test
  fun `deactivate HDC licences job runs successfully`() {
    val licences = listOf(
      aHdcLicence,
    )
    whenever(hdcLicenceRepository.getDraftLicencesIneligibleForHdcRelease()).thenReturn(licences)

    service.runJob()

    val licenceCaptor = argumentCaptor<List<Licence>>()
    val auditCaptor = ArgumentCaptor.forClass(AuditEvent::class.java)
    val eventCaptor = ArgumentCaptor.forClass(LicenceEvent::class.java)

    verify(hdcLicenceRepository, times(1)).getDraftLicencesIneligibleForHdcRelease()
    verify(licenceRepository, times(1)).saveAllAndFlush(licenceCaptor.capture())
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())
    verify(licenceEventRepository, times(1)).saveAndFlush(eventCaptor.capture())
    verify(domainEventsService, times(1)).recordDomainEvent(aHdcLicence, LicenceStatusInactive)
    verify(telemetryClient).trackEvent("DeactivateHdcLicencesJob", mapOf("licences" to "1"), null)

    assertThat(licenceCaptor.firstValue[0])
      .extracting("statusCode")
      .isEqualTo(LicenceStatusInactive)

    assertThat(auditCaptor.value)
      .extracting("licenceId", "username", "fullName", "eventType", "summary", "detail")
      .isEqualTo(
        listOf(
          1L,
          "SYSTEM",
          "SYSTEM",
          SYSTEM_EVENT,
          "HDC licence automatically deactivated as now ineligible for HDC release for ${aHdcLicence.forename} ${aHdcLicence.surname}",
          "ID ${aHdcLicence.id} type ${aHdcLicence.typeCode} status $LicenceStatusInactive version ${aHdcLicence.version}",
        ),
      )

    assertThat(eventCaptor.value)
      .extracting("licenceId", "eventType", "username", "forenames", "surname", "eventDescription")
      .isEqualTo(
        listOf(
          1L,
          LicenceEventTypeInactive,
          "SYSTEM",
          "SYSTEM",
          "SYSTEM",
          "HDC licence automatically deactivated as now ineligible for HDC release for ${aHdcLicence.forename} ${aHdcLicence.surname}",
        ),
      )
  }

  private companion object {
    val aHdcLicence = createHdcLicence().copy(
      conditionalReleaseDate = LocalDate.now().plusDays(9),
    )
  }
}
