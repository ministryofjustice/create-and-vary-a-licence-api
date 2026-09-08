package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.addressSearch.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class OsPlacesApiAddress(
  @field:JsonProperty("DPA")
  val dpa: DeliveryPointAddress? = null,
  @field:JsonProperty("LPI")
  var lpi: LocalPropertyIdentifierAddress? = null,
)
