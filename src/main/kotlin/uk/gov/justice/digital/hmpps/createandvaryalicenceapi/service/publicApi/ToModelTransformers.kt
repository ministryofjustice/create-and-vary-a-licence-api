package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.publicApi

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.LicenceSummary as ModelPublicLicenceSummary

/*
** Functions which transform JPA entity objects into their public API model equivalents.
** Mostly pass-thru but some translations, so useful to keep the database objects separate from API objects.
*/

fun Licence.transformToPublicLicenceSummary(): ModelPublicLicenceSummary {
  return ModelPublicLicenceSummary(
    id = this.id,
    licenceType = this.typeCode.mapToPublicLicenceType(),
    policyVersion = this.licenceVersion ?: this.valueNotPresent("policyVersion"),
    version = this.version ?: this.valueNotPresent("version"),
    statusCode = this.statusCode.mapToPublicLicenceStatus(),
    prisonNumber = this.nomsId ?: this.valueNotPresent("prisonNumber"),
    bookingId = this.bookingId ?: this.valueNotPresent("bookingId"),
    crn = this.crn ?: this.valueNotPresent("crn"),
    approvedByUsername = this.approvedByUsername,
    approvedDateTime = this.approvedDate,
    createdByUsername = this.createdBy?.username ?: this.valueNotPresent("createdByUsername"),
    createdDateTime = this.dateCreated ?: this.valueNotPresent("createdDateTime"),
    updatedByUsername = this.updatedByUsername,
    updatedDateTime = this.dateLastUpdated,
    isInPssPeriod = this.isInPssPeriod(),
  )
}

private fun Licence.valueNotPresent(fieldName: String): Nothing = error("Null field retrieved: $fieldName for licence ${this.id}")

private fun LicenceType.mapToPublicLicenceType() =
  when {
    this == LicenceType.AP -> uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.LicenceType.AP
    this == LicenceType.PSS -> uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.LicenceType.PSS
    this == LicenceType.AP_PSS -> uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.LicenceType.AP_PSS
    else -> error("No matching licence type found")
  }

private fun LicenceStatus.mapToPublicLicenceStatus() =
  when {
    this == LicenceStatus.IN_PROGRESS -> uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.LicenceStatus.IN_PROGRESS
    this == LicenceStatus.SUBMITTED -> uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.LicenceStatus.SUBMITTED
    this == LicenceStatus.APPROVED -> uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.LicenceStatus.APPROVED
    this == LicenceStatus.ACTIVE -> uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.LicenceStatus.ACTIVE
    this == LicenceStatus.REJECTED -> uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.LicenceStatus.REJECTED
    this == LicenceStatus.INACTIVE -> uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.LicenceStatus.INACTIVE
    this == LicenceStatus.VARIATION_IN_PROGRESS -> uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.LicenceStatus.VARIATION_IN_PROGRESS
    this == LicenceStatus.VARIATION_SUBMITTED -> uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.LicenceStatus.VARIATION_SUBMITTED
    this == LicenceStatus.VARIATION_APPROVED -> uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.LicenceStatus.VARIATION_APPROVED
    else -> error("No matching licence status found")
  }
