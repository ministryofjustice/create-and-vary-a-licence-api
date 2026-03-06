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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs.ISRPssProgressionService

@Tag(name = Tags.JOBS)
@RestController
@RequestMapping("", produces = [MediaType.APPLICATION_JSON_VALUE])
class ISRPssProgressionJobController(
  val progressionService: ISRPssProgressionService,
) {

  @ProtectedByIngress
  @PostMapping(value = ["/jobs/isr-in-flight-ap-pss-licences"])
  @Operation(
    summary = "Progress AP+PSS licences.",
    description = """
            Triggers a job to progress licences currently in one of the following states:
            IN_PROGRESS, SUBMITTED, or APPROVED.
        
            The job applies where:
            - The TUSSD (Top-Up Supervision Start Date) is on or after 30/04/2026
            - The licence type code is AP+PSS
        
            The licence will then be updated to:
            <ul>
              <li>Change the type code to AP</li>
              <li>Remove PSS standard conditions</li>
              <li>Remove PSS additional conditions</li>
            </ul>
    """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Progression of AP+PSS licences job executed.",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised",
        content = [Content(mediaType = "text/html")],
      ),
    ],
  )
  fun progressionOfTypeApPssLicences() {
    progressionService.processApPssLicences()
  }
}
