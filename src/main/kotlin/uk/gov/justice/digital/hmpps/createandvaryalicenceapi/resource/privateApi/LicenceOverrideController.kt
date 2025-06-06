package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.privateApi

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.OverrideLicenceDatesRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.OverrideLicencePrisonerDetailsRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.OverrideLicenceStatusRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.OverrideLicenceTypeRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.Tags
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.support.LicenceOverrideService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.support.LicenceTypeOverrideService

@Tag(name = Tags.ADMIN)
@RestController
@RequestMapping("/licence/id/{licenceId}/override", produces = [MediaType.APPLICATION_JSON_VALUE])
class LicenceOverrideController(
  private val licenceOverrideService: LicenceOverrideService,
  private val licenceTypeOverrideService: LicenceTypeOverrideService,
) {
  @PostMapping(value = ["/status"])
  @PreAuthorize("hasAnyRole('CVL_ADMIN')")
  @ResponseBody
  @ResponseStatus(HttpStatus.ACCEPTED)
  @Operation(
    summary = "Override a licence status",
    description = "Override the status for an exising licence. Only to be used in exceptional circumstances." +
      " Requires ROLE_CVL_ADMIN.",
    security = [SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "202",
        description = "Status has been updated",
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
      ),
      ApiResponse(
        responseCode = "404",
        description = "The licence for this ID was not found.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun changeStatus(
    @PathVariable("licenceId") licenceId: Long,
    @RequestBody @Valid
    request: OverrideLicenceStatusRequest,
  ) {
    licenceOverrideService.changeStatus(licenceId, request.statusCode, request.reason)
  }

  @PutMapping(value = ["/dates"])
  @PreAuthorize("hasAnyRole('CVL_ADMIN')")
  @ResponseBody
  @ResponseStatus(HttpStatus.ACCEPTED)
  @Operation(
    summary = "Override licence dates",
    description = "Override the dates for an exising licence. Only to be used in exceptional circumstances." +
      " Requires ROLE_CVL_ADMIN.",
    security = [SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Licence dates have been updated",
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
      ),
      ApiResponse(
        responseCode = "404",
        description = "The licence for this ID was not found.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun changeDates(
    @PathVariable("licenceId") licenceId: Long,
    @RequestBody @Valid
    request: OverrideLicenceDatesRequest,
  ) {
    licenceOverrideService.changeDates(licenceId, request)
  }

  @PostMapping(value = ["/type"])
  @PreAuthorize("hasAnyRole('CVL_ADMIN')")
  @ResponseBody
  @ResponseStatus(HttpStatus.ACCEPTED)
  @Operation(
    summary = "Override a licence type",
    description = "Override the type for an exising licence. Only to be used in exceptional circumstances." +
      " Requires ROLE_CVL_ADMIN.",
    security = [SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "202",
        description = "Status has been updated",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad request, request body must be valid",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ProblemDetail::class))],
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
        description = "The licence for this ID was not found.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun changeType(
    @PathVariable("licenceId") licenceId: Long,
    @RequestBody @Valid
    request: OverrideLicenceTypeRequest,
  ) {
    licenceTypeOverrideService.changeType(licenceId, request.licenceType, request.reason)
  }

  @PostMapping(value = ["/prisoner-details"])
  @PreAuthorize("hasAnyRole('CVL_ADMIN')")
  @ResponseBody
  @ResponseStatus(HttpStatus.ACCEPTED)
  @Operation(
    summary = "Override prisoner details on a licence",
    description = "Override the prisoner details for an existing licence. Only to be used in exceptional circumstances." +
      " Requires ROLE_CVL_ADMIN.",
    security = [SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "202", description = "Prisoner details updated"),
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
      ),
      ApiResponse(
        responseCode = "404",
        description = "The licence for this ID was not found.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun changePrisonerDetails(
    @PathVariable("licenceId") licenceId: Long,
    @RequestBody @Valid request: OverrideLicencePrisonerDetailsRequest,
  ) {
    licenceOverrideService.changePrisonerDetails(licenceId, request)
  }
}
