package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.events

import java.time.LocalDate

data class UpdateOffenderDetailsEvent(
  val forename: String? = null,
  val middleNames: String? = null,
  val surname: String? = null,
  val dateOfBirth: LocalDate? = null,
)
