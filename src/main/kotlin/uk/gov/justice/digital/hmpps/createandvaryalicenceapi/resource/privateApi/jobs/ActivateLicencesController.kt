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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs.LicenceActivationService

@Tag(name = Tags.JOBS)
@RestController
@RequestMapping("", produces = [MediaType.APPLICATION_JSON_VALUE])
class ActivateLicencesController(
  private val licenceActivationService: LicenceActivationService,
) {

  @ProtectedByIngress
  @PostMapping(value = ["/jobs/activate-licences"])
  @Operation(
    summary = "Activates and inactivates licences on release day.",
    description = """Triggers a job that activates licences with: 
      <ul>
       <li>a status of APPROVED</li>
       <li>an appropriate HDC approval state based on type</li>
       <li>an LSD of today</li>
       <li>are either IS91 cases or have an NOMIS status beginning with 'INACTIVE'</li>
      </ul> 
       It also inactivates CRD licences that have been approved for HDC. 
       This only fires for licences we haven't processed a release event for.""",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Activation job executed.",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised",
        content = [Content(mediaType = "text/html")],
      ),
    ],
  )
  fun runLicenceActivationJob() = licenceActivationService.licenceActivation()
}
