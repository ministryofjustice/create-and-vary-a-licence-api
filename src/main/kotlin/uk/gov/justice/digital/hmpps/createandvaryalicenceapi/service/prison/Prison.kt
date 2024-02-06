package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison

import com.fasterxml.jackson.annotation.JsonProperty

data class Prison(
  @JsonProperty("agencyId")
  val prisonId: String,

  @JsonProperty("formattedDescription")
  val description: String,

  @JsonProperty("phones")
  val phoneDetails: List<PhoneDetail>,
) {
  fun getPrisonContactNumber(): String {
    return listOfNotNull(
      this.phoneDetails.find { it.type == "BUS" }?.number,
      this.phoneDetails.find { it.type == "BUS" }?.ext,
    )
      .joinToString("")
  }
}
