package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs

import org.slf4j.LoggerFactory
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.LicenceEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StaffRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.DomainEventsService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AuditEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus

@Service
class DeactivateLicencesService(
  private val licenceRepository: LicenceRepository,
  private val auditEventRepository: AuditEventRepository,
  private val licenceEventRepository: LicenceEventRepository,
  private val domainEventsService: DomainEventsService,
  private val staffRepository: StaffRepository,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  fun deactivateLicences() {
    log.info("Job deactivateLicencesJob started")
    val licencesToDeactivate = licenceRepository.getDraftLicencesPassedReleaseDate()
    if (licencesToDeactivate.isEmpty()) {
      log.info("Job deactivateLicencesJob has no licences to deactivate")
      return
    }
    log.info("deactivateLicencesJob is updating status INACTIVE on ${licencesToDeactivate.size} licences")
    updateLicencesStatus(licencesToDeactivate)
    log.info("deactivateLicencesJob updated status to INACTIVE on ${licencesToDeactivate.size} licences")
  }

  private fun updateLicencesStatus(licences: List<Licence>, reason: String? = null) {
    val authentication: Authentication = SecurityContextHolder.getContext().authentication
    val userName = if (authentication !is AnonymousAuthenticationToken) authentication.name else "SYSTEM"
    val staffMember = this.staffRepository.findByUsernameIgnoreCase(userName)

    licences.forEach { it.deactivate(staffMember) }
    licenceRepository.saveAllAndFlush(licences)

    licences.forEach { licence ->
      auditEventRepository.saveAndFlush(
        AuditEvent(
          licenceId = licence.id,
          username = userName,
          fullName = userName,
          eventType = AuditEventType.SYSTEM_EVENT,
          summary = "${reason ?: "Licence deactivated automatically as it passed release date"} for ${licence.forename} ${licence.surname}",
          detail = "ID ${licence.id} type ${licence.typeCode} status ${licence.statusCode.name} version ${licence.version}",
        ),
      )

      licenceEventRepository.saveAndFlush(
        LicenceEvent(
          licenceId = licence.id,
          eventType = LicenceEventType.INACTIVE,
          username = userName,
          forenames = userName,
          surname = userName,
          eventDescription = "${reason ?: "Licence deactivated automatically as it passed release date"} for ${licence.forename} ${licence.surname}",
        ),
      )
      domainEventsService.recordDomainEvent(licence, LicenceStatus.INACTIVE)
    }
  }
}
