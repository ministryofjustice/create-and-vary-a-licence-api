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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.Tags
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs.HardStopLicenceReviewOverdueService

@Tag(name = Tags.JOBS)
@RestController
@RequestMapping("", produces = [MediaType.APPLICATION_JSON_VALUE])
class WarnHardStopReviewOverdueController(
  private val hardStopLicenceReviewOverdueService: HardStopLicenceReviewOverdueService,
) {

  @ProtectedByIngress
  @PostMapping(value = ["/jobs/warn-hard-stop-review-overdue"])
  @Operation(
    summary = "Reminds COMs to review hard stop licences after release.",
    description = "Triggers a job that sends a notification when the COM has not reviewed a hard stop licence 5 days after activation.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Hard stop licence review overdue job executed.",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised",
        content = [Content(mediaType = "text/html")],
      ),
    ],
  )
  fun runHardStopLicenceReviewOverdueJob() = hardStopLicenceReviewOverdueService.sendComReviewEmail()
}
