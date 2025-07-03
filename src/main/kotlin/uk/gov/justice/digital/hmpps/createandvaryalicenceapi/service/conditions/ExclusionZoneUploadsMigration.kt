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
  private val uploadDetailRepository: AdditionalConditionUploadDetailRepository,
  private val uploadSummaryRepository: AdditionalConditionUploadSummaryRepository,
) {

  fun perform(batchSize: Int = 100) {
    log.info("Migrating AdditionalConditionUploadDetail records (batchSize={}, totalToBeMigrated={})", batchSize, uploadDetailRepository.totalToBeMigrated())
    uploadDetailRepository.toBeMigrated(batchSize).forEach(::migrate)

    log.info("Migrating AdditionalConditionUploadSummary records (batchSize={}, totalToBeMigrated={})", batchSize, uploadSummaryRepository.totalToBeMigrated())
    uploadSummaryRepository.toBeMigrated(batchSize).forEach(::migrate)
  }

  private fun migrate(uploadDetail: AdditionalConditionUploadDetail) {
    val originalDataUuid = documentService.uploadDocument(uploadDetail.originalData!!, metadata(uploadDetail, "pdf"))
    val fullSizeImageUuid = documentService.uploadDocument(uploadDetail.fullSizeImage!!, metadata(uploadDetail, "fullSizeImage"))

    if (null in listOf(originalDataUuid, fullSizeImageUuid)) {
      log.info("Unable to migrate AdditionalConditionUploadDetail id={} (originalDataUuid={}, fullSizeImageUuid={})", uploadDetail.id, originalDataUuid, fullSizeImageUuid)
    } else {
      uploadDetailRepository.saveAndFlush(
        uploadDetail.copy(
          originalDataDsUuid = originalDataUuid?.toString(),
          fullSizeImageDsUuid = fullSizeImageUuid?.toString(),
        ),
      )
    }
  }

  private fun migrate(uploadSummary: AdditionalConditionUploadSummary) {
    val thumbnailUuid = documentService.uploadDocument(uploadSummary.thumbnailImage!!, metadata(uploadSummary, "thumbnail"))

    if (thumbnailUuid == null) {
      log.info("Unable to migrate AdditionalConditionUploadSummary id={} (thumbnailUuid=null)", uploadSummary.id)
    } else {
      uploadSummaryRepository.saveAndFlush(
        uploadSummary.copy(thumbnailImageDsUuid = thumbnailUuid.toString()),
      )
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
