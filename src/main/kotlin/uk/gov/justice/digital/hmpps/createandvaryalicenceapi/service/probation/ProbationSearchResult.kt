package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate

data class ProbationSearchResult(
  val name: Name,
  val identifiers: Identifiers,
  val manager: Manager,
  val allocationDate: String,
  val licenceType: LicenceType? = null,
  val licenceStatus: LicenceStatus? = null,
  val releaseDate: LocalDate? = null,
  val isOnProbation: Boolean? = null,
)
