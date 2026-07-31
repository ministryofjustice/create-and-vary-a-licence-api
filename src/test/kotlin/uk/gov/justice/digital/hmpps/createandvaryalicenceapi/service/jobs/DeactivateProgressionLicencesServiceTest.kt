package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.LicenceEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.NotifyService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TelemetryService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.communityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.DomainEventsService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AuditEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.time.LocalDate

class DeactivateProgressionLicencesServiceTest {
  private val licenceRepository = mock<LicenceRepository>()
  private val auditEventRepository = mock<AuditEventRepository>()
  private val licenceEventRepository = mock<LicenceEventRepository>()
  private val domainEventsService = mock<DomainEventsService>()
  private val telemetryService = mock<TelemetryService>()
  private val notifyService = mock<NotifyService>()

  private val policyV4StartDate = LocalDate.now()
  private val notificationWindowEndDate = LocalDate.now().plusDays(1)

  private val service = DeactivateProgressionLicencesService(
    licenceRepository,
    auditEventRepository,
    licenceEventRepository,
    domainEventsService,
    telemetryService,
    notifyService,
    policyV4StartDate,
    notificationWindowEndDate,
  )

  @BeforeEach
  fun reset() {
    val authentication = mock<Authentication>()
    val securityContext = mock<SecurityContext>()

    whenever(authentication.name).thenReturn(aCom.username)
    whenever(securityContext.authentication).thenReturn(authentication)
    SecurityContextHolder.setContext(securityContext)

    reset(
      licenceRepository,
      auditEventRepository,
      licenceEventRepository,
      domainEventsService,
      telemetryService,
      notifyService,
    )
  }

  @Test
  fun `Given there no licences with release dates after progression start date, it should not deactivate any licence`() {
    whenever(licenceRepository.getLicencesForProgressionDeactivation(policyV4StartDate)).thenReturn(emptyList())
    service.deactivateLicences()
    verify(licenceRepository, times(1)).getLicencesForProgressionDeactivation(policyV4StartDate)
    verify(licenceRepository, times(0)).saveAllAndFlush(emptyList())
    verify(auditEventRepository, times(0)).saveAndFlush(any<AuditEvent>())
    verify(licenceEventRepository, times(0)).saveAndFlush(any<LicenceEvent>())
  }

  @Test
  fun `Given there is a licence with a release date after progression start date, then should deactivate the licence`() {
    whenever(licenceRepository.getLicencesForProgressionDeactivation(policyV4StartDate)).thenReturn(
      listOf(
        aLicenceEntity,
      ),
    )

    service.deactivateLicences()

    val licenceCaptor = argumentCaptor<List<Licence>>()
    val auditCaptor = ArgumentCaptor.forClass(AuditEvent::class.java)
    val eventCaptor = ArgumentCaptor.forClass(LicenceEvent::class.java)

    verify(licenceRepository, times(1)).getLicencesForProgressionDeactivation(policyV4StartDate)

    verify(licenceRepository, times(1)).saveAllAndFlush(licenceCaptor.capture())

    assertThat(licenceCaptor.firstValue[0])
      .extracting("statusCode", "updatedByUsername", "updatedBy")
      .isEqualTo(listOf(LicenceStatus.INACTIVE, "SYSTEM_USER", null))

    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())
    verify(licenceEventRepository, times(1)).saveAndFlush(eventCaptor.capture())
    verify(domainEventsService, times(1)).recordDomainEvent(aLicenceEntity, LicenceStatus.INACTIVE)

    assertThat(auditCaptor.value)
      .extracting("licenceId", "username", "fullName", "eventType", "summary", "detail")
      .isEqualTo(
        listOf(
          1L,
          "SYSTEM",
          "SYSTEM",
          AuditEventType.SYSTEM_EVENT,
          "Licence automatically deactivated as it is a progression licence on an older policy version for ${aLicenceEntity.forename} ${aLicenceEntity.surname}",
          "ID ${aLicenceEntity.id} type ${aLicenceEntity.typeCode} status ${LicenceStatus.INACTIVE} version ${aLicenceEntity.version}",
        ),
      )

    assertThat(eventCaptor.value)
      .extracting("licenceId", "eventType", "username", "forenames", "surname", "eventDescription")
      .isEqualTo(
        listOf(
          1L,
          LicenceEventType.INACTIVE,
          "SYSTEM",
          "SYSTEM",
          "SYSTEM",
          "Licence automatically deactivated as it is a progression licence on an older policy version for ${aLicenceEntity.forename} ${aLicenceEntity.surname}",
        ),
      )
  }

  @Test
  fun `Given the licence start date is within the notification window, it should email the COM`() {
    whenever(licenceRepository.getLicencesForProgressionDeactivation(policyV4StartDate)).thenReturn(
      listOf(
        aLicenceEntity.copy(licenceStartDate = policyV4StartDate),
      ),
    )

    val auditCaptor = ArgumentCaptor.forClass(AuditEvent::class.java)

    service.deactivateLicences()

    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())
    assertThat(auditCaptor.value)
      .extracting("licenceId", "username", "fullName", "eventType", "summary", "detail")
      .isEqualTo(
        listOf(
          1L,
          "SYSTEM",
          "SYSTEM",
          AuditEventType.SYSTEM_EVENT,
          "Licence automatically deactivated as it is a progression licence on an older policy version for ${aLicenceEntity.forename} ${aLicenceEntity.surname}",
          "ID ${aLicenceEntity.id} type ${aLicenceEntity.typeCode} status ${LicenceStatus.INACTIVE} version ${aLicenceEntity.version}",
        ),
      )

    verify(notifyService, times(1)).sendLicenceDeactivatedForProgressionEmail(
      emailAddress = "testemail1@probation.gov.uk",
      crn = "X12345",
      comFirstName = "X",
      comLastName = "Y",
      pipFirstName = "John",
      pipLastName = "Smith",
    )
  }

  @Test
  fun `Given the licence start date is beyond the notification window, it should not email the COM`() {
    whenever(licenceRepository.getLicencesForProgressionDeactivation(policyV4StartDate)).thenReturn(
      listOf(
        aLicenceEntity.copy(licenceStartDate = notificationWindowEndDate.plusDays(1)),
      ),
    )

    val auditCaptor = ArgumentCaptor.forClass(AuditEvent::class.java)

    service.deactivateLicences()

    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())
    assertThat(auditCaptor.value)
      .extracting("licenceId", "username", "fullName", "eventType", "summary", "detail")
      .isEqualTo(
        listOf(
          1L,
          "SYSTEM",
          "SYSTEM",
          AuditEventType.SYSTEM_EVENT,
          "Licence automatically deactivated as it is a progression licence on an older policy version for ${aLicenceEntity.forename} ${aLicenceEntity.surname}",
          "ID ${aLicenceEntity.id} type ${aLicenceEntity.typeCode} status ${LicenceStatus.INACTIVE} version ${aLicenceEntity.version}",
        ),
      )

    verify(notifyService, times(0)).sendLicenceDeactivatedForProgressionEmail(any(), any(), any(), any(), any(), any())
  }

  private companion object {
    val aLicenceEntity = TestData.createCrdLicence().copy()
    val aCom = communityOffenderManager()
  }
}
