package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.documents

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionUploadRepository

class AdditionalConditionUploadDetailRepositoryIntegrationTest : IntegrationTestBase() {

  @Autowired
  lateinit var repository: AdditionalConditionUploadRepository

  @Sql(
    "classpath:test_data/seed-a-few-licences.sql",
    "classpath:test_data/seed-uploads-for-copied-licences.sql",
  )
  @Test
  fun `returns all UUIDs for all additional conditions`() {
    // Given
    val allAdditionalConditions = listOf(1L, 2L, 3L)

    // When
    val allUuids = repository.findDocumentUuidsFor(allAdditionalConditions)

    // Then
    assertThat(allUuids).containsExactlyInAnyOrder(
      "37eb7e31-a133-4259-96bc-93369b917eb8",
      "1595ef41-36e0-4fa8-a98b-bce5c5c98220",
      "20ca047a-0ae6-4c09-8b97-e3f211feb733",
      "92939445-4159-4214-aa75-d07568a3e136",
      "0bbf1459-ee7a-4114-b509-eb9a3fcc2756",
      "53655fe1-1368-4ed3-bfb0-2727a4e73be5",
    )
  }

  @Sql(
    "classpath:test_data/seed-a-few-licences.sql",
    "classpath:test_data/seed-uploads-for-copied-licences.sql",
  )
  @Test
  fun `returns UUIDs scoped to a subset of additional conditions`() {
    // Given
    val someAdditionalConditions = listOf(1L, 2L)

    // When
    val scopedUuids = repository.findDocumentUuidsFor(someAdditionalConditions)

    // Then
    assertThat(scopedUuids).doesNotContain("0bbf1459-ee7a-4114-b509-eb9a3fcc2756")
    assertThat(scopedUuids).containsExactlyInAnyOrder(
      "37eb7e31-a133-4259-96bc-93369b917eb8",
      "1595ef41-36e0-4fa8-a98b-bce5c5c98220",
      "20ca047a-0ae6-4c09-8b97-e3f211feb733",
      "92939445-4159-4214-aa75-d07568a3e136",
      "53655fe1-1368-4ed3-bfb0-2727a4e73be5",
    )
  }

  @Sql(
    "classpath:test_data/seed-a-few-licences.sql",
    "classpath:test_data/seed-uploads-for-copied-licences.sql",
  )
  @Test
  fun `returns true for UUID that is uploaded only once`() {
    // Given
    val uuid = "20ca047a-0ae6-4c09-8b97-e3f211feb733"

    // When
    val result = repository.hasOnlyOneUpload(uuid)

    // Then
    assertThat(result).isTrue
  }

  @Sql(
    "classpath:test_data/seed-a-few-licences.sql",
    "classpath:test_data/seed-uploads-for-copied-licences.sql",
  )
  @Test
  fun `returns false for UUID that is uploaded multiple times`() {
    // Given
    val uuid = "37eb7e31-a133-4259-96bc-93369b917eb8"

    // When
    val result = repository.hasOnlyOneUpload(uuid)

    // Then
    assertThat(result).isFalse
  }
}
