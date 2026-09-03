package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence.Companion.SYSTEM_USER
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StaffRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.PrisonInformationService.UpdateType.MOVEMENT
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.PrisonInformationService.UpdateType.SUPPORTING_PRISON_UPDATE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.communityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createCrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PhoneDetail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.Prison
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AuditEventType.SYSTEM_EVENT
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AuditEventType.USER_EVENT

class PrisonInformationServiceTest {
  private val prisonApiClient = mock<PrisonApiClient>()
  private val staffRepository = mock<StaffRepository>()
  private val auditEventRepository = mock<AuditEventRepository>()

  private val service = PrisonInformationService(prisonApiClient, staffRepository, auditEventRepository)

  @BeforeEach
  fun reset() {
    val authentication = mock<Authentication>()
    val securityContext = mock<SecurityContext>()

    whenever(authentication.name).thenReturn(aCom.username)
    whenever(securityContext.authentication).thenReturn(authentication)
    SecurityContextHolder.setContext(securityContext)

    reset(prisonApiClient, staffRepository, auditEventRepository)
  }

  @Test
  fun `updates the licence prison details when the prison code has changed`() {
    val licence = createCrdLicence().copy(prisonCode = "MDI")
    whenever(prisonApiClient.getPrisonInformation("BAI")).thenReturn(aPrison)
    whenever(staffRepository.findByUsernameIgnoreCase(aCom.username)).thenReturn(aCom)

    service.updatePrisonInformation(MOVEMENT, listOf(licence), "BAI")

    assertThat(licence.prisonCode).isEqualTo("BAI")
    assertThat(licence.prisonDescription).isEqualTo("Belmarsh (HMP)")
    assertThat(licence.prisonTelephone).isEqualTo("0114 2565555")
    assertThat(licence.updatedByUsername).isEqualTo(aCom.username)
    assertThat(licence.updatedBy).isEqualTo(aCom)

    val auditCaptor = ArgumentCaptor.forClass(AuditEvent::class.java)
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())

    val audit = auditCaptor.value
    assertThat(audit.licenceId).isEqualTo(licence.id)
    assertThat(audit.username).isEqualTo(aCom.username)
    assertThat(audit.eventType).isEqualTo(USER_EVENT)
    assertThat(audit.summary).isEqualTo("Prison information changed for ${licence.forename} ${licence.surname} on prisoner receive event")
    assertThat(audit.changes).isEqualTo(
      mapOf(
        "field" to "prisonCode",
        "previousValue" to "MDI",
        "newValue" to "BAI",
      ),
    )
  }

  @Test
  fun `uses the SUPPORTING_PRISON_UPDATE message when updating supporting prison information`() {
    val licence = createCrdLicence().copy(prisonCode = "MDI")
    whenever(prisonApiClient.getPrisonInformation("BAI")).thenReturn(aPrison)
    whenever(staffRepository.findByUsernameIgnoreCase(aCom.username)).thenReturn(aCom)

    service.updatePrisonInformation(SUPPORTING_PRISON_UPDATE, listOf(licence), "BAI")

    val auditCaptor = ArgumentCaptor.forClass(AuditEvent::class.java)
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())

    assertThat(auditCaptor.value.summary).isEqualTo("Supporting prison information changed for ${licence.forename} ${licence.surname}")
  }

  @Test
  fun `updates all licences in a list`() {
    val licence1 = createCrdLicence().copy(id = 1L, prisonCode = "MDI")
    val licence2 = createCrdLicence().copy(id = 2L, prisonCode = "MDI")
    whenever(prisonApiClient.getPrisonInformation("BAI")).thenReturn(aPrison)
    whenever(staffRepository.findByUsernameIgnoreCase(aCom.username)).thenReturn(aCom)

    service.updatePrisonInformation(MOVEMENT, listOf(licence1, licence2), "BAI")

    assertThat(licence1.prisonCode).isEqualTo("BAI")
    assertThat(licence2.prisonCode).isEqualTo("BAI")
    verify(auditEventRepository, times(2)).saveAndFlush(any())
  }

  @Test
  fun `does nothing when the prison code has not changed`() {
    val licence = createCrdLicence().copy(prisonCode = "BAI")
    whenever(prisonApiClient.getPrisonInformation("BAI")).thenReturn(aPrison)

    service.updatePrisonInformation(MOVEMENT, listOf(licence), "BAI")

    assertThat(licence.prisonDescription).isNotEqualTo("Belmarsh (HMP)")
    verify(auditEventRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `records a system event and SYSTEM_USER username when no staff member is found for the current user`() {
    val licence = createCrdLicence().copy(prisonCode = "MDI")
    whenever(prisonApiClient.getPrisonInformation("BAI")).thenReturn(aPrison)
    whenever(staffRepository.findByUsernameIgnoreCase(aCom.username)).thenReturn(null)

    service.updatePrisonInformation(MOVEMENT, listOf(licence), "BAI")

    assertThat(licence.updatedByUsername).isEqualTo(SYSTEM_USER)

    val auditCaptor = ArgumentCaptor.forClass(AuditEvent::class.java)
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())

    val audit = auditCaptor.value
    assertThat(audit.username).isEqualTo(SYSTEM_USER)
    assertThat(audit.fullName).isEqualTo(SYSTEM_USER)
    assertThat(audit.eventType).isEqualTo(SYSTEM_EVENT)
  }

  private companion object {
    val aCom = communityOffenderManager()

    val aPrison = Prison(
      prisonId = "BAI",
      description = "Belmarsh (HMP)",
      phoneDetails = listOf(
        PhoneDetail(phoneId = 1, number = "0114 2565555", type = "BUS", ext = null),
      ),
    )
  }
}
