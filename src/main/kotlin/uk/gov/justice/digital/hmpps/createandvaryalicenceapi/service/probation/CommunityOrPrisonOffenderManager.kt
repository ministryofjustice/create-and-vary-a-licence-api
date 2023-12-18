package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation

data class CommunityOrPrisonOffenderManager(
  val staffCode: String,
  val staffId: Long,
  val team: TeamDetail,
  val probationArea: Detail,
)
