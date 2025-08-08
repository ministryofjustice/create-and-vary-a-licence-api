package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anyList
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HardStopLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.PrisonUser
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.PrrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.StandardCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.HdcCurfewAddress
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.CrdLicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.HdcCurfewAddressRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StaffRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StandardConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.ResourceAlreadyExistsException
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.prisonerSearchResult
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions.convertToTitleCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.HdcLicenceData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.HARD_STOP_CONDITION
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.LicencePolicyService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PhoneDetail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.Prison
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.CommunityManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.Detail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.Name
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ProbationCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.TeamDetail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.User
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.APPROVED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.IN_PROGRESS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.REJECTED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.SUBMITTED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.TIMED_OUT
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AuditEvent as EntityAuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcCurfewAddress as EntityHdcCurfewAddress
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence as EntityLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.LicenceEvent as EntityLicenceEvent

class LicenceCreationServiceTest {
  private val licencePolicyService = LicencePolicyService()

  private val additionalConditionRepository = mock<AdditionalConditionRepository>()
  private val hdcCurfewAddressRepository = mock<HdcCurfewAddressRepository>()
  private val standardConditionRepository = mock<StandardConditionRepository>()
  private val licenceRepository = mock<LicenceRepository>()
  private val crdLicenceRepository = mock<CrdLicenceRepository>()
  private val staffRepository = mock<StaffRepository>()
  private val licenceEventRepository = mock<LicenceEventRepository>()
  private val auditEventRepository = mock<AuditEventRepository>()
  private val prisonerSearchApiClient = mock<PrisonerSearchApiClient>()
  private val prisonApiClient = mock<PrisonApiClient>()
  private val deliusApiClient = mock<DeliusApiClient>()
  private val releaseDateService = mock<ReleaseDateService>()
  private val hdcService = mock<HdcService>()

  private val service = LicenceCreationService(
    licenceRepository,
    crdLicenceRepository,
    staffRepository,
    standardConditionRepository,
    additionalConditionRepository,
    licenceEventRepository,
    licencePolicyService,
    auditEventRepository,
    hdcCurfewAddressRepository,
    prisonerSearchApiClient,
    prisonApiClient,
    deliusApiClient,
    releaseDateService,
    hdcService,
  )

  @Nested
  inner class CreatingCrdLicences {

    @BeforeEach
    fun reset() {
      reset(
        licenceRepository,
        licenceEventRepository,
        auditEventRepository,
        prisonerSearchApiClient,
        prisonApiClient,
        deliusApiClient,
      )
      val authentication = mock<Authentication>()
      val securityContext = mock<SecurityContext>()

      whenever(authentication.name).thenReturn("tcom")
      whenever(securityContext.authentication).thenReturn(authentication)
      SecurityContextHolder.setContext(securityContext)

      whenever(prisonApiClient.getPrisonInformation(any())).thenReturn(somePrisonInformation)

      whenever(staffRepository.findByStaffIdentifier(2000)).thenReturn(com)
      whenever(staffRepository.findByUsernameIgnoreCase("tcom")).thenReturn(com)
      whenever(deliusApiClient.getOffenderManager(any())).thenReturn(aCommunityManager)

      whenever(additionalConditionRepository.saveAllAndFlush(anyList())).thenAnswer { it.arguments[0] }
      whenever(standardConditionRepository.saveAllAndFlush(anyList())).thenAnswer { it.arguments[0] }
      whenever(licenceRepository.saveAndFlush(any())).thenAnswer { it.arguments[0] }
    }

    @Test
    fun `service creates populates licence with expected fields`() {
      val aPrisonerSearchResult = prisonerSearchResult()
      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(listOf(aPrisonerSearchResult))
      whenever(deliusApiClient.getProbationCase(any())).thenReturn(aProbationCaseResult)
      whenever(releaseDateService.getLicenceStartDate(any(), anyOrNull())).thenReturn(LocalDate.of(2022, 10, 10))

      service.createCrdLicence(PRISON_NUMBER)

      val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
      verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())

      with(licenceCaptor.value as CrdLicence) {
        assertThat(kind).isEqualTo(LicenceKind.CRD)
        assertThat(typeCode).isEqualTo(LicenceType.AP)
        assertThat(version).isEqualTo("2.1")
        assertThat(statusCode).isEqualTo(IN_PROGRESS)
        assertThat(versionOfId).isNull()
        assertThat(licenceVersion).isEqualTo("1.0")
        assertThat(nomsId).isEqualTo(nomsId)
        assertThat(bookingNo).isEqualTo(aPrisonerSearchResult.bookNumber)
        assertThat(bookingId).isEqualTo(aPrisonerSearchResult.bookingId!!.toLong())
        assertThat(prisonCode).isEqualTo(aPrisonerSearchResult.prisonId)
        assertThat(forename).isEqualTo(aPrisonerSearchResult.firstName.convertToTitleCase())
        assertThat(middleNames).isEqualTo(aPrisonerSearchResult.middleNames?.convertToTitleCase() ?: "")
        assertThat(surname).isEqualTo(aPrisonerSearchResult.lastName.convertToTitleCase())
        assertThat(dateOfBirth).isEqualTo(aPrisonerSearchResult.dateOfBirth)
        assertThat(actualReleaseDate).isEqualTo(aPrisonerSearchResult.confirmedReleaseDate)
        assertThat(sentenceStartDate).isEqualTo(aPrisonerSearchResult.sentenceStartDate)
        assertThat(sentenceEndDate).isEqualTo(aPrisonerSearchResult.sentenceExpiryDate)
        assertThat(licenceStartDate).isEqualTo(LocalDate.of(2022, 10, 10))
        assertThat(licenceExpiryDate).isEqualTo(aPrisonerSearchResult.licenceExpiryDate)
        assertThat(topupSupervisionStartDate).isEqualTo(aPrisonerSearchResult.topupSupervisionStartDate)
        assertThat(topupSupervisionExpiryDate).isEqualTo(aPrisonerSearchResult.topupSupervisionExpiryDate)
        assertThat(postRecallReleaseDate).isNull()
        assertThat(prisonDescription).isEqualTo(somePrisonInformation.description)
        assertThat(prisonTelephone).isEqualTo(somePrisonInformation.getPrisonContactNumber())
        assertThat(probationAreaCode).isEqualTo(aCommunityManager.provider.code)
        assertThat(probationAreaDescription).isEqualTo(aCommunityManager.provider.description)
        assertThat(probationPduCode).isEqualTo(aCommunityManager.team.borough.code)
        assertThat(probationPduDescription).isEqualTo(aCommunityManager.team.borough.description)
        assertThat(probationLauCode).isEqualTo(aCommunityManager.team.district.code)
        assertThat(probationLauDescription).isEqualTo(aCommunityManager.team.district.description)
        assertThat(probationTeamCode).isEqualTo(aCommunityManager.team.code)
        assertThat(probationTeamDescription).isEqualTo(aCommunityManager.team.description)
        assertThat(crn).isEqualTo(aProbationCaseResult.crn)
        assertThat(pnc).isEqualTo(aProbationCaseResult.pncNumber)
        assertThat(responsibleCom).isEqualTo(com)
      }
    }

    @Test
    fun `Populates licence with CRO from delius when provided`() {
      val offender = aProbationCaseResult.copy(croNumber = "ZZZZZ")
      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(
        listOf(
          prisonerSearchResult().copy(
            croNumber = "AAAAA",
          ),
        ),
      )
      whenever(deliusApiClient.getProbationCase(any())).thenReturn(
        offender,
      )

      service.createCrdLicence(PRISON_NUMBER)

      argumentCaptor<CrdLicence>().apply {
        verify(licenceRepository, times(1)).saveAndFlush(capture())
        assertThat(firstValue.cro).isEqualTo(offender.croNumber)
      }
    }

    @Test
    fun `Populates licence with CRO from NOMIS when not provided by delius`() {
      val prisoner = prisonerSearchResult().copy(
        croNumber = "AAAAA",
      )
      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(
        listOf(
          prisoner,
        ),
      )
      whenever(deliusApiClient.getProbationCase(any())).thenReturn(
        aProbationCaseResult.copy(croNumber = null),
      )

      service.createCrdLicence(PRISON_NUMBER)

      argumentCaptor<CrdLicence>().apply {
        verify(licenceRepository, times(1)).saveAndFlush(capture())
        assertThat(firstValue.cro).isEqualTo(prisoner.croNumber)
      }
    }

    @Test
    fun `Populates licence with middlename if provided`() {
      val prisoner = prisonerSearchResult().copy(
        middleNames = "Timothy",
      )
      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(
        listOf(
          prisoner,
        ),
      )
      whenever(deliusApiClient.getProbationCase(any())).thenReturn(
        aProbationCaseResult,
      )

      service.createCrdLicence(PRISON_NUMBER)

      argumentCaptor<CrdLicence>().apply {
        verify(licenceRepository, times(1)).saveAndFlush(capture())
        assertThat(firstValue.middleNames).isEqualTo("Timothy")
      }
    }

    @Test
    fun `Populates licence with default middlename if not provided`() {
      val prisoner = prisonerSearchResult().copy(
        middleNames = null,
      )
      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(
        listOf(
          prisoner,
        ),
      )
      whenever(deliusApiClient.getProbationCase(any())).thenReturn(
        aProbationCaseResult,
      )

      service.createCrdLicence(PRISON_NUMBER)

      argumentCaptor<CrdLicence>().apply {
        verify(licenceRepository, times(1)).saveAndFlush(capture())
        assertThat(firstValue.middleNames).isEqualTo("")
      }
    }

    @Test
    fun `Populates licence with conditions for AP licence`() {
      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(
        listOf(prisonerSearchResult().copy(topupSupervisionExpiryDate = null, licenceExpiryDate = LocalDate.now())),
      )
      whenever(deliusApiClient.getProbationCase(any())).thenReturn(
        aProbationCaseResult,
      )

      service.createCrdLicence(PRISON_NUMBER)

      argumentCaptor<CrdLicence>().apply {
        verify(licenceRepository, times(1)).saveAndFlush(capture())
        assertThat(firstValue.typeCode).isEqualTo(LicenceType.AP)
      }
      argumentCaptor<List<StandardCondition>>().apply {
        verify(standardConditionRepository, times(1)).saveAllAndFlush(capture())
        verifyNoInteractions(additionalConditionRepository)
        val apConditions = firstValue.filter { it.conditionType == "AP" }
        assertThat(apConditions).isNotEmpty()
        val pssConditions = firstValue.filter { it.conditionType == "PSS" }
        assertThat(pssConditions).isEmpty()
      }
    }

    @Test
    fun `Populates licence with conditions for PSS licence`() {
      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(
        listOf(prisonerSearchResult().copy(topupSupervisionExpiryDate = LocalDate.now(), licenceExpiryDate = null)),
      )
      whenever(deliusApiClient.getProbationCase(any())).thenReturn(
        aProbationCaseResult,
      )

      service.createCrdLicence(PRISON_NUMBER)

      argumentCaptor<CrdLicence>().apply {
        verify(licenceRepository, times(1)).saveAndFlush(capture())
        assertThat(firstValue.typeCode).isEqualTo(LicenceType.PSS)
      }
      argumentCaptor<List<StandardCondition>>().apply {
        verify(standardConditionRepository, times(1)).saveAllAndFlush(capture())
        verifyNoInteractions(additionalConditionRepository)
        val apConditions = firstValue.filter { it.conditionType == "AP" }
        assertThat(apConditions).isEmpty()
        val pssConditions = firstValue.filter { it.conditionType == "PSS" }
        assertThat(pssConditions).isNotEmpty()
      }
    }

    @Test
    fun `Populates licence with standard conditions for an AP and PSS licence`() {
      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(
        listOf(
          prisonerSearchResult().copy(
            topupSupervisionExpiryDate = LocalDate.now().plusDays(1),
            licenceExpiryDate = LocalDate.now(),
          ),
        ),
      )
      whenever(deliusApiClient.getProbationCase(any())).thenReturn(
        aProbationCaseResult,
      )

      service.createCrdLicence(PRISON_NUMBER)

      argumentCaptor<CrdLicence>().apply {
        verify(licenceRepository, times(1)).saveAndFlush(capture())
        assertThat(firstValue.typeCode).isEqualTo(LicenceType.AP_PSS)
      }
      argumentCaptor<List<StandardCondition>>().apply {
        verify(standardConditionRepository, times(1)).saveAllAndFlush(capture())
        verifyNoInteractions(additionalConditionRepository)
        val apConditions = firstValue.filter { it.conditionType == "AP" }
        assertThat(apConditions).isNotEmpty()
        val pssConditions = firstValue.filter { it.conditionType == "PSS" }
        assertThat(pssConditions).isNotEmpty()
      }
    }

    @Test
    fun `service audits correctly`() {
      val aPrisonerSearchResult = prisonerSearchResult()
      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(listOf(aPrisonerSearchResult))
      whenever(deliusApiClient.getProbationCase(any())).thenReturn(aProbationCaseResult)

      service.createCrdLicence(PRISON_NUMBER)

      val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)
      val eventCaptor = ArgumentCaptor.forClass(EntityLicenceEvent::class.java)

      verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())
      verify(licenceEventRepository, times(1)).saveAndFlush(eventCaptor.capture())

      assertThat(auditCaptor.value)
        .extracting("licenceId", "username", "fullName", "summary")
        .isEqualTo(
          listOf(
            -1L,
            "tcom",
            "X Y",
            "Licence created for ${aPrisonerSearchResult.firstName} ${aPrisonerSearchResult.lastName}",
          ),
        )

      assertThat(eventCaptor.value)
        .extracting("licenceId", "eventType", "username", "forenames", "surname", "eventDescription")
        .isEqualTo(
          listOf(
            -1L,
            LicenceEventType.CREATED,
            "tcom",
            "X",
            "Y",
            "Licence created for ${aPrisonerSearchResult.firstName} ${aPrisonerSearchResult.lastName}",
          ),
        )
    }

    @Test
    fun `service throws a validation exception if an in progress licence exists for this person`() {
      val existingLicence = TestData.createCrdLicence().copy(statusCode = APPROVED)
      whenever(
        licenceRepository
          .findAllByNomsIdAndStatusCodeIn(
            PRISON_NUMBER,
            listOf(IN_PROGRESS, SUBMITTED, APPROVED, REJECTED),
          ),
      ).thenReturn(listOf(existingLicence))

      val exception = assertThrows<ResourceAlreadyExistsException> {
        service.createCrdLicence(PRISON_NUMBER)
      }

      assertThat(exception)
        .isInstanceOf(ResourceAlreadyExistsException::class.java)
        .withFailMessage("A licence already exists for this person (IN_PROGRESS, SUBMITTED, APPROVED or REJECTED)")

      assertThat(exception.existingResourceId).isEqualTo(existingLicence.id)

      verify(licenceRepository, times(0)).saveAndFlush(any())
      verify(standardConditionRepository, times(0)).saveAllAndFlush(anyList())
      verify(auditEventRepository, times(0)).saveAndFlush(any())
      verify(licenceEventRepository, times(0)).saveAndFlush(any())
    }

    @Test
    fun `service throws a validation exception if an in progress licence exists for this person and chose non approved version`() {
      val approvedLicence = TestData.createCrdLicence().copy(statusCode = APPROVED)
      val notApprovedLicence = TestData.createCrdLicence().copy(statusCode = SUBMITTED)

      whenever(
        licenceRepository
          .findAllByNomsIdAndStatusCodeIn(
            PRISON_NUMBER,
            listOf(IN_PROGRESS, SUBMITTED, APPROVED, REJECTED),
          ),
      ).thenReturn(listOf(approvedLicence, notApprovedLicence))

      val exception = assertThrows<ResourceAlreadyExistsException> {
        service.createCrdLicence(PRISON_NUMBER)
      }

      assertThat(exception)
        .isInstanceOf(ResourceAlreadyExistsException::class.java)
        .withFailMessage("A licence already exists for this person (IN_PROGRESS, SUBMITTED, APPROVED or REJECTED)")

      assertThat(exception.existingResourceId).isEqualTo(notApprovedLicence.id)

      verify(licenceRepository, times(0)).saveAndFlush(any())
      verify(standardConditionRepository, times(0)).saveAllAndFlush(anyList())
      verify(auditEventRepository, times(0)).saveAndFlush(any())
      verify(licenceEventRepository, times(0)).saveAndFlush(any())
    }

    @Test
    fun `service throws an error if no active offender manager found for this person`() {
      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(listOf(prisonerSearchResult()))
      whenever(deliusApiClient.getProbationCase(any())).thenReturn(aProbationCaseResult)
      whenever(prisonApiClient.getPrisonInformation(any())).thenReturn(somePrisonInformation)
      whenever(deliusApiClient.getOffenderManager(any())).thenReturn(null)

      val exception = assertThrows<IllegalStateException> {
        service.createCrdLicence(PRISON_NUMBER)
      }

      assertThat(exception)
        .isInstanceOf(IllegalStateException::class.java)
        .hasMessage("No active offender manager found for NOMSID")

      verify(licenceRepository, times(0)).saveAndFlush(any())
      verify(standardConditionRepository, times(0)).saveAllAndFlush(anyList())
      verify(auditEventRepository, times(0)).saveAndFlush(any())
      verify(licenceEventRepository, times(0)).saveAndFlush(any())
    }

    @Test
    fun `service throws an error if no responsible officer details found for this person`() {
      whenever(deliusApiClient.getOffenderManager(any())).thenReturn(null)
      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(listOf(prisonerSearchResult()))
      whenever(deliusApiClient.getProbationCase(any())).thenReturn(aProbationCaseResult)

      val exception = assertThrows<IllegalStateException> {
        service.createCrdLicence(PRISON_NUMBER)
      }

      assertThat(exception)
        .isInstanceOf(IllegalStateException::class.java)
        .hasMessage("No active offender manager found for NOMSID")

      verify(licenceRepository, times(0)).saveAndFlush(any())
      verify(standardConditionRepository, times(0)).saveAllAndFlush(anyList())
      verify(auditEventRepository, times(0)).saveAndFlush(any())
      verify(licenceEventRepository, times(0)).saveAndFlush(any())
    }

    @Test
    fun `service throws an error if no responsible COM found for this person`() {
      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(listOf(prisonerSearchResult()))
      whenever(deliusApiClient.getProbationCase(any())).thenReturn(aProbationCaseResult)
      whenever(prisonApiClient.getPrisonInformation(any())).thenReturn(somePrisonInformation)
      whenever(deliusApiClient.getOffenderManager(any())).thenReturn(aCommunityManager)
      whenever(staffRepository.findByStaffIdentifier(2000)).thenReturn(null)

      val exception = assertThrows<IllegalStateException> {
        service.createCrdLicence(PRISON_NUMBER)
      }

      assertThat(exception)
        .isInstanceOf(IllegalStateException::class.java)
        .hasMessage("staff with staff identifier: '2000', missing record in delius")

      verify(licenceRepository, times(0)).saveAndFlush(any())
      verify(standardConditionRepository, times(0)).saveAllAndFlush(anyList())
      verify(auditEventRepository, times(0)).saveAndFlush(any())
      verify(licenceEventRepository, times(0)).saveAndFlush(any())
    }

    @Test
    fun `service creates COM if no responsible COM exists in DB for this person`() {
      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(listOf(prisonerSearchResult()))
      whenever(deliusApiClient.getProbationCase(any())).thenReturn(aProbationCaseResult)
      whenever(prisonApiClient.getPrisonInformation(any())).thenReturn(somePrisonInformation)
      whenever(deliusApiClient.getOffenderManager(any())).thenReturn(aCommunityManager)
      whenever(staffRepository.findByStaffIdentifier(2000)).thenReturn(null)
      whenever(deliusApiClient.getStaffByIdentifier(any())).thenReturn(comUser)
      whenever(staffRepository.saveAndFlush(any())).thenReturn(newCom)

      service.createCrdLicence(PRISON_NUMBER)

      argumentCaptor<CommunityOffenderManager>().apply {
        verify(staffRepository, times(1)).saveAndFlush(capture())
        assertThat(firstValue.staffIdentifier).isEqualTo(comUser.id)
        assertThat(firstValue.username).isEqualTo(comUser.username!!.uppercase())
        assertThat(firstValue.email).isEqualTo(comUser.email)
        assertThat(firstValue.firstName).isEqualTo(comUser.name.forename)
        assertThat(firstValue.lastName).isEqualTo(comUser.name.surname)
      }

      argumentCaptor<CrdLicence>().apply {
        verify(licenceRepository, times(1)).saveAndFlush(capture())
        assertThat(firstValue.responsibleCom.id).isEqualTo(newCom.id)
      }
    }

    @Test
    fun `service throws an error if no user found for this person`() {
      val expectedCom = CommunityOffenderManager(
        staffIdentifier = 2000,
        username = "tcom",
        email = "testemail@probation.gov.uk",
        firstName = "X",
        lastName = "Y",
      )

      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(listOf(prisonerSearchResult()))
      whenever(deliusApiClient.getProbationCase(any())).thenReturn(aProbationCaseResult)
      whenever(prisonApiClient.getPrisonInformation(any())).thenReturn(somePrisonInformation)
      whenever(deliusApiClient.getOffenderManager(any())).thenReturn(aCommunityManager)

      whenever(staffRepository.findByStaffIdentifier(2000)).thenReturn(expectedCom)
      whenever(staffRepository.findByUsernameIgnoreCase("tcom")).thenReturn(null)

      val exception = assertThrows<IllegalStateException> {
        service.createCrdLicence(PRISON_NUMBER)
      }

      assertThat(exception)
        .isInstanceOf(IllegalStateException::class.java)
        .hasMessage("Staff with username tcom not found")

      verify(licenceRepository, times(0)).saveAndFlush(any())
      verify(standardConditionRepository, times(0)).saveAllAndFlush(anyList())
      verify(auditEventRepository, times(0)).saveAndFlush(any())
      verify(licenceEventRepository, times(0)).saveAndFlush(any())
    }
  }

  @Nested
  inner class CreatingPrrdLicences {

    @BeforeEach
    fun reset() {
      reset(
        licenceRepository,
        licenceEventRepository,
        auditEventRepository,
        prisonerSearchApiClient,
        prisonApiClient,
        deliusApiClient,
      )
      val authentication = mock<Authentication>()
      val securityContext = mock<SecurityContext>()

      whenever(authentication.name).thenReturn("tcom")
      whenever(securityContext.authentication).thenReturn(authentication)
      SecurityContextHolder.setContext(securityContext)

      whenever(prisonApiClient.getPrisonInformation(any())).thenReturn(somePrisonInformation)

      whenever(staffRepository.findByStaffIdentifier(2000)).thenReturn(com)
      whenever(staffRepository.findByUsernameIgnoreCase("tcom")).thenReturn(com)
      whenever(deliusApiClient.getOffenderManager(any())).thenReturn(aCommunityManager)

      whenever(additionalConditionRepository.saveAllAndFlush(anyList())).thenAnswer { it.arguments[0] }
      whenever(standardConditionRepository.saveAllAndFlush(anyList())).thenAnswer { it.arguments[0] }
      whenever(licenceRepository.saveAndFlush(any())).thenAnswer { it.arguments[0] }
    }

    @Test
    fun `service creates populates licence with expected fields`() {
      val aPrisonerSearchResult = prisonerSearchResult(postRecallReleaseDate = LocalDate.now())
      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(listOf(aPrisonerSearchResult))
      whenever(deliusApiClient.getProbationCase(any())).thenReturn(aProbationCaseResult)
      whenever(releaseDateService.getLicenceStartDate(any(), anyOrNull())).thenReturn(LocalDate.of(2022, 10, 10))

      service.createPrrdLicence(PRISON_NUMBER)

      val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
      verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())

      with(licenceCaptor.value as PrrdLicence) {
        assertThat(kind).isEqualTo(LicenceKind.PRRD)
        assertThat(typeCode).isEqualTo(LicenceType.AP)
        assertThat(version).isEqualTo("2.1")
        assertThat(statusCode).isEqualTo(IN_PROGRESS)
        assertThat(versionOfId).isNull()
        assertThat(licenceVersion).isEqualTo("1.0")
        assertThat(nomsId).isEqualTo(nomsId)
        assertThat(bookingNo).isEqualTo(aPrisonerSearchResult.bookNumber)
        assertThat(bookingId).isEqualTo(aPrisonerSearchResult.bookingId!!.toLong())
        assertThat(prisonCode).isEqualTo(aPrisonerSearchResult.prisonId)
        assertThat(forename).isEqualTo(aPrisonerSearchResult.firstName.convertToTitleCase())
        assertThat(middleNames).isEqualTo(aPrisonerSearchResult.middleNames?.convertToTitleCase() ?: "")
        assertThat(surname).isEqualTo(aPrisonerSearchResult.lastName.convertToTitleCase())
        assertThat(dateOfBirth).isEqualTo(aPrisonerSearchResult.dateOfBirth)
        assertThat(actualReleaseDate).isEqualTo(aPrisonerSearchResult.confirmedReleaseDate)
        assertThat(sentenceStartDate).isEqualTo(aPrisonerSearchResult.sentenceStartDate)
        assertThat(sentenceEndDate).isEqualTo(aPrisonerSearchResult.sentenceExpiryDate)
        assertThat(licenceStartDate).isEqualTo(LocalDate.of(2022, 10, 10))
        assertThat(licenceExpiryDate).isEqualTo(aPrisonerSearchResult.licenceExpiryDate)
        assertThat(topupSupervisionStartDate).isEqualTo(aPrisonerSearchResult.topupSupervisionStartDate)
        assertThat(topupSupervisionExpiryDate).isEqualTo(aPrisonerSearchResult.topupSupervisionExpiryDate)
        assertThat(postRecallReleaseDate).isEqualTo(LocalDate.now())
        assertThat(prisonDescription).isEqualTo(somePrisonInformation.description)
        assertThat(prisonTelephone).isEqualTo(somePrisonInformation.getPrisonContactNumber())
        assertThat(probationAreaCode).isEqualTo(aCommunityManager.provider.code)
        assertThat(probationAreaDescription).isEqualTo(aCommunityManager.provider.description)
        assertThat(probationPduCode).isEqualTo(aCommunityManager.team.borough.code)
        assertThat(probationPduDescription).isEqualTo(aCommunityManager.team.borough.description)
        assertThat(probationLauCode).isEqualTo(aCommunityManager.team.district.code)
        assertThat(probationLauDescription).isEqualTo(aCommunityManager.team.district.description)
        assertThat(probationTeamCode).isEqualTo(aCommunityManager.team.code)
        assertThat(probationTeamDescription).isEqualTo(aCommunityManager.team.description)
        assertThat(crn).isEqualTo(aProbationCaseResult.crn)
        assertThat(pnc).isEqualTo(aProbationCaseResult.pncNumber)
        assertThat(responsibleCom).isEqualTo(com)
      }
    }

    @Test
    fun `Populates licence with CRO from delius when provided`() {
      val offender = aProbationCaseResult.copy(croNumber = "ZZZZZ")
      val aPrisonerSearchResult = prisonerSearchResult(postRecallReleaseDate = LocalDate.now())
      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(
        listOf(
          aPrisonerSearchResult.copy(
            croNumber = "AAAAA",
          ),
        ),
      )

      whenever(deliusApiClient.getProbationCase(any())).thenReturn(
        offender,
      )

      service.createPrrdLicence(PRISON_NUMBER)

      argumentCaptor<PrrdLicence>().apply {
        verify(licenceRepository, times(1)).saveAndFlush(capture())
        assertThat(firstValue.cro).isEqualTo(offender.croNumber)
      }
    }

    @Test
    fun `Populates licence with CRO from NOMIS when not provided by delius`() {
      val prisoner = prisonerSearchResult(postRecallReleaseDate = LocalDate.now()).copy(
        croNumber = "AAAAA",
      )
      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(
        listOf(
          prisoner,
        ),
      )
      whenever(deliusApiClient.getProbationCase(any())).thenReturn(
        aProbationCaseResult.copy(croNumber = null),
      )

      service.createPrrdLicence(PRISON_NUMBER)

      argumentCaptor<PrrdLicence>().apply {
        verify(licenceRepository, times(1)).saveAndFlush(capture())
        assertThat(firstValue.cro).isEqualTo(prisoner.croNumber)
      }
    }

    @Test
    fun `Populates licence with middlename if provided`() {
      val prisoner = prisonerSearchResult(postRecallReleaseDate = LocalDate.now()).copy(
        middleNames = "Timothy",
      )
      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(
        listOf(
          prisoner,
        ),
      )
      whenever(deliusApiClient.getProbationCase(any())).thenReturn(
        aProbationCaseResult,
      )

      service.createPrrdLicence(PRISON_NUMBER)

      argumentCaptor<PrrdLicence>().apply {
        verify(licenceRepository, times(1)).saveAndFlush(capture())
        assertThat(firstValue.middleNames).isEqualTo("Timothy")
      }
    }

    @Test
    fun `Populates licence with default middlename if not provided`() {
      val prisoner = prisonerSearchResult(postRecallReleaseDate = LocalDate.now()).copy(
        middleNames = null,
      )
      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(
        listOf(
          prisoner,
        ),
      )
      whenever(deliusApiClient.getProbationCase(any())).thenReturn(
        aProbationCaseResult,
      )

      service.createPrrdLicence(PRISON_NUMBER)

      argumentCaptor<PrrdLicence>().apply {
        verify(licenceRepository, times(1)).saveAndFlush(capture())
        assertThat(firstValue.middleNames).isEqualTo("")
      }
    }

    @Test
    fun `Populates licence with conditions for AP licence`() {
      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(
        listOf(prisonerSearchResult(postRecallReleaseDate = LocalDate.now()).copy(topupSupervisionExpiryDate = null, licenceExpiryDate = LocalDate.now())),
      )
      whenever(deliusApiClient.getProbationCase(any())).thenReturn(
        aProbationCaseResult,
      )

      service.createPrrdLicence(PRISON_NUMBER)

      argumentCaptor<PrrdLicence>().apply {
        verify(licenceRepository, times(1)).saveAndFlush(capture())
        assertThat(firstValue.typeCode).isEqualTo(LicenceType.AP)
      }
      argumentCaptor<List<StandardCondition>>().apply {
        verify(standardConditionRepository, times(1)).saveAllAndFlush(capture())
        verifyNoInteractions(additionalConditionRepository)
        val apConditions = firstValue.filter { it.conditionType == "AP" }
        assertThat(apConditions).isNotEmpty()
        val pssConditions = firstValue.filter { it.conditionType == "PSS" }
        assertThat(pssConditions).isEmpty()
      }
    }

    @Test
    fun `Populates licence with conditions for PSS licence`() {
      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(
        listOf(prisonerSearchResult(postRecallReleaseDate = LocalDate.now()).copy(topupSupervisionExpiryDate = LocalDate.now(), licenceExpiryDate = null)),
      )
      whenever(deliusApiClient.getProbationCase(any())).thenReturn(
        aProbationCaseResult,
      )

      service.createPrrdLicence(PRISON_NUMBER)

      argumentCaptor<PrrdLicence>().apply {
        verify(licenceRepository, times(1)).saveAndFlush(capture())
        assertThat(firstValue.typeCode).isEqualTo(LicenceType.PSS)
      }
      argumentCaptor<List<StandardCondition>>().apply {
        verify(standardConditionRepository, times(1)).saveAllAndFlush(capture())
        verifyNoInteractions(additionalConditionRepository)
        val apConditions = firstValue.filter { it.conditionType == "AP" }
        assertThat(apConditions).isEmpty()
        val pssConditions = firstValue.filter { it.conditionType == "PSS" }
        assertThat(pssConditions).isNotEmpty()
      }
    }

    @Test
    fun `Populates licence with standard conditions for an AP and PSS licence`() {
      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(
        listOf(
          prisonerSearchResult(postRecallReleaseDate = LocalDate.now()).copy(
            topupSupervisionExpiryDate = LocalDate.now().plusDays(1),
            licenceExpiryDate = LocalDate.now(),
          ),
        ),
      )
      whenever(deliusApiClient.getProbationCase(any())).thenReturn(
        aProbationCaseResult,
      )

      service.createPrrdLicence(PRISON_NUMBER)

      argumentCaptor<PrrdLicence>().apply {
        verify(licenceRepository, times(1)).saveAndFlush(capture())
        assertThat(firstValue.typeCode).isEqualTo(LicenceType.AP_PSS)
      }
      argumentCaptor<List<StandardCondition>>().apply {
        verify(standardConditionRepository, times(1)).saveAllAndFlush(capture())
        verifyNoInteractions(additionalConditionRepository)
        val apConditions = firstValue.filter { it.conditionType == "AP" }
        assertThat(apConditions).isNotEmpty()
        val pssConditions = firstValue.filter { it.conditionType == "PSS" }
        assertThat(pssConditions).isNotEmpty()
      }
    }

    @Test
    fun `service audits correctly`() {
      val aPrisonerSearchResult = prisonerSearchResult(postRecallReleaseDate = LocalDate.now())
      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(listOf(aPrisonerSearchResult))
      whenever(deliusApiClient.getProbationCase(any())).thenReturn(aProbationCaseResult)

      service.createPrrdLicence(PRISON_NUMBER)

      val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)
      val eventCaptor = ArgumentCaptor.forClass(EntityLicenceEvent::class.java)

      verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())
      verify(licenceEventRepository, times(1)).saveAndFlush(eventCaptor.capture())

      assertThat(auditCaptor.value)
        .extracting("licenceId", "username", "fullName", "summary")
        .isEqualTo(
          listOf(
            -1L,
            "tcom",
            "X Y",
            "Licence created for ${aPrisonerSearchResult.firstName} ${aPrisonerSearchResult.lastName}",
          ),
        )

      assertThat(eventCaptor.value)
        .extracting("licenceId", "eventType", "username", "forenames", "surname", "eventDescription")
        .isEqualTo(
          listOf(
            -1L,
            LicenceEventType.CREATED,
            "tcom",
            "X",
            "Y",
            "Licence created for ${aPrisonerSearchResult.firstName} ${aPrisonerSearchResult.lastName}",
          ),
        )
    }

    @Test
    fun `service throws a validation exception if an in progress licence exists for this person`() {
      val existingLicence = TestData.createPrrdLicence().copy(statusCode = APPROVED)
      whenever(
        licenceRepository
          .findAllByNomsIdAndStatusCodeIn(
            PRISON_NUMBER,
            listOf(IN_PROGRESS, SUBMITTED, APPROVED, REJECTED),
          ),
      ).thenReturn(listOf(existingLicence))

      val exception = assertThrows<ResourceAlreadyExistsException> {
        service.createPrrdLicence(PRISON_NUMBER)
      }

      assertThat(exception)
        .isInstanceOf(ResourceAlreadyExistsException::class.java)
        .withFailMessage("A licence already exists for this person (IN_PROGRESS, SUBMITTED, APPROVED or REJECTED)")

      assertThat(exception.existingResourceId).isEqualTo(existingLicence.id)

      verify(licenceRepository, times(0)).saveAndFlush(any())
      verify(standardConditionRepository, times(0)).saveAllAndFlush(anyList())
      verify(auditEventRepository, times(0)).saveAndFlush(any())
      verify(licenceEventRepository, times(0)).saveAndFlush(any())
    }

    @Test
    fun `service throws a validation exception if an in progress licence exists for this person and chose non approved version`() {
      val approvedLicence = TestData.createPrrdLicence().copy(statusCode = APPROVED)
      val notApprovedLicence = TestData.createPrrdLicence().copy(statusCode = SUBMITTED)

      whenever(
        licenceRepository
          .findAllByNomsIdAndStatusCodeIn(
            PRISON_NUMBER,
            listOf(IN_PROGRESS, SUBMITTED, APPROVED, REJECTED),
          ),
      ).thenReturn(listOf(approvedLicence, notApprovedLicence))

      val exception = assertThrows<ResourceAlreadyExistsException> {
        service.createPrrdLicence(PRISON_NUMBER)
      }

      assertThat(exception)
        .isInstanceOf(ResourceAlreadyExistsException::class.java)
        .withFailMessage("A licence already exists for this person (IN_PROGRESS, SUBMITTED, APPROVED or REJECTED)")

      assertThat(exception.existingResourceId).isEqualTo(notApprovedLicence.id)

      verify(licenceRepository, times(0)).saveAndFlush(any())
      verify(standardConditionRepository, times(0)).saveAllAndFlush(anyList())
      verify(auditEventRepository, times(0)).saveAndFlush(any())
      verify(licenceEventRepository, times(0)).saveAndFlush(any())
    }

    @Test
    fun `service throws an error if no active offender manager found for this person`() {
      val aPrisonerSearchResult = prisonerSearchResult(postRecallReleaseDate = LocalDate.now())
      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(listOf(aPrisonerSearchResult))
      whenever(deliusApiClient.getProbationCase(any())).thenReturn(aProbationCaseResult)
      whenever(prisonApiClient.getPrisonInformation(any())).thenReturn(somePrisonInformation)
      whenever(deliusApiClient.getOffenderManager(any())).thenReturn(null)

      val exception = assertThrows<IllegalStateException> {
        service.createPrrdLicence(PRISON_NUMBER)
      }

      assertThat(exception)
        .isInstanceOf(IllegalStateException::class.java)
        .hasMessage("No active offender manager found for NOMSID")

      verify(licenceRepository, times(0)).saveAndFlush(any())
      verify(standardConditionRepository, times(0)).saveAllAndFlush(anyList())
      verify(auditEventRepository, times(0)).saveAndFlush(any())
      verify(licenceEventRepository, times(0)).saveAndFlush(any())
    }

    @Test
    fun `service throws an error if no responsible officer details found for this person`() {
      val aPrisonerSearchResult = prisonerSearchResult(postRecallReleaseDate = LocalDate.now())
      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(listOf(aPrisonerSearchResult))
      whenever(deliusApiClient.getOffenderManager(any())).thenReturn(null)
      whenever(deliusApiClient.getProbationCase(any())).thenReturn(aProbationCaseResult)

      val exception = assertThrows<IllegalStateException> {
        service.createPrrdLicence(PRISON_NUMBER)
      }

      assertThat(exception)
        .isInstanceOf(IllegalStateException::class.java)
        .hasMessage("No active offender manager found for NOMSID")

      verify(licenceRepository, times(0)).saveAndFlush(any())
      verify(standardConditionRepository, times(0)).saveAllAndFlush(anyList())
      verify(auditEventRepository, times(0)).saveAndFlush(any())
      verify(licenceEventRepository, times(0)).saveAndFlush(any())
    }

    @Test
    fun `service throws an error if no responsible COM found for this person`() {
      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(
        listOf(prisonerSearchResult(postRecallReleaseDate = LocalDate.now())),
      )
      whenever(deliusApiClient.getProbationCase(any())).thenReturn(aProbationCaseResult)
      whenever(prisonApiClient.getPrisonInformation(any())).thenReturn(somePrisonInformation)
      whenever(deliusApiClient.getOffenderManager(any())).thenReturn(aCommunityManager)
      whenever(staffRepository.findByStaffIdentifier(2000)).thenReturn(null)

      val exception = assertThrows<IllegalStateException> {
        service.createPrrdLicence(PRISON_NUMBER)
      }

      assertThat(exception)
        .isInstanceOf(IllegalStateException::class.java)
        .hasMessage("staff with staff identifier: '2000', missing record in delius")

      verify(licenceRepository, times(0)).saveAndFlush(any())
      verify(standardConditionRepository, times(0)).saveAllAndFlush(anyList())
      verify(auditEventRepository, times(0)).saveAndFlush(any())
      verify(licenceEventRepository, times(0)).saveAndFlush(any())
    }

    @Test
    fun `service creates COM if no responsible COM exists in DB for this person`() {
      val aPrisonerSearchResult = prisonerSearchResult(postRecallReleaseDate = LocalDate.now())
      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(listOf(aPrisonerSearchResult))
      whenever(deliusApiClient.getProbationCase(any())).thenReturn(aProbationCaseResult)
      whenever(prisonApiClient.getPrisonInformation(any())).thenReturn(somePrisonInformation)
      whenever(deliusApiClient.getOffenderManager(any())).thenReturn(aCommunityManager)
      whenever(staffRepository.findByStaffIdentifier(2000)).thenReturn(null)
      whenever(deliusApiClient.getStaffByIdentifier(any())).thenReturn(comUser)
      whenever(staffRepository.saveAndFlush(any())).thenReturn(newCom)

      service.createPrrdLicence(PRISON_NUMBER)

      argumentCaptor<CommunityOffenderManager>().apply {
        verify(staffRepository, times(1)).saveAndFlush(capture())
        assertThat(firstValue.staffIdentifier).isEqualTo(comUser.id)
        assertThat(firstValue.username).isEqualTo(comUser.username!!.uppercase())
        assertThat(firstValue.email).isEqualTo(comUser.email)
        assertThat(firstValue.firstName).isEqualTo(comUser.name.forename)
        assertThat(firstValue.lastName).isEqualTo(comUser.name.surname)
      }

      argumentCaptor<PrrdLicence>().apply {
        verify(licenceRepository, times(1)).saveAndFlush(capture())
        assertThat(firstValue.responsibleCom.id).isEqualTo(newCom.id)
      }
    }

    @Test
    fun `service throws an error if no user found for this person`() {
      val expectedCom = CommunityOffenderManager(
        staffIdentifier = 2000,
        username = "tcom",
        email = "testemail@probation.gov.uk",
        firstName = "X",
        lastName = "Y",
      )

      val aPrisonerSearchResult = prisonerSearchResult(postRecallReleaseDate = LocalDate.now())
      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(listOf(aPrisonerSearchResult))
      whenever(deliusApiClient.getProbationCase(any())).thenReturn(aProbationCaseResult)
      whenever(prisonApiClient.getPrisonInformation(any())).thenReturn(somePrisonInformation)
      whenever(deliusApiClient.getOffenderManager(any())).thenReturn(aCommunityManager)

      whenever(staffRepository.findByStaffIdentifier(2000)).thenReturn(expectedCom)
      whenever(staffRepository.findByUsernameIgnoreCase("tcom")).thenReturn(null)

      val exception = assertThrows<IllegalStateException> {
        service.createPrrdLicence(PRISON_NUMBER)
      }

      assertThat(exception)
        .isInstanceOf(IllegalStateException::class.java)
        .hasMessage("Staff with username tcom not found")

      verify(licenceRepository, times(0)).saveAndFlush(any())
      verify(standardConditionRepository, times(0)).saveAllAndFlush(anyList())
      verify(auditEventRepository, times(0)).saveAndFlush(any())
      verify(licenceEventRepository, times(0)).saveAndFlush(any())
    }
  }

  @Nested
  inner class CreatingHardStopLicences {

    @BeforeEach
    fun reset() {
      reset(
        licenceRepository,
        licenceEventRepository,
        auditEventRepository,
        prisonerSearchApiClient,
        prisonApiClient,
        deliusApiClient,
      )
      val authentication = mock<Authentication>()
      val securityContext = mock<SecurityContext>()

      whenever(authentication.name).thenReturn(prisonUser.username)
      whenever(securityContext.authentication).thenReturn(authentication)
      SecurityContextHolder.setContext(securityContext)

      whenever(prisonApiClient.getPrisonInformation(any())).thenReturn(somePrisonInformation)

      whenever(staffRepository.findByStaffIdentifier(2000)).thenReturn(com)
      whenever(staffRepository.findByUsernameIgnoreCase(prisonUser.username)).thenReturn(prisonUser)
      whenever(deliusApiClient.getOffenderManager(any())).thenReturn(aCommunityManager)

      whenever(standardConditionRepository.saveAllAndFlush(anyList())).thenAnswer { it.arguments[0] }
      whenever(licenceRepository.saveAndFlush(any())).thenAnswer { it.arguments[0] }
    }

    @Test
    fun `service creates populates licence with expected fields`() {
      val aPrisonerSearchResult = prisonerSearchResult()
      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(listOf(aPrisonerSearchResult))
      whenever(deliusApiClient.getProbationCase(any())).thenReturn(aProbationCaseResult)
      whenever(releaseDateService.getLicenceStartDate(any(), anyOrNull())).thenReturn(LocalDate.of(2022, 10, 10))

      service.createHardStopLicence(PRISON_NUMBER)

      val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
      verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())

      with(licenceCaptor.value as HardStopLicence) {
        assertThat(kind).isEqualTo(LicenceKind.HARD_STOP)
        assertThat(typeCode).isEqualTo(LicenceType.AP)
        assertThat(version).isEqualTo("2.1")
        assertThat(statusCode).isEqualTo(IN_PROGRESS)
        assertThat(licenceVersion).isEqualTo("1.0")
        assertThat(nomsId).isEqualTo(nomsId)
        assertThat(bookingNo).isEqualTo(aPrisonerSearchResult.bookNumber)
        assertThat(bookingId).isEqualTo(aPrisonerSearchResult.bookingId!!.toLong())
        assertThat(prisonCode).isEqualTo(aPrisonerSearchResult.prisonId)
        assertThat(forename).isEqualTo(aPrisonerSearchResult.firstName.convertToTitleCase())
        assertThat(middleNames).isEqualTo(aPrisonerSearchResult.middleNames?.convertToTitleCase() ?: "")
        assertThat(surname).isEqualTo(aPrisonerSearchResult.lastName.convertToTitleCase())
        assertThat(dateOfBirth).isEqualTo(aPrisonerSearchResult.dateOfBirth)
        assertThat(actualReleaseDate).isEqualTo(aPrisonerSearchResult.confirmedReleaseDate)
        assertThat(sentenceStartDate).isEqualTo(aPrisonerSearchResult.sentenceStartDate)
        assertThat(sentenceEndDate).isEqualTo(aPrisonerSearchResult.sentenceExpiryDate)
        assertThat(licenceStartDate).isEqualTo(LocalDate.of(2022, 10, 10))
        assertThat(licenceExpiryDate).isEqualTo(aPrisonerSearchResult.licenceExpiryDate)
        assertThat(topupSupervisionStartDate).isEqualTo(aPrisonerSearchResult.topupSupervisionStartDate)
        assertThat(topupSupervisionExpiryDate).isEqualTo(aPrisonerSearchResult.topupSupervisionExpiryDate)
        assertThat(prisonDescription).isEqualTo(somePrisonInformation.description)
        assertThat(prisonTelephone).isEqualTo(somePrisonInformation.getPrisonContactNumber())
        assertThat(probationAreaCode).isEqualTo(aCommunityManager.provider.code)
        assertThat(probationAreaDescription).isEqualTo(aCommunityManager.provider.description)
        assertThat(probationPduCode).isEqualTo(aCommunityManager.team.borough.code)
        assertThat(probationPduDescription).isEqualTo(aCommunityManager.team.borough.description)
        assertThat(probationLauCode).isEqualTo(aCommunityManager.team.district.code)
        assertThat(probationLauDescription).isEqualTo(aCommunityManager.team.district.description)
        assertThat(probationTeamCode).isEqualTo(aCommunityManager.team.code)
        assertThat(probationTeamDescription).isEqualTo(aCommunityManager.team.description)
        assertThat(crn).isEqualTo(aProbationCaseResult.crn)
        assertThat(pnc).isEqualTo(aProbationCaseResult.pncNumber)
        assertThat(responsibleCom).isEqualTo(com)
      }
    }

    @Test
    fun `Populates licence with PPRD date when PRRD is present`() {
      val prisoner = prisonerSearchResult().copy(
        postRecallReleaseDate = LocalDate.now().plusDays(1),
      )
      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(
        listOf(prisoner),
      )
      whenever(deliusApiClient.getProbationCase(any())).thenReturn(aProbationCaseResult)

      service.createHardStopLicence(PRISON_NUMBER)

      argumentCaptor<HardStopLicence>().apply {
        verify(licenceRepository, times(1)).saveAndFlush(capture())
        assertThat(firstValue.postRecallReleaseDate).isEqualTo(prisoner.postRecallReleaseDate)
      }
    }

    @Test
    fun `Populates licence with CRO from delius when provided`() {
      val offender = aProbationCaseResult.copy(croNumber = "ZZZZZ")
      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(
        listOf(
          prisonerSearchResult().copy(
            croNumber = "AAAAA",
          ),
        ),
      )
      whenever(deliusApiClient.getProbationCase(any())).thenReturn(
        offender,
      )

      service.createHardStopLicence(PRISON_NUMBER)

      argumentCaptor<HardStopLicence>().apply {
        verify(licenceRepository, times(1)).saveAndFlush(capture())
        assertThat(firstValue.cro).isEqualTo(offender.croNumber)
      }
    }

    @Test
    fun `Populates licence with CRO from NOMIS when not provided by delius`() {
      val prisoner = prisonerSearchResult().copy(
        croNumber = "AAAAA",
      )
      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(
        listOf(
          prisoner,
        ),
      )
      whenever(deliusApiClient.getProbationCase(any())).thenReturn(aProbationCaseResult.copy(croNumber = null))

      service.createHardStopLicence(PRISON_NUMBER)

      argumentCaptor<HardStopLicence>().apply {
        verify(licenceRepository, times(1)).saveAndFlush(capture())
        assertThat(firstValue.cro).isEqualTo(prisoner.croNumber)
      }
    }

    @Test
    fun `Populates licence with middlename if provided`() {
      val prisoner = prisonerSearchResult().copy(
        middleNames = "Timothy",
      )
      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(
        listOf(
          prisoner,
        ),
      )
      whenever(deliusApiClient.getProbationCase(any())).thenReturn(
        aProbationCaseResult,
      )

      service.createHardStopLicence(PRISON_NUMBER)

      argumentCaptor<HardStopLicence>().apply {
        verify(licenceRepository, times(1)).saveAndFlush(capture())
        assertThat(firstValue.middleNames).isEqualTo("Timothy")
      }
    }

    @Test
    fun `Populates licence with default middlename if not provided`() {
      val prisoner = prisonerSearchResult().copy(
        middleNames = null,
      )
      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(
        listOf(
          prisoner,
        ),
      )
      whenever(deliusApiClient.getProbationCase(any())).thenReturn(
        aProbationCaseResult,
      )

      service.createHardStopLicence(PRISON_NUMBER)

      argumentCaptor<HardStopLicence>().apply {
        verify(licenceRepository, times(1)).saveAndFlush(capture())
        assertThat(firstValue.middleNames).isEqualTo("")
      }
    }

    @Test
    fun `Populates licence with previous licence if one exists`() {
      val aPrisonerSearchResult = prisonerSearchResult()
      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(
        listOf(
          aPrisonerSearchResult,
        ),
      )
      whenever(deliusApiClient.getProbationCase(any())).thenReturn(aProbationCaseResult)

      val previousLicence = TestData.createCrdLicence().copy(id = 1234L)

      whenever(
        crdLicenceRepository.findAllByBookingIdInAndStatusCodeOrderByDateCreatedDesc(
          listOf(aPrisonerSearchResult.bookingId!!.toLong()),
          TIMED_OUT,
        ),
      ).thenReturn(listOf(previousLicence))

      service.createHardStopLicence(PRISON_NUMBER)

      argumentCaptor<HardStopLicence>().apply {
        verify(licenceRepository, times(1)).saveAndFlush(capture())
        assertThat(firstValue.substituteOfId).isEqualTo(previousLicence.id)
      }
    }

    @Test
    fun `Populates licence with conditions for AP licence`() {
      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(
        listOf(prisonerSearchResult().copy(topupSupervisionExpiryDate = null, licenceExpiryDate = LocalDate.now())),
      )
      whenever(deliusApiClient.getProbationCase(any())).thenReturn(
        aProbationCaseResult,
      )

      service.createHardStopLicence(PRISON_NUMBER)

      argumentCaptor<HardStopLicence>().apply {
        verify(licenceRepository, times(1)).saveAndFlush(capture())
        assertThat(firstValue.typeCode).isEqualTo(LicenceType.AP)
      }
      argumentCaptor<List<StandardCondition>>().apply {
        verify(standardConditionRepository, times(1)).saveAllAndFlush(capture())
        val apConditions = firstValue.filter { it.conditionType == "AP" }
        assertThat(apConditions).isNotEmpty()
        val pssConditions = firstValue.filter { it.conditionType == "PSS" }
        assertThat(pssConditions).isEmpty()
      }

      argumentCaptor<List<AdditionalCondition>>().apply {
        verify(additionalConditionRepository, times(1)).saveAllAndFlush(capture())
        val apConditions = firstValue.filter { it.conditionType == "AP" }
        assertThat(apConditions).isNotEmpty()
        assertThat(apConditions.first().conditionCode).isEqualTo(HARD_STOP_CONDITION.code)

        val pssConditions = firstValue.filter { it.conditionType == "PSS" }
        assertThat(pssConditions).isEmpty()
      }
    }

    @Test
    fun `Populates licence with standard conditions for PSS licence`() {
      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(
        listOf(prisonerSearchResult().copy(topupSupervisionExpiryDate = LocalDate.now(), licenceExpiryDate = null)),
      )
      whenever(deliusApiClient.getProbationCase(any())).thenReturn(
        aProbationCaseResult,
      )

      service.createHardStopLicence(PRISON_NUMBER)

      argumentCaptor<HardStopLicence>().apply {
        verify(licenceRepository, times(1)).saveAndFlush(capture())
        assertThat(firstValue.typeCode).isEqualTo(LicenceType.PSS)
      }
      argumentCaptor<List<StandardCondition>>().apply {
        verify(standardConditionRepository, times(1)).saveAllAndFlush(capture())
        val apConditions = firstValue.filter { it.conditionType == "AP" }
        assertThat(apConditions).isEmpty()
        val pssConditions = firstValue.filter { it.conditionType == "PSS" }
        assertThat(pssConditions).isNotEmpty()
      }

      verify(additionalConditionRepository).saveAllAndFlush(emptyList())
    }

    @Test
    fun `Populates licence with conditions for an AP and PSS licence`() {
      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(
        listOf(
          prisonerSearchResult().copy(
            topupSupervisionExpiryDate = LocalDate.now().plusDays(1),
            licenceExpiryDate = LocalDate.now(),
          ),
        ),
      )
      whenever(deliusApiClient.getProbationCase(any())).thenReturn(
        aProbationCaseResult,
      )

      service.createHardStopLicence(PRISON_NUMBER)

      argumentCaptor<HardStopLicence>().apply {
        verify(licenceRepository, times(1)).saveAndFlush(capture())
        assertThat(firstValue.typeCode).isEqualTo(LicenceType.AP_PSS)
      }
      argumentCaptor<List<StandardCondition>>().apply {
        verify(standardConditionRepository, times(1)).saveAllAndFlush(capture())
        val apConditions = firstValue.filter { it.conditionType == "AP" }
        assertThat(apConditions).isNotEmpty()
        val pssConditions = firstValue.filter { it.conditionType == "PSS" }
        assertThat(pssConditions).isNotEmpty()
      }
      argumentCaptor<List<AdditionalCondition>>().apply {
        verify(additionalConditionRepository, times(1)).saveAllAndFlush(capture())
        val apConditions = firstValue.filter { it.conditionType == "AP" }
        assertThat(apConditions).isNotEmpty()
        assertThat(apConditions.first().conditionCode).isEqualTo(HARD_STOP_CONDITION.code)

        val pssConditions = firstValue.filter { it.conditionType == "PSS" }
        assertThat(pssConditions).isEmpty()
      }
    }

    @Test
    fun `service audits correctly`() {
      val aPrisonerSearchResult = prisonerSearchResult()
      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(listOf(aPrisonerSearchResult))
      whenever(deliusApiClient.getProbationCase(any())).thenReturn(aProbationCaseResult)

      service.createHardStopLicence(PRISON_NUMBER)

      val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)
      val eventCaptor = ArgumentCaptor.forClass(EntityLicenceEvent::class.java)

      verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())
      verify(licenceEventRepository, times(1)).saveAndFlush(eventCaptor.capture())

      assertThat(auditCaptor.value)
        .extracting("licenceId", "username", "fullName", "summary")
        .isEqualTo(
          listOf(
            -1L,
            "ca",
            "A B",
            "Licence created for ${aPrisonerSearchResult.firstName} ${aPrisonerSearchResult.lastName}",
          ),
        )

      assertThat(eventCaptor.value)
        .extracting("licenceId", "eventType", "username", "forenames", "surname", "eventDescription")
        .isEqualTo(
          listOf(
            -1L,
            LicenceEventType.HARD_STOP_CREATED,
            "ca",
            "A",
            "B",
            "Licence created for ${aPrisonerSearchResult.firstName} ${aPrisonerSearchResult.lastName}",
          ),
        )
    }

    @Test
    fun `service throws a validation exception if an in progress licence exists for this person`() {
      val existingLicence = TestData.createCrdLicence()

      whenever(
        licenceRepository
          .findAllByNomsIdAndStatusCodeIn(
            PRISON_NUMBER,
            listOf(IN_PROGRESS, SUBMITTED, APPROVED, REJECTED),
          ),
      ).thenReturn(listOf(existingLicence))

      val exception = assertThrows<ResourceAlreadyExistsException> {
        service.createHardStopLicence(PRISON_NUMBER)
      }

      assertThat(exception)
        .isInstanceOf(ResourceAlreadyExistsException::class.java)
        .withFailMessage("A licence already exists for this person (IN_PROGRESS, SUBMITTED, APPROVED or REJECTED)")
      assertThat(exception.existingResourceId).isEqualTo(existingLicence.id)

      verify(licenceRepository, times(0)).saveAndFlush(any())
      verify(standardConditionRepository, times(0)).saveAllAndFlush(anyList())
      verify(auditEventRepository, times(0)).saveAndFlush(any())
      verify(licenceEventRepository, times(0)).saveAndFlush(any())
    }

    @Test
    fun `service throws an error if no active offender manager found for this person`() {
      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(listOf(prisonerSearchResult()))
      whenever(deliusApiClient.getProbationCase(any())).thenReturn(aProbationCaseResult)
      whenever(prisonApiClient.getPrisonInformation(any())).thenReturn(somePrisonInformation)
      whenever(deliusApiClient.getOffenderManager(any())).thenReturn(null)

      val exception = assertThrows<IllegalStateException> {
        service.createHardStopLicence(PRISON_NUMBER)
      }

      assertThat(exception)
        .isInstanceOf(IllegalStateException::class.java)
        .hasMessage("No active offender manager found for NOMSID")

      verify(licenceRepository, times(0)).saveAndFlush(any())
      verify(standardConditionRepository, times(0)).saveAllAndFlush(anyList())
      verify(auditEventRepository, times(0)).saveAndFlush(any())
      verify(licenceEventRepository, times(0)).saveAndFlush(any())
    }

    @Test
    fun `service throws an error if no responsible officer details found for this person`() {
      whenever(deliusApiClient.getOffenderManager(any())).thenReturn(null)
      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(listOf(prisonerSearchResult()))
      whenever(deliusApiClient.getProbationCase(any())).thenReturn(aProbationCaseResult)

      val exception = assertThrows<IllegalStateException> {
        service.createHardStopLicence(PRISON_NUMBER)
      }

      assertThat(exception)
        .isInstanceOf(IllegalStateException::class.java)
        .hasMessage("No active offender manager found for NOMSID")

      verify(licenceRepository, times(0)).saveAndFlush(any())
      verify(standardConditionRepository, times(0)).saveAllAndFlush(anyList())
      verify(auditEventRepository, times(0)).saveAndFlush(any())
      verify(licenceEventRepository, times(0)).saveAndFlush(any())
    }

    @Test
    fun `service throws an error if no responsible COM found for this person`() {
      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(listOf(prisonerSearchResult()))
      whenever(deliusApiClient.getProbationCase(any())).thenReturn(aProbationCaseResult)
      whenever(prisonApiClient.getPrisonInformation(any())).thenReturn(somePrisonInformation)
      whenever(deliusApiClient.getOffenderManager(any())).thenReturn(aCommunityManager)
      whenever(staffRepository.findByStaffIdentifier(2000)).thenReturn(null)

      val exception = assertThrows<IllegalStateException> {
        service.createHardStopLicence(PRISON_NUMBER)
      }

      assertThat(exception)
        .isInstanceOf(IllegalStateException::class.java)
        .hasMessage("staff with staff identifier: '2000', missing record in delius")

      verify(licenceRepository, times(0)).saveAndFlush(any())
      verify(standardConditionRepository, times(0)).saveAllAndFlush(anyList())
      verify(auditEventRepository, times(0)).saveAndFlush(any())
      verify(licenceEventRepository, times(0)).saveAndFlush(any())
    }

    @Test
    fun `service creates COM if no responsible COM exists in DB for this person`() {
      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(listOf(prisonerSearchResult()))
      whenever(deliusApiClient.getProbationCase(any())).thenReturn(aProbationCaseResult)
      whenever(prisonApiClient.getPrisonInformation(any())).thenReturn(somePrisonInformation)
      whenever(deliusApiClient.getOffenderManager(any())).thenReturn(aCommunityManager)
      whenever(staffRepository.findByStaffIdentifier(2000)).thenReturn(null)
      whenever(deliusApiClient.getStaffByIdentifier(any())).thenReturn(comUser)
      whenever(staffRepository.saveAndFlush(any())).thenReturn(newCom)

      service.createHardStopLicence(PRISON_NUMBER)

      argumentCaptor<CommunityOffenderManager>().apply {
        verify(staffRepository, times(1)).saveAndFlush(capture())
        assertThat(firstValue.staffIdentifier).isEqualTo(comUser.id)
        assertThat(firstValue.username).isEqualTo(comUser.username!!.uppercase())
        assertThat(firstValue.email).isEqualTo(comUser.email)
        assertThat(firstValue.firstName).isEqualTo(comUser.name.forename)
        assertThat(firstValue.lastName).isEqualTo(comUser.name.surname)
      }

      argumentCaptor<HardStopLicence>().apply {
        verify(licenceRepository, times(1)).saveAndFlush(capture())
        assertThat(firstValue.responsibleCom.id).isEqualTo(newCom.id)
      }
    }

    @Test
    fun `service throws an error if no user found for this person`() {
      val expectedCom = CommunityOffenderManager(
        staffIdentifier = 2000,
        username = "tcom",
        email = "testemail@probation.gov.uk",
        firstName = "X",
        lastName = "Y",
      )

      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(listOf(prisonerSearchResult()))
      whenever(deliusApiClient.getProbationCase(any())).thenReturn(aProbationCaseResult)
      whenever(prisonApiClient.getPrisonInformation(any())).thenReturn(somePrisonInformation)
      whenever(deliusApiClient.getOffenderManager(any())).thenReturn(aCommunityManager)

      whenever(staffRepository.findByStaffIdentifier(2000)).thenReturn(expectedCom)
      whenever(staffRepository.findByUsernameIgnoreCase(prisonUser.username)).thenReturn(null)

      val exception = assertThrows<IllegalStateException> {
        service.createHardStopLicence(PRISON_NUMBER)
      }

      assertThat(exception)
        .isInstanceOf(IllegalStateException::class.java)
        .hasMessage("Staff with username ca not found")

      verify(licenceRepository, times(0)).saveAndFlush(any())
      verify(standardConditionRepository, times(0)).saveAllAndFlush(anyList())
      verify(auditEventRepository, times(0)).saveAndFlush(any())
      verify(licenceEventRepository, times(0)).saveAndFlush(any())
    }
  }

  @Nested
  inner class CreatingHdcLicences {
    @BeforeEach
    fun reset() {
      reset(
        licenceRepository,
        licenceEventRepository,
        auditEventRepository,
        hdcCurfewAddressRepository,
        prisonerSearchApiClient,
        prisonApiClient,
        deliusApiClient,
      )
      val authentication = mock<Authentication>()
      val securityContext = mock<SecurityContext>()

      whenever(authentication.name).thenReturn("tcom")
      whenever(securityContext.authentication).thenReturn(authentication)
      SecurityContextHolder.setContext(securityContext)

      whenever(prisonApiClient.getPrisonInformation(any())).thenReturn(somePrisonInformation)

      whenever(staffRepository.findByStaffIdentifier(2000)).thenReturn(com)
      whenever(staffRepository.findByUsernameIgnoreCase("tcom")).thenReturn(com)
      whenever(deliusApiClient.getOffenderManager(any())).thenReturn(aCommunityManager)

      whenever(additionalConditionRepository.saveAllAndFlush(anyList())).thenAnswer { it.arguments[0] }
      whenever(standardConditionRepository.saveAllAndFlush(anyList())).thenAnswer { it.arguments[0] }
      whenever(hdcCurfewAddressRepository.saveAndFlush(any())).thenAnswer { it.arguments[0] }
      whenever(licenceRepository.saveAndFlush(any())).thenAnswer { it.arguments[0] }
    }

    @Test
    fun `service populates licence with expected fields`() {
      val aPrisonerSearchResult = prisonerSearchResult().copy(
        homeDetentionCurfewActualDate = LocalDate.of(2020, 10, 22),
        homeDetentionCurfewEndDate = LocalDate.of(2020, 10, 23),
        homeDetentionCurfewEligibilityDate = LocalDate.of(2020, 10, 22),
      )

      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(listOf(aPrisonerSearchResult))
      whenever(deliusApiClient.getProbationCase(any())).thenReturn(aProbationCaseResult)
      whenever(hdcService.getHdcLicenceDataByBookingId(any())).thenReturn(someHdcLicenceData)
      whenever(hdcService.getHdcLicenceData(any())).thenReturn(someHdcLicenceData)

      service.createHdcLicence(PRISON_NUMBER)

      val hdcCurfewAddressCaptor = ArgumentCaptor.forClass(EntityHdcCurfewAddress::class.java)
      val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
      verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
      verify(standardConditionRepository, times(1)).saveAllAndFlush(anyList())
      verify(auditEventRepository, times(1)).saveAndFlush(any())
      verify(licenceEventRepository, times(1)).saveAndFlush(any())
      verify(hdcCurfewAddressRepository, times(1)).saveAndFlush(hdcCurfewAddressCaptor.capture())

      with(licenceCaptor.value as HdcLicence) {
        assertThat(kind).isEqualTo(LicenceKind.HDC)
        assertThat(typeCode).isEqualTo(LicenceType.AP)
        assertThat(version).isEqualTo("2.1")
        assertThat(statusCode).isEqualTo(IN_PROGRESS)
        assertThat(versionOfId).isNull()
        assertThat(licenceVersion).isEqualTo("1.0")
        assertThat(nomsId).isEqualTo(nomsId)
        assertThat(bookingNo).isEqualTo(aPrisonerSearchResult.bookNumber)
        assertThat(bookingId).isEqualTo(aPrisonerSearchResult.bookingId!!.toLong())
        assertThat(prisonCode).isEqualTo(aPrisonerSearchResult.prisonId)
        assertThat(forename).isEqualTo(aPrisonerSearchResult.firstName.convertToTitleCase())
        assertThat(middleNames).isEqualTo(aPrisonerSearchResult.middleNames?.convertToTitleCase() ?: "")
        assertThat(surname).isEqualTo(aPrisonerSearchResult.lastName.convertToTitleCase())
        assertThat(dateOfBirth).isEqualTo(aPrisonerSearchResult.dateOfBirth)
        assertThat(actualReleaseDate).isEqualTo(aPrisonerSearchResult.confirmedReleaseDate)
        assertThat(sentenceStartDate).isEqualTo(aPrisonerSearchResult.sentenceStartDate)
        assertThat(sentenceEndDate).isEqualTo(aPrisonerSearchResult.sentenceExpiryDate)
        assertThat(licenceExpiryDate).isEqualTo(aPrisonerSearchResult.licenceExpiryDate)
        assertThat(homeDetentionCurfewActualDate).isEqualTo(aPrisonerSearchResult.homeDetentionCurfewActualDate)
        assertThat(homeDetentionCurfewEndDate).isEqualTo(aPrisonerSearchResult.homeDetentionCurfewEndDate)
        assertThat(topupSupervisionStartDate).isEqualTo(aPrisonerSearchResult.topupSupervisionStartDate)
        assertThat(topupSupervisionExpiryDate).isEqualTo(aPrisonerSearchResult.topupSupervisionExpiryDate)
        assertThat(postRecallReleaseDate).isNull()
        assertThat(prisonDescription).isEqualTo(somePrisonInformation.description)
        assertThat(prisonTelephone).isEqualTo(somePrisonInformation.getPrisonContactNumber())
        assertThat(probationAreaCode).isEqualTo(aCommunityManager.provider.code)
        assertThat(probationAreaDescription).isEqualTo(aCommunityManager.provider.description)
        assertThat(probationPduCode).isEqualTo(aCommunityManager.team.borough.code)
        assertThat(probationPduDescription).isEqualTo(aCommunityManager.team.borough.description)
        assertThat(probationLauCode).isEqualTo(aCommunityManager.team.district.code)
        assertThat(probationLauDescription).isEqualTo(aCommunityManager.team.district.description)
        assertThat(probationTeamCode).isEqualTo(aCommunityManager.team.code)
        assertThat(probationTeamDescription).isEqualTo(aCommunityManager.team.description)
        assertThat(crn).isEqualTo(aProbationCaseResult.crn)
        assertThat(pnc).isEqualTo(aProbationCaseResult.pncNumber)
        assertThat(responsibleCom).isEqualTo(com)
      }
    }

    @Test
    fun `service throws an error if HDC licence type is PSS`() {
      val aPrisonerSearchResult = prisonerSearchResult().copy(
        homeDetentionCurfewActualDate = LocalDate.of(2020, 10, 22),
        licenceExpiryDate = null,
        homeDetentionCurfewEligibilityDate = LocalDate.of(2020, 10, 22),
      )

      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(listOf(aPrisonerSearchResult))
      whenever(deliusApiClient.getProbationCase(any())).thenReturn(aProbationCaseResult)

      val exception = assertThrows<IllegalStateException> {
        service.createHdcLicence(PRISON_NUMBER)
      }

      assertThat(exception).isInstanceOf(IllegalStateException::class.java)
        .hasMessage("HDC Licence for A1234AA can not be of type PSS")

      verify(licenceRepository, times(0)).saveAndFlush(any())
      verify(standardConditionRepository, times(0)).saveAllAndFlush(anyList())
      verify(auditEventRepository, times(0)).saveAndFlush(any())
      verify(licenceEventRepository, times(0)).saveAndFlush(any())
      verify(hdcCurfewAddressRepository, times(0)).saveAndFlush(any())
    }

    @Test
    fun `service throws an error if person is not eligible for a HDC licence`() {
      val aPrisonerSearchResult = prisonerSearchResult().copy(
        homeDetentionCurfewActualDate = LocalDate.of(2020, 10, 22),
        homeDetentionCurfewEndDate = LocalDate.of(2020, 10, 23),
        homeDetentionCurfewEligibilityDate = LocalDate.of(2020, 10, 22),
      )

      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(listOf(aPrisonerSearchResult))
      whenever(deliusApiClient.getProbationCase(any())).thenReturn(aProbationCaseResult)
      whenever(hdcService.getHdcLicenceDataByBookingId(any())).thenReturn(
        someHdcLicenceData.copy(
          curfewAddress = null,
        ),
      )
      whenever(
        hdcService.checkEligibleForHdcLicence(
          aPrisonerSearchResult,
          someHdcLicenceData.copy(
            curfewAddress = null,
          ),
        ),
      ).thenThrow(IllegalStateException("HDC licence for ${aPrisonerSearchResult.prisonerNumber} could not be created as there is no curfew address"))

      val exception = assertThrows<IllegalStateException> {
        service.createHdcLicence(PRISON_NUMBER)
      }

      assertThat(exception).isInstanceOf(IllegalStateException::class.java)
        .hasMessage("HDC licence for A1234AA could not be created as there is no curfew address")

      verify(licenceRepository, times(0)).saveAndFlush(any())
      verify(standardConditionRepository, times(0)).saveAllAndFlush(anyList())
      verify(auditEventRepository, times(0)).saveAndFlush(any())
      verify(licenceEventRepository, times(0)).saveAndFlush(any())
      verify(hdcCurfewAddressRepository, times(0)).saveAndFlush(any())
    }
  }

  @Test
  fun `should determine a licence with PRRD in future and after CRD in the past as a recall licence`() {
    val prisoner =
      prisonerSearchResult(
        conditionalReleaseDate = LocalDate.now().minusDays(1),
        postRecallReleaseDate = LocalDate.now().plusDays(1),
      )

    val licenceKind = service.determineLicenceKind(prisoner)

    assertThat(licenceKind).isEqualTo(LicenceKind.PRRD)
  }

  @Test
  fun `should determine a licence with PRRD in future and before CRD as a CRD licence`() {
    val prisoner =
      prisonerSearchResult(
        postRecallReleaseDate = LocalDate.now().plusDays(1),
        conditionalReleaseDate = LocalDate.now().plusDays(2),
      )
    whenever(releaseDateService.getHardStopDate(any())).thenReturn(null)

    val licenceKind = service.determineLicenceKind(prisoner)

    assertThat(licenceKind).isEqualTo(LicenceKind.CRD)
  }

  @Test
  fun `should determine a licence with null PRRD and a hard stop date of today as a hard stop licence`() {
    val prisoner = prisonerSearchResult(conditionalReleaseDate = LocalDate.now().plusDays(2))
    whenever(releaseDateService.getHardStopDate(any())).thenReturn(LocalDate.now())

    val licenceKind = service.determineLicenceKind(prisoner)

    assertThat(licenceKind).isEqualTo(LicenceKind.HARD_STOP)
  }

  @Test
  fun `should determine a licence with null PRRD and hard stop date in future as a CRD licence`() {
    val prisoner = prisonerSearchResult(conditionalReleaseDate = LocalDate.now().plusDays(2))
    whenever(releaseDateService.getHardStopDate(any())).thenReturn(null)

    val licenceKind = service.determineLicenceKind(prisoner)

    assertThat(licenceKind).isEqualTo(LicenceKind.CRD)
  }

  @Test
  fun `should determine a licence with a future PRRD and a null CRD as a PRRD licence`() {
    val prisoner =
      prisonerSearchResult(conditionalReleaseDate = null, postRecallReleaseDate = LocalDate.now().plusDays(2))
    whenever(releaseDateService.getHardStopDate(any())).thenReturn(null)

    val licenceKind = service.determineLicenceKind(prisoner)

    assertThat(licenceKind).isEqualTo(LicenceKind.PRRD)
  }

  private companion object {
    const val PRISON_NUMBER = "NOMSID"
    val aProbationCaseResult = ProbationCase(
      crn = "X12345",
      croNumber = "AB01/234567C",
    )

    val somePrisonInformation = Prison(
      prisonId = "MDI",
      description = "Moorland (HMP)",
      phoneDetails = listOf(
        PhoneDetail(
          phoneId = 1,
          number = "0123 456 7890",
          type = "BUS",
          ext = null,
        ),
        PhoneDetail(
          phoneId = 2,
          number = "0800 123 4567",
          type = "FAX",
          ext = null,
        ),
      ),
    )

    val aCommunityManager =
      CommunityManager(
        code = "AB012C",
        id = 2000L,
        team = TeamDetail(
          code = "NA01A2-A",
          description = "Cardiff South Team A",
          borough = Detail(
            code = "N01A",
            description = "Cardiff",
          ),
          district = Detail(
            code = "N01A2",
            description = "Cardiff South",
          ),
        ),
        provider = Detail(
          code = "N01",
          description = "Wales",
        ),
        case = ProbationCase("A12345"),
        name = Name("com", null, "user"),
        allocationDate = LocalDate.of(2000, 1, 1),
        unallocated = false,
        username = "aComUser",
      )

    val com = CommunityOffenderManager(
      id = 1,
      staffIdentifier = 2000,
      username = "tcom",
      email = "testemail@probation.gov.uk",
      firstName = "X",
      lastName = "Y",
    )

    val prisonUser = PrisonUser(
      id = 1,
      username = "ca",
      email = "testemail@prison.gov.uk",
      firstName = "A",
      lastName = "B",
    )

    val comUser = User(
      id = 2000,
      username = "com-user",
      email = "comuser@probation.gov.uk",
      name = Name(
        forename = "com",
        surname = "user",
      ),
      teams = emptyList(),
      code = "AB00001",
      provider = Detail(code = "", description = null),
      unallocated = true,
    )

    val newCom = CommunityOffenderManager(
      id = 1,
      staffIdentifier = 2000,
      username = "com-user",
      email = "comuser@probation.gov.uk",
      firstName = "com",
      lastName = "user",
    )

    val aModelCurfewAddress = HdcCurfewAddress(
      1L,
      "1 Test Street",
      "Test Area",
      "Test Town",
      null,
      "AB1 2CD",
    )

    val someHdcLicenceData = HdcLicenceData(
      licenceId = 1L,
      aModelCurfewAddress,
      null,
      null,
    )
  }
}
