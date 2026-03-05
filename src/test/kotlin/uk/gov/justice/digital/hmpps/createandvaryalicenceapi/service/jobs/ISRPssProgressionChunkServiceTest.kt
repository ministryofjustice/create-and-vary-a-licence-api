package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.ISRProgressionLicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType

class ISRPssProgressionChunkServiceTest {

  private val repository = mock<ISRProgressionLicenceRepository>()

  private val service = ISRPssProgressionChunkService(
    repository,
  )

  @BeforeEach
  fun resetMocks() {
    reset(repository)
  }

  @Test
  fun `should return immediately when licenceIds is empty`() {
    // Given
    val licenceIds = emptyList<Long>()

    // When
    service.processApPssLicenceChunk(licenceIds)

    // Then
    verifyNoInteractions(repository)
  }

  @Test
  fun `should try to delete conditions when update count is zero`() {
    // Given
    val licenceIds = listOf(1L, 2L, 3L)
    whenever(repository.updateTypeCodeToAp(licenceIds, LicenceType.AP_PSS.toString())).thenReturn(0)

    // When
    service.processApPssLicenceChunk(licenceIds)

    // Then
    verify(repository).updateTypeCodeToAp(licenceIds, LicenceType.AP_PSS.toString())
    verify(repository, times(1)).deletePssStandardConditions(licenceIds)
    verify(repository, times(1)).deletePssAdditionalConditions(licenceIds)
  }

  @Test
  fun `should update and delete PSS conditions when update count is greater than zero`() {
    // Given
    val licenceIds = listOf(10L, 20L)

    whenever(repository.updateTypeCodeToAp(licenceIds, LicenceType.AP_PSS.toString())).thenReturn(2)
    whenever(repository.deletePssStandardConditions(licenceIds)).thenReturn(4)
    whenever(repository.deletePssAdditionalConditions(licenceIds)).thenReturn(1)

    // When
    service.processApPssLicenceChunk(licenceIds)

    // Then
    verify(repository).updateTypeCodeToAp(
      licenceIds,
      LicenceType.AP_PSS.toString(),
    )

    verify(repository).deletePssStandardConditions(licenceIds)
    verify(repository).deletePssAdditionalConditions(licenceIds)
  }
}
