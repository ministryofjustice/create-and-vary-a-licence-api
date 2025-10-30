package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.NomisTimeServedLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.RecordNomisLicenceReasonRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateNomisLicenceReasonRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response.NomisLicenceReasonResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.NomisTimeServedLicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StaffRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AuditEventType
import java.time.LocalDateTime

@Service
class NomisTimeServedLicenceService(
  private val licenceRepository: NomisTimeServedLicenceRepository,
  private val staffRepository: StaffRepository,
  private val auditEventRepository: AuditEventRepository,
) {

  @Transactional
  fun recordNomisLicenceReason(request: RecordNomisLicenceReasonRequest) {
    // Fetch Staff entity for updatedByCaId
    val staff = staffRepository.findById(request.updatedByCaId)
      .orElseThrow { IllegalArgumentException("Staff with ID ${request.updatedByCaId} not found") }

    // Create entity
    val licenceRecord = NomisTimeServedLicence(
      nomsId = request.nomsId,
      bookingId = request.bookingId,
      reason = request.reason,
      prisonCode = request.prisonCode,
      updatedByCa = staff,
    )

    // Save
    licenceRepository.saveAndFlush(licenceRecord)

    val summary = "Recorded NOMIS licence reason"
    val detail = """
        Created NOMIS licence record:
        ID=${licenceRecord.id}, nomsId=${licenceRecord.nomsId}, bookingId=${licenceRecord.bookingId},
        reason=${licenceRecord.reason}, prisonCode=${licenceRecord.prisonCode}
    """.trimIndent()

    val changes = mutableMapOf<String, Any>(
      "nomsId" to licenceRecord.nomsId,
      "bookingId" to licenceRecord.bookingId,
      "reason" to licenceRecord.reason,
      "prisonCode" to licenceRecord.prisonCode,
    )

    auditEventRepository.saveAndFlush(
      AuditEvent(
        detail = detail,
        eventTime = LocalDateTime.now(),
        eventType = AuditEventType.USER_EVENT,
        username = staff.username,
        fullName = staff.fullName,
        summary = summary,
        changes = changes,
      ),
    )
  }

  @Transactional
  fun updateNomisLicenceReason(nomsId: String, bookingId: Int, request: UpdateNomisLicenceReasonRequest) {
    // Fetch existing licence record
    val licence = licenceRepository.findByNomsIdAndBookingId(nomsId, bookingId)
      .orElseThrow { IllegalArgumentException("Reason record with nomsId $nomsId and bookingId $bookingId not found") }

    // Fetch Staff entity for updatedByCaId
    val staff = staffRepository.findById(request.updatedByCaId)
      .orElseThrow { IllegalArgumentException("Staff with ID ${request.updatedByCaId} not found") }

    val oldReason = licence.reason

    // Update fields
    licence.reason = request.reason
    licence.updatedByCa = staff
    licence.dateLastUpdated = LocalDateTime.now()

    val updatedLicence = licenceRepository.saveAndFlush(licence)

    val summary = "Updated NOMIS licence reason"
    val detail = """
        Updated NOMIS licence record:
        ID=${updatedLicence.id}, nomsId=${updatedLicence.nomsId}, bookingId=${updatedLicence.bookingId},
        oldReason=$oldReason, newReason=${updatedLicence.reason}, prisonCode=${updatedLicence.prisonCode}
    """.trimIndent()

    val changes = mutableMapOf<String, Any>(
      "reason (old)" to oldReason,
      "reason (new)" to updatedLicence.reason,
      "nomsId" to updatedLicence.nomsId,
      "bookingId" to updatedLicence.bookingId,
      "prisonCode" to updatedLicence.prisonCode,
    )

    auditEventRepository.saveAndFlush(
      AuditEvent(
        detail = detail,
        eventTime = LocalDateTime.now(),
        eventType = AuditEventType.USER_EVENT,
        username = staff.username,
        fullName = staff.fullName,
        summary = summary,
        changes = changes,
      ),
    )
  }

  fun findByNomsIdAndBookingId(nomsId: String, bookingId: Int): NomisLicenceReasonResponse? = licenceRepository.findLicenceReasonByNomsIdAndBookingId(nomsId, bookingId)
}
