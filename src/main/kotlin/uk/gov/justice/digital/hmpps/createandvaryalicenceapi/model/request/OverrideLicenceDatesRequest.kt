package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import java.time.LocalDate

@Schema(description = "Request object for overriding licence dates")
data class OverrideLicenceDatesRequest(
  @field:Schema(description = "The updated licence kind based on the current dates", example = "CRD")
  @field:JsonFormat(pattern = "dd/MM/yyyy")
  val updatedKind: LicenceKind,

  @field:Schema(description = "The conditional release date", example = "18/06/2022")
  @field:JsonFormat(pattern = "dd/MM/yyyy")
  val conditionalReleaseDate: LocalDate? = null,

  @field:Schema(description = "The actual release date", example = "18/07/2022")
  @field:JsonFormat(pattern = "dd/MM/yyyy")
  val actualReleaseDate: LocalDate? = null,

  @field:Schema(description = "The sentence start date", example = "06/05/2019")
  @field:JsonFormat(pattern = "dd/MM/yyyy")
  val sentenceStartDate: LocalDate? = null,

  @field:Schema(description = "The sentence end date", example = "06/05/2023")
  @field:JsonFormat(pattern = "dd/MM/yyyy")
  val sentenceEndDate: LocalDate? = null,

  @field:Schema(description = "The licence expiry date", example = "06/05/2023")
  @field:JsonFormat(pattern = "dd/MM/yyyy")
  val licenceExpiryDate: LocalDate? = null,

  @field:Schema(
    description = "The date when the post sentence supervision period starts",
    example = "06/05/2023",
  )
  @field:JsonFormat(pattern = "dd/MM/yyyy")
  val topupSupervisionStartDate: LocalDate? = null,

  @field:Schema(
    description = "The date when the post sentence supervision period ends",
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

  @field:Schema(
    description = "The person's actual home detention curfew date",
    example = "06/06/2023",
  )
  @field:JsonFormat(pattern = "dd/MM/yyyy")
  val homeDetentionCurfewActualDate: LocalDate? = null,

  @field:Schema(
    description = "The person's home detention curfew end date",
    example = "06/06/2023",
  )
  @field:JsonFormat(pattern = "dd/MM/yyyy")
  val homeDetentionCurfewEndDate: LocalDate? = null,

  @field:Schema(description = "Reason for overriding the licence dates")
  @field:NotEmpty
  val reason: String,
)
