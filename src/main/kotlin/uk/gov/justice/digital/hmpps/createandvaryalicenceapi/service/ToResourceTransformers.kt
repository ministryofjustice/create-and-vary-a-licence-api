package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.LicenceType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.BespokeCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.additionalConditions.ConditionTypes
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.additionalConditions.ElectronicMonitoringAdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.additionalConditions.ElectronicMonitoringType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.additionalConditions.MultipleExclusionZoneAdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licencePolicy.StandardCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.ELECTRONIC_TAG_COND_CODE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.EXCLUSION_ZONE_COND_CODE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.BespokeCondition as ModelBespokeCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.additionalConditions.AdditionalCondition as ModelAdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.additionalConditions.StandardAdditionalCondition as ModelStandardAdditionalCondition

/*
** Functions which transform JPA model objects into their API model equivalents.
** Mostly pass-thru but some translations, so useful to keep the database objects separate from API objects.
*/
private const val ELECTRONIC_MONITORING_TYPES = "electronicMonitoringTypes"

fun uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType.mapToPublicLicenceType() = when {
  this == uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType.AP -> LicenceType.AP
  this == uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType.PSS -> LicenceType.PSS
  this == uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType.AP_PSS -> LicenceType.AP_PSS
  else -> error("No matching licence type found")
}

// Transform a list of model standard conditions to resource standard conditions
fun List<uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.StandardCondition>.transformToResourceStandard(): List<StandardCondition> =
  map(::transform)

fun transform(condition: uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.StandardCondition): StandardCondition {
  return StandardCondition(
    code = condition.code.orEmpty(),
    text = condition.text.orEmpty(),
  )
}

// Transform a list of model additional conditions to resource additional conditions
fun List<AdditionalCondition>.transformToResourceAdditional(): List<ModelAdditionalCondition> {
  val electronicMonitoringConditions =
    filter { condition -> condition.code == ELECTRONIC_TAG_COND_CODE }.map(::transformElectronicMonitoring)
  val multipleExclusionZoneAdditionalConditions =
    filter { condition -> condition.code == EXCLUSION_ZONE_COND_CODE }.map(::transformMultipleExclusionZonesCondition)
  val stdConditions =
    filter { condition -> condition.code != ELECTRONIC_TAG_COND_CODE && condition.code != EXCLUSION_ZONE_COND_CODE }.map(
      ::standardAdditionalCondition,
    )

  return electronicMonitoringConditions + multipleExclusionZoneAdditionalConditions + stdConditions
}

fun transformElectronicMonitoring(model: AdditionalCondition): ElectronicMonitoringAdditionalCondition {

  return ElectronicMonitoringAdditionalCondition(
    category = model.category.orEmpty(),
    type = ConditionTypes.ELECTRONIC_MONITORING,
    id = model.id ?: 0,
    code = model.code.orEmpty(),
    text = model.text.orEmpty(),
    electronicMonitoringTypes = model.data.filter { data -> data.field == ELECTRONIC_MONITORING_TYPES }
      .map { data -> ElectronicMonitoringType.find(data.value.orEmpty())!! },
  )
}

fun transformMultipleExclusionZonesCondition(model: AdditionalCondition): MultipleExclusionZoneAdditionalCondition {
  return MultipleExclusionZoneAdditionalCondition(
    category = model.category.orEmpty(),
    type = ConditionTypes.MULTIPLE_EXCLUSION_ZONE,
    id = model.id ?: 0,
    code = model.code.orEmpty(),
    text = model.text.orEmpty(),
    hasImageUpload = model.uploadSummary.isNotEmpty(),
  )
}


typealias PublicStandardAdditionalCondition = ModelStandardAdditionalCondition

fun standardAdditionalCondition(model: AdditionalCondition): PublicStandardAdditionalCondition {

  return PublicStandardAdditionalCondition(
    category = model.category.orEmpty(),
    type = ConditionTypes.STANDARD,
    id = model.id ?: 0,
    code = model.code.orEmpty(),
    text = model.text.orEmpty(),
  )
}

// Transform a list of model bespoke conditions to resource bespoke conditions
fun List<uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.BespokeCondition>.transformToResourceBespoke(): List<BespokeCondition> =
  map(::transform)

fun transform(model: ModelBespokeCondition): BespokeCondition {
  return BespokeCondition(

    text = model.text.orEmpty(),
  )
}
