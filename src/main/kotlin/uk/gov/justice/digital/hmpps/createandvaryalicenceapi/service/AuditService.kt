package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AuditRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AuditEvent as ModelAuditEvent

@Service
class AuditService(
  private val auditEventRepository: AuditEventRepository,
  private val licenceRepository: LicenceRepository,
) {

  fun recordAuditEvent(auditEvent: ModelAuditEvent) {
    auditEventRepository.save(transform(auditEvent))
  }

  fun getAuditEvents(auditRequest: AuditRequest): List<ModelAuditEvent> {
    return if (auditRequest.licenceId != null && auditRequest.username != null) {
      getAuditEventsForLicenceAndUser(auditRequest)
    } else if (auditRequest.licenceId != null) {
      getAuditEventsForLicence(auditRequest)
    } else if (auditRequest.username != null) {
      getAuditEventsForUser(auditRequest)
    } else {
      getAllEvents(auditRequest)
    }
  }

  private fun getAuditEventsForLicence(auditRequest: AuditRequest): List<ModelAuditEvent> {
    licenceRepository
      .findById(auditRequest.licenceId!!)
      .orElseThrow { EntityNotFoundException("${auditRequest.licenceId}") }

    return auditEventRepository
      .findAllByLicenceIdAndEventTimeBetweenOrderByEventTimeDesc(
        auditRequest.licenceId,
        auditRequest.startTime,
        auditRequest.endTime,
      )
      .transformToModelAuditEvents()
  }

  private fun getAuditEventsForUser(auditRequest: AuditRequest): List<ModelAuditEvent> {
    return auditEventRepository
      .findAllByUsernameAndEventTimeBetweenOrderByEventTimeDesc(
        auditRequest.username!!,
        auditRequest.startTime,
        auditRequest.endTime,
      )
      .transformToModelAuditEvents()
  }

  private fun getAuditEventsForLicenceAndUser(auditRequest: AuditRequest): List<ModelAuditEvent> {
    licenceRepository
      .findById(auditRequest.licenceId!!)
      .orElseThrow { EntityNotFoundException("${auditRequest.licenceId}") }

    return auditEventRepository
      .findAllByLicenceIdAndUsernameAndEventTimeBetweenOrderByEventTimeDesc(
        auditRequest.licenceId,
        auditRequest.username!!,
        auditRequest.startTime,
        auditRequest.endTime,
      )
      .transformToModelAuditEvents()
  }

  private fun getAllEvents(auditRequest: AuditRequest): List<ModelAuditEvent> {
    return auditEventRepository
      .findAllByEventTimeBetweenOrderByEventTimeDesc(auditRequest.startTime, auditRequest.endTime)
      .transformToModelAuditEvents()
  }
}
