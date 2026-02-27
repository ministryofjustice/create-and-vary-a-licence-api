package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.AssertionsForClassTypes
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.spy
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionUpload
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionUploadRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.anAdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions.upload.UploadFileConditionsService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.documents.DocumentService
import java.util.*

class UploadFileConditionsServiceTest {
  private val licenceRepository = mock<LicenceRepository>()
  private val additionalConditionRepository = mock<AdditionalConditionRepository>()
  private val additionalConditionUploadRepository = mock<AdditionalConditionUploadRepository>()
  private val documentService = mock<DocumentService>()

  private val service = UploadFileConditionsService(
    licenceRepository,
    additionalConditionRepository,
    additionalConditionUploadRepository,
    documentService,
  )

  @BeforeEach
  fun reset() {
    reset(
      licenceRepository,
      additionalConditionRepository,
      additionalConditionUploadRepository,
      documentService,
    )
  }

  @Test
  fun `service uploads an exclusion zone file`() {
    // Given
    val additionalCondition = anAdditionalConditionEntityWithoutUpload.copy(
      additionalConditionUpload = spy(mutableListOf()),
    )

    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))
    whenever(additionalConditionRepository.findById(1L)).thenReturn(Optional.of(additionalCondition))

    val fileResource = ClassPathResource("Test_map_2021-12-06_112550.pdf")
    AssertionsForClassTypes.assertThat(fileResource).isNotNull

    val multiPartFile = MockMultipartFile(
      "file",
      fileResource.filename,
      MediaType.APPLICATION_PDF_VALUE,
      fileResource.file.inputStream(),
    )

    // When
    service.uploadFile(1L, 1L, multiPartFile)

    // Then
    verify(licenceRepository, times(1)).findById(1L)
    verify(additionalConditionRepository, times(1)).findById(1L)
    verify(additionalCondition.additionalConditionUpload, times(1)).clear()
    verify(additionalCondition.additionalConditionUpload, times(1)).add(any())
  }

  @Test
  fun `service returns a full-sized exclusion zone image`() {
    val documentServiceFile = ClassPathResource("test_map.jpg").inputStream.readAllBytes()

    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity.copy(additionalConditions = listOf(anAdditionalConditionEntityWithUpload))))
    whenever(additionalConditionRepository.findById(1L)).thenReturn(Optional.of(anAdditionalConditionEntityWithUpload))
    whenever(documentService.downloadDocument(UUID.fromString(anAdditionalConditionEntityWithUpload.additionalConditionUpload.first().fullSizeImageDsUuid)))
      .thenReturn(documentServiceFile)

    val image = service.getImage(1L, 1L)

    assertThat(image).isEqualTo(documentServiceFile)

    verify(licenceRepository, times(1)).findById(1L)
    verify(additionalConditionRepository, times(1)).findById(1L)
  }

  private companion object {
    val aLicenceEntity = TestData.createCrdLicence()

    val someAdditionalConditionData = mutableListOf(
      AdditionalConditionData(
        id = 1,
        dataField = "outOfBoundArea",
        dataValue = "Bristol town centre",
        additionalCondition = anAdditionalCondition(id = 1, licence = aLicenceEntity),
      ),
      AdditionalConditionData(
        id = 2,
        dataField = "outOfBoundFile",
        dataValue = "test.pdf",
        additionalCondition = anAdditionalCondition(id = 2, licence = aLicenceEntity),
      ),
    )

    val anAdditionalConditionEntityWithoutUpload = AdditionalCondition(
      id = 1,
      licence = aLicenceEntity,
      conditionVersion = "1.0",
      conditionCode = "outOfBounds",
      conditionCategory = "Freedom of movement",
      conditionSequence = 1,
      conditionText = "text",
      conditionType = "AP",
      additionalConditionData = someAdditionalConditionData,
      additionalConditionUpload = mutableListOf(),
    )

    val someUploadSummaryData = AdditionalConditionUpload(
      id = 1,
      filename = "test.pdf",
      fileType = "application/pdf",
      description = "Description",
      // Any additional condition is Ok here
      additionalCondition = anAdditionalConditionEntityWithoutUpload,
      fullSizeImageDsUuid = "9f7f2002-cdd6-4e3a-8f3f-135271433300",
    )

    val anAdditionalConditionEntityWithUpload = AdditionalCondition(
      id = 1,
      conditionVersion = "1.0",
      licence = aLicenceEntity,
      conditionCode = "outOfBounds",
      conditionCategory = "Freedom of movement",
      conditionSequence = 1,
      conditionText = "text",
      conditionType = "AP",
      additionalConditionData = someAdditionalConditionData,
      additionalConditionUpload = mutableListOf(someUploadSummaryData),
    )
  }
}
