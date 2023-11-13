package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.*
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.additionalConditions.ConditionTypes
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.additionalConditions.ElectronicMonitoringType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licencePolicy.StandardCondition


/*
** Functions which transform JPA model objects into their API model equivalents.
** Mostly pass-thru but some translations, so useful to keep the database objects separate from API objects.
*/

fun transformToPublicLicence(licence: uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Licence): Licence {
  val licenseConditions = Conditions(
    apConditions = ApConditions(
      licence.standardLicenceConditions?.transformToResourceStandard("AP").orEmpty(),
      licence.additionalLicenceConditions.transformToResourceAdditional("AP"),
      licence.bespokeConditions.transformToResourceBespoke(),
    ),
    pssConditions = PssConditions(
      licence.standardPssConditions?.transformToResourceStandard("PSS").orEmpty(),
      licence.additionalPssConditions.transformToResourceAdditional("PSS"),
    ),
  )
  return Licence(
    id = licence.id,
    licenceType = licence.typeCode.mapToPublicLicenceType(),
    policyVersion = licence.version.orEmpty(),
    version = licence.licenceVersion.orEmpty(),
    statusCode = LicenceStatus.valueOf(licence.statusCode.toString()),

    prisonNumber = licence.nomsId.orEmpty(),
    bookingId = licence.bookingId ?: 0,
    crn = licence.crn.orEmpty(),
    approvedByUsername = licence.approvedByUsername,
    approvedDateTime = licence.approvedDate,
    createdByUsername = licence.createdByUsername.orEmpty(),
    createdDateTime = licence.dateCreated,
    updatedByUsername = licence.updatedByUsername,
    updatedDateTime = licence.dateLastUpdated,
    isInPssPeriod = licence.isInPssPeriod ?: false,
    conditions = licenseConditions,

    )
}

private fun uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType.mapToPublicLicenceType() =
  when {
    this == uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType.AP -> LicenceType.AP
    this == uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType.PSS -> LicenceType.PSS
    this == uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType.AP_PSS -> LicenceType.AP_PSS
    else -> error("No matching licence type found")
  }

// Transform a list of model standard conditions to resource standard conditions
fun List<uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.StandardCondition>.transformToResourceStandard(
  conditionType: String,
): List<StandardCondition> =
  map(::transform)

fun transform(condition: uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.StandardCondition): StandardCondition {
  return StandardCondition(
    code = condition.code.orEmpty(),
    text = condition.text.orEmpty(),
  )
}

private const val ELECTRONIC_MONITORING = "Electronic monitoring"
private const val FREEDOM_OF_MOVEMENT = "Freedom of movement"

// Transform a list of model additional conditions to resource additional conditions
fun List<AdditionalCondition>.transformToResourceAdditional(conditionType: String): List<uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.additionalConditions.AdditionalCondition> {
  val electronicMonitoringConditions =
    filter { condition -> condition.category == ELECTRONIC_MONITORING }.map(::transformElectronicMonitoring)
  val multipleExclusionZoneAdditionalConditions =
    filter { condition -> condition.category == FREEDOM_OF_MOVEMENT }.map(::transformMultipleExclusionZonesCondition)
  val stdConditions =
    filter { condition -> condition.category != FREEDOM_OF_MOVEMENT && condition.category != ELECTRONIC_MONITORING}.map(::transformMultipleExclusionZonesCondition)

  return electronicMonitoringConditions+multipleExclusionZoneAdditionalConditions+stdConditions
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
      .map { data -> ElectronicMonitoringType.valueOf(data.value.orEmpty()) },
  )
}

fun transformMultipleExclusionZonesCondition(model: AdditionalCondition): uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.additionalConditions.MultipleExclusionZoneAdditionalCondition {
  return uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.additionalConditions.MultipleExclusionZoneAdditionalCondition(
    category = model.category.orEmpty(),
    type = ConditionTypes.MULTIPLE_EXCLUSION_ZONE,
    id = model.id ?: 0,
    code = model.code.orEmpty(),
    text = model.text.orEmpty(),
    hasImageUpload = !model.uploadSummary.isEmpty(),
  )
}
fun transformStdCondition(model: AdditionalCondition): uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.additionalConditions.StandardAdditionalCondition {

  return uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.additionalConditions.StandardAdditionalCondition(
    category = model.category.orEmpty(),
    type = ConditionTypes.STANDARD,
    id = model.id ?: 0,
    code = model.code.orEmpty(),
    text = model.text.orEmpty(),
  )
}
// Transform a list of model bespoke conditions to resource bespoke conditions
fun List<uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.BespokeCondition>.transformToResourceBespoke(): List<uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.BespokeCondition> =
  map(::transform)

fun transform(model: uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.BespokeCondition): uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.BespokeCondition {
  return uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.BespokeCondition(

    text = model.text.orEmpty(),
  )
}