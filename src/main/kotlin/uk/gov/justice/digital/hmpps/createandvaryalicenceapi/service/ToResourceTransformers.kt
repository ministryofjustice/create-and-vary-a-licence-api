package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.*
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.additionalConditions.ConditionTypes
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.additionalConditions.ElectronicMonitoringType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licencePolicy.StandardCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.additionalConditions.StandardAdditionalCondition as ModelStandardAdditionalCondition


/*
** Functions which transform JPA model objects into their API model equivalents.
** Mostly pass-thru but some translations, so useful to keep the database objects separate from API objects.
*/

fun uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Licence.transformToPublicLicence(): Licence {
  val licenseConditions = Conditions(
    apConditions = ApConditions(
      this.standardLicenceConditions?.transformToResourceStandard().orEmpty(),
      this.additionalLicenceConditions.transformToResourceAdditional(),
      this.bespokeConditions.transformToResourceBespoke(),
    ),
    pssConditions = PssConditions(
      this.standardPssConditions?.transformToResourceStandard().orEmpty(),
      this.additionalPssConditions.transformToResourceAdditional(),
    ),
  )
  return Licence(
    id = this.id,
    licenceType = this.typeCode.mapToPublicLicenceType(),
    policyVersion = this.version.orEmpty(),
    version = this.licenceVersion.orEmpty(),
    statusCode = LicenceStatus.valueOf(statusCode.toString()),

    prisonNumber = this.nomsId.orEmpty(),
    bookingId = this.bookingId ?: 0,
    crn = this.crn.orEmpty(),
    approvedByUsername = this.approvedByUsername,
    approvedDateTime = this.approvedDate,
    createdByUsername = this.createdByUsername.orEmpty(),
    createdDateTime = this.dateCreated,
    updatedByUsername = this.updatedByUsername,
    updatedDateTime = this.dateLastUpdated,
    isInPssPeriod = this.isInPssPeriod ?: false,
    conditions = licenseConditions,

    )
}

private fun uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType.mapToPublicLicenceType() = when {
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

private const val ELECTRONIC_TAG_COND_CODE = "fd129172-bdd3-4d97-a4a0-efd7b47a49d4"
private const val EXCLUSION_ZONE_COND_CODE = "0f9a20f4-35c7-4c77-8af8-f200f153fa11"

// Transform a list of model additional conditions to resource additional conditions
fun List<AdditionalCondition>.transformToResourceAdditional(): List<uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.additionalConditions.AdditionalCondition> {
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

private const val ELECTRONIC_MONITORING_TYPES = "electronicMonitoringTypes"

fun transformElectronicMonitoring(model: AdditionalCondition): uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.additionalConditions.ElectronicMonitoringAdditionalCondition {


  return uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.additionalConditions.ElectronicMonitoringAdditionalCondition(
    category = model.category.orEmpty(),
    type = ConditionTypes.ELECTRONIC_MONITORING,
    id = model.id ?: 0,
    code = model.code.orEmpty(),
    text = model.text.orEmpty(),
    electronicMonitoringTypes = model.data.filter { data -> data.field == ELECTRONIC_MONITORING_TYPES }
      .map { data -> ElectronicMonitoringType.find(data.value.orEmpty())!! },
  )
}

fun transformMultipleExclusionZonesCondition(model: AdditionalCondition): uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.additionalConditions.MultipleExclusionZoneAdditionalCondition {
  return uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.additionalConditions.MultipleExclusionZoneAdditionalCondition(
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

fun transform(model: uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.BespokeCondition): BespokeCondition {
  return BespokeCondition(

    text = model.text.orEmpty(),
  )
}