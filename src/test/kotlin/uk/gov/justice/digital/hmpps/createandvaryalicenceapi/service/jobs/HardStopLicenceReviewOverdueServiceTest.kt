package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.NotifyService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData

class HardStopLicenceReviewOverdueServiceTest {
  private val licenceRepository = mock<LicenceRepository>()
  private val notifyService = mock<NotifyService>()

  private val service = HardStopLicenceReviewOverdueService(
    hardStopEnabled = true,
    licenceRepository,
    notifyService,
  )

  @BeforeEach
  fun reset() {
    val authentication = mock<Authentication>()
    val securityContext = mock<SecurityContext>()

    whenever(authentication.name).thenReturn("smills")
    whenever(securityContext.authentication).thenReturn(authentication)
    SecurityContextHolder.setContext(securityContext)

    org.mockito.kotlin.reset(
      licenceRepository,
      notifyService,
    )
  }

  @Test
  fun `should not send notifications if there are no eligible licences`() {
    whenever(licenceRepository.getHardStopLicencesNeedingReview()).thenReturn(emptyList())

    service.sendComReviewEmail()

    verify(licenceRepository, times(1)).getHardStopLicencesNeedingReview()
    verify(licenceRepository, times(0)).saveAllAndFlush(emptyList())
  }

  @Test
  fun `should send notifications if there are eligible licences`() {
    whenever(licenceRepository.getHardStopLicencesNeedingReview()).thenReturn(
      listOf(
        aHardStopLicenceEntity,
      ),
    )

    service.sendComReviewEmail()

    verify(licenceRepository, times(1)).getHardStopLicencesNeedingReview()

    verify(notifyService, times(1)).sendHardStopLicenceReviewOverdueEmail(
      aHardStopLicenceEntity.responsibleCom?.email,
      "${aHardStopLicenceEntity.responsibleCom?.firstName} ${aHardStopLicenceEntity.responsibleCom?.lastName}",
      aHardStopLicenceEntity.forename!!,
      aHardStopLicenceEntity.surname!!,
      aHardStopLicenceEntity.crn!!,
      aHardStopLicenceEntity.id.toString(),
    )
  }

  @Test
  fun `should not send notifications if there is no COM attached to the licence`() {
    whenever(licenceRepository.getHardStopLicencesNeedingReview()).thenReturn(
      listOf(
        aHardStopLicenceEntity.copy(
          responsibleCom = null,
        ),
      ),
    )

    service.sendComReviewEmail()

    verify(licenceRepository, times(1)).getHardStopLicencesNeedingReview()

    verifyNoInteractions(notifyService)
  }

  private companion object {
    val aHardStopLicenceEntity = TestData.createHardStopLicence().copy()
  }
}
