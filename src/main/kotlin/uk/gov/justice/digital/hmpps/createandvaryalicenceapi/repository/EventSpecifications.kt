package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository

import jakarta.validation.ValidationException
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.LicenceEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.kotlinjpaspecificationdsl.and
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.kotlinjpaspecificationdsl.equal
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.kotlinjpaspecificationdsl.`includedIn`
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceEventType

data class EventQueryObject(
  val licenceId: Long? = null,
  val eventTypes: List<LicenceEventType>? = null,
  val sortBy: String? = null,
  val sortOrder: String? = null,
)

fun EventQueryObject.toSpecification(): Specification<LicenceEvent> = and(
  hasLicenceEqualTo(licenceId),
  hasEventTypeIn(eventTypes),
)

fun EventQueryObject.getSort(): Sort = when {
  sortBy == null -> Sort.unsorted()
  sortOrder == null -> Sort.by(Sort.Direction.ASC, sortBy)
  else -> {
    try {
      Sort.by(Sort.Direction.fromString(sortOrder), sortBy)
    } catch (e: IllegalArgumentException) {
      throw ValidationException(e.message, e)
    }
  }
}

fun hasLicenceEqualTo(licenceId: Long?): Specification<LicenceEvent>? = licenceId?.let {
  LicenceEvent::licenceId.equal(it)
}

fun hasEventTypeIn(eventTypes: List<LicenceEventType>?): Specification<LicenceEvent>? = eventTypes?.let {
  LicenceEvent::eventType.includedIn(it)
}
