package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.privateApi

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response.LastMinuteHandoverCaseResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response.UpcomingReleasesWithMonitoringConditionsResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.Tags
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.reports.LastMinuteHandoverCaseService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.reports.UpcomingReleasesWithMonitoringConditionsReportService

@RestController
@Tag(name = Tags.REPORTS)
@RequestMapping(value = ["/cvl-report"], produces = [MediaType.APPLICATION_JSON_VALUE])
class CvlReportController(
  private val lastMinuteHandoverCaseService: LastMinuteHandoverCaseService,
  private val upcomingElectronicMonitoringCasesService: UpcomingReleasesWithMonitoringConditionsReportService,
) {
  @GetMapping("/last-minute-handover-cases")
  @PreAuthorize("hasAnyRole('CVL_ADMIN')")
  @Operation(
    summary = "Retrieve list of cases that need to be reported to the TAG team",
    description = "Returns a list of LastMinuteHandoverCaseResponse objects",
    security = [SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "A list of last minute handover cases",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = LastMinuteHandoverCaseResponse::class)),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad request, request parameters must be valid",
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
    ],
  )
  fun getLastMinuteCases(): List<LastMinuteHandoverCaseResponse> = lastMinuteHandoverCaseService.getLastMinuteCases()

  @GetMapping("/upcoming-releases-with-monitoring")
  @PreAuthorize("hasAnyRole('CVL_ADMIN')")
  @Operation(
    summary = "Retrieve list of upcoming cases with electronic monitoring conditions for FTR-56 report",
    description = "Returns a list of LastMinuteHandoverCaseResponse objects",
    security = [SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "A list of last minute handover cases",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = UpcomingReleasesWithMonitoringConditionsResponse::class)),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad request, request parameters must be valid",
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
    ],
  )
  fun getUpcomingReleasesWithMonitoringConditions(): List<UpcomingReleasesWithMonitoringConditionsResponse> = upcomingElectronicMonitoringCasesService.getUpcomingReleasesWithMonitoringConditions()
}
