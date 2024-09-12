package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc

data class HdcLicenceData(
  val curfewAddress: CurfewAddress? = null,
  val firstNightCurfewHours: FirstNight? = null,
  val curfewHours: CurfewHours? = null,
)
