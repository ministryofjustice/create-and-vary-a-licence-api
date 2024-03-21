package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.document

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.document.LicenceDocumentType.EXCLUSION_ZONE_MAP_FULL_IMG
import java.nio.charset.StandardCharsets.UTF_8

class DocumentServiceTest {

  @Nested
  inner class Enabled {
    val documentApiClient = mock<DocumentApiClient>()

    val documentService = DocumentService(documentApiClient = documentApiClient, enabled = true)

    @BeforeEach
    fun reset() {
      reset(documentApiClient)
    }

    @Test
    fun `upload file`() {
      val file = "some content".toByteArray(UTF_8)
      val uuid = documentService.uploadExclusionZoneFile(
        type = EXCLUSION_ZONE_MAP_FULL_IMG,
        licenceId = 2L,
        additionalConditionId = 3L,
        file = file,
      )

      verify(documentApiClient, times(1)).postDocument(
        DOCUMENT_TYPE,
        uuid!!,
        EXCLUSION_ZONE_MAP_FULL_IMG.fileType,
        file,
        DocumentMetaData(licenceId = "2", additionalConditionId = "3", subType = EXCLUSION_ZONE_MAP_FULL_IMG),
      )
    }

    @Test
    fun `download file`() {
      val doc = "a result".toByteArray(UTF_8)

      whenever(documentApiClient.getDocument("1234")).thenReturn(doc)

      val result = documentService.getDocument("1234")
      assertThat(result).isEqualTo(doc)
      verify(documentApiClient, times(1)).getDocument("1234")
    }
  }

  @Nested
  inner class Disabled {
    val documentApiClient = mock<DocumentApiClient>()

    val documentService = DocumentService(documentApiClient = documentApiClient, enabled = false)

    @Test
    fun `upload file`() {
      val file = "some content".toByteArray(UTF_8)
      val uuid = documentService.uploadExclusionZoneFile(
        type = EXCLUSION_ZONE_MAP_FULL_IMG,
        licenceId = 2L,
        additionalConditionId = 3L,
        file = file,
      )

      verifyNoInteractions(documentApiClient)
    }

    @Test
    fun `download file`() {
      val result = documentService.getDocument("1234")
      assertThat(result).isNull()
      verifyNoInteractions(documentApiClient)
    }
  }
}
