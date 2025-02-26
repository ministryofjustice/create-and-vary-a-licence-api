package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.support

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.reset
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.StandardCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.AdditionalConditionAp
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.AdditionalConditionPss
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.ILicenceCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StaffRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createCrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.LicencePolicyService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.support.LicenceTypeOverrideService.ErrorType.IS_IN_PAST
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.support.LicenceTypeOverrideService.ErrorType.IS_MISSING
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.support.LicenceTypeOverrideService.ErrorType.IS_PRESENT
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.DetailedValidationException
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType.AP
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType.AP_PSS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType.PSS
import java.time.LocalDate
import java.util.Optional

class LicenceTypeOverrideServiceTest {

  private val licenceRepository = mock<LicenceRepository>()
  private val auditEventRepository = mock<AuditEventRepository>()
  private val staffRepository = mock<StaffRepository>()

  private val licenceOverrideService = LicenceTypeOverrideService(
    licenceRepository,
    auditEventRepository,
    staffRepository,
    policyService,
  )

  @BeforeEach
  fun reset() {
    val authentication = mock<Authentication>()
    val securityContext = mock<SecurityContext>()

    whenever(authentication.name).thenReturn("smills")
    whenever(securityContext.authentication).thenReturn(authentication)
    SecurityContextHolder.setContext(securityContext)
    reset(staffRepository, licenceRepository)

    whenever(staffRepository.findByUsernameIgnoreCase("smills")).thenReturn(aCom)
  }

  @Test
  fun `throws exception when no licence present`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.empty())

    assertThrows<jakarta.persistence.EntityNotFoundException> {
      licenceOverrideService.changeType(1L, AP, "Test change to AP")
    }
  }

  @Nested
  inner class `To AP` {
    @Test
    fun `throws exception from PSS and dates not updated`() {
      val licence = pssLicenceWithAllConditions.copy(
        licenceExpiryDate = null,
        topupSupervisionExpiryDate = LocalDate.of(2021, 1, 2),
      )
      whenever(licenceRepository.findById(licence.id)).thenReturn(Optional.of(licence))

      val error = assertThrows<DetailedValidationException> {
        licenceOverrideService.changeType(licence.id, AP, "Test change to AP")
      }

      assertThat(error.title).isEqualTo("Incorrect dates for new licence type: AP")
      assertThat(error.message).isEqualTo("Incorrect dates for new licence type: AP")
      assertThat(error.errors).isEqualTo(
        mapOf(
          "fieldErrors" to mapOf(
            "LED" to IS_MISSING,
            "TUSED" to IS_PRESENT,
          ),
        ),
      )
    }

    @Test
    fun `throws exception from AP_PSS and dates not updated`() {
      val licence = apAndPssLicenceWithAllConditions.copy(
        licenceExpiryDate = LocalDate.of(2021, 1, 2),
        topupSupervisionExpiryDate = LocalDate.of(2021, 1, 2),
      )
      whenever(licenceRepository.findById(licence.id)).thenReturn(Optional.of(licence))

      val error = assertThrows<DetailedValidationException> {
        licenceOverrideService.changeType(licence.id, AP, "Test change to AP")
      }

      assertThat(error.title).isEqualTo("Incorrect dates for new licence type: AP")
      assertThat(error.message).isEqualTo("Incorrect dates for new licence type: AP")
      assertThat(error.errors).isEqualTo(mapOf("fieldErrors" to mapOf("TUSED" to IS_PRESENT)))
    }

    @Test
    fun `successfully converts from PSS`() {
      val licence = pssLicenceWithAllConditions.copy(
        licenceExpiryDate = LocalDate.of(2021, 1, 2),
        topupSupervisionExpiryDate = null,
      )
      whenever(licenceRepository.findById(licence.id)).thenReturn(Optional.of(licence))

      licenceOverrideService.changeType(licence.id, AP, "Test change to AP")

      argumentCaptor<Licence>().apply {
        verify(licenceRepository, times(1)).saveAndFlush(capture())
        assertThat(firstValue.typeCode).isEqualTo(AP)
        assertThat(firstValue.standardConditions.filter { it.conditionType == "AP" }).hasSize(9)
        assertThat(firstValue.standardConditions.filter { it.conditionType == "PSS" }).isEmpty()
        assertThat(firstValue.additionalConditions.filter { it.conditionType == "AP" }).isEmpty()
        assertThat(firstValue.additionalConditions.filter { it.conditionType == "PSS" }).isEmpty()
        assertThat(firstValue.additionalConditions).isEmpty()
      }

      argumentCaptor<AuditEvent>().apply {
        verify(auditEventRepository, times(1)).saveAndFlush(capture())
        assertThat(firstValue.detail).isEqualTo("ID 1 type AP status IN_PROGRESS version 1.1")
        assertThat(firstValue.username).isEqualTo("smills")
        assertThat(firstValue.summary).isEqualTo("Licence type overridden for John Smith: from PSS to AP: Test change to AP")
        assertThat(firstValue.changes).containsEntry("oldType", "PSS")
        assertThat(firstValue.changes).containsEntry("newType", "AP")
        assertThat((firstValue.changes as Map<String, List<Any>>)["deletedAdditionalConditions"]).hasSize(2)
      }
    }

    @Test
    fun `successfully converts from AP_PSS`() {
      val licence = apAndPssLicenceWithAllConditions.copy(
        licenceExpiryDate = LocalDate.of(2021, 1, 2),
        topupSupervisionExpiryDate = null,
      )
      whenever(licenceRepository.findById(licence.id)).thenReturn(Optional.of(licence))

      licenceOverrideService.changeType(licence.id, AP, "Test change to AP")

      argumentCaptor<Licence>().apply {
        verify(licenceRepository, times(1)).saveAndFlush(capture())
        assertThat(firstValue.typeCode).isEqualTo(AP)
      }
      argumentCaptor<Licence>().apply {
        verify(licenceRepository, times(1)).saveAndFlush(capture())
        assertThat(firstValue.typeCode).isEqualTo(AP)
        assertThat(firstValue.standardConditions.filter { it.conditionType == "AP" }).hasSize(9)
        assertThat(firstValue.standardConditions.filter { it.conditionType == "PSS" }).isEmpty()
        assertThat(firstValue.additionalConditions.filter { it.conditionType == "AP" }).hasSize(51)
        assertThat(firstValue.additionalConditions.filter { it.conditionType == "PSS" }).isEmpty()
      }
      argumentCaptor<AuditEvent>().apply {
        verify(auditEventRepository, times(1)).saveAndFlush(capture())
        assertThat(firstValue.detail).isEqualTo("ID 1 type AP status IN_PROGRESS version 1.1")
        assertThat(firstValue.username).isEqualTo("smills")
        assertThat(firstValue.summary).isEqualTo("Licence type overridden for John Smith: from AP_PSS to AP: Test change to AP")
        assertThat(firstValue.changes).containsEntry("oldType", "AP_PSS")
        assertThat(firstValue.changes).containsEntry("newType", "AP")
        assertThat((firstValue.changes as Map<String, List<Any>>)["deletedAdditionalConditions"]).hasSize(2)
      }
    }

    @Test
    fun `ignores conversion to AP`() {
      val licence = apLicenceWithAllConditions.copy(
        licenceExpiryDate = LocalDate.of(2021, 1, 2),
        topupSupervisionExpiryDate = null,
      )
      whenever(licenceRepository.findById(licence.id)).thenReturn(Optional.of(licence))

      licenceOverrideService.changeType(licence.id, AP, "Test change to AP")

      verify(licenceRepository, never()).saveAndFlush(any())
      verifyNoInteractions(auditEventRepository)
    }
  }

  @Nested
  inner class `To PSS` {
    @Test
    fun `throws exception from AP and dates not updated`() {
      val licence = apLicenceWithAllConditions.copy(
        licenceExpiryDate = LocalDate.of(2021, 1, 2),
        topupSupervisionExpiryDate = null,
      )
      whenever(licenceRepository.findById(licence.id)).thenReturn(Optional.of(licence))

      val error = assertThrows<DetailedValidationException> {
        licenceOverrideService.changeType(licence.id, PSS, "Test change to PSS")
      }

      assertThat(error.title).isEqualTo("Incorrect dates for new licence type: PSS")
      assertThat(error.message).isEqualTo("Incorrect dates for new licence type: PSS")
      assertThat(error.errors).isEqualTo(
        mapOf(
          "fieldErrors" to mapOf(
            "LED" to IS_PRESENT,
            "TUSED" to IS_MISSING,
          ),
        ),
      )
    }

    @Test
    fun `throws exception when TUSED date in past`() {
      val licence = apLicenceWithAllConditions.copy(
        licenceExpiryDate = LocalDate.of(2021, 1, 2),
        topupSupervisionExpiryDate = LocalDate.now().minusDays(1),
      )
      whenever(licenceRepository.findById(licence.id)).thenReturn(Optional.of(licence))

      val error = assertThrows<DetailedValidationException> {
        licenceOverrideService.changeType(licence.id, PSS, "Test change to PSS")
      }

      assertThat(error.title).isEqualTo("Incorrect dates for new licence type: PSS")
      assertThat(error.message).isEqualTo("Incorrect dates for new licence type: PSS")
      assertThat(error.errors).isEqualTo(
        mapOf(
          "fieldErrors" to mapOf(
            "LED" to IS_PRESENT,
            "TUSED" to IS_IN_PAST,
          ),
        ),
      )
    }

    @Test
    fun `throws exception from AP_PSS and dates not updated`() {
      val licence = apAndPssLicenceWithAllConditions.copy(
        licenceExpiryDate = LocalDate.of(2021, 1, 2),
        topupSupervisionExpiryDate = LocalDate.now().plusDays(1),
      )
      whenever(licenceRepository.findById(licence.id)).thenReturn(Optional.of(licence))

      val error = assertThrows<DetailedValidationException> {
        licenceOverrideService.changeType(licence.id, PSS, "Test change to AP")
      }

      assertThat(error.title).isEqualTo("Incorrect dates for new licence type: PSS")
      assertThat(error.message).isEqualTo("Incorrect dates for new licence type: PSS")
      assertThat(error.errors).isEqualTo(mapOf("fieldErrors" to mapOf("LED" to IS_PRESENT)))
    }

    @Test
    fun `successfully converts from AP`() {
      val licence = apLicenceWithAllConditions.copy(
        licenceExpiryDate = null,
        topupSupervisionExpiryDate = LocalDate.now().plusDays(1),
      )
      whenever(licenceRepository.findById(licence.id)).thenReturn(Optional.of(licence))

      licenceOverrideService.changeType(licence.id, PSS, "Test change to PSS")

      argumentCaptor<Licence>().apply {
        verify(licenceRepository, times(1)).saveAndFlush(capture())
        assertThat(firstValue.typeCode).isEqualTo(PSS)
        assertThat(firstValue.standardConditions.filter { it.conditionType == "AP" }).isEmpty()
        assertThat(firstValue.standardConditions.filter { it.conditionType == "PSS" }).hasSize(8)
        assertThat(firstValue.additionalConditions.filter { it.conditionType == "AP" }).isEmpty()
        assertThat(firstValue.additionalConditions.filter { it.conditionType == "PSS" }).isEmpty()
      }
      argumentCaptor<AuditEvent>().apply {
        verify(auditEventRepository, times(1)).saveAndFlush(capture())
        assertThat(firstValue.detail).isEqualTo("ID 1 type PSS status IN_PROGRESS version 1.1")
        assertThat(firstValue.username).isEqualTo("smills")
        assertThat(firstValue.summary).isEqualTo("Licence type overridden for John Smith: from AP to PSS: Test change to PSS")
        assertThat(firstValue.changes).containsEntry("oldType", "AP")
        assertThat(firstValue.changes).containsEntry("newType", "PSS")
        assertThat((firstValue.changes as Map<String, List<Any>>)["deletedAdditionalConditions"]).hasSize(51)
      }
    }

    @Test
    fun `successfully converts from AP_PSS`() {
      val licence = apAndPssLicenceWithAllConditions.copy(
        licenceExpiryDate = null,
        topupSupervisionExpiryDate = LocalDate.now().plusDays(1),
      )
      whenever(licenceRepository.findById(licence.id)).thenReturn(Optional.of(licence))

      licenceOverrideService.changeType(licence.id, PSS, "Test change to PSS")

      argumentCaptor<Licence>().apply {
        verify(licenceRepository, times(1)).saveAndFlush(capture())
        assertThat(firstValue.typeCode).isEqualTo(PSS)
        assertThat(firstValue.standardConditions.filter { it.conditionType == "AP" }).isEmpty()
        assertThat(firstValue.standardConditions.filter { it.conditionType == "PSS" }).hasSize(8)
        assertThat(firstValue.additionalConditions.filter { it.conditionType == "AP" }).isEmpty()
        assertThat(firstValue.additionalConditions.filter { it.conditionType == "PSS" }).hasSize(2)
      }
      argumentCaptor<AuditEvent>().apply {
        verify(auditEventRepository, times(1)).saveAndFlush(capture())
        assertThat(firstValue.detail).isEqualTo("ID 1 type PSS status IN_PROGRESS version 1.1")
        assertThat(firstValue.username).isEqualTo("smills")
        assertThat(firstValue.summary).isEqualTo("Licence type overridden for John Smith: from AP_PSS to PSS: Test change to PSS")
        assertThat(firstValue.changes).containsEntry("oldType", "AP_PSS")
        assertThat(firstValue.changes).containsEntry("newType", "PSS")
        assertThat((firstValue.changes as Map<String, List<Any>>)["deletedAdditionalConditions"]).hasSize(51)
      }
    }

    @Test
    fun `ignores conversion to PSS`() {
      val licence = pssLicenceWithAllConditions.copy(
        licenceExpiryDate = LocalDate.of(2021, 1, 2),
        topupSupervisionExpiryDate = null,
      )
      whenever(licenceRepository.findById(licence.id)).thenReturn(Optional.of(licence))

      licenceOverrideService.changeType(licence.id, PSS, "Test change to PSS")

      verify(licenceRepository, never()).saveAndFlush(any())
      verifyNoInteractions(auditEventRepository)
    }
  }

  @Nested
  inner class `To AP_PSS` {
    @Test
    fun `throws exception from AP and dates not updated`() {
      val licence = apLicenceWithAllConditions.copy(
        licenceExpiryDate = LocalDate.of(2021, 1, 2),
        topupSupervisionExpiryDate = null,
      )
      whenever(licenceRepository.findById(licence.id)).thenReturn(Optional.of(licence))

      val error = assertThrows<DetailedValidationException> {
        licenceOverrideService.changeType(licence.id, AP_PSS, "Test change to AP_PSS")
      }

      assertThat(error.title).isEqualTo("Incorrect dates for new licence type: AP_PSS")
      assertThat(error.message).isEqualTo("Incorrect dates for new licence type: AP_PSS")
      assertThat(error.errors).isEqualTo(mapOf("fieldErrors" to mapOf("TUSED" to IS_MISSING)))
    }

    @Test
    fun `throws exception from PSS and dates not updated`() {
      val licence = pssLicenceWithAllConditions.copy(
        licenceExpiryDate = null,
        topupSupervisionExpiryDate = LocalDate.now().plusDays(1),
      )
      whenever(licenceRepository.findById(licence.id)).thenReturn(Optional.of(licence))

      val error = assertThrows<DetailedValidationException> {
        licenceOverrideService.changeType(licence.id, AP_PSS, "Test change to AP_PSS")
      }

      assertThat(error.title).isEqualTo("Incorrect dates for new licence type: AP_PSS")
      assertThat(error.message).isEqualTo("Incorrect dates for new licence type: AP_PSS")
      assertThat(error.errors).isEqualTo(mapOf("fieldErrors" to mapOf("LED" to IS_MISSING)))
    }

    @Test
    fun `throws exception when TUSED date in past`() {
      val licence = pssLicenceWithAllConditions.copy(
        licenceExpiryDate = null,
        topupSupervisionExpiryDate = LocalDate.now().minusDays(1),
      )
      whenever(licenceRepository.findById(licence.id)).thenReturn(Optional.of(licence))

      val error = assertThrows<DetailedValidationException> {
        licenceOverrideService.changeType(licence.id, AP_PSS, "Test change to AP_PSS")
      }

      assertThat(error.title).isEqualTo("Incorrect dates for new licence type: AP_PSS")
      assertThat(error.message).isEqualTo("Incorrect dates for new licence type: AP_PSS")
      assertThat(error.errors).isEqualTo(
        mapOf(
          "fieldErrors" to mapOf(
            "LED" to IS_MISSING,
            "TUSED" to IS_IN_PAST,
          ),
        ),
      )
    }

    @Test
    fun `successfully converts from AP`() {
      val licence = apLicenceWithAllConditions.copy(
        licenceExpiryDate = LocalDate.of(2021, 1, 2),
        topupSupervisionExpiryDate = LocalDate.now().plusDays(1),
      )
      whenever(licenceRepository.findById(licence.id)).thenReturn(Optional.of(licence))

      licenceOverrideService.changeType(licence.id, AP_PSS, "Test change to AP_PSS")

      argumentCaptor<Licence>().apply {
        verify(licenceRepository, times(1)).saveAndFlush(capture())
        assertThat(firstValue.typeCode).isEqualTo(AP_PSS)
        assertThat(firstValue.standardConditions).hasSize(17)
        assertThat(firstValue.standardConditions.filter { it.conditionType == "AP" }).hasSize(9)
        assertThat(firstValue.standardConditions.filter { it.conditionType == "PSS" }).hasSize(8)
        assertThat(firstValue.additionalConditions.filter { it.conditionType == "AP" }).hasSize(51)
        assertThat(firstValue.additionalConditions.filter { it.conditionType == "PSS" }).isEmpty()
      }
      argumentCaptor<AuditEvent>().apply {
        verify(auditEventRepository, times(1)).saveAndFlush(capture())
        assertThat(firstValue.detail).isEqualTo("ID 1 type AP_PSS status IN_PROGRESS version 1.1")
        assertThat(firstValue.username).isEqualTo("smills")
        assertThat(firstValue.summary).isEqualTo("Licence type overridden for John Smith: from AP to AP_PSS: Test change to AP_PSS")
        assertThat(firstValue.changes).containsEntry("oldType", "AP")
        assertThat(firstValue.changes).containsEntry("newType", "AP_PSS")
        assertThat((firstValue.changes as Map<String, List<Any>>)["deletedAdditionalConditions"]).hasSize(0)
      }
    }

    @Test
    fun `successfully converts from PSS`() {
      val licence = pssLicenceWithAllConditions.copy(
        licenceExpiryDate = LocalDate.of(2021, 1, 2),
        topupSupervisionExpiryDate = LocalDate.now().plusDays(1),
      )
      whenever(licenceRepository.findById(licence.id)).thenReturn(Optional.of(licence))

      licenceOverrideService.changeType(licence.id, AP_PSS, "Test change to AP_PSS")

      argumentCaptor<Licence>().apply {
        verify(licenceRepository, times(1)).saveAndFlush(capture())
        assertThat(firstValue.typeCode).isEqualTo(AP_PSS)
        assertThat(firstValue.standardConditions.filter { it.conditionType == "AP" }).hasSize(9)
        assertThat(firstValue.standardConditions.filter { it.conditionType == "PSS" }).hasSize(8)
        assertThat(firstValue.additionalConditions.filter { it.conditionType == "AP" }).isEmpty()
        assertThat(firstValue.additionalConditions.filter { it.conditionType == "PSS" }).hasSize(2)
        assertThat(firstValue.typeCode).isEqualTo(AP_PSS)
      }
      argumentCaptor<AuditEvent>().apply {
        verify(auditEventRepository, times(1)).saveAndFlush(capture())
        assertThat(firstValue.detail).isEqualTo("ID 1 type AP_PSS status IN_PROGRESS version 1.1")
        assertThat(firstValue.username).isEqualTo("smills")
        assertThat(firstValue.summary).isEqualTo("Licence type overridden for John Smith: from PSS to AP_PSS: Test change to AP_PSS")
        assertThat(firstValue.changes).containsEntry("oldType", "PSS")
        assertThat(firstValue.changes).containsEntry("newType", "AP_PSS")
        assertThat((firstValue.changes as Map<String, List<Any>>)["deletedAdditionalConditions"]).isEmpty()
      }
    }

    @Test
    fun `ignores conversion to AP_PSS`() {
      val licence = apAndPssLicenceWithAllConditions.copy(
        licenceExpiryDate = LocalDate.of(2021, 1, 2),
        topupSupervisionExpiryDate = LocalDate.of(2021, 1, 2),
      )
      whenever(licenceRepository.findById(licence.id)).thenReturn(Optional.of(licence))

      licenceOverrideService.changeType(licence.id, AP_PSS, "Test change to AP_PSS")

      verify(licenceRepository, never()).saveAndFlush(any())
      verifyNoInteractions(auditEventRepository)
    }
  }

  private companion object {
    val policyService = LicencePolicyService()
    val aCom = TestData.com()

    val pssLicenceWithAllConditions = createCrdLicence().let { licence ->
      licence.copy(
        typeCode = PSS,
        licenceExpiryDate = null,
        topupSupervisionExpiryDate = LocalDate.of(2021, 1, 2),
        standardConditions = pssRequirement(licence),
        additionalConditions = pssAdditionalConditions(licence),
      )
    }

    val apAndPssLicenceWithAllConditions = createCrdLicence().let { licence ->
      licence.copy(
        typeCode = AP_PSS,
        licenceExpiryDate = null,
        topupSupervisionExpiryDate = LocalDate.of(2021, 1, 2),
        standardConditions = standardConditions(licence) + pssRequirement(licence),
        additionalConditions = additionalConditions(licence) + pssAdditionalConditions(licence),
      )
    }

    val apLicenceWithAllConditions = createCrdLicence().let { licence ->
      licence.copy(
        typeCode = AP,
        licenceExpiryDate = null,
        topupSupervisionExpiryDate = LocalDate.of(2021, 1, 2),
        standardConditions = standardConditions(licence),
        additionalConditions = additionalConditions(licence),
      )
    }

    private fun standardConditions(licence: CrdLicence): List<StandardCondition> = policyService.currentPolicy().standardConditions.standardConditionsAp
      .mapIndexed { i: Int, condition: ILicenceCondition ->
        StandardCondition(
          licence = licence,
          conditionType = "AP",
          conditionSequence = i,
          conditionCode = condition.code,
          conditionText = condition.text,
        )
      }

    private fun pssRequirement(licence: CrdLicence): List<StandardCondition> = policyService.currentPolicy().standardConditions.standardConditionsPss
      .mapIndexed { i: Int, condition: ILicenceCondition ->
        StandardCondition(
          licence = licence,
          conditionType = "PSS",
          conditionSequence = i,
          conditionCode = condition.code,
          conditionText = condition.text,
        )
      }

    private fun pssAdditionalConditions(licence: CrdLicence): List<AdditionalCondition> = policyService.currentPolicy().additionalConditions.pss.mapIndexed { i: Int, condition: AdditionalConditionPss ->
      AdditionalCondition(
        licence = licence,
        conditionType = "PSS",
        conditionSequence = i,
        conditionCode = condition.code,
        conditionText = condition.text,
        expandedConditionText = condition.text,
        conditionVersion = licence.version!!,
        conditionCategory = condition.categoryShort ?: condition.category,
      )
    }

    private fun additionalConditions(licence: CrdLicence): List<AdditionalCondition> = policyService.currentPolicy().additionalConditions.ap.mapIndexed { i: Int, condition: AdditionalConditionAp ->
      AdditionalCondition(
        licence = licence,
        conditionType = "AP",
        conditionSequence = i,
        conditionCode = condition.code,
        conditionText = condition.text,
        expandedConditionText = condition.text,
        conditionVersion = licence.version!!,
        conditionCategory = condition.categoryShort ?: condition.category,
      )
    }
  }
}
