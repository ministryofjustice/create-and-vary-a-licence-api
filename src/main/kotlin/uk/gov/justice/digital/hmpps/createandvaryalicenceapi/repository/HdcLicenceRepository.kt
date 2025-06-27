package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcLicence
import java.time.LocalDate

@Repository
interface HdcLicenceRepository :
  JpaRepository<HdcLicence, Long>,
  JpaSpecificationExecutor<HdcLicence> {

  @Query(
    """
  SELECT l FROM HdcLicence l
    WHERE l.kind = 'HDC'
      AND l.statusCode IN ('IN_PROGRESS', 'SUBMITTED', 'APPROVED')
      AND l.conditionalReleaseDate <= :cutoffDate
""",
  )
  fun getDraftLicencesIneligibleForHdcRelease(cutoffDate: LocalDate? = LocalDate.now().plusDays(9)): List<HdcLicence>
}
