package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.support

import jakarta.validation.ValidationException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence.Companion.SYSTEM_USER
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.LicenceEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.OverrideLicenceDatesRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.OverrideLicencePrisonerDetailsRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StaffRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.communityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createCrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createHdcLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createVariationLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.DomainEventsService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AuditEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.ACTIVE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.APPROVED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.INACTIVE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.SUBMITTED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.VARIATION_APPROVED
import java.time.LocalDate
import java.util.Optional

class LicenceOverrideServiceTest {

  private val licenceRepository = mock<LicenceRepository>()
  private val auditEventRepository = mock<AuditEventRepository>()
  private val licenceEventRepository = mock<LicenceEventRepository>()
  private val domainEventsService = mock<DomainEventsService>()
  private val staffRepository = mock<StaffRepository>()
  private val licenceService = mock<LicenceService>()
  private val licenceOverrideService =
    LicenceOverrideService(
      licenceRepository,
      auditEventRepository,
      licenceEventRepository,
      domainEventsService,
      staffRepository,
      licenceService,
    )

  val inactiveLicenceA = createCrdLicence().copy(
    statusCode = INACTIVE,
  )
  val inactiveLicenceB = createCrdLicence().copy(
    statusCode = INACTIVE,
  )
  val approvedLicenceA = createCrdLicence().copy(
    statusCode = APPROVED,
  )
  val approvedLicenceB = createCrdLicence().copy(
    statusCode = APPROVED,
  )
  val approvedHdcLicenceA = createHdcLicence().copy(
    statusCode = APPROVED,
  )
  val activeVariationLicence = createVariationLicence().copy(
    statusCode = ACTIVE,
  )
  val variationApprovedLicence = createCrdLicence().copy(
    statusCode = VARIATION_APPROVED,
  )
  val activeLicence = createCrdLicence().copy(
    statusCode = ACTIVE,
  )

  val aCom = communityOffenderManager()

  val aPreviousUser = CommunityOffenderManager(
    staffIdentifier = 4000,
    staffCode = "test-code",
    username = "test",
    email = "test@test.com",
    firstName = "Test",
    lastName = "Test",
  )

  @BeforeEach
  fun reset() {
    val authentication = mock<Authentication>()
    val securityContext = mock<SecurityContext>()

    whenever(authentication.name).thenReturn(aCom.username)
    whenever(securityContext.authentication).thenReturn(authentication)
    SecurityContextHolder.setContext(securityContext)

    reset(licenceRepository, staffRepository)
  }

  @Test
  fun `Override status fails if another licence for the same offender already has requested status`() {
    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(
      listOf(inactiveLicenceA, approvedLicenceA, approvedLicenceB),
    )

    whenever(licenceRepository.findById(approvedLicenceB.id)).thenReturn(Optional.of(approvedLicenceB))

    whenever(staffRepository.findByUsernameIgnoreCase(aCom.username)).thenReturn(aCom)

    val exception = assertThrows<ValidationException> {
      licenceOverrideService.changeStatus(approvedLicenceB.id, APPROVED, "Test Exception")
    }

    assertThat(exception).isInstanceOf(ValidationException::class.java)
    assertThat(exception.message).isEqualTo("$APPROVED is already in use for this offender on another licence")

    verifyNoInteractions(domainEventsService)
  }

  @Test
  fun `Override status should not fail when changing to INACTIVE regardless of other licences`() {
    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(
      listOf(inactiveLicenceA, inactiveLicenceB, approvedLicenceA, approvedLicenceB),
    )

    whenever(licenceRepository.findById(approvedLicenceB.id)).thenReturn(Optional.of(approvedLicenceB))

    whenever(staffRepository.findByUsernameIgnoreCase(aCom.username)).thenReturn(aCom)

    val reasonForChange = "Test override from $APPROVED to $INACTIVE"

    licenceOverrideService.changeStatus(
      approvedLicenceB.id,
      INACTIVE,
      reasonForChange,
    )

    val licenceCaptor = ArgumentCaptor.forClass(Licence::class.java)
    val auditCaptor = ArgumentCaptor.forClass(AuditEvent::class.java)
    val licenceEventCaptor = ArgumentCaptor.forClass(LicenceEvent::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())
    verify(licenceEventRepository, times(1)).saveAndFlush(licenceEventCaptor.capture())
    verify(domainEventsService, times(1)).recordDomainEvent(approvedLicenceB, INACTIVE)
    verify(staffRepository, times(1)).findByUsernameIgnoreCase(aCom.username)

    assertThat(licenceCaptor.value)
      .extracting("statusCode", "updatedByUsername", "licenceActivatedDate", "updatedBy")
      .isEqualTo(listOf(INACTIVE, aCom.username, null, aCom))

    assertThat(auditCaptor.value).extracting("licenceId", "username", "eventType", "summary")
      .isEqualTo(
        listOf(
          approvedLicenceB.id,
          aCom.username,
          AuditEventType.USER_EVENT,
          "Licence status overridden to $INACTIVE for John Smith: $reasonForChange",
        ),
      )

    assertThat(licenceEventCaptor.value)
      .extracting("licenceId", "username", "eventType", "eventDescription")
      .isEqualTo(listOf(approvedLicenceB.id, aCom.username, LicenceEventType.INACTIVE, reasonForChange))
  }

  @Test
  fun `Override status updates licence as expected for unused status code`() {
    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(
      listOf(inactiveLicenceA, approvedLicenceA, approvedLicenceB),
    )

    whenever(licenceRepository.findById(approvedLicenceA.id)).thenReturn(Optional.of(approvedLicenceA))

    whenever(staffRepository.findByUsernameIgnoreCase(aCom.username)).thenReturn(aCom)

    val reasonForChange = "Test override from $APPROVED to $SUBMITTED"

    licenceOverrideService.changeStatus(
      approvedLicenceA.id,
      SUBMITTED,
      reasonForChange,
    )

    val licenceCaptor = ArgumentCaptor.forClass(Licence::class.java)
    val auditCaptor = ArgumentCaptor.forClass(AuditEvent::class.java)
    val licenceEventCaptor = ArgumentCaptor.forClass(LicenceEvent::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())
    verify(licenceEventRepository, times(1)).saveAndFlush(licenceEventCaptor.capture())
    verify(domainEventsService, times(1)).recordDomainEvent(approvedLicenceA, SUBMITTED)
    verify(staffRepository, times(1)).findByUsernameIgnoreCase(aCom.username)

    assertThat(licenceCaptor.value)
      .extracting("statusCode", "updatedByUsername", "licenceActivatedDate", "updatedBy")
      .isEqualTo(listOf(SUBMITTED, aCom.username, null, aCom))

    assertThat(auditCaptor.value).extracting("licenceId", "username", "eventType", "summary")
      .isEqualTo(
        listOf(
          approvedLicenceA.id,
          aCom.username,
          AuditEventType.USER_EVENT,
          "Licence status overridden to $SUBMITTED for John Smith: $reasonForChange",
        ),
      )

    assertThat(licenceEventCaptor.value)
      .extracting("licenceId", "username", "eventType", "eventDescription")
      .isEqualTo(listOf(approvedLicenceA.id, aCom.username, LicenceEventType.SUBMITTED, reasonForChange))
  }

  @Test
  fun `update licenceActivatedDate when licence status is ACTIVE`() {
    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(
      listOf(inactiveLicenceA, approvedLicenceA, approvedLicenceB),
    )

    whenever(licenceRepository.findById(approvedLicenceA.id)).thenReturn(Optional.of(approvedLicenceA))

    whenever(staffRepository.findByUsernameIgnoreCase(aCom.username)).thenReturn(aCom)

    val reasonForChange = "Test licenceActivatedDate when licence is made $ACTIVE"

    licenceOverrideService.changeStatus(
      approvedLicenceA.id,
      ACTIVE,
      reasonForChange,
    )

    val licenceCaptor = ArgumentCaptor.forClass(Licence::class.java)
    val auditCaptor = ArgumentCaptor.forClass(AuditEvent::class.java)
    val licenceEventCaptor = ArgumentCaptor.forClass(LicenceEvent::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())
    verify(licenceEventRepository, times(1)).saveAndFlush(licenceEventCaptor.capture())
    verify(domainEventsService, times(1)).recordDomainEvent(approvedLicenceA, ACTIVE)
    verify(staffRepository, times(1)).findByUsernameIgnoreCase(aCom.username)

    assertThat(licenceCaptor.value.licenceActivatedDate).isNotNull()

    assertThat(licenceCaptor.value)
      .extracting("statusCode", "updatedByUsername", "licenceActivatedDate", "updatedBy")
      .isEqualTo(listOf(ACTIVE, aCom.username, licenceCaptor.value.licenceActivatedDate, aCom))

    assertThat(auditCaptor.value).extracting("licenceId", "username", "eventType", "summary")
      .isEqualTo(
        listOf(
          approvedLicenceA.id,
          aCom.username,
          AuditEventType.USER_EVENT,
          "Licence status overridden to $ACTIVE for John Smith: $reasonForChange",
        ),
      )

    assertThat(licenceEventCaptor.value)
      .extracting("licenceId", "username", "eventType", "eventDescription")
      .isEqualTo(listOf(approvedLicenceA.id, aCom.username, LicenceEventType.ACTIVATED, reasonForChange))
  }

  @Test
  fun `inactivates timed out licences when new licence status is ACTIVE`() {
    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(
      listOf(inactiveLicenceA, approvedLicenceA, approvedLicenceB),
    )

    whenever(licenceRepository.findById(approvedLicenceA.id)).thenReturn(Optional.of(approvedLicenceA))

    whenever(staffRepository.findByUsernameIgnoreCase(aCom.username)).thenReturn(aCom)

    val reasonForChange = "Test licenceActivatedDate when licence is made $ACTIVE"

    licenceOverrideService.changeStatus(
      approvedLicenceA.id,
      ACTIVE,
      reasonForChange,
    )

    verify(licenceService).inactivateTimedOutLicenceVersions(
      listOf(approvedLicenceA),
      "Deactivating timed out licence as the licence status was overridden to active",
    )
  }

  @Test
  fun `Override status updates licence variation to ACTIVE`() {
    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(
      emptyList(),
    )

    whenever(licenceRepository.findById(variationApprovedLicence.id)).thenReturn(Optional.of(variationApprovedLicence))

    whenever(staffRepository.findByUsernameIgnoreCase(aCom.username)).thenReturn(aCom)

    val reasonForChange = "Test override from $VARIATION_APPROVED to $ACTIVE"

    licenceOverrideService.changeStatus(
      variationApprovedLicence.id,
      ACTIVE,
      reasonForChange,
    )

    val licenceCaptor = ArgumentCaptor.forClass(Licence::class.java)
    val auditCaptor = ArgumentCaptor.forClass(AuditEvent::class.java)
    val licenceEventCaptor = ArgumentCaptor.forClass(LicenceEvent::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())
    verify(licenceEventRepository, times(1)).saveAndFlush(licenceEventCaptor.capture())
    verify(domainEventsService, times(1)).recordDomainEvent(variationApprovedLicence, ACTIVE)
    verify(staffRepository, times(1)).findByUsernameIgnoreCase(aCom.username)

    assertThat(licenceCaptor.value)
      .extracting("statusCode", "updatedByUsername", "licenceActivatedDate", "updatedBy")
      .isEqualTo(listOf(ACTIVE, aCom.username, licenceCaptor.value.licenceActivatedDate, aCom))

    assertThat(auditCaptor.value).extracting("licenceId", "username", "eventType", "summary")
      .isEqualTo(
        listOf(
          variationApprovedLicence.id,
          aCom.username,
          AuditEventType.USER_EVENT,
          "Licence status overridden to $ACTIVE for John Smith: $reasonForChange",
        ),
      )

    assertThat(licenceEventCaptor.value)
      .extracting("licenceId", "username", "eventType", "eventDescription")
      .isEqualTo(listOf(variationApprovedLicence.id, aCom.username, LicenceEventType.ACTIVATED, reasonForChange))
  }

  @Test
  fun `Override status updates licence variation to INACTIVE`() {
    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(
      emptyList(),
    )

    whenever(licenceRepository.findById(activeVariationLicence.id)).thenReturn(Optional.of(activeVariationLicence))

    whenever(staffRepository.findByUsernameIgnoreCase(aCom.username)).thenReturn(aCom)

    val reasonForChange = "Test override from $ACTIVE to $INACTIVE"

    licenceOverrideService.changeStatus(
      activeVariationLicence.id,
      INACTIVE,
      reasonForChange,
    )

    val licenceCaptor = ArgumentCaptor.forClass(Licence::class.java)
    val auditCaptor = ArgumentCaptor.forClass(AuditEvent::class.java)
    val licenceEventCaptor = ArgumentCaptor.forClass(LicenceEvent::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())
    verify(licenceEventRepository, times(1)).saveAndFlush(licenceEventCaptor.capture())
    verify(domainEventsService, times(1)).recordDomainEvent(activeVariationLicence, INACTIVE)
    verify(staffRepository, times(1)).findByUsernameIgnoreCase(aCom.username)

    assertThat(licenceCaptor.value)
      .extracting("statusCode", "updatedByUsername", "licenceActivatedDate", "updatedBy")
      .isEqualTo(listOf(INACTIVE, aCom.username, null, aCom))

    assertThat(auditCaptor.value).extracting("licenceId", "username", "eventType", "summary")
      .isEqualTo(
        listOf(
          activeVariationLicence.id,
          aCom.username,
          AuditEventType.USER_EVENT,
          "Licence status overridden to $INACTIVE for John Smith: $reasonForChange",
        ),
      )

    assertThat(licenceEventCaptor.value)
      .extracting("licenceId", "username", "eventType", "eventDescription")
      .isEqualTo(listOf(activeVariationLicence.id, aCom.username, LicenceEventType.INACTIVE, reasonForChange))
  }

  @Test
  fun `Override dates updates licence dates`() {
    whenever(licenceRepository.findById(approvedLicenceA.id)).thenReturn(Optional.of(approvedLicenceA))
    whenever(staffRepository.findByUsernameIgnoreCase(aCom.username)).thenReturn(aCom)

    val request = OverrideLicenceDatesRequest(
      conditionalReleaseDate = LocalDate.now(),
      actualReleaseDate = LocalDate.now(),
      sentenceStartDate = LocalDate.now(),
      sentenceEndDate = LocalDate.now(),
      licenceStartDate = LocalDate.now(),
      licenceExpiryDate = LocalDate.now(),
      topupSupervisionStartDate = LocalDate.now(),
      topupSupervisionExpiryDate = LocalDate.now(),
      postRecallReleaseDate = LocalDate.now(),
      reason = "Test override dates",
    )

    licenceOverrideService.changeDates(
      approvedLicenceA.id,
      request,
    )

    val licenceCaptor = ArgumentCaptor.forClass(Licence::class.java)
    val auditCaptor = ArgumentCaptor.forClass(AuditEvent::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())
    verify(staffRepository, times(1)).findByUsernameIgnoreCase(aCom.username)

    assertThat(licenceCaptor.value)
      .extracting(
        "conditionalReleaseDate", "actualReleaseDate",
        "sentenceStartDate", "sentenceEndDate", "licenceStartDate", "licenceExpiryDate",
        "topupSupervisionStartDate", "topupSupervisionExpiryDate", "postRecallReleaseDate",
        "updatedByUsername", "updatedBy",
      )
      .isEqualTo(
        listOf(
          request.conditionalReleaseDate, request.actualReleaseDate, request.sentenceStartDate,
          request.sentenceEndDate, request.licenceStartDate, request.licenceExpiryDate,
          request.topupSupervisionStartDate, request.topupSupervisionExpiryDate, request.postRecallReleaseDate,
          aCom.username, aCom,
        ),
      )

    assertThat(auditCaptor.value).extracting("licenceId", "username", "eventType", "summary")
      .isEqualTo(
        listOf(
          approvedLicenceA.id,
          aCom.username,
          AuditEventType.USER_EVENT,
          "Sentence dates overridden for John Smith: ${request.reason}",
        ),
      )
  }

  @Test
  fun `Override dates updates HDC licence dates`() {
    whenever(licenceRepository.findById(approvedHdcLicenceA.id)).thenReturn(Optional.of(approvedHdcLicenceA))
    whenever(staffRepository.findByUsernameIgnoreCase(aCom.username)).thenReturn(aCom)

    val request = OverrideLicenceDatesRequest(
      conditionalReleaseDate = LocalDate.now(),
      actualReleaseDate = LocalDate.now(),
      sentenceStartDate = LocalDate.now(),
      sentenceEndDate = LocalDate.now(),
      licenceStartDate = LocalDate.now(),
      licenceExpiryDate = LocalDate.now(),
      topupSupervisionStartDate = LocalDate.now(),
      topupSupervisionExpiryDate = LocalDate.now(),
      postRecallReleaseDate = LocalDate.now(),
      homeDetentionCurfewActualDate = LocalDate.now(),
      reason = "Test override dates",
    )

    licenceOverrideService.changeDates(
      approvedHdcLicenceA.id,
      request,
    )

    val licenceCaptor = ArgumentCaptor.forClass(Licence::class.java)
    val auditCaptor = ArgumentCaptor.forClass(AuditEvent::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())
    verify(staffRepository, times(1)).findByUsernameIgnoreCase(aCom.username)

    assertThat(licenceCaptor.value)
      .extracting(
        "conditionalReleaseDate", "actualReleaseDate",
        "sentenceStartDate", "sentenceEndDate", "licenceStartDate", "licenceExpiryDate",
        "topupSupervisionStartDate", "topupSupervisionExpiryDate", "postRecallReleaseDate",
        "homeDetentionCurfewActualDate", "updatedByUsername", "updatedBy",
      )
      .isEqualTo(
        listOf(
          request.conditionalReleaseDate, request.actualReleaseDate, request.sentenceStartDate,
          request.sentenceEndDate, request.licenceStartDate, request.licenceExpiryDate,
          request.topupSupervisionStartDate, request.topupSupervisionExpiryDate, request.postRecallReleaseDate,
          request.homeDetentionCurfewActualDate, aCom.username, aCom,
        ),
      )

    assertThat(auditCaptor.value).extracting("licenceId", "username", "eventType", "summary")
      .isEqualTo(
        listOf(
          approvedHdcLicenceA.id,
          aCom.username,
          AuditEventType.USER_EVENT,
          "Sentence dates overridden for John Smith: ${request.reason}",
        ),
      )
  }

  @Test
  fun `updating user is retained and username is set to SYSTEM_USER when a staff member cannot be found`() {
    whenever(licenceRepository.findById(approvedLicenceA.id)).thenReturn(
      Optional.of(
        approvedLicenceA.copy(
          updatedBy = aPreviousUser,
        ),
      ),
    )
    whenever(staffRepository.findByUsernameIgnoreCase(aCom.username)).thenReturn(null)

    val request = OverrideLicenceDatesRequest(
      conditionalReleaseDate = LocalDate.now(),
      actualReleaseDate = LocalDate.now(),
      sentenceStartDate = LocalDate.now(),
      sentenceEndDate = LocalDate.now(),
      licenceStartDate = LocalDate.now(),
      licenceExpiryDate = LocalDate.now(),
      topupSupervisionStartDate = LocalDate.now(),
      topupSupervisionExpiryDate = LocalDate.now(),
      reason = "Test override dates",
    )

    licenceOverrideService.changeDates(
      approvedLicenceA.id,
      request,
    )

    val licenceCaptor = ArgumentCaptor.forClass(Licence::class.java)
    val auditCaptor = ArgumentCaptor.forClass(AuditEvent::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())
    verify(staffRepository, times(1)).findByUsernameIgnoreCase(aCom.username)

    assertThat(licenceCaptor.value)
      .extracting(
        "conditionalReleaseDate", "actualReleaseDate",
        "sentenceStartDate", "sentenceEndDate", "licenceStartDate", "licenceExpiryDate",
        "topupSupervisionStartDate", "topupSupervisionExpiryDate", "updatedByUsername",
        "updatedBy",
      )
      .isEqualTo(
        listOf(
          request.conditionalReleaseDate, request.actualReleaseDate, request.sentenceStartDate,
          request.sentenceEndDate, request.licenceStartDate, request.licenceExpiryDate,
          request.topupSupervisionStartDate, request.topupSupervisionExpiryDate, SYSTEM_USER,
          aPreviousUser,
        ),
      )

    assertThat(auditCaptor.value).extracting("licenceId", "username", "eventType", "summary")
      .isEqualTo(
        listOf(
          approvedLicenceA.id,
          SYSTEM_USER,
          AuditEventType.USER_EVENT,
          "Sentence dates overridden for John Smith: ${request.reason}",
        ),
      )
  }

  @Test
  fun `Override prisoner details updates licence and creates audit event`() {
    val licence = activeLicence.copy()
    whenever(licenceRepository.findById(licence.id)).thenReturn(Optional.of(licence))
    whenever(staffRepository.findByUsernameIgnoreCase(aCom.username)).thenReturn(aCom)

    val request = OverrideLicencePrisonerDetailsRequest(
      forename = "New",
      middleNames = "Middle",
      surname = "Name",
      dateOfBirth = LocalDate.of(1998, 1, 1),
      reason = "Test override prisoner details",
    )

    licenceOverrideService.changePrisonerDetails(licence.id, request)
    val licenceCaptor = ArgumentCaptor.forClass(Licence::class.java)
    val auditCaptor = ArgumentCaptor.forClass(AuditEvent::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())
    verify(staffRepository, times(1)).findByUsernameIgnoreCase(aCom.username)

    assertThat(licenceCaptor.value)
      .extracting("forename", "middleNames", "surname", "dateOfBirth")
      .isEqualTo(listOf("New", "Middle", "Name", LocalDate.of(1998, 1, 1)))

    assertThat(auditCaptor.value).extracting("licenceId", "username", "eventType", "summary", "changes")
      .isEqualTo(
        listOf(
          activeLicence.id,
          aCom.username,
          AuditEventType.USER_EVENT,
          "Prisoner details overridden for licence with ID ${activeLicence.id}: ${request.reason}",
          mapOf(
            "dateOfBirth" to "1998-01-01",
            "forename" to "New",
            "middleNames" to "Middle",
            "reason" to "Test override prisoner details",
            "surname" to "Name",
          ),
        ),
      )
  }

  @Test
  fun `Override prisoner details does not update middleNames when not given`() {
    val licence = activeLicence.copy()
    whenever(licenceRepository.findById(licence.id)).thenReturn(Optional.of(licence))

    val request = OverrideLicencePrisonerDetailsRequest(
      forename = "New",
      surname = "Name",
      dateOfBirth = LocalDate.of(1998, 1, 1),
      reason = "Test override prisoner details",
    )

    licenceOverrideService.changePrisonerDetails(licence.id, request)
    val licenceCaptor = ArgumentCaptor.forClass(Licence::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    assertThat(licenceCaptor.value)
      .extracting("forename", "middleNames", "surname", "dateOfBirth")
      .isEqualTo(listOf("New", null, "Name", LocalDate.of(1998, 1, 1)))
  }
}
