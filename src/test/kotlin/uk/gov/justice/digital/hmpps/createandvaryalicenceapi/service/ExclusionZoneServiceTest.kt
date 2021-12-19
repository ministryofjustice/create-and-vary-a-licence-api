package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.reset
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionUploadDetailRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository

class ExclusionZoneServiceTest {
  private val licenceRepository = mock<LicenceRepository>()
  private val additionalConditionRepository = mock<AdditionalConditionRepository>()
  private val additionalConditionUploadDetailRepository = mock<AdditionalConditionUploadDetailRepository>()

  private val service = ExclusionZoneService(
    licenceRepository,
    additionalConditionRepository,
    additionalConditionUploadDetailRepository,
  )

  @BeforeEach
  fun reset() {
    reset(
      licenceRepository,
      additionalConditionRepository,
      additionalConditionUploadDetailRepository,
    )
  }

  @Test
  fun `service uploads an exclusion zone file`() {
    /*
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(LicenceServiceTest.aLicenceEntity))
    val licence = service.getLicenceById(1L)
    Assertions.assertThat(licence).isExactlyInstanceOf(Licence::class.java)
    verify(licenceRepository, times(1)).findById(1L)
     */
  }

  @Test
  fun `service removes an upload exclusion zone`() {
  }
}
