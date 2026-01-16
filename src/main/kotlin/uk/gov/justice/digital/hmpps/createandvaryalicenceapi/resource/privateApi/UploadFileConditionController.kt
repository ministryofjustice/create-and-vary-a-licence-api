package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.privateApi

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.Tags
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions.upload.UploadFileConditionsService

const val UPLOAD_FILE_CONDITION_ENDPOINT = "/licence/{licenceId}/condition/{conditionId}/supporting-document"

@Tag(name = Tags.CONDITION_SUPPORTING_DOCUMENTS)
@RestController
@RequestMapping(UPLOAD_FILE_CONDITION_ENDPOINT, produces = [MediaType.APPLICATION_JSON_VALUE])
class UploadFileConditionController(private val uploadFileConditionsService: UploadFileConditionsService) {

  @PostMapping(
    consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  @PreAuthorize("hasAnyRole('CVL_ADMIN')")
  @Operation(
    summary = "Upload a multipart/form-data request containing a PDF file.",
    description = "Uploads a condition file and description. Requires ROLE_CVL_ADMIN.",
    security = [SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "The condition file was uploaded",
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
    ],
  )
  fun uploadFile(
    @PathVariable(value = "licenceId") licenceId: Long,
    @PathVariable(value = "conditionId") conditionId: Long,
    @RequestPart("file") file: MultipartFile,
  ) = uploadFileConditionsService.uploadFile(licenceId, conditionId, file)

  @GetMapping(
    produces = [MediaType.IMAGE_JPEG_VALUE],
  )
  @PreAuthorize("hasAnyRole('CVL_ADMIN')")
  @ResponseBody
  @Operation(
    summary = "Get the condition image for a specified licence and condition",
    description = "Get the condition image. Requires ROLE_CVL_ADMIN.",
    security = [SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Image returned",
        content = [Content(mediaType = "image/jpeg")],
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
      ),
    ],
  )
  fun getImage(
    @PathVariable(name = "licenceId") licenceId: Long,
    @PathVariable(name = "conditionId") conditionId: Long,
  ): ByteArray? = uploadFileConditionsService.getImage(licenceId, conditionId)
}
