package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionUploadDetail

@Repository
interface AdditionalConditionUploadDetailRepository : JpaRepository<AdditionalConditionUploadDetail, Long> {
  @Query(
    """
    select aud
    from AdditionalConditionUploadDetail aud
    where aud.fullSizeImageDsUuid is null
    and aud.originalDataDsUuid is null
    order by aud.id desc
    limit :limit
  """,
  )
  fun toBeMigrated(limit: Int = 100): List<AdditionalConditionUploadDetail>
}
