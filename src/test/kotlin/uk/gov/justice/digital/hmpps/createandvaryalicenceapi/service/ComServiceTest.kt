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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.UpdateComRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.ProbationUserSearchRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.CommunityOffenderManagerRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.CommunityApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.Identifiers
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.Manager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.Name
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ProbationSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ProbationSearchResult
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.Team

class ComServiceTest {
  private val communityOffenderManagerRepository = mock<CommunityOffenderManagerRepository>()
  private val communityApiClient = mock<CommunityApiClient>()
  private val probationSearchApiClient = mock<ProbationSearchApiClient>()

  private val service = ComService(communityOffenderManagerRepository, communityApiClient, probationSearchApiClient)

  @BeforeEach
  fun reset() {
    reset(communityOffenderManagerRepository, communityApiClient, probationSearchApiClient)
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
      .thenReturn(listOf(CommunityOffenderManager(staffIdentifier = 2000, username = "joebloggs", email = "jbloggs@probation.gov.uk", firstName = "A", lastName = "B")))

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
      .thenReturn(listOf(CommunityOffenderManager(staffIdentifier = 2000, username = "joebloggs", email = "jbloggs123@probation.gov.uk", firstName = "X", lastName = "Y")))

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

    whenever(communityOffenderManagerRepository.findByStaffIdentifierOrUsernameIgnoreCase(any(), any())).thenReturn(null)
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
      .thenReturn(listOf(CommunityOffenderManager(staffIdentifier = 2000, username = "JOEBLOGGS", email = "jbloggs@probation.gov.uk", firstName = "A", lastName = "B")))

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
      .thenReturn(listOf(CommunityOffenderManager(staffIdentifier = 2000, username = "JOEBLOGGS", email = "jbloggs@probation.gov.uk", firstName = "A", lastName = "B")))

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
  fun `search for offenders on a staff member's caseload`() {
    whenever(communityApiClient.getTeamsCodesForUser(2000)).thenReturn(
      listOf(
        "A01B02"
      )
    )
    whenever(probationSearchApiClient.searchLicenceCaseloadByTeam("Test", listOf("A01B02"))).thenReturn(
      listOf(
        ProbationSearchResult(
          Name("Test", "Surname"),
          Identifiers("A123456"),
          Manager(
            "A01B02C",
            Name("Staff", "Surname"),
            Team("A01B02")
          ),
          "2023/05/24"
        )
      )
    )

    val result = service.searchForOffenderOnStaffCaseload(
      ProbationUserSearchRequest(
        "Test",
        2000
      )
    )

    assertThat(result.size).isEqualTo(1)
    assertThat(result)
      .extracting<Tuple> { Tuple.tuple(it.name, it.comName, it.comCode) }
      .containsAll(
        listOf(
          Tuple.tuple("Test Surname", "Staff Surname", "A01B02C"),
        )
      )
  }
}
