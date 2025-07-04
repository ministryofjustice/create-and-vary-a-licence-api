package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HardStopLicence
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Repository
interface HardStopLicenceRepository :
  JpaRepository<HardStopLicence, Long>,
  JpaSpecificationExecutor<HardStopLicence> {

  @Query(
    """
  SELECT l FROM HardStopLicence l
    WHERE l.kind = 'HARD_STOP'
      AND l.reviewDate IS NULL
      AND l.licenceActivatedDate BETWEEN :start AND :end
    """,
  )
  fun getHardStopLicencesNeedingReview(
    start: LocalDateTime = LocalDate.now().minusDays(5).atStartOfDay(),
    end: LocalDateTime = LocalDate.now().minusDays(5).atTime(LocalTime.MAX),
  ): List<HardStopLicence>
}
