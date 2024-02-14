package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HardStopLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.VariationLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.CaseloadResult
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate
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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CrdLicence as ModelCrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.FoundProbationRecord as ModelFoundProbationRecord
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.HardStopLicence as ModelHardstopLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceEvent as ModelLicenceEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.StandardCondition as ModelStandardCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.VariationLicence as ModelVariationLicence

/*
** Functions which transform JPA entity objects into their API model equivalents.
** Mostly pass-thru but some translations, so useful to keep the database objects separate from API objects.
*/

fun transformToLicenceSummary(licence: EntityLicence): LicenceSummary {
  return LicenceSummary(
    kind = licence.kind,
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
    sentenceStartDate = licence.sentenceStartDate,
    sentenceEndDate = licence.sentenceEndDate,
    licenceStartDate = licence.licenceStartDate,
    licenceExpiryDate = licence.licenceExpiryDate,
    topupSupervisionStartDate = licence.topupSupervisionStartDate,
    topupSupervisionExpiryDate = licence.topupSupervisionExpiryDate,
    comUsername = licence.responsibleCom!!.username,
    bookingId = licence.bookingId,
    dateCreated = licence.dateCreated,
    approvedByName = licence.approvedByName,
    approvedDate = licence.approvedDate,
    submittedDate = licence.submittedDate,
    licenceVersion = licence.licenceVersion,
    versionOf = if (licence is CrdLicence) licence.versionOfId else null,
  )
}

fun transformToListOfSummaries(licences: List<EntityLicence>): List<LicenceSummary> {
  return licences.map { licence -> transformToLicenceSummary(licence) }
}

fun toHardstop(
  licence: HardStopLicence,
  earliestReleaseDate: LocalDate?,
  isEligibleForEarlyRelease: Boolean,
  isInHardStopPeriod: Boolean,
  conditionSubmissionStatus: Map<String, Boolean>,
) = ModelHardstopLicence(
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
  responsibleComFullName = with(licence.responsibleCom!!) { "$firstName $lastName" },
  updatedByFullName = "",
  probationAreaCode = licence.probationAreaCode,
  probationAreaDescription = licence.probationAreaDescription,
  probationPduCode = licence.probationPduCode,
  probationPduDescription = licence.probationPduDescription,
  probationLauCode = licence.probationLauCode,
  probationLauDescription = licence.probationLauDescription,
  probationTeamCode = licence.probationTeamCode,
  probationTeamDescription = licence.probationTeamDescription,
  appointmentPersonType = licence.appointmentPersonType,
  appointmentPerson = licence.appointmentPerson,
  appointmentTime = licence.appointmentTime,
  appointmentTimeType = licence.appointmentTimeType,
  appointmentAddress = licence.appointmentAddress,
  appointmentContact = licence.appointmentContact,
  reviewDate = licence.reviewDate,
  approvedDate = licence.approvedDate,
  approvedByUsername = licence.approvedByUsername,
  approvedByName = licence.approvedByName,
  submittedDate = licence.submittedDate,
  supersededDate = licence.supersededDate,
  dateCreated = licence.dateCreated,
  createdByUsername = licence.getCreator().username,
  dateLastUpdated = licence.dateLastUpdated,
  updatedByUsername = licence.updatedByUsername,
  standardLicenceConditions = licence.standardConditions.transformToModelStandard("AP"),
  standardPssConditions = licence.standardConditions.transformToModelStandard("PSS"),
  additionalLicenceConditions = licence.additionalConditions.transformToModelAdditional("AP", conditionSubmissionStatus),
  additionalPssConditions = licence.additionalConditions.transformToModelAdditional("PSS", conditionSubmissionStatus),
  bespokeConditions = licence.bespokeConditions.transformToModelBespoke(),
  createdByFullName = with(licence.getCreator()) { "$firstName $lastName" },
  isInPssPeriod = if (licence.typeCode === LicenceType.PSS) true else licence.isInPssPeriod(),
  isActivatedInPssPeriod = licence.isActivatedInPssPeriod(),
  licenceVersion = licence.licenceVersion,
  earliestReleaseDate = earliestReleaseDate,
  isEligibleForEarlyRelease = isEligibleForEarlyRelease,
  isInHardStopPeriod = isInHardStopPeriod,
)

fun toVariation(
  licence: VariationLicence,
  earliestReleaseDate: LocalDate?,
  isEligibleForEarlyRelease: Boolean,
  conditionSubmissionStatus: Map<String, Boolean>,
): ModelVariationLicence = ModelVariationLicence(
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
  responsibleComFullName = with(licence.responsibleCom!!) { "$firstName $lastName" },
  updatedByFullName = "",
  probationAreaCode = licence.probationAreaCode,
  probationAreaDescription = licence.probationAreaDescription,
  probationPduCode = licence.probationPduCode,
  probationPduDescription = licence.probationPduDescription,
  probationLauCode = licence.probationLauCode,
  probationLauDescription = licence.probationLauDescription,
  probationTeamCode = licence.probationTeamCode,
  probationTeamDescription = licence.probationTeamDescription,
  appointmentPersonType = licence.appointmentPersonType,
  appointmentPerson = licence.appointmentPerson,
  appointmentTime = licence.appointmentTime,
  appointmentTimeType = licence.appointmentTimeType,
  appointmentAddress = licence.appointmentAddress,
  appointmentContact = licence.appointmentContact,
  spoDiscussion = licence.spoDiscussion,
  vloDiscussion = licence.vloDiscussion,
  approvedDate = licence.approvedDate,
  approvedByUsername = licence.approvedByUsername,
  approvedByName = licence.approvedByName,
  submittedDate = licence.submittedDate,
  supersededDate = licence.supersededDate,
  dateCreated = licence.dateCreated,
  createdByUsername = licence.getCreator().username,
  dateLastUpdated = licence.dateLastUpdated,
  updatedByUsername = licence.updatedByUsername,
  standardLicenceConditions = licence.standardConditions.transformToModelStandard("AP"),
  standardPssConditions = licence.standardConditions.transformToModelStandard("PSS"),
  additionalLicenceConditions = licence.additionalConditions.transformToModelAdditional("AP", conditionSubmissionStatus),
  additionalPssConditions = licence.additionalConditions.transformToModelAdditional("PSS", conditionSubmissionStatus),
  bespokeConditions = licence.bespokeConditions.transformToModelBespoke(),
  variationOf = licence.variationOfId,
  createdByFullName = with(licence.getCreator()) { "$firstName $lastName" },
  isInPssPeriod = if (licence.typeCode === LicenceType.PSS) true else licence.isInPssPeriod(),
  isActivatedInPssPeriod = licence.isActivatedInPssPeriod(),
  licenceVersion = licence.licenceVersion,
  earliestReleaseDate = earliestReleaseDate,
  isEligibleForEarlyRelease = isEligibleForEarlyRelease,
)

fun toCrd(
  licence: CrdLicence,
  earliestReleaseDate: LocalDate?,
  isEligibleForEarlyRelease: Boolean,
  isInHardStopPeriod: Boolean,
  conditionSubmissionStatus: Map<String, Boolean>,
) = ModelCrdLicence(
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
  responsibleComFullName = with(licence.responsibleCom!!) { "$firstName $lastName" },
  updatedByFullName = "",
  probationAreaCode = licence.probationAreaCode,
  probationAreaDescription = licence.probationAreaDescription,
  probationPduCode = licence.probationPduCode,
  probationPduDescription = licence.probationPduDescription,
  probationLauCode = licence.probationLauCode,
  probationLauDescription = licence.probationLauDescription,
  probationTeamCode = licence.probationTeamCode,
  probationTeamDescription = licence.probationTeamDescription,
  appointmentPersonType = licence.appointmentPersonType,
  appointmentPerson = licence.appointmentPerson,
  appointmentTime = licence.appointmentTime,
  appointmentTimeType = licence.appointmentTimeType,
  appointmentAddress = licence.appointmentAddress,
  appointmentContact = licence.appointmentContact,
  approvedDate = licence.approvedDate,
  approvedByUsername = licence.approvedByUsername,
  approvedByName = licence.approvedByName,
  submittedDate = licence.submittedDate,
  supersededDate = licence.supersededDate,
  dateCreated = licence.dateCreated,
  createdByUsername = licence.getCreator().username,
  dateLastUpdated = licence.dateLastUpdated,
  updatedByUsername = licence.updatedByUsername,
  standardLicenceConditions = licence.standardConditions.transformToModelStandard("AP"),
  standardPssConditions = licence.standardConditions.transformToModelStandard("PSS"),
  additionalLicenceConditions = licence.additionalConditions.transformToModelAdditional("AP", conditionSubmissionStatus),
  additionalPssConditions = licence.additionalConditions.transformToModelAdditional("PSS", conditionSubmissionStatus),
  bespokeConditions = licence.bespokeConditions.transformToModelBespoke(),
  createdByFullName = with(licence.getCreator()) { "$firstName $lastName" },
  isInPssPeriod = if (licence.typeCode === LicenceType.PSS) true else licence.isInPssPeriod(),
  isActivatedInPssPeriod = licence.isActivatedInPssPeriod(),
  licenceVersion = licence.licenceVersion,
  earliestReleaseDate = earliestReleaseDate,
  isEligibleForEarlyRelease = isEligibleForEarlyRelease,
  isInHardStopPeriod = isInHardStopPeriod,
)

// Transform a list of entity standard conditions to model standard conditions
fun List<EntityStandardCondition>.transformToModelStandard(conditionType: String): List<ModelStandardCondition> =
  filter { condition -> condition.conditionType == conditionType }.map(::transform)

fun transform(entity: EntityStandardCondition): ModelStandardCondition {
  return ModelStandardCondition(
    id = entity.id,
    code = entity.conditionCode,
    sequence = entity.conditionSequence,
    text = entity.conditionText,
  )
}

// Transform a list of entity additional conditions to model additional conditions
fun List<EntityAdditionalCondition>.transformToModelAdditional(conditionType: String, conditionSubmissionStatus: Map<String, Boolean>): List<ModelAdditionalCondition> =
  filter { condition -> condition.conditionType == conditionType }.map { transform(it, conditionSubmissionStatus[it.conditionCode]!!) }

fun transform(entity: EntityAdditionalCondition, readyToSubmit: Boolean): ModelAdditionalCondition {
  return ModelAdditionalCondition(
    id = entity.id,
    code = entity.conditionCode,
    version = entity.conditionVersion,
    category = entity.conditionCategory,
    sequence = entity.conditionSequence,
    text = entity.conditionText,
    expandedText = entity.expandedConditionText,
    data = entity.additionalConditionData.transformToModelAdditionalData(),
    uploadSummary = entity.additionalConditionUploadSummary.transformToModelAdditionalConditionUploadSummary(),
    readyToSubmit = readyToSubmit,
  )
}

// Transform a list of entity additional condition data to model additional condition data
fun List<EntityAdditionalConditionData>.transformToModelAdditionalData(): List<ModelAdditionalConditionData> =
  map(::transform)

fun transform(entity: EntityAdditionalConditionData): ModelAdditionalConditionData {
  return ModelAdditionalConditionData(
    id = entity.id,
    sequence = entity.dataSequence,
    field = entity.dataField,
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

// Transform a list of entity additional condition uploads to model additional condition uploads
fun List<EntityAdditionalConditionUploadSummary>.transformToModelAdditionalConditionUploadSummary(): List<ModelAdditionalConditionUploadSummary> =
  map(::transform)

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

private fun transform(entity: EntityAuditEvent): ModelAuditEvent {
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

fun CaseloadResult.transformToModelFoundProbationRecord(licence: Licence?): ModelFoundProbationRecord {
  return ModelFoundProbationRecord(
    name = "${name.forename} ${name.surname}".convertToTitleCase(),
    crn = licence?.crn,
    nomisId = licence?.nomsId,
    comName = manager.name?.let { "${it.forename} ${it.surname}".convertToTitleCase() },
    comStaffCode = manager.code,
    teamName = manager.team.description ?: licence?.probationTeamDescription,
    releaseDate = licence?.conditionalReleaseDate ?: licence?.actualReleaseDate,
    licenceId = licence?.id,
    licenceType = licence?.typeCode,
    licenceStatus = licence?.statusCode,
    isOnProbation = licence?.statusCode?.isOnProbation(),
  )
}

fun CaseloadResult.transformToUnstartedRecord(
  releaseDate: LocalDate?,
  licenceType: LicenceType?,
  licenceStatus: LicenceStatus?,
): ModelFoundProbationRecord {
  return ModelFoundProbationRecord(
    name = "${name.forename} ${name.surname}".convertToTitleCase(),
    crn = identifiers.crn,
    nomisId = identifiers.noms,
    comName = manager.name?.let { "${it.forename} ${it.surname}".convertToTitleCase() },
    comStaffCode = manager.code,
    teamName = manager.team.description,
    releaseDate = releaseDate,
    licenceId = null,
    licenceType = licenceType,
    licenceStatus = licenceStatus,
    isOnProbation = false,
  )
}
