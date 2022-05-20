package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.CreateLicenceRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.util.Base64
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalCondition as EntityAdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionData as EntityAdditionalConditionData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionUploadSummary as EntityAdditionalConditionUploadSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AuditEvent as EntityAuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.BespokeCondition as EntityBespokeCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence as EntityLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.LicenceEvent as EntityLicenceEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.StandardCondition as EntityStandardCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalCondition as ModelAdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalConditionData as ModelAdditionalConditionData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalConditionUploadSummary as ModelAdditionalConditionUploadSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AuditEvent as ModelAuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.BespokeCondition as ModelBespokeCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Licence as ModelLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceEvent as ModelLicenceEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.StandardCondition as ModelStandardCondition

/*
** Functions which transform JPA entity objects into their API model equivalents and vice-versa.
** Mostly pass-thru but some translations, so useful to keep the database objects separate from API objects.
*/

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
    probationAreaCode = licence.probationAreaCode,
    probationAreaDescription = licence.probationAreaDescription,
    probationPduCode = licence.probationPduCode,
    probationPduDescription = licence.probationPduDescription,
    probationLauCode = licence.probationLauCode,
    probationLauDescription = licence.probationLauDescription,
    probationTeamCode = licence.probationTeamCode,
    probationTeamDescription = licence.probationTeamDescription,
    conditionalReleaseDate = licence.conditionalReleaseDate,
    actualReleaseDate = licence.actualReleaseDate,
    comUsername = licence.responsibleCom!!.username,
    bookingId = licence.bookingId
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
    topupSupervisionStartDate = licence.topupSupervisionStartDate,
    topupSupervisionExpiryDate = licence.topupSupervisionExpiryDate,
    comUsername = licence.responsibleCom!!.username,
    comStaffId = licence.responsibleCom!!.staffIdentifier,
    comEmail = licence.responsibleCom!!.email,
    probationAreaCode = licence.probationAreaCode,
    probationAreaDescription = licence.probationAreaDescription,
    probationPduCode = licence.probationPduCode,
    probationPduDescription = licence.probationPduDescription,
    probationLauCode = licence.probationLauCode,
    probationLauDescription = licence.probationLauDescription,
    probationTeamCode = licence.probationTeamCode,
    probationTeamDescription = licence.probationTeamDescription,
    appointmentPerson = licence.appointmentPerson,
    appointmentTime = licence.appointmentTime,
    appointmentAddress = licence.appointmentAddress,
    appointmentContact = licence.appointmentContact,
    spoDiscussion = licence.spoDiscussion,
    vloDiscussion = licence.vloDiscussion,
    approvedDate = licence.approvedDate,
    approvedByUsername = licence.approvedByUsername,
    approvedByName = licence.approvedByName,
    supersededDate = licence.supersededDate,
    dateCreated = licence.dateCreated,
    createdByUsername = licence.createdBy!!.username,
    dateLastUpdated = licence.dateLastUpdated,
    updatedByUsername = licence.updatedByUsername,
    standardLicenceConditions = licence.standardConditions.transformToModelStandard("AP"),
    standardPssConditions = licence.standardConditions.transformToModelStandard("PSS"),
    additionalLicenceConditions = licence.additionalConditions.transformToModelAdditional("AP"),
    additionalPssConditions = licence.additionalConditions.transformToModelAdditional("PSS"),
    bespokeConditions = licence.bespokeConditions.transformToModelBespoke(),
    isVariation = licence.variationOfId != null,
    variationOf = licence.variationOfId,
    createdByFullName = "${licence.createdBy?.firstName} ${licence.createdBy?.lastName}",
  )
}

// Transform a list of model standard conditions to a list of entity StandardConditions, setting the licenceId
fun List<ModelStandardCondition>.transformToEntityStandard(licence: EntityLicence, conditionType: String): List<EntityStandardCondition> = map { term -> transform(term, licence, conditionType) }

fun transform(model: ModelStandardCondition, licence: EntityLicence, conditionType: String): EntityStandardCondition {
  return EntityStandardCondition(
    licence = licence,
    conditionCode = model.code,
    conditionSequence = model.sequence,
    conditionText = model.text,
    conditionType = conditionType,
  )
}

// Transform a list of entity standard conditions to model standard conditions
fun List<EntityStandardCondition>.transformToModelStandard(conditionType: String): List<ModelStandardCondition> = filter { condition -> condition.conditionType == conditionType }.map(::transform)

fun transform(entity: EntityStandardCondition): ModelStandardCondition {
  return ModelStandardCondition(
    id = entity.id,
    code = entity.conditionCode,
    sequence = entity.conditionSequence,
    text = entity.conditionText
  )
}

fun transform(model: ModelAdditionalCondition, licence: EntityLicence, conditionType: String): EntityAdditionalCondition {
  return EntityAdditionalCondition(
    conditionCode = model.code,
    conditionCategory = model.category,
    conditionSequence = model.sequence,
    conditionText = model.text,
    conditionType = conditionType,
    licence = licence,
  )
}

// Transform a list of entity additional conditions to model additional conditions
fun List<EntityAdditionalCondition>.transformToModelAdditional(conditionType: String): List<ModelAdditionalCondition> = filter { condition -> condition.conditionType == conditionType }.map(::transform)

// Transform a list of model additional conditions to entity additional conditions
fun List<ModelAdditionalCondition>.transformToEntityAdditional(licence: EntityLicence, conditionType: String): List<EntityAdditionalCondition> = map { transform(it, licence, conditionType) }

fun transform(entity: EntityAdditionalCondition): ModelAdditionalCondition {
  return ModelAdditionalCondition(
    id = entity.id,
    code = entity.conditionCode,
    category = entity.conditionCategory,
    sequence = entity.conditionSequence,
    text = entity.conditionText,
    expandedText = entity.expandedConditionText,
    data = entity.additionalConditionData.transformToModelAdditionalData(),
    uploadSummary = entity.additionalConditionUploadSummary.transformToModelAdditionalConditionUploadSummary(),
  )
}

// Transform a list of entity additional condition data to model additional condition data
fun List<EntityAdditionalConditionData>.transformToModelAdditionalData(): List<ModelAdditionalConditionData> = map(::transform)

fun transform(entity: EntityAdditionalConditionData): ModelAdditionalConditionData {
  return ModelAdditionalConditionData(
    id = entity.id,
    sequence = entity.dataSequence,
    field = entity.dataField,
    value = entity.dataValue,
  )
}

// Transform a list of model additional condition data to entity additional condition data
fun List<ModelAdditionalConditionData>.transformToEntityAdditionalData(additionalCondition: EntityAdditionalCondition): List<EntityAdditionalConditionData> = map { transform(it, additionalCondition) }

fun transform(model: ModelAdditionalConditionData, additionalCondition: EntityAdditionalCondition): EntityAdditionalConditionData {
  return EntityAdditionalConditionData(
    dataSequence = model.sequence,
    dataField = model.field,
    dataValue = model.value,
    additionalCondition = additionalCondition
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

// Transform a list of entity additional condition uploads to model additional condition uploads
fun List<EntityAdditionalConditionUploadSummary>.transformToModelAdditionalConditionUploadSummary(): List<ModelAdditionalConditionUploadSummary> = map(::transform)

fun transform(entity: EntityAdditionalConditionUploadSummary): ModelAdditionalConditionUploadSummary {
  return ModelAdditionalConditionUploadSummary(
    id = entity.id,
    filename = entity.filename,
    fileType = entity.fileType,
    fileSize = entity.fileSize,
    uploadedTime = entity.uploadedTime,
    description = entity.description,
    thumbnailImage = entity.thumbnailImage?.toBase64(),
    uploadDetailId = entity.uploadDetailId,
  )
}

fun ByteArray.toBase64(): String = String(Base64.getEncoder().encode(this))

fun List<EntityAuditEvent>.transformToModelAuditEvents(): List<ModelAuditEvent> = map(::transform)

fun transform(entity: EntityAuditEvent): ModelAuditEvent {
  return ModelAuditEvent(
    id = entity.id,
    licenceId = entity.licenceId,
    eventTime = entity.eventTime,
    username = entity.username,
    fullName = entity.fullName,
    eventType = entity.eventType,
    summary = entity.summary,
    detail = entity.detail,
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

fun List<EntityLicenceEvent>.transformToModelEvents(): List<ModelLicenceEvent> = map(::transform)

fun transform(entity: EntityLicenceEvent): ModelLicenceEvent {
  return ModelLicenceEvent(
    id = entity.id,
    licenceId = entity.licenceId,
    eventType = entity.eventType,
    username = entity.username,
    forenames = entity.forenames,
    surname = entity.surname,
    eventDescription = entity.eventDescription,
    eventTime = entity.eventTime,
  )
}
