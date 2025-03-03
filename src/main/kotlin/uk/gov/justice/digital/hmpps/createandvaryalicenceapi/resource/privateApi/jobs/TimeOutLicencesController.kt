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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs.TimeOutLicencesService

@Tag(name = Tags.JOBS)
@RestController
@RequestMapping("", produces = [MediaType.APPLICATION_JSON_VALUE])
class TimeOutLicencesController(
  private val timeOutLicencesService: TimeOutLicencesService,
) {

  @ProtectedByIngress
  @PostMapping(value = ["/jobs/time-out-licences"])
  @Operation(
    summary = "Times out licences that have hit hard stop.",
    description = "Triggers a job that causes licences with a status of IN_PROGRESS and a CRD or ARD less than two working days to be updated to TIMED_OUT.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Time out job executed.",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised",
        content = [Content(mediaType = "text/html")],
      ),
    ],
  )
  fun runTimeOutLicencesServiceJob() = timeOutLicencesService.timeOutLicences()
}
