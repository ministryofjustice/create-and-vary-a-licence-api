package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createCrdLicence
import java.time.LocalDate

class LicenceExpiryServiceTest {
  private val licenceRepository = mock<LicenceRepository>()
  private val licenceService = mock<LicenceService>()

  private val service = LicenceExpiryService(
    licenceRepository,
    licenceService,
  )

  @BeforeEach
  fun reset() {
    reset(
      licenceRepository,
    )
  }

  @Test
  fun `return if no licences to deactivate`() {
    whenever(licenceRepository.getLicencesPassedExpiryDate()).thenReturn(emptyList())

    service.expireLicences()

    verify(licenceRepository, times(0)).saveAndFlush(any())
  }

  @Test
  fun `expire licences job runs successfully`() {
    val licences = listOf(
      aLicence,
    )
    whenever(licenceRepository.getLicencesPassedExpiryDate()).thenReturn(licences)

    service.expireLicences()

    verify(licenceRepository, times(1)).getLicencesPassedExpiryDate()
    verify(licenceService, times(1)).inactivateLicences(
      licences = licences,
      reason = "Licence inactivated due to passing expiry date",
    )
  }

  private companion object {
    val aLicence = createCrdLicence().copy(
      topupSupervisionExpiryDate = LocalDate.now().minusDays(1),
    )
  }
}
