package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation

import com.fasterxml.jackson.annotation.JsonProperty

data class OffenderManager(
  @JsonProperty("staff")
  val staffDetail: ProbationSearchStaffDetail,
  val active: Boolean,
)

data class ProbationSearchStaffDetail(
  val code: String,
  val forenames: String?,
  val surname: String?,
  val unallocated: Boolean? = false,
)
