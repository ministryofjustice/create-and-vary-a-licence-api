package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.ApConditions
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.BespokeCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.Conditions
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.LicenceType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.PssConditions
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.additionalConditions.ConditionTypes
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.additionalConditions.ElectronicMonitoringAdditionalConditionWithRestriction
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.additionalConditions.MultipleExclusionZoneAdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.additionalConditions.MultipleUploadAdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.additionalConditions.SingleUploadAdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licencePolicy.StandardCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.ELECTRONIC_TAG_COND_CODE_14A
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.ELECTRONIC_TAG_COND_CODE_14B
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.ELECTRONIC_TAG_COND_CODE_14E
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.EVENT_EXCLUSION_COND_CODE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.EXCLUSION_ZONE_COND_CODE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.ElectronicMonitoringType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.MULTIPLE_UPLOAD_COND_CODE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.PolicyVersion
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.BespokeCondition as ModelBespokeCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Licence as ModelLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.additionalConditions.AdditionalCondition as ModelAdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.additionalConditions.GenericAdditionalCondition as ModelStandardAdditionalCondition

/*
** Functions which transform JPA model objects into their API model equivalents.
** Mostly pass-thru but some translations, so useful to keep the database objects separate from API objects.
*/
private const val ELECTRONIC_MONITORING_TYPES = "electronicMonitoringTypes"
private val electronicMonitoringEndDateFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.ENGLISH)

fun ModelLicence.transformToPublicLicence(): Licence {
  val licenceConditions = Conditions(
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
    kind = this.kind,
    licenceType = this.typeCode.mapToPublicLicenceType(),
    policyVersion = PolicyVersion.entries.find { it.version == this.version }
      ?: error("Policy version not found for licence id:" + this.id),
    version = this.licenceVersion.orEmpty(),
    statusCode = this.statusCode!!,
    prisonNumber = this.nomsId.orEmpty(),
    bookingId = this.bookingId ?: 0,
    crn = this.crn.orEmpty(),
    approvedByUsername = this.approvedByUsername,
    approvedDateTime = this.approvedDate,
    createdByUsername = this.createdByUsername.orEmpty(),
    createdDateTime = this.dateCreated,
    updatedByUsername = this.updatedByUsername,
    updatedDateTime = this.dateLastUpdated,
    licenceStartDate = this.licenceStartDate,
    isInPssPeriod = this.isInPssPeriod ?: false,
    conditions = licenceConditions,
  )
}

fun uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType.mapToPublicLicenceType() = when {
  this == uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType.AP -> LicenceType.AP
  this == uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType.PSS -> LicenceType.PSS
  this == uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType.AP_PSS -> LicenceType.AP_PSS
  else -> error("No matching licence type found")
}

// Transform a list of model standard conditions to resource standard conditions
fun List<uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.StandardCondition>.transformToResourceStandard(): List<StandardCondition> = map(::transform)

fun transform(condition: uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.StandardCondition): StandardCondition = StandardCondition(
  code = condition.code,
  text = condition.text,
)

// Transform a list of model additional conditions to resource additional conditions
fun List<AdditionalCondition>.transformToResourceAdditional(): List<ModelAdditionalCondition> = map {
  when (it.code) {
    ELECTRONIC_TAG_COND_CODE_14A, ELECTRONIC_TAG_COND_CODE_14B, ELECTRONIC_TAG_COND_CODE_14E ->
      transformElectronicTagCondition(it)
    EXCLUSION_ZONE_COND_CODE -> transformMultipleExclusionZonesCondition(it)
    MULTIPLE_UPLOAD_COND_CODE -> transformMultipleUploadsCondition(it)
    EVENT_EXCLUSION_COND_CODE -> transformSingleExclusionZoneCondition(it)
    else -> standardAdditionalCondition(it)
  }
}

fun transformElectronicTagCondition(model: AdditionalCondition): ModelAdditionalCondition = when (model.code) {
  ELECTRONIC_TAG_COND_CODE_14A -> transformElectronicMonitoring(model)
  ELECTRONIC_TAG_COND_CODE_14B, ELECTRONIC_TAG_COND_CODE_14E ->
    standardAdditionalCondition(model).copy(endDate = electronicMonitoringEndDate(model))
  else -> error("Electronic monitoring condition code '${model.code}' isn't supported.")
}

fun transformElectronicMonitoring(model: AdditionalCondition) = ElectronicMonitoringAdditionalConditionWithRestriction(
  category = model.category.orEmpty(),
  type = ConditionTypes.ELECTRONIC_MONITORING,
  id = model.id ?: 0,
  code = model.code.orEmpty(),
  text = model.expandedText.orEmpty(),
  restrictions = model.data.filter { data -> data.field == ELECTRONIC_MONITORING_TYPES }.map { data ->
    ElectronicMonitoringType.find(data.value.orEmpty())
      ?: error("ElectronicMonitoringType '" + data.value + "' isn't supported.")
  },
  endDate = electronicMonitoringEndDate(model),
)

private fun electronicMonitoringEndDate(model: AdditionalCondition): LocalDate? = when (model.code) {
  ELECTRONIC_TAG_COND_CODE_14A, ELECTRONIC_TAG_COND_CODE_14B, ELECTRONIC_TAG_COND_CODE_14E ->
    model.data
      .find { it.field == "endDate" }
      ?.value
      ?.takeIf { it.isNotBlank() }
      ?.let { LocalDate.parse(it, electronicMonitoringEndDateFormatter) }
  else -> null
}

fun transformMultipleExclusionZonesCondition(model: AdditionalCondition) = MultipleExclusionZoneAdditionalCondition(
  category = model.category.orEmpty(),
  type = ConditionTypes.MULTIPLE_EXCLUSION_ZONE,
  id = model.id ?: 0,
  code = model.code.orEmpty(),
  text = model.expandedText.orEmpty(),
  hasImageUpload = model.uploadSummary.isNotEmpty(),
  description = transformUploadDescription(model),
)

fun transformMultipleUploadsCondition(model: AdditionalCondition) = MultipleUploadAdditionalCondition(
  category = model.category.orEmpty(),
  type = ConditionTypes.MULTIPLE_UPLOADS,
  id = model.id ?: 0,
  code = model.code.orEmpty(),
  text = model.expandedText.orEmpty(),
  hasImageUpload = model.uploadSummary.isNotEmpty(),
  description = transformUploadDescription(model),
)

fun transformSingleExclusionZoneCondition(model: AdditionalCondition) = SingleUploadAdditionalCondition(
  category = model.category.orEmpty(),
  type = ConditionTypes.SINGLE_UPLOAD,
  id = model.id ?: 0,
  code = model.code.orEmpty(),
  text = model.expandedText.orEmpty(),
  hasImageUpload = model.uploadSummary.isNotEmpty(),
  description = transformUploadDescription(model),
)

private fun transformUploadDescription(model: AdditionalCondition): String? = model.uploadSummary
  .mapNotNull { it.description?.takeIf(String::isNotBlank) }
  .distinct()
  .takeIf { it.isNotEmpty() }
  ?.joinToString(", ")

fun standardAdditionalCondition(model: AdditionalCondition) = ModelStandardAdditionalCondition(
  category = model.category.orEmpty(),
  type = ConditionTypes.STANDARD,
  id = model.id ?: 0,
  code = model.code.orEmpty(),
  text = model.expandedText.orEmpty(),
)

// Transform a list of model bespoke conditions to resource bespoke conditions
fun List<uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.BespokeCondition>.transformToResourceBespoke(): List<BespokeCondition> = map(::transform)

fun transform(model: ModelBespokeCondition): BespokeCondition = BespokeCondition(
  text = model.text.orEmpty(),
)
