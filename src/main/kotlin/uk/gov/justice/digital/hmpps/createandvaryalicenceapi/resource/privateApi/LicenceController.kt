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
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceCreationResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceSummaryApproverView
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.StatusUpdateRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.ApproveLicencesRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.CreateLicenceRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.DeactivateLicenceAndVariationsRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.LicenceType.CRD
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.LicenceType.HARD_STOP
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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceCreationService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.PrisonApproverService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.UpdateSentenceDateService

@RestController
@RequestMapping("/licence", produces = [MediaType.APPLICATION_JSON_VALUE])
class LicenceController(
  private val licenceService: LicenceService,
  private val prisonApproverService: PrisonApproverService,
  private val updateSentenceDateService: UpdateSentenceDateService,
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
          Content(mediaType = "application/json", schema = Schema(implementation = LicenceCreationResponse::class)),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised, requires a valid Oauth2 token",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an appropriate role",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun createLicence(
    @RequestBody @Valid
    request: CreateLicenceRequest,
  ): LicenceCreationResponse = when (request.type) {
    CRD -> licenceCreationService.createLicence(request.nomsId)
    HARD_STOP -> licenceCreationService.createHardStopLicence(request.nomsId)
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
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an appropriate role",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "The licence for this ID was not found.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getLicenceById(@PathVariable("licenceId") licenceId: Long): Licence {
    return licenceService.getLicenceById(licenceId)
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
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an appropriate role",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
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
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an appropriate role",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
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
            array = ArraySchema(schema = Schema(implementation = LicenceSummaryApproverView::class)),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised, requires a valid Oauth2 token",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an appropriate role",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getRecentlyApprovedLicences(
    @RequestBody @Valid
    body: RecentlyApprovedLicencesRequest,
  ): List<LicenceSummaryApproverView> {
    return prisonApproverService.findRecentlyApprovedLicences(
      body.prisonCodes,
    )
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
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised, requires a valid Oauth2 token",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an appropriate role",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "The licence for this ID was not found.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
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
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised, requires a valid Oauth2 token",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an appropriate role",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "The licence for this ID was not found.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
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
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised, requires a valid Oauth2 token",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an appropriate role",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "The licence for this ID was not found.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
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
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an appropriate role",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "The licence for this ID was not found.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
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
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised, requires a valid Oauth2 token",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an appropriate role",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "The licence for this ID was not found.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
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
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised, requires a valid Oauth2 token",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an appropriate role",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "The licence for this ID was not found.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
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
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised, requires a valid Oauth2 token",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an appropriate role",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "The licence for this ID was not found.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
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
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised, requires a valid Oauth2 token",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an appropriate role",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "The licence for this ID was not found.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
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
  @PutMapping(value = ["/id/{licenceId}/activate-variation"])
  @PreAuthorize("hasAnyRole('SYSTEM_USER', 'CVL_ADMIN')")
  @Operation(
    summary = "Activates an approved variation. Inactivates previous licence.",
    description = "Activates a variation. Inactivates previous licence. Requires ROLE_SYSTEM_USER or ROLE_CVL_ADMIN.",
    security = [SecurityRequirement(name = "ROLE_SYSTEM_USER"), SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Licence variation activated",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad request, request body must be valid",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised, requires a valid Oauth2 token",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an appropriate role",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "The licence for this ID was not found.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun activateVariation(
    @PathVariable("licenceId") licenceId: Long,
  ) {
    licenceService.activateVariation(licenceId)
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
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised, requires a valid Oauth2 token",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an appropriate role",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "The licence for this ID was not found.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
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
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised, requires a valid Oauth2 token",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an appropriate role",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "The licence for this ID was not found.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
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
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised, requires a valid Oauth2 token",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an appropriate role",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "The licence for this ID was not found.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
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
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised, requires a valid Oauth2 token",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an appropriate role",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "The licence for this ID was not found.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
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
  @PostMapping(value = ["/id/{licenceId}/review-with-no-variation-required"])
  @PreAuthorize("hasAnyRole('SYSTEM_USER', 'CVL_ADMIN')")
  @Operation(
    summary = "Marks the HardStop / Time served licence as having been reviewed with no variation required.",
    description = "Marks the HardStop / Time served licence as having been reviewed with no variation required. Requires ROLE_SYSTEM_USER or ROLE_CVL_ADMIN.",
    security = [SecurityRequirement(name = "ROLE_SYSTEM_USER"), SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Licence successfully marked as reviewed",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad request",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised, requires a valid Oauth2 token",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an appropriate role",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "The licence for this ID was not found.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun reviewWithNoVariationRequired(
    @PathVariable("licenceId") licenceId: Long,
  ) {
    licenceService.reviewWithNoVariationRequired(licenceId)
  }

  @Tag(name = Tags.LICENCES)
  @PostMapping(value = ["/licences-for-approval"])
  @PreAuthorize("hasAnyRole('SYSTEM_USER', 'CVL_ADMIN')")
  @Operation(
    summary = "Get a list of licence summaries ready for approval",
    description = "Get a list of licence summaries ready for approval. Requires ROLE_SYSTEM_USER or ROLE_CVL_ADMIN.",
    security = [SecurityRequirement(name = "ROLE_SYSTEM_USER"), SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Returned list of licence summary details - empty if no matches.",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = LicenceSummaryApproverView::class)),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad request, request body must be valid",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised, requires a valid Oauth2 token",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an appropriate role",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getLicencesForApproval(
    @Valid @RequestBody
    body: ApproveLicencesRequest,
  ): List<LicenceSummaryApproverView> {
    return prisonApproverService.getLicencesForApproval(body.prisonCodes)
  }

  @Tag(name = Tags.LICENCES)
  @PostMapping(value = ["/id/{licenceId}/deactivate-licence-and-variations"])
  @PreAuthorize("hasAnyRole('SYSTEM_USER', 'CVL_ADMIN')")
  @Operation(
    summary = "Deactivate an active licence and any associated variations",
    description = "Deactivate the supplied ACTIVE licence, and any variations of that licence. Requires ROLE_SYSTEM_USER or ROLE_CVL_ADMIN.",
    security = [SecurityRequirement(name = "ROLE_SYSTEM_USER"), SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Licence(s) deactivated",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = LicenceSummary::class)),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised, requires a valid Oauth2 token",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an appropriate role",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "The licence for this ID was not found.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun deactivateLicenceAndVariations(
    @PathVariable("licenceId") licenceId: Long,
    @Valid @RequestBody
    body: DeactivateLicenceAndVariationsRequest,
  ) {
    return licenceService.deactivateLicenceAndVariations(licenceId, body)
  }
}
