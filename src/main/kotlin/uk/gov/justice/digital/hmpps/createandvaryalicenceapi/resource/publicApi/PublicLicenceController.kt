package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Min
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
  fun getLicenceById(
    @PathVariable("licenceId")
    @Parameter(name = "licenceId", description = "This is the identifier for a licence")
    @Min(1)
    licenceId: Long,
  ): Licence? {
    return publicLicenceService.getLicenceById(licenceId)
  }

  @GetMapping(value = ["/licence-summaries/prison-number/{prisonNumber}"])
  @ResponseBody
  @Operation(
    summary = "Get a list of in flight licences by prison number",
    description = "Returns a list of licence summaries by a person's prison number. " +
      "Requires ROLE_VIEW_LICENCES.",
    security = [SecurityRequirement(name = "ROLE_VIEW_LICENCES")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "A list of found licence summaries",
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
  fun getLicencesByPrisonNumber(
    @PathVariable("prisonNumber")
    @Parameter(
      name = "prisonNumber",
      description = "The prison identifier for the person on the licence (also known as NOMS id)",
      example = "A1234BC",
    )
    prisonNumber: String,
  ) = publicLicenceService.getAllLicencesByPrisonNumber(prisonNumber)

  @GetMapping(value = ["/licence-summaries/crn/{crn}"])
  @ResponseBody
  @Operation(
    summary = "Get a list of in flight licences by CRN",
    description = "Returns a list of licence summaries by a person's CRN. " +
      "Requires ROLE_VIEW_LICENCES.",
    security = [SecurityRequirement(name = "ROLE_VIEW_LICENCES")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "A list of found licence summaries",
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
  fun getLicenceByCrn(
    @PathVariable("crn")
    @Parameter(
      name = "crn",
      description = "The case reference number (CRN) for the person on the licence",
      example = "A123456",
    )
    crn: String,
  ) = publicLicenceService.getAllLicencesByCrn(crn)

  @GetMapping(
    value = ["/licences/{licenceId}/conditions/{conditionId}/image-upload"],
    produces = [MediaType.IMAGE_JPEG_VALUE],
  )
  @ResponseBody
  @Operation(
    summary = "Get an associated image upload for a specific licence and condition",
    description = "Returns an associated image upload for a specified licence and condition. " +
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
  fun getImageUpload(
    @PathVariable(name = "licenceId")
    @Parameter(
      name = "licenceId",
      description = "This is the identifier for a licence",
    )
    licenceId: Long,
    @PathVariable(name = "conditionId")
    @Parameter(
      name = "conditionId",
      description = "This is the internal identifier for a condition",
    )
    conditionId: Long,
  ): ByteArray? {
    return publicLicenceService.getImageUpload(licenceId, conditionId)
  }
}
