package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.privateApi.jobs

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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.Tags
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs.promptingCom.Com
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs.promptingCom.PromptComService

@Tag(name = Tags.JOBS)
@RestController
@RequestMapping("/jobs", produces = [MediaType.APPLICATION_JSON_VALUE])
class PromptComController(
  private val promptComService: PromptComService,
) {
  @GetMapping(value = ["/prompt-com"])
  @PreAuthorize("hasAnyRole('CVL_ADMIN')")
  @Operation(
    summary = "Retrieve a list of cases that we will send an email for.",
    description = "Retrieve a list of cases that we have identified to encourage them to progress cases. Requires ROLE_CVL_ADMIN.",
    security = [SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "list of COMs we'll send email to",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = Com::class)),
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
  fun run(): List<Com> {
    return promptComService.runJob()
  }
}
