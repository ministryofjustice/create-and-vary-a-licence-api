package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import jakarta.validation.ValidationException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
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
import org.mockito.kotlin.whenever
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.StandardCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.CreateLicenceRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StaffRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StandardConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PhoneDetail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.Prison
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.CommunityApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.CommunityOrPrisonOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.Detail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.OffenderDetail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.OffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.OtherIds
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ProbationSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.StaffDetail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.TeamDetail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AuditEvent as EntityAuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence as EntityLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.LicenceEvent as EntityLicenceEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.StandardCondition as ModelStandardCondition

class LicenceCreationServiceTest {
  private val licencePolicyService = LicencePolicyService()

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
    licenceEventRepository,
    licencePolicyService,
    auditEventRepository,
    probationSearchApiClient,
    prisonerSearchApiClient,
    prisonApiClient,
    communityApiClient,
  )

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

    whenever(standardConditionRepository.saveAllAndFlush(anyList())).thenAnswer { it.arguments[0] }
    whenever(licenceRepository.saveAndFlush(any())).thenAnswer { it.arguments[0] }
  }

  @Test
  fun `service creates populates licence with expected fields`() {
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(listOf(aPrisonerSearchResult))
    whenever(probationSearchApiClient.searchForPersonOnProbation(any())).thenReturn(anOffenderDetailResult)

    service.createLicence(aCreateLicenceRequest)

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())

    with(licenceCaptor.value as CrdLicence) {
      assertThat(kind).isEqualTo(LicenceKind.CRD)
      assertThat(typeCode).isEqualTo(LicenceType.AP)
      assertThat(version).isEqualTo("2.1")
      assertThat(statusCode).isEqualTo(LicenceStatus.IN_PROGRESS)
      assertThat(versionOfId).isNull()
      assertThat(licenceVersion).isEqualTo("1.0")
      assertThat(nomsId).isEqualTo(nomsId)
      assertThat(bookingNo).isEqualTo(aPrisonerSearchResult.bookNumber)
      assertThat(bookingId).isEqualTo(aPrisonerSearchResult.bookingId.toLong())
      assertThat(prisonCode).isEqualTo(aPrisonerSearchResult.prisonId)
      assertThat(forename).isEqualTo(aPrisonerSearchResult.firstName.convertToTitleCase())
      assertThat(middleNames).isEqualTo(aPrisonerSearchResult.middleNames?.convertToTitleCase() ?: "")
      assertThat(surname).isEqualTo(aPrisonerSearchResult.lastName.convertToTitleCase())
      assertThat(dateOfBirth).isEqualTo(aPrisonerSearchResult.dateOfBirth)
      assertThat(actualReleaseDate).isEqualTo(aPrisonerSearchResult.confirmedReleaseDate)
      assertThat(sentenceStartDate).isEqualTo(aPrisonerSearchResult.sentenceStartDate)
      assertThat(sentenceEndDate).isEqualTo(aPrisonerSearchResult.sentenceExpiryDate)
      assertThat(licenceExpiryDate).isEqualTo(aPrisonerSearchResult.licenceExpiryDate)
      assertThat(topupSupervisionStartDate).isEqualTo(aPrisonerSearchResult.topUpSupervisionStartDate)
      assertThat(topupSupervisionExpiryDate).isEqualTo(aPrisonerSearchResult.topUpSupervisionExpiryDate)
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
    val prisoner = aPrisonerSearchResult.copy(
      conditionalReleaseDate = LocalDate.now().plusDays(1),
      conditionalReleaseDateOverrideDate = LocalDate.now().plusDays(2),
    )
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(
      listOf(prisoner),
    )
    whenever(probationSearchApiClient.searchForPersonOnProbation(any())).thenReturn(anOffenderDetailResult)

    service.createLicence(aCreateLicenceRequest)

    argumentCaptor<CrdLicence>().apply {
      verify(licenceRepository, times(1)).saveAndFlush(capture())
      assertThat(firstValue.conditionalReleaseDate).isEqualTo(prisoner.conditionalReleaseDateOverrideDate)
    }
  }

  @Test
  fun `Populates licence with CRD date when CRD override is absent`() {
    val prisoner = aPrisonerSearchResult.copy(
      conditionalReleaseDate = LocalDate.now().plusDays(1),
      conditionalReleaseDateOverrideDate = null,
    )
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(
      listOf(prisoner),
    )
    whenever(probationSearchApiClient.searchForPersonOnProbation(any())).thenReturn(anOffenderDetailResult)

    service.createLicence(aCreateLicenceRequest)

    argumentCaptor<CrdLicence>().apply {
      verify(licenceRepository, times(1)).saveAndFlush(capture())
      assertThat(firstValue.conditionalReleaseDate).isEqualTo(prisoner.conditionalReleaseDate)
    }
  }

  @Test
  fun `Populates licence with start date when confirmed release date is present`() {
    val prisoner = aPrisonerSearchResult.copy(
      confirmedReleaseDate = LocalDate.now().plusDays(1),
      conditionalReleaseDate = LocalDate.now().plusDays(2),
    )
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(
      listOf(prisoner),
    )
    whenever(probationSearchApiClient.searchForPersonOnProbation(any())).thenReturn(anOffenderDetailResult)

    service.createLicence(aCreateLicenceRequest)

    argumentCaptor<CrdLicence>().apply {
      verify(licenceRepository, times(1)).saveAndFlush(capture())
      assertThat(firstValue.licenceStartDate).isEqualTo(prisoner.confirmedReleaseDate)
    }
  }

  @Test
  fun `Populates licence with start date when confirmed release date is absent`() {
    val prisoner = aPrisonerSearchResult.copy(
      confirmedReleaseDate = null,
      conditionalReleaseDate = LocalDate.now().plusDays(1),
    )
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(
      listOf(prisoner),
    )
    whenever(probationSearchApiClient.searchForPersonOnProbation(any())).thenReturn(anOffenderDetailResult)
    whenever(communityApiClient.getAllOffenderManagers(any())).thenReturn(listOf(aCommunityOrPrisonOffenderManager))

    service.createLicence(aCreateLicenceRequest)

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
        aPrisonerSearchResult.copy(
          croNumber = "AAAAA",
        ),
      ),
    )
    whenever(probationSearchApiClient.searchForPersonOnProbation(any())).thenReturn(
      offender,
    )

    service.createLicence(aCreateLicenceRequest)

    argumentCaptor<CrdLicence>().apply {
      verify(licenceRepository, times(1)).saveAndFlush(capture())
      assertThat(firstValue.cro).isEqualTo(offender.otherIds.croNumber)
    }
  }

  @Test
  fun `Populates licence with CRO from NOMIS when not provided by delius`() {
    val prisoner = aPrisonerSearchResult.copy(
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

    service.createLicence(aCreateLicenceRequest)

    argumentCaptor<CrdLicence>().apply {
      verify(licenceRepository, times(1)).saveAndFlush(capture())
      assertThat(firstValue.cro).isEqualTo(prisoner.croNumber)
    }
  }

  @Test
  fun `Populates licence with middlename if provided`() {
    val prisoner = aPrisonerSearchResult.copy(
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

    service.createLicence(aCreateLicenceRequest)

    argumentCaptor<CrdLicence>().apply {
      verify(licenceRepository, times(1)).saveAndFlush(capture())
      assertThat(firstValue.middleNames).isEqualTo("Timothy")
    }
  }

  @Test
  fun `Populates licence with default middlename if not provided`() {
    val prisoner = aPrisonerSearchResult.copy(
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

    service.createLicence(aCreateLicenceRequest)

    argumentCaptor<CrdLicence>().apply {
      verify(licenceRepository, times(1)).saveAndFlush(capture())
      assertThat(firstValue.middleNames).isEqualTo("")
    }
  }

  @Test
  fun `Populates licence with standard conditions for AP licence`() {
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(
      listOf(aPrisonerSearchResult.copy(topUpSupervisionExpiryDate = null, licenceExpiryDate = LocalDate.now())),
    )
    whenever(probationSearchApiClient.searchForPersonOnProbation(any())).thenReturn(
      anOffenderDetailResult,
    )

    service.createLicence(aCreateLicenceRequest)

    argumentCaptor<CrdLicence>().apply {
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
  }

  @Test
  fun `Populates licence with standard conditions for PSS licence`() {
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(
      listOf(aPrisonerSearchResult.copy(topUpSupervisionExpiryDate = LocalDate.now(), licenceExpiryDate = null)),
    )
    whenever(probationSearchApiClient.searchForPersonOnProbation(any())).thenReturn(
      anOffenderDetailResult,
    )

    service.createLicence(aCreateLicenceRequest)

    argumentCaptor<CrdLicence>().apply {
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
  }

  @Test
  fun `Populates licence with standard conditions for an AP and PSS licence`() {
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(
      listOf(
        aPrisonerSearchResult.copy(
          topUpSupervisionExpiryDate = LocalDate.now().plusDays(1),
          licenceExpiryDate = LocalDate.now(),
        ),
      ),
    )
    whenever(probationSearchApiClient.searchForPersonOnProbation(any())).thenReturn(
      anOffenderDetailResult,
    )

    service.createLicence(aCreateLicenceRequest)

    argumentCaptor<CrdLicence>().apply {
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
  }

  @Test
  fun `service audits correctly`() {
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(listOf(aPrisonerSearchResult))
    whenever(probationSearchApiClient.searchForPersonOnProbation(any())).thenReturn(anOffenderDetailResult)

    val createResponse = service.createLicence(aCreateLicenceRequest)

    assertThat(createResponse.licenceStatus).isEqualTo(LicenceStatus.IN_PROGRESS)
    assertThat(createResponse.licenceType).isEqualTo(LicenceType.AP)

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
    whenever(
      licenceRepository
        .findAllByNomsIdAndStatusCodeIn(
          aCreateLicenceRequest.nomsId!!,
          listOf(LicenceStatus.IN_PROGRESS, LicenceStatus.SUBMITTED, LicenceStatus.APPROVED, LicenceStatus.REJECTED),
        ),
    ).thenReturn(listOf(TestData.createCrdLicence()))

    val exception = assertThrows<ValidationException> {
      service.createLicence(aCreateLicenceRequest)
    }

    assertThat(exception)
      .isInstanceOf(ValidationException::class.java)
      .withFailMessage("A licence already exists for this person (IN_PROGRESS, SUBMITTED, APPROVED or REJECTED)")

    verify(licenceRepository, times(0)).saveAndFlush(any())
    verify(standardConditionRepository, times(0)).saveAllAndFlush(anyList())
    verify(auditEventRepository, times(0)).saveAndFlush(any())
    verify(licenceEventRepository, times(0)).saveAndFlush(any())
  }

  @Test
  fun `service throws an error if no active offender manager found for this person`() {
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(listOf(aPrisonerSearchResult))
    whenever(probationSearchApiClient.searchForPersonOnProbation(any())).thenReturn(
      anOffenderDetailResult.copy(
        offenderManagers = listOf(
          OffenderManager(
            staffDetail = StaffDetail(
              code = "AB012C",
            ),
            active = false,
          ),
        ),
      ),
    )
    whenever(prisonApiClient.getPrisonInformation(any())).thenReturn(somePrisonInformation)
    whenever(communityApiClient.getAllOffenderManagers(any())).thenReturn(listOf(aCommunityOrPrisonOffenderManager))

    val exception = assertThrows<IllegalStateException> {
      service.createLicence(aCreateLicenceRequest)
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
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(listOf(aPrisonerSearchResult))
    whenever(probationSearchApiClient.searchForPersonOnProbation(any())).thenReturn(
      anOffenderDetailResult.copy(
        offenderManagers = listOf(
          OffenderManager(
            staffDetail = StaffDetail(
              code = "XXXXXX",
            ),
            active = true,
          ),
        ),
      ),
    )

    val exception = assertThrows<IllegalStateException> {
      service.createLicence(aCreateLicenceRequest)
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
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(listOf(aPrisonerSearchResult))
    whenever(probationSearchApiClient.searchForPersonOnProbation(any())).thenReturn(anOffenderDetailResult)
    whenever(prisonApiClient.getPrisonInformation(any())).thenReturn(somePrisonInformation)
    whenever(communityApiClient.getAllOffenderManagers(any())).thenReturn(listOf(aCommunityOrPrisonOffenderManager))

    whenever(staffRepository.findByStaffIdentifier(2000)).thenReturn(null)

    val exception = assertThrows<IllegalStateException> {
      service.createLicence(aCreateLicenceRequest)
    }

    assertThat(exception)
      .isInstanceOf(IllegalStateException::class.java)
      .hasMessage("Staff with staffIdentifier 2000 not found")

    verify(licenceRepository, times(0)).saveAndFlush(any())
    verify(standardConditionRepository, times(0)).saveAllAndFlush(anyList())
    verify(auditEventRepository, times(0)).saveAndFlush(any())
    verify(licenceEventRepository, times(0)).saveAndFlush(any())
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

    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(anyList())).thenReturn(listOf(aPrisonerSearchResult))
    whenever(probationSearchApiClient.searchForPersonOnProbation(any())).thenReturn(anOffenderDetailResult)
    whenever(prisonApiClient.getPrisonInformation(any())).thenReturn(somePrisonInformation)
    whenever(communityApiClient.getAllOffenderManagers(any())).thenReturn(listOf(aCommunityOrPrisonOffenderManager))

    whenever(staffRepository.findByStaffIdentifier(2000)).thenReturn(expectedCom)
    whenever(staffRepository.findByUsernameIgnoreCase("smills")).thenReturn(null)

    val exception = assertThrows<IllegalStateException> {
      service.createLicence(aCreateLicenceRequest)
    }

    assertThat(exception)
      .isInstanceOf(IllegalStateException::class.java)
      .hasMessage("Staff with username smills not found")

    verify(licenceRepository, times(0)).saveAndFlush(any())
    verify(standardConditionRepository, times(0)).saveAllAndFlush(anyList())
    verify(auditEventRepository, times(0)).saveAndFlush(any())
    verify(licenceEventRepository, times(0)).saveAndFlush(any())
  }

  private companion object {
    val someStandardConditions = listOf(
      ModelStandardCondition(id = 1, code = "goodBehaviour", sequence = 1, text = "Be of good behaviour"),
      ModelStandardCondition(id = 2, code = "notBreakLaw", sequence = 2, text = "Do not break any law"),
      ModelStandardCondition(id = 3, code = "attendMeetings", sequence = 3, text = "Attend meetings"),
    )

    val aCreateLicenceRequest = CreateLicenceRequest(
      typeCode = LicenceType.AP,
      version = "1.4",
      nomsId = "NOMSID",
      bookingNo = "BOOKINGNO",
      bookingId = 1L,
      crn = "CRN1",
      pnc = "PNC1",
      cro = "CRO1",
      prisonCode = "MDI",
      prisonDescription = "Moorland (HMP)",
      forename = "Mike",
      surname = "Myers",
      dateOfBirth = LocalDate.of(2001, 10, 1),
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
      standardLicenceConditions = someStandardConditions,
      standardPssConditions = someStandardConditions,
      responsibleComStaffId = 2000,
    )

    val aPrisonerSearchResult = PrisonerSearchPrisoner(
      prisonerNumber = "A1234AA",
      bookingId = "123456",
      status = "ACTIVE IN",
      mostSeriousOffence = "Robbery",
      licenceExpiryDate = LocalDate.of(2021, 10, 22),
      topUpSupervisionExpiryDate = LocalDate.of(2021, 10, 22),
      homeDetentionCurfewEligibilityDate = null,
      releaseDate = LocalDate.of(2021, 10, 22),
      confirmedReleaseDate = LocalDate.of(2021, 10, 22),
      conditionalReleaseDate = LocalDate.of(2021, 10, 22),
      paroleEligibilityDate = null,
      actualParoleDate = null,
      postRecallReleaseDate = null,
      legalStatus = "SENTENCED",
      indeterminateSentence = false,
      recall = false,
      prisonId = "MDI",
      bookNumber = "12345A",
      firstName = "Bob",
      middleNames = null,
      lastName = "Mortimar",
      dateOfBirth = LocalDate.of(1985, 12, 28),
      conditionalReleaseDateOverrideDate = null,
      sentenceStartDate = LocalDate.of(2018, 10, 22),
      sentenceExpiryDate = LocalDate.of(2021, 10, 22),
      topUpSupervisionStartDate = null,
      croNumber = null,
    )

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
  }
}