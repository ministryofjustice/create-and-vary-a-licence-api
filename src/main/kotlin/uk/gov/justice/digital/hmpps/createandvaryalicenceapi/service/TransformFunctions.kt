package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalTerm
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalTermData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.BespokeTerm
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.StandardTerm
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalConditionData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.BespokeCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence as EntityLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CreateLicenceRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CreateLicenceResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.StandardCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.TestData as EntityTestData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.TestData as ModelTestData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Licence as ModelLicence

import java.time.LocalDateTime

/*
** Functions which transform JPA entity objects into their API model equivalents and vice-versa.
** Mostly pass-thru but some translations, so useful to keep the database objects separate from API objects.
*/

fun transform(testData: EntityTestData): ModelTestData {
  return ModelTestData(
    key = testData.key,
    value = testData.value
  )
}

fun transformToCreateResponse(licence: EntityLicence): CreateLicenceResponse {
  return CreateLicenceResponse(licence.id, licence.typeCode, licence.statusCode)
}

fun transform(createRequest: CreateLicenceRequest): EntityLicence {
  return EntityLicence(
    typeCode = createRequest.typeCode,
    version = createRequest.version,
    statusCode = LicenceStatus.IN_PROGRESS,
    nomsId = createRequest.nomsId,
    bookingNo = createRequest.bookingNo,
    bookingId = createRequest.bookingId,
    crn = createRequest.crn,
    pnc = createRequest.pnc,
    cro = createRequest.cro,
    prisonCode = createRequest.prisonCode,
    prisonDescription = createRequest.prisonDescription,
    prisonTelephone = createRequest.prisonTelephone,
    forename = createRequest.forename,
    middleNames = createRequest.middleNames,
    surname = createRequest.surname,
    dateOfBirth = createRequest.dateOfBirth,
    conditionalReleaseDate = createRequest.conditionalReleaseDate,
    actualReleaseDate = createRequest.actualReleaseDate,
    sentenceStartDate = createRequest.sentenceStartDate,
    sentenceEndDate = createRequest.sentenceEndDate,
    licenceStartDate = createRequest.licenceStartDate,
    licenceExpiryDate = createRequest.licenceExpiryDate,
    comFirstName = createRequest.comFirstName,
    comLastName = createRequest.comLastName,
    comUsername = createRequest.comUsername,
    comStaffId = createRequest.comStaffId,
    comEmail = createRequest.comEmail,
    comTelephone = createRequest.comTelephone,
    probationAreaCode = createRequest.probationAreaCode,
    probationLduCode = createRequest.probationLduCode,
    dateCreated = LocalDateTime.now(),
    createByUsername = createRequest.comUsername,
  )
}

fun transform(licence: EntityLicence) : ModelLicence {
  return ModelLicence (
    id = licence.id,
    typeCode = licence.typeCode,
    version = licence.version ,
    statusCode = licence.statusCode,
    nomsId = licence.nomsId,
    bookingNo = licence.bookingNo,
    bookingId = licence.bookingId,
    crn = licence.crn,
    pnc = licence.pnc,
    cro = licence.cro,
    prisonCode = licence.prisonCode,
    prisonDescription = licence.prisonDescription,
    prisonTelephone = licence.prisonTelephone,
    forename = licence.forename,
    middleNames = licence.middleNames,
    surname = licence.surname,
    dateOfBirth = licence.dateOfBirth,
    conditionalReleaseDate = licence.conditionalReleaseDate,
    actualReleaseDate = licence.actualReleaseDate,
    sentenceStartDate = licence.sentenceStartDate,
    sentenceEndDate = licence.sentenceEndDate,
    licenceStartDate = licence.licenceStartDate,
    licenceExpiryDate = licence.licenceExpiryDate,
    comFirstName = licence.comFirstName,
    comLastName = licence.comLastName,
    comUsername = licence.comUsername,
    comStaffId = licence.comStaffId,
    comEmail = licence.comEmail,
    comTelephone = licence.comTelephone,
    probationAreaCode = licence.probationAreaCode,
    probationLduCode = licence.probationLduCode,
    appointmentPerson = licence.appointmentPerson,
    appointmentDate = licence.appointmentDate,
    appointmentTime = licence.appointmentTime,
    appointmentAddress = licence.appointmentAddress,
    approvedDate = licence.approvedDate,
    approvedByUsername = licence.approvedByUsername,
    supersededDate = licence.supersededDate,
    dateCreated = licence.dateCreated,
    createByUsername = licence.createByUsername,
    dateLastUpdated = licence.dateLastUpdated,
    updatedByUsername = licence.updatedByUsername,
    standardConditions = licence.standardTerms.transformStandard(),
    additionalConditions = licence.additionalTerms.transformAdditional(),
    bespokeConditions = licence.bespokeTerms.transformBespoke(),
  )
}

fun List<StandardTerm>.transformStandard(): List<StandardCondition> = map(::transform)

fun transform(entity: StandardTerm): StandardCondition {
  return StandardCondition(
    id = entity.id,
    code = entity.termCode,
    sequence = entity.termSequence,
    text = entity.termText
  )
}

fun List<BespokeTerm>.transformBespoke(): List<BespokeCondition> = map(::transform)

fun transform(entity: BespokeTerm): BespokeCondition {
  return BespokeCondition(
    id = entity.id,
    sequence = entity.termSequence,
    text = entity.termText,
  )
}

fun List<AdditionalTerm>.transformAdditional(): List<AdditionalCondition> = map(::transform)

fun transform(entity: AdditionalTerm): AdditionalCondition {
 return AdditionalCondition(
   id =  entity.id,
   code = entity.termCode,
   sequence = entity.termSequence,
   text = entity.termText,
   data = entity.additionalTermData.transformAdditionalData(),
 )
}

fun List<AdditionalTermData>.transformAdditionalData(): List<AdditionalConditionData> = map(::transform)

fun transform(entity: AdditionalTermData): AdditionalConditionData {
  return AdditionalConditionData(
    id = entity.id,
    sequence = entity.dataSequence,
    description = entity.dataDescription,
    format = entity.dataFormat,
    value = entity.dataValue,
  )
}
