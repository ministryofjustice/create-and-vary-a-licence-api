package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.model

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate
import java.time.LocalDateTime

class LicenceVaryApproverCase(
  val licenceStartDate: LocalDate?,
  val licenceId: Long,
  val prisonNumber: String?,
  val comUsername: String?,
  var typeCode: LicenceType?,
  var dateCreated: LocalDateTime?,
)
