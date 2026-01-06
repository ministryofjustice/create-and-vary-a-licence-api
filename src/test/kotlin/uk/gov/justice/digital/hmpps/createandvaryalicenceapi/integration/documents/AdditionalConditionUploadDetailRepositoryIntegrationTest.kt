package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.documents

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionUploadDetailRepository

class AdditionalConditionUploadDetailRepositoryIntegrationTest : IntegrationTestBase() {

  @Autowired
  lateinit var repository: AdditionalConditionUploadDetailRepository

  private val allAdditionalConditions = listOf(1L, 2L, 3L)
  private val someAdditionalConditions = listOf(1L, 2L)

  @Sql(
    "classpath:test_data/seed-a-few-licences.sql",
    "classpath:test_data/seed-uploads-for-copied-licences.sql",
  )
  @Test
  fun `returns the count of the times each document is referenced for the given set of additional conditions`() {
    assertThat(
      repository
        .countsOfDocumentsRelatedTo(allAdditionalConditions)
        .associate { it.uuid to it.count },
    ).isEqualTo(
      mapOf(
        "37eb7e31-a133-4259-96bc-93369b917eb8" to 2,
        "1595ef41-36e0-4fa8-a98b-bce5c5c98220" to 2,
        "20ca047a-0ae6-4c09-8b97-e3f211feb733" to 1,
        "92939445-4159-4214-aa75-d07568a3e136" to 1,
        "0bbf1459-ee7a-4114-b509-eb9a3fcc2756" to 1,
        "53655fe1-1368-4ed3-bfb0-2727a4e73be5" to 2,
      ),
    )

    val scopedResults = repository
      .countsOfDocumentsRelatedTo(someAdditionalConditions)
      .associate { it.uuid to it.count }

    assertThat(scopedResults).doesNotContainKey("0bbf1459-ee7a-4114-b509-eb9a3fcc2756")
    assertThat(scopedResults).isEqualTo(
      mapOf(
        "37eb7e31-a133-4259-96bc-93369b917eb8" to 2,
        "1595ef41-36e0-4fa8-a98b-bce5c5c98220" to 2,
        "20ca047a-0ae6-4c09-8b97-e3f211feb733" to 1,
        "92939445-4159-4214-aa75-d07568a3e136" to 1,
        "53655fe1-1368-4ed3-bfb0-2727a4e73be5" to 2,
      ),
    )
  }
}
