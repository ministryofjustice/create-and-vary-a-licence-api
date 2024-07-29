package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.CaViewCasesTab
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.time.LocalDate

@Schema(description = "Describes an CA(OMU) case")
data class CaCase(

  @Schema(description = "Type of this licence", example = LicenceKinds.CRD)
  val kind: LicenceKind? = null,

  @Schema(description = "Unique identifier for this licence within the service", example = "99999")
  val licenceId: Long? = null,

  @Schema(description = "The version number of this licence", example = "1.3")
  val licenceVersionOf: Long? = null,

  @Schema(description = "The full name of the person on licence", example = "John Doe")
  val name: String,

  @Schema(description = "The prison identifier for the person on this licence", example = "A9999AA")
  val prisonerNumber: String,

  @Schema(description = "The details for the active supervising probation officer")
  val probationPractitioner: ProbationPractitioner? = null,

  @Schema(description = "The date on which the prisoner leaves custody", example = "30/11/2022")
  @JsonFormat(pattern = "dd/MM/yyyy")
  val releaseDate: LocalDate? = null,

  @Schema(description = "Label for release date", example = "Confirmed release date")
  val releaseDateLabel: String? = null,

  @Schema(description = "The new status for this licence", example = "APPROVED")
  val licenceStatus: LicenceStatus,

  @Schema(
    description = "The type of tab this licence has to be populated",
    example = "RELEASES_IN_NEXT_TWO_WORKING_DAYS",
  )
  val tabType: CaViewCasesTab? = null,

  @Schema(
    description = "Legal Status",
    example = "SENTENCED",
    allowableValues = ["RECALL", "DEAD", "INDETERMINATE_SENTENCE", "SENTENCED", "CONVICTED_UNSENTENCED", "CIVIL_PRISONER", "IMMIGRATION_DETAINEE", "REMAND", "UNKNOWN", "OTHER"],
  )
  var nomisLegalStatus: String? = null,

  @get:Schema(description = "The full name of the person who last updated this licence", example = "Jane Jones")
  val lastWorkedOnBy: String? = null,

  @Schema(description = "Is the prisoner due for early release", example = "false")
  val isDueForEarlyRelease: Boolean? = null,

  @Schema(description = "Is the licence in the hard stop period? (Within two working days of release)")
  val isInHardStopPeriod: Boolean = false,
)
