package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionUploadDetail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionUploadSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionUploadDetailRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionUploadSummaryRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.documents.DocumentService

@Service
class ExclusionZoneUploadsMigration(
  private val documentService: DocumentService,
  private val additionalConditionUploadDetailRepository: AdditionalConditionUploadDetailRepository,
  private val additionalConditionUploadSummaryRepository: AdditionalConditionUploadSummaryRepository,
) {

  fun perform(batchSize: Int = 100) {
    migrateUploadDetails(batchSize)
    migrateUploadSummaries(batchSize)
  }

  private fun migrateUploadDetails(batchSize: Int) {
    val all = additionalConditionUploadDetailRepository.totalToBeMigrated()
    val batch = additionalConditionUploadDetailRepository.toBeMigrated(batchSize)

    log.info("Migrating AdditionalConditionUploadDetail records (batchSize={}, totalToBeMigrated={})", batchSize, all)

    batch.forEach { uploadDetail ->
      val originalDataUuid = documentService.uploadDocument(uploadDetail.originalData!!, metadata(uploadDetail, "pdf"))
      val fullSizeImageUuid = documentService.uploadDocument(uploadDetail.fullSizeImage!!, metadata(uploadDetail, "fullSizeImage"))

      if (null in listOf(originalDataUuid, fullSizeImageUuid)) {
        log.info("Unable to migrate AdditionalConditionUploadDetail id={} (originalDataUuid={}, fullSizeImageUuid={})", uploadDetail.id, originalDataUuid, fullSizeImageUuid)
      } else {
        additionalConditionUploadDetailRepository.saveAndFlush(
          uploadDetail.copy(
            originalDataDsUuid = originalDataUuid?.toString(),
            fullSizeImageDsUuid = fullSizeImageUuid?.toString(),
          ),
        )
      }
    }
  }

  private fun migrateUploadSummaries(batchSize: Int) {
    val all = additionalConditionUploadSummaryRepository.totalToBeMigrated()
    val batch = additionalConditionUploadSummaryRepository.toBeMigrated(batchSize)

    log.info("Migrating AdditionalConditionUploadSummary records (batchSize={}, totalToBeMigrated={})", batchSize, all)

    batch.forEach { uploadSummary ->
      val thumbnailUuid = documentService.uploadDocument(uploadSummary.thumbnailImage!!, metadata(uploadSummary, "thumbnail"))

      if (thumbnailUuid == null) {
        log.info("Unable to migrate AdditionalConditionUploadSummary id={} (thumbnailUuid=null)", uploadSummary.id)
      } else {
        additionalConditionUploadSummaryRepository.saveAndFlush(
          uploadSummary.copy(thumbnailImageDsUuid = thumbnailUuid.toString()),
        )
      }
    }
  }

  private fun metadata(uploadDetail: AdditionalConditionUploadDetail, kind: String) = mapOf(
    "licenceId" to uploadDetail.licenceId.toString(),
    "additionalConditionId" to uploadDetail.additionalConditionId.toString(),
    "kind" to kind,
  )

  private fun metadata(uploadSummary: AdditionalConditionUploadSummary, kind: String) = mapOf(
    "licenceId" to uploadSummary.additionalCondition.licence.id.toString(),
    "additionalConditionId" to uploadSummary.additionalCondition.id.toString(),
    "kind" to kind,
  )

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
