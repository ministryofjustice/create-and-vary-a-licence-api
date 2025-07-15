package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.documents

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import java.util.UUID

class DocumentServiceDownloadDocumentTest {
  private val documentApiClient = mock<DocumentApiClient>()

  @Test
  fun `retrieves the document with the given id from the remote server`() {
    val uuid = UUID.randomUUID()
    val file = byteArrayOf(4, 5, 6, 7, 8)
    val documentService = DocumentService(documentApiClient)

    whenever(documentApiClient.downloadDocumentFile(uuid)).thenReturn(file)

    assertThat(documentService.downloadDocument(uuid)).isEqualTo(file)
  }
}
