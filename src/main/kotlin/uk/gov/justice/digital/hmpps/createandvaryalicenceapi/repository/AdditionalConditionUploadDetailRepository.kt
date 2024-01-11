package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionUploadDetail

@Repository
interface AdditionalConditionUploadDetailRepository : JpaRepository<AdditionalConditionUploadDetail, Long> {
  @Query(
    """
        FROM AdditionalConditionUploadDetail a 
        WHERE a.fullSizeImage != null 
            AND a.fullSizeImageDSUUID = null
    """,
  )
  fun getFilesWhichAreNotCopiedToDocumentService(pageable: Pageable): List<AdditionalConditionUploadDetail>
}
