package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionUploadSummary

@Repository
interface AdditionalConditionUploadSummaryRepository : JpaRepository<AdditionalConditionUploadSummary, Long> {
  @Query(
    """
    SELECT aus 
    FROM AdditionalConditionUploadSummary aus 
    WHERE aus.thumbnailImageDsUuid IS NULL
    ORDER BY aus.id DESC
    LIMIT :limit
  """,
  )
  fun toBeMigrated(limit: Int): List<AdditionalConditionUploadSummary>
}
