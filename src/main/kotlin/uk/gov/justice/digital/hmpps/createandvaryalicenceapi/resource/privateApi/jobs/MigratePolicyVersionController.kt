package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.privateApi.jobs

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.ProtectedByIngress
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.Tags
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs.MigrateStandardConditionsService

@Tag(name = Tags.JOBS)
@RestController
@RequestMapping("", produces = [MediaType.APPLICATION_JSON_VALUE])
class MigratePolicyVersionController(
  private val migrateStandardConditionsService: MigrateStandardConditionsService,
) {

  @ProtectedByIngress
  @PostMapping(value = ["/jobs/migrate-standard-conditions"])
  @Operation(
    summary = "Migrates standard conditions on in flight licences the requested policy version.",
    description = "Updates the standard conditions for in flight licences to the requested policy version conditions",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "204",
        description = "The job ran successfully",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised, requires a valid Oauth2 token",
        content = [Content(mediaType = "text/html")],
      ),
    ],
  )
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun migrateStandardConditions(
    @RequestParam(name = "policyVersion") policyVersion: String,
  ) = migrateStandardConditionsService.migrateStandardConditions(policyVersion)
}
