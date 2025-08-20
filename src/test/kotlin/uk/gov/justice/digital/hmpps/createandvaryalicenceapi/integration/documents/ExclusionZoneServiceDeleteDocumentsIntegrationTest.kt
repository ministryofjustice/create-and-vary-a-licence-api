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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionUploadDetailRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.DocumentCountsRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions.ExclusionZoneService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.documents.DocumentService
import java.util.UUID
import kotlin.jvm.optionals.getOrElse

class ExclusionZoneServiceDeleteDocumentsIntegrationTest : IntegrationTestBase() {

  lateinit var exclusionZoneService: ExclusionZoneService

  @Autowired lateinit var licenceRepository: LicenceRepository

  @Autowired lateinit var additionalConditionRepository: AdditionalConditionRepository

  @Autowired lateinit var additionalConditionUploadDetailRepository: AdditionalConditionUploadDetailRepository

  @Autowired lateinit var documentCountsRepository: DocumentCountsRepository
  val documentService: DocumentService = mock()

  @BeforeEach
  fun setup() {
    exclusionZoneService = ExclusionZoneService(
      licenceRepository,
      additionalConditionRepository,
      additionalConditionUploadDetailRepository,
      documentService,
      documentCountsRepository,
    )
  }

  @Sql(
    "classpath:test_data/seed-a-few-licences.sql",
    "classpath:test_data/seed-uploads-for-copied-licences.sql",
  )
  @Test
  fun `deletes all documents attached to the given licence that are not attached to another copy of the licence`() {
    val licence = licenceRepository.findById(2L)
      .getOrElse { throw AssertionError("Licence not found") }

    exclusionZoneService.deleteDocumentsFor(licence)

    mapOf(
      "37eb7e31-a133-4259-96bc-93369b917eb8" to never(), // 2 references to this doc
      "1595ef41-36e0-4fa8-a98b-bce5c5c98220" to never(), // 2 references to this doc
      "53655fe1-1368-4ed3-bfb0-2727a4e73be5" to never(), // 2 references to this doc
      "92939445-4159-4214-aa75-d07568a3e136" to atMostOnce(),
      "0bbf1459-ee7a-4114-b509-eb9a3fcc2756" to atMostOnce(),
    ).forEach { (uuid, invoked) ->
      verify(documentService, invoked).deleteDocument(UUID.fromString(uuid))
    }

    // All AdditionalConditionUploadDetails connected to this licence have been deleted
    assertThat(additionalConditionUploadDetailRepository.findById(1L)).isEmpty
    assertThat(additionalConditionUploadDetailRepository.findById(2L)).isPresent
    assertThat(additionalConditionUploadDetailRepository.findById(3L)).isEmpty
  }
}
