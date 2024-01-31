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
    description = "Returns a list of licences and audit details for the Prison Reference Number(prn). " +
      "Requires ROLE_SAR_DATA_ACCESS or ROLE_CVL_ADMIN.",
    security = [SecurityRequirement(name = "ROLE_SAR_DATA_ACCESS"), SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Records found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = SarContent::class))],
      ),
      ApiResponse(
        responseCode = "204",
        description = "Records for this prn was not found.",
      ),
      ApiResponse(
        responseCode = "209",
        description = "Search by crn is not supported.",
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
        description = "Unexpected error occurred",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getSarRecordsById(
    @RequestParam(name = "prn", required = false) prn: String?,
    @RequestParam(name = "crn", required = false) crn: String?,
  ): ResponseEntity<Any> {
    val httpStatusWithIncorrectRequest = 209
    check(!(crn != null && prn != null)) { "Only supports search by single identifier." }

    if (crn != null) {
      return ResponseEntity.status(httpStatusWithIncorrectRequest).body(
        ErrorResponse(
          status = httpStatusWithIncorrectRequest,
          userMessage = "Search by crn is not supported.",
          developerMessage = "Search by crn is not supported.",
        ),
      )
    }

    val result = prn?.let { subjectAccessRequestService.getSarRecordsById(it) }
    return if (result == null) {
      ResponseEntity.status(HttpStatus.NO_CONTENT).body(
        ErrorResponse(
          status = HttpStatus.NO_CONTENT,
          userMessage = "No records found for the prn.",
          developerMessage = "No records found for the prn.",
        ),
      )
    } else {
      ResponseEntity.status(HttpStatus.OK).body(result)
    }
  }
}
