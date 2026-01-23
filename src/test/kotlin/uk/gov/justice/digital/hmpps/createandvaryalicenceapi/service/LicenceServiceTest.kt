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
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.mapping.PropertyReferenceException
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HardStopLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcVariationLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence.Companion.SYSTEM_USER
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.LicenceEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.OmuContact
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.PrisonUser
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.VariationLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.timeserved.TimeServedLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.PrrdLicenceResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.StatusUpdateRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.AdditionalConditionAp
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.AdditionalConditions
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.AllAdditionalConditions
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.LicencePolicy
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.StandardConditions
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.DeactivateLicenceAndVariationsRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.NotifyRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.ReferVariationRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdatePrisonInformationRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateReasonForVariationRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateSpoDiscussionRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateVloDiscussionRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.CrdLicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceQueryObject
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StaffRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.anAdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.anEligibilityAssessment
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.anIneligibleEligibilityAssessment
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.anotherCommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.communityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createCrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createHardStopLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createHdcLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createHdcVariationLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createPrrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createTimeServedLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createVariationLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.offenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.prisonerSearchResult
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions.upload.UploadFileConditionsService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.DomainEventsService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.LicencePolicyService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.POLICY_V2_1
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AuditEventType.SYSTEM_EVENT
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AuditEventType.USER_EVENT
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.DateChangeLicenceDeativationReason
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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.HdcLicence as HdcLicenceModel
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.HdcVariationLicence as HdcVariationLicenceModel

class LicenceServiceTest {
  private val licenceRepository = mock<LicenceRepository>()
  private val crdLicenceRepository = mock<CrdLicenceRepository>()
  private val staffRepository = mock<StaffRepository>()
  private val licenceEventRepository = mock<LicenceEventRepository>()
  private val licencePolicyService = mock<LicencePolicyService>()
  private val auditEventRepository = mock<AuditEventRepository>()
  private val notifyService = mock<NotifyService>()
  private val omuService = mock<OmuService>()
  private val releaseDateService = mock<ReleaseDateService>()
  private val domainEventsService = mock<DomainEventsService>()
  private val prisonerSearchApiClient = mock<PrisonerSearchApiClient>()
  private val eligibilityService = mock<EligibilityService>()
  private val uploadFileConditionsService = mock<UploadFileConditionsService>()
  private val deliusApiClient = mock<DeliusApiClient>()
  private val telemetryService = mock<TelemetryService>()
  private val auditService = mock<AuditService>()

  private val service =
    LicenceService(
      licenceRepository,
      crdLicenceRepository,
      staffRepository,
      licenceEventRepository,
      licencePolicyService,
      auditEventRepository,
      notifyService,
      omuService,
      releaseDateService,
      domainEventsService,
      prisonerSearchApiClient,
      eligibilityService,
      uploadFileConditionsService,
      deliusApiClient,
      telemetryService,
      auditService,
      isTimeServedLogicEnabled = true,
    )

  @BeforeEach
  fun reset() {
    val authentication = mock<Authentication>()
    val securityContext = mock<SecurityContext>()

    whenever(authentication.name).thenReturn(aCom.username)
    whenever(securityContext.authentication).thenReturn(authentication)
    SecurityContextHolder.setContext(securityContext)
    whenever(licencePolicyService.policyByVersion(any())).thenReturn(POLICY_V2_1)

    reset(
      licenceRepository,
      crdLicenceRepository,
      staffRepository,
      licenceEventRepository,
      licencePolicyService,
      auditEventRepository,
      notifyService,
      omuService,
      releaseDateService,
      domainEventsService,
      prisonerSearchApiClient,
      eligibilityService,
      uploadFileConditionsService,
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
  fun `service returns a PRRD licence by ID`() {
    // Given
    whenever(licenceRepository.findById(1L)).thenReturn(
      Optional.of(createPrrdLicence().copy()),
    )

    whenever(licencePolicyService.getAllAdditionalConditions()).thenReturn(
      AllAdditionalConditions(mapOf("2.1" to mapOf("code" to anAdditionalCondition))),
    )

    // When
    val licence = service.getLicenceById(1L)

    // Then
    assertThat(licence).isExactlyInstanceOf(PrrdLicenceResponse::class.java)
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
  fun `service returns a licence with derived fields populated`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))
    whenever(licencePolicyService.getAllAdditionalConditions()).thenReturn(
      AllAdditionalConditions(mapOf("2.1" to mapOf("code" to anAdditionalCondition))),
    )
    whenever(releaseDateService.isInHardStopPeriod(any(), anyOrNull(), anyOrNull())).thenReturn(true)
    whenever(releaseDateService.isDueToBeReleasedInTheNextTwoWorkingDays(any())).thenReturn(true)
    whenever(releaseDateService.getHardStopDate(any(), anyOrNull())).thenReturn(LocalDate.of(2022, 1, 3))
    whenever(releaseDateService.getHardStopWarningDate(any(), anyOrNull())).thenReturn(LocalDate.of(2022, 1, 1))

    val licence = service.getLicenceById(1L) as CrdLicenceModel

    assertThat(licence.isInHardStopPeriod).isTrue()
    assertThat(licence.isDueToBeReleasedInTheNextTwoWorkingDays).isTrue()
    assertThat(licence.hardStopDate).isEqualTo(LocalDate.of(2022, 1, 3))
    assertThat(licence.hardStopWarningDate).isEqualTo(LocalDate.of(2022, 1, 1))
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
  fun `find licences matching criteria - enriched with derived fields`() {
    val expectedHardStopDate = LocalDate.of(2023, 1, 12)
    val expectedHardStopWarningDate = LocalDate.of(2023, 1, 10)

    whenever(releaseDateService.getHardStopDate(any(), anyOrNull())).thenReturn(expectedHardStopDate)
    whenever(releaseDateService.getHardStopWarningDate(any(), anyOrNull())).thenReturn(expectedHardStopWarningDate)
    whenever(licenceRepository.findAll(any<Specification<EntityLicence>>(), any<Sort>())).thenReturn(
      listOf(
        aLicenceEntity,
      ),
    )
    val licenceSummaries = service.findLicencesMatchingCriteria(LicenceQueryObject())

    assertThat(licenceSummaries).hasSize(1)
    with(licenceSummaries.first()) {
      assertThat(hardStopDate).isEqualTo(expectedHardStopDate)
      assertThat(hardStopWarningDate).isEqualTo(expectedHardStopWarningDate)
    }
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
  fun `find licences matching criteria - updated by full name populated`() {
    val licenceQueryObject = LicenceQueryObject(
      prisonCodes = listOf("MDI"),
      statusCodes = listOf(LicenceStatus.APPROVED),
      nomsIds = listOf("A1234AA"),
    )
    whenever(licenceRepository.findAll(any<Specification<EntityLicence>>(), any<Sort>())).thenReturn(
      listOf(
        aLicenceEntity.copy(
          updatedBy = aCom,
        ),
      ),
    )

    val licenceSummaries = service.findLicencesMatchingCriteria(licenceQueryObject)

    assertThat(licenceSummaries).isEqualTo(
      listOf(
        aLicenceSummary.copy(
          updatedByFullName = "X Y",
        ),
      ),
    )
    verify(licenceRepository, times(1)).findAll(any<Specification<EntityLicence>>(), eq(Sort.unsorted()))
  }

  @Test
  fun `review needed - when review date not set on ACTIVE licence`() {
    val licenceQueryObject = LicenceQueryObject(pdus = listOf("A", "B"))
    whenever(licenceRepository.findAll(any<Specification<EntityLicence>>(), any<Sort>())).thenReturn(
      listOf(
        createHardStopLicence().copy(statusCode = LicenceStatus.ACTIVE, reviewDate = null),
      ),
    )

    val licenceSummaries = service.findLicencesMatchingCriteria(licenceQueryObject)

    assertThat(licenceSummaries[0].isReviewNeeded).isTrue()
  }

  @Test
  fun `review not needed - when review date set`() {
    val licenceQueryObject = LicenceQueryObject(pdus = listOf("A", "B"))
    whenever(licenceRepository.findAll(any<Specification<EntityLicence>>(), any<Sort>())).thenReturn(
      listOf(
        createHardStopLicence().copy(reviewDate = LocalDateTime.now()),
      ),
    )

    val licenceSummaries = service.findLicencesMatchingCriteria(licenceQueryObject)

    assertThat(licenceSummaries[0].isReviewNeeded).isFalse()
  }

  @Test
  fun `review not needed - when review date not set on non-ACTIVE licence`() {
    val licenceQueryObject = LicenceQueryObject(pdus = listOf("A", "B"))
    whenever(licenceRepository.findAll(any<Specification<EntityLicence>>(), any<Sort>())).thenReturn(
      listOf(
        createHardStopLicence().copy(reviewDate = null),
      ),
    )

    val licenceSummaries = service.findLicencesMatchingCriteria(licenceQueryObject)

    assertThat(licenceSummaries[0].isReviewNeeded).isFalse()
  }

  @Test
  fun `review not needed - for CRD licences`() {
    val licenceQueryObject = LicenceQueryObject(pdus = listOf("A", "B"))
    whenever(licenceRepository.findAll(any<Specification<EntityLicence>>(), any<Sort>())).thenReturn(
      listOf(
        aLicenceEntity,
      ),
    )

    val licenceSummaries = service.findLicencesMatchingCriteria(licenceQueryObject)

    assertThat(licenceSummaries[0].isReviewNeeded).isFalse()
  }

  @Test
  fun `update licence status persists the licence and history correctly`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))
    whenever(staffRepository.findByUsernameIgnoreCase(aCom.username)).thenReturn(aCom)

    service.updateLicenceStatus(
      1L,
      StatusUpdateRequest(status = LicenceStatus.REJECTED, username = aCom.username, fullName = "Y"),
    )

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())
    verify(licenceEventRepository, times(0)).saveAndFlush(any())
    verify(staffRepository, times(1)).findByUsernameIgnoreCase(aCom.username)

    assertThat(licenceCaptor.value)
      .extracting("id", "statusCode", "updatedByUsername", "licenceActivatedDate", "updatedBy")
      .isEqualTo(listOf(1L, LicenceStatus.REJECTED, aCom.username, null, aCom))

    assertThat(auditCaptor.value)
      .extracting("licenceId", "username", "fullName", "summary", "eventType")
      .isEqualTo(
        listOf(
          1L,
          aCom.username,
          "${aCom.firstName} ${aCom.lastName}",
          "Licence rejected for ${aLicenceEntity.forename} ${aLicenceEntity.surname}",
          USER_EVENT,
        ),
      )
  }

  @Test
  fun `update licence status to APPROVED sets additional values`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))
    whenever(staffRepository.findByUsernameIgnoreCase(aCom.username)).thenReturn(aCom)

    service.updateLicenceStatus(
      1L,
      StatusUpdateRequest(status = LicenceStatus.APPROVED, username = aCom.username, fullName = "Y"),
    )

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    val eventCaptor = ArgumentCaptor.forClass(EntityLicenceEvent::class.java)
    val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(licenceEventRepository, times(1)).saveAndFlush(eventCaptor.capture())
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())
    verify(notifyService, times(0)).sendLicenceToOmuForReApprovalEmail(any(), any(), any(), any(), any(), anyOrNull())
    verify(staffRepository, times(1)).findByUsernameIgnoreCase(aCom.username)

    assertThat(licenceCaptor.value)
      .extracting("id", "statusCode", "approvedByUsername", "approvedByName", "licenceActivatedDate")
      .isEqualTo(listOf(1L, LicenceStatus.APPROVED, aCom.username, "Y", null))

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
          aCom.username,
          "${aCom.firstName} ${aCom.lastName}",
          "Licence approved for ${aLicenceEntity.forename} ${aLicenceEntity.surname}",
          USER_EVENT,
        ),
      )
  }

  @Test
  fun `update licence status to APPROVED deactivates previous version of CRD licence`() {
    val newLicenceId = 23L
    val fullName = "user 1"
    val firstVersionOfLicence = aLicenceEntity.copy(
      statusCode = LicenceStatus.APPROVED,
      approvedByUsername = aCom.username,
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
    whenever(staffRepository.findByUsernameIgnoreCase(aCom.username)).thenReturn(aCom)

    service.updateLicenceStatus(
      newLicenceId,
      StatusUpdateRequest(status = LicenceStatus.APPROVED, username = aCom.username, fullName = fullName),
    )

    val eventCaptor = ArgumentCaptor.forClass(EntityLicenceEvent::class.java)
    val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)

    verify(licenceEventRepository, times(2)).saveAndFlush(eventCaptor.capture())
    verify(auditEventRepository, times(2)).saveAndFlush(auditCaptor.capture())
    verify(notifyService, never()).sendLicenceToOmuForReApprovalEmail(any(), any(), any(), any(), any(), anyOrNull())

    assertThat(firstVersionOfLicence)
      .extracting("id", "statusCode", "updatedByUsername", "licenceActivatedDate", "updatedBy")
      .isEqualTo(listOf(firstVersionOfLicence.id, LicenceStatus.INACTIVE, aCom.username, null, aCom))

    assertThat(newVersionOfLicence)
      .extracting("id", "statusCode", "approvedByUsername", "approvedByName")
      .isEqualTo(listOf(newVersionOfLicence.id, LicenceStatus.APPROVED, aCom.username, fullName))
    assertThat(newVersionOfLicence.approvedDate).isAfter(LocalDateTime.now().minusMinutes(5L))

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
          aCom.username,
          "${aCom.firstName} ${aCom.lastName}",
          "Licence set to INACTIVE for ${aLicenceEntity.forename} ${aLicenceEntity.surname}",
          USER_EVENT,
        ),
      )

    assertThat(auditCaptor.allValues[1])
      .extracting("licenceId", "username", "fullName", "summary", "eventType")
      .isEqualTo(
        listOf(
          newVersionOfLicence.id,
          aCom.username,
          "${aCom.firstName} ${aCom.lastName}",
          "Licence approved for ${aLicenceEntity.forename} ${aLicenceEntity.surname}",
          USER_EVENT,
        ),
      )
  }

  @Test
  fun `update HDC licence status to APPROVED deactivates previous version of HDC licence`() {
    val newLicenceId = 23L
    val fullName = "user 1"
    val firstVersionOfLicence = anHdcLicenceEntity.copy(
      statusCode = LicenceStatus.APPROVED,
      approvedByUsername = aCom.username,
      approvedByName = fullName,
      approvedDate = LocalDateTime.now(),
    )
    val newVersionOfLicence =
      anHdcLicenceEntity.copy(
        id = newLicenceId,
        statusCode = LicenceStatus.IN_PROGRESS,
        versionOfId = anHdcLicenceEntity.id,
      )

    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(firstVersionOfLicence))
    whenever(licenceRepository.findById(newLicenceId)).thenReturn(Optional.of(newVersionOfLicence))
    whenever(staffRepository.findByUsernameIgnoreCase(aCom.username)).thenReturn(aCom)

    service.updateLicenceStatus(
      newLicenceId,
      StatusUpdateRequest(status = LicenceStatus.APPROVED, username = aCom.username, fullName = fullName),
    )

    val eventCaptor = ArgumentCaptor.forClass(EntityLicenceEvent::class.java)
    val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)

    verify(licenceEventRepository, times(2)).saveAndFlush(eventCaptor.capture())
    verify(auditEventRepository, times(2)).saveAndFlush(auditCaptor.capture())
    verify(notifyService, never()).sendLicenceToOmuForReApprovalEmail(any(), any(), any(), any(), any(), anyOrNull())

    assertThat(firstVersionOfLicence)
      .extracting("id", "statusCode", "updatedByUsername", "licenceActivatedDate", "updatedBy")
      .isEqualTo(listOf(firstVersionOfLicence.id, LicenceStatus.INACTIVE, aCom.username, null, aCom))

    assertThat(newVersionOfLicence)
      .extracting("id", "statusCode", "approvedByUsername", "approvedByName")
      .isEqualTo(listOf(newVersionOfLicence.id, LicenceStatus.APPROVED, aCom.username, fullName))
    assertThat(newVersionOfLicence.approvedDate).isAfter(LocalDateTime.now().minusMinutes(5L))

    assertThat(eventCaptor.allValues[0])
      .extracting("licenceId", "eventType", "eventDescription")
      .isEqualTo(
        listOf(
          firstVersionOfLicence.id,
          LicenceEventType.SUPERSEDED,
          "Licence deactivated as a newer version was approved for ${anHdcLicenceEntity.forename} ${anHdcLicenceEntity.surname}",
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
          aCom.username,
          "${aCom.firstName} ${aCom.lastName}",
          "Licence set to INACTIVE for ${anHdcLicenceEntity.forename} ${anHdcLicenceEntity.surname}",
          USER_EVENT,
        ),
      )

    assertThat(auditCaptor.allValues[1])
      .extracting("licenceId", "username", "fullName", "summary", "eventType")
      .isEqualTo(
        listOf(
          newVersionOfLicence.id,
          aCom.username,
          "${aCom.firstName} ${aCom.lastName}",
          "Licence approved for ${anHdcLicenceEntity.forename} ${anHdcLicenceEntity.surname}",
          USER_EVENT,
        ),
      )
  }

  @Test
  fun `update PRRD licence status to APPROVED deactivates previous version of PRRD licence`() {
    val newLicenceId = 23L
    val fullName = "user 1"

    val aPrrdLicence = createPrrdLicence()
    val firstVersionOfLicence = aPrrdLicence.copy(
      statusCode = LicenceStatus.APPROVED,
      approvedByUsername = aCom.username,
      approvedByName = fullName,
      approvedDate = LocalDateTime.now(),
    )
    val newVersionOfLicence =
      aPrrdLicence.copy(
        id = newLicenceId,
        statusCode = LicenceStatus.IN_PROGRESS,
        versionOfId = aPrrdLicence.id,
      )

    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(firstVersionOfLicence))
    whenever(licenceRepository.findById(newLicenceId)).thenReturn(Optional.of(newVersionOfLicence))
    whenever(staffRepository.findByUsernameIgnoreCase(aCom.username)).thenReturn(aCom)

    service.updateLicenceStatus(
      newLicenceId,
      StatusUpdateRequest(status = LicenceStatus.APPROVED, username = aCom.username, fullName = fullName),
    )

    val eventCaptor = ArgumentCaptor.forClass(EntityLicenceEvent::class.java)
    val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)

    verify(licenceEventRepository, times(2)).saveAndFlush(eventCaptor.capture())
    verify(auditEventRepository, times(2)).saveAndFlush(auditCaptor.capture())
    verify(notifyService, never()).sendLicenceToOmuForReApprovalEmail(any(), any(), any(), any(), any(), anyOrNull())

    assertThat(firstVersionOfLicence)
      .extracting("id", "statusCode", "updatedByUsername", "licenceActivatedDate", "updatedBy")
      .isEqualTo(listOf(firstVersionOfLicence.id, LicenceStatus.INACTIVE, aCom.username, null, aCom))

    assertThat(newVersionOfLicence)
      .extracting("id", "statusCode", "approvedByUsername", "approvedByName")
      .isEqualTo(listOf(newVersionOfLicence.id, LicenceStatus.APPROVED, aCom.username, fullName))
    assertThat(newVersionOfLicence.approvedDate).isAfter(LocalDateTime.now().minusMinutes(5L))

    assertThat(eventCaptor.allValues[0])
      .extracting("licenceId", "eventType", "eventDescription")
      .isEqualTo(
        listOf(
          firstVersionOfLicence.id,
          LicenceEventType.SUPERSEDED,
          "Licence deactivated as a newer version was approved for ${newVersionOfLicence.forename} ${newVersionOfLicence.surname}",
        ),
      )

    assertThat(eventCaptor.allValues[1])
      .extracting("licenceId", "eventType", "eventDescription")
      .isEqualTo(
        listOf(
          newVersionOfLicence.id,
          LicenceEventType.APPROVED,
          "Licence updated to APPROVED for ${aPrrdLicence.forename} ${aPrrdLicence.surname}",
        ),
      )

    assertThat(auditCaptor.allValues[0])
      .extracting("licenceId", "username", "fullName", "summary", "eventType")
      .isEqualTo(
        listOf(
          firstVersionOfLicence.id,
          aCom.username,
          "${aCom.firstName} ${aCom.lastName}",
          "Licence set to INACTIVE for ${aPrrdLicence.forename} ${aPrrdLicence.surname}",
          USER_EVENT,
        ),
      )

    assertThat(auditCaptor.allValues[1])
      .extracting("licenceId", "username", "fullName", "summary", "eventType")
      .isEqualTo(
        listOf(
          newVersionOfLicence.id,
          aCom.username,
          "${aCom.firstName} ${aCom.lastName}",
          "Licence approved for ${aPrrdLicence.forename} ${aPrrdLicence.surname}",
          USER_EVENT,
        ),
      )
  }

  @Test
  fun `update licence status to APPROVED works for HardStop licence`() {
    val hardstopLicence = createHardStopLicence().copy(
      id = 1L,
      statusCode = LicenceStatus.APPROVED,
      approvedByUsername = aCom.username,
      approvedByName = "user 1",
      approvedDate = LocalDateTime.now(),
    )

    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(hardstopLicence))
    whenever(staffRepository.findByUsernameIgnoreCase(aCom.username)).thenReturn(aCom)

    service.updateLicenceStatus(
      1L,
      StatusUpdateRequest(
        status = LicenceStatus.APPROVED,
        username = aCom.username,
        fullName = hardstopLicence.approvedByName,
      ),
    )

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    val eventCaptor = ArgumentCaptor.forClass(EntityLicenceEvent::class.java)
    val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(licenceEventRepository, times(1)).saveAndFlush(eventCaptor.capture())
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())
    verify(notifyService, never()).sendLicenceToOmuForReApprovalEmail(any(), any(), any(), any(), any(), anyOrNull())

    assertThat(licenceCaptor.allValues[0])
      .extracting("id", "statusCode", "approvedByUsername", "approvedByName")
      .isEqualTo(listOf(hardstopLicence.id, LicenceStatus.APPROVED, aCom.username, hardstopLicence.approvedByName))
    assertThat(licenceCaptor.allValues[0].approvedDate).isAfter(LocalDateTime.now().minusMinutes(5L))

    assertThat(eventCaptor.allValues[0])
      .extracting("licenceId", "eventType", "eventDescription")
      .isEqualTo(
        listOf(
          hardstopLicence.id,
          LicenceEventType.APPROVED,
          "Licence updated to APPROVED for ${hardstopLicence.forename} ${hardstopLicence.surname}",
        ),
      )

    assertThat(auditCaptor.allValues[0])
      .extracting("licenceId", "username", "fullName", "summary", "eventType")
      .isEqualTo(
        listOf(
          hardstopLicence.id,
          aCom.username,
          "${aCom.firstName} ${aCom.lastName}",
          "Licence approved for ${hardstopLicence.forename} ${hardstopLicence.surname}",
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
        email = "test@test.com",
        dateCreated = LocalDateTime.now(),
      ),
    )

    whenever(staffRepository.findByUsernameIgnoreCase(aCom.username)).thenReturn(aCom)

    service.updateLicenceStatus(
      1L,
      StatusUpdateRequest(status = LicenceStatus.IN_PROGRESS, username = aCom.username, fullName = "Y"),
    )

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())
    verify(licenceEventRepository, times(0)).saveAndFlush(any())
    verify(notifyService, never()).sendLicenceToOmuForReApprovalEmail(
      eq("test@test.com"),
      eq(aLicenceEntity.forename ?: "unknown"),
      eq(aLicenceEntity.surname ?: "unknown"),
      eq(aLicenceEntity.nomsId),
      eq(aLicenceEntity.licenceStartDate),
      anyOrNull(),
    )
    verify(staffRepository, times(1)).findByUsernameIgnoreCase(aCom.username)

    assertThat(licenceCaptor.value)
      .extracting(
        "id",
        "statusCode",
        "updatedByUsername",
        "approvedByUsername",
        "approvedDate",
        "licenceActivatedDate",
        "updatedBy",
      )
      .isEqualTo(listOf(1L, LicenceStatus.IN_PROGRESS, aCom.username, null, null, null, aCom))

    assertThat(auditCaptor.value)
      .extracting("licenceId", "username", "fullName", "summary")
      .isEqualTo(
        listOf(
          1L,
          aCom.username,
          "${aCom.firstName} ${aCom.lastName}",
          "Licence edited for ${aLicenceEntity.forename} ${aLicenceEntity.surname}",
        ),
      )
  }

  @Test
  fun `update an APPROVED licence back to SUBMITTED sends a variation for re approval email`() {
    // Given
    val username = aCom.username
    val fullName = "Y"

    val request = StatusUpdateRequest(status = LicenceStatus.SUBMITTED, username = username, fullName = fullName)
    val omuContact = OmuContact(
      prisonCode = "BXI",
      email = "test@test.com",
      dateCreated = LocalDateTime.now(),
    )

    whenever(licenceRepository.findById(1L))
      .thenReturn(Optional.of(aLicenceEntity.copy(statusCode = LicenceStatus.APPROVED)))

    whenever(omuService.getOmuContactEmail(any())).thenReturn(omuContact)
    whenever(staffRepository.findByUsernameIgnoreCase(username)).thenReturn(aCom)

    // When
    service.updateLicenceStatus(1L, request)

    // Then
    verify(notifyService, times(1)).sendLicenceToOmuForReApprovalEmail(
      eq("test@test.com"),
      eq(aLicenceEntity.forename!!),
      eq(aLicenceEntity.surname!!),
      eq(aLicenceEntity.nomsId),
      eq(aLicenceEntity.licenceStartDate),
      eq(aLicenceEntity.conditionalReleaseDate),
    )
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

    verify(notifyService, times(0)).sendLicenceToOmuForReApprovalEmail(
      eq("test@test.com"),
      eq(aLicenceEntity.forename ?: ""),
      eq(aLicenceEntity.surname ?: ""),
      eq(aLicenceEntity.nomsId),
      eq(aLicenceEntity.licenceStartDate),
      anyOrNull(),
    )
  }

  @Test
  fun `update licenceActivatedDate field when licence status set to ACTIVE`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))
    whenever(staffRepository.findByUsernameIgnoreCase(aCom.username)).thenReturn(aCom)

    service.updateLicenceStatus(
      1L,
      StatusUpdateRequest(status = LicenceStatus.ACTIVE, username = aCom.username, fullName = "Y"),
    )

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())
    verify(domainEventsService, times(1)).recordDomainEvent(licenceCaptor.value, LicenceStatus.ACTIVE)
    verify(staffRepository, times(1)).findByUsernameIgnoreCase(aCom.username)

    assertThat(licenceCaptor.value.licenceActivatedDate).isNotNull()

    assertThat(licenceCaptor.value)
      .extracting("id", "statusCode", "updatedByUsername", "licenceActivatedDate", "updatedBy")
      .isEqualTo(listOf(1L, LicenceStatus.ACTIVE, aCom.username, licenceCaptor.value.licenceActivatedDate, aCom))

    assertThat(auditCaptor.value)
      .extracting("licenceId", "username", "fullName", "summary", "eventType")
      .isEqualTo(
        listOf(
          1L,
          aCom.username,
          "${aCom.firstName} ${aCom.lastName}",
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
    whenever(staffRepository.findByUsernameIgnoreCase(aCom.username)).thenReturn(aCom)

    service.updateLicenceStatus(
      1L,
      StatusUpdateRequest(status = LicenceStatus.ACTIVE, username = aCom.username, fullName = "Y"),
    )

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    val inProgressLicenceCaptor = argumentCaptor<List<EntityLicence>>()
    val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(licenceRepository, times(1)).saveAllAndFlush(inProgressLicenceCaptor.capture())
    verify(auditEventRepository, times(2)).saveAndFlush(auditCaptor.capture())
    verify(domainEventsService, times(1)).recordDomainEvent(licenceCaptor.value, LicenceStatus.ACTIVE)
    verify(staffRepository, times(1)).findByUsernameIgnoreCase(aCom.username)

    assertThat(licenceCaptor.value.licenceActivatedDate).isNotNull()

    assertThat(licenceCaptor.value)
      .extracting("id", "statusCode", "updatedByUsername", "licenceActivatedDate", "updatedBy")
      .isEqualTo(listOf(1L, LicenceStatus.ACTIVE, aCom.username, licenceCaptor.value.licenceActivatedDate, aCom))
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
          "Deactivating licence as the parent licence version was activated for Person One",
          SYSTEM_EVENT,
        ),
      )
    assertThat(auditCaptor.allValues[1]).extracting("licenceId", "username", "fullName", "summary", "eventType")
      .isEqualTo(
        listOf(
          aLicenceEntity.id,
          aCom.username,
          "${aCom.firstName} ${aCom.lastName}",
          "Licence set to ACTIVE for ${aLicenceEntity.forename} ${aLicenceEntity.surname}",
          USER_EVENT,
        ),
      )
  }

  @Test
  fun `update licence status to ACTIVE deactivates any TIMED_OUT version of a licence`() {
    val timedOutLicenceVersion =
      aLicenceEntity.copy(id = 99999, statusCode = LicenceStatus.TIMED_OUT, versionOfId = aLicenceEntity.id)

    whenever(licenceRepository.findById(aLicenceEntity.id)).thenReturn(Optional.of(aLicenceEntity))
    whenever(
      crdLicenceRepository.findAllByBookingIdInAndStatusCodeOrderByDateCreatedDesc(
        listOf(aLicenceEntity.bookingId!!),
        LicenceStatus.TIMED_OUT,
      ),
    ).thenReturn(listOf(timedOutLicenceVersion))
    whenever(staffRepository.findByUsernameIgnoreCase(aCom.username)).thenReturn(aCom)

    service.updateLicenceStatus(
      1L,
      StatusUpdateRequest(status = LicenceStatus.ACTIVE, username = aCom.username, fullName = "Y"),
    )

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    val timedOutLicenceCaptor = argumentCaptor<List<EntityLicence>>()
    val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(licenceRepository, times(1)).saveAllAndFlush(timedOutLicenceCaptor.capture())
    verify(auditEventRepository, times(2)).saveAndFlush(auditCaptor.capture())
    verify(domainEventsService, times(1)).recordDomainEvent(licenceCaptor.value, LicenceStatus.ACTIVE)
    verify(staffRepository, times(1)).findByUsernameIgnoreCase(aCom.username)

    assertThat(licenceCaptor.value.licenceActivatedDate).isNotNull()

    assertThat(licenceCaptor.value)
      .extracting("id", "statusCode", "updatedByUsername", "licenceActivatedDate", "updatedBy")
      .isEqualTo(listOf(1L, LicenceStatus.ACTIVE, aCom.username, licenceCaptor.value.licenceActivatedDate, aCom))
    assertThat(timedOutLicenceCaptor.firstValue[0])
      .extracting("id", "statusCode")
      .isEqualTo(listOf(timedOutLicenceVersion.id, LicenceStatus.INACTIVE))

    assertThat(auditCaptor.allValues[0])
      .extracting("licenceId", "username", "fullName", "summary", "eventType")
      .isEqualTo(
        listOf(
          timedOutLicenceVersion.id,
          "SYSTEM",
          "SYSTEM",
          "Deactivating licence as the parent licence version was activated for Person One",
          SYSTEM_EVENT,
        ),
      )
    assertThat(auditCaptor.allValues[1]).extracting("licenceId", "username", "fullName", "summary", "eventType")
      .isEqualTo(
        listOf(
          aLicenceEntity.id,
          aCom.username,
          "${aCom.firstName} ${aCom.lastName}",
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
    whenever(staffRepository.findByUsernameIgnoreCase(aCom.username)).thenReturn(aCom)

    service.updateLicenceStatus(
      1L,
      StatusUpdateRequest(status = LicenceStatus.INACTIVE, username = aCom.username, fullName = "Y"),
    )

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    val inProgressLicenceCaptor = argumentCaptor<List<EntityLicence>>()
    val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(licenceRepository, times(1)).saveAllAndFlush(inProgressLicenceCaptor.capture())
    verify(auditEventRepository, times(2)).saveAndFlush(auditCaptor.capture())
    verify(domainEventsService, times(1)).recordDomainEvent(licenceCaptor.value, LicenceStatus.INACTIVE)
    verify(staffRepository, times(1)).findByUsernameIgnoreCase(aCom.username)

    assertThat(licenceCaptor.value)
      .extracting("id", "statusCode", "updatedByUsername", "licenceActivatedDate", "updatedBy")
      .isEqualTo(
        listOf(
          aLicenceEntity.id,
          LicenceStatus.INACTIVE,
          aCom.username,
          licenceCaptor.value.licenceActivatedDate,
          aCom,
        ),
      )
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
          aCom.username,
          "${aCom.firstName} ${aCom.lastName}",
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
    whenever(staffRepository.findByUsernameIgnoreCase(aCom.username)).thenReturn(aCom)

    service.updateLicenceStatus(
      1L,
      StatusUpdateRequest(status = LicenceStatus.APPROVED, username = aCom.username, fullName = "Y"),
    )

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(staffRepository, times(1)).findByUsernameIgnoreCase(aCom.username)

    assertThat(licenceCaptor.value.licenceActivatedDate).isNotNull()

    assertThat(licenceCaptor.value)
      .extracting("id", "statusCode", "updatedByUsername", "licenceActivatedDate", "updatedBy")
      .isEqualTo(listOf(1L, LicenceStatus.APPROVED, aCom.username, licence.licenceActivatedDate, aCom))
  }

  @Test
  fun `attempting to update a variation to APPROVED throws an error`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aVariationLicence))
    whenever(staffRepository.findByUsernameIgnoreCase(aCom.username)).thenReturn(aCom)

    assertThrows<IllegalStateException> {
      service.updateLicenceStatus(
        1L,
        StatusUpdateRequest(status = LicenceStatus.APPROVED, username = aCom.username, fullName = "Y"),
      )
    }

    verify(licenceRepository, times(0)).saveAndFlush(any())
    verify(licenceEventRepository, times(0)).saveAndFlush(any())
    verify(auditEventRepository, times(0)).saveAndFlush(any())
    verify(notifyService, times(0)).sendLicenceToOmuForReApprovalEmail(any(), any(), any(), any(), any(), anyOrNull())
    verify(staffRepository, times(1)).findByUsernameIgnoreCase(aCom.username)
  }

  @Test
  fun `attempting to update an HDC variation to APPROVED throws an error`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(anHdcVariationLicence))
    whenever(staffRepository.findByUsernameIgnoreCase(aCom.username)).thenReturn(aCom)

    assertThrows<IllegalStateException> {
      service.updateLicenceStatus(
        1L,
        StatusUpdateRequest(status = LicenceStatus.APPROVED, username = aCom.username, fullName = "Y"),
      )
    }

    verify(licenceRepository, times(0)).saveAndFlush(any())
    verify(licenceEventRepository, times(0)).saveAndFlush(any())
    verify(auditEventRepository, times(0)).saveAndFlush(any())
    verify(notifyService, times(0)).sendLicenceToOmuForReApprovalEmail(any(), any(), any(), any(), any(), anyOrNull())
    verify(staffRepository, times(1)).findByUsernameIgnoreCase(aCom.username)
  }

  @Test
  fun `updating licence status to submitted should set the submitted date`() {
    val licence = aLicenceEntity.copy(
      licenceActivatedDate = LocalDateTime.now(),
    )
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(licence))
    whenever(staffRepository.findByUsernameIgnoreCase(aCom.username)).thenReturn(aCom)

    service.updateLicenceStatus(
      1L,
      StatusUpdateRequest(status = LicenceStatus.SUBMITTED, username = aCom.username, fullName = "Y"),
    )

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(staffRepository, times(1)).findByUsernameIgnoreCase(aCom.username)

    assertThat(licenceCaptor.value.submittedDate).isNotNull()

    assertThat(licenceCaptor.value)
      .extracting("id", "statusCode", "updatedByUsername", "updatedBy")
      .isEqualTo(listOf(1L, LicenceStatus.SUBMITTED, aCom.username, aCom))
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
    verifyNoInteractions(staffRepository)
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
    verifyNoInteractions(staffRepository)
  }

  @Test
  fun `submit a CRD licence saves new fields to the licence`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))
    whenever(staffRepository.findByUsernameIgnoreCase(aCom.username)).thenReturn(aCom)
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(listOf(aPrisonerSearchPrisoner))
    whenever(
      eligibilityService.getEligibilityAssessment(
        eq(aPrisonerSearchPrisoner),
      ),
    ).thenReturn(anEligibilityAssessment())

    service.submitLicence(1L, emptyList())

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    val eventCaptor = ArgumentCaptor.forClass(EntityLicenceEvent::class.java)
    val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(licenceEventRepository, times(1)).saveAndFlush(eventCaptor.capture())
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())
    verify(staffRepository, times(1)).findByUsernameIgnoreCase(aCom.username)

    assertThat(licenceCaptor.value.submittedDate).isNotNull()
    assertThat(licenceCaptor.value)
      .extracting("id", "statusCode", "updatedByUsername", "updatedBy")
      .isEqualTo(listOf(1L, LicenceStatus.SUBMITTED, aCom.username, aCom))

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
          aCom.username,
          "X Y",
          "Licence submitted for approval for ${aLicenceEntity.forename} ${aLicenceEntity.surname}",
        ),
      )
  }

  @Test
  fun `submit a hard stop licence saves new fields to the licence`() {
    val authentication = mock<Authentication>()
    val securityContext = mock<SecurityContext>()

    whenever(authentication.name).thenReturn("tca")
    whenever(securityContext.authentication).thenReturn(authentication)
    SecurityContextHolder.setContext(securityContext)

    val caseAdmin = PrisonUser(
      username = "tca",
      email = "testemail@prison.gov.uk",
      firstName = "X",
      lastName = "Y",
    )

    val hardStopLicence = createHardStopLicence()
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(hardStopLicence))
    whenever(staffRepository.findByUsernameIgnoreCase("tca")).thenReturn(caseAdmin)
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(listOf(aPrisonerSearchPrisoner))
    whenever(eligibilityService.getEligibilityAssessment(eq(aPrisonerSearchPrisoner))).thenReturn(
      anEligibilityAssessment(),
    )

    service.submitLicence(1L, emptyList())

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    val eventCaptor = ArgumentCaptor.forClass(EntityLicenceEvent::class.java)
    val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(licenceEventRepository, times(1)).saveAndFlush(eventCaptor.capture())
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())

    assertThat(licenceCaptor.value.submittedDate).isNotNull()
    assertThat(licenceCaptor.value)
      .extracting("id", "statusCode", "updatedByUsername", "updatedBy")
      .isEqualTo(listOf(1L, LicenceStatus.SUBMITTED, caseAdmin.username, caseAdmin))

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
          "tca",
          "X Y",
          "Licence submitted for approval for ${hardStopLicence.forename} ${hardStopLicence.surname}",
        ),
      )
  }

  @Test
  fun `attempting to submit a licence for an ineligible case results in validation exception `() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))
    whenever(staffRepository.findByUsernameIgnoreCase(aCom.username)).thenReturn(aCom)
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(listOf(aPrisonerSearchPrisoner))

    whenever(eligibilityService.getEligibilityAssessment(eq(aPrisonerSearchPrisoner))).thenReturn(
      anIneligibleEligibilityAssessment(),
    )

    val exception = assertThrows<ValidationException> { service.submitLicence(1L, emptyList()) }

    verify(licenceRepository, times(0)).saveAndFlush(any())
    verify(licenceEventRepository, times(0)).saveAndFlush(any())
    verify(auditEventRepository, times(0)).saveAndFlush(any())
    assertThat(exception).isInstanceOf(ValidationException::class.java)
    assertThat(exception).message().isEqualTo("Unable to perform action, licence 1 is ineligible for CVL")
  }

  @Test
  fun `submitting a licence variation`() {
    val variation = createVariationLicence().copy(
      variationOfId = 1,
      submittedBy = aCom,
    )

    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(variation))
    whenever(staffRepository.findByUsernameIgnoreCase(aCom.username)).thenReturn(aCom)
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(listOf(aPrisonerSearchPrisoner))
    whenever(eligibilityService.getEligibilityAssessment(eq(aPrisonerSearchPrisoner))).thenReturn(
      anEligibilityAssessment(),
    )

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
        eq(variation.submittedBy?.fullName ?: ""),
      )

    assertThat(licenceCaptor.value)
      .extracting("id", "statusCode", "updatedByUsername", "updatedBy")
      .isEqualTo(listOf(1L, LicenceStatus.VARIATION_SUBMITTED, aCom.username, aCom))

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
          aCom.username,
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
  fun `activate licences deactivates an older timed out version of a licence`() {
    val licenceCaptor = argumentCaptor<List<Licence>>()
    val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)
    val eventCaptor = ArgumentCaptor.forClass(EntityLicenceEvent::class.java)

    val approvedLicenceVersion = aLicenceEntity.copy(statusCode = LicenceStatus.APPROVED)
    val timedOutVersion = approvedLicenceVersion.copy(
      id = 99999,
      statusCode = LicenceStatus.TIMED_OUT,
      versionOfId = approvedLicenceVersion.id,
    )
    whenever(
      crdLicenceRepository.findAllByBookingIdInAndStatusCodeOrderByDateCreatedDesc(
        listOf(approvedLicenceVersion.bookingId!!),
        LicenceStatus.TIMED_OUT,
      ),
    ).thenReturn(listOf(timedOutVersion))

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
    verify(domainEventsService, times(1)).recordDomainEvent(timedOutVersion, LicenceStatus.INACTIVE)

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
          timedOutVersion.id,
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
        listOf(timedOutVersion.id, LicenceEventType.SUPERSEDED, "SYSTEM", "SYSTEM"),
      )
  }

  @Test
  fun `inactivate licences sets licence statuses to INACTIVE`() {
    val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)
    val eventCaptor = ArgumentCaptor.forClass(EntityLicenceEvent::class.java)

    service.inactivateLicences(
      listOf(aLicenceEntity.copy(statusCode = LicenceStatus.APPROVED)),
      deactivateInProgressVersions = true,
    )

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

    service.inactivateLicences(
      listOf(aLicenceEntity.copy(statusCode = LicenceStatus.APPROVED)),
      "Test reason",
      deactivateInProgressVersions = true,
    )

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

    service.inactivateLicences(
      listOf(aLicenceEntity.copy(statusCode = LicenceStatus.APPROVED)),
      deactivateInProgressVersions = true,
    )

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
    val variation = createVariationLicence()
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(variation))
    whenever(staffRepository.findByUsernameIgnoreCase(aCom.username)).thenReturn(aCom)

    service.updateSpoDiscussion(1L, UpdateSpoDiscussionRequest(spoDiscussion = "Yes"))

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(staffRepository, times(1)).findByUsernameIgnoreCase(aCom.username)

    assertThat(licenceCaptor.value)
      .extracting("spoDiscussion", "updatedByUsername", "updatedBy")
      .isEqualTo(listOf("Yes", aCom.username, aCom))
  }

  @Test
  fun `update vlo discussion persists the updated entity`() {
    val variation = createVariationLicence()
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(variation))
    whenever(staffRepository.findByUsernameIgnoreCase(aCom.username)).thenReturn(aCom)

    service.updateVloDiscussion(1L, UpdateVloDiscussionRequest(vloDiscussion = "Yes"))

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(staffRepository, times(1)).findByUsernameIgnoreCase(aCom.username)

    assertThat(licenceCaptor.value)
      .extracting("vloDiscussion", "updatedByUsername", "updatedBy")
      .isEqualTo(listOf("Yes", aCom.username, aCom))
  }

  @Test
  fun `update reason for variation creates a licence event containing the reason`() {
    val variation = createVariationLicence()
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(variation))
    whenever(staffRepository.findByUsernameIgnoreCase(aCom.username)).thenReturn(aCom)

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    val eventCaptor = ArgumentCaptor.forClass(EntityLicenceEvent::class.java)

    service.updateReasonForVariation(1L, UpdateReasonForVariationRequest(reasonForVariation = "reason"))

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(licenceEventRepository, times(1)).saveAndFlush(eventCaptor.capture())
    verify(staffRepository, times(1)).findByUsernameIgnoreCase(aCom.username)

    assertThat(licenceCaptor.value)
      .extracting("updatedByUsername", "updatedBy")
      .isEqualTo(listOf(aCom.username, aCom))

    assertThat(eventCaptor.value)
      .extracting("eventType", "username", "eventDescription")
      .isEqualTo(listOf(LicenceEventType.VARIATION_SUBMITTED_REASON, aCom.username, "reason"))
  }

  @Test
  fun `creating a variation`() {
    whenever(staffRepository.findByUsernameIgnoreCase(any())).thenReturn(communityOffenderManager())
    whenever(licencePolicyService.currentPolicy(any())).thenReturn(
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

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
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
  fun `creating an HDC variation`() {
    whenever(staffRepository.findByUsernameIgnoreCase(any())).thenReturn(communityOffenderManager())
    whenever(licencePolicyService.currentPolicy(any())).thenReturn(
      LicencePolicy(
        "2.1",
        standardConditions = StandardConditions(emptyList(), emptyList()),
        additionalConditions = AdditionalConditions(emptyList(), emptyList()),
        changeHints = emptyList(),
      ),
    )
    whenever(licenceRepository.findById(1L)).thenReturn(
      Optional.of(anHdcLicenceEntity),
    )
    whenever(licenceRepository.save(any())).thenReturn(anHdcLicenceEntity)
    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    val licenceEventCaptor = ArgumentCaptor.forClass(LicenceEvent::class.java)

    service.createVariation(1L)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    with(licenceCaptor.value as HdcVariationLicence) {
      assertThat(kind).isEqualTo(LicenceKind.HDC_VARIATION)
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
    whenever(staffRepository.findByUsernameIgnoreCase(any())).thenReturn(communityOffenderManager())
    whenever(licencePolicyService.currentPolicy(any())).thenReturn(
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
    val newLicenceCaptor = argumentCaptor<Licence>()

    service.createVariation(1L)

    verify(licenceRepository, times(1)).saveAndFlush(newLicenceCaptor.capture())
    val newLicence = newLicenceCaptor.firstValue

    assertThat(newLicence.additionalConditions.size).isEqualTo(2)
    assertThat(newLicence.additionalConditions.first().conditionType).isEqualTo(LicenceType.AP.toString())
    assertThat(newLicence.additionalConditions.last().conditionType).isEqualTo(LicenceType.PSS.toString())
  }

  @Test
  fun `creating a variation with no responsible communityOffenderManager allocated`() {
    whenever(staffRepository.findByUsernameIgnoreCase(any())).thenReturn(communityOffenderManager())
    whenever(licencePolicyService.currentPolicy(any())).thenReturn(
      LicencePolicy(
        "2.1",
        standardConditions = StandardConditions(emptyList(), emptyList()),
        additionalConditions = AdditionalConditions(emptyList(), emptyList()),
        changeHints = emptyList(),
      ),
    )
    whenever(licenceRepository.findById(1L)).thenReturn(
      Optional.of(aTimeServedLicence.copy(responsibleCom = null)),
    )
    whenever(licenceRepository.save(any())).thenReturn(aTimeServedLicence)
    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    val licenceEventCaptor = ArgumentCaptor.forClass(LicenceEvent::class.java)

    service.createVariation(1L)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    with(licenceCaptor.value as VariationLicence) {
      assertThat(kind).isEqualTo(LicenceKind.VARIATION)
      assertThat(version).isEqualTo("2.1")
      assertThat(statusCode).isEqualTo(LicenceStatus.VARIATION_IN_PROGRESS)
      assertThat(variationOfId).isEqualTo(1)
      assertThat(licenceVersion).isEqualTo("2.0")
      assertThat(responsibleCom).isNull()
    }
    verify(licenceEventRepository).saveAndFlush(licenceEventCaptor.capture())
    assertThat(licenceEventCaptor.value.eventType).isEqualTo(LicenceEventType.VARIATION_CREATED)
  }

  @Test
  fun `editing an approved licence creates and saves a new licence version`() {
    whenever(staffRepository.findByUsernameIgnoreCase(any())).thenReturn(communityOffenderManager())
    whenever(licencePolicyService.currentPolicy(any())).thenReturn(
      LicencePolicy(
        "2.1",
        standardConditions = StandardConditions(emptyList(), emptyList()),
        additionalConditions = AdditionalConditions(emptyList(), emptyList()),
        changeHints = emptyList(),
      ),
    )

    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(listOf(aPrisonerSearchPrisoner))
    whenever(eligibilityService.getEligibilityAssessment(eq(aPrisonerSearchPrisoner))).thenReturn(
      anEligibilityAssessment(),
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

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
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
    whenever(staffRepository.findByUsernameIgnoreCase(any())).thenReturn(communityOffenderManager())
    whenever(licencePolicyService.currentPolicy(any())).thenReturn(
      LicencePolicy(
        "2.1",
        standardConditions = StandardConditions(emptyList(), emptyList()),
        additionalConditions = AdditionalConditions(emptyList(), emptyList()),
        changeHints = emptyList(),
      ),
    )

    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(listOf(aPrisonerSearchPrisoner))
    whenever(eligibilityService.getEligibilityAssessment(eq(aPrisonerSearchPrisoner))).thenReturn(
      anEligibilityAssessment(),
    )

    val approvedLicence = aLicenceEntity.copy(
      statusCode = LicenceStatus.APPROVED,
      additionalConditions = additionalConditions,
    )
    whenever(licenceRepository.findById(1L)).thenReturn(
      Optional.of(approvedLicence),
    )

    whenever(licenceRepository.save(any())).thenReturn(approvedLicence)

    val newLicenceCaptor = argumentCaptor<Licence>()
    service.editLicence(1L)

    verify(licenceRepository, times(1)).saveAndFlush(newLicenceCaptor.capture())
    val newLicence = newLicenceCaptor.firstValue

    assertThat(newLicence.additionalConditions.size).isEqualTo(2)
    assertThat(newLicence.additionalConditions.first().conditionType).isEqualTo(LicenceType.AP.toString())
    assertThat(newLicence.additionalConditions.last().conditionType).isEqualTo(LicenceType.PSS.toString())
  }

  @Test
  fun `editing an approved licence creates and saves a new licence version and sends a reapproval email`() {
    whenever(staffRepository.findByUsernameIgnoreCase(any())).thenReturn(communityOffenderManager())
    whenever(licencePolicyService.currentPolicy(any())).thenReturn(
      LicencePolicy(
        "2.1",
        standardConditions = StandardConditions(emptyList(), emptyList()),
        additionalConditions = AdditionalConditions(emptyList(), emptyList()),
        changeHints = emptyList(),
      ),
    )

    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(listOf(aPrisonerSearchPrisoner))
    whenever(eligibilityService.getEligibilityAssessment(eq(aPrisonerSearchPrisoner))).thenReturn(
      anEligibilityAssessment(),
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
        email = "test@test.com",
        dateCreated = LocalDateTime.now(),
      ),
    )
    whenever(licenceRepository.save(any())).thenReturn(approvedLicence)

    service.editLicence(1L)
    verify(notifyService, times(1)).sendLicenceToOmuForReApprovalEmail(
      eq("test@test.com"),
      eq(aLicenceEntity.forename ?: "unknown"),
      eq(aLicenceEntity.surname ?: "unknown"),
      eq(aLicenceEntity.nomsId),
      any(),
      anyOrNull(),
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
  fun `attempting to edit a licence that is ineligible for CVL results in validation exception`() {
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(listOf(aPrisonerSearchPrisoner))

    whenever(eligibilityService.getEligibilityAssessment(eq(aPrisonerSearchPrisoner))).thenReturn(
      anIneligibleEligibilityAssessment(),
    )

    val approvedLicence = aLicenceEntity.copy(
      statusCode = LicenceStatus.APPROVED,
      additionalConditions = additionalConditions,
    )
    whenever(licenceRepository.findById(1L)).thenReturn(
      Optional.of(approvedLicence),
    )

    val exception = assertThrows<ValidationException> { service.editLicence(1L) }

    verify(licenceEventRepository, times(0)).saveAndFlush(any())
    verify(auditEventRepository, times(0)).saveAndFlush(any())
    assertThat(exception).isInstanceOf(ValidationException::class.java)
    assertThat(exception).message().isEqualTo("Unable to perform action, licence 1 is ineligible for CVL")
  }

  @Test
  fun `editing an approved licence which already has an in progress version returns that version`() {
    whenever(staffRepository.findByUsernameIgnoreCase(any())).thenReturn(communityOffenderManager())
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
    whenever(staffRepository.findByUsernameIgnoreCase(any())).thenReturn(communityOffenderManager())
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

    verify(notifyService, never()).sendLicenceToOmuForReApprovalEmail(any(), any(), any(), any(), any(), anyOrNull())
  }

  @Test
  fun `discarding a licence`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))
    whenever(staffRepository.findByUsernameIgnoreCase(aCom.username)).thenReturn(aCom)

    val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)

    service.discardLicence(1L)

    verify(licenceRepository, times(1)).delete(aLicenceEntity)
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())
    verify(staffRepository, times(1)).findByUsernameIgnoreCase(aCom.username)
    verify(uploadFileConditionsService, times(1)).deleteDocuments(any())

    assertThat(auditCaptor.value)
      .extracting("licenceId", "username", "fullName", "summary")
      .isEqualTo(
        listOf(
          1L,
          aCom.username,
          "X Y",
          "Licence variation discarded for ${aLicenceEntity.forename} ${aLicenceEntity.surname}",
        ),
      )
  }

  @Test
  fun `update prison information persists the updated entity`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))
    whenever(staffRepository.findByUsernameIgnoreCase(aCom.username)).thenReturn(aCom)

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
    verify(staffRepository, times(1)).findByUsernameIgnoreCase(aCom.username)

    assertThat(licenceCaptor.value)
      .extracting("prisonCode", "prisonDescription", "prisonTelephone", "updatedByUsername", "updatedBy")
      .isEqualTo(listOf("PVI", "Pentonville (HMP)", "+44 276 54545", aCom.username, aCom))

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
    val variation = createVariationLicence()
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(variation))
    whenever(staffRepository.findByUsernameIgnoreCase(aCom.username)).thenReturn(aCom)

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    val eventCaptor = ArgumentCaptor.forClass(EntityLicenceEvent::class.java)
    val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)

    service.referLicenceVariation(1L, referVariationRequest)

    verify(licenceRepository, times(1)).findById(1L)
    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(licenceEventRepository, times(1)).saveAndFlush(eventCaptor.capture())
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())
    verify(staffRepository, times(1)).findByUsernameIgnoreCase(aCom.username)

    assertThat(licenceCaptor.value)
      .extracting("statusCode", "updatedByUsername", "updatedBy")
      .isEqualTo(listOf(LicenceStatus.VARIATION_REJECTED, aCom.username, aCom))

    assertThat(eventCaptor.value)
      .extracting("licenceId", "eventType", "username", "eventDescription")
      .isEqualTo(listOf(1L, LicenceEventType.VARIATION_REFERRED, aCom.username, "reason"))

    assertThat(auditCaptor.value)
      .extracting("licenceId", "username", "fullName", "summary")
      .isEqualTo(
        listOf(
          1L,
          aCom.username,
          "X Y",
          "Licence variation rejected for ${variation.forename} ${variation.surname}",
        ),
      )

    verify(notifyService, times(1)).sendVariationReferredEmail(
      variation.createdBy?.email ?: "",
      "${variation.createdBy?.firstName} ${variation.createdBy?.lastName}",
      variation.responsibleCom?.email ?: "",
      variation.responsibleCom?.fullName ?: "",
      "${variation.forename} ${variation.surname}",
      "1",
    )
  }

  @Test
  fun `referring an HDC licence variation`() {
    val referVariationRequest = ReferVariationRequest(reasonForReferral = "reason")
    val variation = createHdcVariationLicence()
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(variation))
    whenever(staffRepository.findByUsernameIgnoreCase(aCom.username)).thenReturn(aCom)

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    val eventCaptor = ArgumentCaptor.forClass(EntityLicenceEvent::class.java)
    val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)

    service.referLicenceVariation(1L, referVariationRequest)

    verify(licenceRepository, times(1)).findById(1L)
    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(licenceEventRepository, times(1)).saveAndFlush(eventCaptor.capture())
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())
    verify(staffRepository, times(1)).findByUsernameIgnoreCase(aCom.username)

    assertThat(licenceCaptor.value)
      .extracting("statusCode", "updatedByUsername", "updatedBy")
      .isEqualTo(listOf(LicenceStatus.VARIATION_REJECTED, aCom.username, aCom))

    assertThat(eventCaptor.value)
      .extracting("licenceId", "eventType", "username", "eventDescription")
      .isEqualTo(listOf(1L, LicenceEventType.VARIATION_REFERRED, aCom.username, "reason"))

    assertThat(auditCaptor.value)
      .extracting("licenceId", "username", "fullName", "summary")
      .isEqualTo(
        listOf(
          1L,
          aCom.username,
          "X Y",
          "Licence variation rejected for ${variation.forename} ${variation.surname}",
        ),
      )

    verify(notifyService, times(1)).sendVariationReferredEmail(
      variation.createdBy?.email ?: "",
      "${variation.createdBy?.firstName} ${variation.createdBy?.lastName}",
      variation.responsibleCom?.email ?: "",
      variation.responsibleCom?.fullName ?: "",
      "${variation.forename} ${variation.surname}",
      "1",
    )
  }

  @Test
  fun `trying to refer non-variation throws an error`() {
    val referVariationRequest = ReferVariationRequest(reasonForReferral = "reason")
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(createCrdLicence()))

    assertThrows<IllegalStateException> { service.referLicenceVariation(1L, referVariationRequest) }

    verify(licenceRepository, times(1)).findById(1L)
    verify(licenceRepository, times(0)).saveAndFlush(any())
    verify(licenceEventRepository, times(0)).saveAndFlush(any())
    verify(auditEventRepository, times(0)).saveAndFlush(any())
    verify(notifyService, times(0)).sendVariationReferredEmail(
      any(),
      any(),
      any(),
      any(),
      any(),
      any(),
    )
  }

  @Test
  fun `referring a licence variation with no responsible communityOffenderManager allocated`() {
    val referVariationRequest = ReferVariationRequest(reasonForReferral = "reason")
    val variation = createVariationLicence().copy(responsibleCom = null)
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(variation))
    whenever(staffRepository.findByUsernameIgnoreCase(aCom.username)).thenReturn(aCom)

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    val eventCaptor = ArgumentCaptor.forClass(EntityLicenceEvent::class.java)
    val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)

    service.referLicenceVariation(1L, referVariationRequest)

    verify(licenceRepository, times(1)).findById(1L)
    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(licenceEventRepository, times(1)).saveAndFlush(eventCaptor.capture())
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())
    verify(staffRepository, times(1)).findByUsernameIgnoreCase(aCom.username)

    assertThat(licenceCaptor.value)
      .extracting("responsibleCom", "statusCode", "updatedByUsername", "updatedBy")
      .isEqualTo(listOf(null, LicenceStatus.VARIATION_REJECTED, aCom.username, aCom))

    assertThat(eventCaptor.value)
      .extracting("licenceId", "eventType", "username", "eventDescription")
      .isEqualTo(listOf(1L, LicenceEventType.VARIATION_REFERRED, aCom.username, "reason"))

    assertThat(auditCaptor.value)
      .extracting("licenceId", "username", "fullName", "summary")
      .isEqualTo(
        listOf(
          1L,
          aCom.username,
          "X Y",
          "Licence variation rejected for ${variation.forename} ${variation.surname}",
        ),
      )

    verify(notifyService, times(1)).sendVariationReferredEmail(
      variation.createdBy?.email ?: "",
      "${variation.createdBy?.firstName} ${variation.createdBy?.lastName}",
      "",
      "${variation.createdBy?.firstName} ${variation.createdBy?.lastName}",
      "${variation.forename} ${variation.surname}",
      "1",
    )
  }

  @Test
  fun `approving a licence variation`() {
    val variation = createVariationLicence().copy(id = 2, variationOfId = 1L)
    whenever(licenceRepository.findById(2L)).thenReturn(Optional.of(variation))
    whenever(staffRepository.findByUsernameIgnoreCase(aCom.username)).thenReturn(aCom)

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    val eventCaptor = ArgumentCaptor.forClass(EntityLicenceEvent::class.java)
    val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)

    service.approveLicenceVariation(2L)

    verify(licenceRepository, times(1)).findById(2L)

    // Capture calls to licence, history and audit saveAndFlush - as a list
    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(licenceEventRepository, times(1)).saveAndFlush(eventCaptor.capture())
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())
    verify(staffRepository, times(1)).findByUsernameIgnoreCase(aCom.username)

    // Check all calls were made
    assertThat(licenceCaptor.allValues.size).isEqualTo(1)
    assertThat(eventCaptor.allValues.size).isEqualTo(1)
    assertThat(auditCaptor.allValues.size).isEqualTo(1)

    assertThat(licenceCaptor.allValues[0])
      .extracting("id", "statusCode", "updatedByUsername", "approvedByUsername", "approvedByName", "updatedBy")
      .isEqualTo(listOf(2L, LicenceStatus.VARIATION_APPROVED, aCom.username, aCom.username, "X Y", aCom))

    assertThat(eventCaptor.allValues[0])
      .extracting("licenceId", "eventType", "username")
      .isEqualTo(listOf(2L, LicenceEventType.VARIATION_APPROVED, aCom.username))

    assertThat(auditCaptor.allValues[0])
      .extracting("licenceId", "username", "fullName", "summary")
      .isEqualTo(
        listOf(
          2L,
          aCom.username,
          "X Y",
          "Licence variation approved for ${variation.forename} ${variation.surname}",
        ),
      )

    verify(notifyService, times(1)).sendVariationApprovedEmail(
      variation.createdBy?.email ?: "",
      "${variation.createdBy?.firstName} ${variation.createdBy?.lastName}",
      variation.responsibleCom?.email ?: "",
      variation.responsibleCom?.fullName ?: "",
      "${variation.forename} ${variation.surname}",
      "2",
    )
  }

  @Test
  fun `approving an HDC licence variation`() {
    val variation = createHdcVariationLicence().copy(id = 2, variationOfId = 1L)
    whenever(licenceRepository.findById(2L)).thenReturn(Optional.of(variation))
    whenever(staffRepository.findByUsernameIgnoreCase(aCom.username)).thenReturn(aCom)

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    val eventCaptor = ArgumentCaptor.forClass(EntityLicenceEvent::class.java)
    val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)

    service.approveLicenceVariation(2L)

    verify(licenceRepository, times(1)).findById(2L)

    // Capture calls to licence, history and audit saveAndFlush - as a list
    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(licenceEventRepository, times(1)).saveAndFlush(eventCaptor.capture())
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())
    verify(staffRepository, times(1)).findByUsernameIgnoreCase(aCom.username)

    // Check all calls were made
    assertThat(licenceCaptor.allValues.size).isEqualTo(1)
    assertThat(eventCaptor.allValues.size).isEqualTo(1)
    assertThat(auditCaptor.allValues.size).isEqualTo(1)

    assertThat(licenceCaptor.allValues[0])
      .extracting("id", "statusCode", "updatedByUsername", "approvedByUsername", "approvedByName", "updatedBy")
      .isEqualTo(listOf(2L, LicenceStatus.VARIATION_APPROVED, aCom.username, aCom.username, "X Y", aCom))

    assertThat(eventCaptor.allValues[0])
      .extracting("licenceId", "eventType", "username")
      .isEqualTo(listOf(2L, LicenceEventType.VARIATION_APPROVED, aCom.username))

    assertThat(auditCaptor.allValues[0])
      .extracting("licenceId", "username", "fullName", "summary")
      .isEqualTo(
        listOf(
          2L,
          aCom.username,
          "X Y",
          "Licence variation approved for ${variation.forename} ${variation.surname}",
        ),
      )

    verify(notifyService, times(1)).sendVariationApprovedEmail(
      variation.createdBy?.email ?: "",
      "${variation.createdBy?.firstName} ${variation.createdBy?.lastName}",
      variation.getCom().email ?: "",
      variation.getCom().fullName,
      "${variation.forename} ${variation.surname}",
      "2",
    )
  }

  @Test
  fun `approving a licence variation with no responsible communityOffenderManager allocated`() {
    val variation = createVariationLicence().copy(id = 2, variationOfId = 1L, responsibleCom = null)
    whenever(licenceRepository.findById(2L)).thenReturn(Optional.of(variation))
    whenever(staffRepository.findByUsernameIgnoreCase(aCom.username)).thenReturn(aCom)

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    val eventCaptor = ArgumentCaptor.forClass(EntityLicenceEvent::class.java)
    val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)

    service.approveLicenceVariation(2L)

    verify(licenceRepository, times(1)).findById(2L)

    // Capture calls to licence, history and audit saveAndFlush - as a list
    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(licenceEventRepository, times(1)).saveAndFlush(eventCaptor.capture())
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())
    verify(staffRepository, times(1)).findByUsernameIgnoreCase(aCom.username)

    // Check all calls were made
    assertThat(licenceCaptor.allValues.size).isEqualTo(1)
    assertThat(eventCaptor.allValues.size).isEqualTo(1)
    assertThat(auditCaptor.allValues.size).isEqualTo(1)

    assertThat(licenceCaptor.allValues[0])
      .extracting(
        "id",
        "responsibleCom",
        "statusCode",
        "updatedByUsername",
        "approvedByUsername",
        "approvedByName",
        "updatedBy",
      )
      .isEqualTo(listOf(2L, null, LicenceStatus.VARIATION_APPROVED, aCom.username, aCom.username, "X Y", aCom))

    assertThat(eventCaptor.allValues[0])
      .extracting("licenceId", "eventType", "username")
      .isEqualTo(listOf(2L, LicenceEventType.VARIATION_APPROVED, aCom.username))

    assertThat(auditCaptor.allValues[0])
      .extracting("licenceId", "username", "fullName", "summary")
      .isEqualTo(
        listOf(
          2L,
          aCom.username,
          "X Y",
          "Licence variation approved for ${variation.forename} ${variation.surname}",
        ),
      )

    verify(notifyService, times(1)).sendVariationApprovedEmail(
      variation.createdBy?.email ?: "",
      "${variation.createdBy?.firstName} ${variation.createdBy?.lastName}",
      "",
      "${variation.createdBy?.firstName} ${variation.createdBy?.lastName}",
      "${variation.forename} ${variation.surname}",
      "2",
    )
  }

  @Test
  fun `trying to approve a non-variation via approveLicenceVariation throws an error`() {
    val variation = createCrdLicence().copy(id = 2)
    whenever(licenceRepository.findById(2L)).thenReturn(Optional.of(variation))

    assertThrows<IllegalStateException> { service.approveLicenceVariation(2L) }

    verify(licenceRepository, times(1)).findById(2L)
    verify(licenceRepository, times(0)).saveAndFlush(any())
    verify(licenceEventRepository, times(0)).saveAndFlush(any())
    verify(auditEventRepository, times(0)).saveAndFlush(any())
    verify(notifyService, times(0)).sendVariationApprovedEmail(
      any(),
      any(),
      any(),
      any(),
      any(),
      any(),
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
    whenever(staffRepository.findByUsernameIgnoreCase(aCom.username)).thenReturn(aCom)

    service.updateLicenceStatus(
      approvedLicence.id,
      StatusUpdateRequest(status = LicenceStatus.ACTIVE, username = aCom.username, fullName = "Y"),
    )

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())
    verify(domainEventsService, times(1)).recordDomainEvent(approvedLicence, LicenceStatus.ACTIVE)
    verify(staffRepository, times(1)).findByUsernameIgnoreCase(aCom.username)

    assertThat(licenceCaptor.value.licenceActivatedDate).isNotNull()

    assertThat(licenceCaptor.value)
      .extracting("id", "statusCode", "updatedByUsername", "licenceActivatedDate", "updatedBy")
      .isEqualTo(
        listOf(
          approvedLicence.id,
          LicenceStatus.ACTIVE,
          aCom.username,
          licenceCaptor.value.licenceActivatedDate,
          aCom,
        ),
      )

    assertThat(auditCaptor.firstValue).extracting("licenceId", "username", "fullName", "summary", "eventType")
      .isEqualTo(
        listOf(
          approvedLicence.id,
          aCom.username,
          "${aCom.firstName} ${aCom.lastName}",
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
    whenever(staffRepository.findByUsernameIgnoreCase(aCom.username)).thenReturn(aCom)

    service.updateLicenceStatus(
      approvedLicence.id,
      StatusUpdateRequest(status = LicenceStatus.INACTIVE, username = aCom.username, fullName = "Y"),
    )

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())
    verify(domainEventsService, times(1)).recordDomainEvent(approvedLicence, LicenceStatus.INACTIVE)
    verify(staffRepository, times(1)).findByUsernameIgnoreCase(aCom.username)

    assertThat(licenceCaptor.value.licenceActivatedDate).isNull()

    assertThat(licenceCaptor.value)
      .extracting("id", "statusCode", "updatedByUsername", "supersededDate", "updatedBy")
      .isEqualTo(
        listOf(
          approvedLicence.id,
          LicenceStatus.INACTIVE,
          aCom.username,
          licenceCaptor.value.supersededDate,
          aCom,
        ),
      )

    assertThat(auditCaptor.firstValue).extracting("licenceId", "username", "fullName", "summary", "eventType")
      .isEqualTo(
        listOf(
          approvedLicence.id,
          aCom.username,
          "${aCom.firstName} ${aCom.lastName}",
          "Licence set to INACTIVE for ${approvedLicence.forename} ${approvedLicence.surname}",
          USER_EVENT,
        ),
      )
  }

  @Test
  fun `update licence status to ACTIVE for variation approved`() {
    val variationApprovedLicence =
      createVariationLicence().copy(id = 2L, statusCode = LicenceStatus.VARIATION_APPROVED, licenceVersion = "2.0")

    whenever(licenceRepository.findById(variationApprovedLicence.id)).thenReturn(Optional.of(variationApprovedLicence))
    whenever(
      licenceRepository.findAllByVersionOfIdInAndStatusCodeIn(
        listOf(variationApprovedLicence.id),
        listOf(LicenceStatus.IN_PROGRESS, LicenceStatus.SUBMITTED),
      ),
    ).thenReturn(emptyList())
    whenever(staffRepository.findByUsernameIgnoreCase(aCom.username)).thenReturn(aCom)

    service.updateLicenceStatus(
      variationApprovedLicence.id,
      StatusUpdateRequest(status = LicenceStatus.ACTIVE, username = aCom.username, fullName = "Y"),
    )

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())
    verify(domainEventsService, times(1)).recordDomainEvent(variationApprovedLicence, LicenceStatus.ACTIVE)
    verify(staffRepository, times(1)).findByUsernameIgnoreCase(aCom.username)

    assertThat(licenceCaptor.value.licenceActivatedDate).isNotNull()

    assertThat(licenceCaptor.value)
      .extracting("id", "statusCode", "updatedByUsername", "licenceActivatedDate", "updatedBy")
      .isEqualTo(
        listOf(
          variationApprovedLicence.id,
          LicenceStatus.ACTIVE,
          aCom.username,
          licenceCaptor.value.licenceActivatedDate,
          aCom,
        ),
      )

    assertThat(auditCaptor.firstValue).extracting("licenceId", "username", "fullName", "summary", "eventType")
      .isEqualTo(
        listOf(
          variationApprovedLicence.id,
          aCom.username,
          "${aCom.firstName} ${aCom.lastName}",
          "Licence set to ACTIVE for ${variationApprovedLicence.forename} ${variationApprovedLicence.surname}",
          USER_EVENT,
        ),
      )
  }

  @Test
  fun `update licence status to INACTIVE for an existing ACTIVE variation licence`() {
    val variationLicence = createVariationLicence()
      .copy(id = 1L, statusCode = LicenceStatus.ACTIVE, licenceVersion = "2.0")

    whenever(licenceRepository.findById(variationLicence.id)).thenReturn(Optional.of(variationLicence))
    whenever(
      licenceRepository.findAllByVersionOfIdInAndStatusCodeIn(
        listOf(variationLicence.id),
        listOf(LicenceStatus.IN_PROGRESS, LicenceStatus.SUBMITTED),
      ),
    ).thenReturn(emptyList())
    whenever(staffRepository.findByUsernameIgnoreCase(aCom.username)).thenReturn(aCom)

    service.updateLicenceStatus(
      variationLicence.id,
      StatusUpdateRequest(status = LicenceStatus.INACTIVE, username = aCom.username, fullName = "Y"),
    )

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())
    verify(domainEventsService, times(1)).recordDomainEvent(variationLicence, LicenceStatus.INACTIVE)
    verify(staffRepository, times(1)).findByUsernameIgnoreCase(aCom.username)

    assertThat(licenceCaptor.value.licenceActivatedDate).isNull()

    assertThat(licenceCaptor.value)
      .extracting("id", "statusCode", "updatedByUsername", "supersededDate", "updatedBy")
      .isEqualTo(
        listOf(
          variationLicence.id,
          LicenceStatus.INACTIVE,
          aCom.username,
          licenceCaptor.value.supersededDate,
          aCom,
        ),
      )

    assertThat(auditCaptor.firstValue).extracting("licenceId", "username", "fullName", "summary", "eventType")
      .isEqualTo(
        listOf(
          variationLicence.id,
          aCom.username,
          "${aCom.firstName} ${aCom.lastName}",
          "Licence set to INACTIVE for ${variationLicence.forename} ${variationLicence.surname}",
          USER_EVENT,
        ),
      )
  }

  @Test
  fun `deactivateLicenceAndVariations returns when no active licences are found`() {
    whenever(licenceRepository.findLicenceAndVariations(aLicenceEntity.id)).thenReturn(emptyList())

    service.deactivateLicenceAndVariations(
      aLicenceEntity.id,
      DeactivateLicenceAndVariationsRequest(DateChangeLicenceDeativationReason.RESENTENCED),
    )
    verify(
      licenceRepository,
      times(0),
    ).saveAndFlush(any())
    verify(auditEventRepository, times(0)).saveAndFlush(any())
    verify(licenceEventRepository, times(0)).saveAndFlush(any())
  }

  @Test
  fun `deactivateLicenceAndVariations deactivates licences and sets appropriate message for resentensed cases`() {
    val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)
    val eventCaptor = ArgumentCaptor.forClass(EntityLicenceEvent::class.java)
    val activeLicence = aLicenceEntity.copy(statusCode = LicenceStatus.ACTIVE)
    val variationLicence = aLicenceEntity.copy(
      id = 2L,
      statusCode = LicenceStatus.VARIATION_IN_PROGRESS,
      licenceVersion = "2.0",
      versionOfId = 1L,
    )
    whenever(licenceRepository.findLicenceAndVariations(activeLicence.id)).thenReturn(
      listOf(
        activeLicence,
        variationLicence,
      ),
    )

    service.deactivateLicenceAndVariations(
      activeLicence.id,
      DeactivateLicenceAndVariationsRequest(DateChangeLicenceDeativationReason.RESENTENCED),
    )

    verify(
      licenceRepository,
      times(1),
    ).saveAllAndFlush(
      listOf(
        activeLicence.copy(statusCode = LicenceStatus.INACTIVE),
        variationLicence.copy(statusCode = LicenceStatus.INACTIVE),
      ),
    )
    verify(auditEventRepository, times(2)).saveAndFlush(auditCaptor.capture())
    verify(licenceEventRepository, times(2)).saveAndFlush(eventCaptor.capture())
    verify(domainEventsService, times(1)).recordDomainEvent(activeLicence, LicenceStatus.INACTIVE)
    verify(domainEventsService, times(1)).recordDomainEvent(variationLicence, LicenceStatus.INACTIVE)

    assertThat(auditCaptor.value)
      .extracting("licenceId", "username", "fullName", "summary")
      .isEqualTo(
        listOf(
          2L,
          "SYSTEM",
          "SYSTEM",
          "Licence inactivated due to being resentenced for ${aLicenceEntity.forename} ${aLicenceEntity.surname}",
        ),
      )

    assertThat(eventCaptor.value)
      .extracting("licenceId", "eventType", "forenames", "surname")
      .isEqualTo(listOf(2L, LicenceEventType.SUPERSEDED, "SYSTEM", "SYSTEM"))
  }

  @Test
  fun `deactivateLicenceAndVariations deactivates licences and sets appropriate message for recalled cases`() {
    val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)
    val eventCaptor = ArgumentCaptor.forClass(EntityLicenceEvent::class.java)
    val activeLicence = aLicenceEntity.copy(statusCode = LicenceStatus.ACTIVE)
    val variationLicence = aLicenceEntity.copy(
      id = 2L,
      statusCode = LicenceStatus.VARIATION_IN_PROGRESS,
      licenceVersion = "2.0",
      versionOfId = 1L,
    )
    whenever(licenceRepository.findLicenceAndVariations(activeLicence.id)).thenReturn(
      listOf(
        activeLicence,
        variationLicence,
      ),
    )

    service.deactivateLicenceAndVariations(
      activeLicence.id,
      DeactivateLicenceAndVariationsRequest(DateChangeLicenceDeativationReason.RECALLED),
    )

    verify(
      licenceRepository,
      times(1),
    ).saveAllAndFlush(
      listOf(
        activeLicence.copy(statusCode = LicenceStatus.INACTIVE),
        variationLicence.copy(statusCode = LicenceStatus.INACTIVE),
      ),
    )
    verify(auditEventRepository, times(2)).saveAndFlush(auditCaptor.capture())
    verify(licenceEventRepository, times(2)).saveAndFlush(eventCaptor.capture())
    verify(domainEventsService, times(1)).recordDomainEvent(activeLicence, LicenceStatus.INACTIVE)
    verify(domainEventsService, times(1)).recordDomainEvent(variationLicence, LicenceStatus.INACTIVE)

    assertThat(auditCaptor.value)
      .extracting("licenceId", "username", "fullName", "summary")
      .isEqualTo(
        listOf(
          2L,
          "SYSTEM",
          "SYSTEM",
          "Licence inactivated due to being recalled for ${aLicenceEntity.forename} ${aLicenceEntity.surname}",
        ),
      )

    assertThat(eventCaptor.value)
      .extracting("licenceId", "eventType", "forenames", "surname")
      .isEqualTo(listOf(2L, LicenceEventType.SUPERSEDED, "SYSTEM", "SYSTEM"))
  }

  @Test
  fun `should get access permissions for a licence`() {
    val variationLicence = aLicenceEntity.copy(
      id = 2L,
      statusCode = LicenceStatus.VARIATION_IN_PROGRESS,
      licenceVersion = "2.0",
    )
    whenever(licenceRepository.findById(aLicenceEntity.id)).thenReturn(Optional.of(aLicenceEntity))
    whenever(licenceRepository.findAll(any<Specification<EntityLicence>>(), any<Sort>())).thenReturn(
      listOf(
        aLicenceEntity,
        variationLicence,
      ),
    )
    whenever(deliusApiClient.getOffenderManager(aLicenceEntity.crn!!)).thenReturn(offenderManager())

    val permissions = service.getLicencePermissions(licenceId = aLicenceEntity.id, teamCodes = listOf("invalid-team"))
    assertThat(permissions.view).isFalse
  }

  @Nested
  inner class `Marking reviewed when no variation required` {
    @Test
    fun happyPath() {
      whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))
      whenever(staffRepository.findByUsernameIgnoreCase(aCom.username)).thenReturn(aCom)

      val hardstopLicence = createHardStopLicence()
        .copy(id = 1L, statusCode = LicenceStatus.ACTIVE, licenceVersion = "2.0", reviewDate = null)

      whenever(licenceRepository.findById(hardstopLicence.id)).thenReturn(Optional.of(hardstopLicence))

      service.reviewWithNoVariationRequired(1L)

      argumentCaptor<HardStopLicence>().apply {
        verify(licenceRepository, times(1)).saveAndFlush(capture())
        assertThat(firstValue.reviewDate?.toLocalDate()).isEqualTo(LocalDate.now())
        assertThat(firstValue.dateLastUpdated?.toLocalDate()).isEqualTo(LocalDate.now())
        assertThat(firstValue.updatedByUsername).isEqualTo(aCom.username)
        assertThat(firstValue.updatedBy).isEqualTo(aCom)
      }

      argumentCaptor<EntityAuditEvent>().apply {
        verify(auditEventRepository, times(1)).saveAndFlush(capture())
        assertThat(firstValue.licenceId).isEqualTo(hardstopLicence.id)
        assertThat(firstValue.username).isEqualTo(aCom.username)
        assertThat(firstValue.fullName).isEqualTo("X Y")
        assertThat(firstValue.summary).isEqualTo("Licence reviewed without being varied for John Smith")
        assertThat(firstValue.eventType).isEqualTo(USER_EVENT)
      }

      argumentCaptor<LicenceEvent>().apply {
        verify(licenceEventRepository, times(1)).saveAndFlush(capture())
        assertThat(firstValue.licenceId).isEqualTo(hardstopLicence.id)
        assertThat(firstValue.username).isEqualTo(aCom.username)
        assertThat(firstValue.eventDescription).isEqualTo("Licence reviewed without being varied for John Smith")
        assertThat(firstValue.forenames).isEqualTo("X")
        assertThat(firstValue.surname).isEqualTo("Y")
        assertThat(firstValue.eventType).isEqualTo(LicenceEventType.REVIEWED_WITHOUT_VARIATION)
      }
    }

    @Test
    fun `Review time served licence with no responsible COM allocated`() {
      whenever(staffRepository.findByUsernameIgnoreCase(aCom.username)).thenReturn(aCom)
      val timeServedLicence = aTimeServedLicence
        .copy(
          id = 1L,
          statusCode = LicenceStatus.ACTIVE,
          licenceVersion = "2.0",
          reviewDate = null,
          responsibleCom = null,
        )

      whenever(licenceRepository.findById(timeServedLicence.id)).thenReturn(Optional.of(timeServedLicence))

      service.reviewWithNoVariationRequired(1L)

      argumentCaptor<TimeServedLicence>().apply {
        verify(licenceRepository, times(1)).saveAndFlush(capture())
        assertThat(firstValue.reviewDate?.toLocalDate()).isEqualTo(LocalDate.now())
        assertThat(firstValue.dateLastUpdated?.toLocalDate()).isEqualTo(LocalDate.now())
        assertThat(firstValue.updatedByUsername).isEqualTo(aCom.username)
        assertThat(firstValue.updatedBy).isEqualTo(aCom)
      }

      argumentCaptor<EntityAuditEvent>().apply {
        verify(auditEventRepository, times(1)).saveAndFlush(capture())
        assertThat(firstValue.licenceId).isEqualTo(timeServedLicence.id)
        assertThat(firstValue.username).isEqualTo(aCom.username)
        assertThat(firstValue.fullName).isEqualTo("X Y")
        assertThat(firstValue.summary).isEqualTo("Licence reviewed without being varied for John Smith")
        assertThat(firstValue.eventType).isEqualTo(USER_EVENT)
      }

      argumentCaptor<LicenceEvent>().apply {
        verify(licenceEventRepository, times(1)).saveAndFlush(capture())
        assertThat(firstValue.licenceId).isEqualTo(timeServedLicence.id)
        assertThat(firstValue.username).isEqualTo(aCom.username)
        assertThat(firstValue.eventDescription).isEqualTo("Licence reviewed without being varied for John Smith")
        assertThat(firstValue.forenames).isEqualTo("X")
        assertThat(firstValue.surname).isEqualTo("Y")
        assertThat(firstValue.eventType).isEqualTo(LicenceEventType.REVIEWED_WITHOUT_VARIATION)
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

  @Nested
  inner class `Activating variations` {
    @Test
    fun happyPathWhenNotHardstop() {
      whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(createCrdLicence().copy(id = 1L)))
      whenever(licenceRepository.findById(2L)).thenReturn(
        Optional.of(
          createVariationLicence().copy(
            id = 2L,
            variationOfId = 1L,
            statusCode = LicenceStatus.VARIATION_APPROVED,
          ),
        ),
      )
      whenever(staffRepository.findByUsernameIgnoreCase(aCom.username)).thenReturn(aCom)

      service.activateVariation(2L)

      argumentCaptor<Licence>().apply {
        verify(licenceRepository, times(2)).saveAndFlush(capture())
        assertThat(firstValue).isInstanceOf(VariationLicence::class.java)
        assertThat(firstValue.statusCode).isEqualTo(LicenceStatus.ACTIVE)

        assertThat(secondValue).isInstanceOf(CrdLicence::class.java)
        assertThat(secondValue.statusCode).isEqualTo(LicenceStatus.INACTIVE)
      }

      argumentCaptor<EntityAuditEvent>().apply {
        verify(auditEventRepository, times(2)).saveAndFlush(capture())
        assertThat(firstValue.licenceId).isEqualTo(2L)
        assertThat(firstValue.summary).isEqualTo("Licence set to ACTIVE for John Smith")
        assertThat(firstValue.eventType).isEqualTo(USER_EVENT)

        assertThat(secondValue.licenceId).isEqualTo(1L)
        assertThat(secondValue.summary).isEqualTo("Licence set to INACTIVE for John Smith")
        assertThat(secondValue.eventType).isEqualTo(USER_EVENT)
      }

      argumentCaptor<LicenceEvent>().apply {
        verify(licenceEventRepository, times(2)).saveAndFlush(capture())

        assertThat(firstValue.eventDescription).isEqualTo("Licence updated to ACTIVE for John Smith")
        assertThat(firstValue.eventType).isEqualTo(LicenceEventType.ACTIVATED)

        assertThat(secondValue.eventDescription).isEqualTo("Licence updated to INACTIVE for John Smith")
        assertThat(secondValue.eventType).isEqualTo(LicenceEventType.SUPERSEDED)
      }
    }

    @Test
    fun happyPathWhenVaryingHardStopLicence() {
      whenever(licenceRepository.findById(1L)).thenReturn(
        Optional.of(
          createHardStopLicence().copy(
            id = 1L,
            reviewDate = null,
          ),
        ),
      )
      whenever(licenceRepository.findById(2L)).thenReturn(
        Optional.of(
          createVariationLicence().copy(
            id = 2L,
            variationOfId = 1L,
            statusCode = LicenceStatus.VARIATION_APPROVED,
          ),
        ),
      )
      whenever(staffRepository.findByUsernameIgnoreCase(aCom.username)).thenReturn(aCom)

      service.activateVariation(2L)

      argumentCaptor<Licence>().apply {
        verify(licenceRepository, times(2)).saveAndFlush(capture())
        assertThat(firstValue).isInstanceOf(VariationLicence::class.java)
        assertThat(firstValue.statusCode).isEqualTo(LicenceStatus.ACTIVE)

        assertThat(secondValue).isInstanceOf(HardStopLicence::class.java)
        assertThat((secondValue as HardStopLicence).reviewDate?.toLocalDate()).isEqualTo(LocalDate.now())

        assertThat(secondValue.statusCode).isEqualTo(LicenceStatus.INACTIVE)
      }

      argumentCaptor<EntityAuditEvent>().apply {
        verify(auditEventRepository, times(2)).saveAndFlush(capture())
        assertThat(firstValue.licenceId).isEqualTo(2L)
        assertThat(firstValue.summary).isEqualTo("Licence set to ACTIVE for John Smith")
        assertThat(firstValue.eventType).isEqualTo(USER_EVENT)

        assertThat(secondValue.licenceId).isEqualTo(1L)
        assertThat(secondValue.summary).isEqualTo("Licence set to INACTIVE for John Smith")
        assertThat(secondValue.eventType).isEqualTo(USER_EVENT)
      }

      argumentCaptor<LicenceEvent>().apply {
        verify(licenceEventRepository, times(3)).saveAndFlush(capture())

        assertThat(firstValue.eventDescription).isEqualTo("Licence updated to ACTIVE for John Smith")
        assertThat(firstValue.eventType).isEqualTo(LicenceEventType.ACTIVATED)

        assertThat(secondValue.eventDescription).isEqualTo("Licence reviewed with variation for John Smith")
        assertThat(secondValue.eventType).isEqualTo(LicenceEventType.REVIEWED_WITH_VARIATION)

        assertThat(thirdValue.eventDescription).isEqualTo("Licence updated to INACTIVE for John Smith")
        assertThat(thirdValue.eventType).isEqualTo(LicenceEventType.SUPERSEDED)
      }
    }

    @Test
    fun happyPathWhenActivatingHdcVariation() {
      whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(anHdcLicenceEntity.copy(id = 1L)))
      whenever(licenceRepository.findById(2L)).thenReturn(
        Optional.of(
          anHdcVariationLicence.copy(
            id = 2L,
            variationOfId = 1L,
            statusCode = LicenceStatus.VARIATION_APPROVED,
          ),
        ),
      )
      whenever(staffRepository.findByUsernameIgnoreCase(aCom.username)).thenReturn(aCom)

      service.activateVariation(2L)

      argumentCaptor<Licence>().apply {
        verify(licenceRepository, times(2)).saveAndFlush(capture())
        assertThat(firstValue).isInstanceOf(HdcVariationLicence::class.java)
        assertThat(firstValue.statusCode).isEqualTo(LicenceStatus.ACTIVE)

        assertThat(secondValue).isInstanceOf(HdcLicence::class.java)
        assertThat(secondValue.statusCode).isEqualTo(LicenceStatus.INACTIVE)
      }

      argumentCaptor<EntityAuditEvent>().apply {
        verify(auditEventRepository, times(2)).saveAndFlush(capture())
        assertThat(firstValue.licenceId).isEqualTo(2L)
        assertThat(firstValue.summary).isEqualTo("Licence set to ACTIVE for John Smith")
        assertThat(firstValue.eventType).isEqualTo(USER_EVENT)

        assertThat(secondValue.licenceId).isEqualTo(1L)
        assertThat(secondValue.summary).isEqualTo("Licence set to INACTIVE for John Smith")
        assertThat(secondValue.eventType).isEqualTo(USER_EVENT)
      }

      argumentCaptor<LicenceEvent>().apply {
        verify(licenceEventRepository, times(2)).saveAndFlush(capture())

        assertThat(firstValue.eventDescription).isEqualTo("Licence updated to ACTIVE for John Smith")
        assertThat(firstValue.eventType).isEqualTo(LicenceEventType.ACTIVATED)

        assertThat(secondValue.eventDescription).isEqualTo("Licence updated to INACTIVE for John Smith")
        assertThat(secondValue.eventType).isEqualTo(LicenceEventType.SUPERSEDED)
      }
    }

    @Test
    fun attemptingToVaryNonVariation() {
      whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(createCrdLicence().copy(id = 1L)))

      service.activateVariation(1L)

      verify(licenceRepository, never()).saveAndFlush(any())
      verify(auditEventRepository, never()).saveAndFlush(any())
      verify(licenceEventRepository, never()).saveAndFlush(any())
    }

    @Test
    fun attemptingToVaryNonApprovedVariation() {
      whenever(licenceRepository.findById(1L)).thenReturn(
        Optional.of(
          createVariationLicence().copy(
            id = 2L,
            variationOfId = 1L,
            statusCode = LicenceStatus.VARIATION_IN_PROGRESS,
          ),
        ),
      )

      service.activateVariation(1L)

      verify(licenceRepository, never()).saveAndFlush(any())
      verify(auditEventRepository, never()).saveAndFlush(any())
      verify(licenceEventRepository, never()).saveAndFlush(any())
    }

    @Test
    fun `Activate time served licence variation with no responsible COM allocated`() {
      whenever(licenceRepository.findById(1L)).thenReturn(
        Optional.of(
          aTimeServedLicence.copy(
            id = 1L,
            reviewDate = null,
          ),
        ),
      )
      whenever(licenceRepository.findById(2L)).thenReturn(
        Optional.of(
          createVariationLicence().copy(
            id = 2L,
            variationOfId = 1L,
            statusCode = LicenceStatus.VARIATION_APPROVED,
            responsibleCom = null,
          ),
        ),
      )
      whenever(staffRepository.findByUsernameIgnoreCase(aCom.username)).thenReturn(aCom)

      service.activateVariation(2L)

      argumentCaptor<Licence>().apply {
        verify(licenceRepository, times(2)).saveAndFlush(capture())
        assertThat(firstValue).isInstanceOf(VariationLicence::class.java)
        assertThat(firstValue.statusCode).isEqualTo(LicenceStatus.ACTIVE)
        assertThat(firstValue.responsibleCom).isNull()

        assertThat(secondValue).isInstanceOf(TimeServedLicence::class.java)
        assertThat((secondValue as TimeServedLicence).reviewDate?.toLocalDate()).isEqualTo(LocalDate.now())

        assertThat(secondValue.statusCode).isEqualTo(LicenceStatus.INACTIVE)
      }

      argumentCaptor<EntityAuditEvent>().apply {
        verify(auditEventRepository, times(2)).saveAndFlush(capture())
        assertThat(firstValue.licenceId).isEqualTo(2L)
        assertThat(firstValue.summary).isEqualTo("Licence set to ACTIVE for John Smith")
        assertThat(firstValue.eventType).isEqualTo(USER_EVENT)

        assertThat(secondValue.licenceId).isEqualTo(1L)
        assertThat(secondValue.summary).isEqualTo("Licence set to INACTIVE for John Smith")
        assertThat(secondValue.eventType).isEqualTo(USER_EVENT)
      }

      argumentCaptor<LicenceEvent>().apply {
        verify(licenceEventRepository, times(3)).saveAndFlush(capture())

        assertThat(firstValue.eventDescription).isEqualTo("Licence updated to ACTIVE for John Smith")
        assertThat(firstValue.eventType).isEqualTo(LicenceEventType.ACTIVATED)

        assertThat(secondValue.eventDescription).isEqualTo("Licence reviewed with variation for John Smith")
        assertThat(secondValue.eventType).isEqualTo(LicenceEventType.REVIEWED_WITH_VARIATION)

        assertThat(thirdValue.eventDescription).isEqualTo("Licence updated to INACTIVE for John Smith")
        assertThat(thirdValue.eventType).isEqualTo(LicenceEventType.SUPERSEDED)
      }
    }
  }

  @Nested
  inner class `update username and updatedBy when updating a licence` {

    @Test
    fun `updating user is retained and username is set to SYSTEM_USER when a staff member cannot be found`() {
      whenever(licenceRepository.findById(1L)).thenReturn(
        Optional.of(
          aLicenceEntity.copy(
            updatedBy = aPreviousUser,
          ),
        ),
      )
      whenever(staffRepository.findByUsernameIgnoreCase(aCom.username)).thenReturn(null)

      service.updateLicenceStatus(
        1L,
        StatusUpdateRequest(status = LicenceStatus.REJECTED, username = aCom.username, fullName = "Y"),
      )

      val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
      val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)

      verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
      verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())
      verify(licenceEventRepository, times(0)).saveAndFlush(any())

      assertThat(licenceCaptor.value)
        .extracting("id", "statusCode", "updatedByUsername", "licenceActivatedDate", "updatedBy")
        .isEqualTo(listOf(1L, LicenceStatus.REJECTED, SYSTEM_USER, null, aPreviousUser))

      assertThat(auditCaptor.value)
        .extracting("licenceId", "username", "fullName", "summary", "eventType")
        .isEqualTo(
          listOf(
            1L,
            SYSTEM_USER,
            SYSTEM_USER,
            "Licence rejected for ${aLicenceEntity.forename} ${aLicenceEntity.surname}",
            SYSTEM_EVENT,
          ),
        )
    }
  }

  @Nested
  inner class `approving hard stop licences` {
    @Test
    fun `approving a hard stop licence sends a hard stop licence approval email`() {
      val service =
        LicenceService(
          licenceRepository,
          crdLicenceRepository,
          staffRepository,
          licenceEventRepository,
          licencePolicyService,
          auditEventRepository,
          notifyService,
          omuService,
          releaseDateService,
          domainEventsService,
          prisonerSearchApiClient,
          eligibilityService,
          uploadFileConditionsService,
          deliusApiClient,
          telemetryService,
          auditService,
          isTimeServedLogicEnabled = false,
        )
      val submittedLicence =
        createHardStopLicence().copy(id = 2L, statusCode = LicenceStatus.SUBMITTED)

      whenever(licenceRepository.findById(submittedLicence.id)).thenReturn(Optional.of(submittedLicence))
      whenever(staffRepository.findByUsernameIgnoreCase(aCom.username)).thenReturn(aCom)

      service.updateLicenceStatus(
        submittedLicence.id,
        StatusUpdateRequest(status = LicenceStatus.APPROVED, username = aCom.username, fullName = "Y"),
      )

      val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
      val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)

      verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
      verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())
      verify(domainEventsService, times(1)).recordDomainEvent(submittedLicence, LicenceStatus.APPROVED)
      verify(staffRepository, times(1)).findByUsernameIgnoreCase(aCom.username)
      verify(notifyService, times(1)).sendHardStopLicenceApprovedEmail(
        aCom.email,
        submittedLicence.forename!!,
        submittedLicence.surname!!,
        submittedLicence.crn,
        submittedLicence.licenceStartDate,
        submittedLicence.id.toString(),
      )

      assertThat(licenceCaptor.value)
        .extracting("id", "statusCode", "approvedByUsername", "updatedByUsername", "updatedBy")
        .isEqualTo(
          listOf(
            submittedLicence.id,
            LicenceStatus.APPROVED,
            aCom.username,
            aCom.username,
            aCom,
          ),
        )

      assertThat(auditCaptor.firstValue).extracting("licenceId", "username", "fullName", "summary", "eventType")
        .isEqualTo(
          listOf(
            submittedLicence.id,
            aCom.username,
            "${aCom.firstName} ${aCom.lastName}",
            "Licence approved for ${submittedLicence.forename} ${submittedLicence.surname}",
            USER_EVENT,
          ),
        )
    }

    @Test
    fun `approving a hard stop licence sends a hard stop reviewable licence approval email`() {
      val submittedLicence =
        createHardStopLicence().copy(id = 2L, statusCode = LicenceStatus.SUBMITTED)

      whenever(licenceRepository.findById(submittedLicence.id)).thenReturn(Optional.of(submittedLicence))
      whenever(staffRepository.findByUsernameIgnoreCase(aCom.username)).thenReturn(aCom)

      service.updateLicenceStatus(
        submittedLicence.id,
        StatusUpdateRequest(status = LicenceStatus.APPROVED, username = aCom.username, fullName = "Y"),
      )

      val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
      val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)

      verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
      verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())
      verify(domainEventsService, times(1)).recordDomainEvent(submittedLicence, LicenceStatus.APPROVED)
      verify(staffRepository, times(1)).findByUsernameIgnoreCase(aCom.username)
      verify(notifyService, times(1)).sendReviewableLicenceApprovedEmail(
        aCom.email,
        submittedLicence.forename!!,
        submittedLicence.surname!!,
        submittedLicence.crn,
        submittedLicence.licenceStartDate,
        submittedLicence.id.toString(),
        submittedLicence.prisonDescription!!,
        isTimeServedLicence = false,
      )

      assertThat(licenceCaptor.value)
        .extracting("id", "statusCode", "approvedByUsername", "updatedByUsername", "updatedBy")
        .isEqualTo(
          listOf(
            submittedLicence.id,
            LicenceStatus.APPROVED,
            aCom.username,
            aCom.username,
            aCom,
          ),
        )

      assertThat(auditCaptor.firstValue).extracting("licenceId", "username", "fullName", "summary", "eventType")
        .isEqualTo(
          listOf(
            submittedLicence.id,
            aCom.username,
            "${aCom.firstName} ${aCom.lastName}",
            "Licence approved for ${submittedLicence.forename} ${submittedLicence.surname}",
            USER_EVENT,
          ),
        )
    }

    @Test
    fun `approving a time served licence sends a hard stop licence approval email`() {
      val submittedLicence =
        aTimeServedLicence.copy(id = 2L, statusCode = LicenceStatus.SUBMITTED)

      whenever(licenceRepository.findById(submittedLicence.id)).thenReturn(Optional.of(submittedLicence))
      whenever(staffRepository.findByUsernameIgnoreCase(aCom.username)).thenReturn(aCom)

      service.updateLicenceStatus(
        submittedLicence.id,
        StatusUpdateRequest(status = LicenceStatus.APPROVED, username = aCom.username, fullName = "Y"),
      )

      val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
      val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)

      verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
      verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())
      verify(domainEventsService, times(1)).recordDomainEvent(submittedLicence, LicenceStatus.APPROVED)
      verify(staffRepository, times(1)).findByUsernameIgnoreCase(aCom.username)
      verify(notifyService, times(1)).sendReviewableLicenceApprovedEmail(
        aCom.email,
        submittedLicence.forename!!,
        submittedLicence.surname!!,
        submittedLicence.crn,
        submittedLicence.licenceStartDate,
        submittedLicence.id.toString(),
        submittedLicence.prisonDescription!!,
        isTimeServedLicence = true,
      )

      assertThat(licenceCaptor.value)
        .extracting("id", "statusCode", "approvedByUsername", "updatedByUsername", "updatedBy")
        .isEqualTo(
          listOf(
            submittedLicence.id,
            LicenceStatus.APPROVED,
            aCom.username,
            aCom.username,
            aCom,
          ),
        )

      assertThat(auditCaptor.firstValue).extracting("licenceId", "username", "fullName", "summary", "eventType")
        .isEqualTo(
          listOf(
            submittedLicence.id,
            aCom.username,
            "${aCom.firstName} ${aCom.lastName}",
            "Licence approved for ${submittedLicence.forename} ${submittedLicence.surname}",
            USER_EVENT,
          ),
        )
    }

    @Test
    fun `approving a CRD licence does not send a hard stop licence approval email`() {
      val submittedLicence =
        aLicenceEntity.copy(id = 2L, statusCode = LicenceStatus.SUBMITTED)

      whenever(licenceRepository.findById(submittedLicence.id)).thenReturn(Optional.of(submittedLicence))
      whenever(staffRepository.findByUsernameIgnoreCase(aCom.username)).thenReturn(aCom)

      service.updateLicenceStatus(
        submittedLicence.id,
        StatusUpdateRequest(status = LicenceStatus.APPROVED, username = aCom.username, fullName = "Y"),
      )

      val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
      val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)

      verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
      verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())
      verify(domainEventsService, times(1)).recordDomainEvent(submittedLicence, LicenceStatus.APPROVED)
      verify(staffRepository, times(1)).findByUsernameIgnoreCase(aCom.username)
      verifyNoInteractions(notifyService)

      assertThat(licenceCaptor.value)
        .extracting("id", "statusCode", "approvedByUsername", "updatedByUsername", "updatedBy")
        .isEqualTo(
          listOf(
            submittedLicence.id,
            LicenceStatus.APPROVED,
            aCom.username,
            aCom.username,
            aCom,
          ),
        )

      assertThat(auditCaptor.firstValue).extracting("licenceId", "username", "fullName", "summary", "eventType")
        .isEqualTo(
          listOf(
            submittedLicence.id,
            aCom.username,
            "${aCom.firstName} ${aCom.lastName}",
            "Licence approved for ${submittedLicence.forename} ${submittedLicence.surname}",
            USER_EVENT,
          ),
        )
    }
  }

  @Nested
  inner class `timing out licences` {

    @Test
    fun `should time out licence, persist and audit upon timeout`() {
      service.timeout(
        aLicenceEntity,
        "due to entering hard stop",
      )

      val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
      val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)
      val eventCaptor = ArgumentCaptor.forClass(EntityLicenceEvent::class.java)

      verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
      assertThat(licenceCaptor.value)
        .extracting("statusCode", "updatedByUsername")
        .isEqualTo(listOf(LicenceStatus.TIMED_OUT, "SYSTEM"))

      verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())

      assertThat(auditCaptor.value)
        .extracting("licenceId", "username", "fullName", "summary", "eventType")
        .isEqualTo(
          listOf(
            1L,
            "SYSTEM",
            "SYSTEM",
            "Licence automatically timed out for ${aLicenceEntity.forename} ${aLicenceEntity.surname} due to entering hard stop",
            SYSTEM_EVENT,
          ),
        )

      verify(licenceEventRepository, times(1)).saveAndFlush(eventCaptor.capture())

      assertThat(eventCaptor.value)
        .extracting("licenceId", "eventType", "username", "forenames", "surname", "eventDescription")
        .isEqualTo(
          listOf(
            1L,
            LicenceEventType.TIMED_OUT,
            "SYSTEM",
            "SYSTEM",
            "SYSTEM",
            "Licence automatically timed out for ${aLicenceEntity.forename} ${aLicenceEntity.surname} due to entering hard stop",
          ),
        )

      verifyNoInteractions(notifyService)
    }

    @Test
    fun `should also send notification when the license status is timed out and licence is a previously approved edit`() {
      val approvedThenEditedLicence = aLicenceEntity.copy(
        id = 2L,
        versionOfId = 1L,
      )
      service.timeout(
        approvedThenEditedLicence,
        "due to sentence date changes",
      )

      val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
      val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)
      val eventCaptor = ArgumentCaptor.forClass(EntityLicenceEvent::class.java)

      verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
      assertThat(licenceCaptor.value)
        .extracting("statusCode", "updatedByUsername")
        .isEqualTo(listOf(LicenceStatus.TIMED_OUT, "SYSTEM"))

      verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())

      assertThat(auditCaptor.value)
        .extracting("licenceId", "username", "fullName", "summary", "eventType")
        .isEqualTo(
          listOf(
            2L,
            "SYSTEM",
            "SYSTEM",
            "Licence automatically timed out for ${approvedThenEditedLicence.forename} ${approvedThenEditedLicence.surname} due to sentence date changes",
            SYSTEM_EVENT,
          ),
        )

      verify(licenceEventRepository, times(1)).saveAndFlush(eventCaptor.capture())

      assertThat(eventCaptor.value)
        .extracting("licenceId", "eventType", "username", "forenames", "surname", "eventDescription")
        .isEqualTo(
          listOf(
            2L,
            LicenceEventType.TIMED_OUT,
            "SYSTEM",
            "SYSTEM",
            "SYSTEM",
            "Licence automatically timed out for ${approvedThenEditedLicence.forename} ${approvedThenEditedLicence.surname} due to sentence date changes",
          ),
        )

      verify(notifyService, times(1)).sendEditedLicenceTimedOutEmail(
        approvedThenEditedLicence.getCom().email ?: "",
        approvedThenEditedLicence.getCom().fullName,
        approvedThenEditedLicence.forename!!,
        approvedThenEditedLicence.surname!!,
        approvedThenEditedLicence.crn!!,
        approvedThenEditedLicence.licenceStartDate,
        approvedThenEditedLicence.id.toString(),
      )
    }

    @Test
    fun `should throw an exception when trying to timeout a licence that does not support hard stop`() {
      assertThrows<IllegalStateException> {
        service.timeout(createHdcLicence(), "shouldn't work!")
      }
    }
  }

  @Nested
  inner class `HDC Licences` {
    @Test
    fun `service returns a HDC licence by ID`() {
      whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(createHdcLicence()))
      whenever(licencePolicyService.getAllAdditionalConditions()).thenReturn(
        AllAdditionalConditions(mapOf("2.1" to mapOf("code" to anAdditionalCondition))),
      )

      val licence = service.getLicenceById(1L)

      assertThat(licence).isExactlyInstanceOf(HdcLicenceModel::class.java)

      verify(licenceRepository, times(1)).findById(1L)
    }

    @Test
    fun `service returns an HDC variation licence by ID`() {
      whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(anHdcVariationLicence))
      whenever(licencePolicyService.getAllAdditionalConditions()).thenReturn(
        AllAdditionalConditions(mapOf("2.1" to mapOf("code" to anAdditionalCondition))),
      )

      val licence = service.getLicenceById(1L)

      assertThat(licence).isExactlyInstanceOf(HdcVariationLicenceModel::class.java)

      verify(licenceRepository, times(1)).findById(1L)
    }

    @Test
    fun `submitting a HDC licence`() {
      val hdcLicence = createHdcLicence()

      whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(hdcLicence))
      whenever(staffRepository.findByUsernameIgnoreCase(aCom.username)).thenReturn(aCom)
      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(listOf(aPrisonerSearchPrisoner))
      whenever(eligibilityService.getEligibilityAssessment(eq(aPrisonerSearchPrisoner))).thenReturn(
        anEligibilityAssessment(),
      )

      service.submitLicence(
        1L,
        listOf(NotifyRequest("testName", "testEmail"), NotifyRequest("testName1", "testEmail2")),
      )

      val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
      val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)
      val eventCaptor = ArgumentCaptor.forClass(EntityLicenceEvent::class.java)

      verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
      verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())
      verify(licenceEventRepository, times(1)).saveAndFlush(eventCaptor.capture())

      assertThat(licenceCaptor.value)
        .extracting("id", "kind", "statusCode", "updatedByUsername", "updatedBy")
        .isEqualTo(listOf(1L, LicenceKind.HDC, LicenceStatus.SUBMITTED, aCom.username, aCom))

      assertThat(eventCaptor.value)
        .extracting("licenceId", "eventType", "eventDescription")
        .isEqualTo(
          listOf(
            1L,
            LicenceEventType.SUBMITTED,
            "Licence submitted for approval for ${hdcLicence.forename} ${hdcLicence.surname}",
          ),
        )

      assertThat(auditCaptor.value)
        .extracting("licenceId", "username", "fullName", "summary")
        .isEqualTo(
          listOf(
            1L,
            aCom.username,
            "X Y",
            "Licence submitted for approval for ${hdcLicence.forename} ${hdcLicence.surname}",
          ),
        )
    }

    @Test
    fun `submitting a HDC Variation licence`() {
      val hdcLicence = createHdcLicence()

      val variation = anHdcVariationLicence.copy(
        submittedBy = aCom,
      )

      whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(variation))
      whenever(staffRepository.findByUsernameIgnoreCase(aCom.username)).thenReturn(aCom)
      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(listOf(aPrisonerSearchPrisoner))
      whenever(eligibilityService.getEligibilityAssessment(eq(aPrisonerSearchPrisoner))).thenReturn(
        anEligibilityAssessment(),
      )

      service.submitLicence(
        1L,
        listOf(NotifyRequest("testName", "testEmail"), NotifyRequest("testName1", "testEmail2")),
      )

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
          eq(variation.submittedBy?.fullName ?: ""),
        )

      assertThat(licenceCaptor.value)
        .extracting("id", "kind", "statusCode", "updatedByUsername", "updatedBy")
        .isEqualTo(listOf(1L, LicenceKind.HDC_VARIATION, LicenceStatus.VARIATION_SUBMITTED, aCom.username, aCom))

      assertThat(eventCaptor.value)
        .extracting("licenceId", "eventType", "eventDescription")
        .isEqualTo(
          listOf(
            1L,
            LicenceEventType.VARIATION_SUBMITTED,
            "Licence submitted for approval for ${hdcLicence.forename} ${hdcLicence.surname}",
          ),
        )

      assertThat(auditCaptor.value)
        .extracting("licenceId", "username", "fullName", "summary")
        .isEqualTo(
          listOf(
            1L,
            aCom.username,
            "X Y",
            "Licence submitted for approval for ${hdcLicence.forename} ${hdcLicence.surname}",
          ),
        )
    }

    @Test
    fun `editing an approved HDC licence creates and saves a new licence version`() {
      whenever(staffRepository.findByUsernameIgnoreCase(any())).thenReturn(communityOffenderManager())
      whenever(licencePolicyService.currentPolicy(any())).thenReturn(
        LicencePolicy(
          "2.1",
          standardConditions = StandardConditions(emptyList(), emptyList()),
          additionalConditions = AdditionalConditions(emptyList(), emptyList()),
          changeHints = emptyList(),
        ),
      )
      val approvedLicence = anHdcLicenceEntity.copy(statusCode = LicenceStatus.APPROVED)
      whenever(licenceRepository.findById(1L)).thenReturn(
        Optional.of(approvedLicence),
      )
      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(listOf(aPrisonerSearchPrisoner))
      whenever(eligibilityService.getEligibilityAssessment(eq(aPrisonerSearchPrisoner))).thenReturn(
        anEligibilityAssessment(),
      )

      whenever(licenceRepository.save(any())).thenReturn(anHdcLicenceEntity)

      val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
      val licenceEventCaptor = ArgumentCaptor.forClass(LicenceEvent::class.java)
      val auditEventCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)

      service.editLicence(1L)

      verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
      with(licenceCaptor.value as HdcLicence) {
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
    fun `editing an approved HDC licence creates and saves a new licence version returns all conditions`() {
      whenever(staffRepository.findByUsernameIgnoreCase(any())).thenReturn(communityOffenderManager())
      whenever(licencePolicyService.currentPolicy(any())).thenReturn(
        LicencePolicy(
          "2.1",
          standardConditions = StandardConditions(emptyList(), emptyList()),
          additionalConditions = AdditionalConditions(emptyList(), emptyList()),
          changeHints = emptyList(),
        ),
      )

      val approvedLicence = anHdcLicenceEntity.copy(
        statusCode = LicenceStatus.APPROVED,
        additionalConditions = additionalConditions,
      )
      whenever(licenceRepository.findById(1L)).thenReturn(
        Optional.of(approvedLicence),
      )
      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(listOf(aPrisonerSearchPrisoner))
      whenever(eligibilityService.getEligibilityAssessment(eq(aPrisonerSearchPrisoner))).thenReturn(
        anEligibilityAssessment(),
      )

      whenever(licenceRepository.save(any())).thenReturn(approvedLicence)

      val newLicenceCaptor = argumentCaptor<Licence>()

      service.editLicence(1L)

      verify(licenceRepository, times(1)).saveAndFlush(newLicenceCaptor.capture())
      val newLicence = newLicenceCaptor.firstValue

      assertThat(newLicence.additionalConditions.size).isEqualTo(2)
      assertThat(newLicence.additionalConditions.first().conditionType).isEqualTo(LicenceType.AP.toString())
      assertThat(newLicence.additionalConditions.last().conditionType).isEqualTo(LicenceType.PSS.toString())
    }

    @Test
    fun `attempting to editing an HDC licence with status other than approved results in validation exception `() {
      val activeLicence = anHdcLicenceEntity.copy(statusCode = LicenceStatus.ACTIVE)
      whenever(licenceRepository.findById(1L)).thenReturn(
        Optional.of(activeLicence),
      )

      val exception = assertThrows<ValidationException> { service.editLicence(1L) }
      assertThat(exception).isInstanceOf(ValidationException::class.java)
      assertThat(exception).message().isEqualTo("Can only edit APPROVED licences")
    }

    @Test
    fun `editing an approved licence which already has an in progress version returns that version`() {
      whenever(staffRepository.findByUsernameIgnoreCase(any())).thenReturn(communityOffenderManager())
      val approvedLicence = anHdcLicenceEntity.copy(statusCode = LicenceStatus.APPROVED)
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
  }

  @Test
  fun `should update the licence kind and eligible kind if different to the current licence values`() {
    val licence = aLicenceEntity
    val updatedKind = LicenceKind.PRRD
    val updatedLicence = createPrrdLicence()
    val staff = communityOffenderManager()

    whenever(staffRepository.findByUsernameIgnoreCase(any())).thenReturn(staff)
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(updatedLicence))

    val result = service.updateLicenceKind(licence, updatedKind)

    assertThat(result).isEqualTo(updatedLicence)
    verify(licenceRepository).updateLicenceKinds(licence.id, updatedKind, updatedKind)
    verify(auditService).recordAuditEventLicenceKindUpdated(
      licence,
      licence.kind,
      updatedKind,
      licence.eligibleKind,
      updatedKind,
      staff,
    )
  }

  @Test
  fun `should not update the licence kind if eligible kind is the same as the current licence kind`() {
    val licence = aLicenceEntity
    val eligibleKind = licence.kind
    val staff = communityOffenderManager()

    whenever(staffRepository.findByUsernameIgnoreCase(any())).thenReturn(staff)
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(licence))

    val result = service.updateLicenceKind(licence, eligibleKind)

    assertThat(result).isEqualTo(licence)
    verify(licenceRepository, never()).updateLicenceKinds(any(), any(), any())
    verify(auditService, never()).recordAuditEventLicenceKindUpdated(any(), any(), any(), any(), any(), any())
  }

  @Test
  fun `should not update the licence kind for a hard stop licence`() {
    val licence = createHardStopLicence().copy(eligibleKind = LicenceKind.CRD)
    val newEligibleKind = LicenceKind.PRRD
    val updatedLicence = licence.copy(eligibleKind = newEligibleKind)
    val staff = communityOffenderManager()

    whenever(staffRepository.findByUsernameIgnoreCase(any())).thenReturn(staff)
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(updatedLicence))

    val result = service.updateLicenceKind(licence, newEligibleKind)

    assertThat(result).isEqualTo(updatedLicence)
    verify(licenceRepository).updateLicenceKinds(licence.id, licence.kind, newEligibleKind)
    verify(auditService).recordAuditEventLicenceKindUpdated(
      licence,
      licence.kind,
      licence.kind,
      licence.eligibleKind,
      newEligibleKind,
      staff,
    )
  }

  @Test
  fun `should not update the licence kind for a time served licence`() {
    val licence = createTimeServedLicence().copy(eligibleKind = LicenceKind.CRD)
    val newEligibleKind = LicenceKind.PRRD
    val updatedLicence = licence.copy(eligibleKind = newEligibleKind)
    val staff = communityOffenderManager()

    whenever(staffRepository.findByUsernameIgnoreCase(any())).thenReturn(staff)
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(updatedLicence))

    val result = service.updateLicenceKind(licence, newEligibleKind)

    assertThat(result).isEqualTo(updatedLicence)
    verify(licenceRepository).updateLicenceKinds(licence.id, licence.kind, newEligibleKind)
    verify(auditService).recordAuditEventLicenceKindUpdated(
      licence,
      licence.kind,
      licence.kind,
      licence.eligibleKind,
      newEligibleKind,
      staff,
    )
  }

  val aCom = communityOffenderManager()
  val aPreviousUser = anotherCommunityOffenderManager()

  val anAdditionalCondition = AdditionalConditionAp(
    code = "code",
    category = "category",
    text = "text",
    requiresInput = false,
  )

  val aLicenceEntity = createCrdLicence().copy(
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
    forename = "Person",
    surname = "One",
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
    responsibleCom = aCom,
    createdBy = aCom,
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
          conditionType = "AP",
          licence = it,
        ),
        EntityStandardCondition(
          id = 2,
          conditionCode = "notBreakLaw",
          conditionSequence = 2,
          conditionText = "Do not break any law",
          conditionType = "AP",
          licence = it,
        ),
        EntityStandardCondition(
          id = 3,
          conditionCode = "attendMeetings",
          conditionSequence = 3,
          conditionText = "Attend meetings",
          conditionType = "AP",
          licence = it,
        ),
      ),
    )
  }

  val anHdcLicenceEntity = createHdcLicence().copy(
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
    forename = "John",
    surname = "Smith",
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
    responsibleCom = aCom,
    createdBy = aCom,
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
          conditionType = "AP",
          licence = it,
        ),
        EntityStandardCondition(
          id = 2,
          conditionCode = "notBreakLaw",
          conditionSequence = 2,
          conditionText = "Do not break any law",
          conditionType = "AP",
          licence = it,
        ),
        EntityStandardCondition(
          id = 3,
          conditionCode = "attendMeetings",
          conditionSequence = 3,
          conditionText = "Attend meetings",
          conditionType = "AP",
          licence = it,
        ),
      ),
    )
  }

  private val aVariationLicence = createVariationLicence()

  private val aTimeServedLicence = createTimeServedLicence()

  val anHdcVariationLicence = createHdcVariationLicence()

  private val aLicenceSummary = LicenceSummary(
    kind = LicenceKind.CRD,
    licenceId = 1,
    licenceType = LicenceType.AP,
    licenceStatus = LicenceStatus.IN_PROGRESS,
    nomisId = "A1234AA",
    forename = "Person",
    surname = "One",
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
    comUsername = aCom.username,
    bookingId = 54321,
    dateCreated = LocalDateTime.of(2022, 7, 27, 15, 0, 0),
    approvedByName = "jim smith",
    approvedDate = LocalDateTime.of(2023, 9, 19, 16, 38, 42),
    licenceVersion = "1.0",
    isReviewNeeded = false,
  )

  private val someAdditionalConditionData = mutableListOf(
    AdditionalConditionData(
      id = 1,
      dataField = "dataField",
      dataValue = "dataValue",
      additionalCondition = anAdditionalCondition(
        id = 1,
        licence = aLicenceEntity,
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

  val aPrisonerSearchPrisoner = prisonerSearchResult()
}
