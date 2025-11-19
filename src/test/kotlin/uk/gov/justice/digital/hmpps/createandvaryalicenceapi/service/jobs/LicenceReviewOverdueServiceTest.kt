package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceReviewRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.NotifyService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.TIME_SERVED

class LicenceReviewOverdueServiceTest {
  private val licenceReviewRepository = mock<LicenceReviewRepository>()
  private val notifyService = mock<NotifyService>()

  private val service = LicenceReviewOverdueService(
    licenceReviewRepository,
    notifyService,
    isTimeServedLogicEnabled = true,
  )

  @BeforeEach
  fun reset() {
    val authentication = mock<Authentication>()
    val securityContext = mock<SecurityContext>()

    whenever(authentication.name).thenReturn("tcom")
    whenever(securityContext.authentication).thenReturn(authentication)
    SecurityContextHolder.setContext(securityContext)

    reset(
      licenceReviewRepository,
      notifyService,
    )
  }

  @Test
  fun `should not send notifications if there are no eligible licences`() {
    whenever(
      licenceReviewRepository.getLicencesNeedingReview(
        start = any(),
        end = any(),
      ),
    ).thenReturn(emptyList())

    service.sendComReviewEmail()

    verify(licenceReviewRepository, times(1)).getLicencesNeedingReview(
      start = any(),
      end = any(),
    )
    verify(licenceReviewRepository, times(0)).saveAllAndFlush(emptyList())
  }

  @Test
  fun `should send notifications if there are eligible licences`() {
    whenever(
      licenceReviewRepository.getLicencesNeedingReview(
        start = any(),
        end = any(),
      ),
    ).thenReturn(
      listOf(
        aLicenceReviewEntity,
      ),
    )

    service.sendComReviewEmail()

    verify(licenceReviewRepository, times(1)).getLicencesNeedingReview(
      start = any(),
      end = any(),
    )

    verify(notifyService, times(1)).sendLicenceReviewOverdueEmail(
      aLicenceReviewEntity.getCom().email,
      aLicenceReviewEntity.getCom().fullName,
      aLicenceReviewEntity.forename!!,
      aLicenceReviewEntity.surname!!,
      aLicenceReviewEntity.crn!!,
      aLicenceReviewEntity.id.toString(),
      aLicenceReviewEntity.kind == TIME_SERVED,
    )
  }

  private companion object {
    val aLicenceReviewEntity = TestData.createHardStopLicence().copy()
  }
}
