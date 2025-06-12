package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions

import org.assertj.core.api.Assertions.assertThat
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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateElectronicMonitoringProgrammeRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.ElectronicMonitoringProviderRepository
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
  private val electronicMonitoringProviderRepository = mock<ElectronicMonitoringProviderRepository>()

  private val service = ElectronicMonitoringProgrammeService(
    licenceRepository,
    policyService,
    auditService,
    staffRepository,
    electronicMonitoringProviderRepository,
  )

  @BeforeEach
  fun reset() {
    val authentication = mock<Authentication>()
    val securityContext = mock<SecurityContext>()
    whenever(authentication.name).thenReturn("tcom")
    whenever(securityContext.authentication).thenReturn(authentication)
    whenever(policyService.getConfigForCondition(any(), any())).thenReturn(CONDITION_CONFIG)

    SecurityContextHolder.setContext(securityContext)

    reset(
      licenceRepository,
      staffRepository,
      electronicMonitoringProviderRepository,
    )
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
    fun `should handle response when feature toggle is enabled`() {
      val licenceEntity = aLicenceEntity

      whenever(policyService.isElectronicMonitoringResponseRequired(any())).thenReturn(true)
      whenever(electronicMonitoringProviderRepository.saveAndFlush(any()))
        .thenReturn(ElectronicMonitoringProvider(licence = aLicenceEntity))

      // Enable the feature toggle
      val serviceWithFeatureEnabled = ElectronicMonitoringProgrammeService(
        licenceRepository,
        policyService,
        auditService,
        staffRepository,
        electronicMonitoringProviderRepository,
        electronicMonitoringResponseHandlingEnabled = true,
      )

      serviceWithFeatureEnabled.handleResponseIfEnabled(licenceEntity)

      verify(policyService, times(1)).isElectronicMonitoringResponseRequired(licenceEntity)
      verify(electronicMonitoringProviderRepository, times(1)).saveAndFlush(any())
    }

    @Test
    fun `should not handle response when feature toggle is disabled`() {
      val licenceEntity = aLicenceEntity

      // Disable the feature toggle
      val serviceWithFeatureDisabled = ElectronicMonitoringProgrammeService(
        licenceRepository,
        policyService,
        auditService,
        staffRepository,
        electronicMonitoringProviderRepository,
        electronicMonitoringResponseHandlingEnabled = false,
      )

      serviceWithFeatureDisabled.handleResponseIfEnabled(licenceEntity)

      verify(policyService, times(0)).isElectronicMonitoringResponseRequired(any())
      verify(electronicMonitoringProviderRepository, times(0)).saveAndFlush(any())
    }
  }

  private companion object {
    val CONDITION_CONFIG = POLICY_V2_1.allAdditionalConditions().first()

    val aLicenceEntity = TestData.createCrdLicence()

    val aCom = TestData.com()
  }
}
