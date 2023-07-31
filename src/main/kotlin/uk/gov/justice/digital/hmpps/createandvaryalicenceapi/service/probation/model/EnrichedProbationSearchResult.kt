package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model

import com.fasterxml.jackson.annotation.JsonFormat
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.time.LocalDate

data class EnrichedProbationSearchResult(
  val name: String = "",
  val comName: String = "",
  val teamName: String? = "",

  @JsonFormat(pattern = "dd/MM/yyyy")
  val releaseDate: LocalDate? = null,

  val licenceStatus: LicenceStatus? = null,
  val isOnProbation: Boolean? = null,
)
