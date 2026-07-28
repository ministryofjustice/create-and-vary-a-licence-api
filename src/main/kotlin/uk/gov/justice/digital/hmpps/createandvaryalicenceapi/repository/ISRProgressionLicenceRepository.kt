package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence

@Repository
interface ISRProgressionLicenceRepository : JpaRepository<Licence, Long> {
  @Query(
    value = """
            SELECT l.id
            FROM licence l
            WHERE l.status_code IN ('IN_PROGRESS','SUBMITTED','APPROVED','VARIATION_IN_PROGRESS','VARIATION_SUBMITTED','VARIATION_REJECTED','VARIATION_APPROVED','VARIATION_REJECTED')
              """,
    nativeQuery = true,
  )
  fun findInFlightLicenceIds(): List<Long>
}
