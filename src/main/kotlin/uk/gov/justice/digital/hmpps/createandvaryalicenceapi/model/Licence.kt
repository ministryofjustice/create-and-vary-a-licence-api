package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "Describes a licence document within this service")
data class Licence(

  @Schema(description = "Unique identifier for this licence within the service", example = "99999")
  val id: Long = -1,

  @Schema(description = "The licence type code", allowableValues = ["AP", "PSS", "AP_PSS"], example = "AP")
  val typeCode: LicenceType,

  @Schema(description = "The version number used for standard and additional conditions", example = "1.4")
  val version: String? = null,

  @Schema(
    description = "The current status code for this licence",
    allowableValues = ["IN_PROGRESS", "SUBMITTED", "REJECTED", "ACTIVE", "SUPERSEDED"],
    example = "AP",
  )
  val statusCode: LicenceStatus,

  @Schema(description = "The prison identifier for the person on this licence", example = "A9999AA")
  val nomsId: String? = null,

  @Schema(description = "The prison booking number for the person on this licence", example = "F12333")
  val bookingNo: String? = null,

  @Schema(description = "The prison internal booking ID for the person on this licence", example = "989898")
  val bookingId: Long? = null,

  @Schema(description = "The case reference number (CRN) for the person on this licence", example = "X12444")
  val crn: String? = null,

  @Schema(
    description = "The police national computer number (PNC) for the person on this licence",
    example = "2015/12444",
  )
  val pnc: String? = null,

  @Schema(
    description = "The criminal records office number (CRO) for the person on this licence",
    example = "A/12444",
  )
  val cro: String? = null,

  @Schema(description = "The agency code of the detaining prison", example = "LEI")
  val prisonCode: String? = null,

  @Schema(description = "The agency description of the detaining prison", example = "Leeds (HMP)")
  val prisonDescription: String? = null,

  val prisonTelephone: String? = null,
  val forename: String? = null,
  val middleNames: String? = null,
  val surname: String? = null,

  @JsonFormat(pattern = "dd/MM/yyyy")
  val dateOfBirth: LocalDate? = null,

  @JsonFormat(pattern = "dd/MM/yyyy")
  val conditionalReleaseDate: LocalDate? = null,

  @JsonFormat(pattern = "dd/MM/yyyy")
  val actualReleaseDate: LocalDate? = null,

  @JsonFormat(pattern = "dd/MM/yyyy")
  val sentenceStartDate: LocalDate? = null,

  @JsonFormat(pattern = "dd/MM/yyyy")
  val sentenceEndDate: LocalDate? = null,

  @JsonFormat(pattern = "dd/MM/yyyy")
  val licenceStartDate: LocalDate? = null,

  @JsonFormat(pattern = "dd/MM/yyyy")
  val licenceExpiryDate: LocalDate? = null,

  val comFirstName: String? = null,
  val comLastName: String? = null,
  val comUsername: String? = null,
  val comStaffId: Long? = null,
  val comEmail: String? = null,
  val comTelephone: String? = null,
  val probationAreaCode: String? = null,
  val probationLduCode: String? = null,
  val appointmentPerson: String? = null,

  @JsonFormat(pattern = "dd/MM/yyyy")
  val appointmentDate: LocalDate? = null,

  val appointmentTime: String? = null,
  val appointmentAddress: String? = null,

  @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
  val approvedDate: LocalDateTime? = null,
  val approvedByUsername: String? = null,

  @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
  val supersededDate: LocalDateTime? = null,

  @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
  val dateCreated: LocalDateTime? = null,

  val createByUsername: String? = null,

  @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
  val dateLastUpdated: LocalDateTime? = null,

  val updatedByUsername: String? = null,

  @Schema(description = "The list of standard conditions on this licence")
  val standardConditions: List<StandardCondition> = emptyList(),

  @Schema(description = "The list of additional conditions on this licence")
  val additionalConditions: List<AdditionalCondition> = emptyList(),

  @Schema(description = "The list of bespoke conditions on this licence")
  val bespokeConditions: List<BespokeCondition> = emptyList(),
)
