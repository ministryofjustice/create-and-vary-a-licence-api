package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalConditionUploadSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.StandardCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AppointmentTimeType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AuditEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType

fun List<Licence>.transformToSarLicence(): List<SarLicence> = map(::transformToSarLicence)

fun transformToSarLicence(licence: Licence) = SarLicence(
  id = licence.id,
  kind = licence.kind,
  typeCode = licence.typeCode.toSarLicenceType(),
  statusCode = licence.statusCode?.toSarLicenceStatus(),
  nomsId = licence.nomsId,
  dateLastUpdated = licence.dateLastUpdated,
  bookingId = licence.bookingId,
  appointmentPerson = licence.appointmentPerson,
  appointmentTime = licence.appointmentTime,
  appointmentTimeType = licence.appointmentTimeType?.toSarAppointmentTimeType(),
  appointmentAddress = licence.appointmentAddress,
  appointmentContact = licence.appointmentContact,
  approvedDate = licence.approvedDate,
  approvedByUsername = licence.approvedByUsername,
  submittedDate = licence.submittedDate,
  approvedByName = licence.approvedByName,
  supersededDate = licence.supersededDate,
  dateCreated = licence.dateCreated,
  createdByUsername = licence.createdByUsername,
  updatedByUsername = licence.updatedByUsername,
  standardLicenceConditions = licence.standardLicenceConditions?.transformToSarStandardConditions(),
  standardPssConditions = licence.standardPssConditions?.transformToSarStandardConditions(),
  additionalLicenceConditions = licence.additionalLicenceConditions.transformToSarAdditionalConditions(),
  additionalPssConditions = licence.additionalPssConditions.transformToSarAdditionalConditions(),
  bespokeConditions = licence.bespokeConditions.map { it.text.toString() },
  createdByFullName = licence.createdByFullName,
  licenceVersion = licence.licenceVersion,
)

fun LicenceType.toSarLicenceType(): SarLicenceType = when (this) {
  LicenceType.AP -> SarLicenceType.AP
  LicenceType.AP_PSS -> SarLicenceType.AP_PSS
  LicenceType.PSS -> SarLicenceType.PSS
}

fun LicenceStatus.toSarLicenceStatus(): SarLicenceStatus = when (this) {
  LicenceStatus.IN_PROGRESS -> SarLicenceStatus.IN_PROGRESS
  LicenceStatus.SUBMITTED -> SarLicenceStatus.SUBMITTED
  LicenceStatus.APPROVED -> SarLicenceStatus.APPROVED
  LicenceStatus.ACTIVE -> SarLicenceStatus.ACTIVE
  LicenceStatus.REJECTED -> SarLicenceStatus.REJECTED
  LicenceStatus.INACTIVE -> SarLicenceStatus.INACTIVE
  LicenceStatus.RECALLED -> SarLicenceStatus.RECALLED
  LicenceStatus.VARIATION_IN_PROGRESS -> SarLicenceStatus.VARIATION_IN_PROGRESS
  LicenceStatus.VARIATION_SUBMITTED -> SarLicenceStatus.VARIATION_SUBMITTED
  LicenceStatus.VARIATION_REJECTED -> SarLicenceStatus.VARIATION_REJECTED
  LicenceStatus.VARIATION_APPROVED -> SarLicenceStatus.VARIATION_APPROVED
  LicenceStatus.NOT_STARTED -> SarLicenceStatus.NOT_STARTED
  LicenceStatus.TIMED_OUT -> SarLicenceStatus.TIMED_OUT
}

fun AppointmentTimeType.toSarAppointmentTimeType(): SarAppointmentTimeType = when (this) {
  AppointmentTimeType.IMMEDIATE_UPON_RELEASE -> SarAppointmentTimeType.IMMEDIATE_UPON_RELEASE
  AppointmentTimeType.NEXT_WORKING_DAY_2PM -> SarAppointmentTimeType.NEXT_WORKING_DAY_2PM
  AppointmentTimeType.SPECIFIC_DATE_TIME -> SarAppointmentTimeType.SPECIFIC_DATE_TIME
}

fun List<AdditionalCondition>.transformToSarAdditionalConditions(): List<SarAdditionalCondition> = map(::transformToSarAdditionalConditions)

fun transformToSarAdditionalConditions(entity: AdditionalCondition): SarAdditionalCondition = SarAdditionalCondition(
  code = entity.code,
  version = entity.version,
  category = entity.category,
  text = entity.expandedText,
  uploadSummary = entity.uploadSummary.transformToSarAdditionalConditionUploadSummary(),
)

fun List<AdditionalConditionUploadSummary>.transformToSarAdditionalConditionUploadSummary(): List<SarAdditionalConditionUploadSummary> = map(::transformToSarAdditionalConditionUploadSummary)

fun transformToSarAdditionalConditionUploadSummary(entity: AdditionalConditionUploadSummary): SarAdditionalConditionUploadSummary = SarAdditionalConditionUploadSummary(
  filename = entity.filename,
  fileType = entity.fileType,
  fileSize = entity.fileSize,
  uploadedTime = entity.uploadedTime,
  description = entity.description,
)

fun List<StandardCondition>.transformToSarStandardConditions(): List<SarStandardCondition> = map(::transformToSarStandardConditions)

fun transformToSarStandardConditions(entity: StandardCondition): SarStandardCondition = SarStandardCondition(
  code = entity.code,
  text = entity.text,
)

fun List<AuditEvent>.transformToSarAuditEvents(): List<SarAuditEvent> = map(::transformToSarAuditEvents)

fun transformToSarAuditEvents(entity: AuditEvent): SarAuditEvent = SarAuditEvent(
  licenceId = entity.licenceId,
  eventTime = entity.eventTime,
  username = entity.username,
  fullName = entity.fullName,
  eventType = entity.eventType.toSarAuditEventType(),
  summary = entity.summary,
  detail = entity.detail,
)

fun AuditEventType.toSarAuditEventType(): SarAuditEventType = when (this) {
  AuditEventType.USER_EVENT -> SarAuditEventType.USER_EVENT
  AuditEventType.SYSTEM_EVENT -> SarAuditEventType.SYSTEM_EVENT
}
