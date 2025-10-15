package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.ElectronicMonitoringProvider

interface HasElectronicMonitoringResponseProvider {
  fun getProvider(): ElectronicMonitoringProvider?
}
