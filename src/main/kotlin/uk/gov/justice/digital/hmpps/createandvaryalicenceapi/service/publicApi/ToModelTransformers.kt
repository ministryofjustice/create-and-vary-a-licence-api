package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.publicApi

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.AdditionalConditionAp
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.AdditionalConditionPss
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.LicencePolicy
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.StandardConditionAp
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.StandardConditionPss
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licencePolicy.ConditionTypes
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licencePolicy.LicencePolicyConditions
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.PolicyVersion
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.LicenceType as PublicLicenceType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.PublicLicenceSummary as ModelPublicLicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licencePolicy.LicencePolicy as ModelPublicLicencePolicy
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licencePolicy.LicencePolicyAdditionalCondition as ModelPublicAdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licencePolicy.StandardCondition as ModelPublicStandardCondition

/*
** Functions which transform JPA entity objects into their public API model equivalents.
** Mostly pass-thru but some translations, so useful to keep the database objects separate from API objects.
*/

fun Licence.transformToPublicLicenceSummary(): ModelPublicLicenceSummary = ModelPublicLicenceSummary(
  id = this.id,
  kind = this.kind,
  licenceType = this.typeCode.mapToPublicLicenceType(),
  policyVersion = PolicyVersion.entries.find { it.version == this.version } ?: this.valueNotPresent("policyVersion"),
  version = this.licenceVersion ?: this.valueNotPresent("version"),
  statusCode = this.statusCode,
  prisonNumber = this.nomsId ?: this.valueNotPresent("prisonNumber"),
  bookingId = this.bookingId ?: this.valueNotPresent("bookingId"),
  crn = this.crn ?: this.valueNotPresent("crn"),
  approvedByUsername = this.approvedByUsername,
  approvedDateTime = this.approvedDate,
  createdByUsername = this.getCreator().username,
  createdDateTime = this.dateCreated ?: this.valueNotPresent("createdDateTime"),
  updatedByUsername = this.updatedByUsername,
  updatedDateTime = this.dateLastUpdated,
  isInPssPeriod = this.isInPssPeriod(),
)

private fun Licence.valueNotPresent(fieldName: String): Nothing = error("Null field retrieved: $fieldName for licence ${this.id}")

private fun LicenceType.mapToPublicLicenceType() = when {
  this == LicenceType.AP -> PublicLicenceType.AP
  this == LicenceType.PSS -> PublicLicenceType.PSS
  this == LicenceType.AP_PSS -> PublicLicenceType.AP_PSS
  else -> error("No matching licence type found")
}

fun LicencePolicy.transformToPublicLicencePolicy(): ModelPublicLicencePolicy = ModelPublicLicencePolicy(
  version = PolicyVersion.entries.find { it.version == this.version } ?: error("Policy ${this.version} not found"),
  conditions = this.getAllConditions(),
)

private fun LicencePolicy.getAllConditions(): ConditionTypes = ConditionTypes(
  this.getAllApConditions(),
  this.getAllPssConditions(),
)

private fun LicencePolicy.getAllApConditions(): LicencePolicyConditions {
  val standardConditionsAp = this.standardConditions.standardConditionsAp
  val additionalConditionAp = this.additionalConditions.ap

  val mappedStandardConditions = standardConditionsAp.transformToModelPublicApStandardCondition()
  val mappedAdditionalConditions = additionalConditionAp.transformToModelPublicApAdditionalCondition()

  return LicencePolicyConditions(
    mappedStandardConditions,
    mappedAdditionalConditions,
  )
}

private fun LicencePolicy.getAllPssConditions(): LicencePolicyConditions {
  val standardConditionsPss = this.standardConditions.standardConditionsPss
  val additionalConditionPss = this.additionalConditions.pss

  val mappedStandardConditions = standardConditionsPss.transformToModelPublicPssStandardCondition()
  val mappedAdditionalConditions = additionalConditionPss.transformToModelPublicPssAdditionalCondition()

  return LicencePolicyConditions(
    mappedStandardConditions,
    mappedAdditionalConditions,
  )
}

private fun List<StandardConditionAp>.transformToModelPublicApStandardCondition(): List<ModelPublicStandardCondition> = map(::transform)

private fun transform(entity: StandardConditionAp): ModelPublicStandardCondition = ModelPublicStandardCondition(
  code = entity.code,
  text = entity.text,
)

private fun List<AdditionalConditionAp>.transformToModelPublicApAdditionalCondition(): List<ModelPublicAdditionalCondition> = map(::transform)

private fun transform(entity: AdditionalConditionAp): ModelPublicAdditionalCondition = ModelPublicAdditionalCondition(
  code = entity.code,
  text = entity.text,
  category = entity.category,
  categoryShort = entity.categoryShort,
  requiresUserInput = entity.requiresInput,
)

private fun List<StandardConditionPss>.transformToModelPublicPssStandardCondition(): List<ModelPublicStandardCondition> = map(::transform)

private fun transform(entity: StandardConditionPss): ModelPublicStandardCondition = ModelPublicStandardCondition(
  code = entity.code,
  text = entity.text,
)

private fun List<AdditionalConditionPss>.transformToModelPublicPssAdditionalCondition(): List<ModelPublicAdditionalCondition> = map(::transform)

private fun transform(entity: AdditionalConditionPss): ModelPublicAdditionalCondition = ModelPublicAdditionalCondition(
  code = entity.code,
  text = entity.text,
  category = entity.category,
  categoryShort = entity.categoryShort,
  requiresUserInput = entity.requiresInput,
)
