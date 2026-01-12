package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.BespokeCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcCurfewTimes
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AuditRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.AdditionalConditions
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.LicencePolicy
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.StandardConditions
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateElectronicMonitoringProgrammeRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.anAdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.anotherCommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.communityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createCrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createHdcLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.prisonUser
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.events.UpdateProbationTeamEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AuditEventType
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Optional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AuditEvent as EntityAuditEvent

class AuditServiceTest {
  private val licenceRepository = mock<LicenceRepository>()
  private val auditEventRepository = mock<AuditEventRepository>()

  private val service = AuditService(auditEventRepository, licenceRepository)

  @BeforeEach
  fun reset() {
    val authentication = mock<Authentication>()
    val securityContext = mock<SecurityContext>()
    whenever(authentication.name).thenReturn("tcom")
    whenever(securityContext.authentication).thenReturn(authentication)

    SecurityContextHolder.setContext(securityContext)

    reset(auditEventRepository, licenceRepository)
  }

  @Test
  fun `records an audit event`() {
    whenever(auditEventRepository.save(transform(anEvent))).thenReturn(transform(anEvent))
    service.recordAuditEvent(anEvent)
    verify(auditEventRepository, times(1)).save(transform(anEvent))
  }

  @Test
  fun `gets audit events relating to a specific licence`() {
    val aUserRequest = aRequest.copy(username = null, licenceId = 1L)

    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))

    whenever(
      auditEventRepository
        .findAllByLicenceIdAndEventTimeBetweenOrderByEventTimeDesc(
          aUserRequest.licenceId!!,
          aUserRequest.startTime,
          aUserRequest.endTime,
        ),
    ).thenReturn(aListOfEntities)

    val response = service.getAuditEvents(aUserRequest)

    assertThat(response).hasSize(3)
    assertThat(response[0].summary).isEqualTo("Summary1")

    verify(licenceRepository, times(1)).findById(1L)
    verify(auditEventRepository, times(1)).findAllByLicenceIdAndEventTimeBetweenOrderByEventTimeDesc(
      aUserRequest.licenceId!!,
      aUserRequest.startTime,
      aUserRequest.endTime,
    )
  }

  @Test
  fun `get audit events relating to a specific user`() {
    val aUserRequest = aRequest.copy(licenceId = null)
    whenever(
      auditEventRepository
        .findAllByUsernameAndEventTimeBetweenOrderByEventTimeDesc(
          aUserRequest.username!!,
          aUserRequest.startTime,
          aUserRequest.endTime,
        ),
    ).thenReturn(aListOfEntities)

    val response = service.getAuditEvents(aUserRequest)

    assertThat(response).hasSize(3)
    assertThat(response[0].summary).isEqualTo("Summary1")

    verify(auditEventRepository, times(1)).findAllByUsernameAndEventTimeBetweenOrderByEventTimeDesc(
      aUserRequest.username!!,
      aUserRequest.startTime,
      aUserRequest.endTime,
    )
  }

  @Test
  fun `get all audit events`() {
    val aUserRequest = aRequest.copy(username = null, licenceId = null)
    val expectedChanges = mapOf(
      "type" to "Added something",
      "changes" to mapOf(
        "type" to "ADDED",
        "conditionText" to "Something",
      ),
    )

    whenever(
      auditEventRepository
        .findAllByEventTimeBetweenOrderByEventTimeDesc(
          aUserRequest.startTime,
          aUserRequest.endTime,
        ),
    ).thenReturn(aListOfEntities)

    val response = service.getAuditEvents(aUserRequest)

    assertThat(response).hasSize(3)
    assertThat(response.first().summary).isEqualTo("Summary1")
    assertThat(response.first().changes).isEqualTo(expectedChanges)

    verify(auditEventRepository, times(1)).findAllByEventTimeBetweenOrderByEventTimeDesc(
      aUserRequest.startTime,
      aUserRequest.endTime,
    )
  }

  @Test
  fun `records an event when initial appointment details are updated`() {
    service.recordAuditEventInitialAppointmentUpdate(
      aLicenceEntity,
      mapOf(
        "field" to "appointmentPerson",
        "previousValue" to "Joe Bloggs",
        "newValue" to "John Doe",
      ),
      aCom,
    )

    val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)
    verify(auditEventRepository, times(1)).save(auditCaptor.capture())

    assertThat(auditCaptor.value.username).isEqualTo(aCom.username)
    assertThat(auditCaptor.value.summary).isEqualTo("Updated initial appointment details for ${aLicenceEntity.forename} ${aLicenceEntity.surname}")
    assertThat(auditCaptor.value.detail).isEqualTo(
      "ID ${aLicenceEntity.id} type ${aLicenceEntity.typeCode.name} " +
        "status ${aLicenceEntity.statusCode.name} version ${aLicenceEntity.version}",
    )
    assertThat(auditCaptor.value.changes).isEqualTo(
      mapOf(
        "field" to "appointmentPerson",
        "previousValue" to "Joe Bloggs",
        "newValue" to "John Doe",
      ),
    )
  }

  @Nested
  inner class `audit events for licence conditions` {
    @Test
    fun `records an audit event when standard conditions are updated`() {
      service.recordAuditEventUpdateStandardCondition(aLicenceEntity, aPolicy.version, aCom)

      val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)
      verify(auditEventRepository, times(1)).save(auditCaptor.capture())

      assertThat(auditCaptor.value.username).isEqualTo(aCom.username)
      assertThat(auditCaptor.value.summary)
        .isEqualTo(
          "Updated standard conditions to policy version ${aPolicy.version} for " +
            "${aLicenceEntity.forename} ${aLicenceEntity.surname}",
        )
      assertThat(auditCaptor.value.detail)
        .isEqualTo(
          "ID ${aLicenceEntity.id} type ${aLicenceEntity.typeCode.name} " +
            "status ${aLicenceEntity.statusCode.name} version ${aLicenceEntity.version}",
        )
      assertThat(auditCaptor.value.changes)
        .extracting("type", "changes")
        .isEqualTo(
          listOf(
            "Updated standard conditions",
            emptyMap<String, Any>(),
          ),
        )
    }

    @Test
    fun `records an audit event when standard conditions are updated by removing existing conditions`() {
      service.recordAuditEventDeleteStandardConditions(
        aLicenceEntity,
        aLicenceEntity.standardConditions,
        aCom,
      )

      val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)
      verify(auditEventRepository, times(1)).save(auditCaptor.capture())

      assertThat(auditCaptor.value.licenceId).isEqualTo(aLicenceEntity.id)
      assertThat(auditCaptor.value.username).isEqualTo(aCom.username)
      assertThat(auditCaptor.value.summary)
        .isEqualTo(
          "Updated standard conditions for ${aLicenceEntity.forename} ${aLicenceEntity.surname}",
        )
      assertThat(auditCaptor.value.detail)
        .isEqualTo(
          "ID ${aLicenceEntity.id} type ${aLicenceEntity.typeCode.name} " +
            "status ${aLicenceEntity.statusCode.name} version ${aLicenceEntity.version}",
        )

      assertThat(auditCaptor.value.changes)
        .extracting("type", "changes")
        .isEqualTo(
          listOf(
            "Updated standard conditions",
            listOf(
              mapOf(
                "type" to "REMOVED",
                "conditionCode" to "goodBehaviour",
                "conditionType" to "AP",
                "conditionText" to "Be of good behaviour",
              ),
              mapOf(
                "type" to "REMOVED",
                "conditionCode" to "notBreakLaw",
                "conditionType" to "AP",
                "conditionText" to "Do not break any law",
              ),
              mapOf(
                "type" to "REMOVED",
                "conditionCode" to "attendMeetings",
                "conditionType" to "AP",
                "conditionText" to "Attend meetings",
              ),
            ),
          ),
        )
    }

    @Test
    fun `records an audit event when bespoke conditions are deleted in PSS period`() {
      service.recordAuditEventDeleteBespokeConditions(
        aLicenceEntity,
        someBespokeConditions,
        aCom,
      )

      val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)
      verify(auditEventRepository, times(1)).save(auditCaptor.capture())

      assertThat(auditCaptor.value.licenceId).isEqualTo(aLicenceEntity.id)
      assertThat(auditCaptor.value.username).isEqualTo(aCom.username)
      assertThat(auditCaptor.value.summary)
        .isEqualTo(
          "Updated bespoke conditions for ${aLicenceEntity.forename} ${aLicenceEntity.surname}",
        )
      assertThat(auditCaptor.value.detail)
        .isEqualTo(
          "ID ${aLicenceEntity.id} type ${aLicenceEntity.typeCode.name} " +
            "status ${aLicenceEntity.statusCode.name} version ${aLicenceEntity.version}",
        )

      assertThat(auditCaptor.value.changes)
        .extracting("type", "changes")
        .isEqualTo(
          listOf(
            "Updated bespoke conditions",
            listOf(
              mapOf(
                "type" to "REMOVED",
                "conditionText" to someBespokeConditions[0].conditionText,
              ),
            ),
          ),
        )
    }

    @Test
    fun `records no audit event when no standard conditions are removed`() {
      service.recordAuditEventDeleteStandardConditions(aLicenceEntity, emptyList(), aCom)

      verifyNoMoreInteractions(auditEventRepository)
    }

    @Test
    fun `records no audit event when no bespoke conditions are removed`() {
      service.recordAuditEventDeleteBespokeConditions(aLicenceEntity, emptyList(), aCom)

      verifyNoMoreInteractions(auditEventRepository)
    }

    @Test
    fun `records an audit event when an additional condition of the same type is added`() {
      service.recordAuditEventAddAdditionalConditionOfSameType(aLicenceEntity, anAdditionalConditionEntity, aCom)

      val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)
      verify(auditEventRepository, times(1)).save(auditCaptor.capture())

      assertThat(auditCaptor.value.licenceId).isEqualTo(aLicenceEntity.id)
      assertThat(auditCaptor.value.username).isEqualTo(aCom.username)
      assertThat(auditCaptor.value.summary)
        .isEqualTo(
          "Updated additional condition of the same type for ${aLicenceEntity.forename} ${aLicenceEntity.surname}",
        )
      assertThat(auditCaptor.value.detail)
        .isEqualTo(
          "ID ${aLicenceEntity.id} type ${aLicenceEntity.typeCode.name} " +
            "status ${aLicenceEntity.statusCode.name} version ${aLicenceEntity.version}",
        )

      assertThat(auditCaptor.value.changes)
        .extracting("type", "changes")
        .isEqualTo(
          listOf(
            "Updated additional conditions",
            listOf(
              mapOf(
                "type" to "ADDED",
                "conditionCode" to "code1",
                "conditionType" to "AP",
                "conditionText" to "text",
              ),
            ),
          ),
        )
    }

    @Test
    fun `records an audit event when an additional condition is deleted`() {
      val aDeletedCondition = listOf(
        anAdditionalConditionEntity.copy(
          expandedConditionText = "removedText",
        ),
      )

      service.recordAuditEventDeleteAdditionalConditions(aLicenceEntity, aDeletedCondition, aCom)

      val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)
      verify(auditEventRepository, times(1)).save(auditCaptor.capture())

      assertThat(auditCaptor.value.licenceId).isEqualTo(aLicenceEntity.id)
      assertThat(auditCaptor.value.username).isEqualTo(aCom.username)
      assertThat(auditCaptor.value.summary)
        .isEqualTo(
          "Updated additional conditions for ${aLicenceEntity.forename} ${aLicenceEntity.surname}",
        )
      assertThat(auditCaptor.value.detail)
        .isEqualTo(
          "ID ${aLicenceEntity.id} type ${aLicenceEntity.typeCode.name} " +
            "status ${aLicenceEntity.statusCode.name} version ${aLicenceEntity.version}",
        )

      assertThat(auditCaptor.value.changes)
        .extracting("type", "changes")
        .isEqualTo(
          listOf(
            "Updated additional conditions",
            listOf(
              mapOf(
                "type" to "REMOVED",
                "conditionCode" to "code1",
                "conditionType" to "AP",
                "conditionText" to "removedText",
              ),
            ),
          ),
        )
    }

    @Test
    fun `records an audit event when additional conditions are updated by adding new conditions`() {
      val anAddedAdditionalCondition = listOf(
        anAdditionalConditionEntity.copy(
          id = 2L,
          conditionVersion = "1.0",
          conditionCode = "code2",
          conditionCategory = "category2",
          conditionText = "text2",
          additionalConditionData = mutableListOf(),
        ),
      )

      service.recordAuditEventUpdateAdditionalConditions(
        aLicenceEntity,
        anAddedAdditionalCondition,
        emptyList(),
        aCom,
      )

      val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)
      verify(auditEventRepository, times(1)).save(auditCaptor.capture())

      assertThat(auditCaptor.value.licenceId).isEqualTo(aLicenceEntity.id)
      assertThat(auditCaptor.value.username).isEqualTo(aCom.username)
      assertThat(auditCaptor.value.summary)
        .isEqualTo(
          "Updated additional conditions for ${aLicenceEntity.forename} ${aLicenceEntity.surname}",
        )
      assertThat(auditCaptor.value.detail)
        .isEqualTo(
          "ID ${aLicenceEntity.id} type ${aLicenceEntity.typeCode.name} " +
            "status ${aLicenceEntity.statusCode.name} version ${aLicenceEntity.version}",
        )

      assertThat(auditCaptor.value.changes)
        .extracting("type", "changes")
        .isEqualTo(
          listOf(
            "Updated additional conditions",
            listOf(
              mapOf(
                "type" to "ADDED",
                "conditionCode" to "code2",
                "conditionType" to "AP",
                "conditionText" to "text2",
              ),
            ),
          ),
        )
    }

    @Test
    fun `records an audit event when additional conditions are updated by removing existing conditions`() {
      val aRemovedAdditionalCondition = listOf(
        anAdditionalConditionEntity,
      )

      service.recordAuditEventUpdateAdditionalConditions(
        aLicenceEntity,
        emptyList(),
        aRemovedAdditionalCondition,
        aCom,
      )

      val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)
      verify(auditEventRepository, times(1)).save(auditCaptor.capture())

      assertThat(auditCaptor.value.licenceId).isEqualTo(aLicenceEntity.id)
      assertThat(auditCaptor.value.username).isEqualTo(aCom.username)
      assertThat(auditCaptor.value.summary)
        .isEqualTo(
          "Updated additional conditions for ${aLicenceEntity.forename} ${aLicenceEntity.surname}",
        )
      assertThat(auditCaptor.value.detail)
        .isEqualTo(
          "ID ${aLicenceEntity.id} type ${aLicenceEntity.typeCode.name} " +
            "status ${aLicenceEntity.statusCode.name} version ${aLicenceEntity.version}",
        )

      assertThat(auditCaptor.value.changes)
        .extracting("type", "changes")
        .isEqualTo(
          listOf(
            "Updated additional conditions",
            listOf(
              mapOf(
                "type" to "REMOVED",
                "conditionCode" to "code1",
                "conditionType" to "AP",
                "conditionText" to "text",
              ),
            ),
          ),
        )
    }

    @Test
    fun `records an audit event when additional conditions are updated by adding and removing existing conditions at the same time`() {
      val anAddedAddtionalCondition = listOf(
        anAdditionalConditionEntity.copy(
          id = 2L,
          conditionVersion = "1.0",
          conditionCode = "code2",
          conditionCategory = "category2",
          conditionText = "text2",
          additionalConditionData = mutableListOf(),
        ),
      )

      val aRemovedAdditionalCondition = listOf(
        anAdditionalConditionEntity,
      )

      service.recordAuditEventUpdateAdditionalConditions(
        aLicenceEntity,
        anAddedAddtionalCondition,
        aRemovedAdditionalCondition,
        aCom,
      )

      val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)
      verify(auditEventRepository, times(1)).save(auditCaptor.capture())

      assertThat(auditCaptor.value.licenceId).isEqualTo(aLicenceEntity.id)
      assertThat(auditCaptor.value.username).isEqualTo(aCom.username)
      assertThat(auditCaptor.value.summary)
        .isEqualTo(
          "Updated additional conditions for ${aLicenceEntity.forename} ${aLicenceEntity.surname}",
        )
      assertThat(auditCaptor.value.detail)
        .isEqualTo(
          "ID ${aLicenceEntity.id} type ${aLicenceEntity.typeCode.name} " +
            "status ${aLicenceEntity.statusCode.name} version ${aLicenceEntity.version}",
        )

      assertThat(auditCaptor.value.changes)
        .extracting("type", "changes")
        .isEqualTo(
          listOf(
            "Updated additional conditions",
            listOf(
              mapOf(
                "type" to "ADDED",
                "conditionCode" to "code2",
                "conditionType" to "AP",
                "conditionText" to "text2",
              ),
              mapOf(
                "type" to "REMOVED",
                "conditionCode" to "code1",
                "conditionType" to "AP",
                "conditionText" to "text",
              ),
            ),
          ),
        )
    }

    @Test
    fun `does not record an audit event when no additional conditions are updated`() {
      service.recordAuditEventUpdateAdditionalConditions(aLicenceEntity, emptyList(), emptyList(), aCom)

      verifyNoMoreInteractions(auditEventRepository)
    }

    @Test
    fun `records an audit event when bespoke conditions are updated by adding new conditions`() {
      service.recordAuditEventUpdateBespokeConditions(aLicenceEntity, newBespokeConditions, emptyList(), aCom)

      val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)
      verify(auditEventRepository, times(1)).save(auditCaptor.capture())

      assertThat(auditCaptor.value.licenceId).isEqualTo(aLicenceEntity.id)
      assertThat(auditCaptor.value.username).isEqualTo(aCom.username)
      assertThat(auditCaptor.value.summary)
        .isEqualTo(
          "Updated bespoke conditions for ${aLicenceEntity.forename} ${aLicenceEntity.surname}",
        )
      assertThat(auditCaptor.value.detail)
        .isEqualTo(
          "ID ${aLicenceEntity.id} type ${aLicenceEntity.typeCode.name} " +
            "status ${aLicenceEntity.statusCode.name} version ${aLicenceEntity.version}",
        )

      assertThat(auditCaptor.value.changes)
        .extracting("type", "changes")
        .isEqualTo(
          listOf(
            "Updated bespoke conditions",
            listOf(
              mapOf(
                "type" to "ADDED",
                "conditionText" to "Condition1",
              ),
            ),
          ),
        )
    }

    @Test
    fun `records an audit event when bespoke conditions are updated by removing existing conditions`() {
      service.recordAuditEventUpdateBespokeConditions(aLicenceEntity, emptyList(), newBespokeConditions, aCom)

      val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)
      verify(auditEventRepository, times(1)).save(auditCaptor.capture())

      assertThat(auditCaptor.value.licenceId).isEqualTo(aLicenceEntity.id)
      assertThat(auditCaptor.value.username).isEqualTo(aCom.username)
      assertThat(auditCaptor.value.summary)
        .isEqualTo(
          "Updated bespoke conditions for ${aLicenceEntity.forename} ${aLicenceEntity.surname}",
        )
      assertThat(auditCaptor.value.detail)
        .isEqualTo(
          "ID ${aLicenceEntity.id} type ${aLicenceEntity.typeCode.name} " +
            "status ${aLicenceEntity.statusCode.name} version ${aLicenceEntity.version}",
        )

      assertThat(auditCaptor.value.changes)
        .extracting("type", "changes")
        .isEqualTo(
          listOf(
            "Updated bespoke conditions",
            listOf(
              mapOf(
                "type" to "REMOVED",
                "conditionText" to "Condition1",
              ),
            ),
          ),
        )
    }

    @Test
    fun `records an audit event when bespoke conditions are updated by adding and removing existing conditions at the same time`() {
      val anAddedBespokeCondition = listOf(
        "Condition2",
      )

      service.recordAuditEventUpdateBespokeConditions(
        aLicenceEntity,
        anAddedBespokeCondition,
        newBespokeConditions,
        aCom,
      )

      val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)
      verify(auditEventRepository, times(1)).save(auditCaptor.capture())

      assertThat(auditCaptor.value.licenceId).isEqualTo(aLicenceEntity.id)
      assertThat(auditCaptor.value.username).isEqualTo(aCom.username)
      assertThat(auditCaptor.value.summary)
        .isEqualTo(
          "Updated bespoke conditions for ${aLicenceEntity.forename} ${aLicenceEntity.surname}",
        )
      assertThat(auditCaptor.value.detail)
        .isEqualTo(
          "ID ${aLicenceEntity.id} type ${aLicenceEntity.typeCode.name} " +
            "status ${aLicenceEntity.statusCode.name} version ${aLicenceEntity.version}",
        )

      assertThat(auditCaptor.value.changes)
        .extracting("type", "changes")
        .isEqualTo(
          listOf(
            "Updated bespoke conditions",
            listOf(
              mapOf(
                "type" to "ADDED",
                "conditionText" to "Condition2",
              ),
              mapOf(
                "type" to "REMOVED",
                "conditionText" to "Condition1",
              ),
            ),
          ),
        )
    }

    @Test
    fun `does not record an audit event when no bespoke conditions are updated`() {
      service.recordAuditEventUpdateBespokeConditions(aLicenceEntity, emptyList(), emptyList(), aCom)

      verifyNoMoreInteractions(auditEventRepository)
    }

    @Test
    fun `records an audit event when the additional data associated with an additional condition is updated`() {
      val anAdditionalCondition = anAdditionalConditionEntity.copy(
        expandedConditionText = "updatedText",
      )

      service.recordAuditEventUpdateAdditionalConditionData(aLicenceEntity, anAdditionalCondition, aCom)

      val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)
      verify(auditEventRepository, times(1)).save(auditCaptor.capture())

      assertThat(auditCaptor.value.licenceId).isEqualTo(aLicenceEntity.id)
      assertThat(auditCaptor.value.username).isEqualTo(aCom.username)
      assertThat(auditCaptor.value.summary)
        .isEqualTo(
          "Updated additional condition data for ${aLicenceEntity.forename} ${aLicenceEntity.surname}",
        )
      assertThat(auditCaptor.value.detail)
        .isEqualTo(
          "ID ${aLicenceEntity.id} type ${aLicenceEntity.typeCode.name} " +
            "status ${aLicenceEntity.statusCode.name} version ${aLicenceEntity.version}",
        )

      assertThat(auditCaptor.value.changes)
        .extracting("type", "changes")
        .isEqualTo(
          listOf(
            "Updated additional condition data",
            listOf(
              mapOf(
                "type" to "ADDED",
                "conditionCode" to "code1",
                "conditionType" to "AP",
                "conditionText" to "updatedText",
              ),
            ),
          ),
        )
    }

    @Test
    fun `records an audit event with SYSTEM USER when the user is not staff member`() {
      val anAdditionalCondition = anAdditionalConditionEntity.copy(
        expandedConditionText = "updatedText",
      )

      service.recordAuditEventUpdateAdditionalConditionData(aLicenceEntity, anAdditionalCondition, null)

      val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)
      verify(auditEventRepository, times(1)).save(auditCaptor.capture())
      with(auditCaptor.value) {
        assertThat(licenceId).isEqualTo(aLicenceEntity.id)
        assertThat(username).isEqualTo("SYSTEM")
        assertThat(summary)
          .isEqualTo(
            "Updated additional condition data for ${aLicenceEntity.forename} ${aLicenceEntity.surname}",
          )
        assertThat(detail)
          .isEqualTo(
            "ID ${aLicenceEntity.id} type ${aLicenceEntity.typeCode.name} " +
              "status ${aLicenceEntity.statusCode.name} version ${aLicenceEntity.version}",
          )
        assertThat(changes)
          .extracting("type", "changes")
          .isEqualTo(
            listOf(
              "Updated additional condition data",
              listOf(
                mapOf(
                  "type" to "ADDED",
                  "conditionCode" to "code1",
                  "conditionType" to "AP",
                  "conditionText" to "updatedText",
                ),
              ),
            ),
          )
      }
    }
  }

  @Nested
  inner class `audit events for HDC curfew times` {
    @Test
    fun `records an audit event when curfew times are updated`() {
      service.recordAuditEventUpdateHdcCurfewTimes(aLicenceEntity, aSetOfCurfewTimes, aCom)

      val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)
      verify(auditEventRepository, times(1)).save(auditCaptor.capture())

      assertThat(auditCaptor.value.username).isEqualTo(aCom.username)
      assertThat(auditCaptor.value.summary)
        .isEqualTo(
          "Updated HDC curfew times for " +
            "${aLicenceEntity.forename} ${aLicenceEntity.surname}",
        )
      assertThat(auditCaptor.value.detail)
        .isEqualTo(
          "ID ${aLicenceEntity.id} type ${aLicenceEntity.typeCode.name} " +
            "status ${aLicenceEntity.statusCode.name} version ${aLicenceEntity.version}",
        )
      assertThat(auditCaptor.value.changes)
        .extracting("type", "changes")
        .isEqualTo(
          listOf(
            "Updated HDC curfew times",
            listOf(
              mapOf(
                "fromDay" to DayOfWeek.MONDAY,
                "fromTime" to LocalTime.of(20, 0),
                "untilDay" to DayOfWeek.TUESDAY,
                "untilTime" to LocalTime.of(8, 0),
              ),
              mapOf(
                "fromDay" to DayOfWeek.TUESDAY,
                "fromTime" to LocalTime.of(20, 0),
                "untilDay" to DayOfWeek.WEDNESDAY,
                "untilTime" to LocalTime.of(8, 0),
              ),
              mapOf(
                "fromDay" to DayOfWeek.WEDNESDAY,
                "fromTime" to LocalTime.of(20, 0),
                "untilDay" to DayOfWeek.THURSDAY,
                "untilTime" to LocalTime.of(8, 0),
              ),
              mapOf(
                "fromDay" to DayOfWeek.THURSDAY,
                "fromTime" to LocalTime.of(20, 0),
                "untilDay" to DayOfWeek.FRIDAY,
                "untilTime" to LocalTime.of(8, 0),
              ),
              mapOf(
                "fromDay" to DayOfWeek.FRIDAY,
                "fromTime" to LocalTime.of(20, 0),
                "untilDay" to DayOfWeek.SATURDAY,
                "untilTime" to LocalTime.of(8, 0),
              ),
              mapOf(
                "fromDay" to DayOfWeek.SATURDAY,
                "fromTime" to LocalTime.of(20, 0),
                "untilDay" to DayOfWeek.SUNDAY,
                "untilTime" to LocalTime.of(8, 0),
              ),
              mapOf(
                "fromDay" to DayOfWeek.SUNDAY,
                "fromTime" to LocalTime.of(20, 0),
                "untilDay" to DayOfWeek.MONDAY,
                "untilTime" to LocalTime.of(8, 0),
              ),
            ),
          ),
        )
    }
  }

  @Nested
  inner class `audit events for Electronic Monitoring Programme` {
    @Test
    fun `records an audit event when electronic monitoring programme details are updated`() {
      service.recordAuditEventUpdateElectronicMonitoringProgramme(
        aLicenceEntity,
        electronicMonitoringProgrammeRequest,
        aCom,
      )

      val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)
      verify(auditEventRepository, times(1)).save(auditCaptor.capture())

      assertThat(auditCaptor.value.username).isEqualTo(aCom.username)
      assertThat(auditCaptor.value.summary)
        .isEqualTo(
          "Updated electronic monitoring programme details for " +
            "${aLicenceEntity.forename} ${aLicenceEntity.surname}",
        )
      assertThat(auditCaptor.value.detail)
        .isEqualTo(
          "ID ${aLicenceEntity.id} type ${aLicenceEntity.typeCode.name} " +
            "status ${aLicenceEntity.statusCode.name} version ${aLicenceEntity.version}",
        )
      assertThat(auditCaptor.value.changes)
        .extracting("type", "changes")
        .isEqualTo(
          listOf(
            "Updated electronic monitoring programme details",
            mapOf(
              "isToBeTaggedForProgramme" to true,
              "programmeName" to "valid name",
            ),
          ),
        )
    }
  }

  @Nested
  inner class `audits events when COM updated` {
    @Test
    fun `records an audit event when the COM allocated to an offender is updated`() {
      val existingCom = communityOffenderManager()
      val updatedCom = anotherCommunityOffenderManager()
      val user = prisonUser()

      service.recordAuditEventComUpdated(
        aLicenceEntity,
        existingCom,
        updatedCom,
        user,
      )

      val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)
      verify(auditEventRepository).save(auditCaptor.capture())
      val auditEvent = auditCaptor.value
      assertThat(auditEvent.username).isEqualTo(user.username)

      assertThat(auditEvent.summary)
        .isEqualTo(
          "COM updated to ${updatedCom.firstName} ${updatedCom.lastName} on licence for ${aLicenceEntity.forename} ${aLicenceEntity.surname}",
        )
      assertThat(auditCaptor.value.detail)
        .isEqualTo(
          "ID ${aLicenceEntity.id} type ${aLicenceEntity.typeCode.name} " +
            "status ${aLicenceEntity.statusCode.name} version ${aLicenceEntity.version}",
        )
      assertThat(auditCaptor.value.changes)
        .extracting("type", "changes")
        .isEqualTo(
          listOf(
            "COM updated to ${updatedCom.firstName} ${updatedCom.lastName} on licence",
            mapOf(
              "newEmail" to updatedCom.email,
              "newStaffIdentifier" to updatedCom.staffIdentifier,
              "newUsername" to updatedCom.username,
              "oldEmail" to existingCom.email,
              "oldStaffIdentifier" to existingCom.staffIdentifier,
              "oldUsername" to existingCom.username,
            ),
          ),
        )
    }
  }

  @Nested
  inner class `audits events when probation team updated` {
    @Test
    fun `records an audit event when the the probation team on a licence is updated`() {
      val user = prisonUser()

      val updateTeamRequest = UpdateProbationTeamEvent(
        probationAreaCode = "N02",
        probationPduCode = "PDU2",
        probationLauCode = "LAU2",
        probationTeamCode = "TEAM2",
        probationTeamDescription = "team desc",
        probationAreaDescription = "area desc",
      )

      service.recordAuditEventProbationTeamUpdated(
        aLicenceEntity,
        updateTeamRequest,
        user,
      )

      val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)
      verify(auditEventRepository).save(auditCaptor.capture())
      val auditEvent = auditCaptor.value
      assertThat(auditEvent.username).isEqualTo(user.username)

      assertThat(auditEvent.summary)
        .isEqualTo(
          "Probation team updated to ${updateTeamRequest.probationTeamDescription} at ${updateTeamRequest.probationAreaDescription} on licence for ${aLicenceEntity.forename} ${aLicenceEntity.surname}",
        )
      assertThat(auditCaptor.value.detail)
        .isEqualTo(
          "ID ${aLicenceEntity.id} type ${aLicenceEntity.typeCode.name} " +
            "status ${aLicenceEntity.statusCode.name} version ${aLicenceEntity.version}",
        )
      assertThat(auditCaptor.value.changes)
        .extracting("type", "changes")
        .isEqualTo(
          listOf(
            "Probation team updated to team desc at area desc on licence",
            mapOf(
              "newAreaCode" to "N02",
              "newAreaDescription" to "area desc",
              "newLauCode" to "LAU2",
              "newLauDescription" to null,
              "newPduCode" to "PDU2",
              "newPduDescription" to null,
              "newTeamCode" to "TEAM2",
              "newTeamDescription" to "team desc",
              "oldAreaCode" to "N01",
              "oldAreaDescription" to "Wales",
              "oldLauCode" to "N01A2",
              "oldLauDescription" to "Cardiff South",
              "oldPduCode" to "N01A",
              "oldPduDescription" to "Cardiff",
              "oldTeamCode" to "NA01A2-A",
              "oldTeamDescription" to "Cardiff South Team A",
            ),
          ),
        )
    }
  }

  companion object {
    val anEvent = AuditEvent(
      licenceId = 1L,
      eventTime = LocalDateTime.now(),
      username = "USER",
      fullName = "Forename Surname",
      eventType = AuditEventType.USER_EVENT,
      summary = "Summary description",
      detail = "Detail description",
    )

    val aRequest = AuditRequest(
      username = "USER",
      licenceId = 1L,
      startTime = LocalDateTime.now().minusMonths(1),
      endTime = LocalDateTime.now(),
    )

    val aCom = communityOffenderManager()

    val aLicenceEntity = createCrdLicence()

    val aHdcLicenceEntity = createHdcLicence()

    val someAdditionalConditionData = mutableListOf(
      AdditionalConditionData(
        id = 1,
        dataField = "dataField",
        dataValue = "dataValue",
        additionalCondition = anAdditionalCondition(id = 1, licence = aLicenceEntity),
      ),
    )

    val someBespokeConditions = listOf(
      BespokeCondition(1, licence = aLicenceEntity, conditionText = "condition 1"),
    )

    val newBespokeConditions = listOf(
      "Condition1",
    )

    val anAdditionalConditionEntity = AdditionalCondition(
      id = 1,
      conditionVersion = "1.0",
      licence = aLicenceEntity,
      conditionCode = "code1",
      conditionCategory = "category1",
      conditionSequence = 4,
      conditionText = "text",
      additionalConditionData = someAdditionalConditionData,
      additionalConditionUpload = mutableListOf(),
      conditionType = "AP",
    )

    val electronicMonitoringProgrammeRequest = UpdateElectronicMonitoringProgrammeRequest(
      isToBeTaggedForProgramme = true,
      programmeName = "valid name",
    )

    val aPolicy = LicencePolicy(
      "2.1",
      standardConditions = StandardConditions(emptyList(), emptyList()),
      additionalConditions = AdditionalConditions(emptyList(), emptyList()),
      changeHints = emptyList(),
    )

    val aListOfEntities = listOf(
      EntityAuditEvent(
        id = 1L,
        licenceId = 1L,
        eventTime = LocalDateTime.now().minusDays(1L),
        username = "USER",
        fullName = "First Last",
        eventType = AuditEventType.USER_EVENT,
        summary = "Summary1",
        detail = "Detail1",
        changes = mapOf(
          "type" to "Added something",
          "changes" to mapOf(
            "type" to "ADDED",
            "conditionText" to "Something",
          ),
        ),
      ),
      EntityAuditEvent(
        id = 2L,
        licenceId = 1L,
        eventTime = LocalDateTime.now().minusDays(2L),
        username = "USER",
        fullName = "First Last",
        eventType = AuditEventType.USER_EVENT,
        summary = "Summary2",
        detail = "Detail2",
      ),
      EntityAuditEvent(
        id = 3L,
        licenceId = 1L,
        eventTime = LocalDateTime.now().minusDays(3L),
        username = "CUSER",
        fullName = "First Last",
        eventType = AuditEventType.SYSTEM_EVENT,
        summary = "Summary3",
        detail = "Detail3",
      ),
    )

    val aSetOfCurfewTimes =
      listOf(
        HdcCurfewTimes(
          1L,
          aHdcLicenceEntity,
          1,
          DayOfWeek.MONDAY,
          LocalTime.of(20, 0),
          DayOfWeek.TUESDAY,
          LocalTime.of(8, 0),
        ),
        HdcCurfewTimes(
          1L,
          aHdcLicenceEntity,
          2,
          DayOfWeek.TUESDAY,
          LocalTime.of(20, 0),
          DayOfWeek.WEDNESDAY,
          LocalTime.of(8, 0),
        ),
        HdcCurfewTimes(
          1L,
          aHdcLicenceEntity,
          3,
          DayOfWeek.WEDNESDAY,
          LocalTime.of(20, 0),
          DayOfWeek.THURSDAY,
          LocalTime.of(8, 0),
        ),
        HdcCurfewTimes(
          1L,
          aHdcLicenceEntity,
          4,
          DayOfWeek.THURSDAY,
          LocalTime.of(20, 0),
          DayOfWeek.FRIDAY,
          LocalTime.of(8, 0),
        ),
        HdcCurfewTimes(
          1L,
          aHdcLicenceEntity,
          5,
          DayOfWeek.FRIDAY,
          LocalTime.of(20, 0),
          DayOfWeek.SATURDAY,
          LocalTime.of(8, 0),
        ),
        HdcCurfewTimes(
          1L,
          aHdcLicenceEntity,
          6,
          DayOfWeek.SATURDAY,
          LocalTime.of(20, 0),
          DayOfWeek.SUNDAY,
          LocalTime.of(8, 0),
        ),
        HdcCurfewTimes(
          1L,
          aHdcLicenceEntity,
          7,
          DayOfWeek.SUNDAY,
          LocalTime.of(20, 0),
          DayOfWeek.MONDAY,
          LocalTime.of(8, 0),
        ),
      )
  }
}
