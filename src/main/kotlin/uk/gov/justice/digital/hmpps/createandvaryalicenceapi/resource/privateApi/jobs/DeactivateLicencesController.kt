package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.privateApi.jobs

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.ProtectedByIngress
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.Tags.Companion.JOBS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs.DeactivateLicencesService

@Tag(name = JOBS)
@RestController
@RequestMapping("", produces = [MediaType.APPLICATION_JSON_VALUE])
class DeactivateLicencesController(
  private val deactivateLicencesService: DeactivateLicencesService,
) {

  @ProtectedByIngress
  @PostMapping(value = ["/jobs/deactivate-licences-past-release-date"])
  @Operation(
    summary = "Deactivate licences which are past release date.",
    description = "Deactivate licences from IN_PROGRESS and SUBMITTED status to INACTIVE where these are past release date already.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "204",
        description = "Deactivation request for licences past release date is processed successfully.",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised",
        content = [Content(mediaType = "text/html")],
      ),
    ],
  )
  fun runDeactivateLicencesJob() = deactivateLicencesService.deactivateLicences()
}
