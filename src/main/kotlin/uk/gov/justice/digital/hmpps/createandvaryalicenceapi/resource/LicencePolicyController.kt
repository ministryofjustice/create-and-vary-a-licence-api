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
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.LicencePolicy
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.PolicyChanges
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicencePolicyService

@RestController
@RequestMapping("/licence-policy", produces = [MediaType.APPLICATION_JSON_VALUE])
class LicencePolicyController(private val licenceConditionsService: LicencePolicyService) {

  @GetMapping(value = ["/active"])
  @PreAuthorize("hasAnyRole('SYSTEM_USER', 'CVL_ADMIN')")
  @ResponseBody
  @Operation(
    summary = "Get the active licence policy",
    description = "Returns the active policy using its unique identifier. " +
      "Requires ROLE_SYSTEM_USER or ROLE_CVL_ADMIN.",
    security = [SecurityRequirement(name = "ROLE_SYSTEM_USER"), SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Licence Policy found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = LicencePolicy::class))],
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
  fun getCurrentPolicy(): LicencePolicy? {
    return licenceConditionsService.currentPolicy()
  }

  @GetMapping(value = ["/version/{version}"])
  @PreAuthorize("hasAnyRole('SYSTEM_USER', 'CVL_ADMIN')")
  @ResponseBody
  @Operation(
    summary = "Get licence policy be version number",
    description = "Returns a single policy using its unique identifier. " +
      "Requires ROLE_SYSTEM_USER or ROLE_CVL_ADMIN.",
    security = [SecurityRequirement(name = "ROLE_SYSTEM_USER"), SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Licence Policy found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = LicencePolicy::class))],
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
      )
    ]
  )
  fun getPolicyByVersion(@PathVariable("version") version: String): LicencePolicy? {
    return licenceConditionsService.policyByVersion(version)
  }

  @GetMapping(value = ["/"])
  @PreAuthorize("hasAnyRole('SYSTEM_USER', 'CVL_ADMIN')")
  @ResponseBody
  @Operation(
    summary = "Get all licence policy versions",
    description = "Returns a list of policies, active and present" +
      "Requires ROLE_SYSTEM_USER or ROLE_CVL_ADMIN.",
    security = [SecurityRequirement(name = "ROLE_SYSTEM_USER"), SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Licence Policy found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = LicencePolicy::class))],
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
      )
    ]
  )
  fun getPolicies(): List<LicencePolicy> {
    return licenceConditionsService.allPolicies()
  }

  @GetMapping(value = ["/compare/{version1}/{version2}"])
  @PreAuthorize("hasAnyRole('SYSTEM_USER', 'CVL_ADMIN')")
  @ResponseBody
  @Operation(
    summary = "Get licence policy differences between two versions",
    description = "Returns a list of added, remove or amended policy conditions between two versions" +
      "Requires ROLE_SYSTEM_USER or ROLE_CVL_ADMIN.",
    security = [SecurityRequirement(name = "ROLE_SYSTEM_USER"), SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  fun compare(@PathVariable("version1") version1: String, @PathVariable("version2") version2: String): PolicyChanges? {
    val policy1 = licenceConditionsService.policyByVersion("1.0")
    val policy2 = licenceConditionsService.policyByVersion("2.0")
    return licenceConditionsService.compare(policy1, policy2)
  }
}
