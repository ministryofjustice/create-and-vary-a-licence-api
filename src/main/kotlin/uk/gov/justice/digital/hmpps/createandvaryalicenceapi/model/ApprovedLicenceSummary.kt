package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDateTime

@Schema(description = "Response object which summarises an approved licence")
data class ApprovedLicenceSummary(

  @Schema(description = "Internal identifier for this licence generated within this service", example = "123344")
  val licenceId: Long,

  @Schema(description = "The status of this licence", example = "IN_PROGRESS")
  val licenceStatus: LicenceStatus,

  @Schema(description = "Kind of licence", example = "CRD")
  val kind: LicenceKind,

  @Schema(description = "Licence type code", example = "AP")
  val licenceType: LicenceType,

  @Schema(description = "The prison nomis identifier for this offender", example = "A1234AA")
  val nomisId: String?,

  @Schema(
    description = "The case reference number (CRN) of this person, from either prison or probation service",
    example = "X12344",
  )
  val crn: String?,

  @Schema(description = "The username of the responsible probation officer", example = "jsmith")
  val comUsername: String?,

  @Schema(description = "The date the licence was created", example = "02/12/2001 10:15")
  @JsonFormat(pattern = "dd/MM/yyyy HH:mm")
  val dateCreated: LocalDateTime?,

  @Schema(
    description = "The full name of the person who approved the licence",
    example = "John Smith",
  )
  val approvedByName: String? = null,

  @Schema(description = "The date and time that this licence was approved", example = "24/08/2022 11:30:33")
  @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
  val approvedDate: LocalDateTime? = null,

  @Schema(description = "The licence Id which this licence is a version of", example = "86")
  val versionOf: Long? = null,

  @Schema(description = "The username of the person who last updated this licence", example = "John Doe")
  val updatedByFullName: String? = null,

  @Schema(description = "The username of the person who last submitted this licence", example = "Jane Doe")
  val submittedByFullName: String? = null,
)
