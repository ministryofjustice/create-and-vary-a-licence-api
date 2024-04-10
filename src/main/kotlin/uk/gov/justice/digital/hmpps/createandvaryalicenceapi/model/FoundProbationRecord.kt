package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate

@Schema(description = "Describes a search result which has been found and enriched")
data class FoundProbationRecord(

  @Schema(description = "The forename and surname of the offender")
  val name: String = "",

  @Schema(
    description = "The case reference number (CRN) of the offender,",
    example = "X12344",
  )
  val crn: String? = "",

  @Schema(description = "The prison nomis number for the offender", example = "A1234AA")
  val nomisId: String? = "",

  @Schema(description = "The forename and surname of the COM")
  val comName: String? = "",

  @Schema(description = "The COM's staff code")
  val comStaffCode: String? = "",

  @Schema(description = "The description of the COM's team")
  val teamName: String? = "",

  @Schema(description = "The release date of the offender", example = "27/07/2023")
  @JsonFormat(pattern = "dd/MM/yyyy")
  val releaseDate: LocalDate? = null,

  @Schema(
    description = "The date when the hard stop period starts",
    example = "11/09/2022",
  )
  @JsonFormat(pattern = "dd/MM/yyyy")
  val hardStopDate: LocalDate? = null,

  @Schema(
    description = "The date when warning about the hard stop period begins",
    example = "11/09/2022",
  )
  @JsonFormat(pattern = "dd/MM/yyyy")
  val hardStopWarningDate: LocalDate? = null,

  @Schema(description = "Is the licence in the hard stop period? (Within two working days of release)")
  val isInHardStopPeriod: Boolean = false,

  @Schema(description = "The ID of the most recent and relevant licence", example = "123344")
  val licenceId: Long? = null,

  @Schema(description = "The type of licence")
  val licenceType: LicenceType? = null,

  @Schema(description = "The status of the licence")
  val licenceStatus: LicenceStatus? = null,

  @Schema(description = "Indicates whether the offender is in prison or out on probation")
  val isOnProbation: Boolean? = null,

)
