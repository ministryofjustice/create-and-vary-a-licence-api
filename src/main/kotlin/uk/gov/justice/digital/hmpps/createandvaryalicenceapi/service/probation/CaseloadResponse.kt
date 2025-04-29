package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation

import org.springframework.data.web.PagedModel

data class CaseloadResponse(
  val content: List<CaseloadResult>,
  val page: PagedModel.PageMetadata? = null,
)
