package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc

data class HdcLicence(
  val curfewAddress: CurfewAddress? = null,
  val firstNightCurfewHours: FirstNight? = null,
  val curfewHours: CurfewHours? = null,
)
