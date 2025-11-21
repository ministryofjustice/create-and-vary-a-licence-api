package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.privateApi

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.UpdateComRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateOffenderDetailsRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateProbationTeamRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.Tags
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.OffenderService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.StaffService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.ComAllocatedHandler

@RestController
@Tag(name = Tags.OFFENDER)
@RequestMapping("/offender", produces = [MediaType.APPLICATION_JSON_VALUE])
class OffenderController(
  private val comAllocatedHandler: ComAllocatedHandler,
  private val offenderService: OffenderService,
  private val staffService: StaffService,
  @param:Value("\${domain.event.listener.enabled}") private val domainEventListenerEnabled: Boolean,
  @param:Value("\${update.offender.details.handler.enabled}") private val updateOffenderDetailsHandleEnabled: Boolean,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @PutMapping(
    value = ["/crn/{crn}/responsible-com"],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  @PreAuthorize("hasAnyRole('CVL_ADMIN')")
  @Operation(
    summary = "Updates in-flight licences associated with an offender with the community offender manager who is responsible for that offender.",
    description = "Updates in-flight licences associated with an offender with the community offender manager who is responsible for that offender. Requires ROLE_CVL_ADMIN.",
    security = [SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "The responsible COM was updated",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad request, request body must be valid",
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
    ],
  )
  fun updateResponsibleCom(
    @PathVariable crn: String,
    @Valid @RequestBody
    body: UpdateComRequest,
  ) {
    if (domainEventListenerEnabled) {
      log.info("Domain event listener to process COM allocation events enabled so ignoring updateResponsibleCom Endpoint!")
    } else {
      val newCom = staffService.updateComDetails(body)
      offenderService.updateResponsibleCom(crn, newCom)
    }
  }

  @PutMapping(
    value = ["/crn/{crn}/probation-team"],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  @PreAuthorize("hasAnyRole('CVL_ADMIN')")
  @Operation(
    summary = "Updates in-flight licences associated with an offender with a new probation team.",
    description = "Updates in-flight licences associated with an offender with a new probation team. Requires ROLE_CVL_ADMIN.",
    security = [SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "The probation team was updated",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad request, request body must be valid",
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
    ],
  )
  fun updateProbationTeam(
    @PathVariable crn: String,
    @Valid @RequestBody
    body: UpdateProbationTeamRequest,
  ) {
    if (domainEventListenerEnabled) {
      log.info("updateProbationTeam : Domain event listener to process COM allocation events enabled so ignoring updateProbationTeam Endpoint!")
    } else {
      this.offenderService.updateProbationTeam(crn, body)
    }
  }

  @PutMapping(value = ["nomisid/{nomsId}/update-offender-details"])
  @PreAuthorize("hasAnyRole('CVL_ADMIN')")
  @Operation(
    summary = "Updates the offender's personal information on all of their licences.",
    description = "Updates the name and date of birth stored on all licences associated with the given offender. Requires ROLE_CVL_ADMIN.",
    security = [SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "The offender details were updated",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad request, request body must be valid",
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
    ],
  )
  fun updateOffenderDetails(
    @PathVariable nomsId: String,
    @Valid @RequestBody
    body: UpdateOffenderDetailsRequest,
  ) {
    if (!updateOffenderDetailsHandleEnabled) {
      this.offenderService.updateOffenderDetails(nomsId, body)
    }
  }

  @PutMapping(value = ["/sync-com/crn/{crn}"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @PreAuthorize("hasAnyRole('CVL_ADMIN')")
  @Operation(
    summary = "Updates the COM allocation for for licences linked to the given CRN.",
    description = "Updates the COM allocation for for licences linked to the given CRN.",
    security = [SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "The COM allocation info was",
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
        responseCode = "404",
        description = "Not-found, an invalid CRN was provided",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun syncComAllocation(
    @PathVariable crn: String,
  ) = comAllocatedHandler.syncComAllocation(crn)
}
