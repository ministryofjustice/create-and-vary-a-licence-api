package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.UpdateComRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.ComService

@RestController
@RequestMapping("/com", produces = [MediaType.APPLICATION_JSON_VALUE])
class ComController(private val comService: ComService) {
  @PutMapping(
    value = ["/update"],
    produces = [MediaType.APPLICATION_JSON_VALUE]
  )
  @PreAuthorize("hasAnyRole('SYSTEM_USER', 'CVL_ADMIN')")
  @Operation(
    summary = "Updates the details of a community offender manager.",
    description = "Updates the details of a community offender manager (e.g. email address). Requires ROLE_SYSTEM_USER or ROLE_CVL_ADMIN.",
    security = [SecurityRequirement(name = "ROLE_SYSTEM_USER"), SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "The COM was updated",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad request, request body must be valid",
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
      )
    ]
  )
  fun updateComDetails(
    @RequestBody body: UpdateComRequest
  ) {
    this.comService.updateComDetails(body)
  }
}
