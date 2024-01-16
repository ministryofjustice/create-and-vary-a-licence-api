package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.document

import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.document.DocumentMetaData

class DocumentServiceTest {
  @Test
  fun `given a file when post to document service then document is sent to document service `() {
    val documentApiClient = mock<DocumentApiClient>()

    val documentService =
      DocumentService(
        documentApiClient = documentApiClient,
      )
    documentService.postFileToDocumentService(
      file = "file".toByteArray(),
      fileType = MediaType.APPLICATION_PDF,
      metadata =
      DocumentMetaData(
        licenceId = "1",
        additionalConditionId = "1",
        documentType = "gif",
      ),
      documentType = "CVL_DOCS",
    )
    verify(documentApiClient, times(1)).postDocument(any(), any(), any(), any(), any())
  }
}
