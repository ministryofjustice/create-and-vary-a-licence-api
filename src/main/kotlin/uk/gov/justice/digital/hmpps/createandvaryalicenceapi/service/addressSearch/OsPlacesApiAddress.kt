package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.addressSearch

import com.fasterxml.jackson.annotation.JsonProperty

data class OsPlacesApiAddress(
  @JsonProperty("DPA")
  val dpa: DeliveryPointAddress,
)
