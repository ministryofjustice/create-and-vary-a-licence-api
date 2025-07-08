package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionUploadDetail

@Repository
interface AdditionalConditionUploadDetailRepository : JpaRepository<AdditionalConditionUploadDetail, Long> {
  @Query(
    """
    SELECT aud
    FROM AdditionalConditionUploadDetail aud
    WHERE aud.fullSizeImageDsUuid IS NULL
    OR aud.originalDataDsUuid IS NULL
    ORDER BY aud.id DESC
    LIMIT :limit
  """,
  )
  fun toBeMigrated(limit: Int? = null): List<AdditionalConditionUploadDetail>

  @Query(
    """
    SELECT COUNT(aud)
    FROM AdditionalConditionUploadDetail aud
    WHERE aud.fullSizeImageDsUuid IS NULL
    OR aud.originalDataDsUuid IS NULL
  """,
  )
  fun totalToBeMigrated(): Long
}
