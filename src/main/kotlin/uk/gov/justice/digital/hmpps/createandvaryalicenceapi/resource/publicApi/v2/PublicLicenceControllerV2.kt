package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.v2

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping
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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.latestApiVersion
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.v2.model.licence.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.v2.model.licence.PublicHdcLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.publicApi.v2.PublicLicenceServiceV2
import java.util.logging.Logger

@RestController
@Tag(name = Tags.LICENCES)
@PreAuthorize("hasAnyRole('VIEW_LICENCES')")
@RequestMapping("/public/v2", produces = [MediaType.APPLICATION_JSON_VALUE])
class PublicLicenceControllerV2(private val publicLicenceService: PublicLicenceServiceV2) {

  private val log: Logger = Logger.getLogger(this::class.java.name)

  @GetMapping(value = ["/licences/id/{licenceId}"])
  @ResponseBody
  @Operation(
    summary = "Get a licence by its licence id, $latestApiVersion",
    description = "Returns a single licence detail by its unique identifier, including curfew details for HDC licences. " +
      "Requires ROLE_VIEW_LICENCES.",
    security = [SecurityRequirement(name = "ROLE_VIEW_LICENCES")],
    operationId = "getLicenceByIdV2",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Licence found",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(
              oneOf = [Licence::class, PublicHdcLicence::class],
              discriminatorProperty = "kind",
              discriminatorMapping = [
                DiscriminatorMapping(value = "HDC", schema = PublicHdcLicence::class),
                DiscriminatorMapping(value = "HDC_VARIATION", schema = PublicHdcLicence::class),
              ],
            ),
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
      ApiResponse(
        responseCode = "404",
        description = "The licence for this ID was not found.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getLicenceById(
    @PathVariable(name = "licenceId")
    @Parameter(name = "licenceId", description = "This is the identifier for a licence")
    @Min(1)
    licenceId: Long,
  ): Licence {
    log.info("Getting licence by id: $licenceId using $latestApiVersion")
    return publicLicenceService.getLicenceById(licenceId)
  }
}
