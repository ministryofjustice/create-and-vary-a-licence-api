package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple.tuple
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.BespokeCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalConditionData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalConditionRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalConditionsRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.BespokeConditionRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.StandardCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.UpdateAdditionalConditionDataRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.UpdateStandardConditionDataRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.AdditionalConditionAp
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.AdditionalConditions
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.AllAdditionalConditions
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.LicencePolicy
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.StandardConditions
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.AddAdditionalConditionRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.DeleteAdditionalConditionsByCodeRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.BespokeConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StaffRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.AuditService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.anAdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.anotherCommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.communityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.LicencePolicyService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.POLICY_V2_1
import java.util.Optional
import java.util.UUID
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionData as EntityAdditionalConditionData

class LicenceConditionServiceTest {
  private val licenceRepository = mock<LicenceRepository>()
  private val additionalConditionRepository = mock<AdditionalConditionRepository>()
  private val bespokeConditionRepository = mock<BespokeConditionRepository>()
  private val policyService = mock<LicencePolicyService>()
  private val conditionFormatter = mock<ConditionFormatter>()
  private val auditService = mock<AuditService>()
  private val staffRepository = mock<StaffRepository>()
  private val electronicMonitoringProgrammeService = mock<ElectronicMonitoringProgrammeService>()
  private val exclusionZoneService = mock<ExclusionZoneService>()

  private val service = LicenceConditionService(
    licenceRepository,
    additionalConditionRepository,
    bespokeConditionRepository,
    conditionFormatter,
    policyService,
    auditService,
    staffRepository,
    electronicMonitoringProgrammeService,
    exclusionZoneService,
  )

  @BeforeEach
  fun reset() {
    val authentication = mock<Authentication>()
    val securityContext = mock<SecurityContext>()
    whenever(authentication.name).thenReturn("tcom")
    whenever(securityContext.authentication).thenReturn(authentication)
    whenever(policyService.getConfigForCondition(any(), any())).thenReturn(CONDITION_CONFIG)
    whenever(conditionFormatter.format(any(), any())).thenReturn("expanded text")

    SecurityContextHolder.setContext(securityContext)

    reset(
      licenceRepository,
      additionalConditionRepository,
      bespokeConditionRepository,
      staffRepository,
      exclusionZoneService,
    )
  }

  @Nested
  inner class `update standard conditions` {
    @Test
    fun `update standard conditions for an individual licence`() {
      whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))
      whenever(policyService.currentPolicy(any())).thenReturn(aPolicy)
      whenever(staffRepository.findByUsernameIgnoreCase("tcom")).thenReturn(aCom)

      val apConditions = listOf(
        StandardCondition(id = 1, code = "goodBehaviour", sequence = 1, text = "Be of good behaviour"),
      )

      val pssConditions = listOf(
        StandardCondition(id = 2, code = "goodBehaviour", sequence = 1, text = "Be of good behaviour"),
        StandardCondition(id = 3, code = "doNotBreakLaw", sequence = 2, text = "Do not break any law"),
      )

      service.updateStandardConditions(
        1,
        UpdateStandardConditionDataRequest(
          standardLicenceConditions = apConditions,
          standardPssConditions = pssConditions,
        ),
      )

      val licenceCaptor = ArgumentCaptor.forClass(Licence::class.java)

      verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
      verify(auditService, times(1)).recordAuditEventUpdateStandardCondition(any(), any(), any())

      assertThat(licenceCaptor.value)
        .extracting("updatedByUsername", "updatedBy")
        .isEqualTo(listOf(aCom.username, aCom))

      assertThat(licenceCaptor.value.standardConditions).containsExactly(
        uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.StandardCondition(
          conditionCode = "goodBehaviour",
          conditionSequence = 1,
          conditionText = "Be of good behaviour",
          conditionType = "AP",
          licence = aLicenceEntity,
        ),
        uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.StandardCondition(
          conditionCode = "goodBehaviour",
          conditionSequence = 1,
          conditionText = "Be of good behaviour",
          conditionType = "PSS",
          licence = aLicenceEntity,
        ),
        uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.StandardCondition(
          conditionCode = "doNotBreakLaw",
          conditionSequence = 2,
          conditionText = "Do not break any law",
          conditionType = "PSS",
          licence = aLicenceEntity,
        ),
      )
    }
  }

  @Nested
  inner class `deleting additional conditions` {
    @Test
    fun `delete one additional condition`() {
      whenever(licenceRepository.findById(1L))
        .thenReturn(
          Optional.of(
            aLicenceEntity.copy(
              additionalConditions = listOf(
                additionalCondition(1),
                additionalCondition(2),
                additionalCondition(3),
              ),
            ),
          ),
        )
      whenever(staffRepository.findByUsernameIgnoreCase("tcom")).thenReturn(aCom)

      service.deleteAdditionalCondition(1L, 2)

      val licenceCaptor = ArgumentCaptor.forClass(Licence::class.java)

      verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
      verify(auditService, times(1)).recordAuditEventDeleteAdditionalConditions(any(), any(), any())
      verify(exclusionZoneService, times(1)).deleteDocuments(any())

      assertThat(licenceCaptor.value.additionalConditions).containsExactly(
        additionalCondition(1),
        additionalCondition(3),
      )

      // Verify last contact info is recorded
      assertThat(licenceCaptor.value)
        .extracting("updatedByUsername", "updatedBy")
        .isEqualTo(listOf(aCom.username, aCom))
    }

    @Test
    fun `delete multiple conditions`() {
      val licenceEntity = aLicenceEntity.copy(
        additionalConditions = listOf(
          additionalCondition(1),
          additionalCondition(2),
          additionalCondition(3),
        ),
        standardConditions = listOf(
          standardCondition(1).copy(conditionType = "AP"),
          standardCondition(2).copy(conditionType = "AP"),
          standardCondition(3).copy(conditionType = "PSS"),
        ),
        bespokeConditions = listOf(
          BespokeCondition(1, licence = aLicenceEntity, conditionText = "condition 1"),
          BespokeCondition(2, licence = aLicenceEntity, conditionText = "condition 2"),
          BespokeCondition(3, licence = aLicenceEntity, conditionText = "condition 3"),
        ),
      )

      whenever(staffRepository.findByUsernameIgnoreCase("tcom")).thenReturn(aCom)

      service.deleteConditions(licenceEntity, listOf(2, 3), listOf(1, 2), listOf(1, 3))

      val licenceCaptor = ArgumentCaptor.forClass(Licence::class.java)

      verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())

      verify(exclusionZoneService, times(1)).deleteDocuments(any())

      assertThat(licenceCaptor.value.additionalConditions).containsExactly(
        additionalCondition(1),
      )

      assertThat(licenceCaptor.value.standardConditions).containsExactly(
        standardCondition(3).copy(conditionType = "PSS"),
      )

      assertThat(licenceCaptor.value.bespokeConditions).containsExactly(
        BespokeCondition(2, licence = aLicenceEntity, conditionText = "condition 2"),
      )

      // Verify last contact info is recorded
      assertThat(licenceCaptor.value)
        .extracting("updatedByUsername", "updatedBy")
        .isEqualTo(listOf(aCom.username, aCom))
    }

    @Test
    fun `deleting multiple conditions is a noop if no conditions provided`() {
      service.deleteConditions(aLicenceEntity, emptyList(), emptyList(), emptyList())

      verifyNoInteractions(licenceRepository)
      verifyNoInteractions(staffRepository)
    }

    @Test
    fun `delete single additional condition by code`() {
      whenever(licenceRepository.findById(1L))
        .thenReturn(
          Optional.of(
            aLicenceEntity.copy(
              additionalConditions = listOf(
                additionalCondition(1, "code1"),
                additionalCondition(2, "code2"),
                additionalCondition(3, "code3"),
              ),
            ),
          ),
        )
      whenever(staffRepository.findByUsernameIgnoreCase("tcom")).thenReturn(aCom)

      service.deleteAdditionalConditionsByCode(1L, DeleteAdditionalConditionsByCodeRequest(listOf("code2")))

      val licenceCaptor = ArgumentCaptor.forClass(Licence::class.java)

      verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
      verify(auditService, times(1)).recordAuditEventDeleteAdditionalConditions(any(), any(), any())

      assertThat(licenceCaptor.value.additionalConditions).containsExactly(
        additionalCondition(1, "code1"),
        additionalCondition(3, "code3"),
      )

      // Verify last contact info is recorded
      assertThat(licenceCaptor.value)
        .extracting("updatedByUsername", "updatedBy")
        .isEqualTo(listOf(aCom.username, aCom))
    }

    @Test
    fun `delete multiple additional conditions by code`() {
      whenever(licenceRepository.findById(1L))
        .thenReturn(
          Optional.of(
            aLicenceEntity.copy(
              additionalConditions = listOf(
                additionalCondition(1, "code1"),
                additionalCondition(2, "code2"),
                additionalCondition(3, "code3"),
              ),
            ),
          ),
        )
      whenever(staffRepository.findByUsernameIgnoreCase("tcom")).thenReturn(aCom)

      service.deleteAdditionalConditionsByCode(1L, DeleteAdditionalConditionsByCodeRequest(listOf("code2", "code1")))

      val licenceCaptor = ArgumentCaptor.forClass(Licence::class.java)

      verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
      verify(auditService, times(1)).recordAuditEventDeleteAdditionalConditions(any(), any(), any())

      assertThat(licenceCaptor.value.additionalConditions).containsExactly(
        additionalCondition(3, "code3"),
      )

      // Verify last contact info is recorded
      assertThat(licenceCaptor.value)
        .extracting("updatedByUsername", "updatedBy")
        .isEqualTo(listOf(aCom.username, aCom))
    }
  }

  @Nested
  inner class `update additional conditions` {
    @Test
    fun `update additional conditions throws not found exception`() {
      whenever(licenceRepository.findById(1L)).thenReturn(Optional.empty())

      val exception = assertThrows<EntityNotFoundException> {
        service.updateAdditionalConditions(
          1L,
          AdditionalConditionsRequest(
            additionalConditions = listOf(
              AdditionalConditionRequest(
                code = "code",
                category = "category",
                text = "text",
                sequence = 0,
              ),
            ),
            conditionType = "AP",
          ),
        )
      }

      assertThat(exception).isInstanceOf(EntityNotFoundException::class.java)

      verify(licenceRepository, times(1)).findById(1L)
      verify(licenceRepository, times(0)).saveAndFlush(any())
      verifyNoInteractions(exclusionZoneService)
      verifyNoInteractions(staffRepository)
    }

    /**
     * In reality the update method updates, adds and removes conditions using a list
     * of submitted condition codes. This process can be improved once policy documents are
     * migrated form the Node app to this project.
     */
    @Test
    fun `update additional conditions`() {
      val expectedToBeRemoved =
        additionalCondition(2).copy(conditionSequence = 6, conditionCode = "code2", conditionType = "AP")

      whenever(licenceRepository.findById(1L))
        .thenReturn(
          Optional.of(
            aLicenceEntity.copy(
              additionalConditions = listOf(
                additionalCondition(1).copy(
                  conditionSequence = 5,
                  conditionCode = "code",
                  conditionCategory = "oldCategory",
                  conditionText = "oldText",
                  conditionType = "AP",
                ),
                expectedToBeRemoved,
                additionalCondition(3).copy(conditionSequence = 7, conditionCode = "code3", conditionType = "PSS"),
              ),
            ),
          ),
        )
      whenever(staffRepository.findByUsernameIgnoreCase("tcom")).thenReturn(aCom)
      val deletedUUIDS = listOf(UUID.randomUUID())
      whenever(exclusionZoneService.getDeletableDocumentUuids(listOf(expectedToBeRemoved))).thenReturn(deletedUUIDS)

      val request = AdditionalConditionsRequest(
        additionalConditions = listOf(
          AdditionalConditionRequest(
            code = "code",
            category = "category",
            text = "text",
            sequence = 0,
          ),
        ),
        conditionType = "AP",
      )

      service.updateAdditionalConditions(1L, request)

      val licenceCaptor = ArgumentCaptor.forClass(Licence::class.java)

      verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
      verify(auditService, times(1)).recordAuditEventUpdateAdditionalConditions(any(), any(), any(), any())
      verify(exclusionZoneService, times(1)).getDeletableDocumentUuids(listOf(expectedToBeRemoved))
      verify(exclusionZoneService, times(1)).deleteDocuments(deletedUUIDS)

      assertThat(licenceCaptor.value.additionalConditions).containsExactly(
        additionalCondition(1).copy(
          conditionSequence = 0,
          conditionCode = "code",
          conditionType = "AP",
          conditionCategory = "category",
          conditionText = "text",
        ),
        additionalCondition(3).copy(conditionSequence = 7, conditionCode = "code3", conditionType = "PSS"),
      )

      // Verify last contact info is recorded
      assertThat(licenceCaptor.value)
        .extracting("updatedByUsername", "updatedBy")
        .isEqualTo(listOf(aCom.username, aCom))

      verifyNoInteractions(conditionFormatter)
    }

    @Test
    fun `handleUpdatedConditionsIfEnabled is called only for CRD and HDC licences`() {
      val crdLicence = TestData.createCrdLicence()
      val hdcLicence = TestData.createHdcLicence()
      val variationLicence = TestData.createVariationLicence()

      whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(crdLicence))
      whenever(licenceRepository.findById(2L)).thenReturn(Optional.of(hdcLicence))
      whenever(licenceRepository.findById(3L)).thenReturn(Optional.of(variationLicence))

      val request = AdditionalConditionsRequest(
        additionalConditions = listOf(
          AdditionalConditionRequest(code = "code", category = "category", text = "text", sequence = 0),
        ),
        conditionType = "AP",
      )

      service.updateAdditionalConditions(1L, request)
      service.updateAdditionalConditions(2L, request)
      service.updateAdditionalConditions(3L, request)

      verify(electronicMonitoringProgrammeService, times(1)).createMonitoringProviderIfRequired(
        crdLicence,
        setOf("code"),
      )
      verify(electronicMonitoringProgrammeService, times(1)).createMonitoringProviderIfRequired(
        hdcLicence,
        setOf("code"),
      )
      verify(electronicMonitoringProgrammeService, times(0)).createMonitoringProviderIfRequired(
        variationLicence,
        setOf("code"),
      )
    }
  }

  @Nested
  inner class `add additional conditions` {
    @Test
    fun `update additional conditions`() {
      val licence = aLicenceEntity.copy(
        additionalConditions = listOf(
          additionalCondition(1),
          additionalCondition(
            3,
            conditionCode = "code3",
            conditionSequence = 6,
            additionalConditionData = someDifferentAdditionalConditionData,
          ),
        ),
      )

      whenever(licenceRepository.findById(1L))
        .thenReturn(Optional.of(licence))

      whenever(policyService.getAllAdditionalConditions()).thenReturn(
        AllAdditionalConditions(mapOf("1.0" to mapOf(policyApCondition.code to policyApCondition))),
      )

      whenever(staffRepository.findByUsernameIgnoreCase("tcom")).thenReturn(aCom)

      val request = AddAdditionalConditionRequest(
        conditionCode = "code",
        conditionCategory = "category",
        conditionText = "text",
        sequence = 7,
        conditionType = "AP",
        expandedText = "Hello",
      )

      service.addAdditionalCondition(1L, request)

      val licenceCaptor = ArgumentCaptor.forClass(Licence::class.java)

      verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
      verify(auditService, times(1)).recordAuditEventAddAdditionalConditionOfSameType(any(), any(), any())

      assertThat(licenceCaptor.value.additionalConditions)
        .extracting("id", "conditionCode", "conditionSequence")
        .containsExactly(
          tuple(1L, "code", 5),
          tuple(3L, "code3", 6),
          tuple(null, "code", 7),
        )

      // Verify last contact info is recorded
      assertThat(licenceCaptor.value)
        .extracting("updatedByUsername", "updatedBy")
        .isEqualTo(listOf(aCom.username, aCom))

      // No way of providing additional condition data via this endpoint so no point running through formatter
      verifyNoInteractions(conditionFormatter)
    }
  }

  private fun additionalCondition(
    id: Long? = 1,
    conditionCode: String = "code",
    conditionSequence: Int = 5,
    additionalConditionData: MutableList<EntityAdditionalConditionData> = someAdditionalConditionData,
  ) = AdditionalCondition(
    id = id,
    conditionVersion = "1.0",
    conditionCode = conditionCode,
    conditionSequence = conditionSequence,
    conditionCategory = "oldCategory",
    conditionText = "oldText",
    additionalConditionData = additionalConditionData,
    licence = aLicenceEntity,
    conditionType = "AP",
  )

  @Nested
  inner class `update bespoke conditions` {
    @Test
    fun `update bespoke conditions persists multiple entities`() {
      whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))
      whenever(staffRepository.findByUsernameIgnoreCase("tcom")).thenReturn(aCom)

      val bespokeEntities = listOf(
        BespokeCondition(licence = aLicenceEntity, conditionSequence = 1, conditionText = "Condition 2"),
        BespokeCondition(licence = aLicenceEntity, conditionSequence = 2, conditionText = "Condition 3"),
        BespokeCondition(licence = aLicenceEntity, conditionSequence = 0, conditionText = "Condition 1"),
      )

      bespokeEntities.forEach { bespoke ->
        whenever(bespokeConditionRepository.saveAndFlush(bespoke)).thenReturn(bespoke)
      }

      service.updateBespokeConditions(1L, someBespokeConditions)

      // Verify licence entity is updated with last contact info
      val licenceCaptor = ArgumentCaptor.forClass(Licence::class.java)

      verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
      verify(auditService, times(1)).recordAuditEventUpdateBespokeConditions(any(), any(), any(), any())

      assertThat(licenceCaptor.value)
        .extracting("bespokeConditions", "updatedByUsername", "updatedBy")
        .isEqualTo(listOf(emptyList<BespokeCondition>(), aCom.username, aCom))

      // Verify new bespoke conditions are added in their place
      bespokeEntities.forEach { bespoke ->
        verify(bespokeConditionRepository, times(1)).saveAndFlush(bespoke)
      }
    }

    @Test
    fun `update bespoke conditions with an empty list - removes previously persisted entities`() {
      val bespokeEntities = listOf(
        BespokeCondition(id = -1L, licence = aLicenceEntity, conditionSequence = 1, conditionText = "Condition 2"),
        BespokeCondition(id = -1L, licence = aLicenceEntity, conditionSequence = 2, conditionText = "Condition 3"),
        BespokeCondition(id = -1L, licence = aLicenceEntity, conditionSequence = 0, conditionText = "Condition 1"),
      )

      whenever(licenceRepository.findById(1L)).thenReturn(
        Optional.of(
          aLicenceEntity.copy(
            bespokeConditions = bespokeEntities,
          ),
        ),
      )
      whenever(staffRepository.findByUsernameIgnoreCase("tcom")).thenReturn(aCom)

      service.updateBespokeConditions(1L, BespokeConditionRequest())

      val licenceCaptor = ArgumentCaptor.forClass(Licence::class.java)

      verify(bespokeConditionRepository, times(0)).saveAndFlush(any())
      verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
      verify(auditService, times(1)).recordAuditEventUpdateBespokeConditions(any(), any(), any(), any())

      assertThat(licenceCaptor.value)
        .extracting("updatedByUsername", "updatedBy")
        .isEqualTo(listOf(aCom.username, aCom))
    }

    @Test
    fun `update bespoke conditions throws not found exception if licence not found`() {
      whenever(licenceRepository.findById(1L)).thenReturn(Optional.empty())

      val exception = assertThrows<EntityNotFoundException> {
        service.updateBespokeConditions(1L, someBespokeConditions)
      }

      assertThat(exception).isInstanceOf(EntityNotFoundException::class.java)

      verify(licenceRepository, times(1)).findById(1L)
      verify(bespokeConditionRepository, times(0)).saveAndFlush(any())
      verifyNoInteractions(staffRepository)
    }
  }

  @Nested
  inner class `update additional condition data` {

    @Test
    fun `update additional condition data throws not found exception if licence is not found`() {
      whenever(licenceRepository.findById(1L)).thenReturn(Optional.empty())

      val exception = assertThrows<EntityNotFoundException> {
        service.updateAdditionalConditionData(
          1L,
          1L,
          UpdateAdditionalConditionDataRequest(
            data = listOf(
              AdditionalConditionData(
                field = "field1",
                value = "value1",
                sequence = 0,
              ),
            ),
          ),
        )
      }

      assertThat(exception).isInstanceOf(EntityNotFoundException::class.java)

      verify(licenceRepository, times(1)).findById(1L)
      verify(licenceRepository, times(0)).saveAndFlush(any())
      verifyNoInteractions(staffRepository)
    }

    @Test
    fun `update additional condition data throws not found exception if condition is not found`() {
      whenever(licenceRepository.findById(1L))
        .thenReturn(
          Optional.of(
            aLicenceEntity.copy(
              additionalConditions = listOf(
                additionalCondition(1),
              ),
            ),
          ),
        )

      whenever(additionalConditionRepository.findById(1L)).thenReturn(Optional.empty())

      val exception = assertThrows<EntityNotFoundException> {
        service.updateAdditionalConditionData(
          1L,
          1L,
          UpdateAdditionalConditionDataRequest(
            data = listOf(
              AdditionalConditionData(
                id = 1,
                field = "field1",
                value = "value1",
                sequence = 0,
              ),
            ),
          ),
        )
      }

      assertThat(exception).isInstanceOf(EntityNotFoundException::class.java)

      verify(licenceRepository, times(1)).findById(1L)
      verify(licenceRepository, times(0)).saveAndFlush(any())
      verifyNoInteractions(staffRepository)
    }

    @Test
    fun `update additional condition data`() {
      // Given

      val additionalCondition = additionalCondition(1)

      val licence = aLicenceEntity.copy(additionalConditions = listOf(additionalCondition))
      whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(licence))
      whenever(additionalConditionRepository.findById(1L)).thenReturn(Optional.of(additionalCondition))
      whenever(staffRepository.findByUsernameIgnoreCase("tcom")).thenReturn(aCom)

      val additionalConditionDataRequest = AdditionalConditionData(
        id = 1,
        field = "field1",
        value = "value1",
        sequence = 0,
      )
      val request = UpdateAdditionalConditionDataRequest(
        data = listOf(additionalConditionDataRequest),
      )

      // When
      service.updateAdditionalConditionData(1L, 1L, request)

      // Then
      verify(auditService, times(1)).recordAuditEventUpdateAdditionalConditionData(
        licence,
        additionalCondition,
        aCom,
      )
      verify(staffRepository, times(1)).findByUsernameIgnoreCase("tcom")
      verify(conditionFormatter, times(1)).format(CONDITION_CONFIG, additionalCondition.additionalConditionData)

      assertThat(additionalCondition.conditionVersion).isEqualTo(licence.version)
      assertThat(additionalCondition.expandedConditionText).isEqualTo("expanded text")
      val additionalConditionData = additionalCondition.additionalConditionData.first()
      assertThat(additionalConditionData.dataField).isEqualTo("field1")
      assertThat(additionalConditionData.dataValue).isEqualTo("value1")
      assertThat(additionalConditionData.additionalCondition).isEqualTo(additionalCondition)
    }
  }

  @Nested
  inner class `update username and updatedBy` {
    @Test
    fun `updating user is retained and username is set to SYSTEM_USER when a staff member cannot be found`() {
      whenever(licenceRepository.findById(1L)).thenReturn(
        Optional.of(
          aLicenceEntity.copy(
            updatedBy = aPreviousUser,
          ),
        ),
      )
      whenever(staffRepository.findByUsernameIgnoreCase("tcom")).thenReturn(null)

      val bespokeEntities = listOf(
        BespokeCondition(licence = aLicenceEntity, conditionSequence = 1, conditionText = "Condition 2"),
        BespokeCondition(licence = aLicenceEntity, conditionSequence = 2, conditionText = "Condition 3"),
        BespokeCondition(licence = aLicenceEntity, conditionSequence = 0, conditionText = "Condition 1"),
      )

      bespokeEntities.forEach { bespoke ->
        whenever(bespokeConditionRepository.saveAndFlush(bespoke)).thenReturn(bespoke)
      }

      service.updateBespokeConditions(1L, someBespokeConditions)

      // Verify licence entity is updated with last contact info
      val licenceCaptor = ArgumentCaptor.forClass(Licence::class.java)

      verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
      verify(auditService, times(1)).recordAuditEventUpdateBespokeConditions(any(), any(), any(), anyOrNull())

      assertThat(licenceCaptor.value)
        .extracting("updatedByUsername", "updatedBy")
        .isEqualTo(listOf(Licence.SYSTEM_USER, aPreviousUser))
      assertThat(licenceCaptor.value).extracting("bespokeConditions")
        .isEqualTo(emptyList<BespokeCondition>())

      // Verify new bespoke conditions are added in their place
      bespokeEntities.forEach { bespoke ->
        verify(bespokeConditionRepository, times(1)).saveAndFlush(bespoke)
      }
    }
  }

  private companion object {
    val CONDITION_CONFIG = POLICY_V2_1.allAdditionalConditions().first()

    val aLicenceEntity = TestData.createCrdLicence()

    val someAdditionalConditionData = mutableListOf(
      EntityAdditionalConditionData(
        id = 1,
        dataField = "dataField",
        dataValue = "dataValue",
        additionalCondition = anAdditionalCondition(id = 1, licence = aLicenceEntity),
      ),
    )

    val someDifferentAdditionalConditionData = mutableListOf(
      EntityAdditionalConditionData(
        id = 2,
        dataField = "dataField",
        dataValue = "dataValue2",
        additionalCondition = anAdditionalCondition(id = 2, licence = aLicenceEntity),
      ),
    )

    val someBespokeConditions =
      BespokeConditionRequest(conditions = listOf("Condition 1", "Condition 2", "Condition 3"))

    val aPolicy = LicencePolicy(
      "2.1",
      standardConditions = StandardConditions(emptyList(), emptyList()),
      additionalConditions = AdditionalConditions(emptyList(), emptyList()),
      changeHints = emptyList(),
    )

    val policyApCondition = AdditionalConditionAp(
      code = "code",
      category = "category",
      text = "text",
      requiresInput = false,
    )

    val aCom = communityOffenderManager()

    val aPreviousUser = anotherCommunityOffenderManager()
  }

  private fun standardCondition(id: Long) = uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.StandardCondition(
    id = id,
    conditionCode = "goodBehaviour",
    conditionSequence = id.toInt(),
    conditionText = "Be of good behaviour",
    conditionType = "AP",
    licence = aLicenceEntity,
  )
}
