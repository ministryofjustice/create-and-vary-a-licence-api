package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate

@Schema(description = "Additional information pertinent to CVL")
data class CvlFields(
  @field:Schema(
    description = "The type of licence this person should have based on their current dates, NB: this may differ from the current licence type if sentence dates have changed since any licence has been created",
    example = "AP_PSS",
  )
  val licenceType: LicenceType,

  @field:Schema(description = "Date which the hard stop period will start", example = "03/05/2023")
  @field:JsonFormat(pattern = "dd/MM/yyyy")
  var hardStopDate: LocalDate? = null,

  @field:Schema(description = "Date which to show the hard stop warning", example = "01/05/2023")
  @field:JsonFormat(pattern = "dd/MM/yyyy")
  var hardStopWarningDate: LocalDate? = null,

  @field:Schema(description = "Is the licence in the hard stop period? (Within two working days of release)")
  val isInHardStopPeriod: Boolean = false,

  @field:Schema(description = "If ARD||CRD falls on Friday/Bank holiday/Weekend then it is eligible for early release)")
  val isEligibleForEarlyRelease: Boolean = false,

  @field:Schema(description = "Is the prisoner due to be released in the next two working days")
  val isDueToBeReleasedInTheNextTwoWorkingDays: Boolean = false,

  @field:Schema(description = "Date that the licence is due to activate", example = "05/05/2023")
  @field:JsonFormat(pattern = "dd/MM/yyyy")
  val licenceStartDate: LocalDate? = null,

  @field:Schema(
    description = "The kind of licence this person should have based on their current dates",
    example = "CRD",
  )
  val licenceKind: LicenceKind,
)

@Schema(description = "A combination of the NOMIS prisoner record and additional CVL fields")
data class PrisonerWithCvlFields(
  @field:Schema(description = "Details about a prisoner")
  val prisoner: Prisoner,
  @field:Schema(description = "Additional CVL specific information including derived fields")
  val cvl: CvlFields,
)

@Schema(description = "A combination of the NOMIS prisoner record and their respective licence start date")
data class CaseloadItem(
  @field:Schema(description = "Details about a prisoner")
  val prisoner: Prisoner,

  @field:Schema(description = "Date that the licence is due to activate", example = "05/05/2023")
  @field:JsonFormat(pattern = "dd/MM/yyyy")
  val licenceStartDate: LocalDate? = null,
)
