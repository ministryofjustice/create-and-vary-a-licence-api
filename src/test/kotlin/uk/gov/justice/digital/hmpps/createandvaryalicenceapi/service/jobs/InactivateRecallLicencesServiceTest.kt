package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.time.LocalDate

class InactivateRecallLicencesServiceTest {
  private val licenceRepository = mock<LicenceRepository>()
  private val licenceService = mock<LicenceService>()

  private val service = InactivateRecallLicencesService(
    licenceRepository,
    licenceService,
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
      licenceService,
    )
  }

  @Test
  fun `should inactivate active licence with a PRRD of today`() {
    val licences = listOf(
      aLicence.copy(
        statusCode = LicenceStatus.ACTIVE,
        postRecallReleaseDate = LocalDate.now(),
      ),
    )
    whenever(licenceRepository.getActiveAndVariedLicencesWhichAreNowRecalls()).thenReturn(
      licences,
    )

    service.inactivateLicences()

    verify(licenceRepository, times(1)).getActiveAndVariedLicencesWhichAreNowRecalls()
  }

  @Test
  fun `should inactivate active licence with a PRRD of tomorrow`() {
    val licences = listOf(
      aLicence.copy(
        statusCode = LicenceStatus.ACTIVE,
        postRecallReleaseDate = LocalDate.now().plusDays(1),
      ),
    )
    whenever(licenceRepository.getActiveAndVariedLicencesWhichAreNowRecalls()).thenReturn(
      licences,
    )

    service.inactivateLicences()

    verify(licenceRepository, times(1)).getActiveAndVariedLicencesWhichAreNowRecalls()
  }

  @Test
  fun `should inactivate active and variation licence with a PRRD of today`() {
    val licences = listOf(
      aLicence.copy(
        id = 1L,
        statusCode = LicenceStatus.ACTIVE,
        postRecallReleaseDate = LocalDate.now().plusDays(1),
      ),
      aVariationLicence.copy(
        variationOfId = 1L,
        statusCode = LicenceStatus.VARIATION_IN_PROGRESS,
        postRecallReleaseDate = LocalDate.now().plusDays(1),
      ),
    )
    whenever(licenceRepository.getActiveAndVariedLicencesWhichAreNowRecalls()).thenReturn(
      licences,
    )

    service.inactivateLicences()

    verify(licenceRepository, times(1)).getActiveAndVariedLicencesWhichAreNowRecalls()
  }

  @Test
  fun `should inactivate active and variation licence with a PRRD of tomorrow`() {
    val licences = listOf(
      aLicence.copy(
        id = 1L,
        statusCode = LicenceStatus.ACTIVE,
        postRecallReleaseDate = LocalDate.now().plusDays(1),
      ),
      aVariationLicence.copy(
        variationOfId = 1L,
        statusCode = LicenceStatus.VARIATION_APPROVED,
        postRecallReleaseDate = LocalDate.now().plusDays(1),
      ),
    )
    whenever(licenceRepository.getActiveAndVariedLicencesWhichAreNowRecalls()).thenReturn(
      licences,
    )

    service.inactivateLicences()

    verify(licenceRepository, times(1)).getActiveAndVariedLicencesWhichAreNowRecalls()
  }

  private companion object {
    val aLicence = TestData.createCrdLicence().copy()
    val aVariationLicence = TestData.createVariationLicence().copy()
  }
}
