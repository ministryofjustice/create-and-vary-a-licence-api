package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "Response object which summarises a licence")
data class LicenceSummary(
  @field:Schema(description = "kind of licence", example = "CRD")
  val kind: LicenceKind,

  @field:Schema(description = "Internal identifier for this licence generated within this service", example = "123344")
  val licenceId: Long,

  @field:Schema(description = "Licence type code", example = "AP")
  val licenceType: LicenceType,

  @field:Schema(description = "The status of this licence", example = "IN_PROGRESS")
  val licenceStatus: LicenceStatus,

  @field:Schema(description = "The prison nomis identifier for this offender", example = "A1234AA")
  val nomisId: String,

  @field:Schema(description = "The offender surname", example = "Smith")
  val surname: String?,

  @field:Schema(description = "The offender forename", example = "Brian")
  val forename: String?,

  @field:Schema(description = "The prison code where this offender resides or was released from", example = "MDI")
  val prisonCode: String?,

  @field:Schema(description = "The prison where this offender resides or was released from", example = "Moorland (HMP)")
  val prisonDescription: String?,

  @field:Schema(description = "The probation area code where the licence is supervised", example = "N01")
  val probationAreaCode: String?,

  @field:Schema(description = "The probation area description", example = "Wales")
  val probationAreaDescription: String? = null,

  @field:Schema(
    description = "The probation delivery unit (PDU or borough) where the licence is supervised",
    example = "N01CA",
  )
  val probationPduCode: String?,

  @field:Schema(description = "The description for the PDU", example = "North Wales")
  val probationPduDescription: String? = null,

  @field:Schema(
    description = "The local administrative unit (LAU or district) where the licence is supervised",
    example = "NA01CA-02",
  )
  val probationLauCode: String?,

  @field:Schema(description = "The LAU description", example = "North Wales")
  val probationLauDescription: String? = null,

  @field:Schema(description = "The probation team code which supervises the licence", example = "NA01CA-02-A")
  val probationTeamCode: String?,

  @field:Schema(description = "The team description", example = "Cardiff South")
  val probationTeamDescription: String? = null,

  @field:Schema(description = "The conditional release date on the licence", example = "12/12/2022")
  @field:JsonFormat(pattern = "dd/MM/yyyy")
  val conditionalReleaseDate: LocalDate?,

  @field:Schema(description = "The actual release date on the licence", example = "12/12/2022")
  @field:JsonFormat(pattern = "dd/MM/yyyy")
  val actualReleaseDate: LocalDate?,

  @field:Schema(description = "The sentence start date", example = "13/09/2019")
  @field:JsonFormat(pattern = "dd/MM/yyyy")
  val sentenceStartDate: LocalDate? = null,

  @field:Schema(description = "The sentence end date", example = "13/09/2022")
  @field:JsonFormat(pattern = "dd/MM/yyyy")
  val sentenceEndDate: LocalDate? = null,

  @field:Schema(description = "The date that the licence will start", example = "13/09/2022")
  @field:JsonFormat(pattern = "dd/MM/yyyy")
  val licenceStartDate: LocalDate? = null,

  @field:Schema(description = "The date that the licence will expire", example = "13/09/2024")
  @field:JsonFormat(pattern = "dd/MM/yyyy")
  val licenceExpiryDate: LocalDate? = null,

  @field:Schema(
    description = "The date when the post sentence supervision period starts, from prison services",
    example = "06/05/2023",
  )
  @field:JsonFormat(pattern = "dd/MM/yyyy")
  val topupSupervisionStartDate: LocalDate? = null,

  @field:Schema(
    description = "The date when the post sentence supervision period ends, from prison services",
    example = "06/06/2023",
  )
  @field:JsonFormat(pattern = "dd/MM/yyyy")
  val topupSupervisionExpiryDate: LocalDate? = null,

  @field:Schema(
    description = "The release date after being recalled",
    example = "06/06/2023",
  )
  @field:JsonFormat(pattern = "dd/MM/yyyy")
  val postRecallReleaseDate: LocalDate? = null,

  @field:Schema(description = "Type of hardstop licence", example = LicenceKinds.HARD_STOP)
  val hardStopKind: LicenceKind? = null,

  @field:Schema(
    description = "The date when the hard stop period starts",
    example = "11/09/2022",
  )
  @field:JsonFormat(pattern = "dd/MM/yyyy")
  val hardStopDate: LocalDate? = null,

  @field:Schema(
    description = "The date when warning about the hard stop period begins",
    example = "11/09/2022",
  )
  @field:JsonFormat(pattern = "dd/MM/yyyy")
  val hardStopWarningDate: LocalDate? = null,

  @field:Schema(description = "Is the licence in the hard stop period? (Within two working days of release)")
  val isInHardStopPeriod: Boolean = false,

  @field:Schema(description = "Is the prisoner due to be released in the next two working days")
  val isDueToBeReleasedInTheNextTwoWorkingDays: Boolean = false,

  @field:Schema(
    description = "The case reference number (CRN) of this person, from either prison or probation service",
    example = "X12344",
  )
  val crn: String?,

  @field:Schema(
    description = "The offender's date of birth, from either prison or probation services",
    example = "12/12/2001",
  )
  @field:JsonFormat(pattern = "dd/MM/yyyy")
  val dateOfBirth: LocalDate?,

  @field:Schema(description = "The username of the responsible probation officer", example = "jsmith")
  val comUsername: String?,

  @field:Schema(description = "The bookingId associated with the licence", example = "773722")
  val bookingId: Long?,

  @field:Schema(description = "The date the licence was created", example = "02/12/2001 10:15")
  @field:JsonFormat(pattern = "dd/MM/yyyy HH:mm")
  val dateCreated: LocalDateTime?,

  @field:Schema(
    description = "The full name of the person who approved the licence",
    example = "John Smith",
  )
  val approvedByName: String? = null,

  @field:Schema(description = "The date and time that this licence was approved", example = "24/08/2022 11:30:33")
  @field:JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
  val approvedDate: LocalDateTime? = null,

  @field:Schema(
    description = "The date and time that this licence was submitted for approval",
    example = "24/08/2022 11:30:33",
  )
  @field:JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
  val submittedDate: LocalDateTime? = null,

  @field:Schema(description = "The version number of this licence", example = "1.3")
  val licenceVersion: String? = null,

  @field:Schema(description = "The licence Id which this licence is a version of", example = "86")
  val versionOf: Long? = null,

  @field:Schema(description = "Is a review of this licence is required", example = "true")
  val isReviewNeeded: Boolean = false,

  @field:Schema(description = "The full name of the person who last updated this licence", example = "Jane Jones")
  val updatedByFullName: String? = null,

  @field:Schema(
    description = "The person’s actual home detention curfew date",
    example = "30/01/2025",
  )
  @field:JsonFormat(pattern = "dd/MM/yyyy")
  val homeDetentionCurfewActualDate: LocalDate? = null,
)
