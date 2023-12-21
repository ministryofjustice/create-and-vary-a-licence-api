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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AuditEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceEventType

@Service
class DeactivateLicencesService(
  private val licenceRepository: LicenceRepository,
  private val auditEventRepository: AuditEventRepository,
  private val licenceEventRepository: LicenceEventRepository,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  fun deactivateLicencesJob() {
    log.info("Job deactivateLicencesJob started")
    val licencesToDeactivate = licenceRepository.getDraftLicencesPassedCRD()
    if (licencesToDeactivate.isEmpty()) {
      log.info("Job deactivateLicencesJob has no licences to time out")
      return
    }
    log.info("deactivateLicencesJob is updating status INACTIVE on ${licencesToDeactivate.size} licences")
    updateLicencesStatus(licencesToDeactivate)
    log.info("TimeOutLicencesServiceJob updated status TIMED_OUT on ${licencesToDeactivate.size} licences")
  }

  private fun updateLicencesStatus(licences: List<Licence>, reason: String? = null) {
    val deactivateLicences = licences.map { it.deactivate() }

    licenceRepository.saveAllAndFlush(deactivateLicences)
    var userName = "SYSTEM"
    val authentication: Authentication = SecurityContextHolder.getContext().authentication
    if (authentication !is AnonymousAuthenticationToken) {
      userName = authentication.getName()
    }

    deactivateLicences.map { licence ->
      auditEventRepository.saveAndFlush(
        AuditEvent(
          licenceId = licence.id,
          username = userName,
          fullName = userName,
          eventType = AuditEventType.SYSTEM_EVENT,
          summary = "${reason ?: "Licence deactivated automatically as it passed CRD"} for ${licence.forename} ${licence.surname}",
          detail = "ID ${licence.id} type ${licence.typeCode} status ${licence.statusCode.name} version ${licence.version}",
        ),
      )

      licenceEventRepository.saveAndFlush(
        LicenceEvent(
          licenceId = licence.id,
          eventType = LicenceEventType.TIMED_OUT,
          username = userName,
          forenames = userName,
          surname = userName,
          eventDescription = "${reason ?: "Licence deactivated automatically as it passed CRD"} for ${licence.forename} ${licence.surname}",
        ),
      )
    }
  }
}
