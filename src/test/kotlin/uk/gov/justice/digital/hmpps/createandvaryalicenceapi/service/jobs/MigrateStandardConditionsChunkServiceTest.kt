package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs

import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.AuditService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createCrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.someEntityStandardConditions
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.LicencePolicyService

class MigrateStandardConditionsChunkServiceTest {
  private val auditService = mock<AuditService>()
  private val licencePolicyService = mock<LicencePolicyService>()
  private val licenceRepository = mock<LicenceRepository>()

  private val service = MigrateStandardConditionsChunkService(auditService, licencePolicyService, licenceRepository)

  @Test
  fun `Should update licences standard conditions to requested version`() {
    val licenceIds = listOf(1L, 2L, 3L)
    val licences = licenceIds.map { createCrdLicence().copy(id = it) }
    val version = "4.0"

    whenever(licenceRepository.findAllById(licenceIds)).thenReturn(licences)
    licences.forEach {
      whenever(licencePolicyService.getStandardConditionsForLicence(it, version)).thenReturn(
        someEntityStandardConditions(it),
      )
    }

    service.migrateStandardConditions(licenceIds, version)

    licences.forEach {
      verify(licenceRepository).saveAndFlush(it)
      verify(auditService).recordAuditEventUpdateStandardCondition(it, version, null)
    }
  }

  @Test
  fun `Does not update conditions if they are already on the requested version`() {
    val licenceIds = listOf(1L, 2L, 3L)
    val licences = licenceIds.map { createCrdLicence().copy(id = it) }
    val version = licences[0].version!!

    whenever(licenceRepository.findAllById(licenceIds)).thenReturn(licences)
    licences.forEach {
      whenever(licencePolicyService.getStandardConditionsForLicence(it, version)).thenReturn(
        someEntityStandardConditions(it),
      )
    }

    service.migrateStandardConditions(licenceIds, version)

    verify(licenceRepository, times(0)).saveAndFlush(any())
    verify(auditService, times(0)).recordAuditEventUpdateStandardCondition(any(), any(), any())
  }
}
