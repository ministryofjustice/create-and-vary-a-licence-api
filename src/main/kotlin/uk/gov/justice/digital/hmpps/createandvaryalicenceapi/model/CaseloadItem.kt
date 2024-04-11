package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate

@Schema(description = "Additional information pertinent to CVL")
data class CvlFields(
  @Schema(
    description = "The type of licence this person should have based on their current dates, NB: this may differ from the current licence type if sentence dates have changed since any licence has been created",
    example = "AP_PSS",
  )
  val licenceType: LicenceType,

  @Schema(description = "Date which the hard stop period will start", example = "2023-05-01")
  @JsonFormat(pattern = "dd/MM/yyyy")
  var hardStopDate: LocalDate? = null,

  @Schema(description = "Date which to show the hard stop warning", example = "2023-05-01")
  @JsonFormat(pattern = "dd/MM/yyyy")
  var hardStopWarningDate: LocalDate? = null,

  @Schema(description = "Is the licence in the hard stop period? (Within two working days of release)")
  val isInHardStopPeriod: Boolean = false,
)

@Schema(description = "An item in the caseload")
data class CaseloadItem(
  @Schema(description = "Details about a prisoner")
  val prisoner: Prisoner,
  @Schema(description = "Additional CVL specific information including derived fields")
  val cvl: CvlFields,
)
