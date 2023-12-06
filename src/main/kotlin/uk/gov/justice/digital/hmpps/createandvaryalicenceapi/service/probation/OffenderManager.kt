package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation

import com.fasterxml.jackson.annotation.JsonProperty

class OffenderManager(
  @JsonProperty("staff")
  val staffDetail: StaffDetail,
  val active: Boolean,
)
