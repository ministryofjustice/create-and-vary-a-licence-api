package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceStatistics
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceStatisticsService

@RestController
@RequestMapping("/support/licence-statistics", produces = [MediaType.APPLICATION_JSON_VALUE])
class LicenceStatisticsController(private val licenceStatisticsService: LicenceStatisticsService) {

  @GetMapping
  @PreAuthorize("hasAnyRole('SYSTEM_USER', 'CVL_ADMIN')")
  @Operation(
    summary = "Get licence statistics.",
    description = "Licence statistics data required by the support staff. Requires ROLE_SYSTEM_USER or ROLE_CVL_ADMIN.",
    security = [SecurityRequirement(name = "ROLE_SYSTEM_USER"), SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Licence statistics found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = LicenceStatistics::class))]
      ),
      ApiResponse(
        responseCode = "404",
        description = "Licence statistics not found",
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
  fun getLicenceStatistics(@RequestParam("startDate") startDate: String, @RequestParam("endDate")endDate: String): List<LicenceStatistics> {
    return this.licenceStatisticsService.getStatistics(startDate, endDate)
  }
}
