package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AuditEvent
import java.time.LocalDateTime

@Repository
interface AuditEventRepository : JpaRepository<AuditEvent, Long> {
  fun findAllByLicenceIdIn(licenceIds: List<Long>): List<AuditEvent>
  fun findAllByLicenceIdAndEventTimeBetweenOrderByEventTimeDesc(
    licenceId: Long,
    startTime: LocalDateTime,
    endTime: LocalDateTime,
  ): List<AuditEvent>

  fun findAllByUsernameAndEventTimeBetweenOrderByEventTimeDesc(
    username: String,
    startTime: LocalDateTime,
    endTime: LocalDateTime,
  ): List<AuditEvent>

  fun findAllByLicenceIdAndUsernameAndEventTimeBetweenOrderByEventTimeDesc(
    licenceId: Long,
    username: String,
    startTime: LocalDateTime,
    endTime: LocalDateTime,
  ): List<AuditEvent>

  fun findAllByEventTimeBetweenOrderByEventTimeDesc(startTime: LocalDateTime, endTime: LocalDateTime): List<AuditEvent>
}
