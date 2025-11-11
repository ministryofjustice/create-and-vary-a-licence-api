package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.stubbing.OngoingStubbing
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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.communityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.LicencePolicyService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.POLICY_V2_1
import java.util.Optional

class ElectronicMonitoringProgrammeServiceTest {
  private val licenceRepository = mock<LicenceRepository>()
  private val policyService = mock<LicencePolicyService>()
  private val auditService = mock<AuditService>()
  private val staffRepository = mock<StaffRepository>()
  private val aLicenceEntity = TestData.createCrdLicence()
  private val aCom = communityOffenderManager()
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

    whenever(
      policyService.getConditionsRequiringElectronicMonitoringResponse(
        aLicenceEntity.version!!,
        conditionCodes,
      ),
    ).thenReturn(
      listOf(policyApCondition),
    )

    SecurityContextHolder.setContext(securityContext)
  }

  @AfterEach
  fun afterEach() {
    reset(licenceRepository, policyService, auditService, staffRepository)
  }

  @Test
  fun `update electronic monitoring programme details`() {
    val crdLicence = aLicenceEntity.copy(
      electronicMonitoringProvider = ElectronicMonitoringProvider(
        isToBeTaggedForProgramme = false,
        programmeName = "Old Programme",
        licence = aLicenceEntity,
      ),
    )

    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(crdLicence))
    whenever(staffRepository.findByUsernameIgnoreCase("tcom")).thenReturn(aCom)

    val request = UpdateElectronicMonitoringProgrammeRequest(
      isToBeTaggedForProgramme = true,
      programmeName = "Programme Name",
    )

    service.updateElectronicMonitoringProgramme(1L, request)

    verify(licenceRepository, times(1)).saveAndFlush(crdLicence)

    assertThat(crdLicence.electronicMonitoringProvider!!.isToBeTaggedForProgramme).isTrue()
    assertThat(crdLicence.electronicMonitoringProvider!!.programmeName).isEqualTo("Programme Name")
  }

  @Test
  fun `Adds EM provider when required`() {
    assertThat(aLicenceEntity.electronicMonitoringProvider).isNull()

    whenCheckingIfElectronicMonitoringProviderIsRequired(aLicenceEntity).thenReturn(true)

    service.createMonitoringProviderIfRequired(aLicenceEntity, conditionCodes)

    assertThat(aLicenceEntity.electronicMonitoringProvider).isNotNull()
  }

  @Test
  fun `Does not add EM provider when not required`() {
    assertThat(aLicenceEntity.electronicMonitoringProvider).isNull()

    whenCheckingIfElectronicMonitoringProviderIsRequired(aLicenceEntity).thenReturn(false)

    service.createMonitoringProviderIfRequired(aLicenceEntity, conditionCodes)

    assertThat(aLicenceEntity.electronicMonitoringProvider).isNull()
  }

  @Test
  fun `Removes EM provider when no longer required`() {
    val licence = aLicenceEntity.copy(
      electronicMonitoringProvider = ElectronicMonitoringProvider(
        isToBeTaggedForProgramme = true,
        programmeName = "Programme Name",
        licence = aLicenceEntity,
      ),
    )

    whenCheckingIfElectronicMonitoringProviderIsRequired(licence).thenReturn(false)

    service.removeMonitoringProviderIfNoLongerRequired(licence, conditionCodes)

    assertThat(licence.electronicMonitoringProvider).isNull()
  }

  @Test
  fun `Does not remove EM provider when still required`() {
    val licence = aLicenceEntity.copy(
      electronicMonitoringProvider = ElectronicMonitoringProvider(
        isToBeTaggedForProgramme = true,
        programmeName = "Programme Name",
        licence = aLicenceEntity,
      ),
    )

    whenCheckingIfElectronicMonitoringProviderIsRequired(licence).thenReturn(true)

    service.removeMonitoringProviderIfNoLongerRequired(licence, conditionCodes)

    assertThat(licence.electronicMonitoringProvider).isNull()
  }

  private fun whenCheckingIfElectronicMonitoringProviderIsRequired(licenceEntity: CrdLicence): OngoingStubbing<Boolean> = whenever(
    policyService.isElectronicMonitoringResponseRequired(
      conditionCodes,
      licenceEntity.version!!,
    ),
  )

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
