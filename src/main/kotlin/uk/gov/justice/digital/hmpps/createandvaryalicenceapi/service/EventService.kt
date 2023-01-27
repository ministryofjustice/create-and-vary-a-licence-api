package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import jakarta.validation.ValidationException
import org.springframework.data.mapping.PropertyReferenceException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.EventQueryObject
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.getSort
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.toSpecification

@Service
class EventService(private val licenceEventRepository: LicenceEventRepository) {

  fun findEventsMatchingCriteria(eventQueryObject: EventQueryObject): List<LicenceEvent> {
    try {
      val matchingEvents = licenceEventRepository.findAll(eventQueryObject.toSpecification(), eventQueryObject.getSort())
      return matchingEvents.transformToModelEvents()
    } catch (e: PropertyReferenceException) {
      throw ValidationException(e.message)
    }
  }
}
