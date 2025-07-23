package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionUploadSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionUploadDetailRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.documents.DocumentService
import java.util.*

class ExclusionZoneServicePreloadLicenceThumbnailsTest {
  private val licence: Licence = mock()

  private val licenceRepository: LicenceRepository = mock()
  private val additionalConditionRepository: AdditionalConditionRepository = mock()
  private val additionalConditionUploadDetailRepository: AdditionalConditionUploadDetailRepository = mock()
  private val documentService: DocumentService = mock()

  @Test
  fun `return a map of thumbnail id to image bytes for every additional condition featuring an uploaded thumbnail`() {
    val additionalCondition1 = anAdditionalConditionWithSummary(thumbnailUuid = "9e3c18a8-8856-4edc-9366-8ea2c4b3bcf3")
    val additionalCondition2 = anAdditionalConditionWithSummary(thumbnailUuid = "17f6bbba-baef-4c5d-9c59-9e4548f48d8f")

    whenever(documentService.downloadDocument(UUID.fromString("9e3c18a8-8856-4edc-9366-8ea2c4b3bcf3"))).thenReturn(byteArrayOf(1, 2, 3))
    whenever(documentService.downloadDocument(UUID.fromString("17f6bbba-baef-4c5d-9c59-9e4548f48d8f"))).thenReturn(byteArrayOf(4, 5, 6))

    whenever(licence.additionalConditions).thenReturn(listOf(additionalCondition1, additionalCondition2).toMutableList())

    val exclusionZoneService = ExclusionZoneService(
      licenceRepository,
      additionalConditionRepository,
      additionalConditionUploadDetailRepository,
      documentService,
    )

    exclusionZoneService.preloadThumbnailsFor(licence)

    assertThat(additionalCondition1.additionalConditionUploadSummary.first().preloadedThumbnailImage).isEqualTo(byteArrayOf(1, 2, 3))
    assertThat(additionalCondition2.additionalConditionUploadSummary.first().preloadedThumbnailImage).isEqualTo(byteArrayOf(4, 5, 6))
  }

  private fun anAdditionalConditionWithSummary(thumbnailUuid: String): AdditionalCondition = AdditionalCondition(
    id = 1L,
    licence = mock(),
    conditionVersion = "",
    conditionCode = "",
    conditionCategory = "",
    conditionText = "",
    conditionType = "",
    additionalConditionData = emptyList(),
    additionalConditionUploadSummary = listOf(
      AdditionalConditionUploadSummary(
        id = 2L,
        additionalCondition = mock(),
        uploadedTime = mock(),
        thumbnailImageDsUuid = thumbnailUuid,
        uploadDetailId = 3L,
      ),
    ),
  )
}
