package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.ElectronicMonitoringProvider

interface HasElectronicMonitoringResponseProvider {
  var electronicMonitoringProvider: ElectronicMonitoringProvider?

  fun ensureElectronicMonitoringProviderExists() {
    if (electronicMonitoringProvider == null) {
      electronicMonitoringProvider = createNewElectronicMonitoringProvider()
    }
  }

  fun createNewElectronicMonitoringProvider(): ElectronicMonitoringProvider
}
