package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
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
            WHERE l.status_code IN ('IN PROGRESS','SUBMITTED','APPROVED')
              AND l.topup_supervision_start_date >= :cutoffDate
              AND l.type_code = :typeCode
              """,
    nativeQuery = true,
  )
  fun findLicenceIds(
    @Param("cutoffDate") cutoffDate: LocalDate,
    @Param("typeCode") typeCode: String,
  ): List<Long>

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
    value = """
            UPDATE licence SET type_code = 'AP' WHERE id IN (:ids) AND type_code = :typeCode
        """,
    nativeQuery = true,
  )
  fun updateTypeCodeToAp(
    @Param("ids") ids: List<Long>,
    @Param("typeCode") typeCode: String,
  ): Int

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
    value = """
        DELETE FROM standard_condition WHERE licence_id IN (:licenceIds) AND condition_type = 'PSS'
    """,
    nativeQuery = true,
  )
  fun deletePssStandardConditions(
    licenceIds: List<Long>,
  ): Int

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
    value = """
        DELETE FROM additional_condition WHERE licence_id IN (:licenceIds) AND condition_type = 'PSS'
    """,
    nativeQuery = true,
  )
  fun deletePssAdditionalConditions(
    licenceIds: List<Long>,
  ): Int
}
