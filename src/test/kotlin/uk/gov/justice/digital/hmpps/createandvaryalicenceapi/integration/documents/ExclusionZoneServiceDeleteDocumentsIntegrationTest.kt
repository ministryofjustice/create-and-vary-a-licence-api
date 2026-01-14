package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.documents

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.atMostOnce
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.never
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionUploadRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions.ExclusionZoneService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.documents.DocumentService
import java.util.UUID

class ExclusionZoneServiceDeleteDocumentsIntegrationTest : IntegrationTestBase() {

  lateinit var exclusionZoneService: ExclusionZoneService

  @Autowired
  lateinit var licenceRepository: LicenceRepository

  @Autowired
  lateinit var additionalConditionRepository: AdditionalConditionRepository

  @Autowired
  lateinit var additionalConditionUploadRepository: AdditionalConditionUploadRepository

  val documentService: DocumentService = mock()

  @BeforeEach
  fun setup() {
    exclusionZoneService = ExclusionZoneService(
      licenceRepository,
      additionalConditionRepository,
      additionalConditionUploadRepository,
      documentService,
    )
  }

  @Sql(
    "classpath:test_data/seed-a-few-licences.sql",
    "classpath:test_data/seed-uploads-for-copied-licences.sql",
  )
  @Test
  fun `deletes all documents attached to the given licence that are not attached to another copy of the licence`() {
    val additionalConditions = testRepository.getAdditionalCondition(2)

    exclusionZoneService.deleteDocumentsForConditions(additionalConditions)

    mapOf(
      "37eb7e31-a133-4259-96bc-93369b917eb8" to never(), // 2 references to this doc
      "1595ef41-36e0-4fa8-a98b-bce5c5c98220" to never(), // 2 references to this doc
      "53655fe1-1368-4ed3-bfb0-2727a4e73be5" to atMostOnce(), // There are two of these uuids but they are in the same row
      "92939445-4159-4214-aa75-d07568a3e136" to atMostOnce(),
      "0bbf1459-ee7a-4114-b509-eb9a3fcc2756" to atMostOnce(),
      "20ca047a-0ae6-4c09-8b97-e3f211feb733" to atMostOnce(),
    ).forEach { (uuid, invoked) ->
      verify(documentService, invoked).deleteDocument(UUID.fromString(uuid))
    }
  }

  @Sql(
    "classpath:test_data/seed-a-few-licences.sql",
    "classpath:test_data/seed-uploads-for-copied-licences.sql",
  )
  @Test
  fun `getDeletableDocumentUuids returns only documents that have a single upload reference in different rows`() {
    // Given
    val additionalConditions = testRepository.getAdditionalConditions(listOf(1, 2, 3))

    // When
    val result = exclusionZoneService.getDeletableDocumentUuids(additionalConditions)

    // Then
    assertThat(result).containsExactlyInAnyOrder(
      UUID.fromString("53655fe1-1368-4ed3-bfb0-2727a4e73be5"),
      UUID.fromString("92939445-4159-4214-aa75-d07568a3e136"),
      UUID.fromString("0bbf1459-ee7a-4114-b509-eb9a3fcc2756"),
      UUID.fromString("20ca047a-0ae6-4c09-8b97-e3f211feb733"),
    )

    assertThat(result).doesNotContain(
      UUID.fromString("37eb7e31-a133-4259-96bc-93369b917eb8"),
      UUID.fromString("1595ef41-36e0-4fa8-a98b-bce5c5c98220"),
    )
  }

  @Sql(
    "classpath:test_data/seed-a-few-licences.sql",
    "classpath:test_data/seed-uploads-for-copied-licences-duplicates.sql",
  )
  @Test
  fun `getDeletableDocumentUuids returns no document uuids as they are all duplicates`() {
    // Given
    val additionalConditions = testRepository.getAdditionalConditions(listOf(1, 2, 3))

    // When
    val result = exclusionZoneService.getDeletableDocumentUuids(additionalConditions)

    // Then
    assertThat(result).isEmpty()
  }

  @Sql(
    "classpath:test_data/seed-a-few-licences.sql",
    "classpath:test_data/seed-uploads-for-copied-licences-no-duplicates.sql",
  )
  @Test
  fun `getDeletableDocumentUuids returns all document uuids as they are only duplicates on same rows`() {
    // Given
    val additionalConditions = testRepository.getAdditionalConditions(listOf(1, 2, 3))

    // When
    val result = exclusionZoneService.getDeletableDocumentUuids(additionalConditions)

    // Then
    assertThat(result).hasSize(3)
  }

  // @Test
  @Sql(
    "classpath:test_data/seed-a-few-licences.sql",
    "classpath:test_data/seed-uploads-for-copied-licences.sql",
  )
  fun `when deleteDocuments call document is deleted`() {
    // Given
    val documentUUID = UUID.fromString("53655fe1-1368-4ed3-bfb0-2727a4e73be5")
    val documentUUIDs = listOf(documentUUID)

    // When
    exclusionZoneService.deleteDocuments(documentUUIDs)

    // Then
    verify(documentService).deleteDocument(documentUUID)
  }
}
