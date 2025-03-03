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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs.LicenceExpiryService

@Tag(name = Tags.JOBS)
@RestController
@RequestMapping("", produces = [MediaType.APPLICATION_JSON_VALUE])
class ExpireLicencesController(
  private val licenceExpiryService: LicenceExpiryService,
) {

  @ProtectedByIngress
  @PostMapping(value = ["/jobs/expire-licences"])
  @Operation(
    summary = "Expires licences that have ended.",
    description = "Triggers a job that causes licences with an ACTIVE status to be deactivated if they are passed their TUSED (if present) or LED (if TUSED is null).",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Expiry job executed.",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised, requires a valid Oauth2 token",
        content = [Content(mediaType = "text/html")],
      ),
    ],
  )
  fun runLicenceExpiryJob() = licenceExpiryService.expireLicences()
}
