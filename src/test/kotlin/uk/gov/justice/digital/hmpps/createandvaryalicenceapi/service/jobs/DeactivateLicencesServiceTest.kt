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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StaffRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.DomainEventsService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AuditEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus

class DeactivateLicencesServiceTest {
  private val licenceRepository = mock<LicenceRepository>()
  private val auditEventRepository = mock<AuditEventRepository>()
  private val licenceEventRepository = mock<LicenceEventRepository>()
  private val domainEventsService = mock<DomainEventsService>()
  private val staffRepository = mock<StaffRepository>()

  private val service = DeactivateLicencesService(
    licenceRepository,
    auditEventRepository,
    licenceEventRepository,
    domainEventsService,
    staffRepository,
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
      licenceEventRepository,
      staffRepository,
    )
  }

  @Test
  fun `Given there no licences with release date in past When deactivateLicencesJob Then should not deactivate any licence`() {
    whenever(licenceRepository.getDraftLicencesPassedReleaseDate()).thenReturn(emptyList())
    service.deactivateLicences()
    verify(licenceRepository, times(1)).getDraftLicencesPassedReleaseDate()
    verify(licenceRepository, times(0)).saveAllAndFlush(emptyList())
    verify(auditEventRepository, times(0)).saveAndFlush(any())
    verify(licenceEventRepository, times(0)).saveAndFlush(any())
  }

  @Test
  fun `Given there are licence with release date in past When deactivateLicencesJob Then should deactivate the licence`() {
    whenever(licenceRepository.getDraftLicencesPassedReleaseDate()).thenReturn(
      listOf(
        aLicenceEntity,
      ),
    )
    whenever(staffRepository.findByUsernameIgnoreCase(aCom.username)).thenReturn(aCom)

    service.deactivateLicences()

    val licenceCaptor = argumentCaptor<List<Licence>>()
    val auditCaptor = ArgumentCaptor.forClass(AuditEvent::class.java)
    val eventCaptor = ArgumentCaptor.forClass(LicenceEvent::class.java)

    verify(licenceRepository, times(1)).getDraftLicencesPassedReleaseDate()

    verify(licenceRepository, times(1)).saveAllAndFlush(licenceCaptor.capture())

    assertThat(licenceCaptor.firstValue[0])
      .extracting("statusCode", "updatedByUsername", "updatedBy")
      .isEqualTo(listOf(LicenceStatus.INACTIVE, aCom.username, aCom))

    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())
    verify(licenceEventRepository, times(1)).saveAndFlush(eventCaptor.capture())
    verify(domainEventsService, times(1)).recordDomainEvent(aLicenceEntity, LicenceStatus.INACTIVE)
    verify(staffRepository, times(1)).findByUsernameIgnoreCase(aCom.username)

    assertThat(auditCaptor.value)
      .extracting("licenceId", "username", "fullName", "eventType", "summary", "detail")
      .isEqualTo(
        listOf(
          1L,
          "smills",
          "smills",
          AuditEventType.SYSTEM_EVENT,
          "Licence deactivated automatically as it passed release date for ${aLicenceEntity.forename} ${aLicenceEntity.surname}",
          "ID ${aLicenceEntity.id} type ${aLicenceEntity.typeCode} status ${LicenceStatus.INACTIVE} version ${aLicenceEntity.version}",
        ),
      )

    assertThat(eventCaptor.value)
      .extracting("licenceId", "eventType", "username", "forenames", "surname", "eventDescription")
      .isEqualTo(
        listOf(
          1L,
          LicenceEventType.INACTIVE,
          "smills",
          "smills",
          "smills",
          "Licence deactivated automatically as it passed release date for ${aLicenceEntity.forename} ${aLicenceEntity.surname}",
        ),
      )
  }

  private companion object {
    val aLicenceEntity = TestData.createCrdLicence().copy()
    val aCom = TestData.com()
  }
}
