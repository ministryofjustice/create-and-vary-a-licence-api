package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation

data class OffenderDetail(
  val offenderId: Int,
  val otherIds: OtherIds,
  val offenderManagers: List<OffenderManager>,
)
