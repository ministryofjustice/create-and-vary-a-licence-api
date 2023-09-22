package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.BankHolidayService
import java.time.LocalDate

@RestController
@RequestMapping("/", produces = [MediaType.APPLICATION_JSON_VALUE])
class BankHolidayController(private val bankHolidayService: BankHolidayService) {

  @Tag(name = Tags.ANCILLARY)
  @GetMapping(value = ["/bank-holidays"])
  @PreAuthorize("hasAnyRole('SYSTEM_USER', 'CVL_ADMIN')")
  @ResponseBody
  @Operation(
    summary = "Get the bank holiday dates for England and Wales",
    description = "Returns a list of bank holiday dates for England and Wales. " +
      "Requires ROLE_SYSTEM_USER or ROLE_CVL_ADMIN.",
    security = [SecurityRequirement(name = "ROLE_SYSTEM_USER"), SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Bank Holidays retrieved",
        content = [Content(mediaType = "application/json", array = ArraySchema(schema = Schema(implementation = LocalDate::class)))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an appropriate role",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "No bank holidays were not found.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getBankHolidaysForEnglandAndWales() = this.bankHolidayService.getBankHolidaysForEnglandAndWales()
}
