package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Staff
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.TimeServedExternalRecord
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.ExternalTimeServedRecordRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response.TimeServedExternalRecordsResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StaffRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.TimeServedExternalRecordsRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AuditEventType
import java.time.LocalDateTime

private const val CREATED_MESSAGE = "TimeServed External Record Reason created"
private const val UPDATED_MESSAGE = "TimeServed External Record Reason updated"
private const val DELETED_MESSAGE = "Deleted NOMIS licence reason"

@Service
class TimeServedExternalRecordService(
  private val timesServedRecordService: TimeServedExternalRecordsRepository,
  private val staffRepository: StaffRepository,
  private val auditEventRepository: AuditEventRepository,
) {

  @Transactional
  fun setTimeServedExternalRecord(prisonNumber: String, bookingId: Long, request: ExternalTimeServedRecordRequest) {
    val record = timesServedRecordService.findByNomsIdAndBookingId(prisonNumber, bookingId)
    if (record == null) {
      createTimeServedExternalRecord(prisonNumber, bookingId, request)
    } else {
      updateTimeServedExternalRecord(record, request)
    }
  }

  private fun createTimeServedExternalRecord(
    prisonNumber: String,
    bookingId: Long,
    request: ExternalTimeServedRecordRequest,
  ) {
    val staff = getCurrentStaff()

    val licenceRecord = timesServedRecordService.saveAndFlush(
      TimeServedExternalRecord(
        nomsId = prisonNumber,
        bookingId = bookingId,
        reason = request.reason,
        prisonCode = request.prisonCode,
        updatedByCa = staff,
      ),
    )

    saveAuditEvent(
      summary = CREATED_MESSAGE,
      detail = "Created TimeServed External Record: ID=${licenceRecord.id}",
      staff = staff,
      changes = mapOf(
        "nomsId" to licenceRecord.nomsId,
        "bookingId" to licenceRecord.bookingId,
        "reason" to licenceRecord.reason,
        "prisonCode" to licenceRecord.prisonCode,
      ),
    )
  }

  private fun updateTimeServedExternalRecord(
    record: TimeServedExternalRecord,
    request: ExternalTimeServedRecordRequest,
  ) {
    val oldReason = record.reason
    val oldPrisonCode = record.prisonCode

    val staff = getCurrentStaff()

    record.reason = request.reason
    record.prisonCode = request.prisonCode
    record.updatedByCa = staff
    record.dateLastUpdated = LocalDateTime.now()

    val updatedLicence = timesServedRecordService.saveAndFlush(record)

    saveAuditEvent(
      summary = UPDATED_MESSAGE,
      detail = "Updated TimeServed External Record: ID=${updatedLicence.id}",
      staff = staff,
      changes = mapOf(
        "nomsId" to updatedLicence.nomsId,
        "bookingId" to updatedLicence.bookingId,
        "reason (old)" to oldReason,
        "reason (new)" to request.reason,
        "prisonCode (old)" to oldPrisonCode,
        "prisonCode (new)" to request.prisonCode,
      ),
    )
  }

  @Transactional(readOnly = true)
  fun findByNomsIdAndBookingId(nomsId: String, bookingId: Long): TimeServedExternalRecordsResponse? = timesServedRecordService
    .findByNomsIdAndBookingId(nomsId, bookingId)?.let {
      TimeServedExternalRecordsResponse(
        nomsId = it.nomsId,
        bookingId = it.bookingId,
        reason = it.reason,
        prisonCode = it.prisonCode,
        dateCreated = it.dateCreated,
        dateLastUpdated = it.dateLastUpdated,
      )
    }

  @Transactional
  fun deleteTimeServedExternalRecord(nomsId: String, bookingId: Long) {
    val reasonEntity = timesServedRecordService.findByNomsIdAndBookingId(nomsId, bookingId)
      ?: return

    val staff = getCurrentStaff()

    timesServedRecordService.delete(reasonEntity)

    saveAuditEvent(
      summary = DELETED_MESSAGE,
      detail = "Deleted NOMIS licence record: ID=${reasonEntity.id}",
      staff = staff,
      changes = mapOf(
        "reason (deleted)" to reasonEntity.reason,
        "nomsId" to reasonEntity.nomsId,
        "bookingId" to reasonEntity.bookingId,
        "prisonCode" to reasonEntity.prisonCode,
      ),
    )
  }

  private fun getCurrentStaff(): Staff {
    val username = SecurityContextHolder.getContext().authentication.name
    return staffRepository.findByUsernameIgnoreCase(username)
      ?: error("Staff with username $username not found")
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
