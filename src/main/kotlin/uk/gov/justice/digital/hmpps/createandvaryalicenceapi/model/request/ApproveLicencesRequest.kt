package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Request object for searching for licences which are ready to approve")
data class ApproveLicencesRequest(

  @field:Schema(description = "A list of prison codes", example = "['PVI', 'BAI']")
  val prisonCodes: List<String>? = null,
)
