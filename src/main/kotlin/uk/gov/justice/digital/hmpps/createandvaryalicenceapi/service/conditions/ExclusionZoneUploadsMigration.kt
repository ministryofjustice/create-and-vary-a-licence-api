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

  fun perform(limit: Int = 100) {
    additionalConditionUploadDetailRepository.toBeMigrated(limit).forEach(::migrate)
    additionalConditionUploadSummaryRepository.toBeMigrated(limit).forEach(::migrate)
  }

  private fun migrate(uploadDetail: AdditionalConditionUploadDetail) {
    val originalDataUuid = documentService.uploadDocument(uploadDetail.originalData!!, metadata(uploadDetail, "pdf"))
    val fullSizeImageUuid = documentService.uploadDocument(uploadDetail.fullSizeImage!!, metadata(uploadDetail, "fullSizeImage"))

    additionalConditionUploadDetailRepository.saveAndFlush(
      uploadDetail.copy(
        originalDataDsUuid = originalDataUuid?.toString(),
        fullSizeImageDsUuid = fullSizeImageUuid?.toString(),
      ),
    )
  }

  private fun migrate(uploadSummary: AdditionalConditionUploadSummary) {
    val thumbnailUuid = documentService.uploadDocument(uploadSummary.thumbnailImage!!, metadata(uploadSummary, "thumbnail"))

    additionalConditionUploadSummaryRepository.saveAndFlush(
      uploadSummary.copy(thumbnailImageDsUuid = thumbnailUuid?.toString()),
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
