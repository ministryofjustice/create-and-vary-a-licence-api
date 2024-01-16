package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.privateApi.jobs

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Min
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.Tags
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.document.MigrateDocumentsToDSService

@Tag(name = Tags.JOBS)
@RestController
@RequestMapping("", produces = [MediaType.APPLICATION_JSON_VALUE])
class MigrateFilesToDocumentServiceController(
  private val migrateDocumentsToDS: MigrateDocumentsToDSService,
) {
  @PostMapping(value = ["/run-copy-documents/{maxDocsCountToMigrate}"])
  @PreAuthorize("hasAnyRole('CVL_ADMIN')")
  @Operation(
    summary = "Copy documents to documents service.",
    description = "Copy documents to document service. Requires ROLE_CVL_ADMIN.",
    security = [SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Request for copying the documents to the document service is processed successfully.",
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
  fun runMigrateDocumentsToDocumentServiceJob(
    @PathVariable("maxDocsCountToMigrate")
    @Parameter(
      name = "maxDocsCountToMigrate",
      description = "This is the max number of documents which should be migrated",
    )
    @Min(1)
    maxDocsCountToMigrate: Int,
  ) {
    return migrateDocumentsToDS.migrateDocuments(maxDocsCountToMigrate)
  }

  @PostMapping(value = ["/run-remove-copied-documents/{maxDocsCountToRemove}"])
  @PreAuthorize("hasAnyRole('CVL_ADMIN')")
  @Operation(
    summary = "Remove documents from database which are already copied to documents service.",
    description = "remove documents which are already copied to document service. Requires ROLE_CVL_ADMIN.",
    security = [SecurityRequirement(name = "ROLE_CVL_ADMIN")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Request for removing the documents is processed successfully.",
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
  fun runRemoveDocumentsAlreadyCopiedToDocumentServiceJob(
    @PathVariable("maxDocsCountToRemove")
    @Parameter(
      name = "maxDocsCountToRemove",
      description = "This is the max number of documents which should be migrated",
    )
    @Min(1)
    maxDocsCountToRemove: Int,
  ) {
    return migrateDocumentsToDS.removeDocuments(maxDocsCountToRemove)
  }
}
