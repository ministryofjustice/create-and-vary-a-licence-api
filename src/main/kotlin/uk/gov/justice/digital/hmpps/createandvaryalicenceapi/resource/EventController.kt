package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.EventQueryObject
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.EventService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceEventType

@RestController
@RequestMapping("/events", produces = [MediaType.APPLICATION_JSON_VALUE])
class EventController(private val eventService: EventService) {

  @GetMapping(value = ["/match"])
  @PreAuthorize("hasAnyRole('CVL_ADMIN')")
  @Operation(
    summary = "Get a list of licence events that match the supplied criteria.",
    description = "Get a list of licence events that match the supplied criteria. Requires ROLE_CVL_ADMIN.",
    security = [SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Returned matching licence events - empty if no matches.",
        content = [Content(mediaType = "application/json", array = ArraySchema(schema = Schema(implementation = LicenceEvent::class)))],
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
  fun getEventsMatchingCriteria(
    @RequestParam(name = "licenceId", required = false) licenceId: Long?,
    @RequestParam(name = "eventType", required = false) eventType: List<LicenceEventType>?,
    @RequestParam(name = "sortBy", required = false) sortBy: String?,
    @RequestParam(name = "sortOrder", required = false) sortOrder: String?,
  ): List<LicenceEvent> {
    return eventService.findEventsMatchingCriteria(
      EventQueryObject(licenceId = licenceId, eventTypes = eventType, sortBy = sortBy, sortOrder = sortOrder),
    )
  }
}
