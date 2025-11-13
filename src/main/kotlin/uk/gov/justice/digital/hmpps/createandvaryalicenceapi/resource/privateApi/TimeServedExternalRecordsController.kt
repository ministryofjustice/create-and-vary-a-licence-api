package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.privateApi

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.persistence.EntityNotFoundException
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.ExternalTimeServedRecordRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response.TimeServedExternalRecordsResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.Tags
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TimeServedExternalRecordService

@Tag(name = Tags.TIME_SERVED_EXTERNAL_RECORDS)
@RestController
@RequestMapping("/time-served/external-records", produces = [MediaType.APPLICATION_JSON_VALUE])
class TimeServedExternalRecordsController(
  private val timeServedExternalRecordService: TimeServedExternalRecordService,
) {
  @PutMapping("/{nomsId}/{bookingId}")
  @PreAuthorize("hasAnyRole('CVL_ADMIN')")
  @Operation(
    summary = "Updates an existing Time Served External Record.",
    description = "Updates the Time Served External Record Reason. Requires ROLE_CVL_ADMIN.",
    security = [SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Licence updated successfully"),
      ApiResponse(
        responseCode = "400",
        description = "Bad request, request body must be valid",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised, requires a valid Oauth2 token",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an appropriate role",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Record not found",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun updateTimeServedExternalRecord(
    @PathVariable("nomsId") nomsId: String,
    @PathVariable("bookingId") bookingId: Long,
    @Valid @RequestBody body: ExternalTimeServedRecordRequest,
  ) = timeServedExternalRecordService.setTimeServedExternalRecord(nomsId, bookingId, body)

  @GetMapping(
    value = ["/{nomsId}/{bookingId}"],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  @PreAuthorize("hasAnyRole('CVL_ADMIN')")
  @Operation(
    summary = "Retrieve the timeserved external record for creating a licence in NOMIS",
    description = "Fetches the timeserved external record for a given NOMIS ID and booking ID. Returns null if no record exists. Requires ROLE_CVL_ADMIN.",
    security = [SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "The timeserved external record was retrieved successfully",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = TimeServedExternalRecordsResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad request, invalid parameters",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised, requires a valid Oauth2 token",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an appropriate role",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "a timeserved external record was not found",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getTimeServedExternalRecord(
    @PathVariable nomsId: String,
    @PathVariable bookingId: Long,
  ) = timeServedExternalRecordService.findByNomsIdAndBookingId(nomsId, bookingId)
    ?: throw EntityNotFoundException("'$nomsId' and booking: $bookingId")
}
