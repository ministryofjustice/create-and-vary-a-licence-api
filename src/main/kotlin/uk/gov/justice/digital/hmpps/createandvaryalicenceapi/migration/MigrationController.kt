package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.migration

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.migration.request.MigrateFromHdcToCvlRequest

@RestController
@RequestMapping("/licences/migration")
@Tag(name = "HDC Licence Migration", description = "Operations related to licence migration from HDC")
class MigrationController(
  private val migrationService: MigrationService,
) {

  @PostMapping
  @PreAuthorize("hasAnyRole('CVL_ADMIN')")
  @Operation(
    summary = "Migrate a single licence from HDC to CVL",
    description = "Triggers migration of the supplied data into CVL",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Licence migrated to CVL successfully",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Invalid request, Migration failed",
      ),
    ],
  )
  fun migrateLicenceToCvl(
    @Valid @RequestBody request: MigrateFromHdcToCvlRequest,
  ): ResponseEntity<Void> {
    migrationService.migrate(request)
    return ResponseEntity.ok().build()
  }
}
