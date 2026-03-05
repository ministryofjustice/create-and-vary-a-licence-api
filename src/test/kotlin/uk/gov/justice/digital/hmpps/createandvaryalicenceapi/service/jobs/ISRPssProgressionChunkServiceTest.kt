package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.StandardCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AuditEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import kotlin.collections.emptyMap

class ISRPssProgressionChunkServiceTest {

  private val repository = mock<LicenceRepository>()
  private val auditEventRepository = mock<AuditEventRepository>()

  private val chunkService = ISRPssProgressionChunkService(
    repository,
    auditEventRepository,
  )

  @BeforeEach
  fun resetMocks() {
    reset(repository, auditEventRepository)
  }

  @Test
  fun `should process AP_PSS licences and remove PSS conditions`() {
    // Given
    val licence1 = createLicence(id = 1)
    val licence2 = createLicence(id = 2)

    val licenceIds = listOf(1L, 2L)

    whenever(repository.findAllById(licenceIds)).thenReturn(listOf(licence1, licence2))

    // When
    chunkService.processApPssLicenceChunk(licenceIds)

    // Then
    assertThat(licence1.typeCode).isEqualTo(LicenceType.AP)
    assertThat(licence2.typeCode).isEqualTo(LicenceType.AP)

    assertThat(licence1.additionalConditions).noneMatch { it.conditionType == "PSS" }
    assertThat(licence2.additionalConditions).noneMatch { it.conditionType == "PSS" }
    assertThat(licence1.standardConditions).noneMatch { it.conditionType == "PSS" }
    assertThat(licence2.standardConditions).noneMatch { it.conditionType == "PSS" }

    verify(auditEventRepository).saveAll(any<List<AuditEvent>>())
  }

  @Test
  fun `should skip licence when type is not AP_PSS`() {
    // Given
    val licence = createLicence(id = 1, typeCode = LicenceType.AP)

    whenever(repository.findAllById(listOf(1L))).thenReturn(listOf(licence))

    // When
    chunkService.processApPssLicenceChunk(listOf(1L))

    // Then
    assertThat(licence.typeCode).isEqualTo(LicenceType.AP)
    verifyNoInteractions(auditEventRepository)
  }

  @Test
  fun `should return immediately when licenceIds is empty`() {
    // Given
    val licenceIds = emptyList<Long>()

    // When
    chunkService.processApPssLicenceChunk(licenceIds)

    // Then
    verifyNoInteractions(repository)
    verifyNoInteractions(auditEventRepository)
  }

  @Test
  fun `should create correct audit event`() {
    // Given
    val licence = createLicence(id = 1, typeCode = LicenceType.AP_PSS)
      .apply {
        forename = "John"
        surname = "Smith"
      }

    whenever(repository.findAllById(listOf(1L))).thenReturn(listOf(licence))

    val auditCaptor = argumentCaptor<List<AuditEvent>>()

    // When
    chunkService.processApPssLicenceChunk(listOf(1L))

    // Then
    verify(auditEventRepository).saveAll(auditCaptor.capture())

    val audits = auditCaptor.firstValue
    assertThat(audits).hasSize(1)
    assertAuditEvent(audits.first())
  }

  private fun createLicence(
    id: Long = 1L,
    typeCode: LicenceType = LicenceType.AP_PSS,
    status: LicenceStatus = LicenceStatus.IN_PROGRESS,
    includePssAdditionalCondition: Boolean = true,
    includePssStandardCondition: Boolean = true,
  ): Licence {
    val licence = CrdLicence(
      id = id,
      typeCode = typeCode,
      responsibleCom = mock<CommunityOffenderManager>(),
      statusCode = status,
      standardConditions = mutableListOf(),
      additionalConditions = mutableListOf(),
    )

    if (includePssAdditionalCondition) {
      licence.additionalConditions.add(
        AdditionalCondition(
          id = 1,
          licence = licence,
          conditionType = "PSS",
          conditionCode = "PSS_CODE",
          conditionCategory = "TEST",
          conditionText = "PSS condition",
          conditionVersion = "1",
        ),
      )
    }

    if (includePssStandardCondition) {
      licence.standardConditions.add(
        StandardCondition(
          id = 1,
          licence = licence,
          conditionType = "PSS",
          conditionCode = "PSS_CODE",
          conditionText = "PSS condition",
          conditionSequence = 1,
        ),
      )
    }

    return licence
  }

  private fun assertAuditEvent(audit: AuditEvent) {
    assertThat(audit.licenceId).isEqualTo(1L)
    assertThat(audit.eventType).isEqualTo(AuditEventType.SYSTEM_EVENT)
    assertThat(audit.username).isEqualTo(Licence.SYSTEM_USER)
    assertThat(audit.fullName).isEqualTo(Licence.SYSTEM_USER)

    assertThat(audit.summary)
      .contains("John Smith")
      .contains("Licence type automatically changed to AP")

    assertThat(audit.detail)
      .contains("ID 1")
      .contains("type AP_PSS")
      .contains("status IN_PROGRESS")

    val changesMap = audit.changes?.get("changes") as? Map<*, *> ?: emptyMap<Any, Any>()
    assertThat(changesMap["oldTypeCode"]).isEqualTo(LicenceType.AP_PSS.name)
    assertThat(changesMap["newTypeCode"]).isEqualTo(LicenceType.AP.name)

    val additionalDeleted = changesMap["additional conditions deleted for "] as? List<*> ?: emptyList<Any>()
    val standardDeleted = changesMap["standard conditions deleted for "] as? List<*> ?: emptyList<Any>()

    assertThat(additionalDeleted).contains("PSS condition")
    assertThat(standardDeleted).contains("PSS condition")
  }
}
