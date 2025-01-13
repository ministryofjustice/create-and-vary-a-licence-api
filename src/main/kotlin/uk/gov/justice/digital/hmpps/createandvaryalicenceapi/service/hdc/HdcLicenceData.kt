package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.HdcCurfewTimes

data class HdcLicenceData(
  val licenceId: Long? = null,
  val curfewAddress: CurfewAddress? = null,
  val firstNightCurfewHours: FirstNight? = null,
  val curfewTimes: List<HdcCurfewTimes>? = null,
)
