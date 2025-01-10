package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.CaseloadResult

@Schema(description = "CVL Probation Search Record")
data class CvlProbationSearchRecord(
  @Schema(description = "Delius caseload record")
  val caseloadResult: CaseloadResult,

  @Schema(description = "Prisoner Search prisoner record")
  val prisonerSearchPrisoner: PrisonerSearchPrisoner?,

  @Schema(description = "CVL licence")
  val licence: Licence? = null,
)
