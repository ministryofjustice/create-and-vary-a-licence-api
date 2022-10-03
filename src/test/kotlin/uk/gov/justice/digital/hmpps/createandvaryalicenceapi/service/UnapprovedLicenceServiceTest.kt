package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
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

class UnapprovedLicenceServiceTest {
  private val licenceRepository = mock<LicenceRepository>()
  private val service = UnapprovedLicenceService(
    licenceRepository
  )

  @BeforeEach
  fun reset() {
    val authentication = mock<Authentication>()
    val securityContext = mock<SecurityContext>()

    whenever(authentication.name).thenReturn("smills")
    whenever(securityContext.authentication).thenReturn(authentication)
    SecurityContextHolder.setContext(securityContext)

    reset(
      licenceRepository
    )
  }

  @Test
  fun `service returns prisoner and com details for licences not approved by CRD`() {
    whenever(licenceRepository.getEditedLicencesNotReApprovedByCrd()).thenReturn(listOf(anUnapprovedLicence))
    val result = service.getEditedLicencesNotReApprovedByCrd()

    assertThat(result).isEqualTo(listOf(anUnapprovedLicence))
    verify(licenceRepository, times(1)).getEditedLicencesNotReApprovedByCrd()
  }

  val anUnapprovedLicence = UnapprovedLicence(
    crn = "100A",
    forename = "jim",
    surname = "smith",
    comFirstName = "ComF",
    comLastName = "ComL",
    comEmail = "com@gmail.com"
  )
}
