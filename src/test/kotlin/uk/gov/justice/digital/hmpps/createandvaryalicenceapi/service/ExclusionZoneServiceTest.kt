package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.reset
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.AssertionsForClassTypes
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionUploadDetail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionUploadSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionUploadDetailRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional

class ExclusionZoneServiceTest {
  private val licenceRepository = mock<LicenceRepository>()
  private val additionalConditionRepository = mock<AdditionalConditionRepository>()
  private val additionalConditionUploadDetailRepository = mock<AdditionalConditionUploadDetailRepository>()

  private val service = ExclusionZoneService(
    licenceRepository,
    additionalConditionRepository,
    additionalConditionUploadDetailRepository,
  )

  @BeforeEach
  fun reset() {
    reset(
      licenceRepository,
      additionalConditionRepository,
      additionalConditionUploadDetailRepository,
    )
  }

  @Test
  fun `service uploads an exclusion zone file`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))
    whenever(additionalConditionRepository.findById(1L)).thenReturn(Optional.of(anAdditionalConditionEntityWithoutUpload))
    whenever(additionalConditionUploadDetailRepository.saveAndFlush(any())).thenReturn(anAdditionalConditionUploadDetailEntity)

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
    whenever(additionalConditionUploadDetailRepository.findById(1)).thenReturn(Optional.of(anAdditionalConditionUploadDetailEntity))

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
    whenever(additionalConditionUploadDetailRepository.findById(1L)).thenReturn(Optional.of(anAdditionalConditionUploadDetailEntity))

    val image = service.getExclusionZoneImage(1L, 1L)

    assertThat(image).isEqualTo(ClassPathResource("test_map.jpg").inputStream.readAllBytes())

    verify(licenceRepository, times(1)).findById(1L)
    verify(additionalConditionRepository, times(1)).findById(1L)
    verify(additionalConditionUploadDetailRepository, times(1)).findById(1L)
  }

  private companion object {

    val someEntityStandardConditions = listOf(
      uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.StandardCondition(
        id = 1,
        conditionCode = "goodBehaviour",
        conditionSequence = 1,
        conditionText = "Be of good behaviour"
      ),
    )

    val aLicenceEntity = Licence(
      id = 1,
      typeCode = LicenceType.AP,
      version = "1.1",
      statusCode = LicenceStatus.IN_PROGRESS,
      nomsId = "A1234AA",
      bookingNo = "123456",
      bookingId = 54321,
      crn = "X12345",
      pnc = "2019/123445",
      cro = "12345",
      prisonCode = "MDI",
      prisonDescription = "Moorland (HMP)",
      forename = "Bob",
      surname = "Mortimer",
      dateOfBirth = LocalDate.of(1985, 12, 28),
      conditionalReleaseDate = LocalDate.of(2021, 10, 22),
      actualReleaseDate = LocalDate.of(2021, 10, 22),
      sentenceStartDate = LocalDate.of(2018, 10, 22),
      sentenceEndDate = LocalDate.of(2021, 10, 22),
      licenceStartDate = LocalDate.of(2021, 10, 22),
      licenceExpiryDate = LocalDate.of(2021, 10, 22),
      topupSupervisionStartDate = LocalDate.of(2021, 10, 22),
      topupSupervisionExpiryDate = LocalDate.of(2021, 10, 22),
      comFirstName = "Stephen",
      comLastName = "Mills",
      comUsername = "X12345",
      comStaffId = 12345,
      comEmail = "stephen.mills@nps.gov.uk",
      comTelephone = "0116 2788777",
      probationAreaCode = "N01",
      probationLduCode = "LDU1",
      dateCreated = LocalDateTime.now(),
      createdByUsername = "X12345",
      standardConditions = someEntityStandardConditions,
    )

    val someAdditionalConditionData = listOf(
      AdditionalConditionData(id = 1, dataField = "outOfBoundArea", dataValue = "Bristol town centre", additionalCondition = AdditionalCondition(licence = aLicenceEntity)),
      AdditionalConditionData(id = 2, dataField = "outOfBoundFile", dataValue = "test.pdf", additionalCondition = AdditionalCondition(licence = aLicenceEntity))
    )

    val anAdditionalConditionEntityWithoutUpload = AdditionalCondition(
      id = 1,
      licence = aLicenceEntity,
      conditionCode = "outOfBounds",
      conditionCategory = "Freedom of movement",
      conditionSequence = 1,
      conditionText = "text",
      additionalConditionData = someAdditionalConditionData,
      additionalConditionUploadSummary = emptyList(),
    )

    val someUploadSummaryData = AdditionalConditionUploadSummary(
      id = 1,
      filename = "test.pdf",
      fileType = "application/pdf",
      description = "Description",
      thumbnailImage = ByteArray(0),
      additionalCondition = anAdditionalConditionEntityWithoutUpload,  // Any additional condition is Ok here.
      uploadDetailId = 1,
    )

    val anAdditionalConditionEntityWithUpload = AdditionalCondition(
      id = 1,
      licence = aLicenceEntity,
      conditionCode = "outOfBounds",
      conditionCategory = "Freedom of movement",
      conditionSequence = 1,
      conditionText = "text",
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
