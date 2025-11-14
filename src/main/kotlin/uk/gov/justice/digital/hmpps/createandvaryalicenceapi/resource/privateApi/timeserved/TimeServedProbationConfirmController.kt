package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.privateApi.timeserved

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.timeserved.TimeServedProbationConfirmContactRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.Tags
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.timeserved.TimeServedProbationConfirmContactService

@Tag(name = Tags.TIME_SERVED)
@RestController
@RequestMapping("/licences/time-served")
class TimeServedProbationConfirmController(
  private val timeServedProbationConfirmContactService: TimeServedProbationConfirmContactService,
) {

  companion object {
    private val log = LoggerFactory.getLogger(TimeServedProbationConfirmController::class.java)
  }

  @PutMapping("{licenceId}/confirm/probation-contact")
  @PreAuthorize("hasAnyRole('CVL_ADMIN')")
  @Operation(
    summary = "Adds the probation contact confirmation for a time served licence.",
    description = "Adds the probation contact status and communication methods for a time served licence. Requires ROLE_CVL_ADMIN.",
    security = [SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "204", description = "Probation contact confirmation added successfully"),
      ApiResponse(
        responseCode = "400",
        description = "Bad request — request body must be valid",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised — requires a valid OAuth2 token",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden — requires an appropriate role",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Licence record not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun addTimeServedProbationConfirmContact(
    @PathVariable("licenceId") licenceId: Long,
    @Valid @RequestBody body: TimeServedProbationConfirmContactRequest,
  ) {
    log.info("Adding probation contact confirmation for licenceId=$licenceId with body=$body")
    timeServedProbationConfirmContactService.addConfirmContact(licenceId, body)
    log.debug("Probation contact confirmation added successfully for licenceId=$licenceId")
  }
}
