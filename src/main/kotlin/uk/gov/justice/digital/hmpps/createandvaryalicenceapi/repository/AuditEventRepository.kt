package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AuditEvent
import java.time.LocalDateTime

@Repository
interface AuditEventRepository : JpaRepository<AuditEvent, Long> {
  fun findAllByLicenceIdIn(licenceIds: List<Long>): List<AuditEvent>

  @Query(
    """
        SELECT ae FROM AuditEvent ae
        WHERE ae.licenceId = :licenceId
        AND (cast(:startTime as timestamp) IS NULL OR ae.eventTime >= :startTime)
        AND (cast(:endTime as timestamp) IS NULL OR ae.eventTime <= :endTime)
    """,
  )
  fun findAllByLicenceIdAndEventTimeBetweenOrderByEventTimeDesc(
    licenceId: Long,
    startTime: LocalDateTime?,
    endTime: LocalDateTime?,
  ): List<AuditEvent>

  @Query(
    """
        SELECT ae FROM AuditEvent ae
        WHERE ae.username = :username
        AND (cast(:startTime as timestamp) IS NULL OR ae.eventTime >= :startTime)
        AND (cast(:endTime as timestamp) IS NULL OR ae.eventTime <= :endTime)
    """,
  )
  fun findAllByUsernameAndEventTimeBetweenOrderByEventTimeDesc(
    username: String,
    startTime: LocalDateTime?,
    endTime: LocalDateTime?,
  ): List<AuditEvent>

  @Query(
    """
        SELECT ae FROM AuditEvent ae
        WHERE ae.licenceId = :licenceId
        AND ae.username = :username
        AND (cast(:startTime as timestamp ) IS NULL OR ae.eventTime >= :startTime)
        AND (cast(:endTime as timestamp) IS NULL OR ae.eventTime <= :endTime)
    """,
  )
  fun findAllByLicenceIdAndUsernameAndEventTimeBetweenOrderByEventTimeDesc(
    licenceId: Long,
    username: String,
    startTime: LocalDateTime?,
    endTime: LocalDateTime?,
  ): List<AuditEvent>

  @Query(
    """
        SELECT ae FROM AuditEvent ae
        WHERE (cast(:startTime as timestamp) IS NULL OR ae.eventTime >= :startTime)
        AND (cast(:endTime as timestamp) IS NULL OR ae.eventTime <= :endTime)
    """,
  )
  fun findAllByEventTimeBetweenOrderByEventTimeDesc(
    startTime: LocalDateTime?,
    endTime: LocalDateTime?,
  ): List<AuditEvent>
}
