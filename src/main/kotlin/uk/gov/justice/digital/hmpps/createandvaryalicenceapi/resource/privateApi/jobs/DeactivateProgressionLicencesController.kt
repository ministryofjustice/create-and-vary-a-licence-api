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
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.ProtectedByIngress
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.Tags.Companion.JOBS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs.DeactivateProgressionLicencesService

@Tag(name = JOBS)
@RestController
@RequestMapping("", produces = [MediaType.APPLICATION_JSON_VALUE])
class DeactivateProgressionLicencesController(
  private val deactivateProgressionLicencesService: DeactivateProgressionLicencesService,
) {

  @ProtectedByIngress
  @PostMapping(value = ["/jobs/deactivate-progression-licences"])
  @Operation(
    summary = "Deactivate licences not on policy version 4 which have release dates beyond the policy version 4 go-live date.",
    description = "Deactivate any in-flight (IN_PROGRESS, SUBMITTED, APPROVED, and TIMED_OUT) licences not on policy version 4 that have licence start dates beyond the policy version 4 go-live date.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "204",
        description = "Deactivation request for progression licences is processed successfully.",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised",
        content = [Content(mediaType = "text/html")],
      ),
    ],
  )
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun runDeactivateProgressionLicencesJob() = deactivateProgressionLicencesService.deactivateLicences()
}
