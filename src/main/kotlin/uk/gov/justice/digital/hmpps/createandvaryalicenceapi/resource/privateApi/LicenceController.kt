package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.privateApi

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalConditionsRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AppointmentAddressRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AppointmentPersonRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AppointmentTimeRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.BespokeConditionRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ContactNumberRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.HardStopCutoffDate
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.StatusUpdateRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.UpdateAdditionalConditionDataRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.UpdateStandardConditionDataRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.AddAdditionalConditionRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.CreateLicenceRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.MatchLicencesRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.NotifyRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.RecentlyApprovedLicencesRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.ReferVariationRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdatePrisonInformationRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateReasonForVariationRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateSentenceDatesRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateSpoDiscussionRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateVloDiscussionRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceQueryObject
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.Tags
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceConditionService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceCreationService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.UpdateSentenceDateService
import java.time.LocalDate

@RestController
@RequestMapping("/licence", produces = [MediaType.APPLICATION_JSON_VALUE])
class LicenceController(
  private val licenceService: LicenceService,
  private val updateSentenceDateService: UpdateSentenceDateService,
  private val licenceConditionService: LicenceConditionService,
  private val licenceCreationService: LicenceCreationService,
) {

  @Tag(name = Tags.LICENCES)
  @PostMapping(value = ["/create"])
  @PreAuthorize("hasAnyRole('SYSTEM_USER', 'CVL_ADMIN')")
  @ResponseBody
  @Operation(
    summary = "Create a licence",
    description = "Creates a licence with the default status IN_PROGRESS and populates with the details provided." +
      " Requires ROLE_SYSTEM_USER or ROLE_CVL_ADMIN.",
    security = [SecurityRequirement(name = "ROLE_SYSTEM_USER"), SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Licence created",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = LicenceSummary::class)),
        ],
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
  fun createLicence(
    @RequestBody @Valid
    request: CreateLicenceRequest,
  ): LicenceSummary {
    return licenceCreationService.createLicence(request.nomsId)
  }

  @Tag(name = Tags.LICENCES)
  @GetMapping(value = ["/id/{licenceId}"])
  @PreAuthorize("hasAnyRole('SYSTEM_USER', 'CVL_ADMIN')")
  @ResponseBody
  @Operation(
    summary = "Get a licence by its licence id",
    description = "Returns a single licence detail by its unique identifier. " +
      "Requires ROLE_SYSTEM_USER or ROLE_CVL_ADMIN.",
    security = [SecurityRequirement(name = "ROLE_SYSTEM_USER"), SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Licence found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = Licence::class))],
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
        description = "The licence for this ID was not found.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getLicenceById(@PathVariable("licenceId") licenceId: Long): Licence {
    return licenceService.getLicenceById(licenceId)
  }

  @Tag(name = Tags.LICENCES)
  @PutMapping(value = ["/id/{licenceId}/appointmentPerson"])
  @PreAuthorize("hasAnyRole('SYSTEM_USER', 'CVL_ADMIN')")
  @Operation(
    summary = "Update the person to meet at the initial appointment",
    description = "Update the person the person on probation will meet at the initial appointment" +
      " Requires ROLE_SYSTEM_USER or ROLE_CVL_ADMIN.",
    security = [SecurityRequirement(name = "ROLE_SYSTEM_USER"), SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Appointment person updated",
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
      ApiResponse(
        responseCode = "404",
        description = "The licence for this ID was not found.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun updateAppointmentPerson(
    @PathVariable("licenceId") licenceId: Long,
    @Valid @RequestBody
    request: AppointmentPersonRequest,
  ) {
    licenceService.updateAppointmentPerson(licenceId, request)
  }

  @Tag(name = Tags.LICENCES)
  @PutMapping(value = ["/id/{licenceId}/appointmentTime"])
  @PreAuthorize("hasAnyRole('SYSTEM_USER', 'CVL_ADMIN')")
  @Operation(
    summary = "Update the appointment date and time",
    description = "Update the date and time for the initial appointment. Requires ROLE_SYSTEM_USER or ROLE_CVL_ADMIN.",
    security = [SecurityRequirement(name = "ROLE_SYSTEM_USER"), SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Appointment date and time updated",
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
      ApiResponse(
        responseCode = "404",
        description = "The licence for this ID was not found.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun updateAppointmentTime(
    @PathVariable("licenceId") licenceId: Long,
    @Valid @RequestBody
    request: AppointmentTimeRequest,
  ) {
    licenceService.updateAppointmentTime(licenceId, request)
  }

  @Tag(name = Tags.LICENCES)
  @PutMapping(value = ["/id/{licenceId}/contact-number"])
  @PreAuthorize("hasAnyRole('SYSTEM_USER', 'CVL_ADMIN')")
  @Operation(
    summary = "Update the officer contact number for a licence",
    description = "Update the contact number for the officer related to this licence. Requires ROLE_SYSTEM_USER or ROLE_CVL_ADMIN.",
    security = [SecurityRequirement(name = "ROLE_SYSTEM_USER"), SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Contact number updated",
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
      ApiResponse(
        responseCode = "404",
        description = "The licence for this ID was not found.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun updateContactNumber(
    @PathVariable("licenceId") licenceId: Long,
    @Valid @RequestBody
    request: ContactNumberRequest,
  ) {
    licenceService.updateContactNumber(licenceId, request)
  }

  @Tag(name = Tags.LICENCES)
  @PutMapping(value = ["/id/{licenceId}/appointment-address"])
  @PreAuthorize("hasAnyRole('SYSTEM_USER', 'CVL_ADMIN')")
  @Operation(
    summary = "Update the address where the initial appointment will take place",
    description = "Update the address where the initial appointment will take place. Requires ROLE_SYSTEM_USER or ROLE_CVL_ADMIN.",
    security = [SecurityRequirement(name = "ROLE_SYSTEM_USER"), SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Address updated",
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
      ApiResponse(
        responseCode = "404",
        description = "The licence for this ID was not found.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun updateAppointmentAddress(
    @PathVariable("licenceId") licenceId: Long,
    @Valid @RequestBody
    request: AppointmentAddressRequest,
  ) {
    licenceService.updateAppointmentAddress(licenceId, request)
  }

  @Tag(name = Tags.LICENCE_CONDITIONS)
  @PutMapping(value = ["/id/{licenceId}/bespoke-conditions"])
  @PreAuthorize("hasAnyRole('SYSTEM_USER', 'CVL_ADMIN')")
  @Operation(
    summary = "Add or replace the bespoke conditions for a licence.",
    description = "Add or replace the bespoke conditions on a licence with the content of this request. Requires ROLE_SYSTEM_USER or ROLE_CVL_ADMIN.",
    security = [SecurityRequirement(name = "ROLE_SYSTEM_USER"), SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Bespoke conditions added or replaced",
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
      ApiResponse(
        responseCode = "404",
        description = "The licence for this ID was not found.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun updateBespokeConditions(
    @PathVariable("licenceId") licenceId: Long,
    @Valid @RequestBody
    request: BespokeConditionRequest,
  ) {
    licenceConditionService.updateBespokeConditions(licenceId, request)
  }

  @Tag(name = Tags.LICENCE_VARIATIONS)
  @GetMapping(value = ["/variations/submitted/area/{areaCode}"])
  @PreAuthorize("hasAnyRole('CVL_ADMIN')")
  @Operation(
    summary = "Get a list of licence summaries for submitted variations by probation area.",
    description = "Get a list of licence summaries for all submitted variations belonging to the specified probation area code. Requires ROLE_CVL_ADMIN.",
    security = [SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Returned matching licence summary details - empty if no matches.",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = LicenceSummary::class)),
          ),
        ],
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
  fun submittedVariations(
    @PathVariable("areaCode") areaCode: String,
  ): List<LicenceSummary> {
    return licenceService.findSubmittedVariationsByRegion(probationAreaCode = areaCode)
  }

  @Tag(name = Tags.LICENCES)
  @PostMapping(value = ["/match"])
  @PreAuthorize("hasAnyRole('CVL_ADMIN')")
  @Operation(
    summary = "Get a list of licence summaries matching the supplied criteria.",
    description = "Get the licences matching the supplied lists of status, prison, staffId, nomsId and PDU. Requires ROLE_CVL_ADMIN.",
    security = [SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Returned matching licence summary details - empty if no matches.",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = LicenceSummary::class)),
          ),
        ],
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
  fun getLicencesMatchingCriteria(
    @Valid @RequestBody
    body: MatchLicencesRequest,
    @RequestParam(name = "sortBy", required = false) sortBy: String?,
    @RequestParam(name = "sortOrder", required = false) sortOrder: String?,
  ): List<LicenceSummary> {
    return licenceService.findLicencesMatchingCriteria(
      LicenceQueryObject(
        prisonCodes = body.prison,
        statusCodes = body.status,
        staffIds = body.staffId,
        nomsIds = body.nomsId,
        pdus = body.pdu,
        sortBy = sortBy,
        sortOrder = sortOrder,
      ),
    )
  }

  @Tag(name = Tags.LICENCES)
  @PostMapping(value = ["/recently-approved"])
  @PreAuthorize("hasAnyRole('CVL_ADMIN')")
  @Operation(
    summary = "Get a list of recently approved licence summaries matching the supplied list of prisons.",
    description = "Get the recently approved licences matching the supplied list of prisons. Requires ROLE_CVL_ADMIN.",
    security = [SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Returned matching licence summary details - empty if no matches.",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = LicenceSummary::class)),
          ),
        ],
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
  fun getRecentlyApprovedLicences(
    @RequestBody @Valid
    body: RecentlyApprovedLicencesRequest,
  ): List<LicenceSummary> {
    return licenceService.findRecentlyApprovedLicences(
      body.prisonCodes,
    )
  }

  @Tag(name = Tags.LICENCE_CONDITIONS)
  @PostMapping(value = ["/id/{licenceId}/additional-condition/{conditionType}"])
  @PreAuthorize("hasAnyRole('SYSTEM_USER', 'CVL_ADMIN')")
  @Operation(
    summary = "Add additional condition to the licence.",
    description = "Add additional condition to the licence. " +
      "This does not include accompanying data per condition. Existing conditions which appear on " +
      "the licence will be unaffected. More than one condition with the same code can be added " +
      "Requires ROLE_SYSTEM_USER or ROLE_CVL_ADMIN.",
    security = [SecurityRequirement(name = "ROLE_SYSTEM_USER"), SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Set of additional conditions added",
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
      ApiResponse(
        responseCode = "404",
        description = "The licence for this ID was not found.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun addAdditionalCondition(
    @PathVariable licenceId: Long,
    @PathVariable conditionType: String,
    @Valid @RequestBody
    request: AddAdditionalConditionRequest,
  ): AdditionalCondition {
    return this.licenceConditionService.addAdditionalCondition(licenceId, request)
  }

  @Tag(name = Tags.LICENCE_CONDITIONS)
  @DeleteMapping(value = ["/id/{licenceId}/additional-condition/id/{conditionId}"])
  @PreAuthorize("hasAnyRole('SYSTEM_USER', 'CVL_ADMIN')")
  @ResponseStatus(code = HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Remove additional condition with specified condition Id",
    description = "Remove additional condition from the licence list of additional conditions." +
      "All user submitted condition data will also be removed.",
    security = [SecurityRequirement(name = "ROLE_SYSTEM_USER"), SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "204",
        description = "Condition has been removed from the licence",
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
        description = "The licence for this ID was not found.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun deleteAdditionalCondition(
    @PathVariable("licenceId") licenceId: Long,
    @PathVariable("conditionId") conditionId: Long,
  ) {
    return this.licenceConditionService.deleteAdditionalCondition(licenceId, conditionId)
  }

  @Tag(name = Tags.LICENCE_CONDITIONS)
  @PutMapping(value = ["/id/{licenceId}/additional-conditions"])
  @PreAuthorize("hasAnyRole('SYSTEM_USER', 'CVL_ADMIN')")
  @Operation(
    summary = "Update the set of additional conditions on the licence.",
    description = "Update the set of additional conditions on the licence. " +
      "This does not include accompanying data per condition. Existing conditions which appear on " +
      "the licence but which are not supplied to this endpoint will be deleted. " +
      "Requires ROLE_SYSTEM_USER or ROLE_CVL_ADMIN.",
    security = [SecurityRequirement(name = "ROLE_SYSTEM_USER"), SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Set of additional conditions updated",
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
      ApiResponse(
        responseCode = "404",
        description = "The licence for this ID was not found.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun updateAdditionalConditions(
    @PathVariable("licenceId") licenceId: Long,
    @Valid @RequestBody
    request: AdditionalConditionsRequest,
  ) {
    return licenceConditionService.updateAdditionalConditions(licenceId, request)
  }

  /**
   * This functionality can be replaced once proper licence versioning is implemented
   */
  @Tag(name = Tags.LICENCE_CONDITIONS)
  @PutMapping("/id/{licenceId}/standard-conditions")
  @PreAuthorize("hasAnyRole('SYSTEM_USER', 'CVL_ADMIN')")
  @Operation(
    summary = "Update the standard conditions for a licence.",
    description = "Replace the standard conditions against a licence if policy changes. " +
      "Existing data for a condition which does not appear in this request will be deleted. " +
      "Requires ROLE_SYSTEM_USER or ROLE_CVL_ADMIN.",
    security = [SecurityRequirement(name = "ROLE_SYSTEM_USER"), SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Standard conditions updated",
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
      ApiResponse(
        responseCode = "404",
        description = "The licence for this ID was not found.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun updateStandardConditions(
    @PathVariable("licenceId") licenceId: Long,
    @Valid @RequestBody
    request: UpdateStandardConditionDataRequest,
  ) {
    return licenceConditionService.updateStandardConditions(licenceId, request)
  }

  @Tag(name = Tags.LICENCE_CONDITIONS)
  @PutMapping(value = ["/id/{licenceId}/additional-conditions/condition/{additionalConditionId}"])
  @PreAuthorize("hasAnyRole('SYSTEM_USER', 'CVL_ADMIN')")
  @Operation(
    summary = "Update the user entered data to accompany an additional condition template.",
    description = "Update the user entered data to accompany an additional condition template. " +
      "Existing data for a condition which does not appear in this request will be deleted. " +
      "Requires ROLE_SYSTEM_USER or ROLE_CVL_ADMIN.",
    security = [SecurityRequirement(name = "ROLE_SYSTEM_USER"), SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Additional condition updated",
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
      ApiResponse(
        responseCode = "404",
        description = "The licence for this ID was not found.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun updateAdditionalConditionData(
    @PathVariable("licenceId") licenceId: Long,
    @PathVariable("additionalConditionId") conditionId: Long,
    @Valid @RequestBody
    request: UpdateAdditionalConditionDataRequest,
  ) {
    return licenceConditionService.updateAdditionalConditionData(licenceId, conditionId, request)
  }

  @Tag(name = Tags.LICENCES)
  @PutMapping(value = ["/id/{licenceId}/status"])
  @PreAuthorize("hasAnyRole('SYSTEM_USER', 'CVL_ADMIN')")
  @Operation(
    summary = "Update the status of a licence.",
    description = "Update the status of a licence. Requires ROLE_SYSTEM_USER or ROLE_CVL_ADMIN.",
    security = [SecurityRequirement(name = "ROLE_SYSTEM_USER"), SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Licence status updated",
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
      ApiResponse(
        responseCode = "404",
        description = "The licence for this ID was not found.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun updateLicenceStatus(
    @PathVariable("licenceId") licenceId: Long,
    @Valid @RequestBody
    request: StatusUpdateRequest,
  ) {
    return licenceService.updateLicenceStatus(licenceId, request)
  }

  @Tag(name = Tags.LICENCE_VARIATIONS)
  @PutMapping(value = ["/id/{licenceId}/submit"])
  @PreAuthorize("hasAnyRole('SYSTEM_USER', 'CVL_ADMIN')")
  @Operation(
    summary = "Update the status of a licence to SUBMITTED or VARIATION_SUBMITTED.",
    description = "Update the status of a licence to SUBMITTED or VARIATION_SUBMITTED, and record the details of the COM who submitted. Requires ROLE_SYSTEM_USER or ROLE_CVL_ADMIN.",
    security = [SecurityRequirement(name = "ROLE_SYSTEM_USER"), SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Licence submitted for approval",
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
      ApiResponse(
        responseCode = "404",
        description = "The licence for this ID was not found.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun submitLicence(
    @PathVariable("licenceId") licenceId: Long,
    @Valid @RequestBody
    request: List<NotifyRequest>?,
  ) {
    return licenceService.submitLicence(licenceId, request)
  }

  @Tag(name = Tags.LICENCE_VARIATIONS)
  @PostMapping(value = ["/id/{licenceId}/create-variation"])
  @PreAuthorize("hasAnyRole('SYSTEM_USER', 'CVL_ADMIN')")
  @Operation(
    summary = "Create a variation of this licence",
    description = "Create a variation of this licence. The new licence will have a new ID and have a status VARIATION_IN_PROGRESS. Requires ROLE_SYSTEM_USER or ROLE_CVL_ADMIN.",
    security = [SecurityRequirement(name = "ROLE_SYSTEM_USER"), SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Licence variation created",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = LicenceSummary::class)),
        ],
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
      ApiResponse(
        responseCode = "404",
        description = "The licence for this ID was not found.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun createVariation(
    @PathVariable("licenceId") licenceId: Long,
  ): LicenceSummary {
    return licenceService.createVariation(licenceId)
  }

  @Tag(name = Tags.LICENCES)
  @PostMapping(value = ["/id/{licenceId}/edit"])
  @PreAuthorize("hasAnyRole('SYSTEM_USER', 'CVL_ADMIN')")
  @Operation(
    summary = "Create a new version of an approved licence",
    description = "Create a new version of an  approved licence. The new licence will have a new id and have status IN_PROGRESS. Requires ROLE_SYSTEM_USER or ROLE_CVL_ADMIN.",
    security = [SecurityRequirement(name = "ROLE_SYSTEM_USER"), SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Licence version created",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = LicenceSummary::class)),
        ],
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
        description = "The licence for this ID was not found.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun editLicence(
    @PathVariable("licenceId") licenceId: Long,
  ): LicenceSummary {
    return licenceService.editLicence(licenceId)
  }

  @Tag(name = Tags.LICENCE_VARIATIONS)
  @PutMapping(value = ["/id/{licenceId}/spo-discussion"])
  @PreAuthorize("hasAnyRole('SYSTEM_USER', 'CVL_ADMIN')")
  @Operation(
    summary = "Sets whether the variation has been discussed with an SPO.",
    description = "Sets whether the variation has been discussed with an SPO. Either Yes or No. Requires ROLE_SYSTEM_USER or ROLE_CVL_ADMIN.",
    security = [SecurityRequirement(name = "ROLE_SYSTEM_USER"), SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "SPO discussion updated",
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
      ApiResponse(
        responseCode = "404",
        description = "The licence for this ID was not found.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun updateSpoDiscussion(
    @PathVariable("licenceId") licenceId: Long,
    @Valid @RequestBody
    request: UpdateSpoDiscussionRequest,
  ) {
    licenceService.updateSpoDiscussion(licenceId, request)
  }

  @Tag(name = Tags.LICENCE_VARIATIONS)
  @PutMapping(value = ["/id/{licenceId}/vlo-discussion"])
  @PreAuthorize("hasAnyRole('SYSTEM_USER', 'CVL_ADMIN')")
  @Operation(
    summary = "Sets whether the variation has been discussed with a VLO.",
    description = "Sets whether the variation has been discussed with a VLO. Either Yes or Not applicable. Requires ROLE_SYSTEM_USER or ROLE_CVL_ADMIN.",
    security = [SecurityRequirement(name = "ROLE_SYSTEM_USER"), SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "VLO discussion updated",
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
      ApiResponse(
        responseCode = "404",
        description = "The licence for this ID was not found.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun updateVloDiscussion(
    @PathVariable("licenceId") licenceId: Long,
    @Valid @RequestBody
    request: UpdateVloDiscussionRequest,
  ) {
    licenceService.updateVloDiscussion(licenceId, request)
  }

  @Tag(name = Tags.LICENCE_VARIATIONS)
  @PutMapping(value = ["/id/{licenceId}/reason-for-variation"])
  @PreAuthorize("hasAnyRole('SYSTEM_USER', 'CVL_ADMIN')")
  @Operation(
    summary = "Updates the reason for the licence variation.",
    description = "Updates the reason for the licence variation. Requires ROLE_SYSTEM_USER or ROLE_CVL_ADMIN.",
    security = [SecurityRequirement(name = "ROLE_SYSTEM_USER"), SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Reason for variation updated",
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
      ApiResponse(
        responseCode = "404",
        description = "The licence for this ID was not found.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun updateReasonForVariation(
    @PathVariable("licenceId") licenceId: Long,
    @Valid @RequestBody
    request: UpdateReasonForVariationRequest,
  ) {
    licenceService.updateReasonForVariation(licenceId, request)
  }

  @Tag(name = Tags.LICENCE_VARIATIONS)
  @PutMapping(value = ["/id/{licenceId}/refer-variation"])
  @PreAuthorize("hasAnyRole('SYSTEM_USER', 'CVL_ADMIN')")
  @Operation(
    summary = "Updates a licence to referred and stores the reason provided.",
    description = "Updates a licence to referred and stores the reason provided by the approver. Requires ROLE_SYSTEM_USER or ROLE_CVL_ADMIN.",
    security = [SecurityRequirement(name = "ROLE_SYSTEM_USER"), SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Licence updated to referred and the referral reason stored",
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
      ApiResponse(
        responseCode = "404",
        description = "The licence for this ID was not found.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun referVariation(
    @PathVariable("licenceId") licenceId: Long,
    @Valid @RequestBody
    request: ReferVariationRequest,
  ) {
    licenceService.referLicenceVariation(licenceId, request)
  }

  @Tag(name = Tags.LICENCE_VARIATIONS)
  @PutMapping(value = ["/id/{licenceId}/approve-variation"])
  @PreAuthorize("hasAnyRole('SYSTEM_USER', 'CVL_ADMIN')")
  @Operation(
    summary = "Approves a licence variation.",
    description = "Approves a licence variation. Requires ROLE_SYSTEM_USER or ROLE_CVL_ADMIN.",
    security = [SecurityRequirement(name = "ROLE_SYSTEM_USER"), SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Variation approved",
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
      ApiResponse(
        responseCode = "404",
        description = "The licence for this ID was not found.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun approveVariation(
    @PathVariable("licenceId") licenceId: Long,
  ) {
    licenceService.approveLicenceVariation(licenceId)
  }

  @Tag(name = Tags.LICENCES)
  @DeleteMapping(value = ["/id/{licenceId}/discard"])
  @PreAuthorize("hasAnyRole('SYSTEM_USER', 'CVL_ADMIN')")
  @Operation(
    summary = "Discards a licence record.",
    description = "Discards a licence record. Requires ROLE_SYSTEM_USER or ROLE_CVL_ADMIN.",
    security = [SecurityRequirement(name = "ROLE_SYSTEM_USER"), SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Licence discarded",
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
      ApiResponse(
        responseCode = "404",
        description = "The licence for this ID was not found.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun discard(
    @PathVariable("licenceId") licenceId: Long,
  ) {
    licenceService.discardLicence(licenceId)
  }

  @Tag(name = Tags.LICENCES)
  @PutMapping(value = ["/id/{licenceId}/prison-information"])
  @PreAuthorize("hasAnyRole('SYSTEM_USER', 'CVL_ADMIN')")
  @Operation(
    summary = "Updates the prison information.",
    description = "Updates the prison information. Requires ROLE_SYSTEM_USER or ROLE_CVL_ADMIN.",
    security = [SecurityRequirement(name = "ROLE_SYSTEM_USER"), SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Prison information updated",
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
      ApiResponse(
        responseCode = "404",
        description = "The licence for this ID was not found.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun updatePrisonInformation(
    @PathVariable("licenceId") licenceId: Long,
    @Valid @RequestBody
    request: UpdatePrisonInformationRequest,
  ) {
    licenceService.updatePrisonInformation(licenceId, request)
  }

  @Tag(name = Tags.LICENCES)
  @PutMapping(value = ["/id/{licenceId}/sentence-dates"])
  @PreAuthorize("hasAnyRole('SYSTEM_USER', 'CVL_ADMIN')")
  @Operation(
    summary = "Updates the sentence dates.",
    description = "Updates the sentence dates. Requires ROLE_SYSTEM_USER or ROLE_CVL_ADMIN.",
    security = [SecurityRequirement(name = "ROLE_SYSTEM_USER"), SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Sentence dates updated",
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
      ApiResponse(
        responseCode = "404",
        description = "The licence for this ID was not found.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun updateSentenceDates(
    @PathVariable("licenceId") licenceId: Long,
    @Valid @RequestBody
    request: UpdateSentenceDatesRequest,
  ) {
    updateSentenceDateService.updateSentenceDates(licenceId, request)
  }


  @Tag(name = Tags.LICENCES)
  @GetMapping(value = ["/getCutOffDate"])
  @PreAuthorize("hasAnyRole('SYSTEM_USER', 'CVL_ADMIN')")
  @ResponseBody
  @Operation(
    summary = "Get cut-off date for licence time out",
    description = "Returns a cut-off date to filter hardstop licences. " +
      "Requires ROLE_SYSTEM_USER or ROLE_CVL_ADMIN.",
    security = [SecurityRequirement(name = "ROLE_SYSTEM_USER"), SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Licence found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = Licence::class))],
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
        description = "The licence for this ID was not found.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getCutOffDate(): HardStopCutoffDate {
    return licenceService.getCutOffDate(LocalDate.now())
  }
}
