package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionUploadDetail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionUploadSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionUploadDetailRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.DocumentCountsRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.documents.DocumentService
import java.util.Optional
import java.util.UUID

class ExclusionZoneServiceGetFullImageTest {

  private val licenceRepository: LicenceRepository = mock()
  private val additionalConditionRepository: AdditionalConditionRepository = mock()
  private val additionalConditionUploadDetailRepository: AdditionalConditionUploadDetailRepository = mock()
  private val documentService: DocumentService = mock()
  private val documentCountsRepository: DocumentCountsRepository = mock()

  private val fileStoredInDatabase = byteArrayOf(9, 9, 9)
  private val fileStoredRemotely = byteArrayOf(1, 1, 1)

  private val licenceId = 1L
  private val additionalConditionId = 2L
  private val additionalConditionUploadDetailId = 4L

  private val uuid = UUID.fromString("fcc03409-b1a0-4c60-8662-ad9e441bc54d")

  @Test
  fun `when there is no document service uuid recorded then the image is returned from the document service`() {
    givenUploadDetailHas(fullSizeImage = fileStoredInDatabase, fullSizeImageUUID = uuid)

    val exclusionZoneService = ExclusionZoneService(
      licenceRepository,
      additionalConditionRepository,
      additionalConditionUploadDetailRepository,
      documentService,
      documentCountsRepository,
    )

    assertThat(exclusionZoneService.getExclusionZoneImage(licenceId, additionalConditionId))
      .isEqualTo(fileStoredRemotely)
  }

  @Test
  fun `when there is no document service uuid recorded then the image is returned from the database`() {
    givenUploadDetailHas(fullSizeImage = fileStoredInDatabase, fullSizeImageUUID = null)

    val exclusionZoneService = ExclusionZoneService(
      licenceRepository,
      additionalConditionRepository,
      additionalConditionUploadDetailRepository,
      documentService,
      documentCountsRepository,
    )

    assertThat(exclusionZoneService.getExclusionZoneImage(licenceId, additionalConditionId))
      .isEqualTo(fileStoredInDatabase)
  }

  private fun givenUploadDetailHas(fullSizeImage: ByteArray?, fullSizeImageUUID: UUID?) {
    whenever(licenceRepository.findById(licenceId)).thenReturn(Optional.of(mock()))

    whenever(additionalConditionRepository.findById(additionalConditionId)).thenReturn(
      Optional.of(additionalCondition()),
    )

    whenever(additionalConditionUploadDetailRepository.findById(additionalConditionUploadDetailId)).thenReturn(
      Optional.of(additionalConditionUploadDetail(fullSizeImage, fullSizeImageUUID)),
    )

    whenever(documentService.downloadDocument(uuid)).thenReturn(fileStoredRemotely)
  }

  private fun additionalCondition(): AdditionalCondition = AdditionalCondition(
    id = additionalConditionId,
    licence = mock(),
    additionalConditionData = mock(),
    additionalConditionUploadSummary = mutableListOf(additionalConditionUploadSummary()),
    conditionVersion = "",
    conditionCode = "",
    conditionCategory = "",
    conditionText = "",
    expandedConditionText = "",
    conditionType = "",
  )

  private fun additionalConditionUploadSummary(): AdditionalConditionUploadSummary = AdditionalConditionUploadSummary(
    id = 3L,
    additionalCondition = mock(),
    uploadDetailId = additionalConditionUploadDetailId,
  )

  private fun additionalConditionUploadDetail(fullSizeImage: ByteArray?, fullSizeImageUUID: UUID?): AdditionalConditionUploadDetail = AdditionalConditionUploadDetail(
    id = 4L,
    licenceId = 1L,
    additionalConditionId = 2L,
    fullSizeImage = fullSizeImage,
    fullSizeImageDsUuid = fullSizeImageUUID?.toString(),
  )
}
