package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
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

  @Nested
  inner class ProcessApPssLicenceChunk {

    @Test
    fun `should process AP_PSS licences and remove PSS conditions`() {
      // Given
      val licence1 = createLicence(id = 1)
      val licence2 = createLicence(id = 2)

      val ids = listOf(1L, 2L)

      whenever(repository.findAllById(ids)).thenReturn(listOf(licence1, licence2))

      // When
      chunkService.processApPssLicenceChunk(ids)

      // Then
      assertThat(listOf(licence1, licence2)).allSatisfy { licence ->
        assertThat(licence.typeCode).isEqualTo(LicenceType.AP)
        assertThat(licence.additionalConditions).noneMatch { it.conditionType == "PSS" }
        assertThat(licence.standardConditions).noneMatch { it.conditionType == "PSS" }
      }

      verify(auditEventRepository).saveAll(any<List<AuditEvent>>())
    }

    @Test
    fun `should skip licences that are not AP_PSS`() {
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
    fun `should process only AP_PSS licences in mixed batch`() {
      // Given
      val apPss = createLicence(id = 1, typeCode = LicenceType.AP_PSS)
      val ap = createLicence(id = 2, typeCode = LicenceType.AP)

      whenever(repository.findAllById(listOf(1L, 2L)))
        .thenReturn(listOf(apPss, ap))

      val captor = argumentCaptor<List<AuditEvent>>()

      // When
      chunkService.processApPssLicenceChunk(listOf(1L, 2L))

      // Then
      assertThat(apPss.typeCode).isEqualTo(LicenceType.AP)
      assertThat(ap.typeCode).isEqualTo(LicenceType.AP)

      verify(auditEventRepository).saveAll(captor.capture())
      assertThat(captor.firstValue).hasSize(1)
    }

    @Test
    fun `should create correct audit event`() {
      // Given
      val licence = createLicence(id = 1).apply {
        forename = "John"
        surname = "Smith"
      }

      whenever(repository.findAllById(listOf(1L))).thenReturn(listOf(licence))

      val captor = argumentCaptor<List<AuditEvent>>()

      // When
      chunkService.processApPssLicenceChunk(listOf(1L))

      // Then
      verify(auditEventRepository).saveAll(captor.capture())

      val audit = captor.firstValue.first()

      assertCommonAuditFields(audit)

      assertThat(audit.summary)
        .contains("John Smith")
        .contains("AP")
        .contains("PSS repeal")

      assertThat(audit.detail)
        .contains("ID 1")
        .contains("type AP_PSS")
        .contains("status IN_PROGRESS")
        .contains("batch details")

      assertThat(audit.changes).isNotNull
      val changes = audit.changes!!["changes"] as Map<*, *>

      assertThat(changes["oldTypeCode"]).isEqualTo(LicenceType.AP_PSS.name)
      assertThat(changes["newTypeCode"]).isEqualTo(LicenceType.AP.name)
    }

    @Test
    fun `should return immediately when licenceIds is empty`() {
      // Given
      val ids = emptyList<Long>()

      // When
      chunkService.processApPssLicenceChunk(ids)

      // Then
      verifyNoInteractions(repository)
      verifyNoInteractions(auditEventRepository)
    }
  }

  @Nested
  inner class ProcessPssLicenceChunk {

    @Test
    fun `should mark licences as inactive`() {
      // Given
      val licence = createLicence(
        id = 1,
        typeCode = LicenceType.PSS,
        status = LicenceStatus.ACTIVE,
      ).apply {
        forename = "John"
        surname = "Smith"
      }

      whenever(repository.findAllById(listOf(1L))).thenReturn(listOf(licence))

      val captor = argumentCaptor<List<AuditEvent>>()

      // When
      chunkService.processPssLicenceChunk(listOf(1L))

      // Then
      assertThat(licence.statusCode).isEqualTo(LicenceStatus.INACTIVE)

      verify(auditEventRepository).saveAll(captor.capture())

      val audit = captor.firstValue.first()

      assertCommonAuditFields(audit)

      assertThat(audit.summary)
        .contains("John Smith")
        .contains("INACTIVE")
        .contains("PSS repeal")

      assertThat(audit.detail)
        .contains("ID 1")
        .contains("status ACTIVE")
        .contains("batch details")

      assertThat(audit.changes).isNotNull

      val changes = audit.changes!!["changes"] as Map<*, *>
      assertThat(changes["oldStatusCode"]).isEqualTo(LicenceStatus.ACTIVE.name)
      assertThat(changes["newStatusCode"]).isEqualTo(LicenceStatus.INACTIVE.name)
    }

    @Test
    fun `should return immediately when licenceIds is empty`() {
      // Given
      val ids = emptyList<Long>()

      // When
      chunkService.processPssLicenceChunk(ids)

      // Then
      verifyNoInteractions(repository)
      verifyNoInteractions(auditEventRepository)
    }
  }

  @Nested
  inner class CommonBehaviour {

    @Test
    fun `should not save audit events when no valid AP_PSS licences`() {
      // Given
      val licence = createLicence(id = 1, typeCode = LicenceType.AP)

      whenever(repository.findAllById(listOf(1L))).thenReturn(listOf(licence))

      // When
      chunkService.processApPssLicenceChunk(listOf(1L))

      // Then
      verifyNoInteractions(auditEventRepository)
    }
  }

  private fun createLicence(
    id: Long = 1L,
    typeCode: LicenceType = LicenceType.AP_PSS,
    status: LicenceStatus = LicenceStatus.IN_PROGRESS,
  ): Licence {
    val licence = CrdLicence(
      id = id,
      typeCode = typeCode,
      responsibleCom = mock<CommunityOffenderManager>(),
      statusCode = status,
      standardConditions = mutableListOf(),
      additionalConditions = mutableListOf(),
    )

    licence.additionalConditions.add(
      AdditionalCondition(
        id = 1,
        licence = licence,
        conditionType = "PSS",
        conditionText = "PSS condition",
        conditionCode = "PSS_CODE",
        conditionCategory = "TEST",
        conditionVersion = "1",
      ),
    )

    licence.standardConditions.add(
      StandardCondition(
        id = 1,
        licence = licence,
        conditionType = "PSS",
        conditionText = "PSS condition",
        conditionCode = "PSS_CODE",
        conditionSequence = 1,
      ),
    )

    return licence
  }

  private fun assertCommonAuditFields(audit: AuditEvent) {
    assertThat(audit.licenceId).isEqualTo(1L)
    assertThat(audit.eventType).isEqualTo(AuditEventType.SYSTEM_EVENT)
    assertThat(audit.username).isEqualTo(Licence.SYSTEM_USER)
    assertThat(audit.fullName).isEqualTo(Licence.SYSTEM_USER)
  }
}
