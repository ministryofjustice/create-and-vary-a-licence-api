package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.reponse

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.address.hdc.AccommodationType

data class CurfewAddress(

  val addressLine1: String? = null,

  val addressLine2: String? = null,

  val townOrCity: String? = null,

  val postcode: String? = null,

  val curfewAddressType: AccommodationType,
)
