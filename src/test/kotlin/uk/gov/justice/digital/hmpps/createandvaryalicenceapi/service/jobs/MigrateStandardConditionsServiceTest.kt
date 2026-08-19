package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.ISRProgressionLicenceRepository

class MigrateStandardConditionsServiceTest {
  private val isrProgressionLicenceRepository = mock<ISRProgressionLicenceRepository>()
  private val migrateStandardConditionsChunkService = mock<MigrateStandardConditionsChunkService>()

  private val service =
    MigrateStandardConditionsService(isrProgressionLicenceRepository, migrateStandardConditionsChunkService)

  @Test
  fun `update standard conditions for an individual licence to the requested version`() {
    val version = "4.0"
    val licenceIds = listOf(1L, 2L, 3L)

    whenever(isrProgressionLicenceRepository.findInFlightLicenceIds()).thenReturn(licenceIds)

    service.migrateStandardConditions(version)

    verify(migrateStandardConditionsChunkService).migrateStandardConditions(licenceIds, version)
  }
}
