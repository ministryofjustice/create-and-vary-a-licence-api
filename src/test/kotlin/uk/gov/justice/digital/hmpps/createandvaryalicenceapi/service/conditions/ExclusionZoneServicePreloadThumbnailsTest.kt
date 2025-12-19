package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionUpload
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.documents.DocumentService
import java.util.Base64
import java.util.UUID
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalCondition as ModelAdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalConditionUploadSummary as ModelAdditionalConditionUpload

class ExclusionZoneServicePreloadThumbnailsTest {

  private val licence: Licence = mock()
  private val modelLicence: CrdLicence = mock()
  private val documentService: DocumentService = mock()
  private val exclusionZoneService = ExclusionZoneService(
    licenceRepository = mock(),
    additionalConditionRepository = mock(),
    additionalConditionUploadRepository = mock(),
    documentService = documentService,
  )

  @Test
  fun `given uploads with thumbnails when preloadThumbnails is called then thumbnails are attached to model licence conditions`() {
    // Given
    val upload1 = createEntityUpload(1, "11111111-1111-1111-1111-111111111111")
    val upload2 = createEntityUpload(2, "22222222-2222-2222-2222-222222222222")

    whenever(licence.additionalConditions).thenReturn(
      mutableListOf(createEntityCondition(10, mutableListOf(upload1)), createEntityCondition(20, mutableListOf(upload2))),
    )
    whenever(modelLicence.additionalLicenceConditions).thenReturn(
      mutableListOf(createModelCondition(upload1.id!!), createModelCondition(upload2.id!!)),
    )
    whenever(documentService.downloadDocument(UUID.fromString(upload1.thumbnailImageDsUuid!!)))
      .thenReturn(byteArrayOf(1, 2, 3))
    whenever(documentService.downloadDocument(UUID.fromString(upload2.thumbnailImageDsUuid!!)))
      .thenReturn(byteArrayOf(4, 5, 6))

    // When
    exclusionZoneService.preloadThumbnails(licence, modelLicence)

    // Then
    val summary1 = modelLicence.additionalLicenceConditions[0].uploadSummary[0]
    val summary2 = modelLicence.additionalLicenceConditions[1].uploadSummary[0]
    assertThat(summary1.thumbnailImage).isEqualTo(Base64.getEncoder().encodeToString(byteArrayOf(1, 2, 3)))
    assertThat(summary2.thumbnailImage).isEqualTo(Base64.getEncoder().encodeToString(byteArrayOf(4, 5, 6)))
  }

  @Test
  fun `given a thumbnail download fails when preloadThumbnails is called then model licence summary remains unchanged`() {
    // Given
    val upload = createEntityUpload(3, "33333333-3333-3333-3333-333333333333")
    whenever(licence.additionalConditions).thenReturn(mutableListOf(createEntityCondition(30, mutableListOf(upload))))
    whenever(modelLicence.additionalLicenceConditions).thenReturn(mutableListOf(createModelCondition(upload.id!!)))
    whenever(documentService.downloadDocument(any())).thenThrow(RuntimeException("download failed"))

    // When
    exclusionZoneService.preloadThumbnails(licence, modelLicence)

    // Then
    val summary = modelLicence.additionalLicenceConditions[0].uploadSummary[0]
    assertThat(summary.thumbnailImage).isNull()
  }

  @Test
  fun `given uploads with no thumbnail UUID when preloadThumbnails is called then uploads are ignored`() {
    // Given
    val upload = createEntityUpload(4)
    whenever(licence.additionalConditions).thenReturn(mutableListOf(createEntityCondition(40, mutableListOf(upload))))
    whenever(modelLicence.additionalLicenceConditions).thenReturn(mutableListOf(createModelCondition(upload.id!!)))

    // When
    exclusionZoneService.preloadThumbnails(licence, modelLicence)

    // Then
    val summary = modelLicence.additionalLicenceConditions[0].uploadSummary[0]
    assertThat(summary.thumbnailImage).isNull()
  }

  @Test
  fun `given uploads with no matching summary when preloadThumbnails is called then nothing is attached`() {
    // Given
    val upload = createEntityUpload(5, "55555555-5555-5555-5555-555555555555")
    whenever(licence.additionalConditions).thenReturn(mutableListOf(createEntityCondition(50, listOf(upload))))
    whenever(modelLicence.additionalLicenceConditions).thenReturn(mutableListOf())
    whenever(documentService.downloadDocument(UUID.fromString(upload.thumbnailImageDsUuid!!)))
      .thenReturn(byteArrayOf(9, 9, 9))

    // When
    exclusionZoneService.preloadThumbnails(licence, modelLicence)

    // Then
    assertThat(modelLicence.additionalLicenceConditions).isEmpty()
  }

  @Test
  fun `given a licence with no additional conditions when preloadThumbnails is called then model licence remains unchanged`() {
    // Given
    whenever(licence.additionalConditions).thenReturn(mutableListOf())
    whenever(modelLicence.additionalLicenceConditions).thenReturn(mutableListOf())

    // When
    exclusionZoneService.preloadThumbnails(licence, modelLicence)

    // Then
    assertThat(modelLicence.additionalLicenceConditions).isEmpty()
  }

  private fun createEntityUpload(id: Long, thumbnailUuid: String? = null) = AdditionalConditionUpload(
    id = id,
    additionalCondition = mock(),
    thumbnailImageDsUuid = thumbnailUuid,
  )

  private fun createEntityCondition(id: Long, uploads: List<AdditionalConditionUpload>) = AdditionalCondition(
    id = id,
    licence = licence,
    conditionVersion = "",
    conditionCode = "",
    conditionCategory = "",
    conditionText = "",
    conditionType = "",
    additionalConditionData = mutableListOf(),
    additionalConditionUpload = uploads.toMutableList(),
  )

  private fun createModelCondition(uploadId: Long) = ModelAdditionalCondition(
    id = uploadId + 1000L,
    uploadSummary = mutableListOf(ModelAdditionalConditionUpload(id = uploadId)),
    readyToSubmit = true,
    requiresInput = false,
  )
}
