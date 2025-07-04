package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.AssertionsForClassTypes
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionUploadDetail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionUploadSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionUploadDetailRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.anAdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.documents.DocumentService
import java.util.Optional

class ExclusionZoneServiceTest {
  private val licenceRepository = mock<LicenceRepository>()
  private val additionalConditionRepository = mock<AdditionalConditionRepository>()
  private val additionalConditionUploadDetailRepository = mock<AdditionalConditionUploadDetailRepository>()
  private val documentService = mock<DocumentService>()

  private val service = ExclusionZoneService(
    licenceRepository,
    additionalConditionRepository,
    additionalConditionUploadDetailRepository,
    documentService,
  )

  @BeforeEach
  fun reset() {
    reset(
      licenceRepository,
      additionalConditionRepository,
      additionalConditionUploadDetailRepository,
      documentService,
    )
  }

  @Test
  fun `service uploads an exclusion zone file`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))
    whenever(additionalConditionRepository.findById(1L)).thenReturn(Optional.of(anAdditionalConditionEntityWithoutUpload))
    whenever(additionalConditionUploadDetailRepository.saveAndFlush(any())).thenReturn(
      anAdditionalConditionUploadDetailEntity,
    )

    val fileResource = ClassPathResource("Test_map_2021-12-06_112550.pdf")
    AssertionsForClassTypes.assertThat(fileResource).isNotNull

    val multiPartFile = MockMultipartFile(
      "file",
      fileResource.filename,
      MediaType.APPLICATION_PDF_VALUE,
      fileResource.file.inputStream(),
    )

    service.uploadExclusionZoneFile(1L, 1L, multiPartFile)

    verify(licenceRepository, times(1)).findById(1L)
    verify(additionalConditionRepository, times(1)).findById(1L)
    verify(additionalConditionUploadDetailRepository, times(1)).saveAndFlush(any())
    verify(additionalConditionRepository, times(1)).saveAndFlush(any())
  }

  @Test
  fun `service removes an upload exclusion zone`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))
    whenever(additionalConditionRepository.findById(1L)).thenReturn(Optional.of(anAdditionalConditionEntityWithUpload))
    whenever(additionalConditionUploadDetailRepository.findById(1)).thenReturn(
      Optional.of(
        anAdditionalConditionUploadDetailEntity,
      ),
    )

    service.removeExclusionZoneFile(1L, 1L)

    verify(licenceRepository, times(1)).findById(1L)
    verify(additionalConditionRepository, times(1)).findById(1L)
    verify(additionalConditionUploadDetailRepository, times(1)).delete(any())
    verify(additionalConditionRepository, times(1)).saveAndFlush(any())
  }

  @Test
  fun `service returns a full-sized exclusion zone image`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))
    whenever(additionalConditionRepository.findById(1L)).thenReturn(Optional.of(anAdditionalConditionEntityWithUpload))
    whenever(additionalConditionUploadDetailRepository.findById(1L)).thenReturn(
      Optional.of(
        anAdditionalConditionUploadDetailEntity,
      ),
    )

    val image = service.getExclusionZoneImage(1L, 1L)

    assertThat(image).isEqualTo(ClassPathResource("test_map.jpg").inputStream.readAllBytes())

    verify(licenceRepository, times(1)).findById(1L)
    verify(additionalConditionRepository, times(1)).findById(1L)
    verify(additionalConditionUploadDetailRepository, times(1)).findById(1L)
  }

  private companion object {
    val aLicenceEntity = TestData.createCrdLicence()

    val someAdditionalConditionData = listOf(
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
      additionalConditionUploadSummary = emptyList(),
    )

    val someUploadSummaryData = AdditionalConditionUploadSummary(
      id = 1,
      filename = "test.pdf",
      fileType = "application/pdf",
      description = "Description",
      thumbnailImage = ByteArray(0),
      // Any additional condition is Ok here
      additionalCondition = anAdditionalConditionEntityWithoutUpload,
      uploadDetailId = 1,
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
      additionalConditionUploadSummary = listOf(someUploadSummaryData),
    )

    val anAdditionalConditionUploadDetailEntity = AdditionalConditionUploadDetail(
      id = 1,
      licenceId = 1,
      additionalConditionId = 1,
      fullSizeImage = ClassPathResource("test_map.jpg").inputStream.readAllBytes(),
      originalData = ClassPathResource("Test_map_2021-12-06_112550.pdf").inputStream.readAllBytes(),
    )
  }
}
