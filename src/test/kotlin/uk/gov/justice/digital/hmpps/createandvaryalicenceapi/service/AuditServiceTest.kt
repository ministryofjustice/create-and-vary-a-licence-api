package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AuditRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.AdditionalConditions
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.LicencePolicy
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.StandardConditions
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StaffRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AuditEventType
import java.lang.IllegalStateException
import java.time.LocalDateTime
import java.util.Optional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AuditEvent as EntityAuditEvent

class AuditServiceTest {
  private val licenceRepository = mock<LicenceRepository>()
  private val auditEventRepository = mock<AuditEventRepository>()
  private val staffRepository = mock<StaffRepository>()

  private val service = AuditService(auditEventRepository, licenceRepository, staffRepository)

  @BeforeEach
  fun reset() {
    val authentication = mock<Authentication>()
    val securityContext = mock<SecurityContext>()
    whenever(authentication.name).thenReturn("smills")
    whenever(securityContext.authentication).thenReturn(authentication)

    SecurityContextHolder.setContext(securityContext)

    reset(auditEventRepository, licenceRepository, staffRepository)
    whenever(staffRepository.findByUsernameIgnoreCase("smills")).thenReturn(aCom)
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
    whenever(
      auditEventRepository
        .findAllByEventTimeBetweenOrderByEventTimeDesc(
          aUserRequest.startTime,
          aUserRequest.endTime,
        ),
    ).thenReturn(aListOfEntities)

    val response = service.getAuditEvents(aUserRequest)

    assertThat(response).hasSize(3)
    assertThat(response[0].summary).isEqualTo("Summary1")

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
        "newValue" to "Bob Robertson",
      ),
    )

    val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)
    verify(auditEventRepository, times(1)).save(auditCaptor.capture())

    assertThat(auditCaptor.value.username).isEqualTo("smills")
    assertThat(auditCaptor.value.summary).isEqualTo("Updated initial appointment details for ${aLicenceEntity.forename} ${aLicenceEntity.surname}")
    assertThat(auditCaptor.value.detail).isEqualTo(
      "ID ${aLicenceEntity.id} type ${aLicenceEntity.typeCode.name} " +
        "status ${aLicenceEntity.statusCode.name} version ${aLicenceEntity.version}",
    )
    assertThat(auditCaptor.value.changes).isEqualTo(
      mapOf(
        "field" to "appointmentPerson",
        "previousValue" to "Joe Bloggs",
        "newValue" to "Bob Robertson",
      ),
    )
  }

  @Nested
  inner class `audit events for licence conditions` {
    @Test
    fun `records an audit event when standard conditions are updated`() {
      service.recordAuditEventUpdateStandardCondition(aLicenceEntity, aPolicy.version)

      val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)
      verify(auditEventRepository, times(1)).save(auditCaptor.capture())

      assertThat(auditCaptor.value.username).isEqualTo("smills")
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
      )

      val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)
      verify(auditEventRepository, times(1)).save(auditCaptor.capture())

      assertThat(auditCaptor.value.licenceId).isEqualTo(aLicenceEntity.id)
      assertThat(auditCaptor.value.username).isEqualTo("smills")
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
      )

      val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)
      verify(auditEventRepository, times(1)).save(auditCaptor.capture())

      assertThat(auditCaptor.value.licenceId).isEqualTo(aLicenceEntity.id)
      assertThat(auditCaptor.value.username).isEqualTo("smills")
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
      service.recordAuditEventDeleteStandardConditions(aLicenceEntity, emptyList())

      verifyNoMoreInteractions(auditEventRepository)
    }

    @Test
    fun `records no audit event when no bespoke conditions are removed`() {
      service.recordAuditEventDeleteBespokeConditions(aLicenceEntity, emptyList())

      verifyNoMoreInteractions(auditEventRepository)
    }

    @Test
    fun `records an audit event when an additional condition of the same type is added`() {
      service.recordAuditEventAddAdditionalConditionOfSameType(aLicenceEntity, anAdditionalConditionEntity)

      val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)
      verify(auditEventRepository, times(1)).save(auditCaptor.capture())

      assertThat(auditCaptor.value.licenceId).isEqualTo(aLicenceEntity.id)
      assertThat(auditCaptor.value.username).isEqualTo("smills")
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

      service.recordAuditEventDeleteAdditionalConditions(aLicenceEntity, aDeletedCondition)

      val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)
      verify(auditEventRepository, times(1)).save(auditCaptor.capture())

      assertThat(auditCaptor.value.licenceId).isEqualTo(aLicenceEntity.id)
      assertThat(auditCaptor.value.username).isEqualTo("smills")
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
          additionalConditionData = emptyList(),
        ),
      )

      service.recordAuditEventUpdateAdditionalConditions(
        aLicenceEntity,
        anAddedAdditionalCondition,
        emptyList(),
      )

      val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)
      verify(auditEventRepository, times(1)).save(auditCaptor.capture())

      assertThat(auditCaptor.value.licenceId).isEqualTo(aLicenceEntity.id)
      assertThat(auditCaptor.value.username).isEqualTo("smills")
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
      )

      val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)
      verify(auditEventRepository, times(1)).save(auditCaptor.capture())

      assertThat(auditCaptor.value.licenceId).isEqualTo(aLicenceEntity.id)
      assertThat(auditCaptor.value.username).isEqualTo("smills")
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
          additionalConditionData = emptyList(),
        ),
      )

      val aRemovedAdditionalCondition = listOf(
        anAdditionalConditionEntity,
      )

      service.recordAuditEventUpdateAdditionalConditions(
        aLicenceEntity,
        anAddedAddtionalCondition,
        aRemovedAdditionalCondition,
      )

      val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)
      verify(auditEventRepository, times(1)).save(auditCaptor.capture())

      assertThat(auditCaptor.value.licenceId).isEqualTo(aLicenceEntity.id)
      assertThat(auditCaptor.value.username).isEqualTo("smills")
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
      service.recordAuditEventUpdateAdditionalConditions(aLicenceEntity, emptyList(), emptyList())

      verifyNoMoreInteractions(auditEventRepository)
    }

    @Test
    fun `records an audit event when bespoke conditions are updated by adding new conditions`() {
      service.recordAuditEventUpdateBespokeConditions(aLicenceEntity, newBespokeConditions, emptyList())

      val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)
      verify(auditEventRepository, times(1)).save(auditCaptor.capture())

      assertThat(auditCaptor.value.licenceId).isEqualTo(aLicenceEntity.id)
      assertThat(auditCaptor.value.username).isEqualTo("smills")
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
      service.recordAuditEventUpdateBespokeConditions(aLicenceEntity, emptyList(), newBespokeConditions)

      val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)
      verify(auditEventRepository, times(1)).save(auditCaptor.capture())

      assertThat(auditCaptor.value.licenceId).isEqualTo(aLicenceEntity.id)
      assertThat(auditCaptor.value.username).isEqualTo("smills")
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
      )

      val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)
      verify(auditEventRepository, times(1)).save(auditCaptor.capture())

      assertThat(auditCaptor.value.licenceId).isEqualTo(aLicenceEntity.id)
      assertThat(auditCaptor.value.username).isEqualTo("smills")
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
      service.recordAuditEventUpdateBespokeConditions(aLicenceEntity, emptyList(), emptyList())

      verifyNoMoreInteractions(auditEventRepository)
    }

    @Test
    fun `records an audit event when the additional data associated with an additional condition is updated`() {
      val anAdditionalCondition = anAdditionalConditionEntity.copy(
        expandedConditionText = "updatedText",
      )

      service.recordAuditEventUpdateAdditionalConditionData(aLicenceEntity, anAdditionalCondition)

      val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)
      verify(auditEventRepository, times(1)).save(auditCaptor.capture())

      assertThat(auditCaptor.value.licenceId).isEqualTo(aLicenceEntity.id)
      assertThat(auditCaptor.value.username).isEqualTo("smills")
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

      whenever(staffRepository.findByUsernameIgnoreCase("smills")).thenReturn(null)
      service.recordAuditEventUpdateAdditionalConditionData(aLicenceEntity, anAdditionalCondition)

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
  inner class `createUserAuditEvent` {
    @Test
    fun `throws an error if a user is not present`() {
      whenever(staffRepository.findByUsernameIgnoreCase("smills")).thenReturn(null)

      val error = assertThrows<IllegalStateException> {
        service.recordAuditEventInitialAppointmentUpdate(
          aLicenceEntity,
          mapOf(
            "field" to "appointmentPerson",
            "previousValue" to "Joe Bloggs",
            "newValue" to "John Smith",
          ),
        )
      }

      assertThat(error.message).isEqualTo("User not found when creating audit event Updated initial appointment details for licence ${aLicenceEntity.id}")
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

    val aCom = CommunityOffenderManager(
      staffIdentifier = 2000,
      username = "smills",
      email = "testemail@probation.gov.uk",
      firstName = "X",
      lastName = "Y",
    )

    val aLicenceEntity = TestData.createCrdLicence()

    val someAdditionalConditionData = listOf(
      AdditionalConditionData(
        id = 1,
        dataField = "dataField",
        dataValue = "dataValue",
        additionalCondition = AdditionalCondition(licence = aLicenceEntity, conditionVersion = "1.0"),
      ),
    )

    val someBespokeConditions = listOf(
      BespokeCondition(1, licence = aLicenceEntity).copy(conditionText = "condition 1"),
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
      additionalConditionUploadSummary = emptyList(),
      conditionType = "AP",
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
  }
}
