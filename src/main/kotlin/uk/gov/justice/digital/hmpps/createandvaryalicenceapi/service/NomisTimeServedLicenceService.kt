package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.NomisTimeServedLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Staff
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
    val staff = getCurrentStaff()

    val licenceRecord = licenceRepository.saveAndFlush(
      NomisTimeServedLicence(
        nomsId = request.nomsId,
        bookingId = request.bookingId,
        reason = request.reason,
        prisonCode = request.prisonCode,
        updatedByCa = staff,
      ),
    )

    saveAuditEvent(
      summary = "Recorded NOMIS licence reason",
      detail = """
                Created NOMIS licence record:
                ID=${licenceRecord.id}, nomsId=${licenceRecord.nomsId}, bookingId=${licenceRecord.bookingId},
                reason=${licenceRecord.reason}, prisonCode=${licenceRecord.prisonCode}
      """.trimIndent(),
      staff = staff,
      changes = mapOf(
        "nomsId" to licenceRecord.nomsId,
        "bookingId" to licenceRecord.bookingId,
        "reason" to licenceRecord.reason,
        "prisonCode" to licenceRecord.prisonCode,
      ),
    )
  }

  @Transactional
  fun updateNomisLicenceReason(nomsId: String, bookingId: Long, request: UpdateNomisLicenceReasonRequest) {
    val licence = licenceRepository.findByNomsIdAndBookingId(nomsId, bookingId)
      .orElseThrow { EntityNotFoundException("Reason record with nomsId $nomsId and bookingId $bookingId not found") }

    val staff = getCurrentStaff()
    val oldReason = licence.reason

    licence.reason = request.reason
    licence.updatedByCa = staff
    licence.dateLastUpdated = LocalDateTime.now()

    val updatedLicence = licenceRepository.saveAndFlush(licence)

    saveAuditEvent(
      summary = "Updated NOMIS licence reason",
      detail = """
                Updated NOMIS licence record:
                ID=${updatedLicence.id}, nomsId=${updatedLicence.nomsId}, bookingId=${updatedLicence.bookingId},
                oldReason=$oldReason, newReason=${updatedLicence.reason}, prisonCode=${updatedLicence.prisonCode}
      """.trimIndent(),
      staff = staff,
      changes = mapOf(
        "reason (old)" to oldReason,
        "reason (new)" to updatedLicence.reason,
        "nomsId" to updatedLicence.nomsId,
        "bookingId" to updatedLicence.bookingId,
        "prisonCode" to updatedLicence.prisonCode,
      ),
    )
  }

  @Transactional(readOnly = true)
  fun findByNomsIdAndBookingId(nomsId: String, bookingId: Long): NomisLicenceReasonResponse? = licenceRepository.findLicenceReasonByNomsIdAndBookingId(nomsId, bookingId)

  @Transactional
  fun deleteNomisLicenceReason(nomsId: String, bookingId: Long) {
    val reasonEntity = licenceRepository.findByNomsIdAndBookingId(nomsId, bookingId).orElse(null)
    val staff = getCurrentStaff()

    if (reasonEntity == null) {
      // Log audit for attempted deletion with no record found
      saveAuditEvent(
        summary = "Attempted to delete NOMIS licence reason - No record found",
        detail = """
                No NOMIS licence record found for:
                nomsId=$nomsId, bookingId=$bookingId
        """.trimIndent(),
        staff = staff,
        changes = mapOf(
          "nomsId" to nomsId,
          "bookingId" to bookingId,
          "status" to "No record found",
        ),
      )
      return
    }

    // Normal deletion flow
    saveAuditEvent(
      summary = "Deleted NOMIS licence reason",
      detail = """
            Deleted NOMIS licence record:
            ID=${reasonEntity.id}, nomsId=${reasonEntity.nomsId}, bookingId=${reasonEntity.bookingId},
            reason=${reasonEntity.reason}, prisonCode=${reasonEntity.prisonCode}
      """.trimIndent(),
      staff = staff,
      changes = mapOf(
        "reason (deleted)" to reasonEntity.reason,
        "nomsId" to reasonEntity.nomsId,
        "bookingId" to reasonEntity.bookingId,
        "prisonCode" to reasonEntity.prisonCode,
      ),
    )

    licenceRepository.delete(reasonEntity)
  }

  // ✅ Common helper methods
  private fun getCurrentStaff(): Staff {
    val username = SecurityContextHolder.getContext().authentication.name
    return staffRepository.findByUsernameIgnoreCase(username)
      ?: throw IllegalStateException("Staff with username $username not found")
  }

  private fun saveAuditEvent(summary: String, detail: String, staff: Staff, changes: Map<String, Any>) {
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
}
