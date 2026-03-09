package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CurfewTimes
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.HdcCurfewAddress

data class HdcLicenceData(
  val licenceId: Long? = null,
  val curfewAddress: HdcCurfewAddress? = null,
  val firstNightCurfewHours: FirstNight? = null,
  val weeklyCurfewTimes: List<CurfewTimes>? = null,
)
