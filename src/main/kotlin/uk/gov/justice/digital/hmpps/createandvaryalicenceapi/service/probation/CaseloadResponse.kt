package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation

import org.springframework.data.web.PagedModel.PageMetadata

data class CaseloadResponse(
  val content: List<ManagedOffender>,
  val page: PageMetadata? = null,
)
