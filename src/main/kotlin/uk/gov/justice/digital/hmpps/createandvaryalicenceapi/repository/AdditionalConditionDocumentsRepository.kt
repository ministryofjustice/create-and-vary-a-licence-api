package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionDocuments

@Repository
interface AdditionalConditionDocumentsRepository : JpaRepository<AdditionalConditionDocuments, Long> {
  @Query(
    """
        FROM AdditionalConditionDocuments 
        WHERE (thumbnailImageDsUuid IS NULL AND thumbnailImage IS NOT NULL)
              OR 
              (fullSizeImageDsUuid IS NULL AND fullSizeImage IS NOT NULL)
    """,
  )
  fun getFilesWhichAreNotCopiedToDocumentService(pageable: Pageable): List<AdditionalConditionDocuments>
}
