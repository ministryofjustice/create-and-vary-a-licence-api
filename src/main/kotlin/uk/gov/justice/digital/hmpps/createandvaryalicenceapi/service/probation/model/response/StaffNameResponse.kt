package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.response

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.Name

data class StaffNameResponse(
  val id: Long,
  val name: Name,
  val code: String,
  val username: String?,
)
