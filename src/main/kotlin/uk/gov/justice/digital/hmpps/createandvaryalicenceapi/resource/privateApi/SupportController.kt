package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.privateApi

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.EligibilityAssessment
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.Tags
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.ComAllocatedHandler
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.support.SupportService

@RestController
@Tag(name = Tags.SUPPORT)
@RequestMapping("/offender", produces = [MediaType.APPLICATION_JSON_VALUE])
class SupportController(
  private val supportService: SupportService,
  private val comAllocatedHandler: ComAllocatedHandler,
) {
  @GetMapping(
    value = ["/nomisid/{nomsId}/ineligibility-reasons"],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  @PreAuthorize("hasAnyRole('CVL_ADMIN')")
  @Operation(
    summary = "Retrieve ineligibility reasons for offender",
    description = "Returns ineligibility reasons for creating a licence for a specific prisoner. Requires ROLE_CVL_ADMIN.",
    security = [SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "a list of ineligibility reasons",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = EligibilityAssessment::class),
          ),
        ],
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
        description = "Could not find prisoner",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getIneligibilityReasons(
    @PathVariable nomsId: String,
  ) = supportService.getIneligibilityReasons(nomsId)

  @GetMapping(
    value = ["/nomisid/{nomsId}/is-91-status"],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  @PreAuthorize("hasAnyRole('CVL_ADMIN')")
  @Operation(
    summary = "Retrieve IS-91 status for offender",
    description = "Returns IS-91 status for creating a licence for a specific prisoner. Requires ROLE_CVL_ADMIN.",
    security = [SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "a boolean for IS-91 status",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = Boolean::class),
          ),
        ],
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
        description = "Could not find prisoner",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getIS91Status(
    @PathVariable nomsId: String,
  ) = supportService.getIS91Status(nomsId)

  @PutMapping(value = ["/sync-com/crn/{crn}"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @PreAuthorize("hasAnyRole('CVL_ADMIN')")
  @Operation(
    summary = "Updates the COM allocation for for licences linked to the given CRN.",
    description = "Updates the COM allocation for for licences linked to the given CRN.",
    security = [SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "The COM allocation info was",
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
        description = "Not-found, an invalid CRN was provided",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun syncComAllocation(
    @PathVariable crn: String,
  ) = comAllocatedHandler.syncComAllocation(crn)
}
