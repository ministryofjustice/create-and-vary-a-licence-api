package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs.promptingCom

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Case

data class PromptComNotification(
  val email: String,
  val comName: String,
  val initialPromptCases: List<Case> = emptyList(),
)
