package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation

import io.swagger.v3.oas.annotations.media.Schema

class PhoneNumber(
  val number: String?,
  @Schema(
    description = "phone number type",
    example = "MOBILE",
    allowableValues = ["MOBILE", "TELEPHONE"],
  )
  val type: String?,
)
