package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation

import com.fasterxml.jackson.annotation.JsonProperty

data class OffenderManager(
  @JsonProperty("staff")
  val staffDetail: StaffHuman,
  val active: Boolean,
  val unallocated: Boolean? = false,
)
