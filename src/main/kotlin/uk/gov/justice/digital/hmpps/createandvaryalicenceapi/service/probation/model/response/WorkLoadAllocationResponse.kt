package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.response

import java.time.LocalDateTime

data class WorkLoadAllocationResponse(
  val id: String,
  val staffCode: String,
  val teamCode: String,
  val createdDate: LocalDateTime,
  val crn: String,
)
