package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.UpdateComRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.CommunityOffenderManagerRepository

class ComServiceTest {
  private val communityOffenderManagerRepository = mock<CommunityOffenderManagerRepository>()

  private val service = ComService(communityOffenderManagerRepository)

  @BeforeEach
  fun reset() {
    reset(communityOffenderManagerRepository)
  }

  @Test
  fun `updates existing COM with new details - email only`() {
    val expectedCom = CommunityOffenderManager(
      staffIdentifier = 2000,
      username = "joebloggs",
      email = "jbloggs123@probation.gov.uk",
      firstName = "X",
      lastName = "Y",
    )

    whenever(communityOffenderManagerRepository.findByStaffIdentifierOrUsername(any(), any()))
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

    verify(communityOffenderManagerRepository, times(1)).findByStaffIdentifierOrUsername(3000, "jbloggs")
    verify(communityOffenderManagerRepository, times(1)).saveAndFlush(any())
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

    whenever(communityOffenderManagerRepository.findByStaffIdentifierOrUsername(any(), any()))
      .thenReturn(listOf(CommunityOffenderManager(staffIdentifier = 2000, username = "joebloggs", email = "jbloggs123@probation.gov.uk", firstName = "X", lastName = "Y")))

    whenever(communityOffenderManagerRepository.saveAndFlush(any())).thenReturn(expectedCom)

    val comDetails = UpdateComRequest(
      staffIdentifier = 2000,
      staffUsername = "joebloggs",
      staffEmail = "jbloggs123@probation.gov.uk",
      firstName = "X",
      lastName = "Y",
    )

    service.updateComDetails(comDetails)

    verify(communityOffenderManagerRepository, times(1)).findByStaffIdentifierOrUsername(2000, "joebloggs")
    verify(communityOffenderManagerRepository, times(0)).saveAndFlush(any())
  }

  @Test
  fun `adds a new existing COM if it doesnt exist`() {
    val expectedCom = CommunityOffenderManager(
      staffIdentifier = 3000,
      username = "jbloggs",
      email = "jbloggs123@probation.gov.uk",
      firstName = "X",
      lastName = "Y",
    )

    whenever(communityOffenderManagerRepository.findByStaffIdentifierOrUsername(any(), any())).thenReturn(null)
    whenever(communityOffenderManagerRepository.saveAndFlush(any())).thenReturn(expectedCom)

    val comDetails = UpdateComRequest(
      staffIdentifier = 3000,
      staffUsername = "jbloggs",
      staffEmail = "jbloggs123@probation.gov.uk",
      firstName = "X",
      lastName = "Y",
    )

    service.updateComDetails(comDetails)

    verify(communityOffenderManagerRepository, times(1)).findByStaffIdentifierOrUsername(3000, "jbloggs")
    verify(communityOffenderManagerRepository, times(1)).saveAndFlush(expectedCom)
  }

  @Test
  fun `updates existing COM with new username`() {
    val expectedCom = CommunityOffenderManager(
      staffIdentifier = 2000,
      username = "joebloggs",
      email = "jbloggs123@probation.gov.uk",
      firstName = "X",
      lastName = "Y",
    )

    whenever(communityOffenderManagerRepository.findByStaffIdentifierOrUsername(any(), any()))
      .thenReturn(listOf(CommunityOffenderManager(staffIdentifier = 2000, username = "joebloggs", email = "jbloggs@probation.gov.uk", firstName = "A", lastName = "B")))

    whenever(communityOffenderManagerRepository.saveAndFlush(any())).thenReturn(expectedCom)

    val comDetails = UpdateComRequest(
      staffIdentifier = 2000,
      staffUsername = "jbloggsnew1",
      staffEmail = "jbloggs123@probation.gov.uk",
      firstName = "X",
      lastName = "Y",
    )

    service.updateComDetails(comDetails)

    verify(communityOffenderManagerRepository, times(1)).findByStaffIdentifierOrUsername(2000, "jbloggsnew1")
    verify(communityOffenderManagerRepository, times(1)).saveAndFlush(any())
  }

  @Test
  fun `updates existing COM with new staffIdentifier`() {
    val expectedCom = CommunityOffenderManager(
      staffIdentifier = 2001,
      username = "joebloggs",
      email = "jbloggs123@probation.gov.uk",
      firstName = "X",
      lastName = "Y",
    )

    whenever(communityOffenderManagerRepository.findByStaffIdentifierOrUsername(any(), any()))
      .thenReturn(listOf(CommunityOffenderManager(staffIdentifier = 2000, username = "joebloggs", email = "jbloggs@probation.gov.uk", firstName = "A", lastName = "B")))

    whenever(communityOffenderManagerRepository.saveAndFlush(any())).thenReturn(expectedCom)

    val comDetails = UpdateComRequest(
      staffIdentifier = 2001,
      staffUsername = "joebloggs",
      staffEmail = "jbloggs123@probation.gov.uk",
      firstName = "X",
      lastName = "Y",
    )

    service.updateComDetails(comDetails)

    verify(communityOffenderManagerRepository, times(1)).findByStaffIdentifierOrUsername(2001, "joebloggs")
    verify(communityOffenderManagerRepository, times(1)).saveAndFlush(any())
  }
}
