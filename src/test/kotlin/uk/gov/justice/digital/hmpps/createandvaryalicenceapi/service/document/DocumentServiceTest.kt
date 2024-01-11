package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.document

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.document.DocumentMetaData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.document.LicenceDocumentType

class DocumentServiceTest {
  @Test
  fun postExclusionZoneMaps() {
    val documentService = DocumentService(documentApiClient = mock())
    documentService.postExclusionZoneMaps(
      "file".toByteArray(),
      DocumentMetaData(
        licenceId = "1",
        additionalConditionId = "1",
        documentType = LicenceDocumentType.EXCLUSION_ZONE_MAP_FULL_IMG.toString(),
      ),
      LicenceDocumentType.EXCLUSION_ZONE_MAP.toString(),
    )
  }
}
