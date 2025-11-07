package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.privateApi

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.RecordNomisLicenceReasonRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateNomisLicenceReasonRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response.RecordNomisLicenceReasonResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.Tags
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.RecordNomisTimeServedLicenceReasonService

@Tag(name = Tags.NON_CVL_LICENCE_REASON)
@RestController
@RequestMapping("/time-served/nomis/licence/reason", produces = [MediaType.APPLICATION_JSON_VALUE])
class RecordNomisTimeServedLicenceReasonController(
  private val recordNomisTimeServedLicenceReasonService: RecordNomisTimeServedLicenceReasonService,
) {

  @PostMapping
  @PreAuthorize("hasAnyRole('CVL_ADMIN')")
  @Operation(
    summary = "Records the reason for creating a licence in NOMIS.",
    description = "Stores metadata about why a licence was created in NOMIS. Requires ROLE_CVL_ADMIN.",
    security = [SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "201", description = "Reason recorded successfully"),
      ApiResponse(responseCode = "400", description = "Invalid request body", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
      ApiResponse(responseCode = "401", description = "Unauthorized", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
      ApiResponse(responseCode = "403", description = "Forbidden", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
    ],
  )
  fun recordNomisLicenceReason(
    @Valid @RequestBody body: RecordNomisLicenceReasonRequest,
  ) = recordNomisTimeServedLicenceReasonService.recordNomisLicenceReason(body)

  @PutMapping("/{nomsId}/{bookingId}")
  @PreAuthorize("hasAnyRole('CVL_ADMIN')")
  @Operation(
    summary = "Updates an existing NOMIS Time Served Licence record.",
    description = "Updates the NOMIS Time Served Licence details. Requires ROLE_CVL_ADMIN.",
    security = [SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Licence updated successfully"),
      ApiResponse(responseCode = "400", description = "Bad request, request body must be valid", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token", content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role", content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]),
      ApiResponse(responseCode = "404", description = "Record not found", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
    ],
  )
  fun updateNomisLicenceReason(
    @PathVariable("nomsId") nomsId: String,
    @PathVariable("bookingId") bookingId: Long,
    @Valid @RequestBody body: UpdateNomisLicenceReasonRequest,
  ) = recordNomisTimeServedLicenceReasonService.updateNomisLicenceReason(nomsId, bookingId, body)

  @GetMapping(
    value = ["/{nomsId}/{bookingId}"],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  @PreAuthorize("hasAnyRole('CVL_ADMIN')")
  @Operation(
    summary = "Retrieve the recorded reason for creating a licence in NOMIS",
    description = "Fetches the reason record for a given NOMIS ID and booking ID. Returns null if no record exists. Requires ROLE_CVL_ADMIN.",
    security = [SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "The reason record was retrieved successfully (or null if not found)", content = [Content(mediaType = "application/json", schema = Schema(implementation = RecordNomisLicenceReasonResponse::class))]),
      ApiResponse(responseCode = "400", description = "Bad request, invalid parameters", content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]),
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
    ],
  )
  fun getNomisLicenceReason(
    @PathVariable nomsId: String,
    @PathVariable bookingId: Long,
  ): RecordNomisLicenceReasonResponse? = recordNomisTimeServedLicenceReasonService.findByNomsIdAndBookingId(nomsId, bookingId)
}
