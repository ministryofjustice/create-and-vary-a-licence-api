package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.model

data class ElectronicMonitoringData(
  val licenceId: Long,
  val value: String,
  val type: String,
)
