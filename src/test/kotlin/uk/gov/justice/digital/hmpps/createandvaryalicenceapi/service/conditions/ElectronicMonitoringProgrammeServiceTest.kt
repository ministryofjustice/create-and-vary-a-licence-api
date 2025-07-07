package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.ElectronicMonitoringProvider
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.AdditionalConditionAp
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.Input
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.InputType.TEXT
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateElectronicMonitoringProgrammeRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StaffRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.AuditService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.LicencePolicyService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.POLICY_V2_1
import java.util.Optional

class ElectronicMonitoringProgrammeServiceTest {
  private val licenceRepository = mock<LicenceRepository>()
  private val policyService = mock<LicencePolicyService>()
  private val auditService = mock<AuditService>()
  private val staffRepository = mock<StaffRepository>()
  private val aLicenceEntity = TestData.createCrdLicence()
  private val aVariationLicence = TestData.createVariationLicence()
  private val aCom = TestData.com()
  private val serviceWithFeatureEnabled = ElectronicMonitoringProgrammeService(
    licenceRepository,
    policyService,
    auditService,
    staffRepository,
    electronicMonitoringResponseHandlingEnabled = true,
  )

  private val service = ElectronicMonitoringProgrammeService(
    licenceRepository,
    policyService,
    auditService,
    staffRepository,
  )

  @BeforeEach
  fun beforeEach() {
    val authentication = mock<Authentication>()
    val securityContext = mock<SecurityContext>()
    whenever(authentication.name).thenReturn("tcom")
    whenever(securityContext.authentication).thenReturn(authentication)
    whenever(policyService.getConfigForCondition(any(), any())).thenReturn(CONDITION_CONFIG)

    SecurityContextHolder.setContext(securityContext)
  }

  @AfterEach
  fun afterEach() {
    reset(licenceRepository, policyService, auditService, staffRepository)
  }

  @Nested
  inner class `electronic monitoring programme` {
    @Test
    fun `update electronic monitoring programme details`() {
      val electronicMonitoringProvider = ElectronicMonitoringProvider(
        isToBeTaggedForProgramme = false,
        programmeName = "Old Programme",
        licence = aLicenceEntity,
      )

      val crdLicence = aLicenceEntity.copy(electronicMonitoringProvider = electronicMonitoringProvider)

      whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(crdLicence))
      whenever(staffRepository.findByUsernameIgnoreCase("tcom")).thenReturn(aCom)

      val request = UpdateElectronicMonitoringProgrammeRequest(
        isToBeTaggedForProgramme = true,
        programmeName = "Programme Name",
      )

      service.updateElectronicMonitoringProgramme(1L, request)

      val licenceCaptor = ArgumentCaptor.forClass(CrdLicence::class.java)

      verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())

      assertThat(licenceCaptor.value.electronicMonitoringProvider)
        .extracting("isToBeTaggedForProgramme", "programmeName")
        .containsExactly(
          true,
          "Programme Name",
        )
    }
  }

  @Nested
  inner class `feature toggle electronicMonitoringResponseHandlingEnabled` {

    @Test
    fun `should handle updated conditions when feature toggle is enabled`() {
      val licenceEntity = aLicenceEntity

      whenever(policyService.isElectronicMonitoringResponseRequired(conditionCodes, licenceEntity.version!!)).thenReturn(true)

      serviceWithFeatureEnabled.handleUpdatedConditionsIfEnabled(licenceEntity, conditionCodes)

      verify(policyService, times(1)).isElectronicMonitoringResponseRequired(conditionCodes, licenceEntity.version!!)
    }

    @Test
    fun `should not handle updated conditions when feature toggle is disabled`() {
      val licenceEntity = aLicenceEntity

      service.handleUpdatedConditionsIfEnabled(licenceEntity, conditionCodes)

      verify(policyService, times(0)).isElectronicMonitoringResponseRequired(any(), any())
    }

    @Test
    fun `should handle removed conditions when feature toggle is enabled`() {
      val licenceEntity = aLicenceEntity

      whenever(policyService.getConditionsRequiringElectronicMonitoringResponse(licenceEntity.version!!, conditionCodes)).thenReturn(
        listOf(policyApCondition),
      )
      whenever(policyService.isElectronicMonitoringResponseRequired(licenceEntity.additionalConditions.map { it.conditionCode }.toSet(), licenceEntity.version!!)).thenReturn(true)

      serviceWithFeatureEnabled.handleRemovedConditionsIfEnabled(licenceEntity, conditionCodes)

      verify(policyService, times(1)).isElectronicMonitoringResponseRequired(licenceEntity.additionalConditions.map { it.conditionCode }.toSet(), licenceEntity.version!!)
    }

    @Test
    fun `should not handle removed conditions when feature toggle is disabled`() {
      val licenceEntity = aLicenceEntity

      whenever(policyService.getConditionsRequiringElectronicMonitoringResponse(licenceEntity.version!!, conditionCodes)).thenReturn(
        listOf(policyApCondition),
      )
      whenever(policyService.isElectronicMonitoringResponseRequired(licenceEntity.additionalConditions.map { it.conditionCode }.toSet(), licenceEntity.version!!)).thenReturn(true)

      service.handleRemovedConditionsIfEnabled(licenceEntity, conditionCodes)

      verify(policyService, times(0)).isElectronicMonitoringResponseRequired(any(), any())
    }
  }

  private companion object {
    val CONDITION_CONFIG = POLICY_V2_1.allAdditionalConditions().first()
    val conditionCodes = setOf("condition1", "condition3", "599bdcae-d545-461c-b1a9-02cb3d4ba268")

    val anInput = Input(
      type = TEXT,
      label = "Label",
      name = "name",
    )

    val policyApCondition = AdditionalConditionAp(
      code = "code",
      category = "599bdcae-d545-461c-b1a9-02cb3d4ba268",
      text = "text",
      inputs = listOf(anInput),
      requiresInput = true,
    )
  }
}
