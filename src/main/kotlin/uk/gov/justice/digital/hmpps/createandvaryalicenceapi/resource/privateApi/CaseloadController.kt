package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.privateApi

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
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
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ApprovalCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CaCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CaseloadItem
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ComCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.PrisonCaseAdminSearchResult
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.PrisonerNumbers
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.PrisonerWithCvlFields
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.TeamCaseloadRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.VaryApproverCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.ApproverSearchRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.PrisonUserSearchRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response.ApproverSearchResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.Tags
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CaseloadService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ApproverCaseloadService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.CaCaseloadService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ComCreateCaseloadService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ComVaryCaseloadService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.VaryApproverCaseloadService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.model.request.CaCaseloadSearch
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.request.VaryApproverCaseloadSearchRequest

@Tag(
  name = Tags.CASELOAD,
  description = "Returns information used for building caseloads",
)
@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class CaseloadController(
  val caseloadService: CaseloadService,
  val approverCaseloadService: ApproverCaseloadService,
  val caCaseloadService: CaCaseloadService,
  val comCreateCaseloadService: ComCreateCaseloadService,
  val comVaryCaseloadService: ComVaryCaseloadService,
  val varyApproverCaseloadService: VaryApproverCaseloadService,
) {

  @PostMapping("/prisoner-search/prisoner-numbers")
  @PreAuthorize("hasAnyRole('CVL_ADMIN')")
  @Operation(
    summary = "Returns enriched prisoners by prison number",
    description = "Match prisoners by a list of prisoner numbers",
    security = [SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Returning A list of prisoners",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = CaseloadItem::class)),
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
  fun findByNumbers(@Parameter(required = true) @Valid @RequestBody criteria: PrisonerNumbers) = caseloadService.getPrisonersByNumber(criteria.prisonerNumbers)

  @GetMapping("/prisoner-search/nomisid/{nomsId}")
  @PreAuthorize("hasAnyRole('CVL_ADMIN')")
  @Operation(
    summary = "Returns a single prisoner by prison number",
    description = "Returns a single prisoner by prison number",
    security = [SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Returning A list of prisoners",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = PrisonerWithCvlFields::class)),
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
        description = "Could not find prisoner with prison number",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun findByNumber(@Parameter(required = true) @PathVariable nomsId: String) = caseloadService.getPrisoner(nomsId)

  @PostMapping("/caseload/prison-approver/approval-needed")
  @PreAuthorize("hasAnyRole('CVL_ADMIN')")
  @Operation(
    summary = "Returns a caseload waiting for approval",
    description = "Returns an enriched list of cases which are awaiting approval",
    security = [SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Returns a list of cases awaiting approval",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = ApprovalCase::class)),
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
  fun getApprovalNeeded(@Parameter(required = true) @Valid @RequestBody prisonCodes: List<String>) = approverCaseloadService.getApprovalNeeded(prisonCodes)

  @PostMapping("/caseload/prison-approver/recently-approved")
  @PreAuthorize("hasAnyRole('CVL_ADMIN')")
  @Operation(
    summary = "Returns a caseload that has recently been approved",
    description = "Returns an enriched list of cases which have recently been approved",
    security = [SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Returns a list of cases that have recently been approved",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = ApprovalCase::class)),
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
  fun getRecentlyApproved(@Parameter(required = true) @Valid @RequestBody prisonCodes: List<String>) = approverCaseloadService.getRecentlyApproved(prisonCodes)

  @PostMapping("/caseload/prison-approver/case-search")
  @PreAuthorize("hasAnyRole('SYSTEM_USER', 'CVL_ADMIN')")
  @Operation(
    summary = "Search for offenders on a given approver's caseload",
    description = "Search for offenders on a given approver's caseload. Requires ROLE_SYSTEM_USER or ROLE_CVL_ADMIN.",
    security = [SecurityRequirement(name = "ROLE_SYSTEM_USER"), SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "The query retrieved a set of enriched results",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ApproverSearchResponse::class),
          ),
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
  fun searchForOffenderOnApproverCaseload(
    @Valid @RequestBody
    body: ApproverSearchRequest,
  ) = approverCaseloadService.searchForOffenderOnApproverCaseload(body)

  @PostMapping("/caseload/case-admin/prison-view")
  @PreAuthorize("hasAnyRole('CVL_ADMIN')")
  @Operation(
    summary = "Returns a case admin caseload",
    description = "Returns an enriched list of cases for people on prison",
    security = [SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Returns a list of cases for people on prison",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = CaCase::class)),
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
  fun getPrisonView(
    @Parameter(required = true) @Valid @RequestBody caCaseloadSearch: CaCaseloadSearch,
  ) = caCaseloadService.getPrisonOmuCaseload(
    caCaseloadSearch.prisonCodes,
    caCaseloadSearch.searchString,
  )

  @PostMapping("/caseload/case-admin/probation-view")
  @PreAuthorize("hasAnyRole('CVL_ADMIN')")
  @Operation(
    summary = "Returns a case admin caseload",
    description = "Returns an enriched list of cases for people on probation",
    security = [SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Returns a list of cases for people on probation",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = CaCase::class)),
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
  fun getProbationView(
    @Parameter(required = true) @Valid @RequestBody caCaseloadSearch: CaCaseloadSearch,
  ) = caCaseloadService.getProbationOmuCaseload(caCaseloadSearch.prisonCodes, caCaseloadSearch.searchString)

  @PostMapping("/caseload/case-admin/case-search")
  @PreAuthorize("hasAnyRole('SYSTEM_USER', 'CVL_ADMIN')")
  @Operation(
    summary = "Search for offenders on a given case admin's caseload.",
    description = "Search for offenders on a given case admin's caseload. Requires ROLE_SYSTEM_USER or ROLE_CVL_ADMIN.",
    security = [SecurityRequirement(name = "ROLE_SYSTEM_USER"), SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "The query retrieved a set of enriched results",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = PrisonCaseAdminSearchResult::class),
          ),
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
  fun searchForOffenderOnPrisonCaseAdminCaseload(
    @Valid @RequestBody
    body: PrisonUserSearchRequest,
  ) = caCaseloadService.searchForOffenderOnPrisonCaseAdminCaseload(body)

  @GetMapping("/caseload/com/staff/{deliusStaffIdentifier}/create-case-load")
  @PreAuthorize("hasAnyRole('CVL_ADMIN')")
  @Operation(
    summary = "Returns the create caseload for a member of staff",
    description = "Returns an enriched list of cases which require a licence to be created",
    security = [SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Returns a list of cases that require a licence to be created",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = ComCase::class)),
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
  fun getStaffCreateCaseload(@Parameter(required = true) @PathVariable deliusStaffIdentifier: Long) = comCreateCaseloadService.getStaffCreateCaseload(deliusStaffIdentifier)

  @PostMapping("/caseload/com/team/create-case-load")
  @PreAuthorize("hasAnyRole('CVL_ADMIN')")
  @Operation(
    summary = "Returns the create caseload for a team",
    description = "Returns an enriched list of cases which require a licence to be created for a team",
    security = [SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Returns a list of cases that require a licence to be created for a team",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = ComCase::class)),
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
  fun getTeamCreateCaseload(@Parameter(required = true) @Valid @RequestBody request: TeamCaseloadRequest): List<ComCase> = comCreateCaseloadService.getTeamCreateCaseload(request.probationTeamCodes, request.teamSelected)

  @GetMapping("/caseload/com/staff/{deliusStaffIdentifier}/vary-case-load")
  @PreAuthorize("hasAnyRole('CVL_ADMIN')")
  @Operation(
    summary = "Returns the vary caseload for a member of staff",
    description = "Returns an enriched list of cases which can have a variation created for a member of staff",
    security = [SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Returns a list of cases that can have a variation created",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = ComCase::class)),
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
  fun getStaffVaryCaseload(@Parameter(required = true) @PathVariable deliusStaffIdentifier: Long) = comVaryCaseloadService.getStaffVaryCaseload(deliusStaffIdentifier)

  @PostMapping("/caseload/com/team/vary-case-load")
  @PreAuthorize("hasAnyRole('CVL_ADMIN')")
  @Operation(
    summary = "Returns the vary caseload for a team",
    description = "Returns an enriched list of cases which can have a variation created for a team",
    security = [SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Returns a list of cases that can have a variation created for a team",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = ComCase::class)),
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
  fun getTeamVaryCaseload(@Parameter(required = true) @Valid @RequestBody request: TeamCaseloadRequest): List<ComCase> = comVaryCaseloadService.getTeamVaryCaseload(request.probationTeamCodes, request.teamSelected)

  @PostMapping("/caseload/vary-approver")
  @PreAuthorize("hasAnyRole('CVL_ADMIN')")
  @Operation(
    summary = "Returns the variation approver caseload for a probation region",
    description = "Returns an enriched list of cases for people that have a variation to be approved",
    security = [SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Returns a list of cases with variations awaiting approval",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = VaryApproverCase::class)),
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
  fun getVaryApproverCaseload(
    @Parameter(required = true) @Valid @RequestBody varyApproverCaseloadSearchRequest: VaryApproverCaseloadSearchRequest,
  ): List<VaryApproverCase> = varyApproverCaseloadService.getVaryApproverCaseload(varyApproverCaseloadSearchRequest)
}
