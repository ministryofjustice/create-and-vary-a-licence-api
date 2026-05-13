package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.privateApi

import jakarta.validation.constraints.Size

data class AddressSearchRequest(
  @field:Size(min = 1, max = 200, message = "Search query length must be more than 0 and no more than 200")
  val searchQuery: String,
)
