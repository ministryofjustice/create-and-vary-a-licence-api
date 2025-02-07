package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate

data class OffenderManager(
  @JsonProperty("staff")
  val staffDetail: ProbationSearchStaffDetail,
  val active: Boolean,
  @JsonFormat(pattern = "yyyy-MM-dd")
  val fromDate: LocalDate? = null,
)

data class ProbationSearchStaffDetail(
  val code: String,
  val forenames: String?,
  val surname: String?,
  val unallocated: Boolean? = false,
)
