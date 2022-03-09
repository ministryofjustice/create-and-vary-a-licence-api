package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus

@Service
class
OffenderService(
  private val licenceRepository: LicenceRepository,
  private val auditEventRepository: AuditEventRepository,
) {

  @Transactional
  fun updateOffenderWithResponsibleCom(crn: String, newCom: CommunityOffenderManager) {
    val inFlightLicenceStatuses = listOf(LicenceStatus.IN_PROGRESS, LicenceStatus.SUBMITTED, LicenceStatus.APPROVED, LicenceStatus.ACTIVE)
    var offenderLicences = this.licenceRepository.findAllByCrnAndStatusCodeIn(crn, inFlightLicenceStatuses)

    // Update the active licences for this person on probation
    offenderLicences = offenderLicences.map { it.copy(responsibleCom = newCom) }
    this.licenceRepository.saveAllAndFlush(offenderLicences)

    // Create an audit event for each of the licences updated
    offenderLicences.map {
      auditEventRepository.saveAndFlush(
        transform(
          AuditEvent(
            licenceId = it.id,
            username = "SYSTEM",
            fullName = "SYSTEM",
            summary = "COM updated to ${newCom?.firstName} ${newCom?.lastName} on licence for ${it.forename} ${it.surname}",
            detail = "ID ${it.id} type ${it.typeCode} status ${it.statusCode.name} version ${it.version}",
          )
        )
      )
    }
  }
}
