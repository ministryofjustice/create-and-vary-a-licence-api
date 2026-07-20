package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents

interface EventHandler {
  fun handleEvent(message: String)
}
