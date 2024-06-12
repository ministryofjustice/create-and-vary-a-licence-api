package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest

data class PageResponse<T>(val content: List<T>, val number: Int, val size: Int, val totalElements: Long) {
  fun toPage(): Page<T> {
    return PageImpl(content, PageRequest.of(number, size), totalElements)
  }
}
