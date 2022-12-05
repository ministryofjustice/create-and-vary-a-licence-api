package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.OverrideLicenceStatusRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceOverrideService
import javax.validation.Valid

@RestController
@RequestMapping("/licence/id/{licenceId}/override", produces = [MediaType.APPLICATION_JSON_VALUE])
class LicenceOverrideController(private val licenceOverrideService: LicenceOverrideService) {
  @PostMapping(value = ["/status"])
  @PreAuthorize("hasAnyRole('SYSTEM_USER', 'CVL_ADMIN')")
  @ResponseBody
  @ResponseStatus(HttpStatus.ACCEPTED)
  @Operation(
    summary = "Override a licence status",
    description = "Override the status for an exising licence. Only to be used in exceptional circumstances." +
      " Requires ROLE_SYSTEM_USER or ROLE_CVL_ADMIN.",
    security = [SecurityRequirement(name = "ROLE_SYSTEM_USER"), SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  fun changeStatus(
    @PathVariable("licenceId") licenceId: Long,
    @RequestBody @Valid request: OverrideLicenceStatusRequest
  ) {
    val licence = licenceOverrideService.getLicenceById(licenceId)
    licenceOverrideService.changeStatus(licence!!, request.statusCode, request.reason)
  }
}
