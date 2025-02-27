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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs.NotifyAttentionNeededLicencesService

@Tag(name = Tags.JOBS)
@RestController
@RequestMapping("", produces = [MediaType.APPLICATION_JSON_VALUE])
class NotifyAttentionNeededLicencesController(
  private val notifyAttentionNeededLicencesService: NotifyAttentionNeededLicencesService,
) {

  @ProtectedByIngress
  @PostMapping(value = ["/jobs/send-attention-needed-licences-email"])
  @Operation(
    summary = "Sends the team an email of all licences that require attention.",
    description = "Triggers a job that notifies licences with a status of APPROVED and a CRD or ARD in past or licences with a status of APPROVED, SUBMITTED, IN_PROGRESS, NOT_STARTED with no release date.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Activation job executed.",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised, requires a valid Oauth2 token",
        content = [Content(mediaType = "text/html")],
      ),
    ],
  )
  fun runNotifyAttentionNeededLicencesJob() = notifyAttentionNeededLicencesService.notifyAttentionNeededLicences()
}
