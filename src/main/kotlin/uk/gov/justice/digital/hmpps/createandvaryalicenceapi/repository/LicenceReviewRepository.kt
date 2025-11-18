package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Repository
interface LicenceReviewRepository : JpaRepository<Licence, Long> {

  @Query(
    """
  SELECT l FROM Licence l
    WHERE l.kind in ('HARD_STOP', 'TIME_SERVED')
      AND l.reviewDate IS NULL
      AND l.licenceActivatedDate BETWEEN :start AND :end
    """,
  )
  fun getLicencesNeedingReview(
    start: LocalDateTime = LocalDate.now().minusDays(5).atStartOfDay(),
    end: LocalDateTime = LocalDate.now().minusDays(5).atTime(LocalTime.MAX),
  ): List<Licence>
}
