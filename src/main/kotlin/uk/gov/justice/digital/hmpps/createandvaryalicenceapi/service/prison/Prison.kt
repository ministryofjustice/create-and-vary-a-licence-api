package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison

import com.fasterxml.jackson.annotation.JsonProperty

data class Prison(
  @JsonProperty("agencyId")
  val prisonId: String,

  @JsonProperty("formattedDescription")
  val description: String,

  @JsonProperty("phones")
  val phoneDetails: List<PhoneDetail>,
)
