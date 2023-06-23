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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.BespokeCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalConditionsRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.BespokeConditionRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.StandardCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.UpdateAdditionalConditionDataRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.UpdateStandardConditionDataRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.AdditionalConditions
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.LicencePolicy
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.StandardConditions
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.AddAdditionalConditionRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionUploadDetailRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.BespokeConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.CommunityOffenderManagerRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.POLICY_V2_1
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional

class LicenceConditionServiceTest {
  private val licenceRepository = mock<LicenceRepository>()
  private val additionalConditionRepository = mock<AdditionalConditionRepository>()
  private val bespokeConditionRepository = mock<BespokeConditionRepository>()
  private val additionalConditionUploadDetailRepository = mock<AdditionalConditionUploadDetailRepository>()
  private val auditEventRepository = mock<AuditEventRepository>()
  private val communityOffenderManagerRepository = mock<CommunityOffenderManagerRepository>()
  private val policyService = mock<LicencePolicyService>()
  private val conditionFormatter = mock<ConditionFormatter>()

  private val service = LicenceConditionService(
    licenceRepository,
    additionalConditionRepository,
    bespokeConditionRepository,
    additionalConditionUploadDetailRepository,
    conditionFormatter,
    policyService,
  )

  @BeforeEach
  fun reset() {
    val authentication = mock<Authentication>()
    val securityContext = mock<SecurityContext>()
    whenever(authentication.name).thenReturn("smills")
    whenever(securityContext.authentication).thenReturn(authentication)
    whenever(policyService.getConfigForCondition(any())).thenReturn(CONDITION_CONFIG)
    SecurityContextHolder.setContext(securityContext)

    reset(
      licenceRepository,
      additionalConditionRepository,
      bespokeConditionRepository,
      additionalConditionUploadDetailRepository,
      auditEventRepository,
      communityOffenderManagerRepository,
      licencePolicyService,
    )
  }

  @Test
  fun `update standard conditions for an individual licence`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))
    whenever(communityOffenderManagerRepository.findByUsernameIgnoreCase("smills")).thenReturn(aCom)
    whenever(licencePolicyService.currentPolicy()).thenReturn(aPolicy)

    val APConditions = listOf(
      StandardCondition(code = "goodBehaviour", sequence = 1, text = "Be of good behaviour"),
    )

    val PSSConditions = listOf(
      StandardCondition(code = "goodBehaviour", sequence = 1, text = "Be of good behaviour"),
      StandardCondition(code = "doNotBreakLaw", sequence = 2, text = "Do not break any law"),
    )

    service.updateStandardConditions(
      1,
      UpdateStandardConditionDataRequest(
        standardLicenceConditions = APConditions,
        standardPssConditions = PSSConditions,
      ),
    )

    val licenceCaptor = ArgumentCaptor.forClass(Licence::class.java)
    val auditCaptor = ArgumentCaptor.forClass(AuditEvent::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())
    assertThat(licenceCaptor.value).extracting("updatedByUsername").isEqualTo("smills")

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

    assertThat(auditCaptor.value.licenceId).isEqualTo(licenceCaptor.value.id)
    assertThat(auditCaptor.value.username).isEqualTo("smills")
    assertThat(auditCaptor.value.summary)
      .isEqualTo(
        "Standard conditions updated to policy version ${aPolicy.version} for " +
          "${licenceCaptor.value.forename} ${licenceCaptor.value.surname}",
      )
    assertThat(auditCaptor.value.detail)
      .isEqualTo(
        "ID ${licenceCaptor.value.id} type ${licenceCaptor.value.typeCode.name} " +
          "status ${licenceCaptor.value.statusCode.name} version ${licenceCaptor.value.version}",
      )
    assertThat(auditCaptor.value.changes)
      .extracting("typeOfChange", "condition", "changes")
      .isEqualTo(
        listOf(
          "update",
          "standard",
          emptyMap<String, Any>(),
        ),
      )
  }

  @Test
  fun `delete one additional condition`() {
    whenever(licenceRepository.findById(1L))
      .thenReturn(
        Optional.of(
          aLicenceEntity.copy(
            additionalConditions = listOf(
              AdditionalCondition(
                id = 1,
                conditionVersion = "1.0",
                conditionCode = "code",
                conditionSequence = 5,
                conditionCategory = "oldCategory",
                conditionText = "oldText",
                expandedConditionText = "expandedOldText",
                additionalConditionData = someAdditionalConditionData,
                licence = aLicenceEntity,
                conditionType = "AP",
              ),
              AdditionalCondition(
                id = 2,
                conditionVersion = "1.0",
                conditionCode = "code2",
                conditionSequence = 6,
                conditionCategory = "removedCategory",
                conditionText = "removedText",
                expandedConditionText = "removedText",
                additionalConditionData = someAdditionalConditionData,
                licence = aLicenceEntity,
                conditionType = "AP",
              ),
              AdditionalCondition(
                id = 3,
                conditionVersion = "1.0",
                conditionCode = "code3",
                conditionSequence = 6,
                conditionCategory = "oldCategory3",
                conditionText = "oldText3",
                expandedConditionText = "expandedOldText3",
                additionalConditionData = someAdditionalConditionData,
                licence = aLicenceEntity,
                conditionType = "AP",
              ),
            ),
          ),
        ),
      )
    whenever(communityOffenderManagerRepository.findByUsernameIgnoreCase("smills")).thenReturn(aCom)

    service.deleteAdditionalCondition(1L, 2)

    val licenceCaptor = ArgumentCaptor.forClass(Licence::class.java)
    val auditCaptor = ArgumentCaptor.forClass(AuditEvent::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())

    assertThat(licenceCaptor.value.additionalConditions).containsExactly(
      AdditionalCondition(
        id = 1,
        conditionVersion = "1.0",
        conditionCode = "code",
        conditionCategory = "oldCategory",
        conditionSequence = 5,
        conditionText = "oldText",
        expandedConditionText = "expandedOldText",
        conditionType = "AP",
        additionalConditionData = someAdditionalConditionData,
        licence = aLicenceEntity,
      ),
      AdditionalCondition(
        id = 3,
        conditionVersion = "1.0",
        conditionCode = "code3",
        conditionCategory = "oldCategory3",
        conditionSequence = 6,
        conditionText = "oldText3",
        expandedConditionText = "expandedOldText3",
        conditionType = "AP",
        additionalConditionData = someAdditionalConditionData,
        licence = aLicenceEntity,
      ),
    )

    // Verify last contact info is recorded
    assertThat(licenceCaptor.value.updatedByUsername).isEqualTo("smills")

    assertThat(auditCaptor.value.licenceId).isEqualTo(licenceCaptor.value.id)
    assertThat(auditCaptor.value.username).isEqualTo("smills")
    assertThat(auditCaptor.value.summary)
      .isEqualTo(
        "Deleted condition for ${licenceCaptor.value.forename} ${licenceCaptor.value.surname}",
      )
    assertThat(auditCaptor.value.detail)
      .isEqualTo(
        "ID ${licenceCaptor.value.id} type ${licenceCaptor.value.typeCode.name} " +
          "status ${licenceCaptor.value.statusCode.name} version ${licenceCaptor.value.version}",
      )

    assertThat(auditCaptor.value.changes)
      .extracting("typeOfChange", "condition", "changes")
      .isEqualTo(
        listOf(
          "delete",
          "additional",
          listOf(
            mapOf(
              "conditionCode" to "code2",
              "conditionType" to "AP",
              "conditionText" to "removedText",
            ),
          ),
        ),
      )
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
              uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalCondition(
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
                AdditionalCondition(
                  id = 1,
                  conditionVersion = "1.0",
                  conditionCode = "code",
                  conditionSequence = 5,
                  conditionCategory = "oldCategory",
                  conditionText = "oldText",
                  additionalConditionData = someAdditionalConditionData,
                  licence = aLicenceEntity,
                  conditionType = "AP",
                ),
                AdditionalCondition(
                  id = 2,
                  conditionVersion = "1.0",
                  conditionCode = "code2",
                  conditionSequence = 6,
                  conditionCategory = "removedCategory",
                  conditionText = "removedText",
                  additionalConditionData = someAdditionalConditionData,
                  licence = aLicenceEntity,
                  conditionType = "AP",
                ),
                AdditionalCondition(
                  id = 3,
                  conditionVersion = "1.0",
                  conditionCode = "code3",
                  conditionSequence = 6,
                  conditionCategory = "pssCategory",
                  conditionText = "pssText",
                  additionalConditionData = someDifferentAdditionalConditionData,
                  licence = aLicenceEntity,
                  conditionType = "PSS",
                ),
              ),
            ),
          ),
        )

      val request = AdditionalConditionsRequest(
        additionalConditions = listOf(
          uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalCondition(
            code = "code",
            category = "category",
            text = "text",
            sequence = 0,
          ),
        ),
        conditionType = "AP",
      )
    whenever(communityOffenderManagerRepository.findByUsernameIgnoreCase("smills")).thenReturn(aCom)

      service.updateAdditionalConditions(1L, request)

      val licenceCaptor = ArgumentCaptor.forClass(Licence::class.java)

      verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())

      assertThat(licenceCaptor.value.additionalConditions).containsExactly(
        AdditionalCondition(
          id = 1,
          conditionVersion = "1.0",
          conditionCode = "code",
          conditionCategory = "category",
          conditionSequence = 0,
          conditionText = "text",
          conditionType = "AP",
          additionalConditionData = someAdditionalConditionData,
          licence = aLicenceEntity,
        ),
        AdditionalCondition(
          id = 3,
          conditionVersion = "1.0",
          conditionCode = "code3",
          conditionSequence = 6,
          conditionCategory = "pssCategory",
          conditionText = "pssText",
          additionalConditionData = someDifferentAdditionalConditionData,
          licence = aLicenceEntity,
          conditionType = "PSS",
        ),
      )

      // Verify last contact info is recorded
      assertThat(licenceCaptor.value.updatedByUsername).isEqualTo("smills")

      verify(conditionFormatter).format(CONDITION_CONFIG, someAdditionalConditionData)
      verify(conditionFormatter).format(CONDITION_CONFIG, someDifferentAdditionalConditionData)
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
                AdditionalCondition(
                  id = 1,
                  conditionVersion = "1.0",
                  conditionCode = "code",
                  conditionSequence = 5,
                  conditionCategory = "oldCategory",
                  conditionText = "oldText",
                  additionalConditionData = someAdditionalConditionData,
                  licence = aLicenceEntity,
                  conditionType = "AP",
                ),
                AdditionalCondition(
                  id = 3,
                  conditionVersion = "1.0",
                  conditionCode = "code3",
                  conditionSequence = 6,
                  conditionCategory = "pssCategory",
                  conditionText = "pssText",
                  additionalConditionData = someDifferentAdditionalConditionData,
                  licence = aLicenceEntity,
                  conditionType = "PSS",
                ),
              ),
            ),
          ),
        )

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

      assertThat(licenceCaptor.value.additionalConditions).extracting("id", "conditionCode", "conditionSequence")
        .containsExactly(
          tuple(1L, "code", 5),
          tuple(3L, "code3", 6),
          tuple(-1L, "code", 7),
        )

      // Verify last contact info is recorded
      assertThat(licenceCaptor.value.updatedByUsername).isEqualTo("smills")

      // No way of providing additional condition data via this endpoint so no point running through formatter
      verifyNoInteractions(conditionFormatter)
    }
  }

  @Nested
  inner class `update bespoke conditions` {
    @Test
    fun `update bespoke conditions persists multiple entities`() {
      whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))

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

      assertThat(licenceCaptor.value).extracting("updatedByUsername").isEqualTo("smills")
      assertThat(licenceCaptor.value).extracting("bespokeConditions").isEqualTo(emptyList<BespokeCondition>())

      // Verify new bespoke conditions are added in their place
      bespokeEntities.forEach { bespoke ->
        verify(bespokeConditionRepository, times(1)).saveAndFlush(bespoke)
      }
    }

    @Test
    fun `update bespoke conditions with an empty list - removes previously persisted entities`() {
      whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))

      service.updateBespokeConditions(1L, BespokeConditionRequest())

      verify(bespokeConditionRepository, times(0)).saveAndFlush(any())
      verify(licenceRepository, times(1)).saveAndFlush(any())
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
            expandedConditionText = "expanded text",
          ),
        )
      }

      assertThat(exception).isInstanceOf(EntityNotFoundException::class.java)

      verify(licenceRepository, times(1)).findById(1L)
      verify(licenceRepository, times(0)).saveAndFlush(any())
    }

    @Test
    fun `update additional condition data throws not found exception if condition is not found`() {
      whenever(licenceRepository.findById(1L))
        .thenReturn(
          Optional.of(
            aLicenceEntity.copy(
              additionalConditions = listOf(
                AdditionalCondition(
                  id = 1,
                  conditionVersion = "1.0",
                  conditionCode = "code",
                  conditionSequence = 5,
                  conditionCategory = "oldCategory",
                  conditionText = "oldText",
                  additionalConditionData = emptyList(),
                  licence = aLicenceEntity,
                ),
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
            expandedConditionText = "expanded text",
          ),
        )
      }

      assertThat(exception).isInstanceOf(EntityNotFoundException::class.java)

      verify(licenceRepository, times(1)).findById(1L)
      verify(licenceRepository, times(0)).saveAndFlush(any())
    }

    @Test
    fun `update additional condition data`() {
      whenever(licenceRepository.findById(1L))
        .thenReturn(
          Optional.of(
            aLicenceEntity.copy(
              additionalConditions = listOf(
                AdditionalCondition(
                  id = 1,
                  conditionVersion = "1.0",
                  conditionCode = "code",
                  conditionSequence = 5,
                  conditionCategory = "oldCategory",
                  conditionText = "oldText",
                  additionalConditionData = someAdditionalConditionData,
                  licence = aLicenceEntity,
                ),
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

      val request = UpdateAdditionalConditionDataRequest(
        data = listOf(
          uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalConditionData(
            field = "field1",
            value = "value1",
            sequence = 0,
          ),
        ),
        expandedConditionText = "expanded text",
      )

      service.updateAdditionalConditionData(1L, 1L, request)

      val conditionCaptor = ArgumentCaptor.forClass(AdditionalCondition::class.java)
      val licenceCaptor = ArgumentCaptor.forClass(Licence::class.java)

      verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())

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
      assertThat(licenceCaptor.value.updatedByUsername).isEqualTo("smills")

      verify(conditionFormatter).format(CONDITION_CONFIG, conditionCaptor.value.additionalConditionData)
    }

    @Test
    fun `updating additional condition data triggers checking condition formatting`() {
      whenever(licenceRepository.findById(1L))
        .thenReturn(
          Optional.of(
            aLicenceEntity.copy(
              additionalConditions = listOf(
                AdditionalCondition(
                  id = 1,
                  conditionVersion = "1.0",
                  conditionCode = "code",
                  conditionSequence = 5,
                  conditionCategory = "oldCategory",
                  conditionText = "oldText",
                  additionalConditionData = someAdditionalConditionData,
                  licence = aLicenceEntity,
                ),
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

      val request = UpdateAdditionalConditionDataRequest(
        data = listOf(
          uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalConditionData(
            field = "field1",
            value = "value1",
            sequence = 0,
          ),
        ),
        expandedConditionText = "expanded text",
      )

      service.updateAdditionalConditionData(1L, 1L, request)

      val conditionCaptor = ArgumentCaptor.forClass(AdditionalCondition::class.java)

      verify(additionalConditionRepository).saveAndFlush(conditionCaptor.capture())
      verify(conditionFormatter).format(CONDITION_CONFIG, conditionCaptor.value.additionalConditionData)
    }

    @Test
    fun `formatting errors are not propagated`() {
      whenever(licenceRepository.findById(1L))
        .thenReturn(
          Optional.of(
            aLicenceEntity.copy(
              additionalConditions = listOf(
                AdditionalCondition(
                  id = 1,
                  conditionVersion = "1.0",
                  conditionCode = "code",
                  conditionSequence = 5,
                  conditionCategory = "oldCategory",
                  conditionText = "oldText",
                  additionalConditionData = someAdditionalConditionData,
                  licence = aLicenceEntity,
                ),
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

      val request = UpdateAdditionalConditionDataRequest(
        data = listOf(
          uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalConditionData(
            field = "field1",
            value = "value1",
            sequence = 0,
          ),
        ),
        expandedConditionText = "expanded text",
      )

      whenever(conditionFormatter.format(any(), any())).thenThrow(NullPointerException())

      service.updateAdditionalConditionData(1L, 1L, request)
    }
  }

  private companion object {
    val CONDITION_CONFIG = POLICY_V2_1.allAdditionalConditions().first()

    val someEntityStandardConditions = listOf(
      uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.StandardCondition(
        id = 1,
        conditionCode = "goodBehaviour",
        conditionSequence = 1,
        conditionText = "Be of good behaviour",
        licence = mock(),
      ),
      uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.StandardCondition(
        id = 2,
        conditionCode = "notBreakLaw",
        conditionSequence = 2,
        conditionText = "Do not break any law",
        licence = mock(),
      ),
      uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.StandardCondition(
        id = 3,
        conditionCode = "attendMeetings",
        conditionSequence = 3,
        conditionText = "Attend meetings",
        licence = mock(),
      ),
    )

    val aLicenceEntity = Licence(
      id = 1,
      typeCode = LicenceType.AP,
      version = "1.1",
      statusCode = LicenceStatus.IN_PROGRESS,
      nomsId = "A1234AA",
      bookingNo = "123456",
      bookingId = 54321,
      crn = "X12345",
      pnc = "2019/123445",
      cro = "12345",
      prisonCode = "MDI",
      prisonDescription = "Moorland (HMP)",
      forename = "Bob",
      surname = "Mortimer",
      dateOfBirth = LocalDate.of(1985, 12, 28),
      conditionalReleaseDate = LocalDate.of(2021, 10, 22),
      actualReleaseDate = LocalDate.of(2021, 10, 22),
      sentenceStartDate = LocalDate.of(2018, 10, 22),
      sentenceEndDate = LocalDate.of(2021, 10, 22),
      licenceStartDate = LocalDate.of(2021, 10, 22),
      licenceExpiryDate = LocalDate.of(2021, 10, 22),
      topupSupervisionStartDate = LocalDate.of(2021, 10, 22),
      topupSupervisionExpiryDate = LocalDate.of(2021, 10, 22),
      probationAreaCode = "N01",
      probationAreaDescription = "Wales",
      probationPduCode = "N01A",
      probationPduDescription = "Cardiff",
      probationLauCode = "N01A2",
      probationLauDescription = "Cardiff South",
      probationTeamCode = "NA01A2-A",
      probationTeamDescription = "Cardiff South Team A",
      dateCreated = LocalDateTime.of(2022, 7, 27, 15, 0, 0),
      standardConditions = someEntityStandardConditions,
      responsibleCom = CommunityOffenderManager(
        staffIdentifier = 2000,
        username = "smills",
        email = "testemail@probation.gov.uk",
        firstName = "X",
        lastName = "Y",
      ),
      createdBy = CommunityOffenderManager(
        staffIdentifier = 2000,
        username = "smills",
        email = "testemail@probation.gov.uk",
        firstName = "X",
        lastName = "Y",
      ),
    )

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

    val aCom = CommunityOffenderManager(
      staffIdentifier = 2000,
      username = "smills",
      email = "testemail@probation.gov.uk",
      firstName = "X",
      lastName = "Y",
    )
  }
}
