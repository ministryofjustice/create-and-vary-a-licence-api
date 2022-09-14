package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import javax.validation.constraints.NotNull

@Schema(description = "Request object for persisting uploads to cloud storage")
class AdditionalConditionUploadRequest(
  @field:Schema(description = "Unique key assigned to cloud storage upload") @field:NotNull val key: String,
  @field:Schema(description = "the category of upload the file belongs to. E.g. Exclusion Zone") @field:NotNull val category: String,
  @field:Schema(description = "File URL for accessing cloud upload") @field:NotNull val url: String,
  @field:NotNull val mineType: String
)
