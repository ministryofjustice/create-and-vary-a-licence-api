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
    with(uploadDetailRepository) {
      log.info("Migrating {} of {} total AdditionalConditionUploadDetail record(s)", batchSize, totalToBeMigrated())
      toBeMigrated(batchSize).forEach(::migrate)
    }

    with(uploadSummaryRepository) {
      log.info("Migrating {} of {} total AdditionalConditionUploadSummary record(s)", batchSize, totalToBeMigrated())
      toBeMigrated(batchSize).forEach(::migrate)
    }

    log.info("Batch completed")
  }

  private fun migrate(uploadDetail: AdditionalConditionUploadDetail) {
    runCatching {
      val originalDataUuid = documentService.uploadDocument(uploadDetail.originalData!!, metadata(uploadDetail, "pdf"))
      val fullSizeImageUuid = documentService.uploadDocument(uploadDetail.fullSizeImage!!, metadata(uploadDetail, "fullSizeImage"))
      uploadDetailRepository.saveAndFlush(
        uploadDetail.copy(
          originalDataDsUuid = originalDataUuid.toString(),
          fullSizeImageDsUuid = fullSizeImageUuid.toString(),
        ),
      )
    }.onFailure { log.error("Unable to migrate AdditionalConditionUploadDetail id={}", uploadDetail.id, it) }
  }

  private fun migrate(uploadSummary: AdditionalConditionUploadSummary) {
    runCatching {
      val thumbnailUuid = documentService.uploadDocument(uploadSummary.thumbnailImage!!, metadata(uploadSummary, "thumbnail"))
      uploadSummaryRepository.saveAndFlush(uploadSummary.copy(thumbnailImageDsUuid = thumbnailUuid.toString()))
    }.onFailure { log.error("Unable to migrate AdditionalConditionUploadSummary id={}", uploadSummary.id, it) }
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
