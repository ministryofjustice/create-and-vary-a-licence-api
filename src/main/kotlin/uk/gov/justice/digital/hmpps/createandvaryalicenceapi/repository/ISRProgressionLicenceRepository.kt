package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import java.time.LocalDate

@Repository
interface ISRProgressionLicenceRepository : JpaRepository<Licence, Long> {

  @Query(
    value = """
            SELECT l.id
            FROM licence l
            WHERE l.status_code IN ('IN_PROGRESS','SUBMITTED','APPROVED')
              AND l.topup_supervision_start_date >= :cutoffDate
              AND l.type_code = :typeCode ORDER BY l.id
              """,
    nativeQuery = true,
  )
  fun findLicenceIds(
    @Param("cutoffDate") cutoffDate: LocalDate,
    @Param("typeCode") typeCode: String,
  ): List<Long>
}
