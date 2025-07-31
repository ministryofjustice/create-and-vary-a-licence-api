package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.NativeQuery
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionUploadDetail

interface DocumentCountResult {
  val uuid: String
  val count: Int
}

data class DocumentCount(override val uuid: String, override val count: Int) : DocumentCountResult

@Repository
interface DocumentCountsRepository : JpaRepository<AdditionalConditionUploadDetail, Long> {
  @NativeQuery(
    """
      SELECT uuid, COUNT(uuid) AS count
      FROM (
        SELECT full_size_image_ds_uuid AS uuid
        FROM additional_condition_upload_detail
        WHERE additional_condition_id IN (:additionalConditionIds)
        UNION ALL
        SELECT original_data_ds_uuid AS uuid
        FROM additional_condition_upload_detail
        WHERE additional_condition_id IN (:additionalConditionIds)
        UNION ALL
        SELECT thumbnail_image_ds_uuid AS uuid
        FROM additional_condition_upload_summary
        WHERE additional_condition_id IN (:additionalConditionIds)
      )
      GROUP BY uuid
    """,
  )
  fun countsOfDocumentsRelatedTo(additionalConditionIds: List<Long>): List<DocumentCountResult>
}
