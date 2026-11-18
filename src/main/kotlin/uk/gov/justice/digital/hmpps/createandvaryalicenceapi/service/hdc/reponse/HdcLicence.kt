package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.reponse

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.address.hdc.AccommodationType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.HdcStatus

data class HdcLicence(
  val licenceId: Long? = null,
  val curfewAddress: CurfewAddress? = null,
  val firstNightCurfewTimes: FirstNight? = null,
  val curfewTimes: List<CurfewTimes>? = null,
  val status: HdcStatus,
  val proposeAddressType: AccommodationType? = null,
)
