package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.privateApi

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response.AddressSearchResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.Tags
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.addressSearch.AddressSearchService

private const val MIN_PAGE_RESULTS = 2L
private const val MAX_PAGE_RESULTS = 100L

@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = Tags.ANCILLARY)
class AddressSearchResource(private val addressSearchService: AddressSearchService) {

  @GetMapping("/address/search/by/text/{searchQuery}")
  @PreAuthorize("hasAnyRole('CVL_ADMIN')")
  @Operation(
    summary = "Searches for addresses that match the given search text",
    description = "Searches for addresses that match the given search text",
    security = [SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Returns addresses matching the given search text",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = AddressSearchResponse::class),
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
  fun searchForAddresses(
    @Size(min = 1, max = 100, message = "Search query must be more than 3 and no more than 100")
    @PathVariable(name = "searchQuery")
    searchQuery: String,
    @RequestParam(value = "page", required = false)
    @Parameter(
      description = "Pagination page number, starting at zero",
      example = "0",
    )
    @PositiveOrZero
    page: Int = 0,
    @RequestParam(value = "pageSize", required = false)
    @Parameter(
      description = "Pagination size per page, min/max = 2/200",
      example = "50",
    )
    @Min(MIN_PAGE_RESULTS)
    @Max(MAX_PAGE_RESULTS)
    pageSize: Int = 50,
  ): List<AddressSearchResponse> = addressSearchService.searchForAddressesByText(searchQuery, page, pageSize)

  @GetMapping("/address/search/by/postcode/{postcode}")
  @PreAuthorize("hasAnyRole('CVL_ADMIN')")
  @Operation(
    summary = "Searches for addresses that match the given postcode",
    description = "Searches for addresses that match the given postcode",
    security = [SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Returns addresses matching the given search text",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = AddressSearchResponse::class),
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
  fun searchForAddressesByPostcode(
    @Size(min = 5, max = 10, message = "Postcode must be more than 4 and no more than 10 in length")
    @PathVariable(name = "postcode")
    postcode: String,
    @RequestParam(value = "page", required = false)
    @Parameter(
      description = "Pagination page number, starting at zero",
      example = "0",
    )
    @PositiveOrZero
    page: Int = 0,
    @RequestParam(value = "pageSize", required = false)
    @Parameter(
      description = "Pagination size per page, min/max = 2/200",
      example = "50",
    )
    @Min(MIN_PAGE_RESULTS)
    @Max(MAX_PAGE_RESULTS)
    pageSize: Int = 50,
  ): List<AddressSearchResponse> = addressSearchService.searchForAddressesByPostcode(postcode, page, pageSize)

  @GetMapping("/address/search/by/reference/{reference}")
  @PreAuthorize("hasAnyRole('CVL_ADMIN')")
  @Operation(
    summary = "Gets an address by it's reference",
    description = "Gets an address by it's reference (Unique Property Reference Number)",
    security = [SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Returns the address with the provided reference",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = AddressSearchResponse::class),
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
  fun searchForAddressByReference(@PathVariable(name = "reference") reference: String) = addressSearchService.searchForAddressByReference(reference)
}
