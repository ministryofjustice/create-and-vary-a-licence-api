package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.publicApi

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.LicenceStatus as ModelPublicLicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.LicenceSummary as ModelPublicLicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.LicenceType as ModelPublicLicenceType

/*
** Functions which transform JPA entity objects into their public API model equivalents.
** Mostly pass-thru but some translations, so useful to keep the database objects separate from API objects.
*/

fun Licence.transformToPublicLicenceSummary(
  licenceType: ModelPublicLicenceType,
  statusCode: ModelPublicLicenceStatus,
): ModelPublicLicenceSummary {
  return ModelPublicLicenceSummary(
    id = this.id,
    licenceType = licenceType,
    policyVersion = this.licenceVersion ?: this.valueNotPresent("policyVersion"),
    version = this.version ?: this.valueNotPresent("version"),
    statusCode = statusCode,
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
