package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.documents

import org.junit.jupiter.api.Assertions.assertEquals
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

      assertEquals(3, allValues.size)
      assertEquals(allValues.distinct(), allValues)
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

      assertEquals(firstValue, documentUuid)
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

      assertEquals(DocumentType.EXCLUSION_ZONE_MAP, firstValue)
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

      assertEquals(givenMetadata, firstValue)
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

      assertEquals(file, firstValue)
    }
  }
}
