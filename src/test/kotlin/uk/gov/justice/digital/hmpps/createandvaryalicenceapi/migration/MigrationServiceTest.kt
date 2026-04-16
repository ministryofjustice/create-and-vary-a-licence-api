package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.migration

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.BespokeCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Staff
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.migration.repository.MigrationRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.migration.request.MigrateAdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.migration.request.MigrateConditions
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.migration.request.MigrateFromHdcToCvlRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.migration.request.MigrateLicenceDetails
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.migration.request.MigrateLicenceLifecycleDetails
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.migration.request.MigratePrisonDetails
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.migration.request.MigratePrisonerDetails
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.migration.request.MigrateSentenceDetails
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StaffRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CvlRecordService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceCreationService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.CommunityManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate

class MigrationServiceTest {

  private val staffRepository = mock<StaffRepository>()
  private val deliusApiClient = mock<DeliusApiClient>()
  private val licenceCreationService = mock<LicenceCreationService>()
  private val licenceRepository = mock<LicenceRepository>()
  private val migrationRepository = mock<MigrationRepository>()
  private val cvlRecordService = mock<CvlRecordService>()
  private val prisonerSearchApiClient = mock<PrisonerSearchApiClient>()
  private val releaseDateService = mock<ReleaseDateService>()

  private val service = MigrationService(
    staffRepository,
    deliusApiClient,
    licenceCreationService,
    licenceRepository,
    migrationRepository,
    cvlRecordService,
    prisonerSearchApiClient,
  )

  @BeforeEach
  fun resetMocks() {
    reset(
      staffRepository,
      deliusApiClient,
      licenceCreationService,
      licenceRepository,
      migrationRepository,
      releaseDateService,
    )

    whenever(licenceRepository.saveAndFlush(any<HdcLicence>())).thenAnswer { it.arguments[0] }
  }

  @Test
  fun `migrate should orchestrate creation and save metadata`() {
    // Given
    val prisonerNumber = "A1234AA"
    val staffId = 1L

    val request = migrateRequest(
      prisonerNumber = prisonerNumber,
      additionalConditions = listOf(
        MigrateAdditionalCondition("cond1", "CODE1", 1),
      ),
    )

    val offenderManager = mock<CommunityManager>()
    whenever(offenderManager.id).thenReturn(staffId)

    val com = mock<CommunityOffenderManager>()

    whenever(deliusApiClient.getOffenderManager(prisonerNumber)).thenReturn(offenderManager)
    whenever(licenceCreationService.getOrCreateCom(staffId)).thenReturn(com)

    val savedLicence = TestData.createHdcLicence(id = 1L).apply {
      bespokeConditions.add(
        BespokeCondition(id = 10L, conditionText = "cond1", licence = this),
      )
    }

    whenever(licenceRepository.saveAndFlush(any<HdcLicence>())).thenReturn(savedLicence)

    // When
    service.migrate(request)

    // Then
    verify(deliusApiClient).getOffenderManager(any())
    verify(licenceCreationService).getOrCreateCom(staffId)
    verify(licenceRepository, atLeastOnce()).saveAndFlush(any())

    verify(migrationRepository).saveConditionMetaData(
      eq(1L),
      eq(10L),
      eq("CODE1"),
      eq(1),
    )

    verify(migrationRepository).saveMetaData(
      eq(1L),
      eq(2L),
      eq(3),
      eq(4),
    )
  }

  @Test
  fun `migrate should ask licence service for submitted by com if staff lookup needed then creation service`() {
    // Given
    val prisonerNumber = "A1234AA"
    val staffId = 1L
    val submittedByUserName = "NewSubmittedByUserName"

    val request = migrateRequest(
      prisonerNumber = prisonerNumber,
      submittedBy = submittedByUserName,
    )
    val offenderManager = mock<CommunityManager>()
    whenever(offenderManager.id).thenReturn(staffId)
    val responsibleCom = mock<CommunityOffenderManager>().apply {
      whenever(username).thenReturn("responsible")
    }
    val submittedByCom = mock<CommunityOffenderManager>()

    whenever(deliusApiClient.getOffenderManager(any())).thenReturn(offenderManager)
    whenever(licenceCreationService.getOrCreateCom(staffId)).thenReturn(responsibleCom)
    whenever(licenceCreationService.getOrCreateCom(submittedByUserName)).thenReturn(responsibleCom, submittedByCom)

    // When
    service.migrate(request)

    // Then
    verify(licenceCreationService).getOrCreateCom(submittedByUserName)
  }

  @Test
  fun `migrate should ask licence service for created by com if staff lookup needed then creation service`() {
    // Given
    val prisonerNumber = "A1234AA"
    val staffId = 1L
    val createdByUserName = "NewCreatedBy"

    val request = migrateRequest(
      prisonerNumber = prisonerNumber,
      createdByUserName = createdByUserName,
    )
    val offenderManager = mock<CommunityManager>()
    whenever(offenderManager.id).thenReturn(staffId)
    val responsibleCom = mock<CommunityOffenderManager>().apply {
      whenever(username).thenReturn("responsible")
    }
    val submittedByCom = mock<CommunityOffenderManager>()

    whenever(deliusApiClient.getOffenderManager(any())).thenReturn(offenderManager)
    whenever(licenceCreationService.getOrCreateCom(staffId)).thenReturn(responsibleCom)
    whenever(licenceCreationService.getOrCreateCom(createdByUserName)).thenReturn(responsibleCom, submittedByCom)

    // When
    service.migrate(request)

    // Then
    verify(licenceCreationService).getOrCreateCom(createdByUserName)
  }

  @Test
  fun `migrate should reuse responsibleCom for other coms if user names match`() {
    // Given
    val prisonerNumber = "A1234AA"
    val staffId = 1L
    val commonUserName = "commonUserName"

    val request = migrateRequest(
      prisonerNumber = prisonerNumber,
      createdByUserName = commonUserName,
      submittedBy = commonUserName,
    )

    val offenderManager = mock<CommunityManager>()
    whenever(offenderManager.id).thenReturn(staffId)
    val responsibleCom = mock<CommunityOffenderManager>().apply {
      whenever(username).thenReturn(commonUserName)
      whenever(fullName).thenReturn("commonFirstName commonLastName")
    }

    whenever(deliusApiClient.getOffenderManager(any())).thenReturn(offenderManager)
    whenever(licenceCreationService.getOrCreateCom(staffId)).thenReturn(responsibleCom)

    // When
    service.migrate(request)

    // Then
    val licenceCaptor = argumentCaptor<HdcLicence>()
    verify(licenceRepository).saveAndFlush(licenceCaptor.capture())
    val savedLicence = licenceCaptor.firstValue
    assertThat(savedLicence.createdBy).isEqualTo(savedLicence.responsibleCom)
    assertThat(savedLicence.submittedBy).isEqualTo(savedLicence.responsibleCom)
    assertThat(savedLicence.approvedByName).isNull()
  }

  @Test
  fun `migrate should get approved by name from then given approvedByUsername`() {
    // Given
    val prisonerNumber = "A1234AA"
    val staffId = 1L
    val approvedByUsername = "approvedByUsername"

    val request = migrateRequest(
      prisonerNumber = prisonerNumber,
      approvedByUsername = approvedByUsername,
    )

    val offenderManager = mock<CommunityManager>()
    whenever(offenderManager.id).thenReturn(staffId)
    whenever(deliusApiClient.getOffenderManager(any())).thenReturn(offenderManager)
    whenever(licenceCreationService.getOrCreateCom(staffId)).thenReturn(mock<CommunityOffenderManager>())

    val approvedByStaff = mock<Staff>().apply {
      whenever(username).thenReturn(approvedByUsername)
      whenever(fullName).thenReturn("approvedFirstName approvedLastName")
    }

    whenever(staffRepository.findByUsernameIgnoreCase(approvedByUsername)).thenReturn(approvedByStaff)

    // When
    service.migrate(request)

    // Then
    val licenceCaptor = argumentCaptor<HdcLicence>()
    verify(licenceRepository).saveAndFlush(licenceCaptor.capture())
    val savedLicence = licenceCaptor.firstValue
    assertThat(savedLicence.approvedByName).isEqualTo("approvedFirstName approvedLastName")
  }

  @Test
  fun `migrate should throw error when prisoner number missing`() {
    // Given
    val request = migrateRequest(prisonerNumber = null)

    // When / Then
    assertThatThrownBy { service.migrate(request) }
      .isInstanceOf(EntityNotFoundException::class.java)

    verifyNoInteractions(licenceRepository, migrationRepository)
  }

  private fun migrateRequest(
    prisonerNumber: String? = "A1234AA",
    submittedBy: String? = null,
    createdByUserName: String? = null,
    approvedByUsername: String? = null,
    additionalConditions: List<MigrateAdditionalCondition> = emptyList(),
  ): MigrateFromHdcToCvlRequest = MigrateFromHdcToCvlRequest(
    bookingNo = "BOOK1",
    bookingId = 1L,
    pnc = null,
    cro = null,
    prisoner = MigratePrisonerDetails(
      prisonerNumber = prisonerNumber,
      forename = "John",
      middleNames = null,
      surname = "Smith",
      dateOfBirth = LocalDate.now(),
    ),
    prison = MigratePrisonDetails("MDI", "Moorland", "123"),
    sentence = MigrateSentenceDetails(
      sentenceStartDate = LocalDate.now(),
      sentenceEndDate = LocalDate.now(),
      conditionalReleaseDate = LocalDate.now(),
      actualReleaseDate = LocalDate.now(),
      topupSupervisionStartDate = null,
      topupSupervisionExpiryDate = null,
      postRecallReleaseDate = null,
    ),
    licence = MigrateLicenceDetails(
      licenceId = 2,
      typeCode = LicenceType.AP,
      licenceActivationDate = null,
      homeDetentionCurfewActualDate = LocalDate.now(),
      homeDetentionCurfewEndDate = LocalDate.now(),
      licenceExpiryDate = LocalDate.now(),
      licenceVersion = 3,
      varyVersion = 4,
    ),
    lifecycle = MigrateLicenceLifecycleDetails(
      approvedDate = null,
      approvedByUsername = approvedByUsername,
      submittedDate = null,
      submittedByUserName = submittedBy,
      createdByUserName = createdByUserName,
      dateCreated = null,
    ),
    conditions = MigrateConditions(
      bespoke = emptyList(),
      additional = additionalConditions,
    ),
    curfewAddress = null,
    curfew = null,
    appointment = null,
  )
}
