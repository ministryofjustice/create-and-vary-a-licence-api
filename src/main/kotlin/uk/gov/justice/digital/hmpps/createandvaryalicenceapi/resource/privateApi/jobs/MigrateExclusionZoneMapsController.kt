package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.privateApi.jobs

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Min
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.ProtectedByIngress
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.Tags
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions.ExclusionZoneUploadsMigration

@Tag(name = Tags.JOBS)
@RestController
@RequestMapping("", produces = [MediaType.APPLICATION_JSON_VALUE])
class MigrateExclusionZoneMapsController(
  private val exclusionZoneUploadsMigration: ExclusionZoneUploadsMigration,
) {

  @ProtectedByIngress
  @PostMapping(value = ["/jobs/migrate-exclusion-zone-maps"])
  @Operation(
    summary = "Upload all exclusion zone maps from the DB to the document service.",
    description = "Upload all exclusion zone maps from the DB to the document service.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Job executed.",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised, requires a valid Oauth2 token",
        content = [Content(mediaType = "text/html")],
      ),
    ],
  )
  fun runJob(@RequestParam @Min(1) batchSize: Int = 1) = exclusionZoneUploadsMigration.perform(batchSize)
}
