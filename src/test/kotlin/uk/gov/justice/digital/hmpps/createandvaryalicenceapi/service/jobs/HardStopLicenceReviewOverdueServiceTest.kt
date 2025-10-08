package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.HardStopLicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.NotifyService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData

class HardStopLicenceReviewOverdueServiceTest {
  private val hardStopLicenceRepository = mock<HardStopLicenceRepository>()
  private val notifyService = mock<NotifyService>()

  private val service = HardStopLicenceReviewOverdueService(
    hardStopLicenceRepository,
    notifyService,
  )

  @BeforeEach
  fun reset() {
    val authentication = mock<Authentication>()
    val securityContext = mock<SecurityContext>()

    whenever(authentication.name).thenReturn("tcom")
    whenever(securityContext.authentication).thenReturn(authentication)
    SecurityContextHolder.setContext(securityContext)

    reset(
      hardStopLicenceRepository,
      notifyService,
    )
  }

  @Test
  fun `should not send notifications if there are no eligible licences`() {
    whenever(hardStopLicenceRepository.getHardStopLicencesNeedingReview()).thenReturn(emptyList())

    service.sendComReviewEmail()

    verify(hardStopLicenceRepository, times(1)).getHardStopLicencesNeedingReview()
    verify(hardStopLicenceRepository, times(0)).saveAllAndFlush(emptyList())
  }

  @Test
  fun `should send notifications if there are eligible licences`() {
    whenever(hardStopLicenceRepository.getHardStopLicencesNeedingReview()).thenReturn(
      listOf(
        aHardStopLicenceEntity,
      ),
    )

    service.sendComReviewEmail()

    verify(hardStopLicenceRepository, times(1)).getHardStopLicencesNeedingReview()

    verify(notifyService, times(1)).sendHardStopLicenceReviewOverdueEmail(
      aHardStopLicenceEntity.getCom().email,
      aHardStopLicenceEntity.getCom().fullName,
      aHardStopLicenceEntity.forename!!,
      aHardStopLicenceEntity.surname!!,
      aHardStopLicenceEntity.crn!!,
      aHardStopLicenceEntity.id.toString(),
    )
  }

  private companion object {
    val aHardStopLicenceEntity = TestData.createHardStopLicence().copy()
  }
}
