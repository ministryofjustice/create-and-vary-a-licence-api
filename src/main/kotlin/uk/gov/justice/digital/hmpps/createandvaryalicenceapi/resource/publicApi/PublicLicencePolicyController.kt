package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
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
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.Tags
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licencePolicy.LicencePolicy
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.publicApi.PublicLicencePolicyService

@RestController
@Tag(name = Tags.LICENCE_POLICY)
@PreAuthorize("hasAnyRole('VIEW_LICENCES')")
@RequestMapping("/public/policy", produces = [MediaType.APPLICATION_JSON_VALUE])
class PublicLicencePolicyController(private val publicLicencePolicyService: PublicLicencePolicyService) {
  @GetMapping(value = ["/{version}"])
  @ResponseBody
  @Operation(
    summary = "Get a policy by its version number",
    description = "Returns a policy by its version number. " +
      "Requires ROLE_VIEW_LICENCES.",
    security = [SecurityRequirement(name = "ROLE_VIEW_LICENCES")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Policy found",
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
        description = "The policy for this version was not found.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getPolicyByVersionNumber(
    @PathVariable("version")
    @Parameter(
      name = "version",
      description = "The version of the licence policy",
      example = "V2_1",
    )
    versionNumber: String,
  ) =
    publicLicencePolicyService.getLicencePolicyByVersionNumber(versionNumber)

  @GetMapping(value = ["/latest"])
  @ResponseBody
  @Operation(
    summary = "Get latest policy.",
    description = "Returns latest policy. " +
      "Requires ROLE_VIEW_LICENCES.",
    security = [SecurityRequirement(name = "ROLE_VIEW_LICENCES")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Policy found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = LicencePolicy::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised, requires a valid Oauth2 token.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an appropriate role.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getLatestPolicy() = publicLicencePolicyService.getLatestLicencePolicy()
}
