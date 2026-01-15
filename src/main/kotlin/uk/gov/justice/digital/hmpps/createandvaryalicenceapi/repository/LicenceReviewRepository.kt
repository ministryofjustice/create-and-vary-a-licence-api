package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import java.time.LocalDateTime

@Repository
interface LicenceReviewRepository : JpaRepository<Licence, Long> {

  @Query(
    """
  SELECT l FROM Licence l
    WHERE l.kind in ('HARD_STOP', 'TIME_SERVED')
      AND l.reviewDate IS NULL
      AND l.licenceActivatedDate BETWEEN :start AND :end
      ORDER BY l.id
    """,
  )
  fun getLicencesNeedingReview(start: LocalDateTime, end: LocalDateTime): List<Licence>
}
