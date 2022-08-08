package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.ConditionChangesByType

data class PolicyChanges(
  val standardConditions: ConditionChangesByType<StandardConditionAp, StandardConditionPss>,
  val additionalConditions: ConditionChangesByType<AdditionalConditionAp, AdditionalConditionPss>
)
