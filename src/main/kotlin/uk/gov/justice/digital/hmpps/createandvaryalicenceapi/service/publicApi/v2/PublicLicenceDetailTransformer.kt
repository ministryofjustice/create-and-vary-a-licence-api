package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.publicApi.v2

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.v2.model.licence.ApConditions
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.v2.model.licence.Conditions
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.v2.model.licence.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.v2.model.licence.PssConditions
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.v2.model.licence.additionalConditions.ElectronicMonitoringAdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.v2.model.licence.additionalConditions.ElectronicMonitoringAdditionalConditionWithRestriction
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.v2.model.licence.additionalConditions.GenericAdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.v2.model.licence.additionalConditions.MultipleExclusionZoneAdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.v2.model.licence.additionalConditions.MultipleUploadAdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.v2.model.licence.additionalConditions.SingleUploadAdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.mapToPublicLicenceType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.ELECTRONIC_TAG_COND_CODE_14A
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.ELECTRONIC_TAG_COND_CODE_14B
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.ELECTRONIC_TAG_COND_CODE_14E
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.EVENT_EXCLUSION_COND_CODE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.EXCLUSION_ZONE_COND_CODE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.ElectronicMonitoringType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.MULTIPLE_UPLOAD_COND_CODE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.PolicyVersion
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.transformToResourceBespoke
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.transformToResourceStandard
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Licence as ModelLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.v2.model.licence.additionalConditions.AdditionalCondition as PublicAdditionalCondition

private val electronicMonitoringEndDateFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.ENGLISH)
private const val ELECTRONIC_MONITORING_TYPES = "electronicMonitoringTypes"

fun ModelLicence.transformToPublicLicence(): Licence = Licence(
  id = id,
  kind = kind,
  licenceType = typeCode.mapToPublicLicenceType(),
  policyVersion = PolicyVersion.entries.find { it.version == version }
    ?: error("Policy version not found for licence id:$id"),
  version = licenceVersion.orEmpty(),
  statusCode = statusCode!!,
  prisonNumber = nomsId.orEmpty(),
  bookingId = bookingId ?: 0,
  crn = crn.orEmpty(),
  approvedByUsername = approvedByUsername,
  approvedDateTime = approvedDate,
  createdByUsername = createdByUsername.orEmpty(),
  createdDateTime = dateCreated,
  updatedByUsername = updatedByUsername,
  updatedDateTime = dateLastUpdated,
  licenceStartDate = licenceStartDate,
  isInPssPeriod = isInPssPeriod ?: false,
  conditions = Conditions(
    apConditions = ApConditions(
      standard = standardLicenceConditions?.transformToResourceStandard().orEmpty(),
      additional = additionalLicenceConditions.map { it.toPublicAdditionalCondition() },
      bespoke = bespokeConditions.transformToResourceBespoke(),
    ),
    pssConditions = PssConditions(
      standard = standardPssConditions?.transformToResourceStandard().orEmpty(),
      additional = additionalPssConditions.map { it.toPublicAdditionalCondition() },
    ),
  ),
)

private fun AdditionalCondition.toPublicAdditionalCondition(): PublicAdditionalCondition = when (code) {
  ELECTRONIC_TAG_COND_CODE_14A -> transformElectronicMonitoringWithRestrictions(this)
  ELECTRONIC_TAG_COND_CODE_14B, ELECTRONIC_TAG_COND_CODE_14E -> transformElectronicMonitoring(this)
  EXCLUSION_ZONE_COND_CODE -> transformMultipleExclusionZonesCondition(this)
  MULTIPLE_UPLOAD_COND_CODE -> transformMultipleUploadsCondition(this)
  EVENT_EXCLUSION_COND_CODE -> transformSingleExclusionZoneCondition(this)
  else -> transformGenericAdditionalCondition(this)
}

fun transformElectronicMonitoringWithRestrictions(model: AdditionalCondition) = ElectronicMonitoringAdditionalConditionWithRestriction(
  category = model.category.orEmpty(),
  id = model.id ?: 0,
  code = model.code.orEmpty(),
  text = model.expandedText.orEmpty(),
  restrictions = model.data.filter { it.field == ELECTRONIC_MONITORING_TYPES }.map {
    ElectronicMonitoringType.find(it.value.orEmpty())
      ?: error("ElectronicMonitoringType '${it.value}' isn't supported.")
  },
  endDate = electronicMonitoringEndDate(model),
)

private fun transformElectronicMonitoring(model: AdditionalCondition) = ElectronicMonitoringAdditionalCondition(
  category = model.category.orEmpty(),
  id = model.id ?: 0,
  code = model.code.orEmpty(),
  text = model.expandedText.orEmpty(),
  endDate = electronicMonitoringEndDate(model),
)

private fun transformMultipleExclusionZonesCondition(model: AdditionalCondition) = MultipleExclusionZoneAdditionalCondition(
  category = model.category.orEmpty(),
  id = model.id ?: 0,
  code = model.code.orEmpty(),
  text = model.expandedText.orEmpty(),
  hasImageUpload = model.uploadSummary.isNotEmpty(),
  description = model.uploadDescription(),
)

private fun transformMultipleUploadsCondition(model: AdditionalCondition) = MultipleUploadAdditionalCondition(
  category = model.category.orEmpty(),
  id = model.id ?: 0,
  code = model.code.orEmpty(),
  text = model.expandedText.orEmpty(),
  hasImageUpload = model.uploadSummary.isNotEmpty(),
  description = model.uploadDescription(),
)

private fun transformSingleExclusionZoneCondition(model: AdditionalCondition) = SingleUploadAdditionalCondition(
  category = model.category.orEmpty(),
  id = model.id ?: 0,
  code = model.code.orEmpty(),
  text = model.expandedText.orEmpty(),
  hasImageUpload = model.uploadSummary.isNotEmpty(),
  description = model.uploadDescription(),
)

private fun transformGenericAdditionalCondition(model: AdditionalCondition) = GenericAdditionalCondition(
  category = model.category.orEmpty(),
  id = model.id ?: 0,
  code = model.code.orEmpty(),
  text = model.expandedText.orEmpty(),
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

private fun AdditionalCondition.uploadDescription(): String? = uploadSummary
  .mapNotNull { it.description?.takeIf(String::isNotBlank) }
  .distinct()
  .takeIf { it.isNotEmpty() }
  ?.joinToString(", ")
