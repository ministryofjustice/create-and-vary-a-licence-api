package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.ExclusionZoneService
import java.io.IOException
import javax.validation.ValidationException

@RestController
@RequestMapping("/exclusion-zone", produces = [MediaType.APPLICATION_JSON_VALUE])
class ExclusionZoneController(private val exclusionZoneService: ExclusionZoneService) {

  @PostMapping(
    value = ["/id/{licenceId}/condition/id/{conditionId}/file-upload"],
    consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
    produces = [MediaType.APPLICATION_JSON_VALUE]
  )
  @PreAuthorize("hasAnyRole('SYSTEM_USER', 'CVL_ADMIN')")
  @Operation(
    summary = "Upload a multipart/form-data request containing a PDF exclusion zone file.",
    description = "Uploads a PDF file containing an exclusion zone map and description. Requires ROLE_SYSTEM_USER or ROLE_CVL_ADMIN.",
    security = [SecurityRequirement(name = "ROLE_SYSTEM_USER"), SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "The exclusion zone file was uploaded",
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
      )
    ]
  )
  fun uploadExclusionZoneFile(
    @PathVariable(value = "licenceId") licenceId: Long,
    @PathVariable(value = "conditionId") conditionId: Long,
    @RequestPart("file") file: MultipartFile
  ) {
    try {
      exclusionZoneService.uploadExclusionZoneFile(licenceId, conditionId, file)
    } catch (e: IOException) {
      throw ValidationException("Exclusion zone file could not be processed")
    }
  }

  @PutMapping(
    value = ["/id/{licenceId}/condition/id/{conditionId}/remove-upload"],
    produces = [MediaType.APPLICATION_JSON_VALUE]
  )
  @PreAuthorize("hasAnyRole('SYSTEM_USER', 'CVL_ADMIN')")
  @Operation(
    summary = "Removes a previously uploaded exclusion zone file from an additional condition.",
    description = "Removes a previously uploaded exclusion zone file. Requires ROLE_SYSTEM_USER or ROLE_CVL_ADMIN.",
    security = [SecurityRequirement(name = "ROLE_SYSTEM_USER"), SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "The exclusion zone file was removed",
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
      )
    ]
  )
  fun removeExclusionZoneFile(
    @PathVariable(value = "licenceId") licenceId: Long,
    @PathVariable(value = "conditionId") conditionId: Long,
  ) {
    return exclusionZoneService.removeExclusionZoneFile(licenceId, conditionId)
  }

  @GetMapping(
    value = ["/id/{licenceId}/condition/id/{conditionId}/full-size-image"],
    produces = [MediaType.IMAGE_JPEG_VALUE]
  )
  @PreAuthorize("hasAnyRole('SYSTEM_USER', 'CVL_ADMIN')")
  @ResponseBody
  @Operation(
    summary = "Get the exclusion zone map image for a specified licence and condition",
    description = "Get the exclusion zone map image. Requires ROLE_SYSTEM_USER or ROLE_CVL_ADMIN.",
    security = [SecurityRequirement(name = "ROLE_SYSTEM_USER"), SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Image returned",
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
        description = "No image was found.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      )
    ]
  )
  fun getExclusionZoneImage(
    @PathVariable(name = "licenceId") licenceId: Long,
    @PathVariable(name = "conditionId") conditionId: Long,
  ): ByteArray? {
    return exclusionZoneService.getExclusionZoneImage(licenceId, conditionId)
  }
}
