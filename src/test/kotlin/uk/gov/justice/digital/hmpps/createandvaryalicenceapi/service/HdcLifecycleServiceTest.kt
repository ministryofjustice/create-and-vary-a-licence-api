package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.HdcLicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.HdcPostponementRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createCrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createHdcLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents.HdcCvlEventType
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

class HdcLifecycleServiceTest {

  private val licenceRepository = mock<LicenceRepository>()
  private val hdcLicenceRepository = mock<HdcLicenceRepository>()
  private val hdcPostponementRepository = mock<HdcPostponementRepository>()
  private val auditService = mock<AuditService>()
  private val crdLicenceFactory = mock<CrdLicenceFactory>()
  private val hdcLicenceFactory = mock<HdcLicenceFactory>()
  private val licenceService = mock<LicenceService>()
  private val licenceLinkingService = mock<LicenceLinkingService>()

  private val service =
    HdcLifecycleService(
      licenceRepository,
      hdcLicenceRepository,
      hdcPostponementRepository,
      auditService,
      crdLicenceFactory,
      hdcLicenceFactory,
      licenceLinkingService,
    )

  @BeforeEach
  fun reset() {
    service.licenceService = licenceService
    val authentication = mock<Authentication>()
    val securityContext = mock<SecurityContext>()
    whenever(authentication.name).thenReturn("tcom")
    whenever(securityContext.authentication).thenReturn(authentication)

    SecurityContextHolder.setContext(securityContext)

    reset(
      licenceRepository,
      hdcLicenceRepository,
      hdcPostponementRepository,
      auditService,
      crdLicenceFactory,
      hdcLicenceFactory,
      licenceService,
      licenceLinkingService,
    )
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
    val hdcToPopulate = createHdcLicence().copy(id = null, versionOfId = crdLicence.id)
    val populatedHdc = hdcToPopulate.copy(id = 2)
    val auditCaptor = argumentCaptor<AuditEvent>()

    whenever(licenceRepository.findInFlightCrdLicences("A1234AA")).thenReturn(listOf(crdLicence))
    whenever(hdcLicenceFactory.createFromCrd(crdLicence, IN_PROGRESS)).thenReturn(hdcToPopulate)
    whenever(licenceService.populateCopy(crdLicence, hdcToPopulate)).thenReturn(populatedHdc)

    // When
    service.convertToHdcLicence("A1234AA")

    // Then
    verify(hdcLicenceFactory).createFromCrd(crdLicence, IN_PROGRESS)
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
    verifyNoMoreInteractions(hdcLicenceFactory, licenceService, auditService, licenceLinkingService)
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
    val hdcToPopulate = createHdcLicence().copy(id = null, versionOfId = edit.id)
    val populatedHdc = hdcToPopulate.copy(id = 3)
    val auditCaptor = argumentCaptor<AuditEvent>()

    whenever(licenceRepository.findInFlightCrdLicences("A1234AA")).thenReturn(licences)
    whenever(hdcLicenceFactory.createFromCrd(edit, IN_PROGRESS)).thenReturn(hdcToPopulate)
    whenever(licenceService.populateCopy(edit, hdcToPopulate)).thenReturn(populatedHdc)

    // When
    service.convertToHdcLicence("A1234AA")

    // Then
    verify(hdcLicenceFactory).createFromCrd(edit, IN_PROGRESS)
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
    verifyNoMoreInteractions(hdcLicenceFactory, licenceService, auditService, licenceLinkingService)
  }

  @Test
  fun `convertToHdcLicence does nothing when there are no eligible CRD licences`() {
    // Given
    whenever(licenceRepository.findInFlightCrdLicences("A1234AA")).thenReturn(emptyList())

    // When
    service.convertToHdcLicence("A1234AA")

    // Then
    verifyNoInteractions(hdcLicenceFactory, licenceService, auditService, licenceLinkingService)
  }

  @Test
  fun `already IN_PROGRESS HDC postpone records current postponement state once`() {
    val hdcLicence = createHdcLicence(id = 1).copy(nomsId = "A1234BC", statusCode = IN_PROGRESS)
    whenever(hdcLicenceRepository.findInFlightHdcLicences("A1234BC")).thenReturn(listOf(hdcLicence))

    service.postponeLicence("A1234BC")

    verify(hdcPostponementRepository, times(1)).save(any())
    verify(auditService, times(1)).recordAuditEventHdcLicencePostponementChange(hdcLicence, true)
  }

  @Test
  fun `records postponement when only an approved HDC licence exists`() {
    val approvedLicence = createHdcLicence(id = 1).copy(nomsId = "A1234BC", statusCode = APPROVED)
    whenever(hdcLicenceRepository.findInFlightHdcLicences("A1234BC")).thenReturn(listOf(approvedLicence))

    service.postponeLicence("A1234BC")

    assertThat(approvedLicence.statusCode).isEqualTo(IN_PROGRESS)
    verify(hdcPostponementRepository, times(1)).save(any())
    verify(auditService, times(1)).recordAuditEventHdcLicencePostponementChange(approvedLicence, true)
    verify(auditService).recordAuditEventHdcLicencePostponed(approvedLicence, APPROVED)
  }

  @Test
  fun `postpone with approved and submitted licences inactivates the approved licence and reverts the submitted edit`() {
    val approvedLicence = createHdcLicence(id = 1).copy(nomsId = "A1234BC", statusCode = APPROVED)
    val submittedEdit = createHdcLicence(id = 2).copy(nomsId = "A1234BC", statusCode = SUBMITTED)
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
  fun `resume HDC postponement clears the current postponed state`() {
    val hdcLicence = createHdcLicence(id = 1).copy(nomsId = "A1234BC")
    whenever(hdcLicenceRepository.findInFlightHdcLicences("A1234BC")).thenReturn(listOf(hdcLicence))

    service.resumeHdcPostponement("A1234BC")

    verify(hdcPostponementRepository).save(any())
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

      val editLicence = createHdcLicence(id = 456L)
      editLicence.statusCode = SUBMITTED
      editLicence.nomsId = "A1234BC"
      editLicence.approvedByUsername = "approver"
      editLicence.approvedByName = "Approver Name"
      editLicence.approvedDate = LocalDateTime.of(2024, 1, 11, 9, 0)

      whenever(hdcLicenceRepository.findInFlightHdcLicences("A1234BC")).thenReturn(listOf(approvedLicence, editLicence))
      whenever(hdcPostponementRepository.findByLicenceId(editLicence.id)).thenReturn(null)
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
      verify(hdcPostponementRepository, times(2)).save(any())
    }

    @Test
    fun `inactivates an approved HDC licence and leaves an already IN_PROGRESS edit licence unchanged`() {
      // Given
      val approvedLicence = createHdcLicence(id = 123L)
      approvedLicence.statusCode = APPROVED
      approvedLicence.nomsId = "A1234BC"

      val editLicence = createHdcLicence(id = 456L)
      editLicence.statusCode = IN_PROGRESS
      editLicence.nomsId = "A1234BC"

      whenever(hdcLicenceRepository.findInFlightHdcLicences("A1234BC")).thenReturn(listOf(approvedLicence, editLicence))
      whenever(hdcPostponementRepository.findByLicenceId(editLicence.id)).thenReturn(null)
      whenever(hdcPostponementRepository.findByLicenceId(approvedLicence.id)).thenReturn(null)
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
      verify(hdcPostponementRepository, times(2)).save(any())
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
      whenever(hdcLicenceRepository.findInFlightHdcLicences("A1234BC")).thenReturn(listOf(hdcLicence))
      whenever(hdcPostponementRepository.findByLicenceId(hdcLicence.id)).thenReturn(null)

      // When
      service.postponeLicence("A1234BC")

      // Then
      assertThat(hdcLicence.statusCode).isEqualTo(IN_PROGRESS)
      verify(auditService).recordAuditEventHdcLicencePostponementChange(hdcLicence, true)
      verify(auditService).recordAuditEventHdcLicencePostponed(hdcLicence, SUBMITTED)
      verify(hdcPostponementRepository).save(any())
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
      val originalUpdatedAt = hdcLicence.dateLastUpdated

      whenever(hdcLicenceRepository.findInFlightHdcLicences("A1234BC")).thenReturn(listOf(hdcLicence))
      whenever(hdcPostponementRepository.findByLicenceId(hdcLicence.id)).thenReturn(null)

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
      verify(hdcPostponementRepository).save(any())
    }
  }

  @Nested
  inner class RevertLicenceToInProgress {
    @Test
    fun `reverts a SUBMITTED licence to IN_PROGRESS, clears approval details and audits`() {
      // Given
      val hdcLicence = createHdcLicence(id = 1L)
      hdcLicence.statusCode = SUBMITTED
      hdcLicence.approvedByUsername = "approver"
      hdcLicence.approvedByName = "Approver Name"
      hdcLicence.approvedDate = LocalDateTime.of(2024, 1, 10, 9, 0)
      hdcLicence.supersededDate = LocalDateTime.of(2024, 1, 11, 9, 0)

      // When
      service.revertLicenceToInProgress(hdcLicence)

      // Then
      assertThat(hdcLicence.statusCode).isEqualTo(IN_PROGRESS)
      assertThat(hdcLicence.approvedByUsername).isNull()
      assertThat(hdcLicence.approvedByName).isNull()
      assertThat(hdcLicence.approvedDate).isNull()
      assertThat(hdcLicence.supersededDate).isNull()
      verify(auditService).recordAuditEventHdcLicencePostponed(hdcLicence, SUBMITTED)
      verifyNoMoreInteractions(auditService)
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
      service.revertLicenceToInProgress(hdcLicence)

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
