package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionUploadSummary

@Repository
interface AdditionalConditionUploadSummaryRepository : JpaRepository<AdditionalConditionUploadSummary, Long> {
  @Query(
    """
    select aus 
    from AdditionalConditionUploadSummary aus 
    where aus.thumbnailImageDsUuid is null
    order by aus.id desc
    limit :limit
  """,
  )
  fun toBeMigrated(limit: Int = 100): List<AdditionalConditionUploadSummary>
}
