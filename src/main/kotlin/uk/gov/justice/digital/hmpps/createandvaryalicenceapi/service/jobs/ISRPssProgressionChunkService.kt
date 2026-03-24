package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AuditEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDateTime

/**
 * Each chunk runs in its own transaction so that:
 *  1. Progress is committed per chunk
 *  2. Failures only roll back the current chunk
 *  3. Long-running transactions and large table locks are avoided
 */
@Service
class ISRPssProgressionChunkService(
  private val licenceRepository: LicenceRepository,
  private val auditEventRepository: AuditEventRepository,
) {

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  fun processApPssLicenceChunk(licenceIds: List<Long>) {
    if (licenceIds.isEmpty()) return

    val licences = licenceRepository.findAllById(licenceIds)
    val audits = mutableListOf<AuditEvent>()
    licences.forEach { licence ->

      if (licence.typeCode != LicenceType.AP_PSS) {
        log.warn("ISR AP_PSS repealed licence {} skipped — unexpected type {}", licence.id, licence.typeCode)
        return@forEach
      }
      audits.add(createApPssAuditEvent(licence, licenceIds))

      log.info("ISR AP_PSS repealed licence {} type code {}, to AP started", licence.id, licence.typeCode)
      licence.typeCode = LicenceType.AP
      licence.additionalConditions.removeIf { it.conditionType == "PSS" }
      licence.standardConditions.removeIf { it.conditionType == "PSS" }
    }
    if (audits.isNotEmpty()) {
      auditEventRepository.saveAll(audits)
    }
    log.info("ISR AP_PSS repealed licence chunk completed. size={} batch {} to {}", licenceIds.size, licenceIds.first(), licenceIds.last())
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  fun processPssLicenceChunk(licenceIds: List<Long>) {
    if (licenceIds.isEmpty()) return
    val licences = licenceRepository.findAllById(licenceIds)
    val audits = mutableListOf<AuditEvent>()
    licences.forEach { licence ->
      audits.add(createPssAuditEvent(licence, licenceIds))
      log.info("ISR PSS repealed licence {} type code {}, to Inactive started", licence.id, licence.typeCode)
      licence.statusCode = LicenceStatus.INACTIVE
    }
    if (audits.isNotEmpty()) {
      auditEventRepository.saveAll(audits)
    }
    log.info("ISR PSS repealed licence chunk completed. size={} batch {} to {}", licenceIds.size, licenceIds.first(), licenceIds.last())
  }

  private fun createApPssAuditEvent(
    licence: Licence,
    licenceIds: List<Long>,
  ): AuditEvent {
    val deletedAdditionalConditions = licence.additionalConditions.filter { it.conditionType == "PSS" }
    val deletedStandardConditions = licence.standardConditions.filter { it.conditionType == "PSS" }

    val summary = "ISR AP_PSS licence to ${LicenceType.AP.name} for ${licence.forename} ${licence.surname} due to PSS repeal"
    val changes = mapOf(
      "type" to summary,
      "changes" to mapOf(
        "oldTypeCode" to licence.typeCode.name,
        "newTypeCode" to LicenceType.AP.name,
        "additionalConditionsDeletedFor" to deletedAdditionalConditions.joinToString(", ") { it.conditionText },
        "standardConditionsDeletedFor" to deletedStandardConditions.joinToString(", ") { it.conditionText },
      ),
    )

    return createAudit(licence, licenceIds, summary, changes)
  }

  private fun createPssAuditEvent(
    licence: Licence,
    licenceIds: List<Long>,
  ): AuditEvent {
    val summary = "ISR PSS licence changed to ${LicenceStatus.INACTIVE.name} for ${licence.forename} ${licence.surname} due to PSS repeal"
    val changes = mapOf(
      "type" to summary,
      "changes" to mapOf(
        "oldStatusCode" to licence.statusCode.name,
        "newStatusCode" to LicenceStatus.INACTIVE.name,
      ),
    )

    return createAudit(licence, licenceIds, summary, changes)
  }

  private fun getAuditDetail(
    licenceIds: List<Long>,
    licence: Licence,
  ): String {
    val batchStart = licenceIds.first()
    val batchEnd = licenceIds.last()
    val detail = "ID ${licence.id} type ${licence.typeCode} " +
      "status ${licence.statusCode.name} " +
      "version ${licence.version} " +
      "batch details: $batchStart to $batchEnd"
    return detail
  }

  private fun createAudit(
    licence: Licence,
    licenceIds: List<Long>,
    summary: String,
    changes: Map<String, Any>,
  ): AuditEvent = AuditEvent(
    licenceId = licence.id,
    detail = getAuditDetail(licenceIds, licence),
    eventTime = LocalDateTime.now(),
    eventType = AuditEventType.SYSTEM_EVENT,
    username = Licence.SYSTEM_USER,
    fullName = Licence.SYSTEM_USER,
    summary = summary,
    changes = changes,
  )

  companion object {
    private val log = org.slf4j.LoggerFactory.getLogger(this::class.java)
  }
}
