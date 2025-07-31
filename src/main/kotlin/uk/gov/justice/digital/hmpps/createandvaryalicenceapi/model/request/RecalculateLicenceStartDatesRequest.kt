package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Request object for updating licence start dates for a batch of licences.")
data class RecalculateLicenceStartDatesRequest(
  @field:Schema(description = "The update batch size.")
  val batchSize: Long,

  @field:Schema(description = "The ID of the last updated licence.")
  val id: Long = 0,
)
