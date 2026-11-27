package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcApplication
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.address.hdc.AccommodationType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.HdcLicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.aSetOfweeklyCurfewTimes
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createCrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createHdcLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.firstNightCurfewTimes
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.prisonerSearchResult
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents.HdcCvlEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceEventType.CRD_CREATED_WHEN_HDC_OPT_IN
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceEventType.CRD_CREATED_WHEN_HDC_OPT_OUT
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceEventType.CRD_CREATED_WHEN_HDC_REFUSED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceEventType.INACTIVE_WHEN_HDC_OPT_IN
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceEventType.INACTIVE_WHEN_HDC_OPT_OUT
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceEventType.INACTIVE_WHEN_HDC_REFUSED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.APPROVED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.INACTIVE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.IN_PROGRESS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.SUBMITTED
import java.time.LocalDateTime
import java.util.Optional

class HdcEventServiceTest {

  private val licenceRepository = mock<LicenceRepository>()
  private val hdcLicenceRepository = mock<HdcLicenceRepository>()
  private val auditService = mock<AuditService>()
  private val crdLicenceFactory = mock<CrdLicenceFactory>()
  private val hdcLicenceFactory = mock<HdcLicenceFactory>()
  private val prisonerSearchApiClient = mock<PrisonerSearchApiClient>()
  private val licenceService = mock<LicenceService>()
  private val licenceLinkingService = mock<LicenceLinkingService>()
  private val hdcLifecycleService = mock<HdcLifecycleService>()

  private val service =
    HdcEventService(
      licenceRepository,
      hdcLicenceRepository,
      auditService,
      crdLicenceFactory,
      hdcLicenceFactory,
      prisonerSearchApiClient,
      licenceLinkingService,
      licenceService,
      hdcLifecycleService,
    )

  @BeforeEach
  fun reset() {
    val authentication = mock<Authentication>()
    val securityContext = mock<SecurityContext>()
    whenever(authentication.name).thenReturn("tcom")
    whenever(securityContext.authentication).thenReturn(authentication)

    SecurityContextHolder.setContext(securityContext)

    reset(
      licenceRepository,
      hdcLicenceRepository,
      auditService,
      crdLicenceFactory,
      hdcLicenceFactory,
      prisonerSearchApiClient,
      licenceService,
      licenceLinkingService,
      hdcLifecycleService,
    )
    whenever(licenceLinkingService.getHdcLicenceIdForHdcReplacement(any())).thenReturn(null)
  }

  @ParameterizedTest
  @CsvSource(
    "IN_PROGRESS, OPT_OUT",
    "SUBMITTED, OPT_OUT",
    "APPROVED, OPT_OUT",
    "IN_PROGRESS, REFUSE",
    "SUBMITTED, REFUSE",
    "APPROVED, REFUSE",
  )
  fun `convertToCrdLicence creates an in progress CRD copy audits the conversion and inactivates the HDC licence`(
    status: LicenceStatus,
    eventType: HdcCvlEventType,
  ) {
    // Given
    val hdcLicence = createHdcLicence(id = 1).copy(nomsId = "A1234AA", statusCode = status)
    val crdToPopulate = createCrdLicence().copy(id = null, versionOfId = hdcLicence.id)
    val populatedCrd = crdToPopulate.copy(id = 2)
    val auditCaptor = argumentCaptor<AuditEvent>()

    whenever(hdcLicenceRepository.findInFlightHdcLicences("A1234AA")).thenReturn(listOf(hdcLicence))
    whenever(crdLicenceFactory.createFromHdc(hdcLicence, IN_PROGRESS)).thenReturn(crdToPopulate)
    whenever(licenceService.populateCopy(hdcLicence, crdToPopulate)).thenReturn(populatedCrd)

    // When
    service.convertToCrdLicence("A1234AA", eventType)

    // Then
    verify(crdLicenceFactory).createFromHdc(hdcLicence, IN_PROGRESS)
    verify(licenceService).populateCopy(hdcLicence, crdToPopulate)
    verify(licenceService).inactivateLicences(listOf(hdcLicence), "Licence automatically inactivated after HDC ${eventType.name} event", deactivateInProgressVersions = true)
    verify(auditService, times(2)).recordAuditEvent(auditCaptor.capture())

    val hdcAuditEvent = auditCaptor.firstValue
    assertThat(hdcAuditEvent.licenceId).isEqualTo(hdcLicence.id)
    assertThat(hdcAuditEvent.summary)
      .isEqualTo("HDC licence converted to CRD licence on ${eventType.name}")
    assertThat(hdcAuditEvent.detail)
      .isEqualTo("Old ID 1, new ID 2 type ${populatedCrd.typeCode} status IN_PROGRESS version ${populatedCrd.version}")

    val crdAuditEvent = auditCaptor.secondValue
    assertThat(crdAuditEvent.licenceId).isEqualTo(populatedCrd.id)
    assertThat(crdAuditEvent.summary)
      .isEqualTo("CRD licence converted from HDC on ${eventType.name}")
    assertThat(crdAuditEvent.detail)
      .isEqualTo("Old ID 1, new ID 2 type ${populatedCrd.typeCode} status IN_PROGRESS version ${populatedCrd.version}")

    verify(licenceService).createLicenceEvent(
      licenceId = hdcLicence.id,
      eventType = if (eventType == HdcCvlEventType.REFUSE) INACTIVE_WHEN_HDC_REFUSED else INACTIVE_WHEN_HDC_OPT_OUT,
      eventDescription = "This HDC licence was converted to CRD licence on ${eventType.name}",
    )

    verify(licenceService).createLicenceEvent(
      licenceId = populatedCrd.id,
      eventType = if (eventType == HdcCvlEventType.REFUSE) CRD_CREATED_WHEN_HDC_REFUSED else CRD_CREATED_WHEN_HDC_OPT_OUT,
      eventDescription = "This CRD licence was converted from HDC licence on ${eventType.name}",
    )
    if (eventType == HdcCvlEventType.OPT_OUT) {
      verify(licenceLinkingService).setCrdReplacementForOptedOutLicence(hdcLicence.id, populatedCrd.id)
    } else {
      verify(licenceLinkingService).setCrdReplacementForRefusedLicence(hdcLicence.id, populatedCrd.id)
    }
    verifyNoMoreInteractions(crdLicenceFactory, licenceService, auditService, licenceLinkingService)
  }

  @ParameterizedTest
  @CsvSource(
    "IN_PROGRESS, OPT_OUT",
    "SUBMITTED, OPT_OUT",
    "IN_PROGRESS, REFUSE",
    "SUBMITTED, REFUSE",
  )
  fun `convertToCrdLicence converts only the edit to an in progress CRD licence and inactivates both HDC versions`(
    editStatus: LicenceStatus,
    eventType: HdcCvlEventType,
  ) {
    val approvedLicence = createHdcLicence(id = 1).copy(nomsId = "A1234AA", statusCode = APPROVED)
    val edit = approvedLicence.copy(id = 2, versionOfId = approvedLicence.id, statusCode = editStatus)
    val licences = listOf(edit, approvedLicence)
    val crdToPopulate = createCrdLicence().copy(id = null, versionOfId = edit.id)
    val populatedCrd = crdToPopulate.copy(id = 3)
    val auditCaptor = argumentCaptor<AuditEvent>()

    whenever(hdcLicenceRepository.findInFlightHdcLicences("A1234AA")).thenReturn(licences)
    whenever(crdLicenceFactory.createFromHdc(edit, IN_PROGRESS)).thenReturn(crdToPopulate)
    whenever(licenceService.populateCopy(edit, crdToPopulate)).thenReturn(populatedCrd)

    service.convertToCrdLicence("A1234AA", eventType)

    verify(crdLicenceFactory).createFromHdc(edit, IN_PROGRESS)
    verify(licenceService).inactivateLicences(licences, "Licence automatically inactivated after HDC ${eventType.name} event", deactivateInProgressVersions = true)
    verify(licenceService).populateCopy(edit, crdToPopulate)
    verify(auditService, times(2)).recordAuditEvent(auditCaptor.capture())
    assertThat(auditCaptor.allValues.map { it.licenceId }).containsExactly(edit.id, populatedCrd.id)
    assertThat(auditCaptor.allValues.map { it.detail })
      .containsOnly("Old ID 2, new ID 3 type ${populatedCrd.typeCode} status IN_PROGRESS version ${populatedCrd.version}")
    verify(licenceService).createLicenceEvent(
      licenceId = edit.id,
      eventType = if (eventType == HdcCvlEventType.REFUSE) INACTIVE_WHEN_HDC_REFUSED else INACTIVE_WHEN_HDC_OPT_OUT,
      eventDescription = "This HDC licence was converted to CRD licence on ${eventType.name}",
    )
    verify(licenceService).createLicenceEvent(
      licenceId = populatedCrd.id,
      eventType = if (eventType == HdcCvlEventType.REFUSE) CRD_CREATED_WHEN_HDC_REFUSED else CRD_CREATED_WHEN_HDC_OPT_OUT,
      eventDescription = "This CRD licence was converted from HDC licence on ${eventType.name}",
    )
    if (eventType == HdcCvlEventType.OPT_OUT) {
      verify(licenceLinkingService).setCrdReplacementForOptedOutLicence(edit.id, populatedCrd.id)
    } else {
      verify(licenceLinkingService).setCrdReplacementForRefusedLicence(edit.id, populatedCrd.id)
    }
    verifyNoMoreInteractions(crdLicenceFactory, licenceService, auditService, licenceLinkingService)
  }

  @ParameterizedTest
  @EnumSource(HdcCvlEventType::class)
  fun `convertToCrdLicence does nothing when there are no eligible HDC licences`(eventType: HdcCvlEventType) {
    whenever(hdcLicenceRepository.findInFlightHdcLicences("A1234AA")).thenReturn(emptyList())

    service.convertToCrdLicence("A1234AA", eventType)

    verifyNoInteractions(crdLicenceFactory, licenceService, auditService, licenceLinkingService)
  }

  @ParameterizedTest
  @EnumSource(value = LicenceStatus::class, names = ["IN_PROGRESS", "SUBMITTED", "APPROVED"])
  fun `convertToHdcLicence creates an in progress HDC copy audits the conversion and inactivates the CRD licence`(
    status: LicenceStatus,
  ) {
    // Given
    val crdLicence = createCrdLicence().copy(id = 1, nomsId = "A1234AA", statusCode = status)
    val prisoner = prisonerSearchResult()
    val hdcToPopulate = createHdcLicence().copy(id = null, versionOfId = crdLicence.id)
    val populatedHdc = hdcToPopulate.copy(id = 2)
    val auditCaptor = argumentCaptor<AuditEvent>()

    whenever(licenceRepository.findInFlightCrdLicences("A1234AA")).thenReturn(listOf(crdLicence))
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(listOf("A1234AA"))).thenReturn(listOf(prisoner))
    whenever(hdcLicenceFactory.createFromCrd(crdLicence, prisoner, IN_PROGRESS)).thenReturn(hdcToPopulate)
    whenever(licenceService.populateCopy(crdLicence, hdcToPopulate)).thenReturn(populatedHdc)

    // When
    service.convertToHdcLicence("A1234AA")

    // Then
    verify(hdcLicenceFactory).createFromCrd(crdLicence, prisoner, IN_PROGRESS)
    verify(licenceService).populateCopy(crdLicence, hdcToPopulate)
    verify(licenceService).inactivateLicences(listOf(crdLicence), "Licence automatically inactivated after HDC OPT_IN event", deactivateInProgressVersions = true)
    verify(auditService, times(2)).recordAuditEvent(auditCaptor.capture())

    val crdAuditEvent = auditCaptor.firstValue
    assertThat(crdAuditEvent.licenceId).isEqualTo(crdLicence.id)
    assertThat(crdAuditEvent.summary)
      .isEqualTo("CRD licence converted to HDC licence on OPT_IN")
    assertThat(crdAuditEvent.detail)
      .isEqualTo("Old ID 1, new ID 2 type ${populatedHdc.typeCode} status IN_PROGRESS version ${populatedHdc.version}")

    val hdcAuditEvent = auditCaptor.secondValue
    assertThat(hdcAuditEvent.licenceId).isEqualTo(populatedHdc.id)
    assertThat(hdcAuditEvent.summary)
      .isEqualTo("HDC licence converted from CRD on OPT_IN")
    assertThat(hdcAuditEvent.detail)
      .isEqualTo("Old ID 1, new ID 2 type ${populatedHdc.typeCode} status IN_PROGRESS version ${populatedHdc.version}")

    verify(licenceService).createLicenceEvent(
      licenceId = crdLicence.id,
      eventType = INACTIVE_WHEN_HDC_OPT_IN,
      eventDescription = "This CRD licence was converted to HDC licence on OPT_IN",
    )
    verify(licenceService).createLicenceEvent(
      licenceId = populatedHdc.id,
      eventType = CRD_CREATED_WHEN_HDC_OPT_IN,
      eventDescription = "This HDC licence was converted from CRD licence on OPT_IN",
    )
    verify(licenceLinkingService).setHdcReplacementForOptedInLicence(crdLicence.id, populatedHdc.id)
    verify(licenceLinkingService).getHdcLicenceIdForHdcReplacement(crdLicence.id)
    verify(licenceLinkingService).getHdcLicenceIdForCrdReplacement(crdLicence.id)
    verify(hdcLifecycleService).populateOnCreation(populatedHdc)
    verifyNoMoreInteractions(hdcLicenceFactory, licenceService, auditService, licenceLinkingService, hdcLifecycleService)
  }

  @ParameterizedTest
  @EnumSource(value = LicenceStatus::class, names = ["IN_PROGRESS", "SUBMITTED"])
  fun `convertToHdcLicence converts only the edit to an in progress HDC licence and inactivates both CRD versions`(
    editStatus: LicenceStatus,
  ) {
    // Given
    val approvedLicence = createCrdLicence().copy(id = 1, nomsId = "A1234AA", statusCode = APPROVED)
    val edit = approvedLicence.copy(id = 2, versionOfId = approvedLicence.id, statusCode = editStatus)
    val licences = listOf(edit, approvedLicence)
    val prisoner = prisonerSearchResult()
    val hdcToPopulate = createHdcLicence().copy(id = null, versionOfId = edit.id)
    val populatedHdc = hdcToPopulate.copy(id = 3)
    val auditCaptor = argumentCaptor<AuditEvent>()

    whenever(licenceRepository.findInFlightCrdLicences("A1234AA")).thenReturn(licences)
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(listOf("A1234AA"))).thenReturn(listOf(prisoner))
    whenever(hdcLicenceFactory.createFromCrd(edit, prisoner, IN_PROGRESS)).thenReturn(hdcToPopulate)
    whenever(licenceService.populateCopy(edit, hdcToPopulate)).thenReturn(populatedHdc)

    // When
    service.convertToHdcLicence("A1234AA")

    // Then
    verify(hdcLicenceFactory).createFromCrd(edit, prisoner, IN_PROGRESS)
    verify(licenceService).inactivateLicences(licences, "Licence automatically inactivated after HDC OPT_IN event", deactivateInProgressVersions = true)
    verify(licenceService).populateCopy(edit, hdcToPopulate)
    verify(auditService, times(2)).recordAuditEvent(auditCaptor.capture())
    assertThat(auditCaptor.allValues.map { it.licenceId }).containsExactly(edit.id, populatedHdc.id)
    assertThat(auditCaptor.allValues.map { it.detail })
      .containsOnly("Old ID 2, new ID 3 type ${populatedHdc.typeCode} status IN_PROGRESS version ${populatedHdc.version}")
    verify(licenceService).createLicenceEvent(
      licenceId = edit.id,
      eventType = INACTIVE_WHEN_HDC_OPT_IN,
      eventDescription = "This CRD licence was converted to HDC licence on OPT_IN",
    )
    verify(licenceService).createLicenceEvent(
      licenceId = populatedHdc.id,
      eventType = CRD_CREATED_WHEN_HDC_OPT_IN,
      eventDescription = "This HDC licence was converted from CRD licence on OPT_IN",
    )
    verify(licenceLinkingService).setHdcReplacementForOptedInLicence(edit.id, populatedHdc.id)
    verify(licenceLinkingService).getHdcLicenceIdForHdcReplacement(edit.id)
    verify(licenceLinkingService).getHdcLicenceIdForCrdReplacement(edit.id)
    verifyNoMoreInteractions(hdcLicenceFactory, licenceService, auditService, licenceLinkingService)
  }

  @Test
  fun `convertToHdcLicence does nothing when there are no eligible CRD licences`() {
    // Given
    whenever(licenceRepository.findInFlightCrdLicences("A1234AA")).thenReturn(emptyList())

    // When
    service.convertToHdcLicence("A1234AA")

    // Then
    verifyNoInteractions(hdcLicenceFactory, licenceService, auditService, licenceLinkingService, hdcLifecycleService)
  }

  @Test
  fun `convertToHdcLicence continues when prisoner search has no result`() {
    val crdLicence = createCrdLicence().copy(id = 1, nomsId = "A1234AA")
    val hdcToPopulate = createHdcLicence().copy(id = null, versionOfId = crdLicence.id)
    val populatedHdc = hdcToPopulate.copy(id = 2)

    whenever(licenceRepository.findInFlightCrdLicences("A1234AA")).thenReturn(listOf(crdLicence))
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(listOf("A1234AA"))).thenReturn(emptyList())
    whenever(hdcLicenceFactory.createFromCrd(crdLicence, null, IN_PROGRESS)).thenReturn(hdcToPopulate)
    whenever(licenceService.populateCopy(crdLicence, hdcToPopulate)).thenReturn(populatedHdc)

    service.convertToHdcLicence("A1234AA")

    verify(prisonerSearchApiClient).searchPrisonersByNomisIds(listOf("A1234AA"))
    verify(hdcLicenceFactory).createFromCrd(crdLicence, null, IN_PROGRESS)
    verify(licenceService).populateCopy(crdLicence, hdcToPopulate)
  }

  @Test
  fun `convertToHdcLicence continues when the linked HDC licence cannot be found`() {
    // Given
    val crdLicence = createCrdLicence().copy(id = 1, nomsId = "A1234AA")
    val hdcCopy = createHdcLicence(id = 3)
    whenever(licenceRepository.findInFlightCrdLicences("A1234AA")).thenReturn(listOf(crdLicence))
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(listOf("A1234AA"))).thenReturn(emptyList())
    whenever(hdcLicenceFactory.createFromCrd(crdLicence, null, IN_PROGRESS)).thenReturn(hdcCopy)
    whenever(licenceService.populateCopy(crdLicence, hdcCopy)).thenReturn(hdcCopy)
    whenever(licenceLinkingService.getHdcLicenceIdForHdcReplacement(crdLicence.id)).thenReturn(2L)
    whenever(hdcLicenceRepository.findById(2L)).thenReturn(Optional.empty())

    // When
    service.convertToHdcLicence("A1234AA")

    // Then
    verify(hdcLicenceRepository).findById(2L)
    verify(licenceService).populateCopy(crdLicence, hdcCopy)
    verify(hdcLifecycleService).populateOnCreation(hdcCopy)
    verify(licenceLinkingService).setHdcReplacementForOptedInLicence(crdLicence.id, hdcCopy.id)
  }

  @Nested
  inner class CurfewTimesRestoration {
    private val crdLicence = createCrdLicence().copy(id = 1, nomsId = "A1234AA")
    private val originalHdcLicence = createHdcLicence(id = 2).copy(
      weeklyCurfewTimes = aSetOfweeklyCurfewTimes(),
      firstNightCurfewTimes = firstNightCurfewTimes(),
    )
    private val hdcCopy = createHdcLicence(id = 3)

    @BeforeEach
    fun setupConversion() {
      whenever(licenceRepository.findInFlightCrdLicences("A1234AA")).thenReturn(listOf(crdLicence))
      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(listOf("A1234AA"))).thenReturn(emptyList())
      whenever(hdcLicenceFactory.createFromCrd(crdLicence, null, IN_PROGRESS)).thenReturn(hdcCopy)
      whenever(licenceService.populateCopy(crdLicence, hdcCopy)).thenReturn(hdcCopy)
      whenever(hdcLicenceRepository.findById(originalHdcLicence.id)).thenReturn(Optional.of(originalHdcLicence))
    }

    @Test
    fun `convertToHdcLicence restores curfew times from the previous HDC licence without using the fallback`() {
      // Given
      whenever(licenceLinkingService.getHdcLicenceIdForHdcReplacement(crdLicence.id)).thenReturn(originalHdcLicence.id)

      // When
      service.convertToHdcLicence("A1234AA")

      // Then
      assertCurfewTimesRestored()
      verify(licenceLinkingService, never()).getHdcLicenceIdForCrdReplacement(any())
    }

    @Test
    fun `convertToHdcLicence restores curfew times from the original HDC licence when there is no previous HDC licence`() {
      // Given
      whenever(licenceLinkingService.getHdcLicenceIdForCrdReplacement(crdLicence.id)).thenReturn(originalHdcLicence.id)

      // When
      service.convertToHdcLicence("A1234AA")

      // Then
      assertCurfewTimesRestored()
      verify(licenceLinkingService).getHdcLicenceIdForHdcReplacement(crdLicence.id)
      verify(licenceLinkingService).getHdcLicenceIdForCrdReplacement(crdLicence.id)
    }

    private fun assertCurfewTimesRestored() {
      assertThat(hdcCopy.weeklyCurfewTimes).usingRecursiveComparison().isEqualTo(originalHdcLicence.weeklyCurfewTimes)
      hdcCopy.weeklyCurfewTimes.zip(originalHdcLicence.weeklyCurfewTimes).forEach { (copy, original) ->
        assertThat(copy).isNotSameAs(original)
        assertThat(copy.id).isNull()
      }
      assertThat(hdcCopy.firstNightCurfewTimes).usingRecursiveComparison().isEqualTo(originalHdcLicence.firstNightCurfewTimes)
      assertThat(hdcCopy.firstNightCurfewTimes).isNotSameAs(originalHdcLicence.firstNightCurfewTimes)
      assertThat(hdcCopy.firstNightCurfewTimes?.id).isNull()
      verify(licenceService).populateCopy(crdLicence, hdcCopy)
    }
  }

  @Test
  fun `postpone fails when an HDC application is missing`() {
    val hdcLicence = createHdcLicence(id = 1).copy(nomsId = "A1234BC", statusCode = IN_PROGRESS)
    whenever(hdcLicenceRepository.findInFlightHdcLicences("A1234BC")).thenReturn(listOf(hdcLicence))

    val exception = assertThrows<IllegalArgumentException> {
      service.postponeLicence("A1234BC")
    }
    assertThat(exception.message).isEqualTo("HDC application not found for licence 1")
    verifyNoInteractions(hdcLifecycleService, auditService)
  }

  @Test
  fun `postpone updates the existing application without changing its accommodation type`() {
    val licence = createHdcLicence(id = 1).copy(nomsId = "A1234BC", statusCode = IN_PROGRESS)
    val application = HdcApplication(licence = licence, isPostponed = false, proposedAccommodationType = AccommodationType.CAS_2)
    licence.application = application
    whenever(hdcLicenceRepository.findInFlightHdcLicences("A1234BC")).thenReturn(listOf(licence))

    service.postponeLicence("A1234BC")

    assertThat(application.isPostponed).isTrue()
    assertThat(application.proposedAccommodationType).isEqualTo(AccommodationType.CAS_2)
    assertThat(application.dateLastUpdated).isNotNull()
    verify(auditService).recordAuditEventHdcLicencePostponementChange(licence, true)
  }

  @Test
  fun `records postponement when only an approved HDC licence exists`() {
    val approvedLicence = createHdcLicence(id = 1).copy(nomsId = "A1234BC", statusCode = APPROVED)
    approvedLicence.application = HdcApplication(approvedLicence, false, null)
    whenever(hdcLicenceRepository.findInFlightHdcLicences("A1234BC")).thenReturn(listOf(approvedLicence))

    service.postponeLicence("A1234BC")

    assertThat(approvedLicence.statusCode).isEqualTo(IN_PROGRESS)
    assertThat(approvedLicence.application!!.isPostponed).isTrue()
    verify(auditService, times(1)).recordAuditEventHdcLicencePostponementChange(approvedLicence, true)
    verify(auditService).recordAuditEventHdcLicencePostponed(approvedLicence, APPROVED)
  }

  @Test
  fun `postpone with approved and submitted licences inactivates the approved licence and reverts the submitted edit`() {
    val approvedLicence = createHdcLicence(id = 1).copy(nomsId = "A1234BC", statusCode = APPROVED)
    val submittedEdit = createHdcLicence(id = 2).copy(nomsId = "A1234BC", statusCode = SUBMITTED)
    approvedLicence.application = HdcApplication(approvedLicence, false, null)
    submittedEdit.application = HdcApplication(submittedEdit, false, null)
    whenever(hdcLicenceRepository.findInFlightHdcLicences("A1234BC")).thenReturn(listOf(submittedEdit, approvedLicence))
    whenever(
      licenceService.inactivateLicences(
        listOf(approvedLicence),
        "HDC licence postponed",
        deactivateInProgressVersions = false,
      ),
    ).thenAnswer {
      approvedLicence.statusCode = INACTIVE
      null
    }

    service.postponeLicence("A1234BC")

    assertThat(approvedLicence.statusCode).isEqualTo(INACTIVE)
    assertThat(submittedEdit.statusCode).isEqualTo(IN_PROGRESS)
    verify(licenceService).inactivateLicences(
      listOf(approvedLicence),
      "HDC licence postponed",
      deactivateInProgressVersions = false,
    )
    verify(auditService, times(2)).recordAuditEventHdcLicencePostponementChange(any(), any())
    verify(auditService).recordAuditEventHdcLicencePostponed(submittedEdit, SUBMITTED)
  }

  @Test
  fun `address withdrawal with approved and submitted licences inactivates the approved licence and reverts the editable licence`() {
    // Given
    val approvedLicence = createHdcLicence(id = 1).copy(nomsId = "A1234BC", statusCode = APPROVED)
    val submittedLicence = createHdcLicence(id = 2).copy(nomsId = "A1234BC", statusCode = SUBMITTED)
    whenever(hdcLicenceRepository.findInFlightHdcLicences("A1234BC")).thenReturn(listOf(submittedLicence, approvedLicence))

    // When
    service.withdrawAddress("A1234BC")

    // Then
    verify(hdcLifecycleService).withdrawHdcCurfewAddresses(listOf(submittedLicence, approvedLicence))
    verify(licenceService).inactivateLicences(
      listOf(approvedLicence),
      "HDC address withdrawn",
      deactivateInProgressVersions = false,
    )
    assertThat(submittedLicence.statusCode).isEqualTo(IN_PROGRESS)
    verify(auditService).recordAuditEventHdcLicenceAddressWithdrawn(submittedLicence, SUBMITTED)
  }

  @Test
  fun `resume HDC postponement clears the current postponed state`() {
    val hdcLicence = createHdcLicence(id = 1).copy(nomsId = "A1234BC")
    hdcLicence.application = HdcApplication(hdcLicence, true, null)
    whenever(hdcLicenceRepository.findInFlightHdcLicences("A1234BC")).thenReturn(listOf(hdcLicence))

    service.resumeHdcPostponement("A1234BC")

    assertThat(hdcLicence.application!!.isPostponed).isFalse()
    verify(auditService).recordAuditEventHdcLicencePostponementChange(hdcLicence, false)
  }

  @Nested
  inner class TransitionHdcLicenceToInProgress {

    @Test
    fun `keeps an approved HDC licence and moves the current edit back to IN_PROGRESS`() {
      // Given
      val approvedLicence = createHdcLicence(id = 123L)
      approvedLicence.statusCode = APPROVED
      approvedLicence.nomsId = "A1234BC"
      approvedLicence.approvedByUsername = "approver"
      approvedLicence.approvedByName = "Approver Name"
      approvedLicence.approvedDate = LocalDateTime.of(2024, 1, 10, 9, 0)
      approvedLicence.application = HdcApplication(approvedLicence, false, null)

      val editLicence = createHdcLicence(id = 456L)
      editLicence.statusCode = SUBMITTED
      editLicence.nomsId = "A1234BC"
      editLicence.approvedByUsername = "approver"
      editLicence.approvedByName = "Approver Name"
      editLicence.approvedDate = LocalDateTime.of(2024, 1, 11, 9, 0)
      editLicence.application = HdcApplication(editLicence, false, null)

      whenever(hdcLicenceRepository.findInFlightHdcLicences("A1234BC")).thenReturn(listOf(approvedLicence, editLicence))
      whenever(
        licenceService.inactivateLicences(
          listOf(approvedLicence),
          "HDC licence postponed",
          deactivateInProgressVersions = false,
        ),
      ).thenAnswer {
        approvedLicence.statusCode = INACTIVE
        null
      }

      // When
      service.postponeLicence("A1234BC")

      // Then
      assertThat(approvedLicence.statusCode).isEqualTo(INACTIVE)
      assertThat(editLicence.statusCode).isEqualTo(IN_PROGRESS)
      assertThat(editLicence.approvedByUsername).isNull()
      assertThat(editLicence.approvedByName).isNull()
      assertThat(editLicence.approvedDate).isNull()
      assertThat(editLicence.supersededDate).isNull()
      verify(auditService, times(2)).recordAuditEventHdcLicencePostponementChange(any(), any())
      verify(auditService).recordAuditEventHdcLicencePostponed(editLicence, SUBMITTED)
      assertThat(approvedLicence.application!!.isPostponed).isTrue()
      assertThat(editLicence.application!!.isPostponed).isTrue()
    }

    @Test
    fun `inactivates an approved HDC licence and leaves an already IN_PROGRESS edit licence unchanged`() {
      // Given
      val approvedLicence = createHdcLicence(id = 123L)
      approvedLicence.statusCode = APPROVED
      approvedLicence.nomsId = "A1234BC"
      approvedLicence.application = HdcApplication(approvedLicence, false, null)

      val editLicence = createHdcLicence(id = 456L)
      editLicence.statusCode = IN_PROGRESS
      editLicence.nomsId = "A1234BC"
      editLicence.application = HdcApplication(editLicence, false, null)

      whenever(hdcLicenceRepository.findInFlightHdcLicences("A1234BC")).thenReturn(listOf(approvedLicence, editLicence))
      whenever(
        licenceService.inactivateLicences(
          listOf(approvedLicence),
          "HDC licence postponed",
          deactivateInProgressVersions = false,
        ),
      ).thenAnswer {
        approvedLicence.statusCode = INACTIVE
        null
      }

      // When
      service.postponeLicence("A1234BC")

      // Then
      assertThat(approvedLicence.statusCode).isEqualTo(INACTIVE)
      assertThat(editLicence.statusCode).isEqualTo(IN_PROGRESS)
      verify(auditService, times(2)).recordAuditEventHdcLicencePostponementChange(any(), any())
      verify(auditService, never()).recordAuditEventHdcLicencePostponed(any(), any())
      assertThat(approvedLicence.application!!.isPostponed).isTrue()
      assertThat(editLicence.application!!.isPostponed).isTrue()
    }

    @Test
    fun `transitions HDC licence from SUBMITTED to IN_PROGRESS`() {
      // Given
      val hdcLicence = createHdcLicence(id = 456L)
      hdcLicence.statusCode = SUBMITTED
      hdcLicence.nomsId = "A1234BC"
      hdcLicence.approvedByUsername = "approver"
      hdcLicence.approvedByName = "Approver Name"
      hdcLicence.approvedDate = LocalDateTime.of(2024, 1, 10, 9, 0)
      hdcLicence.application = HdcApplication(hdcLicence, false, null)
      whenever(hdcLicenceRepository.findInFlightHdcLicences("A1234BC")).thenReturn(listOf(hdcLicence))

      // When
      service.postponeLicence("A1234BC")

      // Then
      assertThat(hdcLicence.statusCode).isEqualTo(IN_PROGRESS)
      verify(auditService).recordAuditEventHdcLicencePostponementChange(hdcLicence, true)
      verify(auditService).recordAuditEventHdcLicencePostponed(hdcLicence, SUBMITTED)
      assertThat(hdcLicence.application!!.isPostponed).isTrue()
    }

    @Test
    fun `does not process when licence not found`() {
      // Given
      whenever(hdcLicenceRepository.findInFlightHdcLicences("A1234BC")).thenReturn(emptyList())

      // When
      service.postponeLicence("A1234BC")

      // Then
      verify(auditService, never()).recordAuditEventHdcLicencePostponementChange(any(), any())
      verify(auditService, never()).recordAuditEventHdcLicencePostponed(any(), any())
    }

    @Test
    fun `does not update or audit when HDC licence is already IN_PROGRESS`() {
      // Given
      val hdcLicence = createHdcLicence(id = 789L)
      hdcLicence.statusCode = IN_PROGRESS
      hdcLicence.nomsId = "A1234BC"
      hdcLicence.approvedByUsername = "approver"
      hdcLicence.approvedByName = "Approver Name"
      hdcLicence.approvedDate = LocalDateTime.of(2024, 1, 10, 9, 0)
      hdcLicence.application = HdcApplication(hdcLicence, false, null)
      val originalUpdatedAt = hdcLicence.dateLastUpdated

      whenever(hdcLicenceRepository.findInFlightHdcLicences("A1234BC")).thenReturn(listOf(hdcLicence))

      // When
      service.postponeLicence("A1234BC")

      // Then
      assertThat(hdcLicence.statusCode).isEqualTo(IN_PROGRESS)
      assertThat(hdcLicence.approvedByUsername).isEqualTo("approver")
      assertThat(hdcLicence.approvedByName).isEqualTo("Approver Name")
      assertThat(hdcLicence.approvedDate).isEqualTo(LocalDateTime.of(2024, 1, 10, 9, 0))
      assertThat(hdcLicence.dateLastUpdated).isEqualTo(originalUpdatedAt)
      verify(auditService, times(1)).recordAuditEventHdcLicencePostponementChange(hdcLicence, true)
      verify(auditService, never()).recordAuditEventHdcLicencePostponed(any(), any())
      assertThat(hdcLicence.application!!.isPostponed).isTrue()
    }
  }

  @Nested
  inner class RevertLicenceToInProgress {
    @Test
    fun `reverts a SUBMITTED licence to IN_PROGRESS and clears approval details when postponement event is triggered`() {
      // Given
      val hdcLicence = createHdcLicence(id = 1L)
      hdcLicence.statusCode = SUBMITTED
      hdcLicence.approvedByUsername = "approver"
      hdcLicence.approvedByName = "Approver Name"
      hdcLicence.approvedDate = LocalDateTime.of(2024, 1, 10, 9, 0)
      hdcLicence.supersededDate = LocalDateTime.of(2024, 1, 11, 9, 0)

      // When
      service.revertLicenceToInProgress(hdcLicence, HdcCvlEventType.POSTPONE)

      // Then
      assertThat(hdcLicence.statusCode).isEqualTo(IN_PROGRESS)
      assertThat(hdcLicence.approvedByUsername).isNull()
      assertThat(hdcLicence.approvedByName).isNull()
      assertThat(hdcLicence.approvedDate).isNull()
      assertThat(hdcLicence.supersededDate).isNull()
      verify(auditService).recordAuditEventHdcLicencePostponed(hdcLicence, SUBMITTED)
    }

    @Test
    fun `reverts a SUBMITTED licence to IN_PROGRESS and clears approval details when address withdrawn event is triggered`() {
      // Given
      val hdcLicence = createHdcLicence(id = 1L)
      hdcLicence.statusCode = SUBMITTED
      hdcLicence.approvedByUsername = "approver"
      hdcLicence.approvedByName = "Approver Name"
      hdcLicence.approvedDate = LocalDateTime.of(2024, 1, 10, 9, 0)
      hdcLicence.supersededDate = LocalDateTime.of(2024, 1, 11, 9, 0)

      // When
      service.revertLicenceToInProgress(hdcLicence, HdcCvlEventType.ADDRESS_WITHDRAWN)

      // Then
      assertThat(hdcLicence.statusCode).isEqualTo(IN_PROGRESS)
      assertThat(hdcLicence.approvedByUsername).isNull()
      assertThat(hdcLicence.approvedByName).isNull()
      assertThat(hdcLicence.approvedDate).isNull()
      assertThat(hdcLicence.supersededDate).isNull()
      verify(auditService).recordAuditEventHdcLicenceAddressWithdrawn(hdcLicence, SUBMITTED)
    }

    @Test
    fun `leaves an IN_PROGRESS licence unchanged and does not audit`() {
      // Given
      val approvedDate = LocalDateTime.of(2024, 1, 10, 9, 0)
      val supersededDate = LocalDateTime.of(2024, 1, 11, 9, 0)
      val hdcLicence = createHdcLicence(id = 1L)
      hdcLicence.statusCode = IN_PROGRESS
      hdcLicence.approvedByUsername = "approver"
      hdcLicence.approvedByName = "Approver Name"
      hdcLicence.approvedDate = approvedDate
      hdcLicence.supersededDate = supersededDate

      // When
      service.revertLicenceToInProgress(hdcLicence, HdcCvlEventType.POSTPONE)

      // Then
      assertThat(hdcLicence.statusCode).isEqualTo(IN_PROGRESS)
      assertThat(hdcLicence.approvedByUsername).isEqualTo("approver")
      assertThat(hdcLicence.approvedByName).isEqualTo("Approver Name")
      assertThat(hdcLicence.approvedDate).isEqualTo(approvedDate)
      assertThat(hdcLicence.supersededDate).isEqualTo(supersededDate)
      verifyNoInteractions(auditService)
    }
  }
}
