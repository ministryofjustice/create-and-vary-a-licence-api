package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence

@Repository
interface ISRProgressionLicenceRepository : JpaRepository<Licence, Long> {

  @Query(
    value = """
            SELECT l.id
            FROM licence l
            WHERE l.status_code IN ('IN_PROGRESS','SUBMITTED','APPROVED','ACTIVE','VARIATION_IN_PROGRESS','VARIATION_SUBMITTED','VARIATION_REJECTED','VARIATION_APPROVED','VARIATION_REJECTED')
              AND l.type_code = :typeCode ORDER BY l.id
              """,
    nativeQuery = true,
  )
  fun findInFlightAndActiveLicenceIds(@Param("typeCode") typeCode: String): List<Long>
}
