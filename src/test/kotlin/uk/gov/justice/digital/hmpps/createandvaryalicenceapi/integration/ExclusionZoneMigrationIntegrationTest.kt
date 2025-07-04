package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.anyMap
import org.mockito.Mockito.atMostOnce
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionUploadDetailRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionUploadSummaryRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions.ExclusionZoneUploadsMigration
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.documents.DocumentService
import java.util.UUID

class ExclusionZoneMigrationIntegrationTest : IntegrationTestBase() {

  @Autowired
  lateinit var additionalConditionUploadDetailRepository: AdditionalConditionUploadDetailRepository

  @Autowired
  lateinit var additionalConditionUploadSummaryRepository: AdditionalConditionUploadSummaryRepository

  val documentService: DocumentService = mock()

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-2.sql",
    "classpath:test_data/seed-additional-condition-exclusion-zone-uploads.sql",
  )
  fun `migrates documents stored locally to the remote document service unless we already have their uuid recorded`() {
    val migration = ExclusionZoneUploadsMigration(
      documentService,
      additionalConditionUploadDetailRepository,
      additionalConditionUploadSummaryRepository,
    )

    migration.perform()

    listOf(
      Pair(1, atMostOnce()),
      Pair(2, atMostOnce()),
      Pair(3, never()),
    ).forEach { (id, invoked) ->
      verify(documentService, invoked).uploadDocument(document("originalData", id), metadata("pdf"))
      verify(documentService, invoked).uploadDocument(document("fullSizeImage", id), metadata("fullSizeImage"))
      verify(documentService, invoked).uploadDocument(document("thumbnailImage", id), metadata("thumbnail"))
    }
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-2.sql",
    "classpath:test_data/seed-additional-condition-exclusion-zone-uploads.sql",
  )
  fun `updates the local record of a file to store its document service uuid`() {
    val uuidsFromDocumentService = mapOf(
      "originalData" to listOf("3ff02cc4-d317-45f5-8b10-6f20e0c5e5da", "a41bdf1b-8fb4-4cfc-beb4-c5c02888999c"),
      "fullSizeImage" to listOf("f0adcafd-fb9c-49a5-9fb5-0741e7da4828", "3108f563-a6f0-4677-ac25-15b828bfc9db"),
      "thumbnailImage" to listOf("5cb3ea95-512e-4b04-818e-73cbe37597a3", "87632006-6b54-4644-b160-783f42dab505"),
    )
    uuidsFromDocumentService.entries.forEach { (file, uuids) ->
      uuids.forEachIndexed { i, uuid ->
        whenever(documentService.uploadDocument(eq(document(file, i + 1)), anyMap())).thenReturn(UUID.fromString(uuid))
      }
    }

    val migration = ExclusionZoneUploadsMigration(
      documentService,
      additionalConditionUploadDetailRepository,
      additionalConditionUploadSummaryRepository,
    )
    migration.perform()

    val firstDetail = additionalConditionUploadDetailRepository.findById(1).orElseThrow()
    assertThat(firstDetail.originalDataDsUuid).isEqualTo("3ff02cc4-d317-45f5-8b10-6f20e0c5e5da")
    assertThat(firstDetail.fullSizeImageDsUuid).isEqualTo("f0adcafd-fb9c-49a5-9fb5-0741e7da4828")

    val firstSummary = additionalConditionUploadSummaryRepository.findById(1).orElseThrow()
    assertThat(firstSummary.thumbnailImageDsUuid).isEqualTo("5cb3ea95-512e-4b04-818e-73cbe37597a3")

    val secondDetail = additionalConditionUploadDetailRepository.findById(2).orElseThrow()
    assertThat(secondDetail.originalDataDsUuid).isEqualTo("a41bdf1b-8fb4-4cfc-beb4-c5c02888999c")
    assertThat(secondDetail.fullSizeImageDsUuid).isEqualTo("3108f563-a6f0-4677-ac25-15b828bfc9db")

    val secondSummary = additionalConditionUploadSummaryRepository.findById(2).orElseThrow()
    assertThat(secondSummary.thumbnailImageDsUuid).isEqualTo("87632006-6b54-4644-b160-783f42dab505")
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-2.sql",
    "classpath:test_data/seed-additional-condition-exclusion-zone-uploads.sql",
  )
  fun `can migrate a subset of the documents that need migrating ordered by id desc`() {
    val migration = ExclusionZoneUploadsMigration(
      documentService,
      additionalConditionUploadDetailRepository,
      additionalConditionUploadSummaryRepository,
    )

    migration.perform(batchSize = 1)

    listOf(
      Pair(1, never()),
      Pair(2, atMostOnce()),
    ).forEach { (id, invoked) ->
      verify(documentService, invoked).uploadDocument(document("originalData", id), metadata("pdf"))
      verify(documentService, invoked).uploadDocument(document("fullSizeImage", id), metadata("fullSizeImage"))
      verify(documentService, invoked).uploadDocument(document("thumbnailImage", id), metadata("thumbnail"))
    }
  }

  private fun document(of: String, id: Int) = "$of$id".toByteArray()

  private fun metadata(kind: String) = mapOf(
    "licenceId" to "2",
    "additionalConditionId" to "1",
    "kind" to kind,
  )
}
