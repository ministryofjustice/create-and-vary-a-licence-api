package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Management stats")
data class LicenceStatistics(

  @Schema(description = "Prison ID")
  val prison: String? = null,

  @Schema(description = "Type of licence", example = "AP, PSS, APPSS")
  val licenceType: String? = null,

  @Schema(description = "Number eligible for CVL within timeframe", example = "10")
  val eligibleForCvl: Int? = null,

  @Schema(description = "Status of In progress", example = "2")
  val inProgress: Int? = null,

  @Schema(description = "Status of Submitted", example = "2")
  val submitted: Int? = null,

  @Schema(description = "Status of Approved", example = "8")
  val approved: Int? = null,

  @Schema(description = "Status of Active", example = "5")
  val active: Int? = null,

  @Schema(description = "Total inactive", example = "5")
  val inactiveTotal: Int? = null,

  @Schema(description = "Status of Inactive not approved", example = "6")
  val inactiveNotApproved: Int? = null,

  @Schema(description = "Status of Inactive aprroved", example = "3")
  val inactiveApproved: Int? = null,

  @Schema(description = "Inactive because HDC approved", example = "2")
  val inactiveHdcApproved: Int? = null,

  @Schema(description = "Approved but never printed", example = "1")
  val approvedNotPrinted: Int? = null,
)
