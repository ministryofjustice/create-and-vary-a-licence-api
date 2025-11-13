package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.model

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate
import java.time.LocalDateTime

class LicenceVaryApproverCase(
  val licenceId: Long,
  val crn: String,
  val licenceStartDate: LocalDate?,
  val prisonNumber: String?,
  val comUsername: String?,
  var typeCode: LicenceType?,
  var dateCreated: LocalDateTime?,
)
