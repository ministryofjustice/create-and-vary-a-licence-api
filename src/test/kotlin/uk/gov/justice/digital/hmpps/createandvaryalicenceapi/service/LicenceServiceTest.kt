package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anyList
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.firstValue
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.mapping.PropertyReferenceException
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HardStopLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.LicenceEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.OmuContact
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.PrisonCaseAdministrator
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.VariationLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.StatusUpdateRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.AdditionalConditionAp
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.AdditionalConditions
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.AllAdditionalConditions
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.LicencePolicy
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.StandardConditions
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.NotifyRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.ReferVariationRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdatePrisonInformationRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateReasonForVariationRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateSpoDiscussionRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateVloDiscussionRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionUploadDetailRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.BespokeConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceQueryObject
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StaffRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StandardConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createCrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createHardStopLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.DomainEventsService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.POLICY_V2_1
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AuditEventType.SYSTEM_EVENT
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AuditEventType.USER_EVENT
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AuditEvent as EntityAuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence as EntityLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.LicenceEvent as EntityLicenceEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.StandardCondition as EntityStandardCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CrdLicence as CrdLicenceModel

class LicenceServiceTest {
  private val standardConditionRepository = mock<StandardConditionRepository>()
  private val additionalConditionRepository = mock<AdditionalConditionRepository>()
  private val bespokeConditionRepository = mock<BespokeConditionRepository>()
  private val licenceRepository = mock<LicenceRepository>()
  private val staffRepository = mock<StaffRepository>()
  private val licenceEventRepository = mock<LicenceEventRepository>()
  private val additionalConditionUploadDetailRepository = mock<AdditionalConditionUploadDetailRepository>()
  private val licencePolicyService = mock<LicencePolicyService>()
  private val auditEventRepository = mock<AuditEventRepository>()
  private val notifyService = mock<NotifyService>()
  private val omuService = mock<OmuService>()
  private val releaseDateService = mock<ReleaseDateService>()
  private val domainEventsService = mock<DomainEventsService>()

  private val service =
    LicenceService(
      licenceRepository,
      staffRepository,
      standardConditionRepository,
      additionalConditionRepository,
      bespokeConditionRepository,
      licenceEventRepository,
      licencePolicyService,
      additionalConditionUploadDetailRepository,
      auditEventRepository,
      notifyService,
      omuService,
      releaseDateService,
      domainEventsService,
    )

  @BeforeEach
  fun reset() {
    val authentication = mock<Authentication>()
    val securityContext = mock<SecurityContext>()

    whenever(authentication.name).thenReturn("smills")
    whenever(securityContext.authentication).thenReturn(authentication)
    SecurityContextHolder.setContext(securityContext)
    whenever(licencePolicyService.policyByVersion(any())).thenReturn(POLICY_V2_1)

    reset(
      licenceRepository,
      standardConditionRepository,
      bespokeConditionRepository,
      licenceEventRepository,
      additionalConditionUploadDetailRepository,
      auditEventRepository,
      notifyService,
      omuService,
      releaseDateService,
      domainEventsService,
    )
  }

  @Test
  fun `service returns a licence by ID`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))
    whenever(licencePolicyService.getAllAdditionalConditions()).thenReturn(
      AllAdditionalConditions(mapOf("2.1" to mapOf("code" to anAdditionalCondition))),
    )

    val licence = service.getLicenceById(1L)

    assertThat(licence).isExactlyInstanceOf(CrdLicenceModel::class.java)

    verify(licenceRepository, times(1)).findById(1L)
  }

  @Test
  fun `service returns a licence with the full name of the user who created it`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))
    whenever(licencePolicyService.getAllAdditionalConditions()).thenReturn(
      AllAdditionalConditions(mapOf("2.1" to mapOf("code" to anAdditionalCondition))),
    )

    val licence = service.getLicenceById(1L)

    assertThat(licence.createdByFullName).isEqualTo("X Y")
  }

  @Test
  fun `service returns a licence with is hard stop populated`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))
    whenever(licencePolicyService.getAllAdditionalConditions()).thenReturn(
      AllAdditionalConditions(mapOf("2.1" to mapOf("code" to anAdditionalCondition))),
    )
    whenever(releaseDateService.isInHardStopPeriod(any(), anyOrNull())).thenReturn(true)

    val licence = service.getLicenceById(1L) as CrdLicenceModel

    assertThat(licence.isInHardStopPeriod).isTrue()
  }

  @Test
  fun `service transforms key fields of a licence object correctly`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))
    whenever(licencePolicyService.getAllAdditionalConditions()).thenReturn(
      AllAdditionalConditions(mapOf("2.1" to mapOf("code" to anAdditionalCondition))),
    )

    val licence = service.getLicenceById(1L)

    assertThat(licence.cro).isEqualTo(aLicenceEntity.cro)
    assertThat(licence.nomsId).isEqualTo(aLicenceEntity.nomsId)
    assertThat(licence.bookingId).isEqualTo(aLicenceEntity.bookingId)
    assertThat(licence.pnc).isEqualTo(aLicenceEntity.pnc)

    verify(licenceRepository, times(1)).findById(1L)
  }

  @Test
  fun `service throws a not found exception for unknown ID`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.empty())

    val exception = assertThrows<EntityNotFoundException> {
      service.getLicenceById(1L)
    }

    assertThat(exception).isInstanceOf(EntityNotFoundException::class.java)

    verify(licenceRepository, times(1)).findById(1L)
  }

  @Test
  fun `find licences matching criteria - no parameters matches all`() {
    val licenceQueryObject = LicenceQueryObject()
    whenever(licenceRepository.findAll(any<Specification<EntityLicence>>(), any<Sort>())).thenReturn(
      listOf(
        aLicenceEntity,
      ),
    )

    val licenceSummaries = service.findLicencesMatchingCriteria(licenceQueryObject)

    assertThat(licenceSummaries).isEqualTo(listOf(aLicenceSummary))
    verify(licenceRepository, times(1)).findAll(any<Specification<EntityLicence>>(), eq(Sort.unsorted()))
  }

  @Test
  fun `find licences matching criteria - multiple parameters`() {
    val licenceQueryObject = LicenceQueryObject(
      prisonCodes = listOf("MDI"),
      statusCodes = listOf(LicenceStatus.APPROVED),
      staffIds = listOf(1, 2, 3),
      nomsIds = listOf("A1234AA"),
    )
    whenever(licenceRepository.findAll(any<Specification<EntityLicence>>(), any<Sort>())).thenReturn(
      listOf(
        aLicenceEntity,
      ),
    )

    val licenceSummaries = service.findLicencesMatchingCriteria(licenceQueryObject)

    assertThat(licenceSummaries).isEqualTo(listOf(aLicenceSummary))
    verify(licenceRepository, times(1)).findAll(any<Specification<EntityLicence>>(), eq(Sort.unsorted()))
  }

  @Test
  fun `find licences matching criteria - list of PDUs`() {
    val licenceQueryObject = LicenceQueryObject(pdus = listOf("A", "B"))
    whenever(licenceRepository.findAll(any<Specification<EntityLicence>>(), any<Sort>())).thenReturn(
      listOf(
        aLicenceEntity,
      ),
    )

    val licenceSummaries = service.findLicencesMatchingCriteria(licenceQueryObject)

    assertThat(licenceSummaries).isEqualTo(listOf(aLicenceSummary))
    verify(licenceRepository, times(1)).findAll(any<Specification<EntityLicence>>(), eq(Sort.unsorted()))
  }

  @Test
  fun `find licences matching criteria - sorted`() {
    val licenceQueryObject = LicenceQueryObject(sortBy = "conditionalReleaseDate", sortOrder = "DESC")
    whenever(licenceRepository.findAll(any<Specification<EntityLicence>>(), any<Sort>())).thenReturn(
      listOf(
        aLicenceEntity,
      ),
    )

    val licenceSummaries = service.findLicencesMatchingCriteria(licenceQueryObject)

    assertThat(licenceSummaries).isEqualTo(listOf(aLicenceSummary))
    verify(licenceRepository, times(1)).findAll(
      any<Specification<EntityLicence>>(),
      eq(Sort.by(Sort.Direction.DESC, "conditionalReleaseDate")),
    )
  }

  @Test
  fun `find licences matching criteria - unknown property`() {
    val licenceQueryObject = LicenceQueryObject(sortBy = "unknown", sortOrder = "DESC")
    whenever(
      licenceRepository.findAll(
        any<Specification<EntityLicence>>(),
        any<Sort>(),
      ),
    ).thenThrow(mock<PropertyReferenceException>())

    assertThrows<ValidationException> {
      service.findLicencesMatchingCriteria(licenceQueryObject)
    }
  }

  @Test
  fun `find licences matching criteria - unknown sort order`() {
    val licenceQueryObject = LicenceQueryObject(sortBy = "conditionalReleaseDate", sortOrder = "unknown")
    whenever(licenceRepository.findAll(any<Specification<EntityLicence>>(), any<Sort>())).thenReturn(
      listOf(
        aLicenceEntity,
      ),
    )

    assertThrows<ValidationException> {
      service.findLicencesMatchingCriteria(licenceQueryObject)
    }
  }

  @Test
  fun `find recently approved licences matching criteria - returns recently approved licences`() {
    whenever(licenceRepository.getRecentlyApprovedLicences(any(), any())).thenReturn(
      listOf(
        aRecentlyApprovedLicence,
      ),
    )

    val licenceSummaries = service.findRecentlyApprovedLicences(emptyList())

    assertThat(licenceSummaries).isEqualTo(listOf(aRecentlyApprovedLicenceSummary))
    verify(licenceRepository, times(1)).getRecentlyApprovedLicences(
      anyList(),
      any<LocalDate>(),
    )
  }

  @Test
  fun `find recently approved licences matching criteria - returns the original licence for an active variation`() {
    val aRecentlyApprovedLicence = TestData.createCrdLicence().copy(
      id = 1,
      actualReleaseDate = LocalDate.now().minusDays(1),
      conditionalReleaseDate = LocalDate.now(),
      approvedByName = "jim smith",
      approvedDate = LocalDateTime.of(2023, 9, 19, 16, 38, 42),
    )

    val activeVariationLicence = TestData.createVariationLicence().copy(
      id = aRecentlyApprovedLicence.id + 1,
      statusCode = LicenceStatus.ACTIVE,
      variationOfId = aRecentlyApprovedLicence.id,
    )

    whenever(licenceRepository.findById(aRecentlyApprovedLicence.id)).thenReturn(
      Optional.of(
        aRecentlyApprovedLicence,
      ),
    )
    whenever(
      licenceRepository.getRecentlyApprovedLicences(
        anyList(),
        any<LocalDate>(),
      ),
    ).thenReturn(
      listOf(
        activeVariationLicence,
      ),
    )

    val licenceSummaries = service.findRecentlyApprovedLicences(emptyList())

    assertThat(licenceSummaries).isEqualTo(
      listOf(
        aRecentlyApprovedLicenceSummary.copy(
          surname = activeVariationLicence.surname,
          forename = activeVariationLicence.forename,
        ),
      ),
    )
    verify(licenceRepository, times(1)).getRecentlyApprovedLicences(anyList(), any<LocalDate>())
  }

  @Test
  fun `update licence status persists the licence and history correctly`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))

    service.updateLicenceStatus(
      1L,
      StatusUpdateRequest(status = LicenceStatus.REJECTED, username = "X", fullName = "Y"),
    )

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())
    verify(licenceEventRepository, times(0)).saveAndFlush(any())

    assertThat(licenceCaptor.value)
      .extracting("id", "statusCode", "updatedByUsername", "licenceActivatedDate")
      .isEqualTo(listOf(1L, LicenceStatus.REJECTED, "X", null))

    assertThat(auditCaptor.value)
      .extracting("licenceId", "username", "fullName", "summary", "eventType")
      .isEqualTo(
        listOf(
          1L,
          "X",
          "Y",
          "Licence rejected for ${aLicenceEntity.forename} ${aLicenceEntity.surname}",
          USER_EVENT,
        ),
      )
  }

  @Test
  fun `update licence status to APPROVED sets additional values`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))

    service.updateLicenceStatus(
      1L,
      StatusUpdateRequest(status = LicenceStatus.APPROVED, username = "X", fullName = "Y"),
    )

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    val eventCaptor = ArgumentCaptor.forClass(EntityLicenceEvent::class.java)
    val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(licenceEventRepository, times(1)).saveAndFlush(eventCaptor.capture())
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())
    verify(notifyService, times(0)).sendVariationForReApprovalEmail(any(), any(), any(), any(), any())

    assertThat(licenceCaptor.value)
      .extracting("id", "statusCode", "approvedByUsername", "approvedByName", "licenceActivatedDate")
      .isEqualTo(listOf(1L, LicenceStatus.APPROVED, "X", "Y", null))

    assertThat(licenceCaptor.value.approvedDate).isAfter(LocalDateTime.now().minusMinutes(5L))

    assertThat(eventCaptor.value)
      .extracting("licenceId", "eventType", "eventDescription")
      .isEqualTo(
        listOf(
          1L,
          LicenceEventType.APPROVED,
          "Licence updated to APPROVED for ${aLicenceEntity.forename} ${aLicenceEntity.surname}",
        ),
      )

    assertThat(auditCaptor.value)
      .extracting("licenceId", "username", "fullName", "summary", "eventType")
      .isEqualTo(
        listOf(
          1L,
          "X",
          "Y",
          "Licence approved for ${aLicenceEntity.forename} ${aLicenceEntity.surname}",
          USER_EVENT,
        ),
      )
  }

  @Test
  fun `update licence status to APPROVED deactivates previous version of licence`() {
    val newLicenceId = 23L
    val username = "user"
    val fullName = "user 1"
    val firstVersionOfLicence = aLicenceEntity.copy(
      statusCode = LicenceStatus.APPROVED,
      approvedByUsername = username,
      approvedByName = fullName,
      approvedDate = LocalDateTime.now(),
    )
    val newVersionOfLicence =
      aLicenceEntity.copy(
        id = newLicenceId,
        statusCode = LicenceStatus.IN_PROGRESS,
        versionOfId = aLicenceEntity.id,
      )

    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(firstVersionOfLicence))
    whenever(licenceRepository.findById(newLicenceId)).thenReturn(Optional.of(newVersionOfLicence))

    service.updateLicenceStatus(
      newLicenceId,
      StatusUpdateRequest(status = LicenceStatus.APPROVED, username = username, fullName = fullName),
    )

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    val eventCaptor = ArgumentCaptor.forClass(EntityLicenceEvent::class.java)
    val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)

    verify(licenceRepository, times(2)).saveAndFlush(licenceCaptor.capture())
    verify(licenceEventRepository, times(2)).saveAndFlush(eventCaptor.capture())
    verify(auditEventRepository, times(2)).saveAndFlush(auditCaptor.capture())
    verify(notifyService, never()).sendVariationForReApprovalEmail(any(), any(), any(), any(), any())

    assertThat(licenceCaptor.allValues[0])
      .extracting("id", "statusCode", "updatedByUsername", "licenceActivatedDate")
      .isEqualTo(listOf(firstVersionOfLicence.id, LicenceStatus.INACTIVE, username, null))

    assertThat(licenceCaptor.allValues[1])
      .extracting("id", "statusCode", "approvedByUsername", "approvedByName")
      .isEqualTo(listOf(newVersionOfLicence.id, LicenceStatus.APPROVED, username, fullName))
    assertThat(licenceCaptor.allValues[1].approvedDate).isAfter(LocalDateTime.now().minusMinutes(5L))

    assertThat(eventCaptor.allValues[0])
      .extracting("licenceId", "eventType", "eventDescription")
      .isEqualTo(
        listOf(
          firstVersionOfLicence.id,
          LicenceEventType.SUPERSEDED,
          "Licence deactivated as a newer version was approved for ${aLicenceEntity.forename} ${aLicenceEntity.surname}",
        ),
      )

    assertThat(eventCaptor.allValues[1])
      .extracting("licenceId", "eventType", "eventDescription")
      .isEqualTo(
        listOf(
          newVersionOfLicence.id,
          LicenceEventType.APPROVED,
          "Licence updated to APPROVED for ${newVersionOfLicence.forename} ${newVersionOfLicence.surname}",
        ),
      )

    assertThat(auditCaptor.allValues[0])
      .extracting("licenceId", "username", "fullName", "summary", "eventType")
      .isEqualTo(
        listOf(
          firstVersionOfLicence.id,
          username,
          fullName,
          "Licence set to INACTIVE for ${aLicenceEntity.forename} ${aLicenceEntity.surname}",
          USER_EVENT,
        ),
      )

    assertThat(auditCaptor.allValues[1])
      .extracting("licenceId", "username", "fullName", "summary", "eventType")
      .isEqualTo(
        listOf(
          newVersionOfLicence.id,
          username,
          fullName,
          "Licence approved for ${aLicenceEntity.forename} ${aLicenceEntity.surname}",
          USER_EVENT,
        ),
      )
  }

  @Test
  fun `update an APPROVED licence back to IN_PROGRESS clears the approval fields`() {
    whenever(licenceRepository.findById(1L))
      .thenReturn(
        Optional.of(
          aLicenceEntity.copy(
            statusCode = LicenceStatus.APPROVED,
            approvedByUsername = "X",
            approvedByName = "Y",
          ),
        ),
      )

    whenever(omuService.getOmuContactEmail(any())).thenReturn(
      OmuContact(
        prisonCode = aLicenceEntity.prisonCode!!,
        email = "test@OMU.testing.com",
        dateCreated = LocalDateTime.now(),
      ),
    )

    service.updateLicenceStatus(
      1L,
      StatusUpdateRequest(status = LicenceStatus.IN_PROGRESS, username = "X", fullName = "Y"),
    )

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())
    verify(licenceEventRepository, times(0)).saveAndFlush(any())
    verify(notifyService, times(1)).sendVariationForReApprovalEmail(
      eq("test@OMU.testing.com"),
      eq(aLicenceEntity.forename ?: "unknown"),
      eq(aLicenceEntity.surname ?: "unknown"),
      eq(aLicenceEntity.nomsId),
      any(),
    )

    assertThat(licenceCaptor.value)
      .extracting(
        "id",
        "statusCode",
        "updatedByUsername",
        "approvedByUsername",
        "approvedDate",
        "licenceActivatedDate",
      )
      .isEqualTo(listOf(1L, LicenceStatus.IN_PROGRESS, "X", null, null, null))

    assertThat(auditCaptor.value)
      .extracting("licenceId", "username", "fullName", "summary")
      .isEqualTo(listOf(1L, "X", "Y", "Licence edited for ${aLicenceEntity.forename} ${aLicenceEntity.surname}"))
  }

  @Test
  fun `update an APPROVED licence back to IN_PROGRESS does not call notify if no OMU contact is found`() {
    whenever(licenceRepository.findById(1L))
      .thenReturn(
        Optional.of(
          aLicenceEntity.copy(
            statusCode = LicenceStatus.APPROVED,
            approvedByUsername = "X",
            approvedByName = "Y",
          ),
        ),
      )

    whenever(omuService.getOmuContactEmail(any())).thenReturn(null)

    service.updateLicenceStatus(
      1L,
      StatusUpdateRequest(status = LicenceStatus.IN_PROGRESS, username = "X", fullName = "Y"),
    )

    verify(notifyService, times(0)).sendVariationForReApprovalEmail(
      eq("test@OMU.testing.com"),
      eq(aLicenceEntity.forename ?: ""),
      eq(aLicenceEntity.surname ?: ""),
      eq(aLicenceEntity.nomsId),
      any(),
    )
  }

  @Test
  fun `update licenceActivatedDate field when licence status set to ACTIVE`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))

    service.updateLicenceStatus(
      1L,
      StatusUpdateRequest(status = LicenceStatus.ACTIVE, username = "X", fullName = "Y"),
    )

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())
    verify(domainEventsService, times(1)).recordDomainEvent(licenceCaptor.value, LicenceStatus.ACTIVE)

    assertThat(licenceCaptor.value.licenceActivatedDate).isNotNull()

    assertThat(licenceCaptor.value)
      .extracting("id", "statusCode", "updatedByUsername", "licenceActivatedDate")
      .isEqualTo(listOf(1L, LicenceStatus.ACTIVE, "X", licenceCaptor.value.licenceActivatedDate))

    assertThat(auditCaptor.value)
      .extracting("licenceId", "username", "fullName", "summary", "eventType")
      .isEqualTo(
        listOf(
          1L,
          "X",
          "Y",
          "Licence set to ACTIVE for ${aLicenceEntity.forename} ${aLicenceEntity.surname}",
          USER_EVENT,
        ),
      )
  }

  @Test
  fun `update licence status to ACTIVE deactivates any in progress version of licence`() {
    val inProgressLicenceVersion =
      aLicenceEntity.copy(id = 99999, statusCode = LicenceStatus.SUBMITTED, versionOfId = aLicenceEntity.id)

    whenever(licenceRepository.findById(aLicenceEntity.id)).thenReturn(Optional.of(aLicenceEntity))
    whenever(
      licenceRepository.findAllByVersionOfIdInAndStatusCodeIn(
        listOf(aLicenceEntity.id),
        listOf(LicenceStatus.IN_PROGRESS, LicenceStatus.SUBMITTED),
      ),
    ).thenReturn(listOf(inProgressLicenceVersion))

    service.updateLicenceStatus(
      1L,
      StatusUpdateRequest(status = LicenceStatus.ACTIVE, username = "X", fullName = "Y"),
    )

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    val inProgressLicenceCaptor = argumentCaptor<List<EntityLicence>>()
    val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(licenceRepository, times(1)).saveAllAndFlush(inProgressLicenceCaptor.capture())
    verify(auditEventRepository, times(2)).saveAndFlush(auditCaptor.capture())
    verify(domainEventsService, times(1)).recordDomainEvent(licenceCaptor.value, LicenceStatus.ACTIVE)

    assertThat(licenceCaptor.value.licenceActivatedDate).isNotNull()

    assertThat(licenceCaptor.value)
      .extracting("id", "statusCode", "updatedByUsername", "licenceActivatedDate")
      .isEqualTo(listOf(1L, LicenceStatus.ACTIVE, "X", licenceCaptor.value.licenceActivatedDate))
    assertThat(inProgressLicenceCaptor.firstValue[0])
      .extracting("id", "statusCode")
      .isEqualTo(listOf(inProgressLicenceVersion.id, LicenceStatus.INACTIVE))

    assertThat(auditCaptor.allValues[0])
      .extracting("licenceId", "username", "fullName", "summary", "eventType")
      .isEqualTo(
        listOf(
          inProgressLicenceVersion.id,
          "SYSTEM",
          "SYSTEM",
          "Deactivating licence as the parent licence version was activated for Bob Mortimer",
          SYSTEM_EVENT,
        ),
      )
    assertThat(auditCaptor.allValues[1]).extracting("licenceId", "username", "fullName", "summary", "eventType")
      .isEqualTo(
        listOf(
          aLicenceEntity.id,
          "X",
          "Y",
          "Licence set to ACTIVE for ${aLicenceEntity.forename} ${aLicenceEntity.surname}",
          USER_EVENT,
        ),
      )
  }

  @Test
  fun `updating licence status to INACTIVE deactivates any in progress version of the licence`() {
    val inProgressLicenceVersion =
      aLicenceEntity.copy(id = 99999, statusCode = LicenceStatus.SUBMITTED, versionOfId = aLicenceEntity.id)
    whenever(licenceRepository.findById(aLicenceEntity.id)).thenReturn(Optional.of(aLicenceEntity))
    whenever(
      licenceRepository.findAllByVersionOfIdInAndStatusCodeIn(
        listOf(aLicenceEntity.id),
        listOf(LicenceStatus.IN_PROGRESS, LicenceStatus.SUBMITTED),
      ),
    ).thenReturn(listOf(inProgressLicenceVersion))

    service.updateLicenceStatus(
      1L,
      StatusUpdateRequest(status = LicenceStatus.INACTIVE, username = "X", fullName = "Y"),
    )

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    val inProgressLicenceCaptor = argumentCaptor<List<EntityLicence>>()
    val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(licenceRepository, times(1)).saveAllAndFlush(inProgressLicenceCaptor.capture())
    verify(auditEventRepository, times(2)).saveAndFlush(auditCaptor.capture())
    verify(domainEventsService, times(1)).recordDomainEvent(licenceCaptor.value, LicenceStatus.INACTIVE)

    assertThat(licenceCaptor.value)
      .extracting("id", "statusCode", "updatedByUsername", "licenceActivatedDate")
      .isEqualTo(listOf(aLicenceEntity.id, LicenceStatus.INACTIVE, "X", licenceCaptor.value.licenceActivatedDate))
    assertThat(inProgressLicenceCaptor.firstValue[0])
      .extracting("id", "statusCode")
      .isEqualTo(listOf(inProgressLicenceVersion.id, LicenceStatus.INACTIVE))

    assertThat(auditCaptor.allValues[0]).extracting("licenceId", "username", "fullName", "summary", "eventType")
      .isEqualTo(
        listOf(
          inProgressLicenceVersion.id,
          "SYSTEM",
          "SYSTEM",
          "Deactivating licence as the parent licence version was deactivated for ${inProgressLicenceVersion.forename} ${inProgressLicenceVersion.surname}",
          SYSTEM_EVENT,
        ),
      )

    assertThat(auditCaptor.allValues[1]).extracting("licenceId", "username", "fullName", "summary", "eventType")
      .isEqualTo(
        listOf(
          1L,
          "X",
          "Y",
          "Licence set to INACTIVE for ${aLicenceEntity.forename} ${aLicenceEntity.surname}",
          USER_EVENT,
        ),
      )
  }

  @Test
  fun `licenceActivatedDate field should not be null if it already has value and status is not ACTIVE`() {
    val licence = aLicenceEntity.copy(
      licenceActivatedDate = LocalDateTime.now(),
    )
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(licence))

    service.updateLicenceStatus(
      1L,
      StatusUpdateRequest(status = LicenceStatus.APPROVED, username = "X", fullName = "Y"),
    )

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())

    assertThat(licenceCaptor.value.licenceActivatedDate).isNotNull()

    assertThat(licenceCaptor.value)
      .extracting("id", "statusCode", "updatedByUsername", "licenceActivatedDate")
      .isEqualTo(listOf(1L, LicenceStatus.APPROVED, "X", licence.licenceActivatedDate))
  }

  @Test
  fun `updating licence status to submitted should set the submitted date`() {
    val licence = aLicenceEntity.copy(
      licenceActivatedDate = LocalDateTime.now(),
    )
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(licence))

    service.updateLicenceStatus(
      1L,
      StatusUpdateRequest(status = LicenceStatus.SUBMITTED, username = "X", fullName = "Y"),
    )

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())

    assertThat(licenceCaptor.value.submittedDate).isNotNull()

    assertThat(licenceCaptor.value)
      .extracting("id", "statusCode", "updatedByUsername")
      .isEqualTo(listOf(1L, LicenceStatus.SUBMITTED, "X"))
  }

  @Test
  fun `update licence status throws not found exception`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.empty())

    val exception = assertThrows<EntityNotFoundException> {
      service.updateLicenceStatus(1L, StatusUpdateRequest(status = LicenceStatus.REJECTED, username = "X"))
    }

    assertThat(exception).isInstanceOf(EntityNotFoundException::class.java)

    verify(licenceRepository, times(1)).findById(1L)
    verify(licenceRepository, times(0)).saveAndFlush(any())
    verify(licenceEventRepository, times(0)).saveAndFlush(any())
    verify(auditEventRepository, times(0)).saveAndFlush(any())
  }

  @Test
  fun `submit licence throws not found exception`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.empty())

    val exception = assertThrows<EntityNotFoundException> {
      service.submitLicence(1L, emptyList())
    }

    assertThat(exception).isInstanceOf(EntityNotFoundException::class.java)

    verify(licenceRepository, times(1)).findById(1L)
    verify(licenceRepository, times(0)).saveAndFlush(any())
    verify(licenceEventRepository, times(0)).saveAndFlush(any())
    verify(auditEventRepository, times(0)).saveAndFlush(any())
  }

  @Test
  fun `submit a CRD licence saves new fields to the licence`() {
    val expectedCom = CommunityOffenderManager(
      staffIdentifier = 2000,
      username = "smills",
      email = "testemail@probation.gov.uk",
      firstName = "X",
      lastName = "Y",
    )

    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))
    whenever(staffRepository.findByUsernameIgnoreCase("smills")).thenReturn(expectedCom)

    service.submitLicence(1L, emptyList())

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    val eventCaptor = ArgumentCaptor.forClass(EntityLicenceEvent::class.java)
    val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(licenceEventRepository, times(1)).saveAndFlush(eventCaptor.capture())
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())

    assertThat(licenceCaptor.value.submittedDate).isNotNull()
    assertThat(licenceCaptor.value)
      .extracting("id", "statusCode", "updatedByUsername")
      .isEqualTo(listOf(1L, LicenceStatus.SUBMITTED, "smills"))

    assertThat(eventCaptor.value)
      .extracting("licenceId", "eventType", "eventDescription")
      .isEqualTo(
        listOf(
          1L,
          LicenceEventType.SUBMITTED,
          "Licence submitted for approval for ${aLicenceEntity.forename} ${aLicenceEntity.surname}",
        ),
      )

    assertThat(auditCaptor.value)
      .extracting("licenceId", "username", "fullName", "summary")
      .isEqualTo(
        listOf(
          1L,
          "smills",
          "X Y",
          "Licence submitted for approval for ${aLicenceEntity.forename} ${aLicenceEntity.surname}",
        ),
      )
  }

  @Test
  fun `submit a hard stop licence saves new fields to the licence`() {
    val caseAdmin = PrisonCaseAdministrator(
      username = "smills",
      email = "testemail@prison.gov.uk",
      firstName = "X",
      lastName = "Y",
    )

    val hardStopLicence = createHardStopLicence()
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(hardStopLicence))
    whenever(staffRepository.findByUsernameIgnoreCase("smills")).thenReturn(caseAdmin)

    service.submitLicence(1L, emptyList())

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    val eventCaptor = ArgumentCaptor.forClass(EntityLicenceEvent::class.java)
    val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(licenceEventRepository, times(1)).saveAndFlush(eventCaptor.capture())
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())

    assertThat(licenceCaptor.value.submittedDate).isNotNull()
    assertThat(licenceCaptor.value)
      .extracting("id", "statusCode", "updatedByUsername")
      .isEqualTo(listOf(1L, LicenceStatus.SUBMITTED, "smills"))

    assertThat(eventCaptor.value)
      .extracting("licenceId", "eventType", "eventDescription")
      .isEqualTo(
        listOf(
          1L,
          LicenceEventType.HARD_STOP_SUBMITTED,
          "Licence submitted for approval for ${hardStopLicence.forename} ${hardStopLicence.surname}",
        ),
      )

    assertThat(auditCaptor.value)
      .extracting("licenceId", "username", "fullName", "summary")
      .isEqualTo(
        listOf(
          1L,
          "smills",
          "X Y",
          "Licence submitted for approval for ${hardStopLicence.forename} ${hardStopLicence.surname}",
        ),
      )
  }

  @Test
  fun `submitting a licence variation`() {
    val expectedCom = CommunityOffenderManager(
      staffIdentifier = 2000,
      username = "smills",
      email = "testemail@probation.gov.uk",
      firstName = "X",
      lastName = "Y",
    )
    val variation = TestData.createVariationLicence().copy(
      variationOfId = 1,
      submittedBy = CommunityOffenderManager(
        staffIdentifier = 2000,
        username = "smills",
        email = "testemail@probation.gov.uk",
        firstName = "X",
        lastName = "Y",
      ),
    )

    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(variation))
    whenever(staffRepository.findByUsernameIgnoreCase("smills")).thenReturn(expectedCom)

    service.submitLicence(1L, listOf(NotifyRequest("testName", "testEmail"), NotifyRequest("testName1", "testEmail2")))

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)
    val eventCaptor = ArgumentCaptor.forClass(EntityLicenceEvent::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())
    verify(licenceEventRepository, times(1)).saveAndFlush(eventCaptor.capture())
    verify(notifyService, times(2))
      .sendVariationForApprovalEmail(
        any(),
        eq(variation.id.toString()),
        eq(variation.forename!!),
        eq(variation.surname!!),
        eq(variation.crn!!),
        eq(variation.submittedBy?.username!!),
      )

    assertThat(licenceCaptor.value)
      .extracting("id", "statusCode", "updatedByUsername")
      .isEqualTo(listOf(1L, LicenceStatus.VARIATION_SUBMITTED, "smills"))

    assertThat(eventCaptor.value)
      .extracting("licenceId", "eventType", "eventDescription")
      .isEqualTo(
        listOf(
          1L,
          LicenceEventType.VARIATION_SUBMITTED,
          "Licence submitted for approval for ${variation.forename} ${variation.surname}",
        ),
      )

    assertThat(auditCaptor.value)
      .extracting("licenceId", "username", "fullName", "summary")
      .isEqualTo(
        listOf(
          1L,
          "smills",
          "X Y",
          "Licence submitted for approval for ${variation.forename} ${variation.surname}",
        ),
      )
  }

  @Test
  fun `activate licences sets licence statuses to ACTIVE`() {
    val licenceCaptor = argumentCaptor<List<Licence>>()
    val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)
    val eventCaptor = ArgumentCaptor.forClass(EntityLicenceEvent::class.java)

    service.activateLicences(listOf(aLicenceEntity.copy(statusCode = LicenceStatus.APPROVED)))

    verify(licenceRepository, times(1)).saveAllAndFlush(licenceCaptor.capture())
    assertThat(licenceCaptor.allValues[0])
      .extracting("statusCode")
      .isEqualTo(
        listOf(
          LicenceStatus.ACTIVE,
        ),
      )
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())
    verify(licenceEventRepository, times(1)).saveAndFlush(eventCaptor.capture())
    verify(domainEventsService, times(1)).recordDomainEvent(aLicenceEntity, LicenceStatus.ACTIVE)

    assertThat(auditCaptor.value)
      .extracting("licenceId", "username", "fullName", "summary")
      .isEqualTo(
        listOf(
          1L,
          "SYSTEM",
          "SYSTEM",
          "Licence automatically activated for ${aLicenceEntity.forename} ${aLicenceEntity.surname}",
        ),
      )

    assertThat(eventCaptor.value)
      .extracting("licenceId", "eventType", "forenames", "surname")
      .isEqualTo(listOf(1L, LicenceEventType.ACTIVATED, "SYSTEM", "SYSTEM"))
  }

  @Test
  fun `activate licences sets licence statuses to ACTIVE and logs the provided reason`() {
    val licenceCaptor = argumentCaptor<List<Licence>>()
    val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)
    val eventCaptor = ArgumentCaptor.forClass(EntityLicenceEvent::class.java)

    service.activateLicences(listOf(aLicenceEntity.copy(statusCode = LicenceStatus.APPROVED)), "Test reason")

    verify(licenceRepository, times(1)).saveAllAndFlush(licenceCaptor.capture())
    assertThat(licenceCaptor.allValues[0])
      .extracting("statusCode")
      .isEqualTo(
        listOf(
          LicenceStatus.ACTIVE,
        ),
      )
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())
    verify(licenceEventRepository, times(1)).saveAndFlush(eventCaptor.capture())
    verify(domainEventsService, times(1)).recordDomainEvent(aLicenceEntity, LicenceStatus.ACTIVE)

    assertThat(auditCaptor.value)
      .extracting("licenceId", "username", "fullName", "summary")
      .isEqualTo(
        listOf(
          1L,
          "SYSTEM",
          "SYSTEM",
          "Test reason for ${aLicenceEntity.forename} ${aLicenceEntity.surname}",
        ),
      )

    assertThat(eventCaptor.value)
      .extracting("licenceId", "eventType", "forenames", "surname")
      .isEqualTo(listOf(1L, LicenceEventType.ACTIVATED, "SYSTEM", "SYSTEM"))
  }

  @Test
  fun `activate licences deactivates a newer in progress version of a licence`() {
    val licenceCaptor = argumentCaptor<List<Licence>>()
    val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)
    val eventCaptor = ArgumentCaptor.forClass(EntityLicenceEvent::class.java)

    val approvedLicenceVersion = aLicenceEntity.copy(statusCode = LicenceStatus.APPROVED)
    val inProgressVersion = approvedLicenceVersion.copy(
      id = 99999,
      statusCode = LicenceStatus.IN_PROGRESS,
      versionOfId = approvedLicenceVersion.id,
    )
    whenever(
      licenceRepository.findAllByVersionOfIdInAndStatusCodeIn(
        listOf(approvedLicenceVersion.id),
        listOf(LicenceStatus.IN_PROGRESS, LicenceStatus.SUBMITTED),
      ),
    ).thenReturn(listOf(inProgressVersion))

    service.activateLicences(listOf(approvedLicenceVersion))

    verify(licenceRepository, times(2)).saveAllAndFlush(licenceCaptor.capture())
    val licenceCaptors = licenceCaptor.allValues
    assertThat(licenceCaptors[0])
      .extracting("statusCode")
      .isEqualTo(
        listOf(
          LicenceStatus.ACTIVE,
        ),
      )
    assertThat(licenceCaptors[1])
      .extracting("statusCode")
      .isEqualTo(
        listOf(
          LicenceStatus.INACTIVE,
        ),
      )
    verify(auditEventRepository, times(2)).saveAndFlush(auditCaptor.capture())
    verify(licenceEventRepository, times(2)).saveAndFlush(eventCaptor.capture())
    verify(domainEventsService, times(1)).recordDomainEvent(approvedLicenceVersion, LicenceStatus.ACTIVE)
    verify(domainEventsService, times(1)).recordDomainEvent(inProgressVersion, LicenceStatus.INACTIVE)

    val auditCaptors = auditCaptor.allValues
    assertThat(auditCaptors[0])
      .extracting("licenceId", "username", "fullName", "summary")
      .isEqualTo(
        listOf(
          aLicenceEntity.id,
          "SYSTEM",
          "SYSTEM",
          "Licence automatically activated for ${aLicenceEntity.forename} ${aLicenceEntity.surname}",
        ),
      )
    assertThat(auditCaptors[1])
      .extracting("licenceId", "username", "fullName", "summary")
      .isEqualTo(
        listOf(
          inProgressVersion.id,
          "SYSTEM",
          "SYSTEM",
          "Licence automatically deactivated as the approved licence version was activated for ${aLicenceEntity.forename} ${aLicenceEntity.surname}",
        ),
      )
    val eventCaptors = eventCaptor.allValues
    assertThat(eventCaptors[0])
      .extracting("licenceId", "eventType", "forenames", "surname")
      .isEqualTo(
        listOf(1L, LicenceEventType.ACTIVATED, "SYSTEM", "SYSTEM"),
      )
    assertThat(eventCaptors[1])
      .extracting("licenceId", "eventType", "forenames", "surname")
      .isEqualTo(
        listOf(inProgressVersion.id, LicenceEventType.SUPERSEDED, "SYSTEM", "SYSTEM"),
      )
  }

  @Test
  fun `inactivate licences sets licence statuses to INACTIVE`() {
    val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)
    val eventCaptor = ArgumentCaptor.forClass(EntityLicenceEvent::class.java)

    service.inactivateLicences(listOf(aLicenceEntity.copy(statusCode = LicenceStatus.APPROVED)))

    verify(
      licenceRepository,
      times(1),
    ).saveAllAndFlush(listOf(aLicenceEntity.copy(statusCode = LicenceStatus.INACTIVE)))
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())
    verify(licenceEventRepository, times(1)).saveAndFlush(eventCaptor.capture())
    verify(domainEventsService, times(1)).recordDomainEvent(aLicenceEntity, LicenceStatus.INACTIVE)

    assertThat(auditCaptor.value)
      .extracting("licenceId", "username", "fullName", "summary")
      .isEqualTo(
        listOf(
          1L,
          "SYSTEM",
          "SYSTEM",
          "Licence automatically inactivated for ${aLicenceEntity.forename} ${aLicenceEntity.surname}",
        ),
      )

    assertThat(eventCaptor.value)
      .extracting("licenceId", "eventType", "forenames", "surname")
      .isEqualTo(listOf(1L, LicenceEventType.SUPERSEDED, "SYSTEM", "SYSTEM"))
  }

  @Test
  fun `inactivate licences sets licence statuses to INACTIVE and logs the provided reason`() {
    val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)
    val eventCaptor = ArgumentCaptor.forClass(EntityLicenceEvent::class.java)

    service.inactivateLicences(listOf(aLicenceEntity.copy(statusCode = LicenceStatus.APPROVED)), "Test reason")

    verify(
      licenceRepository,
      times(1),
    ).saveAllAndFlush(listOf(aLicenceEntity.copy(statusCode = LicenceStatus.INACTIVE)))
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())
    verify(licenceEventRepository, times(1)).saveAndFlush(eventCaptor.capture())
    verify(domainEventsService, times(1)).recordDomainEvent(aLicenceEntity, LicenceStatus.INACTIVE)

    assertThat(auditCaptor.value)
      .extracting("licenceId", "username", "fullName", "summary")
      .isEqualTo(
        listOf(
          1L,
          "SYSTEM",
          "SYSTEM",
          "Test reason for ${aLicenceEntity.forename} ${aLicenceEntity.surname}",
        ),
      )

    assertThat(eventCaptor.value)
      .extracting("licenceId", "eventType", "forenames", "surname")
      .isEqualTo(listOf(1L, LicenceEventType.SUPERSEDED, "SYSTEM", "SYSTEM"))
  }

  @Test
  fun `inactivate licences also inactivates any in progress versions of the licences`() {
    val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)
    val eventCaptor = ArgumentCaptor.forClass(EntityLicenceEvent::class.java)

    val inProgressLicenceVersion =
      aLicenceEntity.copy(id = 7843, statusCode = LicenceStatus.SUBMITTED, versionOfId = aLicenceEntity.id)
    whenever(
      licenceRepository.findAllByVersionOfIdInAndStatusCodeIn(
        listOf(aLicenceEntity.id),
        listOf(LicenceStatus.IN_PROGRESS, LicenceStatus.SUBMITTED),
      ),
    ).thenReturn(listOf(inProgressLicenceVersion))

    service.inactivateLicences(listOf(aLicenceEntity.copy(statusCode = LicenceStatus.APPROVED)))

    verify(
      licenceRepository,
      times(1),
    ).saveAllAndFlush(listOf(aLicenceEntity.copy(statusCode = LicenceStatus.INACTIVE)))
    verify(auditEventRepository, times(2)).saveAndFlush(auditCaptor.capture())
    verify(licenceEventRepository, times(2)).saveAndFlush(eventCaptor.capture())
    verify(domainEventsService, times(1)).recordDomainEvent(aLicenceEntity, LicenceStatus.INACTIVE)
    verify(domainEventsService, times(1)).recordDomainEvent(inProgressLicenceVersion, LicenceStatus.INACTIVE)

    assertThat(auditCaptor.allValues[0])
      .extracting("licenceId", "username", "fullName", "summary")
      .isEqualTo(
        listOf(
          aLicenceEntity.id,
          "SYSTEM",
          "SYSTEM",
          "Licence automatically inactivated for ${aLicenceEntity.forename} ${aLicenceEntity.surname}",
        ),
      )
    assertThat(auditCaptor.allValues[1])
      .extracting("licenceId", "username", "fullName", "summary")
      .isEqualTo(
        listOf(
          inProgressLicenceVersion.id,
          "SYSTEM",
          "SYSTEM",
          "Licence automatically inactivated for ${aLicenceEntity.forename} ${aLicenceEntity.surname}",
        ),
      )

    assertThat(eventCaptor.allValues[0])
      .extracting("licenceId", "eventType", "forenames", "surname")
      .isEqualTo(
        listOf(
          1L,
          LicenceEventType.SUPERSEDED,
          "SYSTEM",
          "SYSTEM",
        ),
      )
  }

  @Test
  fun `inActivateLicencesByIds calls inactivateLicences with the licences associated with the given IDs`() {
    val licence = aLicenceEntity.copy(statusCode = LicenceStatus.APPROVED)
    whenever(licenceRepository.findAllById(listOf(1))).thenReturn(listOf(licence))

    service.inActivateLicencesByIds(listOf(1))

    verify(licenceRepository, times(1)).findAllById(listOf(1L))
    verify(
      licenceRepository,
      times(1),
    ).saveAllAndFlush(listOf(licence.copy(statusCode = LicenceStatus.INACTIVE)))
    verify(domainEventsService, times(1)).recordDomainEvent(aLicenceEntity, LicenceStatus.INACTIVE)
  }

  @Test
  fun `update spo discussion persists the updated entity`() {
    val variation = TestData.createVariationLicence()
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(variation))

    service.updateSpoDiscussion(1L, UpdateSpoDiscussionRequest(spoDiscussion = "Yes"))

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())

    assertThat(licenceCaptor.value)
      .extracting("spoDiscussion", "updatedByUsername")
      .isEqualTo(listOf("Yes", "smills"))
  }

  @Test
  fun `update vlo discussion persists the updated entity`() {
    val variation = TestData.createVariationLicence()
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(variation))

    service.updateVloDiscussion(1L, UpdateVloDiscussionRequest(vloDiscussion = "Yes"))

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())

    assertThat(licenceCaptor.value)
      .extracting("vloDiscussion", "updatedByUsername")
      .isEqualTo(listOf("Yes", "smills"))
  }

  @Test
  fun `update reason for variation creates a licence event containing the reason`() {
    val variation = TestData.createVariationLicence()
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(variation))

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    val eventCaptor = ArgumentCaptor.forClass(EntityLicenceEvent::class.java)

    service.updateReasonForVariation(1L, UpdateReasonForVariationRequest(reasonForVariation = "reason"))

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(licenceEventRepository, times(1)).saveAndFlush(eventCaptor.capture())

    assertThat(licenceCaptor.value).extracting("updatedByUsername").isEqualTo("smills")

    assertThat(eventCaptor.value)
      .extracting("eventType", "username", "eventDescription")
      .isEqualTo(listOf(LicenceEventType.VARIATION_SUBMITTED_REASON, "smills", "reason"))
  }

  @Test
  fun `creating a variation`() {
    whenever(staffRepository.findByUsernameIgnoreCase(any())).thenReturn(
      CommunityOffenderManager(
        -1,
        1,
        "user",
        null,
        null,
        null,
      ),
    )
    whenever(licencePolicyService.currentPolicy()).thenReturn(
      LicencePolicy(
        "2.1",
        standardConditions = StandardConditions(emptyList(), emptyList()),
        additionalConditions = AdditionalConditions(emptyList(), emptyList()),
        changeHints = emptyList(),
      ),
    )
    whenever(licenceRepository.findById(1L)).thenReturn(
      Optional.of(aLicenceEntity),
    )
    whenever(licenceRepository.save(any())).thenReturn(aLicenceEntity)
    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    val licenceEventCaptor = ArgumentCaptor.forClass(LicenceEvent::class.java)

    service.createVariation(1L)

    verify(licenceRepository, times(1)).save(licenceCaptor.capture())
    with(licenceCaptor.value as VariationLicence) {
      assertThat(kind).isEqualTo(LicenceKind.VARIATION)
      assertThat(version).isEqualTo("2.1")
      assertThat(statusCode).isEqualTo(LicenceStatus.VARIATION_IN_PROGRESS)
      assertThat(variationOfId).isEqualTo(1)
      assertThat(licenceVersion).isEqualTo("2.0")
    }
    verify(licenceEventRepository).saveAndFlush(licenceEventCaptor.capture())
    assertThat(licenceEventCaptor.value.eventType).isEqualTo(LicenceEventType.VARIATION_CREATED)
  }

  @Test
  fun `creating a variation not PSS period should not delete AP conditions`() {
    whenever(staffRepository.findByUsernameIgnoreCase(any())).thenReturn(
      CommunityOffenderManager(
        -1,
        1,
        "user",
        null,
        null,
        null,
      ),
    )
    whenever(licencePolicyService.currentPolicy()).thenReturn(
      LicencePolicy(
        "2.1",
        standardConditions = StandardConditions(emptyList(), emptyList()),
        additionalConditions = AdditionalConditions(emptyList(), emptyList()),
        changeHints = emptyList(),
      ),
    )
    whenever(licenceRepository.findById(1L)).thenReturn(
      Optional.of(
        aLicenceEntity.copy(
          additionalConditions = additionalConditions,
          licenceExpiryDate = LocalDate.now().plusDays(1),
          topupSupervisionExpiryDate = LocalDate.now().plusDays(1),
          typeCode = LicenceType.AP_PSS,
        ),
      ),
    )
    whenever(licenceRepository.save(any())).thenReturn(
      aLicenceEntity.copy(
        additionalConditions = additionalConditions,
        licenceExpiryDate = LocalDate.now().plusDays(1),
        topupSupervisionExpiryDate = LocalDate.now().plusDays(1),
        typeCode = LicenceType.AP_PSS,
      ),
    )
    val newAdditionalConditionsCaptor = argumentCaptor<List<AdditionalCondition>>()

    service.createVariation(1L)

    verify(additionalConditionRepository, times(2)).saveAll(newAdditionalConditionsCaptor.capture())
    assertThat(newAdditionalConditionsCaptor.firstValue.size).isEqualTo(2)
    assertThat(newAdditionalConditionsCaptor.firstValue.first().conditionType).isEqualTo(LicenceType.AP.toString())
    assertThat(newAdditionalConditionsCaptor.firstValue.last().conditionType).isEqualTo(LicenceType.PSS.toString())
  }

  @Test
  fun `editing an approved licence creates and saves a new licence version`() {
    whenever(staffRepository.findByUsernameIgnoreCase(any())).thenReturn(
      CommunityOffenderManager(
        -1,
        1,
        "user",
        null,
        null,
        null,
      ),
    )
    whenever(licencePolicyService.currentPolicy()).thenReturn(
      LicencePolicy(
        "2.1",
        standardConditions = StandardConditions(emptyList(), emptyList()),
        additionalConditions = AdditionalConditions(emptyList(), emptyList()),
        changeHints = emptyList(),
      ),
    )
    val approvedLicence = aLicenceEntity.copy(statusCode = LicenceStatus.APPROVED)
    whenever(licenceRepository.findById(1L)).thenReturn(
      Optional.of(approvedLicence),
    )
    whenever(licenceRepository.save(any())).thenReturn(aLicenceEntity)
    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    val licenceEventCaptor = ArgumentCaptor.forClass(LicenceEvent::class.java)
    val auditEventCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)
    service.editLicence(1L)

    verify(licenceRepository, times(1)).save(licenceCaptor.capture())
    with(licenceCaptor.value as CrdLicence) {
      assertThat(version).isEqualTo("2.1")
      assertThat(statusCode).isEqualTo(LicenceStatus.IN_PROGRESS)
      assertThat(versionOfId).isEqualTo(1)
      assertThat(licenceVersion).isEqualTo("1.1")
    }

    verify(licenceEventRepository).saveAndFlush(licenceEventCaptor.capture())
    assertThat(licenceEventCaptor.value.eventType).isEqualTo(LicenceEventType.VERSION_CREATED)
    assertThat(licenceEventCaptor.value.eventDescription).isEqualTo("A new licence version was created for ${approvedLicence.forename} ${approvedLicence.surname} from ID ${approvedLicence.id}")
    verify(auditEventRepository).saveAndFlush(auditEventCaptor.capture())
    assertThat(auditEventCaptor.value.summary).isEqualTo("New licence version created for ${approvedLicence.forename} ${approvedLicence.surname}")
  }

  @Test
  fun `editing an approved licence creates and saves a new licence version returns all conditions`() {
    whenever(staffRepository.findByUsernameIgnoreCase(any())).thenReturn(
      CommunityOffenderManager(
        -1,
        1,
        "user",
        null,
        null,
        null,
      ),
    )
    whenever(licencePolicyService.currentPolicy()).thenReturn(
      LicencePolicy(
        "2.1",
        standardConditions = StandardConditions(emptyList(), emptyList()),
        additionalConditions = AdditionalConditions(emptyList(), emptyList()),
        changeHints = emptyList(),
      ),
    )

    val approvedLicence = aLicenceEntity.copy(
      statusCode = LicenceStatus.APPROVED,
      additionalConditions = additionalConditions,
    )
    whenever(licenceRepository.findById(1L)).thenReturn(
      Optional.of(approvedLicence),
    )

    whenever(licenceRepository.save(any())).thenReturn(approvedLicence)

    val newAdditionalConditionsCaptor = argumentCaptor<List<AdditionalCondition>>()
    service.editLicence(1L)
    verify(additionalConditionRepository, times(2)).saveAll(newAdditionalConditionsCaptor.capture())
    assertThat(newAdditionalConditionsCaptor.firstValue.size).isEqualTo(2)
    assertThat(newAdditionalConditionsCaptor.firstValue.first().conditionType).isEqualTo(LicenceType.AP.toString())
    assertThat(newAdditionalConditionsCaptor.firstValue.last().conditionType).isEqualTo(LicenceType.PSS.toString())
  }

  @Test
  fun `editing an approved licence creates and saves a new licence version and sends a reapproval email`() {
    whenever(staffRepository.findByUsernameIgnoreCase(any())).thenReturn(
      CommunityOffenderManager(
        -1,
        1,
        "user",
        null,
        null,
        null,
      ),
    )
    whenever(licencePolicyService.currentPolicy()).thenReturn(
      LicencePolicy(
        "2.1",
        standardConditions = StandardConditions(emptyList(), emptyList()),
        additionalConditions = AdditionalConditions(emptyList(), emptyList()),
        changeHints = emptyList(),
      ),
    )

    val approvedLicence = aLicenceEntity.copy(
      statusCode = LicenceStatus.APPROVED,
      additionalConditions = additionalConditions,
    )
    whenever(licenceRepository.findById(1L)).thenReturn(
      Optional.of(approvedLicence),
    )
    whenever(omuService.getOmuContactEmail(any())).thenReturn(
      OmuContact(
        prisonCode = aLicenceEntity.prisonCode!!,
        email = "test@OMU.testing.com",
        dateCreated = LocalDateTime.now(),
      ),
    )
    whenever(licenceRepository.save(any())).thenReturn(approvedLicence)

    service.editLicence(1L)
    verify(notifyService, times(1)).sendVariationForReApprovalEmail(
      eq("test@OMU.testing.com"),
      eq(aLicenceEntity.forename ?: "unknown"),
      eq(aLicenceEntity.surname ?: "unknown"),
      eq(aLicenceEntity.nomsId),
      any(),
    )
  }

  @Test
  fun `attempting to editing a licence with status other than approved results in validation exception `() {
    val activeLicence = aLicenceEntity.copy(statusCode = LicenceStatus.ACTIVE)
    whenever(licenceRepository.findById(1L)).thenReturn(
      Optional.of(activeLicence),
    )

    val exception = assertThrows<ValidationException> { service.editLicence(1L) }
    assertThat(exception).isInstanceOf(ValidationException::class.java)
    assertThat(exception).message().isEqualTo("Can only edit APPROVED licences")
  }

  @Test
  fun `editing an approved licence which already has an in progress version returns that version`() {
    whenever(staffRepository.findByUsernameIgnoreCase(any())).thenReturn(
      CommunityOffenderManager(
        -1,
        1,
        "user",
        null,
        null,
        null,
      ),
    )
    val approvedLicence = aLicenceEntity.copy(statusCode = LicenceStatus.APPROVED)
    val inProgressLicenceVersion =
      approvedLicence.copy(id = 9032, statusCode = LicenceStatus.IN_PROGRESS, versionOfId = approvedLicence.id)
    whenever(licenceRepository.findById(1L)).thenReturn(
      Optional.of(approvedLicence),
    )
    whenever(
      licenceRepository.findAllByVersionOfIdInAndStatusCodeIn(
        listOf(approvedLicence.id),
        listOf(LicenceStatus.IN_PROGRESS, LicenceStatus.SUBMITTED),
      ),
    )
      .thenReturn(
        listOf(inProgressLicenceVersion),
      )

    val newLicenceVersion = service.editLicence(1L)
    assertNotNull(newLicenceVersion)
    assertThat(newLicenceVersion.licenceId).isEqualTo(inProgressLicenceVersion.id)

    verify(licenceRepository, never()).save(any())
  }

  @Test
  fun `editing an approved licence which already has an in progress version does not send a reapproval email`() {
    whenever(staffRepository.findByUsernameIgnoreCase(any())).thenReturn(
      CommunityOffenderManager(
        -1,
        1,
        "user",
        null,
        null,
        null,
      ),
    )
    val approvedLicence = aLicenceEntity.copy(statusCode = LicenceStatus.APPROVED)
    val inProgressLicenceVersion =
      approvedLicence.copy(id = 9032, statusCode = LicenceStatus.IN_PROGRESS, versionOfId = approvedLicence.id)
    whenever(licenceRepository.findById(1L)).thenReturn(
      Optional.of(approvedLicence),
    )
    whenever(
      licenceRepository.findAllByVersionOfIdInAndStatusCodeIn(
        listOf(approvedLicence.id),
        listOf(LicenceStatus.IN_PROGRESS, LicenceStatus.SUBMITTED),
      ),
    )
      .thenReturn(
        listOf(inProgressLicenceVersion),
      )

    service.editLicence(1L)

    verify(notifyService, never()).sendVariationForReApprovalEmail(any(), any(), any(), any(), any())
  }

  @Test
  fun `discarding a licence`() {
    val expectedCom = CommunityOffenderManager(
      staffIdentifier = 2000,
      username = "smills",
      email = "testemail@probation.gov.uk",
      firstName = "X",
      lastName = "Y",
    )
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))
    whenever(staffRepository.findByUsernameIgnoreCase("smills")).thenReturn(expectedCom)

    val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)

    service.discardLicence(1L)

    verify(licenceRepository, times(1)).delete(aLicenceEntity)
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())

    assertThat(auditCaptor.value)
      .extracting("licenceId", "username", "fullName", "summary")
      .isEqualTo(
        listOf(
          1L,
          "smills",
          "X Y",
          "Licence variation discarded for ${aLicenceEntity.forename} ${aLicenceEntity.surname}",
        ),
      )
  }

  @Test
  fun `update prison information persists the updated entity`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))

    service.updatePrisonInformation(
      1L,
      UpdatePrisonInformationRequest(
        prisonCode = "PVI",
        prisonDescription = "Pentonville (HMP)",
        prisonTelephone = "+44 276 54545",
      ),
    )

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())

    assertThat(licenceCaptor.value)
      .extracting("prisonCode", "prisonDescription", "prisonTelephone", "updatedByUsername")
      .isEqualTo(listOf("PVI", "Pentonville (HMP)", "+44 276 54545", "smills"))

    assertThat(auditCaptor.value)
      .extracting("licenceId", "username", "fullName", "summary")
      .isEqualTo(
        listOf(
          1L,
          "SYSTEM",
          "SYSTEM",
          "Prison information updated for ${aLicenceEntity.forename} ${aLicenceEntity.surname}",
        ),
      )
  }

  @Test
  fun `referring a licence variation`() {
    val referVariationRequest = ReferVariationRequest(reasonForReferral = "reason")
    val expectedCom = CommunityOffenderManager(
      staffIdentifier = 2000,
      username = "smills",
      email = "testemail@probation.gov.uk",
      firstName = "X",
      lastName = "Y",
    )
    val variation = TestData.createVariationLicence()
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(variation))
    whenever(staffRepository.findByUsernameIgnoreCase("smills")).thenReturn(expectedCom)

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    val eventCaptor = ArgumentCaptor.forClass(EntityLicenceEvent::class.java)
    val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)

    service.referLicenceVariation(1L, referVariationRequest)

    verify(licenceRepository, times(1)).findById(1L)
    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(licenceEventRepository, times(1)).saveAndFlush(eventCaptor.capture())
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())

    assertThat(licenceCaptor.value)
      .extracting("statusCode", "updatedByUsername")
      .isEqualTo(listOf(LicenceStatus.VARIATION_REJECTED, "smills"))

    assertThat(eventCaptor.value)
      .extracting("licenceId", "eventType", "username", "eventDescription")
      .isEqualTo(listOf(1L, LicenceEventType.VARIATION_REFERRED, "smills", "reason"))

    assertThat(auditCaptor.value)
      .extracting("licenceId", "username", "fullName", "summary")
      .isEqualTo(
        listOf(
          1L,
          "smills",
          "X Y",
          "Licence variation rejected for ${variation.forename} ${variation.surname}",
        ),
      )

    verify(notifyService, times(1)).sendVariationReferredEmail(
      variation.createdBy?.email ?: "",
      "${variation.createdBy?.firstName} ${variation.createdBy?.lastName}",
      variation.responsibleCom?.email ?: "",
      "${variation.responsibleCom?.firstName} ${variation.responsibleCom?.lastName}",
      "${variation.forename} ${variation.surname}",
      "1",
    )
  }

  @Test
  fun `approving a licence variation`() {
    val expectedCom = CommunityOffenderManager(
      staffIdentifier = 2000,
      username = "smills",
      email = "testemail@probation.gov.uk",
      firstName = "X",
      lastName = "Y",
    )

    val variation = TestData.createVariationLicence().copy(id = 2, variationOfId = 1L)
    whenever(licenceRepository.findById(2L)).thenReturn(Optional.of(variation))
    whenever(staffRepository.findByUsernameIgnoreCase("smills")).thenReturn(expectedCom)

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    val eventCaptor = ArgumentCaptor.forClass(EntityLicenceEvent::class.java)
    val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)

    service.approveLicenceVariation(2L)

    verify(licenceRepository, times(1)).findById(2L)

    // Capture calls to licence, history and audit saveAndFlush - as a list
    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(licenceEventRepository, times(1)).saveAndFlush(eventCaptor.capture())
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())

    // Check all calls were made
    assertThat(licenceCaptor.allValues.size).isEqualTo(1)
    assertThat(eventCaptor.allValues.size).isEqualTo(1)
    assertThat(auditCaptor.allValues.size).isEqualTo(1)

    assertThat(licenceCaptor.allValues[0])
      .extracting("id", "statusCode", "updatedByUsername", "approvedByUsername", "approvedByName")
      .isEqualTo(listOf(2L, LicenceStatus.VARIATION_APPROVED, "smills", "smills", "X Y"))

    assertThat(eventCaptor.allValues[0])
      .extracting("licenceId", "eventType", "username")
      .isEqualTo(listOf(2L, LicenceEventType.VARIATION_APPROVED, "smills"))

    assertThat(auditCaptor.allValues[0])
      .extracting("licenceId", "username", "fullName", "summary")
      .isEqualTo(
        listOf(
          2L,
          "smills",
          "X Y",
          "Licence variation approved for ${variation.forename} ${variation.surname}",
        ),
      )

    verify(notifyService, times(1)).sendVariationApprovedEmail(
      variation.createdBy?.email ?: "",
      "${variation.createdBy?.firstName} ${variation.createdBy?.lastName}",
      variation.responsibleCom?.email ?: "",
      "${variation.responsibleCom?.firstName} ${variation.responsibleCom?.lastName}",
      "${variation.forename} ${variation.surname}",
      "2",
    )
  }

  @Test
  fun `update licence status to ACTIVE`() {
    val approvedLicence =
      aLicenceEntity.copy(id = 2L, statusCode = LicenceStatus.APPROVED, licenceVersion = "2.0")

    whenever(licenceRepository.findById(approvedLicence.id)).thenReturn(Optional.of(approvedLicence))
    whenever(
      licenceRepository.findAllByVersionOfIdInAndStatusCodeIn(
        listOf(approvedLicence.id),
        listOf(LicenceStatus.IN_PROGRESS, LicenceStatus.SUBMITTED),
      ),
    ).thenReturn(emptyList())

    service.updateLicenceStatus(
      approvedLicence.id,
      StatusUpdateRequest(status = LicenceStatus.ACTIVE, username = "X", fullName = "Y"),
    )

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())
    verify(domainEventsService, times(1)).recordDomainEvent(approvedLicence, LicenceStatus.ACTIVE)

    assertThat(licenceCaptor.value.licenceActivatedDate).isNotNull()

    assertThat(licenceCaptor.value)
      .extracting("id", "statusCode", "updatedByUsername", "licenceActivatedDate")
      .isEqualTo(listOf(approvedLicence.id, LicenceStatus.ACTIVE, "X", licenceCaptor.value.licenceActivatedDate))

    assertThat(auditCaptor.firstValue).extracting("licenceId", "username", "fullName", "summary", "eventType")
      .isEqualTo(
        listOf(
          approvedLicence.id,
          "X",
          "Y",
          "Licence set to ACTIVE for ${approvedLicence.forename} ${approvedLicence.surname}",
          USER_EVENT,
        ),
      )
  }

  @Test
  fun `update licence status to INACTIVE`() {
    val approvedLicence =
      aLicenceEntity.copy(id = 2L, statusCode = LicenceStatus.APPROVED, licenceVersion = "2.0")

    whenever(licenceRepository.findById(approvedLicence.id)).thenReturn(Optional.of(approvedLicence))
    whenever(
      licenceRepository.findAllByVersionOfIdInAndStatusCodeIn(
        listOf(approvedLicence.id),
        listOf(LicenceStatus.IN_PROGRESS, LicenceStatus.SUBMITTED),
      ),
    ).thenReturn(emptyList())

    service.updateLicenceStatus(
      approvedLicence.id,
      StatusUpdateRequest(status = LicenceStatus.INACTIVE, username = "X", fullName = "Y"),
    )

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())
    verify(domainEventsService, times(1)).recordDomainEvent(approvedLicence, LicenceStatus.INACTIVE)

    assertThat(licenceCaptor.value.licenceActivatedDate).isNull()

    assertThat(licenceCaptor.value)
      .extracting("id", "statusCode", "updatedByUsername", "supersededDate")
      .isEqualTo(listOf(approvedLicence.id, LicenceStatus.INACTIVE, "X", licenceCaptor.value.supersededDate))

    assertThat(auditCaptor.firstValue).extracting("licenceId", "username", "fullName", "summary", "eventType")
      .isEqualTo(
        listOf(
          approvedLicence.id,
          "X",
          "Y",
          "Licence set to INACTIVE for ${approvedLicence.forename} ${approvedLicence.surname}",
          USER_EVENT,
        ),
      )
  }

  @Test
  fun `update licence status to ACTIVE for variation approved`() {
    val variationApprovedLicence =
      TestData.createVariationLicence().copy(id = 2L, statusCode = LicenceStatus.VARIATION_APPROVED, licenceVersion = "2.0")

    whenever(licenceRepository.findById(variationApprovedLicence.id)).thenReturn(Optional.of(variationApprovedLicence))
    whenever(
      licenceRepository.findAllByVersionOfIdInAndStatusCodeIn(
        listOf(variationApprovedLicence.id),
        listOf(LicenceStatus.IN_PROGRESS, LicenceStatus.SUBMITTED),
      ),
    ).thenReturn(emptyList())

    service.updateLicenceStatus(
      variationApprovedLicence.id,
      StatusUpdateRequest(status = LicenceStatus.ACTIVE, username = "X", fullName = "Y"),
    )

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())
    verify(domainEventsService, times(1)).recordDomainEvent(variationApprovedLicence, LicenceStatus.ACTIVE)

    assertThat(licenceCaptor.value.licenceActivatedDate).isNotNull()

    assertThat(licenceCaptor.value)
      .extracting("id", "statusCode", "updatedByUsername", "licenceActivatedDate")
      .isEqualTo(listOf(variationApprovedLicence.id, LicenceStatus.ACTIVE, "X", licenceCaptor.value.licenceActivatedDate))

    assertThat(auditCaptor.firstValue).extracting("licenceId", "username", "fullName", "summary", "eventType")
      .isEqualTo(
        listOf(
          variationApprovedLicence.id,
          "X",
          "Y",
          "Licence set to ACTIVE for ${variationApprovedLicence.forename} ${variationApprovedLicence.surname}",
          USER_EVENT,
        ),
      )
  }

  @Test
  fun `update licence status to INACTIVE for an existing ACTIVE variation licence`() {
    val variationLicence = TestData
      .createVariationLicence()
      .copy(id = 1L, statusCode = LicenceStatus.ACTIVE, licenceVersion = "2.0")

    whenever(licenceRepository.findById(variationLicence.id)).thenReturn(Optional.of(variationLicence))
    whenever(
      licenceRepository.findAllByVersionOfIdInAndStatusCodeIn(
        listOf(variationLicence.id),
        listOf(LicenceStatus.IN_PROGRESS, LicenceStatus.SUBMITTED),
      ),
    ).thenReturn(emptyList())

    service.updateLicenceStatus(
      variationLicence.id,
      StatusUpdateRequest(status = LicenceStatus.INACTIVE, username = "X", fullName = "Y"),
    )

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())
    verify(domainEventsService, times(1)).recordDomainEvent(variationLicence, LicenceStatus.INACTIVE)

    assertThat(licenceCaptor.value.licenceActivatedDate).isNull()

    assertThat(licenceCaptor.value)
      .extracting("id", "statusCode", "updatedByUsername", "supersededDate")
      .isEqualTo(listOf(variationLicence.id, LicenceStatus.INACTIVE, "X", licenceCaptor.value.supersededDate))

    assertThat(auditCaptor.firstValue).extracting("licenceId", "username", "fullName", "summary", "eventType")
      .isEqualTo(
        listOf(
          variationLicence.id,
          "X",
          "Y",
          "Licence set to INACTIVE for ${variationLicence.forename} ${variationLicence.surname}",
          USER_EVENT,
        ),
      )
  }

  @Nested
  inner class `Marking reviewed when no variation required` {
    @Test
    fun happyPath() {
      whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))
      whenever(staffRepository.findByUsernameIgnoreCase("smills")).thenReturn(
        CommunityOffenderManager(
          staffIdentifier = 2000,
          username = "smills",
          email = "testemail@probation.gov.uk",
          firstName = "X",
          lastName = "Y",
        ),
      )

      val hardstopLicence = createHardStopLicence()
        .copy(id = 1L, statusCode = LicenceStatus.ACTIVE, licenceVersion = "2.0", reviewDate = null)

      whenever(licenceRepository.findById(hardstopLicence.id)).thenReturn(Optional.of(hardstopLicence))

      service.reviewWithNoVariationRequired(1L)

      argumentCaptor<HardStopLicence>().apply {
        verify(licenceRepository, times(1)).saveAndFlush(capture())
        assertThat(firstValue.reviewDate?.toLocalDate()).isEqualTo(LocalDate.now())
        assertThat(firstValue.dateLastUpdated?.toLocalDate()).isEqualTo(LocalDate.now())
        assertThat(firstValue.updatedByUsername).isEqualTo("smills")
      }

      argumentCaptor<EntityAuditEvent>().apply {
        verify(auditEventRepository, times(1)).saveAndFlush(capture())
        assertThat(firstValue.licenceId).isEqualTo(hardstopLicence.id)
        assertThat(firstValue.username).isEqualTo("smills")
        assertThat(firstValue.fullName).isEqualTo("X Y")
        assertThat(firstValue.summary).isEqualTo("Licence reviewed without being varied for John Smith")
        assertThat(firstValue.eventType).isEqualTo(USER_EVENT)
      }

      argumentCaptor<LicenceEvent>().apply {
        verify(licenceEventRepository, times(1)).saveAndFlush(capture())
        assertThat(firstValue.licenceId).isEqualTo(hardstopLicence.id)
        assertThat(firstValue.username).isEqualTo("smills")
        assertThat(firstValue.eventDescription).isEqualTo("Licence reviewed without being varied")
        assertThat(firstValue.forenames).isEqualTo("X")
        assertThat(firstValue.surname).isEqualTo("Y")
        assertThat(firstValue.eventType).isEqualTo(LicenceEventType.HARD_STOP_REVIEWED)
      }
    }

    @Test
    fun alreadyReviewed() {
      val hardstopLicence = createHardStopLicence()
        .copy(id = 1L, reviewDate = LocalDateTime.now())

      whenever(licenceRepository.findById(hardstopLicence.id)).thenReturn(Optional.of(hardstopLicence))

      service.reviewWithNoVariationRequired(1L)

      verify(licenceRepository, never()).saveAndFlush(any())
      verify(auditEventRepository, never()).saveAndFlush(any())
      verify(licenceEventRepository, never()).saveAndFlush(any())
    }

    @Test
    fun missingLicence() {
      whenever(licenceRepository.findById(1L)).thenReturn(Optional.empty())

      assertThatThrownBy {
        service.reviewWithNoVariationRequired(1L)
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessage("1")

      verify(licenceRepository, never()).saveAndFlush(any())
      verify(auditEventRepository, never()).saveAndFlush(any())
      verify(licenceEventRepository, never()).saveAndFlush(any())
    }

    @Test
    fun notAHardStopLicence() {
      val hardstopLicence = createCrdLicence()
        .copy(id = 1L)

      whenever(licenceRepository.findById(hardstopLicence.id)).thenReturn(Optional.of(hardstopLicence))

      assertThatThrownBy {
        service.reviewWithNoVariationRequired(1L)
      }.isInstanceOf(ValidationException::class.java)
        .hasMessage("Trying to review a CrdLicence: 1")

      verify(licenceRepository, never()).saveAndFlush(any())
      verify(auditEventRepository, never()).saveAndFlush(any())
      verify(licenceEventRepository, never()).saveAndFlush(any())
    }
  }

  private companion object {
    val anAdditionalCondition = AdditionalConditionAp(
      code = "code",
      category = "category",
      text = "text",
      requiresInput = false,
    )

    val aLicenceEntity = TestData.createCrdLicence().copy(
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
      standardConditions = emptyList(),
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
      approvedByName = "jim smith",
      approvedDate = LocalDateTime.of(2023, 9, 19, 16, 38, 42),
    ).let {
      it.copy(
        standardConditions = listOf(
          EntityStandardCondition(
            id = 1,
            conditionCode = "goodBehaviour",
            conditionSequence = 1,
            conditionText = "Be of good behaviour",
            licence = it,
          ),
          EntityStandardCondition(
            id = 2,
            conditionCode = "notBreakLaw",
            conditionSequence = 2,
            conditionText = "Do not break any law",
            licence = it,
          ),
          EntityStandardCondition(
            id = 3,
            conditionCode = "attendMeetings",
            conditionSequence = 3,
            conditionText = "Attend meetings",
            licence = it,
          ),
        ),
      )
    }

    val aRecentlyApprovedLicence = aLicenceEntity.copy(
      actualReleaseDate = LocalDate.now().minusDays(1),
      conditionalReleaseDate = LocalDate.now(),
    )

    val aLicenceSummary = LicenceSummary(
      kind = LicenceKind.CRD,
      licenceId = 1,
      licenceType = LicenceType.AP,
      licenceStatus = LicenceStatus.IN_PROGRESS,
      nomisId = "A1234AA",
      forename = "Bob",
      surname = "Mortimer",
      crn = "X12345",
      dateOfBirth = LocalDate.of(1985, 12, 28),
      prisonCode = "MDI",
      prisonDescription = "Moorland (HMP)",
      probationAreaCode = "N01",
      probationAreaDescription = "Wales",
      probationPduCode = "N01A",
      probationPduDescription = "Cardiff",
      probationLauCode = "N01A2",
      probationLauDescription = "Cardiff South",
      probationTeamCode = "NA01A2-A",
      probationTeamDescription = "Cardiff South Team A",
      conditionalReleaseDate = LocalDate.of(2021, 10, 22),
      actualReleaseDate = LocalDate.of(2021, 10, 22),
      sentenceStartDate = LocalDate.of(2018, 10, 22),
      sentenceEndDate = LocalDate.of(2021, 10, 22),
      licenceStartDate = LocalDate.of(2021, 10, 22),
      licenceExpiryDate = LocalDate.of(2021, 10, 22),
      topupSupervisionStartDate = LocalDate.of(2021, 10, 22),
      topupSupervisionExpiryDate = LocalDate.of(2021, 10, 22),
      comUsername = "smills",
      bookingId = 54321,
      dateCreated = LocalDateTime.of(2022, 7, 27, 15, 0, 0),
      approvedByName = "jim smith",
      approvedDate = LocalDateTime.of(2023, 9, 19, 16, 38, 42),
      licenceVersion = "1.0",
    )

    val aRecentlyApprovedLicenceSummary = aLicenceSummary.copy(
      actualReleaseDate = LocalDate.now().minusDays(1),
      conditionalReleaseDate = LocalDate.now(),
    )
    val someAdditionalConditionData = listOf(
      AdditionalConditionData(
        id = 1,
        dataField = "dataField",
        dataValue = "dataValue",
        additionalCondition = AdditionalCondition(
          licence = aLicenceEntity,
          conditionVersion = "1.0",
        ),
      ),
    )

    val additionalConditions = listOf(
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
        conditionCode = "code",
        conditionSequence = 5,
        conditionCategory = "oldCategory",
        conditionText = "oldText",
        additionalConditionData = someAdditionalConditionData,
        licence = aLicenceEntity,
        conditionType = "PSS",
      ),
    )
  }
}
