package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.PrisonCaseAdministrator
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.UpdateComRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.UpdatePrisonCaseAdminRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StaffRepository

class StaffServiceTest {
  private val staffRepository = mock<StaffRepository>()

  private val service = StaffService(staffRepository)

  @BeforeEach
  fun reset() {
    reset(staffRepository)
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
  }

  @Nested
  inner class `CA tests` {
    @Test
    fun `updates existing CA with new details`() {
      val expectedCa = PrisonCaseAdministrator(
        username = "JBLOGGS",
        email = "jbloggs123@probation.gov.uk",
        firstName = "X",
        lastName = "Y",
      )

      whenever(staffRepository.findPrisonCaseAdministratorByUsernameIgnoreCase(expectedCa.username))
        .thenReturn(
          PrisonCaseAdministrator(
            username = "joebloggs",
            email = "jbloggs@probation.gov.uk",
            firstName = "A",
            lastName = "B",
          ),
        )

      service.updatePrisonCaseAdmin(
        UpdatePrisonCaseAdminRequest(
          staffUsername = "jbloggs",
          staffEmail = "jbloggs123@probation.gov.uk",
          firstName = "X",
          lastName = "Y",
        ),
      )

      argumentCaptor<PrisonCaseAdministrator>().apply {
        verify(staffRepository, times(1)).saveAndFlush(capture())
        assertThat(firstValue).usingRecursiveComparison().ignoringFields("lastUpdatedTimestamp")
          .isEqualTo(expectedCa)
      }
    }

    @Test
    fun `does not update CA with same details`() {
      whenever(staffRepository.findPrisonCaseAdministratorByUsernameIgnoreCase(any()))
        .thenReturn(
          PrisonCaseAdministrator(
            username = "jbloggs",
            email = "jbloggs@probation.gov.uk",
            firstName = "A",
            lastName = "B",
          ),
        )

      service.updatePrisonCaseAdmin(
        UpdatePrisonCaseAdminRequest(
          staffUsername = "jbloggs",
          staffEmail = "jbloggs@probation.gov.uk",
          firstName = "A",
          lastName = "B",
        ),
      )

      verify(staffRepository, times(1)).findPrisonCaseAdministratorByUsernameIgnoreCase("jbloggs")
      verify(staffRepository, never()).saveAndFlush(any())
    }

    @Test
    fun `adds a new existing COM if it doesnt exist`() {
      val expectedCa = PrisonCaseAdministrator(
        username = "JBLOGGS",
        email = "jbloggs123@probation.gov.uk",
        firstName = "X",
        lastName = "Y",
      )

      whenever(staffRepository.findPrisonCaseAdministratorByUsernameIgnoreCase(expectedCa.username)).thenReturn(null)

      service.updatePrisonCaseAdmin(
        UpdatePrisonCaseAdminRequest(
          staffUsername = "jbloggs",
          staffEmail = "jbloggs123@probation.gov.uk",
          firstName = "X",
          lastName = "Y",
        ),
      )

      argumentCaptor<PrisonCaseAdministrator>().apply {
        verify(staffRepository, times(1)).saveAndFlush(capture())

        assertThat(firstValue).usingRecursiveComparison().ignoringFields("lastUpdatedTimestamp")
          .isEqualTo(expectedCa)
      }
    }
  }
}
