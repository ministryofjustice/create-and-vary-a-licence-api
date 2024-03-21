package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.document

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(
  description =
  "Document properties and metadata associated with the document. This doesn't contain the actual file data. The document file is downloaded " +
    "separately from document api, using the GET /documents/{documentUuid}/file endpoint.",
)
data class Document(
  @Schema(
    description = "The unique identifier assigned to the document",
    example = "8cdadcf3-b003-4116-9956-c99bd8df6a00",
  )
  val documentUuid: UUID,
)
