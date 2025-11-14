package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.privateApi.timeserved

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.Tags
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.timeServed.TimeServedCaseload
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.timeServed.TimeServedCaseloadService

@Tag(
  name = Tags.CASELOAD,
  description = "Temporary endpoint to return time served cases",
)
@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class TimeServedCasesController(
  private val timeServedCaseloadService: TimeServedCaseloadService,
) {

  @PostMapping("/cases/time-served/{prisonCode}")
  @PreAuthorize("hasAnyRole('CVL_ADMIN')")
  @Operation(
    summary = "Returns a list of time served cases for a prison",
    security = [SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Returns a list of time served cases",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = TimeServedCaseload::class)),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised, requires a valid Oauth2 token",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an appropriate role",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getTimeServedCases(
    @Parameter(required = true) @PathVariable("prisonCode") prisonCode: String,
  ) = timeServedCaseloadService.getCases(prisonCode)
}
