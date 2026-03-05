package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AuditEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDateTime

@Service
class ISRPssProgressionChunkService(
  private val licenceRepository: LicenceRepository,
  private val auditEventRepository: AuditEventRepository,
) {

  /**
   * Each chunk runs in its own transaction so that:
   *  1. Progress is committed per chunk
   *  2. Failures only roll back the current chunk
   *  3. Long-running transactions and large table locks are avoided
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  fun processApPssLicenceChunk(licenceIds: List<Long>) {
    if (licenceIds.isEmpty()) return

    val licences = licenceRepository.findAllById(licenceIds)
    val audits = mutableListOf<AuditEvent>()
    licences.forEach { licence ->

      if (licence.typeCode != LicenceType.AP_PSS) {
        log.warn("ISR PSS repealed licence {} skipped — unexpected type {}", licence.id, licence.typeCode)
        return@forEach
      }
      audits.add(createAuditEvent(licence, licenceIds))

      log.info("ISR PSS repealed licence {} type code {}, to AP started", licence.id, licence.typeCode)
      licence.typeCode = LicenceType.AP
      licence.additionalConditions.removeIf { it.conditionType == "PSS" }
      licence.standardConditions.removeIf { it.conditionType == "PSS" }

      log.info("ISR PSS repealed licence {}, type code {} complete", licence.id, licence.typeCode)
    }
    if (audits.isNotEmpty()) {
      auditEventRepository.saveAll(audits)
    }
    log.info("ISR progression AP+Pss chunk completed. size={} batch {} to {}", licenceIds.size, licenceIds.first(), licenceIds.last())
  }

  private fun createAuditEvent(
    licence: Licence,
    licenceIds: List<Long>,
  ): AuditEvent {
    val deletedAdditionalConditions = licence.additionalConditions.filter { it.conditionType == "PSS" }
    val deletedStandardConditions = licence.standardConditions.filter { it.conditionType == "PSS" }
    val batchStart = licenceIds.first()
    val batchEnd = licenceIds.last()
    val eventTime = LocalDateTime.now()

    val summary = "Licence type automatically changed to AP for ${licence.forename} ${licence.surname} due to PSS repeal"
    val detail = "ID ${licence.id} type ${licence.typeCode} " +
      "status ${licence.statusCode.name} " +
      "version ${licence.version} " +
      "batch details: $batchStart to $batchEnd"

    val changes = mapOf(
      "type" to summary,
      "changes" to mapOf(
        "oldTypeCode" to licence.typeCode.name,
        "newTypeCode" to LicenceType.AP.name,
        "additional conditions deleted for " to deletedAdditionalConditions.map { it.conditionText },
        "standard conditions deleted for " to deletedStandardConditions.map { it.conditionText },
      ),
    )

    return AuditEvent(
      licenceId = licence.id,
      detail = detail,
      eventTime = eventTime,
      eventType = AuditEventType.SYSTEM_EVENT,
      username = Licence.SYSTEM_USER,
      fullName = Licence.SYSTEM_USER,
      summary = summary,
      changes = changes,
    )
  }

  companion object {
    private val log = org.slf4j.LoggerFactory.getLogger(this::class.java)
  }
}
