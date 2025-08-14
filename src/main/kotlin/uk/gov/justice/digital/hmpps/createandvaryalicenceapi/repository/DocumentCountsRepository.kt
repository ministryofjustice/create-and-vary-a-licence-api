package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.NativeQuery
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionUploadDetail

interface DocumentCountResult {
  val uuid: String
  val count: Int
}

@Repository
interface DocumentCountsRepository : JpaRepository<AdditionalConditionUploadDetail, Long> {
  @NativeQuery(
    """
      SELECT uuid, COUNT(*) OVER()
      FROM (
        SELECT 
          d.additional_condition_id,
          UNNEST(array[d.original_data_ds_uuid, d.full_size_image_ds_uuid, s.thumbnail_image_ds_uuid]) AS uuid
        FROM additional_condition_upload_detail d
        JOIN additional_condition_upload_summary s ON s.upload_detail_id = d.id
      )
      WHERE additional_condition_id IN (:additionalConditionIds)
      GROUP BY uuid
    """,
  )
  fun countsOfDocumentsRelatedTo(additionalConditionIds: List<Long>): List<DocumentCountResult>
}
