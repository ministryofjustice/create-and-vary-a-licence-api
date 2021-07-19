package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Describes a test data object")
class TestData(
  @Schema(description = "The key", example = "A")
  val key: String = "",

  @Schema(description = "The value", example = "AAAAA")
  val value: String = "",
)
