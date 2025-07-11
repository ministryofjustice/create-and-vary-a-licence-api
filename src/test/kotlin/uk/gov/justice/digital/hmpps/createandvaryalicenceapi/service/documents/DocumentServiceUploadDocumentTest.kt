package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.documents

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import java.util.UUID

class DocumentServiceUploadDocumentTest {
  private val documentApiClient = mock<DocumentApiClient>()

  @Test
  fun `generates a new document UUID each time it is called`() {
    val documentService = DocumentService(documentApiClient)
    val file = byteArrayOf(1, 2, 3)

    documentService.uploadDocument(file = file)
    documentService.uploadDocument(file = file)
    documentService.uploadDocument(file = file)

    with(argumentCaptor<UUID>()) {
      verify(documentApiClient, times(3))
        .uploadDocument(documentUuid = capture(), documentType = anyOrNull(), file = anyOrNull(), metadata = anyOrNull())

      assertThat(allValues).hasSize(3)
      assertThat(allValues).isEqualTo(allValues.distinct())
    }
  }

  @Test
  fun `returns the generated UUID when the upload is a success`() {
    val documentService = DocumentService(documentApiClient)
    val file = byteArrayOf(1, 2, 3)

    val documentUuid = documentService.uploadDocument(file = file)

    with(argumentCaptor<UUID>()) {
      verify(documentApiClient)
        .uploadDocument(documentUuid = capture(), documentType = anyOrNull(), file = anyOrNull(), metadata = anyOrNull())

      assertThat(firstValue).isEqualTo(documentUuid)
    }
  }

  @Test
  fun `uploads the document as a EXCLUSION_ZONE_MAP document type`() {
    val documentService = DocumentService(documentApiClient)
    val file = byteArrayOf(1, 2, 3)

    documentService.uploadDocument(file = file)

    with(argumentCaptor<DocumentType>()) {
      verify(documentApiClient)
        .uploadDocument(documentUuid = anyOrNull(), documentType = capture(), file = anyOrNull(), metadata = anyOrNull())

      assertThat(firstValue).isEqualTo(DocumentType.EXCLUSION_ZONE_MAP)
    }
  }

  @Test
  fun `uploads the document with provided metadata`() {
    val documentService = DocumentService(documentApiClient)
    val file = byteArrayOf(1, 2, 3)
    val givenMetadata = mapOf("any" to "metadata", "will" to "be saved", "on" to "upload")

    documentService.uploadDocument(metadata = givenMetadata, file = file)

    with(argumentCaptor<Map<String, String>>()) {
      verify(documentApiClient)
        .uploadDocument(documentUuid = anyOrNull(), documentType = anyOrNull(), file = anyOrNull(), metadata = capture())

      assertThat(firstValue).isEqualTo(givenMetadata)
    }
  }

  @Test
  fun `uploads the given file bytes to the remote document service`() {
    val documentService = DocumentService(documentApiClient)
    val file = byteArrayOf(1, 2, 3)

    documentService.uploadDocument(file = file)

    with(argumentCaptor<ByteArray>()) {
      verify(documentApiClient)
        .uploadDocument(documentUuid = anyOrNull(), documentType = anyOrNull(), file = capture(), metadata = anyOrNull())

      assertThat(firstValue).isEqualTo(file)
    }
  }
}
