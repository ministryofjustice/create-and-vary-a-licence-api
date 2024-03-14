package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.PrisonUser
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.UpdateComRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.UpdatePrisonUserRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StaffRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.TeamCountsDto
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.CommunityApiClient

class StaffServiceTest {
  private val staffRepository = mock<StaffRepository>()
  private val communityApiClient = mock<CommunityApiClient>()
  private val licenceRepository = mock<LicenceRepository>()

  private val service =
    StaffService(
      staffRepository,
      communityApiClient,
      licenceRepository,
    )

  @BeforeEach
  fun reset() {
    reset(staffRepository, communityApiClient, licenceRepository)
    whenever(staffRepository.saveAndFlush(any())).thenAnswer { it.arguments[0] }
  }

  @Nested
  inner class `COM tests` {
    @Test
    fun `updates existing COM with new details`() {
      val expectedCom = CommunityOffenderManager(
        staffIdentifier = 3000,
        username = "JBLOGGS",
        email = "jbloggs123@probation.gov.uk",
        firstName = "X",
        lastName = "Y",
      )

      whenever(staffRepository.findByStaffIdentifierOrUsernameIgnoreCase(any(), any()))
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

      val comDetails = UpdateComRequest(
        staffIdentifier = 3000,
        staffUsername = "jbloggs",
        staffEmail = "jbloggs123@probation.gov.uk",
        firstName = "X",
        lastName = "Y",
      )

      service.updateComDetails(comDetails)

      argumentCaptor<CommunityOffenderManager>().apply {
        verify(staffRepository, times(1)).findByStaffIdentifierOrUsernameIgnoreCase(3000, "jbloggs")
        verify(staffRepository, times(1)).saveAndFlush(capture())

        assertThat(firstValue).usingRecursiveComparison().ignoringFields("lastUpdatedTimestamp")
          .isEqualTo(expectedCom)
      }
    }

    @Test
    fun `does not update COM with same details`() {
      whenever(staffRepository.findByStaffIdentifierOrUsernameIgnoreCase(any(), any()))
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

      val comDetails = UpdateComRequest(
        staffIdentifier = 2000,
        staffUsername = "JOEBLOGGS",
        staffEmail = "jbloggs123@probation.gov.uk",
        firstName = "X",
        lastName = "Y",
      )

      service.updateComDetails(comDetails)

      verify(staffRepository, times(1)).findByStaffIdentifierOrUsernameIgnoreCase(2000, "JOEBLOGGS")
      verify(staffRepository, times(0)).saveAndFlush(any())
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
        staffRepository.findByStaffIdentifierOrUsernameIgnoreCase(
          any(),
          any(),
        ),
      ).thenReturn(null)

      val comDetails = UpdateComRequest(
        staffIdentifier = 3000,
        staffUsername = "jbloggs",
        staffEmail = "jbloggs123@probation.gov.uk",
        firstName = "X",
        lastName = "Y",
      )

      service.updateComDetails(comDetails)

      verify(staffRepository, times(1)).findByStaffIdentifierOrUsernameIgnoreCase(3000, "jbloggs")
      verify(staffRepository, times(1)).saveAndFlush(expectedCom)
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

      whenever(staffRepository.findByStaffIdentifierOrUsernameIgnoreCase(any(), any()))
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

      val comDetails = UpdateComRequest(
        staffIdentifier = 2001,
        staffUsername = "JOEBLOGGS",
        staffEmail = "jbloggs123@probation.gov.uk",
        firstName = "X",
        lastName = "Y",
      )

      service.updateComDetails(comDetails)

      argumentCaptor<CommunityOffenderManager>().apply {
        verify(staffRepository, times(1)).findByStaffIdentifierOrUsernameIgnoreCase(2001, "JOEBLOGGS")
        verify(staffRepository, times(1)).saveAndFlush(capture())

        assertThat(firstValue).usingRecursiveComparison().ignoringFields("lastUpdatedTimestamp")
          .isEqualTo(expectedCom)
      }
    }

    @Test
    fun `retrieves review counts for COM and their teams`() {
      val com = CommunityOffenderManager(
        staffIdentifier = 3000,
        username = "JBLOGGS",
        email = "jbloggs123@probation.gov.uk",
        firstName = "X",
        lastName = "Y",
      )

      val teamCodes = listOf(
        "A01B02",
      )

      val teamCountsDto = listOf(
        TeamCountsDto(
          teamCode = teamCodes.first(),
          count = 1L,
        ),
      )
      whenever(staffRepository.findByStaffIdentifier(any())).thenReturn(com)
      whenever(communityApiClient.getTeamsCodesForUser(any())).thenReturn(teamCodes)
      whenever(licenceRepository.getLicenceReviewCountForCom(any())).thenReturn(1L)
      whenever(licenceRepository.getLicenceReviewCountForTeams(any())).thenReturn(teamCountsDto)

      val result = service.getReviewCounts(com.staffIdentifier)

      verify(staffRepository, times(1)).findByStaffIdentifier(3000)
      verify(communityApiClient, times(1)).getTeamsCodesForUser(3000)
      verify(licenceRepository, times(1)).getLicenceReviewCountForCom(com)
      verify(licenceRepository, times(1)).getLicenceReviewCountForTeams(teamCodes)

      assertThat(result.myCount).isEqualTo(1L)
      assertThat(result.teams.size).isEqualTo(1)

      val team = result.teams.first()
      assertThat(team.teamCode).isEqualTo(teamCodes.first())
      assertThat(team.count).isEqualTo(1L)
    }

    @Test
    fun `when getting review counts, an error is thrown if the COM cannot be found`() {
      whenever(staffRepository.findByStaffIdentifier(any())).thenReturn(null)

      val exception = assertThrows<IllegalStateException> {
        service.getReviewCounts(3000)
      }

      verify(staffRepository, times(1)).findByStaffIdentifier(3000)
      verifyNoInteractions(communityApiClient)
      verifyNoInteractions(licenceRepository)

      assertThat(exception).isInstanceOf(IllegalStateException::class.java)
        .hasMessage("Staff with identifier 3000 not found")
    }
  }

  @Nested
  inner class `CA tests` {
    @Test
    fun `updates existing CA with new details`() {
      val expectedCa = PrisonUser(
        username = "JBLOGGS",
        email = "jbloggs123@probation.gov.uk",
        firstName = "X",
        lastName = "Y",
      )

      whenever(staffRepository.findPrisonUserByUsernameIgnoreCase(expectedCa.username))
        .thenReturn(
          PrisonUser(
            username = "joebloggs",
            email = "jbloggs@probation.gov.uk",
            firstName = "A",
            lastName = "B",
          ),
        )

      service.updatePrisonUser(
        UpdatePrisonUserRequest(
          staffUsername = "jbloggs",
          staffEmail = "jbloggs123@probation.gov.uk",
          firstName = "X",
          lastName = "Y",
        ),
      )

      argumentCaptor<PrisonUser>().apply {
        verify(staffRepository, times(1)).saveAndFlush(capture())
        assertThat(firstValue).usingRecursiveComparison().ignoringFields("lastUpdatedTimestamp")
          .isEqualTo(expectedCa)
      }
    }

    @Test
    fun `does not update CA with same details`() {
      whenever(staffRepository.findPrisonUserByUsernameIgnoreCase(any()))
        .thenReturn(
          PrisonUser(
            username = "jbloggs",
            email = "jbloggs@probation.gov.uk",
            firstName = "A",
            lastName = "B",
          ),
        )

      service.updatePrisonUser(
        UpdatePrisonUserRequest(
          staffUsername = "jbloggs",
          staffEmail = "jbloggs@probation.gov.uk",
          firstName = "A",
          lastName = "B",
        ),
      )

      verify(staffRepository, times(1)).findPrisonUserByUsernameIgnoreCase("jbloggs")
      verify(staffRepository, never()).saveAndFlush(any())
    }

    @Test
    fun `adds a new existing COM if it doesnt exist`() {
      val expectedCa = PrisonUser(
        username = "JBLOGGS",
        email = "jbloggs123@probation.gov.uk",
        firstName = "X",
        lastName = "Y",
      )

      whenever(staffRepository.findPrisonUserByUsernameIgnoreCase(expectedCa.username)).thenReturn(null)

      service.updatePrisonUser(
        UpdatePrisonUserRequest(
          staffUsername = "jbloggs",
          staffEmail = "jbloggs123@probation.gov.uk",
          firstName = "X",
          lastName = "Y",
        ),
      )

      argumentCaptor<PrisonUser>().apply {
        verify(staffRepository, times(1)).saveAndFlush(capture())

        assertThat(firstValue).usingRecursiveComparison().ignoringFields("lastUpdatedTimestamp")
          .isEqualTo(expectedCa)
      }
    }
  }
}
