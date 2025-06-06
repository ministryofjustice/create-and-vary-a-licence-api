package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.privateApi.jobs

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
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.RecalculateLicenceStartDatesRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.Tags
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.LsdRecalculationService

@Tag(name = Tags.JOBS)
@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class UpdateLicenceStartDatesController(
  private val lsdRecalculationService: LsdRecalculationService,
) {
  @PostMapping(value = ["/recalculate-licence-start-dates"])
  @PreAuthorize("hasAnyRole('CVL_ADMIN')")
  @ResponseBody
  @Operation(
    summary = "Recalculates licence start dates",
    description = "Recalculates licence start dates for the given number of licences." +
      " Licences are updated sequentially, from newest to oldest, starting with the one after the provided ID." +
      " Requires ROLE_CVL_ADMIN.",
    security = [SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Licences updated",
        content = [
          Content(mediaType = "application/json"),
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
      ApiResponse(
        responseCode = "404",
        description = "Resource not found",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun recalculateLicenceStartDates(
    @Valid @RequestBody
    body: RecalculateLicenceStartDatesRequest,
  ): Long = lsdRecalculationService.batchUpdateLicenceStartDate(body.batchSize, body.id)
}
