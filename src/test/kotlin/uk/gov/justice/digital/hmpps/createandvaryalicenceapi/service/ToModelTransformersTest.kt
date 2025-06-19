package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.ElectronicMonitoringProviderStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.ElectronicMonitoringProvider as EntityElectronicMonitoringProvider

class ToModelTransformersTest {
  @Test
  fun `determineElectronicMonitoringProviderStatus returns NOT_NEEDED when provider is null`() {
    val status = determineElectronicMonitoringProviderStatus(null)
    assertThat(status).isEqualTo(ElectronicMonitoringProviderStatus.NOT_NEEDED)
  }

  @Test
  fun `determineElectronicMonitoringProviderStatus returns NOT_STARTED when isToBeTaggedForProgramme is null`() {
    val provider = electronicMonitoringProvider.copy(
      isToBeTaggedForProgramme = null
    )
    val status = determineElectronicMonitoringProviderStatus(provider)
    assertThat(status).isEqualTo(ElectronicMonitoringProviderStatus.NOT_STARTED)
  }

  @Test
  fun `determineElectronicMonitoringProviderStatus returns COMPLETE when isToBeTaggedForProgramme is true`() {
    val status = determineElectronicMonitoringProviderStatus(electronicMonitoringProvider)
    assertThat(status).isEqualTo(ElectronicMonitoringProviderStatus.COMPLETE)
  }

  @Test
  fun `determineElectronicMonitoringProviderStatus returns COMPLETE when isToBeTaggedForProgramme is false`() {
    val provider = electronicMonitoringProvider.copy(
      isToBeTaggedForProgramme = false,
      programmeName = null
    )
    val status = determineElectronicMonitoringProviderStatus(provider)
    assertThat(status).isEqualTo(ElectronicMonitoringProviderStatus.COMPLETE)
  }

  private companion object {
    val aLicenceEntity = TestData.createCrdLicence()
    val electronicMonitoringProvider = EntityElectronicMonitoringProvider(
      id = 1,
      licence = aLicenceEntity,
      isToBeTaggedForProgramme = true,
      programmeName = "Test Programme",
    )
  }
}
