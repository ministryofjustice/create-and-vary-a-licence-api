package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CreateLicenceRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalCondition as EntityAdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionData as EntityAdditionalConditionData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.BespokeCondition as EntityBespokeCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence as EntityLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.StandardCondition as EntityStandardCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.TestData as EntityTestData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalCondition as ModelAdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalConditionData as ModelAdditionalConditionData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.BespokeCondition as ModelBespokeCondition
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

fun transformToLicenceSummary(licence: EntityLicence): LicenceSummary {
  return LicenceSummary(
    licenceId = licence.id,
    licenceType = licence.typeCode,
    licenceStatus = licence.statusCode,
    nomisId = licence.nomsId,
    surname = licence.surname,
    forename = licence.forename,
    crn = licence.crn,
    dateOfBirth = licence.dateOfBirth,
    prisonCode = licence.prisonCode,
    prisonDescription = licence.prisonDescription,
    conditionalReleaseDate = licence.conditionalReleaseDate,
    actualReleaseDate = licence.actualReleaseDate
  )
}

fun transformToListOfSummaries(licences: List<EntityLicence>): List<LicenceSummary> {
  return licences.map { licence -> transformToLicenceSummary(licence) }
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
    additionalConditions = licence.additionalConditions.transformToModelAdditional(),
    bespokeConditions = licence.bespokeConditions.transformToModelBespoke(),
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

// Transform a list of entity standard conditions to model standard conditions
fun List<EntityStandardCondition>.transformToModelStandard(): List<ModelStandardCondition> = map(::transform)

fun transform(entity: EntityStandardCondition): ModelStandardCondition {
  return ModelStandardCondition(
    id = entity.id,
    code = entity.conditionCode,
    sequence = entity.conditionSequence,
    text = entity.conditionText
  )
}

fun transform(model: ModelAdditionalCondition, licence: EntityLicence): EntityAdditionalCondition {
  return EntityAdditionalCondition(
    conditionCode = model.code,
    conditionCategory = model.category,
    conditionSequence = model.sequence,
    conditionText = model.text,
    licence = licence,
  )
}

// Transform a list of entity additional conditions to model additional conditions
fun List<EntityAdditionalCondition>.transformToModelAdditional(): List<ModelAdditionalCondition> = map(::transform)

// Transform a list of model additional conditions to entity additional conditions
fun List<ModelAdditionalCondition>.transformToEntityAdditional(licence: EntityLicence): List<EntityAdditionalCondition> = map { transform(it, licence) }

fun transform(entity: EntityAdditionalCondition): ModelAdditionalCondition {
  return ModelAdditionalCondition(
    id = entity.id,
    code = entity.conditionCode,
    category = entity.conditionCategory,
    sequence = entity.conditionSequence,
    text = entity.conditionText,
    data = entity.additionalConditionData.transformToModelAdditionalData(),
  )
}

// Transform a list of entity additional condition data to model additional condition data
fun List<EntityAdditionalConditionData>.transformToModelAdditionalData(): List<ModelAdditionalConditionData> = map(::transform)

fun transform(entity: EntityAdditionalConditionData): ModelAdditionalConditionData {
  return ModelAdditionalConditionData(
    id = entity.id,
    sequence = entity.dataSequence,
    description = entity.dataDescription,
    format = entity.dataFormat,
    value = entity.dataValue,
  )
}

// Transform a list of entity bespoke conditions to model bespoke conditions
fun List<EntityBespokeCondition>.transformToModelBespoke(): List<ModelBespokeCondition> = map(::transform)

fun transform(entity: EntityBespokeCondition): ModelBespokeCondition {
  return ModelBespokeCondition(
    id = entity.id,
    sequence = entity.conditionSequence,
    text = entity.conditionText,
  )
}
