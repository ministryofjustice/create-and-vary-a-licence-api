package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.NativeQuery
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionUpload

@Repository
interface AdditionalConditionUploadRepository : JpaRepository<AdditionalConditionUpload, Long> {

  @NativeQuery(
    """
      SELECT DISTINCT uuid FROM (
          SELECT original_data_ds_uuid as uuid    FROM additional_condition_upload
            WHERE additional_condition_id IN (:additionalConditionIds) AND original_data_ds_uuid IS NOT NULL
        UNION ALL
          SELECT full_size_image_ds_uuid as uuid  FROM additional_condition_upload 
            WHERE additional_condition_id IN (:additionalConditionIds) AND full_size_image_ds_uuid IS NOT NULL
        UNION ALL
          SELECT thumbnail_image_ds_uuid as uuid  FROM additional_condition_upload
            WHERE additional_condition_id IN (:additionalConditionIds) AND thumbnail_image_ds_uuid IS NOT NULL
      ) t
    """,
  )
  fun findDocumentUuidsFor(additionalConditionIds: List<Long>): List<String>

  @Query(
    """
    SELECT CASE WHEN COUNT(DISTINCT acu.id) = 1 THEN TRUE ELSE FALSE END 
        FROM AdditionalConditionUpload acu
        WHERE acu.originalDataDsUuid = :uuid OR acu.fullSizeImageDsUuid = :uuid OR acu.thumbnailImageDsUuid = :uuid
    """,
  )
  fun hasOnlyOneUpload(uuid: String): Boolean
}
