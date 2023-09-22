package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Describes a licence within this service")
data class Licence(
  val id: Long = -1,
)
