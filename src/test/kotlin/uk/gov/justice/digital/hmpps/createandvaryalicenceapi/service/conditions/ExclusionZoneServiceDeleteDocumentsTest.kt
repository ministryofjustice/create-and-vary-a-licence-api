package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionUploadDetail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionUploadSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionUploadDetailRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.documents.DocumentService
import java.util.Optional
import java.util.UUID

class ExclusionZoneServiceDeleteDocumentsTest {
  private val licence: Licence = mock()

  private val licenceRepository: LicenceRepository = mock()
  private val additionalConditionRepository: AdditionalConditionRepository = mock()
  private val additionalConditionUploadDetailRepository: AdditionalConditionUploadDetailRepository = mock()
  private val documentService: DocumentService = mock()

  private val exclusionZoneService = ExclusionZoneService(
    licenceRepository,
    additionalConditionRepository,
    additionalConditionUploadDetailRepository,
    documentService,
  )

  @BeforeEach
  fun setup() {
    // Existing AdditionalConditionUploadDetail records
    listOf(
      uploadDetail(
        id = 1L,
        additionalConditionId = 90L,
        originalDataDsUuid = "e4e21a90-9fc3-4ae6-96a4-ba2a7dcd2e39",
        fullSizeImageDsUuid = "35a9401b-a13d-474b-aa49-3a4762f06acf",
      ),
      uploadDetail(
        id = 2L,
        additionalConditionId = 91L,
        originalDataDsUuid = "0a917459-40ec-45c1-a860-a7dd24f1297f",
        fullSizeImageDsUuid = null,
      ),
      uploadDetail(
        id = 3L,
        additionalConditionId = 92L,
        originalDataDsUuid = null,
        fullSizeImageDsUuid = "382e2c38-bc62-42bf-ab16-0816f96b0599",
      ),
    ).forEach { whenever(additionalConditionUploadDetailRepository.findById(it.id)).thenReturn(Optional.of(it)) }

    // Existing AdditionalConditionUploadSummary records
    listOf(
      additionalConditionWithSummary(id = 90L, thumbnailUuid = "21c0744f-24ea-4833-866f-4e623a373b22", uploadDetailId = 1L),
      additionalConditionWithSummary(id = 91L, thumbnailUuid = "24f9a61b-11e1-4978-8203-94a30939fcb5", uploadDetailId = 2L),
      additionalConditionWithSummary(id = 92L, thumbnailUuid = null, uploadDetailId = 3L),
    ).also { whenever(licence.additionalConditions).thenReturn(it.toMutableList()) }
  }

  @Test
  fun `deletes all documents for the given licence`() {
    exclusionZoneService.deleteDocumentsFor(licence)

    listOf(
      "e4e21a90-9fc3-4ae6-96a4-ba2a7dcd2e39",
      "35a9401b-a13d-474b-aa49-3a4762f06acf",
      "0a917459-40ec-45c1-a860-a7dd24f1297f",
      "382e2c38-bc62-42bf-ab16-0816f96b0599",
      "21c0744f-24ea-4833-866f-4e623a373b22",
      "24f9a61b-11e1-4978-8203-94a30939fcb5",
    ).forEach { verify(documentService).deleteDocument(UUID.fromString(it)) }
  }

  @Test
  fun `deletes AdditionalConditionUploadDetail records for given licence`() {
    exclusionZoneService.deleteDocumentsFor(licence)

    verify(additionalConditionUploadDetailRepository).deleteAllByIdInBatch(listOf(1L, 2L, 3L))
  }

  @Test
  fun `deletes all documents for a given set of additionalConditions`() {
    exclusionZoneService.deleteDocumentsFor(licence.additionalConditions.subList(0, 1))

    listOf(
      "21c0744f-24ea-4833-866f-4e623a373b22",
      "e4e21a90-9fc3-4ae6-96a4-ba2a7dcd2e39",
      "35a9401b-a13d-474b-aa49-3a4762f06acf",
    ).forEach { verify(documentService).deleteDocument(UUID.fromString(it)) }
  }

  @Test
  fun `deletes AdditionalConditionUploadDetail records for given additionalConditions`() {
    exclusionZoneService.deleteDocumentsFor(licence.additionalConditions.subList(0, 1))

    verify(additionalConditionUploadDetailRepository).deleteAllByIdInBatch(listOf(1L))
  }

  private fun additionalConditionWithSummary(id: Long, thumbnailUuid: String?, uploadDetailId: Long) = AdditionalCondition(
    id = id,
    licence = licence,
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
        uploadDetailId = uploadDetailId,
      ),
    ),
  )

  private fun uploadDetail(
    id: Long,
    additionalConditionId: Long,
    originalDataDsUuid: String? = null,
    fullSizeImageDsUuid: String? = null,
  ) = AdditionalConditionUploadDetail(
    id = id,
    licenceId = licence.id,
    additionalConditionId = additionalConditionId,
    originalDataDsUuid = originalDataDsUuid,
    fullSizeImageDsUuid = fullSizeImageDsUuid,
  )
}
