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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs.RemoveExpiredConditionsService

@Tag(name = Tags.JOBS)
@RestController
@RequestMapping("", produces = [MediaType.APPLICATION_JSON_VALUE])
class RemoveExpiredConditionsController(
  private val removeExpiredConditionsService: RemoveExpiredConditionsService,
) {
  @ProtectedByIngress
  @PostMapping(value = ["/jobs/remove-expired-conditions"])
  @Operation(
    summary = "Remove AP conditions from in-progress variations after PSS starts.",
    description = "Triggers a job that removes AP conditions for all licences that are in PSS period and status equal to 'VARIATION_IN_PROGRESS' or 'VARIATION_SUBMITTED' or 'VARIATION_REJECTED' or 'VARIATION_APPROVED'.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "run-remove-ap-conditions-job",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised",
        content = [Content(mediaType = "text/html")],
      ),
    ],
  )
  fun runRemoveExpiredConditionsJob() = removeExpiredConditionsService.removeExpiredConditions()
}
