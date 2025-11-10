package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.privateApi.jobs

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.UnapprovedLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.ProtectedByIngress
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.Tags
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs.SendNeedsApprovalReminderService

@Tag(name = Tags.JOBS)
@RestController
@RequestMapping("", produces = [MediaType.APPLICATION_JSON_VALUE])
class SendNeedsApprovalReminderController(private val sendNeedsApprovalReminderService: SendNeedsApprovalReminderService) {

  @ProtectedByIngress
  @PostMapping(value = ["/jobs/notify-probation-of-unapproved-licences"])
  @ResponseBody
  @Operation(
    summary = "Warn of unapproved licences after release",
    description = "Send an email to probation practitioner of any previously approved licences that have been edited but not re-approved by prisoners release date",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Emails sent",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = UnapprovedLicence::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised, requires a valid Oauth2 token",
        content = [Content(mediaType = "text/html")],
      ),
    ],
  )
  fun notifyProbationOfUnapprovedLicences() = sendNeedsApprovalReminderService.sendEmailsToProbationPractitioner()
}
