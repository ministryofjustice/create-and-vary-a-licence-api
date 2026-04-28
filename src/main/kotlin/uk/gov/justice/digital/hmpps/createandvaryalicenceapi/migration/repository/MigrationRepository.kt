package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.migration.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence

@Repository
interface MigrationRepository : JpaRepository<Licence, Long> {

  @Modifying
  @Transactional
  @Query(
    value = """
    INSERT INTO hdc_migration_condition_meta_data(
          licence_id,
      condition_id,
      hdc_condition_code,
      hdc_condition_version
    )  
    VALUES (:licenceId, :conditionId, :hdcConditionCode, :hdcConditionVersion)
  """,
    nativeQuery = true,
  )
  fun saveConditionMetaData(
    licenceId: Long,
    conditionId: Long,
    hdcConditionCode: String,
    hdcConditionVersion: Int?,
  ): Int

  @Modifying
  @Transactional
  @Query(
    value = """
    INSERT INTO hdc_migration_meta_data(
      licence_id,
      hdcLicence_id,
      licence_version,
      vary_version
    )  
    VALUES (:licenceId, :hdcLicenceId, :licenceVersion, :varyVersion)
  """,
    nativeQuery = true,
  )
  fun saveMetaData(licenceId: Long, hdcLicenceId: Long, licenceVersion: Int, varyVersion: Int)

  @Query(
    value = """
        SELECT EXISTS (
            SELECT 1 FROM hdc_migration_meta_data WHERE hdcLicence_id = :hdcLicenceId
        )
    """,
    nativeQuery = true,
  )
  fun hasBeenAlreadyMigrated(hdcLicenceId: Long): Boolean

  @Query(
    value = """
        SELECT EXISTS (
            SELECT 1 FROM licence l WHERE l.noms_id = :nomsId and l.status_code != 'INACTIVE'
        )
    """,
    nativeQuery = true,
  )
  fun hasExistingLicence(nomsId: String): Boolean
}
