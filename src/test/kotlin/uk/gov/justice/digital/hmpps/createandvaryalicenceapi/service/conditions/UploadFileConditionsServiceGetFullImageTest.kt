package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionUpload
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionUploadRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createCrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions.upload.UploadFileConditionsService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.documents.DocumentService
import java.util.Optional
import java.util.UUID

class UploadFileConditionsServiceGetFullImageTest {

  private val licenceRepository: LicenceRepository = mock()
  private val additionalConditionRepository: AdditionalConditionRepository = mock()
  private val additionalConditionUploadRepository: AdditionalConditionUploadRepository = mock()
  private val documentService: DocumentService = mock()

  private val fileStoredRemotely = byteArrayOf(1, 1, 1)

  private val licenceId = 1L
  private val additionalConditionId = 2L
  private val additionalConditionUploadDetailId = 4L

  private val uuid = UUID.fromString("fcc03409-b1a0-4c60-8662-ad9e441bc54d")

  @Test
  fun `when there is no document service uuid recorded then we cannot return an image`() {
    givenUploadDetailHas(fullSizeImageUUID = null)

    val uploadFileConditionsService = UploadFileConditionsService(
      licenceRepository,
      additionalConditionRepository,
      additionalConditionUploadRepository,
      documentService,
    )

    assertThat(uploadFileConditionsService.getImage(licenceId, additionalConditionId))
      .isNull()
  }

  @Test
  fun `when the condition ID does not exist on the licence then throw an EntityNotFoundException`() {
    givenUploadDetailHas(fullSizeImageUUID = null)

    whenever(licenceRepository.findById(licenceId)).thenReturn(
      Optional.of(
        createCrdLicence().copy(
          additionalConditions = emptyList(),
        ),
      ),
    )

    val uploadFileConditionsService = UploadFileConditionsService(
      licenceRepository,
      additionalConditionRepository,
      additionalConditionUploadRepository,
      documentService,
    )

    val error = assertThrows<EntityNotFoundException> { uploadFileConditionsService.getImage(licenceId, additionalConditionId) }

    assertThat(error.message).isEqualTo("Unable to find condition 2 on licence 1")
  }

  @Test
  fun `when there is a document service uuid recorded then we attempt to return it from the remote service`() {
    givenUploadDetailHas(fullSizeImageUUID = uuid)

    val uploadFileConditionsService = UploadFileConditionsService(
      licenceRepository,
      additionalConditionRepository,
      additionalConditionUploadRepository,
      documentService,
    )

    assertThat(uploadFileConditionsService.getImage(licenceId, additionalConditionId))
      .isEqualTo(fileStoredRemotely)
  }

  private fun givenUploadDetailHas(fullSizeImageUUID: UUID?) {
    val additionalCondition = additionalCondition(fullSizeImageUUID)

    whenever(licenceRepository.findById(licenceId)).thenReturn(
      Optional.of(
        createCrdLicence().copy(
          additionalConditions = listOf(additionalCondition),
        ),
      ),
    )

    whenever(additionalConditionRepository.findById(additionalConditionId)).thenReturn(
      Optional.of(additionalCondition),
    )

    whenever(additionalConditionUploadRepository.findById(additionalConditionUploadDetailId)).thenReturn(
      Optional.of(additionalConditionUpload(fullSizeImageUUID)),
    )

    whenever(documentService.downloadDocument(uuid)).thenReturn(fileStoredRemotely)
  }

  private fun additionalCondition(fullSizeImageUUID: UUID?): AdditionalCondition = AdditionalCondition(
    id = additionalConditionId,
    licence = mock(),
    additionalConditionData = mock(),
    additionalConditionUpload = mutableListOf(additionalConditionUpload(fullSizeImageUUID)),
    conditionVersion = "",
    conditionCode = "",
    conditionCategory = "",
    conditionText = "",
    expandedConditionText = "",
    conditionType = "",
  )

  private fun additionalConditionUpload(fullSizeImageUUID: UUID?): AdditionalConditionUpload = AdditionalConditionUpload(
    id = 3L,
    additionalCondition = mock(),
    fullSizeImageDsUuid = fullSizeImageUUID?.toString(),
  )
}
