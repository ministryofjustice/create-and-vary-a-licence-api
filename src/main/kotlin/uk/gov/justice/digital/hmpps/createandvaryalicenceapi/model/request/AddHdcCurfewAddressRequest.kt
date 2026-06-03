package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.address.hdc.AccommodationType

@Schema(description = "Request to add HDC curfew address with post-release checks")
data class AddHdcCurfewAddressRequest(

  @field:Valid
  @field:NotNull
  @Schema(description = "The address to be added as the HDC curfew address", required = true)
  val address: AddAddressRequest,

  @field:Schema(description = "The type of accommodation for the HDC curfew address", example = "RESIDENTIAL")
  val accommodationType: AccommodationType? = null,

  @field:NotNull
  @Schema(description = "Flag to indicate if post release residential checks have been completed", example = "true")
  val postReleaseResidentialChecksCompleted: Boolean? = null,

  @field:Schema(description = "Reason for post release checks not completed, required if postReleaseResidentialChecksCompleted is false", example = "Offender refused to provide information")
  val postReleaseResidentialChecksNotCompletedReason: String? = null,
)
