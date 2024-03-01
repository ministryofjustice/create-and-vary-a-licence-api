package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple.tuple
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.BespokeCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence.Companion.SYSTEM_USER
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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionUploadDetailRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.BespokeConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StaffRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.POLICY_V2_1
import java.util.Optional

class LicenceConditionServiceTest {
  private val licenceRepository = mock<LicenceRepository>()
  private val additionalConditionRepository = mock<AdditionalConditionRepository>()
  private val bespokeConditionRepository = mock<BespokeConditionRepository>()
  private val additionalConditionUploadDetailRepository = mock<AdditionalConditionUploadDetailRepository>()
  private val policyService = mock<LicencePolicyService>()
  private val conditionFormatter = mock<ConditionFormatter>()
  private val auditService = mock<AuditService>()
  private val staffRepository = mock<StaffRepository>()

  private val service = LicenceConditionService(
    licenceRepository,
    additionalConditionRepository,
    bespokeConditionRepository,
    additionalConditionUploadDetailRepository,
    conditionFormatter,
    policyService,
    auditService,
    staffRepository,
  )

  @BeforeEach
  fun reset() {
    val authentication = mock<Authentication>()
    val securityContext = mock<SecurityContext>()
    whenever(authentication.name).thenReturn("smills")
    whenever(securityContext.authentication).thenReturn(authentication)
    whenever(policyService.getConfigForCondition(any(), any())).thenReturn(CONDITION_CONFIG)
    whenever(conditionFormatter.format(any(), any())).thenReturn("expanded text")

    SecurityContextHolder.setContext(securityContext)

    reset(
      licenceRepository,
      additionalConditionRepository,
      bespokeConditionRepository,
      additionalConditionUploadDetailRepository,
      staffRepository,
    )
  }

  @Nested
  inner class `update standard conditions` {
    @Test
    fun `update standard conditions for an individual licence`() {
      whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))
      whenever(policyService.currentPolicy()).thenReturn(aPolicy)
      whenever(staffRepository.findByUsernameIgnoreCase("smills")).thenReturn(aCom)

      val apConditions = listOf(
        StandardCondition(code = "goodBehaviour", sequence = 1, text = "Be of good behaviour"),
      )

      val pssConditions = listOf(
        StandardCondition(code = "goodBehaviour", sequence = 1, text = "Be of good behaviour"),
        StandardCondition(code = "doNotBreakLaw", sequence = 2, text = "Do not break any law"),
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
      verify(auditService, times(1)).recordAuditEventUpdateStandardCondition(any(), any())

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
      whenever(staffRepository.findByUsernameIgnoreCase("smills")).thenReturn(aCom)

      service.deleteAdditionalCondition(1L, 2)

      val licenceCaptor = ArgumentCaptor.forClass(Licence::class.java)

      verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
      verify(auditService, times(1)).recordAuditEventDeleteAdditionalConditions(any(), any())

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
          BespokeCondition(1, licence = aLicenceEntity).copy(conditionText = "condition 1"),
          BespokeCondition(2, licence = aLicenceEntity).copy(conditionText = "condition 2"),
          BespokeCondition(3, licence = aLicenceEntity).copy(conditionText = "condition 3"),
        ),
      )

      whenever(staffRepository.findByUsernameIgnoreCase("smills")).thenReturn(aCom)

      service.deleteConditions(licenceEntity, listOf(2, 3), listOf(1, 2), listOf(1, 3))

      val licenceCaptor = ArgumentCaptor.forClass(Licence::class.java)

      verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())

      assertThat(licenceCaptor.value.additionalConditions).containsExactly(
        additionalCondition(1),
      )

      assertThat(licenceCaptor.value.standardConditions).containsExactly(
        standardCondition(3).copy(conditionType = "PSS"),
      )

      assertThat(licenceCaptor.value.bespokeConditions).containsExactly(
        BespokeCondition(2, licence = aLicenceEntity).copy(conditionText = "condition 2"),
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
      verifyNoInteractions(staffRepository)
    }

    /**
     * In reality the update method updates, adds and removes conditions using a list
     * of submitted condition codes. This process can be improved once policy documents are
     * migrated form the Node app to this project.
     */
    @Test
    fun `update additional conditions`() {
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
                additionalCondition(2).copy(conditionSequence = 6, conditionCode = "code2", conditionType = "AP"),
                additionalCondition(3).copy(conditionSequence = 7, conditionCode = "code3", conditionType = "PSS"),
              ),
            ),
          ),
        )
      whenever(staffRepository.findByUsernameIgnoreCase("smills")).thenReturn(aCom)

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
      verify(auditService, times(1)).recordAuditEventUpdateAdditionalConditions(any(), any(), any())

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
  }

  @Nested
  inner class `add additional conditions` {
    @Test
    fun `update additional conditions`() {
      whenever(licenceRepository.findById(1L))
        .thenReturn(
          Optional.of(
            aLicenceEntity.copy(
              additionalConditions = listOf(
                additionalCondition(1),
                additionalCondition(3).copy(
                  conditionCode = "code3",
                  conditionSequence = 6,
                  additionalConditionData = someDifferentAdditionalConditionData,
                ),
              ),
            ),
          ),
        )

      whenever(policyService.getAllAdditionalConditions()).thenReturn(
        AllAdditionalConditions(mapOf("1.0" to mapOf(policyApCondition.code to policyApCondition))),
      )

      whenever(staffRepository.findByUsernameIgnoreCase("smills")).thenReturn(aCom)

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
      verify(auditService, times(1)).recordAuditEventAddAdditionalConditionOfSameType(any(), any())

      assertThat(licenceCaptor.value.additionalConditions).extracting("id", "conditionCode", "conditionSequence")
        .containsExactly(
          tuple(1L, "code", 5),
          tuple(3L, "code3", 6),
          tuple(-1L, "code", 7),
        )

      // Verify last contact info is recorded
      assertThat(licenceCaptor.value)
        .extracting("updatedByUsername", "updatedBy")
        .isEqualTo(listOf(aCom.username, aCom))

      // No way of providing additional condition data via this endpoint so no point running through formatter
      verifyNoInteractions(conditionFormatter)
    }
  }

  private fun additionalCondition(id: Long) = AdditionalCondition(
    id = id,
    conditionVersion = "1.0",
    conditionCode = "code",
    conditionSequence = 5,
    conditionCategory = "oldCategory",
    conditionText = "oldText",
    additionalConditionData = someAdditionalConditionData,
    licence = aLicenceEntity,
    conditionType = "AP",
  )

  @Nested
  inner class `update bespoke conditions` {
    @Test
    fun `update bespoke conditions persists multiple entities`() {
      whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))
      whenever(staffRepository.findByUsernameIgnoreCase("smills")).thenReturn(aCom)

      val bespokeEntities = listOf(
        BespokeCondition(id = -1L, licence = aLicenceEntity, conditionSequence = 1, conditionText = "Condition 2"),
        BespokeCondition(id = -1L, licence = aLicenceEntity, conditionSequence = 2, conditionText = "Condition 3"),
        BespokeCondition(id = -1L, licence = aLicenceEntity, conditionSequence = 0, conditionText = "Condition 1"),
      )

      bespokeEntities.forEach { bespoke ->
        whenever(bespokeConditionRepository.saveAndFlush(bespoke)).thenReturn(bespoke)
      }

      service.updateBespokeConditions(1L, someBespokeConditions)

      // Verify licence entity is updated with last contact info
      val licenceCaptor = ArgumentCaptor.forClass(Licence::class.java)

      verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
      verify(auditService, times(1)).recordAuditEventUpdateBespokeConditions(any(), any(), any())

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
      whenever(staffRepository.findByUsernameIgnoreCase("smills")).thenReturn(aCom)

      service.updateBespokeConditions(1L, BespokeConditionRequest())

      val licenceCaptor = ArgumentCaptor.forClass(Licence::class.java)

      verify(bespokeConditionRepository, times(0)).saveAndFlush(any())
      verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
      verify(auditService, times(1)).recordAuditEventUpdateBespokeConditions(any(), any(), any())

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
              uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalConditionData(
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
              uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalConditionData(
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
      whenever(additionalConditionRepository.findById(1L))
        .thenReturn(
          Optional.of(
            anAdditionalConditionEntity.copy(),
          ),
        )
      whenever(staffRepository.findByUsernameIgnoreCase("smills")).thenReturn(aCom)

      val request = UpdateAdditionalConditionDataRequest(
        data = listOf(
          uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalConditionData(
            field = "field1",
            value = "value1",
            sequence = 0,
          ),
        ),
      )

      service.updateAdditionalConditionData(1L, 1L, request)

      val conditionCaptor = ArgumentCaptor.forClass(AdditionalCondition::class.java)
      val licenceCaptor = ArgumentCaptor.forClass(Licence::class.java)

      verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
      verify(auditService, times(1)).recordAuditEventUpdateAdditionalConditionData(any(), any())

      verify(additionalConditionRepository, times(1)).saveAndFlush(conditionCaptor.capture())

      assertThat(conditionCaptor.value.expandedConditionText).isEqualTo("expanded text")
      assertThat(conditionCaptor.value.additionalConditionData).containsExactly(
        AdditionalConditionData(
          id = -1,
          additionalCondition = anAdditionalConditionEntity,
          dataSequence = 0,
          dataField = "field1",
          dataValue = "value1",
        ),
      )

      // Verify last contact info is recorded
      assertThat(licenceCaptor.value)
        .extracting("updatedByUsername", "updatedBy")
        .isEqualTo(listOf(aCom.username, aCom))

      verify(conditionFormatter).format(CONDITION_CONFIG, conditionCaptor.value.additionalConditionData)
    }

    @Test
    fun `updating additional condition data triggers checking condition formatting`() {
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
      whenever(additionalConditionRepository.findById(1L))
        .thenReturn(
          Optional.of(
            anAdditionalConditionEntity.copy(),
          ),
        )

      whenever(staffRepository.findByUsernameIgnoreCase("smills")).thenReturn(aCom)

      val request = UpdateAdditionalConditionDataRequest(
        data = listOf(
          uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalConditionData(
            field = "field1",
            value = "value1",
            sequence = 0,
          ),
        ),
      )

      service.updateAdditionalConditionData(1L, 1L, request)

      val conditionCaptor = ArgumentCaptor.forClass(AdditionalCondition::class.java)
      val licenceCaptor = ArgumentCaptor.forClass(Licence::class.java)

      verify(additionalConditionRepository).saveAndFlush(conditionCaptor.capture())
      verify(licenceRepository).saveAndFlush(licenceCaptor.capture())
      verify(conditionFormatter).format(CONDITION_CONFIG, conditionCaptor.value.additionalConditionData)

      assertThat(licenceCaptor.value)
        .extracting("updatedByUsername", "updatedBy")
        .isEqualTo(listOf(aCom.username, aCom))
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
      whenever(staffRepository.findByUsernameIgnoreCase("smills")).thenReturn(null)

      val bespokeEntities = listOf(
        BespokeCondition(id = -1L, licence = aLicenceEntity, conditionSequence = 1, conditionText = "Condition 2"),
        BespokeCondition(id = -1L, licence = aLicenceEntity, conditionSequence = 2, conditionText = "Condition 3"),
        BespokeCondition(id = -1L, licence = aLicenceEntity, conditionSequence = 0, conditionText = "Condition 1"),
      )

      bespokeEntities.forEach { bespoke ->
        whenever(bespokeConditionRepository.saveAndFlush(bespoke)).thenReturn(bespoke)
      }

      service.updateBespokeConditions(1L, someBespokeConditions)

      // Verify licence entity is updated with last contact info
      val licenceCaptor = ArgumentCaptor.forClass(Licence::class.java)

      verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
      verify(auditService, times(1)).recordAuditEventUpdateBespokeConditions(any(), any(), any())

      assertThat(licenceCaptor.value)
        .extracting("updatedByUsername", "updatedBy")
        .isEqualTo(listOf(SYSTEM_USER, aPreviousUser))
      assertThat(licenceCaptor.value).extracting("bespokeConditions").isEqualTo(emptyList<BespokeCondition>())

      // Verify new bespoke conditions are added in their place
      bespokeEntities.forEach { bespoke ->
        verify(bespokeConditionRepository, times(1)).saveAndFlush(bespoke)
      }
    }
  }

  private companion object {
    val CONDITION_CONFIG = POLICY_V2_1.allAdditionalConditions().first()

    val aLicenceEntity = TestData.createCrdLicence()

    val someAdditionalConditionData = listOf(
      AdditionalConditionData(
        id = 1,
        dataField = "dataField",
        dataValue = "dataValue",
        additionalCondition = AdditionalCondition(licence = aLicenceEntity, conditionVersion = "1.0"),
      ),
    )

    val someDifferentAdditionalConditionData = listOf(
      AdditionalConditionData(
        id = 2,
        dataField = "dataField",
        dataValue = "dataValue2",
        additionalCondition = AdditionalCondition(licence = aLicenceEntity, conditionVersion = "1.0"),
      ),
    )

    val someBespokeConditions =
      BespokeConditionRequest(conditions = listOf("Condition 1", "Condition 2", "Condition 3"))

    val anAdditionalConditionEntity = AdditionalCondition(
      id = 1,
      conditionVersion = "1.0",
      licence = aLicenceEntity,
      conditionCode = "code1",
      conditionCategory = "category1",
      conditionSequence = 4,
      conditionText = "text",
      additionalConditionData = someAdditionalConditionData,
      additionalConditionUploadSummary = emptyList(),
      conditionType = "AP",
    )

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

    val aCom = TestData.com()

    val aPreviousUser = CommunityOffenderManager(
      staffIdentifier = 4000,
      username = "test",
      email = "test@test.com",
      firstName = "Test",
      lastName = "Test",
    )
  }

  private fun standardCondition(id: Long) =
    uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.StandardCondition(
      id = id,
      conditionCode = "goodBehaviour",
      conditionSequence = id.toInt(),
      conditionText = "Be of good behaviour",
      conditionType = "AP",
      licence = aLicenceEntity,
    )
}
