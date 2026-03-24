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

/**
   * isr-in-flight-ap-pss-licences-job
   */
  @ProtectedByIngress
  @PostMapping(value = ["/jobs/isr-licence-progression-job"])
  @Operation(
    summary = "Progress AP+PSS licences.",
    description = """
            Triggers a job to progress licences types :
                a) PSS into inactive status
                b) AP_PSS into AP type and remove PSS conditions  
    """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Progression of PSS and AP_PSS licences job executed.",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised",
        content = [Content(mediaType = "text/html")],
      ),
    ],
  )
  fun progressionOfTypeApPssLicences() {
    progressionService.process()
  }
}
