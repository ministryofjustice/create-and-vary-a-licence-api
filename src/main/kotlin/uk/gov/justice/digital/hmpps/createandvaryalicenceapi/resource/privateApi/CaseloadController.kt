package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.privateApi

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.PrisonerNumbers
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.Tags
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CaseloadService

@Tag(
  name = Tags.CASELOAD,
  description = "Returns information used for building caseloads",
)
@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class CaseloadController(val caseloadService: CaseloadService) {

  @PostMapping("/prisoner-search/prisoner-numbers")
  @PreAuthorize("hasAnyRole('SYSTEM_USER', 'CVL_ADMIN')")
  @Operation(
    summary = "Returns enriched prisoners by prison number",
    description = "Match prisoners by a list of prisoner numbers",
    security = [SecurityRequirement(name = "ROLE_SYSTEM_USER"), SecurityRequirement(name = "ROLE_CVL_ADMIN")],

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
  fun findByNumber(@Parameter(required = true) @PathVariable nomsId: String) =
    caseloadService.getPrisoner(nomsId)
}
