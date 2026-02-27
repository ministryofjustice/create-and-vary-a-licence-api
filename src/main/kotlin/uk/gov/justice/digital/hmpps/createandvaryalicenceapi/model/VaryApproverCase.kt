package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.model.LicenceVaryApproverCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate

@Schema(description = "Describes a vary approver case")
data class VaryApproverCase(
  @field:Schema(
    description = "Unique identifier for this licence within the service",
    example = "99999",
    requiredMode = NOT_REQUIRED,
  )
  val licenceId: Long?,

  @field:Schema(
    description = "The full name of the person on licence",
    example = "An offender",
    requiredMode = NOT_REQUIRED,
  )
  val name: String?,

  @field:Schema(
    description = "The case reference number (CRN) for the person on this licence",
    example = "X12444",
    requiredMode = REQUIRED,
  )
  val crnNumber: String,

  @field:Schema(description = "The licence type code", example = "AP", requiredMode = NOT_REQUIRED)
  val licenceType: LicenceType?,

  @field:Schema(
    description = "The date on which the licence variation was created",
    example = "30/11/2022",
    requiredMode = NOT_REQUIRED,
  )
  @field:JsonFormat(pattern = "dd/MM/yyyy")
  val variationRequestDate: LocalDate?,

  @field:Schema(
    description = "The date on which the prisoner leaves custody",
    example = "30/11/2022",
    requiredMode = NOT_REQUIRED,
  )
  @field:JsonFormat(pattern = "dd/MM/yyyy")
  val releaseDate: LocalDate?,

  @field:Schema(description = "The details for the active supervising probation officer", requiredMode = NOT_REQUIRED)
  val probationPractitioner: ProbationPractitioner,

  @field:Schema(description = "Is the offender a limited access offender (LAO)?", example = "true")
  val isRestricted: Boolean,
) {
  companion object {
    fun restrictedCase(licence: LicenceVaryApproverCase) = VaryApproverCase(
      licenceId = null,
      name = "Access restricted on NDelius",
      crnNumber = licence.crn,
      licenceType = null,
      variationRequestDate = null,
      releaseDate = null,
      probationPractitioner = ProbationPractitioner.restrictedView(),
      isRestricted = true,
    )
  }
}
