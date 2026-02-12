package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository

import jakarta.persistence.LockModeType
import jakarta.persistence.QueryHint
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.QueryHints
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.PotentialHardstopCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.PotentialHardstopCaseStatus
import java.time.LocalDateTime

interface PotentialHardstopCaseRepository : JpaRepository<PotentialHardstopCase, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @QueryHints(value = [QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")])
  fun findAllByStatusAndDateCreatedBefore(
    status: PotentialHardstopCaseStatus,
    dateCreatedBefore: LocalDateTime,
  ): List<PotentialHardstopCase>
}
