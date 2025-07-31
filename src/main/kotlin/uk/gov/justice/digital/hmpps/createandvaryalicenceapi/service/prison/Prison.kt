package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison

import com.fasterxml.jackson.annotation.JsonProperty

data class Prison(
  @field:JsonProperty("agencyId")
  val prisonId: String,

  @field:JsonProperty("formattedDescription")
  val description: String,

  @field:JsonProperty("phones")
  val phoneDetails: List<PhoneDetail>,
) {
  fun getPrisonContactNumber(): String = listOfNotNull(
    this.phoneDetails.find { it.type == "BUS" }?.number,
    this.phoneDetails.find { it.type == "BUS" }?.ext,
  )
    .joinToString("")
}
