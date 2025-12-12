package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.CaViewCasesTab
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.time.LocalDate

@Schema(description = "Describes an CA(OMU) case")
data class CaCase(

  @field:Schema(description = "Type of this licence", example = LicenceKinds.CRD)
  val kind: LicenceKind? = null,

  @field:Schema(description = "Unique identifier for this licence within the service", example = "99999")
  val licenceId: Long? = null,

  @field:Schema(description = "The version number of this licence", example = "1.3")
  val licenceVersionOf: Long? = null,

  @field:Schema(description = "The full name of the person on licence", example = "John Doe")
  val name: String,

  @field:Schema(description = "The prison identifier for the person on this licence", example = "A9999AA")
  val prisonerNumber: String,

  @field:Schema(description = "The details for the active supervising probation officer")
  val probationPractitioner: ProbationPractitioner? = null,

  @field:Schema(description = "The date on which the prisoner leaves custody", example = "30/11/2022")
  @field:JsonFormat(pattern = "dd/MM/yyyy")
  val releaseDate: LocalDate? = null,

  @field:Schema(description = "Label for release date", example = "Confirmed release date")
  val releaseDateLabel: String? = null,

  @field:Schema(description = "The new status for this licence", example = "APPROVED")
  val licenceStatus: LicenceStatus,

  @field:Schema(
    description = "The type of tab this licence has to be populated",
    example = "RELEASES_IN_NEXT_TWO_WORKING_DAYS",
  )
  val tabType: CaViewCasesTab? = null,

  @field:Schema(
    description = "Legal Status",
    example = "SENTENCED",
    allowableValues = ["RECALL", "DEAD", "INDETERMINATE_SENTENCE", "SENTENCED", "CONVICTED_UNSENTENCED", "CIVIL_PRISONER", "IMMIGRATION_DETAINEE", "REMAND", "UNKNOWN", "OTHER"],
  )
  var nomisLegalStatus: String? = null,

  @field:Schema(description = "The full name of the person who last updated this licence", example = "Jane Jones")
  val lastWorkedOnBy: String? = null,

  @field:Schema(description = "Is the licence in the hard stop period? (Within two working days of release)")
  val isInHardStopPeriod: Boolean = false,

  @field:Schema(description = "The agency code where this offender resides or was released from", example = "MDI")
  val prisonCode: String? = null,

  @field:Schema(description = "The agency description of the detaining prison", example = "Leeds (HMP)")
  val prisonDescription: String? = null,

  val hasNomisLicence: Boolean = false,
)
