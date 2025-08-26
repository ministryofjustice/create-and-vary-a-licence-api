package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.NativeQuery
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionUploadDetail

interface DocumentCountResult {
  val uuid: String
  val count: Int
}

@Repository
interface DocumentCountsRepository : JpaRepository<AdditionalConditionUploadDetail, Long> {
  @NativeQuery(
    """
      WITH additional_condition_uuids AS (
        SELECT
          d.additional_condition_id,
          UNNEST(array[d.original_data_ds_uuid, d.full_size_image_ds_uuid, s.thumbnail_image_ds_uuid]) AS uuid
        FROM additional_condition_upload_detail d
        JOIN additional_condition_upload_summary s ON s.upload_detail_id = d.id
      )
      SELECT uuid_totals.uuid, uuid_totals.count
      FROM (
        SELECT uuid, COUNT(uuid) AS count
        FROM additional_condition_uuids
        GROUP BY uuid
      ) uuid_totals
      JOIN additional_condition_uuids 
      ON additional_condition_uuids.uuid = uuid_totals.uuid 
      AND additional_condition_uuids.additional_condition_id IN (:additionalConditionIds)
    """,
  )
  @Transactional(readOnly = true)
  fun countsOfDocumentsRelatedTo(additionalConditionIds: List<Long>): List<DocumentCountResult>
}
