package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.events

data class UpdateProbationTeamEvent(
  val probationAreaCode: String? = null,

  val probationAreaDescription: String? = null,

  val probationPduCode: String? = null,

  val probationPduDescription: String? = null,

  val probationLauCode: String? = null,

  val probationLauDescription: String? = null,

  val probationTeamCode: String? = null,

  val probationTeamDescription: String? = null,
)
