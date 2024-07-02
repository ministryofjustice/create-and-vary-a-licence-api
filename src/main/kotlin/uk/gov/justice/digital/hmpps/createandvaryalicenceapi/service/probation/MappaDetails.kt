package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation

class MappaDetails(
  val category: Long?,
  val categoryDescription: String?,
  val level: Long?,
  val levelDescription: String?,
  val notes: String?,
  val officer: StaffHuman?,
  val probationArea: KeyValue?,
  val reviewDate: String?,
  val startDate: String?,
  val team: KeyValue?,
)
