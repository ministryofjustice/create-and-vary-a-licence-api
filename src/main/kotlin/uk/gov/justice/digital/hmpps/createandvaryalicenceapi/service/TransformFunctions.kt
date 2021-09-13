package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalTerm
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalTermData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.BespokeTerm
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalConditionData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.BespokeCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CreateLicenceRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CreateLicenceResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence as EntityLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.StandardCondition as EntityStandardCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.TestData as EntityTestData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Licence as ModelLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.StandardCondition as ModelStandardCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.TestData as ModelTestData

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
    createdByUsername = createRequest.comUsername,
  )
}

fun transform(licence: EntityLicence): ModelLicence {
  return ModelLicence(
    id = licence.id,
    typeCode = licence.typeCode,
    version = licence.version,
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
    createdByUsername = licence.createdByUsername,
    dateLastUpdated = licence.dateLastUpdated,
    updatedByUsername = licence.updatedByUsername,
    standardConditions = licence.standardConditions.transformToModelStandard(),
    additionalConditions = licence.additionalTerms.transformToModelAdditional(),
    bespokeConditions = licence.bespokeTerms.transformToModelBespoke(),
  )
}

// Transform a list of model standard conditions to a list of entity StandardConditions, setting the licenceId
fun List<ModelStandardCondition>.transformToEntityStandard(id: Long): List<EntityStandardCondition> = map { term -> transform(term, id) }

fun transform(model: ModelStandardCondition, id: Long): EntityStandardCondition {
  return EntityStandardCondition(
    licenceId = id,
    conditionCode = model.code,
    conditionSequence = model.sequence,
    conditionText = model.text,
  )
}

// Take list of entity standard terms and transform to model standard terms
fun List<EntityStandardCondition>.transformToModelStandard(): List<ModelStandardCondition> = map(::transform)

fun transform(entity: EntityStandardCondition): ModelStandardCondition {
  return ModelStandardCondition(
    id = entity.id,
    code = entity.conditionCode,
    sequence = entity.conditionSequence,
    text = entity.conditionText
  )
}

// Take list of entity additional terms and transform to model additional terms
fun List<AdditionalTerm>.transformToModelAdditional(): List<AdditionalCondition> = map(::transform)

fun transform(entity: AdditionalTerm): AdditionalCondition {
  return AdditionalCondition(
    id = entity.id,
    code = entity.termCode,
    sequence = entity.termSequence,
    text = entity.termText,
    data = entity.additionalTermData.transformToModelAdditionalData(),
  )
}

// Take list of entity additional term data and transform to model additional term data
fun List<AdditionalTermData>.transformToModelAdditionalData(): List<AdditionalConditionData> = map(::transform)

fun transform(entity: AdditionalTermData): AdditionalConditionData {
  return AdditionalConditionData(
    id = entity.id,
    sequence = entity.dataSequence,
    description = entity.dataDescription,
    format = entity.dataFormat,
    value = entity.dataValue,
  )
}

// Take list of entity bespoke terms and transform to model bespoke terms
fun List<BespokeTerm>.transformToModelBespoke(): List<BespokeCondition> = map(::transform)

fun transform(entity: BespokeTerm): BespokeCondition {
  return BespokeCondition(
    id = entity.id,
    sequence = entity.termSequence,
    text = entity.termText,
  )
}
