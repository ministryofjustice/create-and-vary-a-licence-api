package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "The list of licences, audit events and licence events")
data class Content(
  @field:Schema(description = "The list of licence events")
  val licences: List<SarLicence>,

  @field:Schema(description = "The list of time served licences that were created outside of CVL")
  val timeServedExternalRecords: List<SarExternalRecord>,
)
