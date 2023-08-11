package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.StandardCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.UpdateComRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.ProbationUserSearchRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.CommunityOffenderManagerRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.CommunityApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.Identifiers
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.Manager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.Name
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ProbationSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ProbationSearchResponseResult
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.Team
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.request.ProbationSearchSortByRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.ProbationSearchSortBy
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.SearchDirection
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.SearchField
import java.time.LocalDate
import java.time.LocalDateTime

class ComServiceTest {
  private val communityOffenderManagerRepository = mock<CommunityOffenderManagerRepository>()
  private val licenceRepository = mock<LicenceRepository>()
  private val communityApiClient = mock<CommunityApiClient>()
  private val probationSearchApiClient = mock<ProbationSearchApiClient>()

  private val service =
    ComService(communityOffenderManagerRepository, licenceRepository, communityApiClient, probationSearchApiClient)

  @BeforeEach
  fun reset() {
    reset(communityOffenderManagerRepository, licenceRepository, communityApiClient, probationSearchApiClient)
  }

  @Test
  fun `updates existing COM with new details`() {
    val expectedCom = CommunityOffenderManager(
      staffIdentifier = 3000,
      username = "JBLOGGS",
      email = "jbloggs123@probation.gov.uk",
      firstName = "X",
      lastName = "Y",
    )

    val comCaptor = ArgumentCaptor.forClass(CommunityOffenderManager::class.java)

    whenever(communityOffenderManagerRepository.findByStaffIdentifierOrUsernameIgnoreCase(any(), any()))
      .thenReturn(
        listOf(
          CommunityOffenderManager(
            staffIdentifier = 2000,
            username = "joebloggs",
            email = "jbloggs@probation.gov.uk",
            firstName = "A",
            lastName = "B",
          ),
        ),
      )

    whenever(communityOffenderManagerRepository.saveAndFlush(any())).thenReturn(expectedCom)

    val comDetails = UpdateComRequest(
      staffIdentifier = 3000,
      staffUsername = "jbloggs",
      staffEmail = "jbloggs123@probation.gov.uk",
      firstName = "X",
      lastName = "Y",
    )

    service.updateComDetails(comDetails)

    verify(communityOffenderManagerRepository, times(1)).findByStaffIdentifierOrUsernameIgnoreCase(3000, "jbloggs")
    verify(communityOffenderManagerRepository, times(1)).saveAndFlush(comCaptor.capture())

    assertThat(comCaptor.value).usingRecursiveComparison().ignoringFields("lastUpdatedTimestamp").isEqualTo(expectedCom)
  }

  @Test
  fun `does not update COM with same details`() {
    val expectedCom = CommunityOffenderManager(
      staffIdentifier = 2000,
      username = "joebloggs",
      email = "jbloggs123@probation.gov.uk",
      firstName = "X",
      lastName = "Y",
    )

    whenever(communityOffenderManagerRepository.findByStaffIdentifierOrUsernameIgnoreCase(any(), any()))
      .thenReturn(
        listOf(
          CommunityOffenderManager(
            staffIdentifier = 2000,
            username = "joebloggs",
            email = "jbloggs123@probation.gov.uk",
            firstName = "X",
            lastName = "Y",
          ),
        ),
      )

    whenever(communityOffenderManagerRepository.saveAndFlush(any())).thenReturn(expectedCom)

    val comDetails = UpdateComRequest(
      staffIdentifier = 2000,
      staffUsername = "JOEBLOGGS",
      staffEmail = "jbloggs123@probation.gov.uk",
      firstName = "X",
      lastName = "Y",
    )

    service.updateComDetails(comDetails)

    verify(communityOffenderManagerRepository, times(1)).findByStaffIdentifierOrUsernameIgnoreCase(2000, "JOEBLOGGS")
    verify(communityOffenderManagerRepository, times(0)).saveAndFlush(any())
  }

  @Test
  fun `adds a new existing COM if it doesnt exist`() {
    val expectedCom = CommunityOffenderManager(
      staffIdentifier = 3000,
      username = "JBLOGGS",
      email = "jbloggs123@probation.gov.uk",
      firstName = "X",
      lastName = "Y",
    )

    whenever(
      communityOffenderManagerRepository.findByStaffIdentifierOrUsernameIgnoreCase(
        any(),
        any(),
      ),
    ).thenReturn(null)
    whenever(communityOffenderManagerRepository.saveAndFlush(any())).thenReturn(expectedCom)

    val comDetails = UpdateComRequest(
      staffIdentifier = 3000,
      staffUsername = "jbloggs",
      staffEmail = "jbloggs123@probation.gov.uk",
      firstName = "X",
      lastName = "Y",
    )

    service.updateComDetails(comDetails)

    verify(communityOffenderManagerRepository, times(1)).findByStaffIdentifierOrUsernameIgnoreCase(3000, "jbloggs")
    verify(communityOffenderManagerRepository, times(1)).saveAndFlush(expectedCom)
  }

  @Test
  fun `updates existing COM with new username and forces it to be uppercase`() {
    val expectedCom = CommunityOffenderManager(
      staffIdentifier = 2000,
      username = "JBLOGGSNEW1",
      email = "jbloggs123@probation.gov.uk",
      firstName = "X",
      lastName = "Y",
    )

    val comCaptor = ArgumentCaptor.forClass(CommunityOffenderManager::class.java)

    whenever(communityOffenderManagerRepository.findByStaffIdentifierOrUsernameIgnoreCase(any(), any()))
      .thenReturn(
        listOf(
          CommunityOffenderManager(
            staffIdentifier = 2000,
            username = "JOEBLOGGS",
            email = "jbloggs@probation.gov.uk",
            firstName = "A",
            lastName = "B",
          ),
        ),
      )

    whenever(communityOffenderManagerRepository.saveAndFlush(any())).thenReturn(expectedCom)

    val comDetails = UpdateComRequest(
      staffIdentifier = 2000,
      staffUsername = "jbloggsnew1",
      staffEmail = "jbloggs123@probation.gov.uk",
      firstName = "X",
      lastName = "Y",
    )

    service.updateComDetails(comDetails)

    verify(communityOffenderManagerRepository, times(1)).findByStaffIdentifierOrUsernameIgnoreCase(2000, "jbloggsnew1")
    verify(communityOffenderManagerRepository, times(1)).saveAndFlush(comCaptor.capture())

    assertThat(comCaptor.value).usingRecursiveComparison().ignoringFields("lastUpdatedTimestamp").isEqualTo(expectedCom)
  }

  @Test
  fun `updates existing COM with new staffIdentifier`() {
    val expectedCom = CommunityOffenderManager(
      staffIdentifier = 2001,
      username = "JOEBLOGGS",
      email = "jbloggs123@probation.gov.uk",
      firstName = "X",
      lastName = "Y",
    )

    val comCaptor = ArgumentCaptor.forClass(CommunityOffenderManager::class.java)

    whenever(communityOffenderManagerRepository.findByStaffIdentifierOrUsernameIgnoreCase(any(), any()))
      .thenReturn(
        listOf(
          CommunityOffenderManager(
            staffIdentifier = 2000,
            username = "JOEBLOGGS",
            email = "jbloggs@probation.gov.uk",
            firstName = "A",
            lastName = "B",
          ),
        ),
      )

    whenever(communityOffenderManagerRepository.saveAndFlush(any())).thenReturn(expectedCom)

    val comDetails = UpdateComRequest(
      staffIdentifier = 2001,
      staffUsername = "JOEBLOGGS",
      staffEmail = "jbloggs123@probation.gov.uk",
      firstName = "X",
      lastName = "Y",
    )

    service.updateComDetails(comDetails)

    verify(communityOffenderManagerRepository, times(1)).findByStaffIdentifierOrUsernameIgnoreCase(2001, "JOEBLOGGS")
    verify(communityOffenderManagerRepository, times(1)).saveAndFlush(comCaptor.capture())

    assertThat(comCaptor.value).usingRecursiveComparison().ignoringFields("lastUpdatedTimestamp").isEqualTo(expectedCom)
  }

  @Test
  fun `search for offenders in prison on a staff member's caseload`() {
    whenever(communityApiClient.getTeamsCodesForUser(2000)).thenReturn(
      listOf(
        "A01B02",
      ),
    )
    whenever(probationSearchApiClient.searchLicenceCaseloadByTeam("Test", listOf("A01B02"))).thenReturn(
      listOf(
        ProbationSearchResponseResult(
          Name("Test", "Surname"),
          Identifiers("A123456"),
          Manager(
            "A01B02C",
            Name("Staff", "Surname"),
            Team("A01B02", "Test Team"),
          ),
          "2023/05/24",
        ),
      ),
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn((listOf(aLicenceEntity)))

    val request = ProbationUserSearchRequest(
      "Test",
      2000,
    )

    val result = service.searchForOffenderOnStaffCaseload(request)

    verify(probationSearchApiClient).searchLicenceCaseloadByTeam(
      request.query,
      communityApiClient.getTeamsCodesForUser(request.staffIdentifier),
    )

    val resultsList = result.results
    val offender = resultsList.first()
    val inPrisonCount = result.inPrisonCount
    val onProbationCount = result.onProbationCount

    assertThat(resultsList).isNotEmpty
    assertThat(resultsList.size).isEqualTo(1)

    assertThat(offender)
      .extracting { Tuple.tuple(it.name, it.comName, it.teamName, it.releaseDate, it.licenceStatus, it.isOnProbation) }
      .isEqualTo(
        Tuple.tuple(
          "Test Surname",
          "Staff Surname",
          "Test Team",
          LocalDate.parse("2021-10-22"),
          LicenceStatus.IN_PROGRESS,
          false,
        ),
      )

    assertThat(inPrisonCount).isEqualTo(1)
    assertThat(onProbationCount).isEqualTo(0)
  }

  @Test
  fun `search for offenders in prison on a staff member's caseload with results sorted`() {
    whenever(communityApiClient.getTeamsCodesForUser(2000)).thenReturn(
      listOf(
        "A01B02",
      ),
    )
    whenever(
      probationSearchApiClient.searchLicenceCaseloadByTeam(
        "Test",
        listOf("A01B02"),
        listOf(
          ProbationSearchSortByRequest(SearchField.SURNAME.probationSearchApiSortType, "asc"),
          ProbationSearchSortByRequest(SearchField.COM_FORENAME.probationSearchApiSortType, "desc"),
        ),
      ),
    ).thenReturn(
      listOf(
        ProbationSearchResponseResult(
          Name("Test", "Surname"),
          Identifiers("A123456"),
          Manager(
            "A01B02C",
            Name("Staff", "Surname"),
            Team("A01B02", "Test Team"),
          ),
          "2023/05/24",
        ),
      ),
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn((listOf(aLicenceEntity)))

    val request = ProbationUserSearchRequest(
      "Test",
      2000,
      listOf(
        ProbationSearchSortBy(SearchField.SURNAME, SearchDirection.ASC),
        ProbationSearchSortBy(SearchField.COM_FORENAME, SearchDirection.DESC),
      ),
    )

    val result = service.searchForOffenderOnStaffCaseload(request)

    verify(probationSearchApiClient).searchLicenceCaseloadByTeam(
      request.query,
      communityApiClient.getTeamsCodesForUser(request.staffIdentifier),
      listOf(
        ProbationSearchSortByRequest(SearchField.SURNAME.probationSearchApiSortType, "asc"),
        ProbationSearchSortByRequest(SearchField.COM_FORENAME.probationSearchApiSortType, "desc"),
      ),
    )

    val resultsList = result.results
    val offender = resultsList.first()

    assertThat(resultsList.size).isEqualTo(1)
    assertThat(offender)
      .extracting { Tuple.tuple(it.name, it.comName, it.teamName) }
      .isEqualTo(
        Tuple.tuple("Test Surname", "Staff Surname", "Test Team"),
      )
  }

  @Test
  fun `search for offenders in prison on a staff member's caseload with latest licence selected`() {
    whenever(communityApiClient.getTeamsCodesForUser(2000)).thenReturn(
      listOf(
        "A01B02",
      ),
    )
    whenever(probationSearchApiClient.searchLicenceCaseloadByTeam("Test", listOf("A01B02"))).thenReturn(
      listOf(
        ProbationSearchResponseResult(
          Name("Test", "Surname"),
          Identifiers("A123456"),
          Manager(
            "A01B02C",
            Name("Staff", "Surname"),
            Team("A01B02", "Test Team"),
          ),
          "2023/05/24",
        ),
      ),
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any()))
      .thenReturn(
        (
          listOf(
            aLicenceEntity.copy(
              statusCode = LicenceStatus.ACTIVE,
            ),
            aLicenceEntity.copy(
              statusCode = LicenceStatus.APPROVED,
            ),
          )
          ),
      )

    val request = ProbationUserSearchRequest(
      "Test",
      2000,
    )

    val result = service.searchForOffenderOnStaffCaseload(request)

    verify(probationSearchApiClient).searchLicenceCaseloadByTeam(
      request.query,
      communityApiClient.getTeamsCodesForUser(request.staffIdentifier),
    )

    val resultsList = result.results
    val offender = resultsList.first()
    val inPrisonCount = result.inPrisonCount
    val onProbationCount = result.onProbationCount

    assertThat(resultsList).isNotEmpty
    assertThat(resultsList.size).isEqualTo(1)

    assertThat(offender)
      .extracting { Tuple.tuple(it.name, it.comName, it.teamName, it.releaseDate, it.licenceStatus, it.isOnProbation) }
      .isEqualTo(
        Tuple.tuple(
          "Test Surname",
          "Staff Surname",
          "Test Team",
          LocalDate.parse("2021-10-22"),
          LicenceStatus.APPROVED,
          false,
        ),
      )

    assertThat(inPrisonCount).isEqualTo(1)
    assertThat(onProbationCount).isEqualTo(0)
  }

  @Test
  fun `search for offenders on a staff member's caseload with an irrelevant licence`() {
    whenever(communityApiClient.getTeamsCodesForUser(2000)).thenReturn(
      listOf(
        "A01B02",
      ),
    )
    whenever(probationSearchApiClient.searchLicenceCaseloadByTeam("Test", listOf("A01B02"))).thenReturn(
      listOf(
        ProbationSearchResponseResult(
          Name("Test", "Surname"),
          Identifiers("A123456"),
          Manager(
            "A01B02C",
            Name("Staff", "Surname"),
            Team("A01B02", "Test Team"),
          ),
          "2023/05/24",
        ),
      ),
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any()))
      .thenReturn(
        emptyList(),
      )

    val request = ProbationUserSearchRequest(
      "Test",
      2000,
    )

    val result = service.searchForOffenderOnStaffCaseload(request)

    verify(probationSearchApiClient).searchLicenceCaseloadByTeam(
      request.query,
      communityApiClient.getTeamsCodesForUser(request.staffIdentifier),
    )

    val resultsList = result.results
    val inPrisonCount = result.inPrisonCount
    val onProbationCount = result.onProbationCount

    assertThat(resultsList).isEmpty()
    assertThat(inPrisonCount).isEqualTo(0)
    assertThat(onProbationCount).isEqualTo(0)
  }

  @Test
  fun `search for offenders on probation on a staff member's caseload with latest licence selected`() {
    whenever(communityApiClient.getTeamsCodesForUser(2000)).thenReturn(
      listOf(
        "A01B02",
      ),
    )
    whenever(probationSearchApiClient.searchLicenceCaseloadByTeam("Test", listOf("A01B02"))).thenReturn(
      listOf(
        ProbationSearchResponseResult(
          Name("Test", "Surname"),
          Identifiers("A123456"),
          Manager(
            "A01B02C",
            Name("Staff", "Surname"),
            Team("A01B02", "Test Team"),
          ),
          "2023/05/24",
        ),
      ),
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any()))
      .thenReturn(
        (
          listOf(
            aLicenceEntity.copy(
              statusCode = LicenceStatus.ACTIVE,
            ),
            aLicenceEntity.copy(
              statusCode = LicenceStatus.VARIATION_SUBMITTED,
            ),
          )
          ),
      )

    val request = ProbationUserSearchRequest(
      "Test",
      2000,
    )

    val result = service.searchForOffenderOnStaffCaseload(request)

    verify(probationSearchApiClient).searchLicenceCaseloadByTeam(
      request.query,
      communityApiClient.getTeamsCodesForUser(request.staffIdentifier),
    )

    val resultsList = result.results
    val offender = resultsList.first()
    val inPrisonCount = result.inPrisonCount
    val onProbationCount = result.onProbationCount

    assertThat(resultsList).isNotEmpty
    assertThat(resultsList.size).isEqualTo(1)

    assertThat(offender)
      .extracting { Tuple.tuple(it.name, it.comName, it.teamName, it.releaseDate, it.licenceStatus, it.isOnProbation) }
      .isEqualTo(
        Tuple.tuple(
          "Test Surname",
          "Staff Surname",
          "Test Team",
          LocalDate.parse("2021-10-22"),
          LicenceStatus.VARIATION_SUBMITTED,
          true,
        ),
      )

    assertThat(inPrisonCount).isEqualTo(0)
    assertThat(onProbationCount).isEqualTo(1)
  }

  @Test
  fun `search for offenders on probation on a staff member's caseload with no CRD`() {
    whenever(communityApiClient.getTeamsCodesForUser(2000)).thenReturn(
      listOf(
        "A01B02",
      ),
    )
    whenever(probationSearchApiClient.searchLicenceCaseloadByTeam("Test", listOf("A01B02"))).thenReturn(
      listOf(
        ProbationSearchResponseResult(
          Name("Test", "Surname"),
          Identifiers("A123456"),
          Manager(
            "A01B02C",
            Name("Staff", "Surname"),
            Team("A01B02", "Test Team"),
          ),
          "2023/05/24",
        ),
      ),
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any()))
      .thenReturn(
        (
          listOf(
            aLicenceEntity.copy(
              statusCode = LicenceStatus.ACTIVE,
            ),
            aLicenceEntity.copy(
              statusCode = LicenceStatus.VARIATION_IN_PROGRESS,
              conditionalReleaseDate = null,
              actualReleaseDate = LocalDate.parse("2023-07-27"),
            ),
          )
          ),
      )

    val request = ProbationUserSearchRequest(
      "Test",
      2000,
    )

    val result = service.searchForOffenderOnStaffCaseload(request)

    verify(probationSearchApiClient).searchLicenceCaseloadByTeam(
      request.query,
      communityApiClient.getTeamsCodesForUser(request.staffIdentifier),
    )

    val resultsList = result.results
    val offender = resultsList.first()
    val inPrisonCount = result.inPrisonCount
    val onProbationCount = result.onProbationCount

    assertThat(resultsList).isNotEmpty
    assertThat(resultsList.size).isEqualTo(1)

    assertThat(offender)
      .extracting { Tuple.tuple(it.name, it.comName, it.teamName, it.releaseDate, it.licenceStatus, it.isOnProbation) }
      .isEqualTo(
        Tuple.tuple(
          "Test Surname",
          "Staff Surname",
          "Test Team",
          LocalDate.parse("2023-07-27"),
          LicenceStatus.VARIATION_IN_PROGRESS,
          true,
        ),
      )

    assertThat(inPrisonCount).isEqualTo(0)
    assertThat(onProbationCount).isEqualTo(1)
  }

  private companion object {

    val someEntityStandardConditions = listOf(
      StandardCondition(
        id = 1,
        conditionCode = "goodBehaviour",
        conditionSequence = 1,
        conditionText = "Be of good behaviour",
        licence = mock(),
      ),
      StandardCondition(
        id = 2,
        conditionCode = "notBreakLaw",
        conditionSequence = 2,
        conditionText = "Do not break any law",
        licence = mock(),
      ),
      StandardCondition(
        id = 3,
        conditionCode = "attendMeetings",
        conditionSequence = 3,
        conditionText = "Attend meetings",
        licence = mock(),
      ),
    )

    val aLicenceEntity = Licence(
      id = 1,
      typeCode = LicenceType.AP,
      version = "1.1",
      statusCode = LicenceStatus.IN_PROGRESS,
      nomsId = "A1234AA",
      bookingNo = "123456",
      bookingId = 54321,
      crn = "A123456",
      pnc = "2019/123445",
      cro = "12345",
      prisonCode = "MDI",
      prisonDescription = "Moorland (HMP)",
      forename = "Test",
      surname = "Surname",
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
      probationTeamCode = "A01B02",
      probationTeamDescription = "Test Team",
      dateCreated = LocalDateTime.of(2022, 7, 27, 15, 0, 0),
      standardConditions = someEntityStandardConditions,
      responsibleCom = CommunityOffenderManager(
        staffIdentifier = 2000,
        username = "ssurname",
        email = "testemail@probation.gov.uk",
        firstName = "Staff",
        lastName = "Surname",
      ),
      createdBy = CommunityOffenderManager(
        staffIdentifier = 2000,
        username = "ssurname",
        email = "testemail@probation.gov.uk",
        firstName = "Staff",
        lastName = "Surname",
      ),
    )
  }
}
