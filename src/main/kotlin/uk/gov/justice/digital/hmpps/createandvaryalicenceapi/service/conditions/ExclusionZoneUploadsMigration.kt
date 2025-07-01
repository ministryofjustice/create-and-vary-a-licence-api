package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions

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

  fun perform() {
    migrateUploadDetails()
    migrateUploadSummaries()
  }

  private fun migrateUploadDetails() = additionalConditionUploadDetailRepository.toBeMigrated().forEach { uploadDetail ->
    val originalData = documentService.uploadDocument(uploadDetail.originalData!!, metadata(uploadDetail, "pdf"))
    val fullSizeImage = documentService.uploadDocument(uploadDetail.fullSizeImage!!, metadata(uploadDetail, "fullSizeImage"))

    additionalConditionUploadDetailRepository.saveAndFlush(
      uploadDetail.copy(
        originalDataDsUuid = originalData?.toString(),
        fullSizeImageDsUuid = fullSizeImage?.toString(),
      ),
    )
  }

  private fun migrateUploadSummaries() = additionalConditionUploadSummaryRepository.toBeMigrated().forEach { uploadSummary ->
    val thumbnail = documentService.uploadDocument(uploadSummary.thumbnailImage!!, metadata(uploadSummary, "thumbnail"))

    additionalConditionUploadSummaryRepository.saveAndFlush(
      uploadSummary.copy(thumbnailImageDsUuid = thumbnail?.toString()),
    )
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
}
