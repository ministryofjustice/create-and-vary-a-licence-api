package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.document

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionUploadDetail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.document.DocumentMetaData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.document.LicenceDocumentType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.ExclusionZoneService

@Service
class MigrateDocumentsToDS(
  val documentService: DocumentService,
  val exclusionZoneService: ExclusionZoneService,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  fun migrateDocuments(count: Int) {
    log.info("Start - Copying documents to document service. Max count is: $count")
    val exclusionZoneMaps = exclusionZoneService.getExclusionZoneMaps(count)
    log.info("Found " + exclusionZoneMaps.size + " maps to copy to document service")
    exclusionZoneMaps.forEach {
      migrateExclusionZoneMaps(it)
    }
  }

  private fun migrateExclusionZoneMaps(additionalCond: AdditionalConditionUploadDetail) {
    log.info("Posting exclusion zone map for additionalCond.id: $additionalCond.id")
    documentService.postExclusionZoneMaps(
      additionalCond.fullSizeImage,
      metadata =
      DocumentMetaData(
        licenceId = additionalCond.licenceId.toString(),
        additionalConditionId = additionalCond.additionalConditionId.toString(),
        documentType = LicenceDocumentType.EXCLUSION_ZONE_MAP_FULL_IMG.toString(),
      ),
      documentType = LicenceDocumentType.EXCLUSION_ZONE_MAP.toString(),
    )
  }
}
