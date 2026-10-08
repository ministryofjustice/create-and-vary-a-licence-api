package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.privateApi

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.Tags
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.PrisonerReleasedHandler

@Deprecated("this will be replaced by the event listener")
@RestController
@RequestMapping("/licence", produces = [MediaType.APPLICATION_JSON_VALUE])
class PrisonerReleaseController(
  private val prisonerReleasedHandler: PrisonerReleasedHandler,
  @param:Value("\${prisoner.released.handler.enabled:false}") private val handlerEnabled: Boolean = false,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  @Tag(name = Tags.EVENTS)
  @PostMapping(value = ["/trigger-release-prisoner"])
  @PreAuthorize("hasAnyRole('CVL_ADMIN')")
  @Operation(
    summary = "Triggers the release prisoner process.",
    description = "Temp endpoint to trigger the prisoner release process from the frontend. Requires ROLE_CVL_ADMIN.",
    security = [SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "The release processed successfully",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad request, request body must be valid",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
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
  fun processRelease(
    @Valid @RequestBody
    body: PrisonerToReleaseRequest,
  ) {
    if (handlerEnabled) {
      log.debug(
        "Not processing release for: {} as release event processing toggle is enabled",
        body.prisonNumber,
      )
    } else {
      prisonerReleasedHandler.processLicencesOnRelease(body.prisonNumber!!)
    }
  }

  @Schema(description = "Request for providing details about the prisoner being released")
  data class PrisonerToReleaseRequest(
    @field:NotBlank
    @field:Schema(description = "The prisoner's prison number")
    val prisonNumber: String?,
  )
}
