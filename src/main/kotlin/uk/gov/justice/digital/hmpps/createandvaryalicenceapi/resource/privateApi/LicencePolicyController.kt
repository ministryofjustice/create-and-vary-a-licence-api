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
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ModelVariation
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.LicencePolicy
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.Tags
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.LicenceConditionChanges
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.LicencePolicyService

@Tag(name = Tags.LICENCE_POLICY)
@RestController
@RequestMapping("/licence-policy", produces = [MediaType.APPLICATION_JSON_VALUE])
class LicencePolicyController(
  private val licencePolicyService: LicencePolicyService,
  private val licenceService: LicenceService,
) {

  @GetMapping(value = ["/active"])
  @PreAuthorize("hasAnyRole('CVL_ADMIN')")
  @ResponseBody
  @Operation(
    summary = "Get the active licence policy",
    description = "Returns the active policy using its unique identifier. " +
      "Requires ROLE_CVL_ADMIN.",
    security = [SecurityRequirement(name = "ROLE_CVL_ADMIN")],
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
    ],
  )
  fun getCurrentPolicy(): LicencePolicy? = licencePolicyService.currentPolicy()

  @GetMapping(value = ["/version/{version}"])
  @PreAuthorize("hasAnyRole('CVL_ADMIN')")
  @ResponseBody
  @Operation(
    summary = "Get licence policy be version number",
    description = "Returns a single policy using its unique identifier. " +
      "Requires ROLE_CVL_ADMIN.",
    security = [SecurityRequirement(name = "ROLE_CVL_ADMIN")],
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
      ),
    ],
  )
  fun getPolicyByVersion(@PathVariable("version") version: String): LicencePolicy? = licencePolicyService.policyByVersion(version)

  @GetMapping(value = ["/"])
  @PreAuthorize("hasAnyRole('CVL_ADMIN')")
  @ResponseBody
  @Operation(
    summary = "Get all licence policy versions",
    description = "Returns a list of policies, active and present" +
      "Requires ROLE_CVL_ADMIN.",
    security = [SecurityRequirement(name = "ROLE_CVL_ADMIN")],
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
      ),
    ],
  )
  fun getPolicies(): List<LicencePolicy> = licencePolicyService.allPolicies()

  @GetMapping(value = ["/compare/{version}/licence/{licenceId}"])
  @PreAuthorize("hasAnyRole('CVL_ADMIN')")
  @ResponseBody
  @Operation(
    summary = "Get differences between saved licences conditions and new policy",
    description = "Returns condition data saved against a licence no longer present within the new licence policy" +
      "Requires ROLE_CVL_ADMIN.",
    security = [SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  fun compareLicence(
    @PathVariable("version") version: String,
    @PathVariable("licenceId") licenceId: Long,
  ): List<LicenceConditionChanges>? = when (val currentLicence = licenceService.getLicenceById(licenceId)) {
    is ModelVariation -> getPolicyChanges(currentLicence, currentLicence.variationOf, version)

    else -> emptyList()
  }

  fun getPolicyChanges(currentLicence: ModelVariation, parentLicenceId: Long?, version: String): List<LicenceConditionChanges> {
    if (parentLicenceId === null) return emptyList()
    val parentLicence = licenceService.getLicenceById(parentLicenceId)

    return licencePolicyService.compareLicenceWithPolicy(
      currentLicence,
      parentLicence,
      version,
    )
  }
}
