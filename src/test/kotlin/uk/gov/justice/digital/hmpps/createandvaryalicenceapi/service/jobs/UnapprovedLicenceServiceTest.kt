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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.UnapprovedLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.NotifyService

class UnapprovedLicenceServiceTest {
  private val licenceRepository = mock<LicenceRepository>()
  private val notifyService = mock<NotifyService>()
  private val service = UnapprovedLicenceService(
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

    reset(
      licenceRepository,
    )
  }

  @Test
  fun `service sends an email`() {
    val anUnapprovedLicence = listOf(
      UnapprovedLicence(
        crn = "100A",
        forename = "jim",
        surname = "smith",
        comFirstName = "ComF",
        comLastName = "ComL",
        comEmail = "com@gmail.com",
      ),
    )

    whenever(licenceRepository.getEditedLicencesNotReApprovedByCrd()).thenReturn(anUnapprovedLicence)
    service.sendEmailsToProbationPractitioner()
    verify(notifyService, times(1)).sendUnapprovedLicenceEmail(anUnapprovedLicence)
  }
}
