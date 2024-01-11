package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.document

import io.swagger.v3.oas.annotations.media.Schema

class CreateDocumentRequest(
  @Schema(
    description = "The file payload in binary format",
    example = "exclusionZoneMapThumbnail",
  )
  val file: String,
  @Schema(
    description = "The document meta data",
    example = "exclusionZoneMapThumbnail",
  )
  val metadata: DocumentMetaData,
)
