package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.document

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionDocuments
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionDocumentsRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository

class MigrateDocumentsToDSServiceTest {
  private val documentService = mock<DocumentService>()
  private val additionalConditionDocumentsRepository = mock<AdditionalConditionDocumentsRepository> { }
  val auditEventRepository = mock<AuditEventRepository>()
  private val migrateDocumentsToDSService =
    MigrateDocumentsToDSService(
      documentService = documentService,
      additionalConditionDocumentsRepository = additionalConditionDocumentsRepository,
      auditEventRepository = auditEventRepository,
    )

  @BeforeEach
  fun reset() {
    val authentication = mock<Authentication>()
    val securityContext = mock<SecurityContext>()

    whenever(authentication.name).thenReturn("smills")
    whenever(securityContext.authentication).thenReturn(authentication)
    SecurityContextHolder.setContext(securityContext)

    org.mockito.kotlin.reset(
      documentService,
      auditEventRepository,
      auditEventRepository,
    )
  }

  @Test
  fun `Given there is a exclusion zone map When migrate documents Then it is posted to document service`() {
    val docs =
      AdditionalConditionDocuments(
        id = 1,
        licenceId = 1,
        additionalConditionId = 1,
        thumbnailImage = "img".toByteArray(),
        fullSizeImage = "img".toByteArray(),
        originalData = "img".toByteArray(),
      )
    whenever(additionalConditionDocumentsRepository.getFilesWhichAreNotCopiedToDocumentService(any())).thenReturn(listOf(docs))
    migrateDocumentsToDSService.migrateDocuments(1)
    verify(documentService, times(3)).postFileToDocumentService(any(), any(), any(), any())
  }

  @Test
  fun `Given files are copied to document service When delete document then it is removed from db`() {
    val docs =
      AdditionalConditionDocuments(
        id = 1,
        licenceId = 1,
        additionalConditionId = 1,
        thumbnailImage = "img".toByteArray(),
        fullSizeImage = "img".toByteArray(),
        originalData = "img".toByteArray(),
        thumbnailImageDsUuid = "uuid",
        fullSizeImageDsUuid = "uuid",
        originalDataDsUuid = "uuid",
      )
    whenever(additionalConditionDocumentsRepository.getFilesWhichAreAlreadyCopiedToDocumentService(any())).thenReturn(listOf(docs))
    migrateDocumentsToDSService.removeDocuments(1)
    verify(additionalConditionDocumentsRepository, times(1)).saveAndFlush(any())
  }
}
