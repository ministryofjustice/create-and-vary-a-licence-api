package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anyList
import org.mockito.kotlin.any
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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.StandardCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StaffRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StandardConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.ResourceAlreadyExistsException
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.prisonerSearchResult
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.HARD_STOP_CONDITION
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PhoneDetail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.Prison
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.CommunityApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.CommunityOrPrisonOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.Detail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.OffenderDetail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.OffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.OtherIds
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ProbationSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.StaffDetail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.StaffHuman
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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence as EntityLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.LicenceEvent as EntityLicenceEvent

class LicenceCreationServiceTest {
  private val licencePolicyService = LicencePolicyService()

  private val additionalConditionRepository = mock<AdditionalConditionRepository>()
  private val standardConditionRepository = mock<StandardConditionRepository>()
  private val licenceRepository = mock<LicenceRepository>()
  private val staffRepository = mock<StaffRepository>()
  private val licenceEventRepository = mock<LicenceEventRepository>()
  private val auditEventRepository = mock<AuditEventRepository>()
  private val probationSearchApiClient = mock<ProbationSearchApiClient>()
  private val prisonerSearchApiClient = mock<PrisonerSearchApiClient>()
  private val prisonApiClient = mock<PrisonApiClient>()
  private val communityApiClient = mock<CommunityApiClient>()

  private val service = LicenceCreationService(
    licenceRepository,
    staffRepository,
    standardConditionRepository,
    additionalConditionRepository,
    licenceEventRepository,
    licencePolicyService,
    auditEventRepository,
    probationSearchApiClient,
    prisonerSearchApiClient,
    prisonApiClient,
    communityApiClient,
  )

  @Nested
  inner class CreatingCrdLicences {

    @BeforeEach
    fun reset() {
      reset(
        licenceRepository,
        licenceEventRepository,
        auditEventRepository,
        probationSearchApiClient,
        prisonerSearchApiClient,
        prisonApiClient,
        communityApiClient,
      )
      val authentication = mock<Authentication>()
      val securityContext = mock<SecurityContext>()

      whenever(authentication.name).thenReturn("smills")
      whenever(securityContext.authentication).thenReturn(authentication)
      SecurityContextHolder.setContext(securityContext)

      whenever(prisonApiClient.getPrisonInformation(any())).thenReturn(somePrisonInformation)

      whenever(staffRepository.findByStaffIdentifier(2000)).thenReturn(com)
      whenever(staffRepository.findByUsernameIgnoreCase("smills")).thenReturn(com)
      whenever(communityApiClient.getAllOffenderManagers(any())).thenReturn(listOf(aCommunityOrPrisonOffenderManager))

      whenever(additionalConditionRepository.saveAllAndFlush(anyList())).thenAnswer { it.arguments[0] }
      whenever(standardConditionRepository.saveAllAndFlush(anyList())).thenAnswer { it.arguments[0] }
      whenever(licenceRepository.saveAndFlush(any())).thenAnswer { it.arguments[0] }
    }

    @Test
    fun `service creates populates licence with expected fields`() {
      val aPrisonerSearchResult = prisonerSearchResult()
      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(listOf(aPrisonerSearchResult))
      whenever(probationSearchApiClient.searchForPersonOnProbation(any())).thenReturn(anOffenderDetailResult)

      service.createLicence(prisonNumber)

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
        assertThat(licenceExpiryDate).isEqualTo(aPrisonerSearchResult.licenceExpiryDate)
        assertThat(topupSupervisionStartDate).isEqualTo(aPrisonerSearchResult.topupSupervisionStartDate)
        assertThat(topupSupervisionExpiryDate).isEqualTo(aPrisonerSearchResult.topupSupervisionExpiryDate)
        assertThat(postRecallReleaseDate).isNull()
        assertThat(prisonDescription).isEqualTo(somePrisonInformation.description)
        assertThat(prisonTelephone).isEqualTo(somePrisonInformation.getPrisonContactNumber())
        assertThat(probationAreaCode).isEqualTo(aCommunityOrPrisonOffenderManager.probationArea.code)
        assertThat(probationAreaDescription).isEqualTo(aCommunityOrPrisonOffenderManager.probationArea.description)
        assertThat(probationPduCode).isEqualTo(aCommunityOrPrisonOffenderManager.team.borough.code)
        assertThat(probationPduDescription).isEqualTo(aCommunityOrPrisonOffenderManager.team.borough.description)
        assertThat(probationLauCode).isEqualTo(aCommunityOrPrisonOffenderManager.team.district.code)
        assertThat(probationLauDescription).isEqualTo(aCommunityOrPrisonOffenderManager.team.district.description)
        assertThat(probationTeamCode).isEqualTo(aCommunityOrPrisonOffenderManager.team.code)
        assertThat(probationTeamDescription).isEqualTo(aCommunityOrPrisonOffenderManager.team.description)
        assertThat(crn).isEqualTo(anOffenderDetailResult.otherIds.crn)
        assertThat(pnc).isEqualTo(anOffenderDetailResult.otherIds.pncNumber)
        assertThat(responsibleCom).isEqualTo(com)
      }
    }

    @Test
    fun `populates licence with crd when crd override present`() {
      val prisoner = prisonerSearchResult().copy(
        conditionalReleaseDate = LocalDate.now().plusDays(1),
        conditionalReleaseDateOverrideDate = LocalDate.now().plusDays(2),
      )
      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(
        listOf(prisoner),
      )
      whenever(probationSearchApiClient.searchForPersonOnProbation(any())).thenReturn(anOffenderDetailResult)

      service.createLicence(prisonNumber)

      val l = listOf(IN_PROGRESS)
      assertThat(l).allMatch { it == IN_PROGRESS }
      argumentCaptor<CrdLicence>().apply {
        verify(licenceRepository, times(1)).saveAndFlush(capture())
        assertThat(firstValue.conditionalReleaseDate).isEqualTo(prisoner.conditionalReleaseDateOverrideDate)
      }
    }

    @Test
    fun `Populates licence with CRD date when CRD override is absent`() {
      val prisoner = prisonerSearchResult().copy(
        conditionalReleaseDate = LocalDate.now().plusDays(1),
        conditionalReleaseDateOverrideDate = null,
      )
      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(
        listOf(prisoner),
      )
      whenever(probationSearchApiClient.searchForPersonOnProbation(any())).thenReturn(anOffenderDetailResult)

      service.createLicence(prisonNumber)

      argumentCaptor<CrdLicence>().apply {
        verify(licenceRepository, times(1)).saveAndFlush(capture())
        assertThat(firstValue.conditionalReleaseDate).isEqualTo(prisoner.conditionalReleaseDate)
      }
    }

    @Test
    fun `Populates licence with start date when confirmed release date is present`() {
      val prisoner = prisonerSearchResult().copy(
        confirmedReleaseDate = LocalDate.now().plusDays(1),
        conditionalReleaseDate = LocalDate.now().plusDays(2),
      )
      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(
        listOf(prisoner),
      )
      whenever(probationSearchApiClient.searchForPersonOnProbation(any())).thenReturn(anOffenderDetailResult)

      service.createLicence(prisonNumber)

      argumentCaptor<CrdLicence>().apply {
        verify(licenceRepository, times(1)).saveAndFlush(capture())
        assertThat(firstValue.licenceStartDate).isEqualTo(prisoner.confirmedReleaseDate)
      }
    }

    @Test
    fun `Populates licence with start date when confirmed release date is absent`() {
      val prisoner = prisonerSearchResult().copy(
        confirmedReleaseDate = null,
        conditionalReleaseDate = LocalDate.now().plusDays(1),
      )
      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(
        listOf(prisoner),
      )
      whenever(probationSearchApiClient.searchForPersonOnProbation(any())).thenReturn(anOffenderDetailResult)
      whenever(communityApiClient.getAllOffenderManagers(any())).thenReturn(listOf(aCommunityOrPrisonOffenderManager))

      service.createLicence(prisonNumber)

      argumentCaptor<CrdLicence>().apply {
        verify(licenceRepository, times(1)).saveAndFlush(capture())
        assertThat(firstValue.licenceStartDate).isEqualTo(prisoner.conditionalReleaseDate)
      }
    }

    @Test
    fun `Populates licence with CRO from delius when provided`() {
      val offender = anOffenderDetailResult.copy(
        otherIds = anOffenderDetailResult.otherIds.copy(croNumber = "ZZZZZ"),
      )
      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(
        listOf(
          prisonerSearchResult().copy(
            croNumber = "AAAAA",
          ),
        ),
      )
      whenever(probationSearchApiClient.searchForPersonOnProbation(any())).thenReturn(
        offender,
      )

      service.createLicence(prisonNumber)

      argumentCaptor<CrdLicence>().apply {
        verify(licenceRepository, times(1)).saveAndFlush(capture())
        assertThat(firstValue.cro).isEqualTo(offender.otherIds.croNumber)
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
      whenever(probationSearchApiClient.searchForPersonOnProbation(any())).thenReturn(
        anOffenderDetailResult.copy(
          otherIds = anOffenderDetailResult.otherIds.copy(croNumber = null),
        ),
      )

      service.createLicence(prisonNumber)

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
      whenever(probationSearchApiClient.searchForPersonOnProbation(any())).thenReturn(
        anOffenderDetailResult,
      )

      service.createLicence(prisonNumber)

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
      whenever(probationSearchApiClient.searchForPersonOnProbation(any())).thenReturn(
        anOffenderDetailResult,
      )

      service.createLicence(prisonNumber)

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
      whenever(probationSearchApiClient.searchForPersonOnProbation(any())).thenReturn(
        anOffenderDetailResult,
      )

      service.createLicence(prisonNumber)

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
      whenever(probationSearchApiClient.searchForPersonOnProbation(any())).thenReturn(
        anOffenderDetailResult,
      )

      service.createLicence(prisonNumber)

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
      whenever(probationSearchApiClient.searchForPersonOnProbation(any())).thenReturn(
        anOffenderDetailResult,
      )

      service.createLicence(prisonNumber)

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
      whenever(probationSearchApiClient.searchForPersonOnProbation(any())).thenReturn(anOffenderDetailResult)

      service.createLicence(prisonNumber)

      val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)
      val eventCaptor = ArgumentCaptor.forClass(EntityLicenceEvent::class.java)

      verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())
      verify(licenceEventRepository, times(1)).saveAndFlush(eventCaptor.capture())

      assertThat(auditCaptor.value)
        .extracting("licenceId", "username", "fullName", "summary")
        .isEqualTo(
          listOf(
            -1L,
            "smills",
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
            "smills",
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
            prisonNumber,
            listOf(IN_PROGRESS, SUBMITTED, APPROVED, REJECTED),
          ),
      ).thenReturn(listOf(existingLicence))

      val exception = assertThrows<ResourceAlreadyExistsException> {
        service.createLicence(prisonNumber)
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
            prisonNumber,
            listOf(IN_PROGRESS, SUBMITTED, APPROVED, REJECTED),
          ),
      ).thenReturn(listOf(approvedLicence, notApprovedLicence))

      val exception = assertThrows<ResourceAlreadyExistsException> {
        service.createLicence(prisonNumber)
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
      whenever(probationSearchApiClient.searchForPersonOnProbation(any())).thenReturn(
        anOffenderDetailResult.copy(
          offenderManagers = listOf(
            OffenderManager(
              staffDetail = StaffDetail(
                code = "AB012C",
                forenames = "Test",
                surname = "Test",
              ),
              active = false,
            ),
          ),
        ),
      )
      whenever(prisonApiClient.getPrisonInformation(any())).thenReturn(somePrisonInformation)
      whenever(communityApiClient.getAllOffenderManagers(any())).thenReturn(listOf(aCommunityOrPrisonOffenderManager))

      val exception = assertThrows<IllegalStateException> {
        service.createLicence(prisonNumber)
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
      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(listOf(prisonerSearchResult()))
      whenever(probationSearchApiClient.searchForPersonOnProbation(any())).thenReturn(
        anOffenderDetailResult.copy(
          offenderManagers = listOf(
            OffenderManager(
              staffDetail = StaffDetail(
                code = "XXXXXX",
                forenames = "Test",
                surname = "Test",
              ),
              active = true,
            ),
          ),
        ),
      )

      val exception = assertThrows<IllegalStateException> {
        service.createLicence(prisonNumber)
      }

      assertThat(exception)
        .isInstanceOf(IllegalStateException::class.java)
        .hasMessage("No responsible officer details found for NOMSID")

      verify(licenceRepository, times(0)).saveAndFlush(any())
      verify(standardConditionRepository, times(0)).saveAllAndFlush(anyList())
      verify(auditEventRepository, times(0)).saveAndFlush(any())
      verify(licenceEventRepository, times(0)).saveAndFlush(any())
    }

    @Test
    fun `service throws an error if no responsible COM found for this person`() {
      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(listOf(prisonerSearchResult()))
      whenever(probationSearchApiClient.searchForPersonOnProbation(any())).thenReturn(anOffenderDetailResult)
      whenever(prisonApiClient.getPrisonInformation(any())).thenReturn(somePrisonInformation)
      whenever(communityApiClient.getAllOffenderManagers(any())).thenReturn(listOf(aCommunityOrPrisonOffenderManager))
      whenever(staffRepository.findByStaffIdentifier(2000)).thenReturn(null)

      val exception = assertThrows<IllegalStateException> {
        service.createLicence(prisonNumber)
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
      whenever(probationSearchApiClient.searchForPersonOnProbation(any())).thenReturn(anOffenderDetailResult)
      whenever(prisonApiClient.getPrisonInformation(any())).thenReturn(somePrisonInformation)
      whenever(communityApiClient.getAllOffenderManagers(any())).thenReturn(listOf(aCommunityOrPrisonOffenderManager))
      whenever(staffRepository.findByStaffIdentifier(2000)).thenReturn(null)
      whenever(communityApiClient.getStaffByIdentifier(any())).thenReturn(comUser)
      whenever(staffRepository.saveAndFlush(any())).thenReturn(newCom)

      service.createLicence(prisonNumber)

      argumentCaptor<CommunityOffenderManager>().apply {
        verify(staffRepository, times(1)).saveAndFlush(capture())
        assertThat(firstValue.staffIdentifier).isEqualTo(comUser.staffIdentifier)
        assertThat(firstValue.username).isEqualTo(comUser.username!!.uppercase())
        assertThat(firstValue.email).isEqualTo(comUser.email)
        assertThat(firstValue.firstName).isEqualTo(comUser.staff!!.forenames)
        assertThat(firstValue.lastName).isEqualTo(comUser.staff!!.surname)
      }

      argumentCaptor<CrdLicence>().apply {
        verify(licenceRepository, times(1)).saveAndFlush(capture())
        assertThat(firstValue.responsibleCom!!.id).isEqualTo(newCom.id)
      }
    }

    @Test
    fun `service throws an error if no user found for this person`() {
      val expectedCom = CommunityOffenderManager(
        staffIdentifier = 2000,
        username = "smills",
        email = "testemail@probation.gov.uk",
        firstName = "X",
        lastName = "Y",
      )

      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(listOf(prisonerSearchResult()))
      whenever(probationSearchApiClient.searchForPersonOnProbation(any())).thenReturn(anOffenderDetailResult)
      whenever(prisonApiClient.getPrisonInformation(any())).thenReturn(somePrisonInformation)
      whenever(communityApiClient.getAllOffenderManagers(any())).thenReturn(listOf(aCommunityOrPrisonOffenderManager))

      whenever(staffRepository.findByStaffIdentifier(2000)).thenReturn(expectedCom)
      whenever(staffRepository.findByUsernameIgnoreCase("smills")).thenReturn(null)

      val exception = assertThrows<IllegalStateException> {
        service.createLicence(prisonNumber)
      }

      assertThat(exception)
        .isInstanceOf(IllegalStateException::class.java)
        .hasMessage("Staff with username smills not found")

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
        probationSearchApiClient,
        prisonerSearchApiClient,
        prisonApiClient,
        communityApiClient,
      )
      val authentication = mock<Authentication>()
      val securityContext = mock<SecurityContext>()

      whenever(authentication.name).thenReturn(prisonUser.username)
      whenever(securityContext.authentication).thenReturn(authentication)
      SecurityContextHolder.setContext(securityContext)

      whenever(prisonApiClient.getPrisonInformation(any())).thenReturn(somePrisonInformation)

      whenever(staffRepository.findByStaffIdentifier(2000)).thenReturn(com)
      whenever(staffRepository.findByUsernameIgnoreCase(prisonUser.username)).thenReturn(prisonUser)
      whenever(communityApiClient.getAllOffenderManagers(any())).thenReturn(listOf(aCommunityOrPrisonOffenderManager))

      whenever(standardConditionRepository.saveAllAndFlush(anyList())).thenAnswer { it.arguments[0] }
      whenever(licenceRepository.saveAndFlush(any())).thenAnswer { it.arguments[0] }
    }

    @Test
    fun `service creates populates licence with expected fields`() {
      val aPrisonerSearchResult = prisonerSearchResult()
      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(listOf(aPrisonerSearchResult))
      whenever(probationSearchApiClient.searchForPersonOnProbation(any())).thenReturn(anOffenderDetailResult)

      service.createHardStopLicence(prisonNumber)

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
        assertThat(licenceExpiryDate).isEqualTo(aPrisonerSearchResult.licenceExpiryDate)
        assertThat(topupSupervisionStartDate).isEqualTo(aPrisonerSearchResult.topupSupervisionStartDate)
        assertThat(topupSupervisionExpiryDate).isEqualTo(aPrisonerSearchResult.topupSupervisionExpiryDate)
        assertThat(prisonDescription).isEqualTo(somePrisonInformation.description)
        assertThat(prisonTelephone).isEqualTo(somePrisonInformation.getPrisonContactNumber())
        assertThat(probationAreaCode).isEqualTo(aCommunityOrPrisonOffenderManager.probationArea.code)
        assertThat(probationAreaDescription).isEqualTo(aCommunityOrPrisonOffenderManager.probationArea.description)
        assertThat(probationPduCode).isEqualTo(aCommunityOrPrisonOffenderManager.team.borough.code)
        assertThat(probationPduDescription).isEqualTo(aCommunityOrPrisonOffenderManager.team.borough.description)
        assertThat(probationLauCode).isEqualTo(aCommunityOrPrisonOffenderManager.team.district.code)
        assertThat(probationLauDescription).isEqualTo(aCommunityOrPrisonOffenderManager.team.district.description)
        assertThat(probationTeamCode).isEqualTo(aCommunityOrPrisonOffenderManager.team.code)
        assertThat(probationTeamDescription).isEqualTo(aCommunityOrPrisonOffenderManager.team.description)
        assertThat(crn).isEqualTo(anOffenderDetailResult.otherIds.crn)
        assertThat(pnc).isEqualTo(anOffenderDetailResult.otherIds.pncNumber)
        assertThat(responsibleCom).isEqualTo(com)
      }
    }

    @Test
    fun `populates licence with crd when crd override present`() {
      val prisoner = prisonerSearchResult().copy(
        conditionalReleaseDate = LocalDate.now().plusDays(1),
        conditionalReleaseDateOverrideDate = LocalDate.now().plusDays(2),
      )
      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(
        listOf(prisoner),
      )
      whenever(probationSearchApiClient.searchForPersonOnProbation(any())).thenReturn(anOffenderDetailResult)

      service.createHardStopLicence(prisonNumber)

      argumentCaptor<HardStopLicence>().apply {
        verify(licenceRepository, times(1)).saveAndFlush(capture())
        assertThat(firstValue.conditionalReleaseDate).isEqualTo(prisoner.conditionalReleaseDateOverrideDate)
      }
    }

    @Test
    fun `Populates licence with CRD date when CRD override is absent`() {
      val prisoner = prisonerSearchResult().copy(
        conditionalReleaseDate = LocalDate.now().plusDays(1),
        conditionalReleaseDateOverrideDate = null,
      )
      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(
        listOf(prisoner),
      )
      whenever(probationSearchApiClient.searchForPersonOnProbation(any())).thenReturn(anOffenderDetailResult)

      service.createHardStopLicence(prisonNumber)

      argumentCaptor<HardStopLicence>().apply {
        verify(licenceRepository, times(1)).saveAndFlush(capture())
        assertThat(firstValue.conditionalReleaseDate).isEqualTo(prisoner.conditionalReleaseDate)
      }
    }

    @Test
    fun `Populates licence with start date when confirmed release date is present`() {
      val prisoner = prisonerSearchResult().copy(
        confirmedReleaseDate = LocalDate.now().plusDays(1),
        conditionalReleaseDate = LocalDate.now().plusDays(2),
      )
      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(
        listOf(prisoner),
      )
      whenever(probationSearchApiClient.searchForPersonOnProbation(any())).thenReturn(anOffenderDetailResult)

      service.createHardStopLicence(prisonNumber)

      argumentCaptor<HardStopLicence>().apply {
        verify(licenceRepository, times(1)).saveAndFlush(capture())
        assertThat(firstValue.licenceStartDate).isEqualTo(prisoner.confirmedReleaseDate)
      }
    }

    @Test
    fun `Populates licence with start date when confirmed release date is absent`() {
      val prisoner = prisonerSearchResult().copy(
        confirmedReleaseDate = null,
        conditionalReleaseDate = LocalDate.now().plusDays(1),
      )
      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(
        listOf(prisoner),
      )
      whenever(probationSearchApiClient.searchForPersonOnProbation(any())).thenReturn(anOffenderDetailResult)
      whenever(communityApiClient.getAllOffenderManagers(any())).thenReturn(listOf(aCommunityOrPrisonOffenderManager))

      service.createHardStopLicence(prisonNumber)

      argumentCaptor<HardStopLicence>().apply {
        verify(licenceRepository, times(1)).saveAndFlush(capture())
        assertThat(firstValue.licenceStartDate).isEqualTo(prisoner.conditionalReleaseDate)
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
      whenever(probationSearchApiClient.searchForPersonOnProbation(any())).thenReturn(anOffenderDetailResult)

      service.createHardStopLicence(prisonNumber)

      argumentCaptor<HardStopLicence>().apply {
        verify(licenceRepository, times(1)).saveAndFlush(capture())
        assertThat(firstValue.postRecallReleaseDate).isEqualTo(prisoner.postRecallReleaseDate)
      }
    }

    @Test
    fun `Populates licence with CRO from delius when provided`() {
      val offender = anOffenderDetailResult.copy(
        otherIds = anOffenderDetailResult.otherIds.copy(croNumber = "ZZZZZ"),
      )
      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(
        listOf(
          prisonerSearchResult().copy(
            croNumber = "AAAAA",
          ),
        ),
      )
      whenever(probationSearchApiClient.searchForPersonOnProbation(any())).thenReturn(
        offender,
      )

      service.createHardStopLicence(prisonNumber)

      argumentCaptor<HardStopLicence>().apply {
        verify(licenceRepository, times(1)).saveAndFlush(capture())
        assertThat(firstValue.cro).isEqualTo(offender.otherIds.croNumber)
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
      whenever(probationSearchApiClient.searchForPersonOnProbation(any())).thenReturn(
        anOffenderDetailResult.copy(
          otherIds = anOffenderDetailResult.otherIds.copy(croNumber = null),
        ),
      )

      service.createHardStopLicence(prisonNumber)

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
      whenever(probationSearchApiClient.searchForPersonOnProbation(any())).thenReturn(
        anOffenderDetailResult,
      )

      service.createHardStopLicence(prisonNumber)

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
      whenever(probationSearchApiClient.searchForPersonOnProbation(any())).thenReturn(
        anOffenderDetailResult,
      )

      service.createHardStopLicence(prisonNumber)

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
      whenever(probationSearchApiClient.searchForPersonOnProbation(any())).thenReturn(anOffenderDetailResult)

      val previousLicence = TestData.createCrdLicence().copy(id = 1234L)

      whenever(
        licenceRepository.findAllByBookingIdInAndStatusCodeOrderByDateCreatedDesc(
          listOf(aPrisonerSearchResult.bookingId!!.toLong()),
          TIMED_OUT,
        ),
      ).thenReturn(listOf(previousLicence))

      service.createHardStopLicence(prisonNumber)

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
      whenever(probationSearchApiClient.searchForPersonOnProbation(any())).thenReturn(
        anOffenderDetailResult,
      )

      service.createHardStopLicence(prisonNumber)

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
      whenever(probationSearchApiClient.searchForPersonOnProbation(any())).thenReturn(
        anOffenderDetailResult,
      )

      service.createHardStopLicence(prisonNumber)

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
      whenever(probationSearchApiClient.searchForPersonOnProbation(any())).thenReturn(
        anOffenderDetailResult,
      )

      service.createHardStopLicence(prisonNumber)

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
      whenever(probationSearchApiClient.searchForPersonOnProbation(any())).thenReturn(anOffenderDetailResult)

      service.createHardStopLicence(prisonNumber)

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
            prisonNumber,
            listOf(IN_PROGRESS, SUBMITTED, APPROVED, REJECTED),
          ),
      ).thenReturn(listOf(existingLicence))

      val exception = assertThrows<ResourceAlreadyExistsException> {
        service.createHardStopLicence(prisonNumber)
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
      whenever(probationSearchApiClient.searchForPersonOnProbation(any())).thenReturn(
        anOffenderDetailResult.copy(
          offenderManagers = listOf(
            OffenderManager(
              staffDetail = StaffDetail(
                code = "AB012C",
                forenames = "Test",
                surname = "Test",
              ),
              active = false,
            ),
          ),
        ),
      )
      whenever(prisonApiClient.getPrisonInformation(any())).thenReturn(somePrisonInformation)
      whenever(communityApiClient.getAllOffenderManagers(any())).thenReturn(listOf(aCommunityOrPrisonOffenderManager))

      val exception = assertThrows<IllegalStateException> {
        service.createHardStopLicence(prisonNumber)
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
      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(listOf(prisonerSearchResult()))
      whenever(probationSearchApiClient.searchForPersonOnProbation(any())).thenReturn(
        anOffenderDetailResult.copy(
          offenderManagers = listOf(
            OffenderManager(
              staffDetail = StaffDetail(
                code = "XXXXXX",
                forenames = "Test",
                surname = "Test",
              ),
              active = true,
            ),
          ),
        ),
      )

      val exception = assertThrows<IllegalStateException> {
        service.createHardStopLicence(prisonNumber)
      }

      assertThat(exception)
        .isInstanceOf(IllegalStateException::class.java)
        .hasMessage("No responsible officer details found for NOMSID")

      verify(licenceRepository, times(0)).saveAndFlush(any())
      verify(standardConditionRepository, times(0)).saveAllAndFlush(anyList())
      verify(auditEventRepository, times(0)).saveAndFlush(any())
      verify(licenceEventRepository, times(0)).saveAndFlush(any())
    }

    @Test
    fun `service throws an error if no responsible COM found for this person`() {
      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(listOf(prisonerSearchResult()))
      whenever(probationSearchApiClient.searchForPersonOnProbation(any())).thenReturn(anOffenderDetailResult)
      whenever(prisonApiClient.getPrisonInformation(any())).thenReturn(somePrisonInformation)
      whenever(communityApiClient.getAllOffenderManagers(any())).thenReturn(listOf(aCommunityOrPrisonOffenderManager))
      whenever(staffRepository.findByStaffIdentifier(2000)).thenReturn(null)

      val exception = assertThrows<IllegalStateException> {
        service.createHardStopLicence(prisonNumber)
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
      whenever(probationSearchApiClient.searchForPersonOnProbation(any())).thenReturn(anOffenderDetailResult)
      whenever(prisonApiClient.getPrisonInformation(any())).thenReturn(somePrisonInformation)
      whenever(communityApiClient.getAllOffenderManagers(any())).thenReturn(listOf(aCommunityOrPrisonOffenderManager))
      whenever(staffRepository.findByStaffIdentifier(2000)).thenReturn(null)
      whenever(communityApiClient.getStaffByIdentifier(any())).thenReturn(comUser)
      whenever(staffRepository.saveAndFlush(any())).thenReturn(newCom)

      service.createHardStopLicence(prisonNumber)

      argumentCaptor<CommunityOffenderManager>().apply {
        verify(staffRepository, times(1)).saveAndFlush(capture())
        assertThat(firstValue.staffIdentifier).isEqualTo(comUser.staffIdentifier)
        assertThat(firstValue.username).isEqualTo(comUser.username!!.uppercase())
        assertThat(firstValue.email).isEqualTo(comUser.email)
        assertThat(firstValue.firstName).isEqualTo(comUser.staff!!.forenames)
        assertThat(firstValue.lastName).isEqualTo(comUser.staff!!.surname)
      }

      argumentCaptor<HardStopLicence>().apply {
        verify(licenceRepository, times(1)).saveAndFlush(capture())
        assertThat(firstValue.responsibleCom!!.id).isEqualTo(newCom.id)
      }
    }

    @Test
    fun `service throws an error if no user found for this person`() {
      val expectedCom = CommunityOffenderManager(
        staffIdentifier = 2000,
        username = "smills",
        email = "testemail@probation.gov.uk",
        firstName = "X",
        lastName = "Y",
      )

      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(listOf(prisonerSearchResult()))
      whenever(probationSearchApiClient.searchForPersonOnProbation(any())).thenReturn(anOffenderDetailResult)
      whenever(prisonApiClient.getPrisonInformation(any())).thenReturn(somePrisonInformation)
      whenever(communityApiClient.getAllOffenderManagers(any())).thenReturn(listOf(aCommunityOrPrisonOffenderManager))

      whenever(staffRepository.findByStaffIdentifier(2000)).thenReturn(expectedCom)
      whenever(staffRepository.findByUsernameIgnoreCase(prisonUser.username)).thenReturn(null)

      val exception = assertThrows<IllegalStateException> {
        service.createHardStopLicence(prisonNumber)
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
        probationSearchApiClient,
        prisonerSearchApiClient,
        prisonApiClient,
        communityApiClient,
      )
      val authentication = mock<Authentication>()
      val securityContext = mock<SecurityContext>()

      whenever(authentication.name).thenReturn("smills")
      whenever(securityContext.authentication).thenReturn(authentication)
      SecurityContextHolder.setContext(securityContext)

      whenever(prisonApiClient.getPrisonInformation(any())).thenReturn(somePrisonInformation)

      whenever(staffRepository.findByStaffIdentifier(2000)).thenReturn(com)
      whenever(staffRepository.findByUsernameIgnoreCase("smills")).thenReturn(com)
      whenever(communityApiClient.getAllOffenderManagers(any())).thenReturn(listOf(aCommunityOrPrisonOffenderManager))

      whenever(additionalConditionRepository.saveAllAndFlush(anyList())).thenAnswer { it.arguments[0] }
      whenever(standardConditionRepository.saveAllAndFlush(anyList())).thenAnswer { it.arguments[0] }
      whenever(licenceRepository.saveAndFlush(any())).thenAnswer { it.arguments[0] }
    }

    @Test
    fun `service populates licence with expected fields`() {
      val aPrisonerSearchResult = prisonerSearchResult()
      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(listOf(aPrisonerSearchResult))
      whenever(probationSearchApiClient.searchForPersonOnProbation(any())).thenReturn(anOffenderDetailResult)

      service.createHdcLicence(prisonNumber)

      val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
      verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())

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
        assertThat(topupSupervisionStartDate).isEqualTo(aPrisonerSearchResult.topupSupervisionStartDate)
        assertThat(topupSupervisionExpiryDate).isEqualTo(aPrisonerSearchResult.topupSupervisionExpiryDate)
        assertThat(postRecallReleaseDate).isNull()
        assertThat(prisonDescription).isEqualTo(somePrisonInformation.description)
        assertThat(prisonTelephone).isEqualTo(somePrisonInformation.getPrisonContactNumber())
        assertThat(probationAreaCode).isEqualTo(aCommunityOrPrisonOffenderManager.probationArea.code)
        assertThat(probationAreaDescription).isEqualTo(aCommunityOrPrisonOffenderManager.probationArea.description)
        assertThat(probationPduCode).isEqualTo(aCommunityOrPrisonOffenderManager.team.borough.code)
        assertThat(probationPduDescription).isEqualTo(aCommunityOrPrisonOffenderManager.team.borough.description)
        assertThat(probationLauCode).isEqualTo(aCommunityOrPrisonOffenderManager.team.district.code)
        assertThat(probationLauDescription).isEqualTo(aCommunityOrPrisonOffenderManager.team.district.description)
        assertThat(probationTeamCode).isEqualTo(aCommunityOrPrisonOffenderManager.team.code)
        assertThat(probationTeamDescription).isEqualTo(aCommunityOrPrisonOffenderManager.team.description)
        assertThat(crn).isEqualTo(anOffenderDetailResult.otherIds.crn)
        assertThat(pnc).isEqualTo(anOffenderDetailResult.otherIds.pncNumber)
        assertThat(responsibleCom).isEqualTo(com)
      }
    }
  }

  private companion object {
    val prisonNumber = "NOMSID"
    val anOffenderDetailResult = OffenderDetail(
      offenderId = 1L,
      otherIds = OtherIds(
        crn = "X12345",
        croNumber = "AB01/234567C",
        pncNumber = null,
      ),
      offenderManagers = listOf(
        OffenderManager(
          staffDetail = StaffDetail(
            code = "AB012C",
            forenames = "Test",
            surname = "Test",
          ),
          active = true,
        ),
      ),
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

    val aCommunityOrPrisonOffenderManager =
      CommunityOrPrisonOffenderManager(
        staffCode = "AB012C",
        staffId = 2000L,
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
        probationArea = Detail(
          code = "N01",
          description = "Wales",
        ),
      )

    val com = CommunityOffenderManager(
      staffIdentifier = 2000,
      username = "smills",
      email = "testemail@probation.gov.uk",
      firstName = "X",
      lastName = "Y",
    )

    val prisonUser = PrisonUser(
      username = "ca",
      email = "testemail@prison.gov.uk",
      firstName = "A",
      lastName = "B",
    )

    val comUser = User(
      staffIdentifier = 2000,
      username = "com-user",
      email = "comuser@probation.gov.uk",
      staff = StaffHuman(
        forenames = "com",
        surname = "user",
      ),
      teams = emptyList(),
      staffCode = "AB00001",
    )

    val newCom = CommunityOffenderManager(
      id = -2L,
      staffIdentifier = 2000,
      username = "com-user",
      email = "comuser@probation.gov.uk",
      firstName = "com",
      lastName = "user",
    )
  }
}
