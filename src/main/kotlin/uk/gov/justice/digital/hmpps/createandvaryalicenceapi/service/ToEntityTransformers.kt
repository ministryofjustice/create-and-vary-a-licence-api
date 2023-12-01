package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalConditionRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.CreateLicenceRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalCondition as EntityAdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionData as EntityAdditionalConditionData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AuditEvent as EntityAuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence as EntityLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.StandardCondition as EntityStandardCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalConditionData as ModelAdditionalConditionData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AuditEvent as ModelAuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.StandardCondition as ModelStandardCondition

/*
** Functions which transform API models into their JPA entity objects equivalents.
** Mostly pass-thru but some translations, so useful to keep the database objects separate from API objects.
*/

fun transform(createRequest: CreateLicenceRequest): EntityLicence {
  return EntityLicence(
    kind = LicenceKind.CRD,
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
    topupSupervisionStartDate = createRequest.topupSupervisionStartDate,
    topupSupervisionExpiryDate = createRequest.topupSupervisionExpiryDate,
    probationAreaCode = createRequest.probationAreaCode,
    probationAreaDescription = createRequest.probationAreaDescription,
    probationPduCode = createRequest.probationPduCode,
    probationPduDescription = createRequest.probationPduDescription,
    probationLauCode = createRequest.probationLauCode,
    probationLauDescription = createRequest.probationLauDescription,
    probationTeamCode = createRequest.probationTeamCode,
    probationTeamDescription = createRequest.probationTeamDescription,
  )
}

// Transform a list of model standard conditions to a list of entity StandardConditions, setting the licenceId
fun List<ModelStandardCondition>.transformToEntityStandard(
  licence: EntityLicence,
  conditionType: String,
): List<EntityStandardCondition> = map { term -> transform(term, licence, conditionType) }

fun transform(model: ModelStandardCondition, licence: EntityLicence, conditionType: String): EntityStandardCondition {
  return EntityStandardCondition(
    licence = licence,
    conditionCode = model.code,
    conditionSequence = model.sequence,
    conditionText = model.text,
    conditionType = conditionType,
  )
}

fun transform(
  model: AdditionalConditionRequest,
  licence: EntityLicence,
  conditionType: String,
): EntityAdditionalCondition {
  return EntityAdditionalCondition(
    conditionVersion = licence.version!!,
    conditionCode = model.code,
    conditionCategory = model.category,
    conditionSequence = model.sequence,
    conditionText = model.text,
    conditionType = conditionType,
    licence = licence,
  )
}

// Transform a list of model additional conditions to entity additional conditions
fun List<AdditionalConditionRequest>.transformToEntityAdditional(
  licence: EntityLicence,
  conditionType: String,
): List<EntityAdditionalCondition> = map { transform(it, licence, conditionType) }

// Transform a list of model additional condition data to entity additional condition data
fun List<ModelAdditionalConditionData>.transformToEntityAdditionalData(additionalCondition: EntityAdditionalCondition): List<EntityAdditionalConditionData> =
  map { transform(it, additionalCondition) }

fun transform(
  model: ModelAdditionalConditionData,
  additionalCondition: EntityAdditionalCondition,
): EntityAdditionalConditionData {
  return EntityAdditionalConditionData(
    dataSequence = model.sequence,
    dataField = model.field,
    dataValue = model.value,
    additionalCondition = additionalCondition,
  )
}

fun transform(model: ModelAuditEvent): EntityAuditEvent {
  return EntityAuditEvent(
    licenceId = model.licenceId,
    eventTime = model.eventTime,
    username = model.username,
    fullName = model.fullName,
    eventType = model.eventType,
    summary = model.summary,
    detail = model.detail,
  )
}
