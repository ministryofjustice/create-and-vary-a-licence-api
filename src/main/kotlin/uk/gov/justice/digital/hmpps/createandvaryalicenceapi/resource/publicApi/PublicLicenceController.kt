package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi

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
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.Tags
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.LicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.publicApi.PublicLicenceService

@RestController
@Tag(name = Tags.LICENCES)
@PreAuthorize("hasAnyRole('VIEW_LICENCES')")
@RequestMapping("/public", produces = [MediaType.APPLICATION_JSON_VALUE])
class PublicLicenceController(private val publicLicenceService: PublicLicenceService) {
  @GetMapping(value = ["/licences/id/{licenceId}"])
  @ResponseBody
  @Operation(
    summary = "Get a licence by its licence id",
    description = "Returns a single licence detail by its unique identifier. " +
      "Requires ROLE_VIEW_LICENCES.",
    security = [SecurityRequirement(name = "ROLE_VIEW_LICENCES")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Licence found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = Licence::class))],
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
  fun getLicenceById(): Licence? {
    val licence: Licence? = null
    return licence
  }

  @GetMapping(value = ["/licence-summaries/prison-number/{prisonNumber}"])
  @ResponseBody
  @Operation(
    summary = "Get a list of licences by prison number",
    description = "Returns a list of licence summaries by a person's prison number. " +
      "Requires ROLE_VIEW_LICENCES.",
    security = [SecurityRequirement(name = "ROLE_VIEW_LICENCES")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "A list of found licences",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = LicenceSummary::class)),
          ),
        ],
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
  fun getLicencesByPrisonNumber(@PathVariable("prisonNumber") prisonNumber: String) =
    publicLicenceService.getAllLicencesByPrisonNumber(prisonNumber)

  @GetMapping(value = ["/licence-summaries/crn/{crn}"])
  @ResponseBody
  @Operation(
    summary = "Get a list of licences by CRN",
    description = "Returns a list of licence summaries by a person's CRN. " +
      "Requires ROLE_VIEW_LICENCES.",
    security = [SecurityRequirement(name = "ROLE_VIEW_LICENCES")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "A list of found licences",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = LicenceSummary::class)),
          ),
        ],
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
  fun getLicenceByCrn(@PathVariable("crn") crn: String) = publicLicenceService.getAllLicencesByCrn(crn)

  @GetMapping(
    value = ["/licences/{licenceId}/conditions/{conditionId}/image-upload"],
    produces = [MediaType.IMAGE_JPEG_VALUE],
  )
  @ResponseBody
  @Operation(
    summary = "Get the exclusion zone map image for a specified licence and condition",
    description = "Returns the exclusion zone map image for a specified licence and condition. " +
      "Requires ROLE_VIEW_LICENCES.",
    security = [SecurityRequirement(name = "ROLE_VIEW_LICENCES")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Image returned",
        content = [Content(mediaType = "image/jpeg")],
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
        description = "No image was found.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getExclusionZoneImageByConditionId(
    @PathVariable(name = "licenceId") licenceId: Long,
    @PathVariable(name = "conditionId") conditionId: Long,
  ): ByteArray? {
    return publicLicenceService.getExclusionZoneImageByConditionId(licenceId, conditionId)
  }
}
