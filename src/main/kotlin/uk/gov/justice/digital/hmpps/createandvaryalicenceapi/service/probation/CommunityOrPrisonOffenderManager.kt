package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation

data class CommunityOrPrisonOffenderManager(
  val code: String,
  val id: Long,
  val team: TeamDetail,
  val provider: Detail,
)
