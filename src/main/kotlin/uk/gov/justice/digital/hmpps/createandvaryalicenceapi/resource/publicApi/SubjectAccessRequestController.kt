package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.Tags
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.SarContent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.publicApi.SubjectAccessRequestService

@RestController
@Tag(name = Tags.SAR)
@PreAuthorize("hasAnyRole('SAR_DATA_ACCESS', 'CVL_ADMIN')")
@RequestMapping("/public", produces = [MediaType.APPLICATION_JSON_VALUE])
class SubjectAccessRequestController(private val subjectAccessRequestService: SubjectAccessRequestService) {
  @GetMapping(value = ["/subject-access-request"])
  @ResponseBody
  @Operation(
    summary = "Get a list of licences and audits summaries matching the nomis Prison Reference Number(prn).",
    description = "Get the licences and audits matching the supplied Returns a list of licences and audit details by its unique identifier. " +
      "Requires ROLE_SAR-DATA-ACCESS or ROLE_CVL_ADMIN.",
    security = [SecurityRequirement(name = "ROLE_SAR_DATA_ACCESS"), SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Record found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = Licence::class))],
      ),
      ApiResponse(
        responseCode = "204",
        description = "The licence for this prn was not found.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = Licence::class))],
      ),
      ApiResponse(
        responseCode = "209",
        description = "CRN not supported",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised, requires a valid Oauth2 token",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an appropriate role",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "500",
        description = "When passed both a prn and crn. CRN not supported.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getSarRecordsById(
    @RequestParam(name = "prn", required = false) prn: String?,
    @RequestParam(name = "crn", required = false) crn: String?,
  ): ResponseEntity<SarContent> {
    val httpStatusWithIncorrectRequest = 209
    if (prn != null && crn != null)
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build<SarContent>()

    if (prn == null && crn != null)
      return ResponseEntity.status(httpStatusWithIncorrectRequest).build<SarContent>()

    val result = prn?.let { subjectAccessRequestService.getSarRecordsById(it) }
    if (result == null)
      return ResponseEntity.status(HttpStatus.NO_CONTENT).build<SarContent>()
    else
      return ResponseEntity.status(HttpStatus.OK).body(result)

  }
}
