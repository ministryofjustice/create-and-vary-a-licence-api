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
import org.springframework.data.domain.Page
import org.springframework.data.web.PagedModel
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ApprovalCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CaseloadItem
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ComReviewCount
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ManagedCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.PrisonerNumbers
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ReleaseDateSearch
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.TeamCaseloadRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.Tags
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CaseloadService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ApproverCaseloadService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ComCaseloadService

@Tag(
  name = Tags.CASELOAD,
  description = "Returns information used for building caseloads",
)
@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class CaseloadController(
  val caseloadService: CaseloadService,
  val approverCaseloadService: ApproverCaseloadService,
  val comCaseloadService: ComCaseloadService,
) {

  @PostMapping("/prisoner-search/prisoner-numbers")
  @PreAuthorize("hasAnyRole('SYSTEM_USER', 'CVL_ADMIN')")
  @Operation(
    summary = "Returns enriched prisoners by prison number",
    description = "Match prisoners by a list of prisoner numbers",
    security = [SecurityRequirement(name = "ROLE_SYSTEM_USER"), SecurityRequirement(name = "ROLE_CVL_ADMIN")],
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
  fun findByNumbers(@Parameter(required = true) @Valid @RequestBody criteria: PrisonerNumbers) =
    caseloadService.getPrisonersByNumber(criteria.prisonerNumbers)

  @GetMapping("/prisoner-search/nomisid/{nomsId}")
  @PreAuthorize("hasAnyRole('SYSTEM_USER', 'CVL_ADMIN')")
  @Operation(
    summary = "Returns a single prisoner by prison number",
    description = "Returns a single prisoner by prison number",
    security = [SecurityRequirement(name = "ROLE_SYSTEM_USER"), SecurityRequirement(name = "ROLE_CVL_ADMIN")],
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

  @PostMapping("/prisoner-search/release-date-by-prison")
  @PreAuthorize("hasAnyRole('SYSTEM_USER', 'CVL_ADMIN')")
  @Operation(
    summary = "Returns prisoners by release date and prison id",
    description = "Match prisoners in a subset of prisons with a release date within a given range",
    security = [SecurityRequirement(name = "ROLE_SYSTEM_USER"), SecurityRequirement(name = "ROLE_CVL_ADMIN")],
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
  @Deprecated("use paginated version")
  fun findByReleaseDate(
    @Parameter(required = true) @Valid @RequestBody criteria: ReleaseDateSearch,
  ) = caseloadService.getPrisonersByReleaseDate(
    criteria.earliestReleaseDate!!,
    criteria.latestReleaseDate!!,
    criteria.prisonIds!!,
    page = 0,
  ).content

  @PostMapping("/release-date-by-prison")
  @PreAuthorize("hasAnyRole('SYSTEM_USER', 'CVL_ADMIN')")
  @Operation(
    summary = "Returns prisoners by release date and prison id",
    description = "Match prisoners in a subset of prisons with a release date within a given range",
    security = [SecurityRequirement(name = "ROLE_SYSTEM_USER"), SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Returning A page of search results",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = SearchResultsPage::class),
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
  fun findByReleaseDatePaginated(
    @Parameter(required = true) @Valid @RequestBody criteria: ReleaseDateSearch,
    @Parameter(
      description = "page of results to return (0 indexed), defaults to first page",
      required = false,
    ) @Valid @RequestParam(value = "page") page: Int = 0,
  ) = caseloadService.getPrisonersByReleaseDate(
    criteria.earliestReleaseDate!!,
    criteria.latestReleaseDate!!,
    criteria.prisonIds!!,
    page,
  )

  @PostMapping("/caseload/prison-approver/approval-needed")
  @PreAuthorize("hasAnyRole('SYSTEM_USER', 'CVL_ADMIN')")
  @Operation(
    summary = "Returns a caseload waiting for approval",
    description = "Returns an enriched list of cases which are awaiting approval",
    security = [SecurityRequirement(name = "ROLE_SYSTEM_USER"), SecurityRequirement(name = "ROLE_CVL_ADMIN")],
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
  fun getApprovalNeeded(@Parameter(required = true) @Valid @RequestBody prisonCodes: List<String>) =
    approverCaseloadService.getApprovalNeeded(prisonCodes)

  @PostMapping("/caseload/prison-approver/recently-approved")
  @PreAuthorize("hasAnyRole('SYSTEM_USER', 'CVL_ADMIN')")
  @Operation(
    summary = "Returns a caseload that has recently been approved",
    description = "Returns an enriched list of cases which have recently been approved",
    security = [SecurityRequirement(name = "ROLE_SYSTEM_USER"), SecurityRequirement(name = "ROLE_CVL_ADMIN")],
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
  fun getRecentlyApproved(@Parameter(required = true) @Valid @RequestBody prisonCodes: List<String>) =
    approverCaseloadService.getRecentlyApproved(prisonCodes)

  // TODO : update docs
  @GetMapping("/caseload/com/staff/{deliusStaffIdentifier}/create-case-load")
  @PreAuthorize("hasAnyRole('SYSTEM_USER', 'CVL_ADMIN')")
  @Operation(
    summary = "Returns a caseload that has recently been approved",
    description = "Returns an enriched list of cases which have recently been approved",
    security = [SecurityRequirement(name = "ROLE_SYSTEM_USER"), SecurityRequirement(name = "ROLE_CVL_ADMIN")],
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
  fun getStaffCreateCaseload(@Parameter(required = true) @PathVariable deliusStaffIdentifier: Long) =
    comCaseloadService.getStaffCreateCaseload(deliusStaffIdentifier)

  @PostMapping("/caseload/com/team/create-case-load")
  @PreAuthorize("hasAnyRole('SYSTEM_USER', 'CVL_ADMIN')")
  @Operation(
    summary = "Returns a caseload that has recently been approved",
    description = "Returns an enriched list of cases which have recently been approved",
    security = [SecurityRequirement(name = "ROLE_SYSTEM_USER"), SecurityRequirement(name = "ROLE_CVL_ADMIN")],
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
  fun getTeamCreateCaseload(@Parameter(required = true) @Valid @RequestBody request: TeamCaseloadRequest): List<ManagedCase> =
    comCaseloadService.getTeamCreateCaseload(request.probationTeamCodes, request.teamSelected)

  @GetMapping("/caseload/com/staff/{deliusStaffIdentifier}/vary-case-load")
  @PreAuthorize("hasAnyRole('SYSTEM_USER', 'CVL_ADMIN')")
  @Operation(
    summary = "Returns a caseload that has recently been approved",
    description = "Returns an enriched list of cases which have recently been approved",
    security = [SecurityRequirement(name = "ROLE_SYSTEM_USER"), SecurityRequirement(name = "ROLE_CVL_ADMIN")],
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
  fun getStaffVaryCaseload(@Parameter(required = true) @PathVariable deliusStaffIdentifier: Long) =
    comCaseloadService.getStaffVaryCaseload(deliusStaffIdentifier)

  @PostMapping("/caseload/com/team/vary-case-load")
  @PreAuthorize("hasAnyRole('SYSTEM_USER', 'CVL_ADMIN')")
  @Operation(
    summary = "Returns a caseload that has recently been approved",
    description = "Returns an enriched list of cases which have recently been approved",
    security = [SecurityRequirement(name = "ROLE_SYSTEM_USER"), SecurityRequirement(name = "ROLE_CVL_ADMIN")],
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
  fun getTeamVaryCaseload(@Parameter(required = true) @Valid @RequestBody request: TeamCaseloadRequest): List<ManagedCase> =
    comCaseloadService.getTeamVaryCaseload(request.probationTeamCodes, request.teamSelected)

  @GetMapping("/caseload/com/staff/{deliusStaffIdentifier}/com-review-count")
  @PreAuthorize("hasAnyRole('SYSTEM_USER', 'CVL_ADMIN')")
  @Operation(
    summary = "Returns a caseload that has recently been approved",
    description = "Returns an enriched list of cases which have recently been approved",
    security = [SecurityRequirement(name = "ROLE_SYSTEM_USER"), SecurityRequirement(name = "ROLE_CVL_ADMIN")],
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
  fun getComReviewCount(@Parameter(required = true) @PathVariable deliusStaffIdentifier: Long): ComReviewCount =
    comCaseloadService.getComReviewCount(deliusStaffIdentifier)
}

class SearchResultsPage : PagedModel<CaseloadItem>(Page.empty())
