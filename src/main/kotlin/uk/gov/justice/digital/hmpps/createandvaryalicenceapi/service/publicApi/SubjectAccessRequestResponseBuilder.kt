package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.publicApi

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalConditionUploadSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.StandardCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest.Content
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest.SarAdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest.SarAdditionalConditionUploadSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest.SarAppointmentTimeType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest.SarAttachmentDetail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest.SarAuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest.SarAuditEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest.SarContent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest.SarLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest.SarLicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest.SarLicenceType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest.SarStandardCondition
import java.util.concurrent.atomic.AtomicInteger

private const val UNAVAILABLE = "unavailable"

class SubjectAccessRequestResponseBuilder(val baseUrl: String) {
  private val attachmentIdSeq = AtomicInteger()
  private val sarLicences: MutableList<SarLicence> = mutableListOf()
  private val attachmentDetail: MutableList<SarAttachmentDetail> = mutableListOf()

  fun addLicence(licence: Licence): SubjectAccessRequestResponseBuilder {
    sarLicences.add(
      SarLicence(
        id = licence.id,
        kind = licence.kind,
        typeCode = SarLicenceType.from(licence.typeCode),
        statusCode = SarLicenceStatus.from(licence.statusCode!!),
        prisonNumber = licence.nomsId,
        dateLastUpdated = licence.dateLastUpdated,
        bookingId = licence.bookingId,
        appointmentPerson = licence.appointmentPerson,
        appointmentTime = licence.appointmentTime,
        appointmentTimeType = SarAppointmentTimeType.from(licence.appointmentTimeType),
        appointmentAddress = licence.licenceAppointmentAddress?.toString() ?: licence.appointmentAddress,
        appointmentContact = licence.appointmentContact,
        approvedDate = licence.approvedDate,
        approvedByUsername = licence.approvedByUsername,
        submittedDate = licence.submittedDate,
        approvedByName = licence.approvedByName,
        supersededDate = licence.supersededDate,
        dateCreated = licence.dateCreated,
        createdByUsername = licence.createdByUsername,
        updatedByUsername = licence.updatedByUsername,
        standardLicenceConditions = licence.standardLicenceConditions?.map(::transformToSarStandardConditions),
        standardPssConditions = licence.standardPssConditions?.map(::transformToSarStandardConditions),
        additionalLicenceConditions = licence.additionalLicenceConditions.map {
          transformToSarAdditionalConditions(
            licence,
            it,
          )
        },
        additionalPssConditions = licence.additionalPssConditions.map {
          transformToSarAdditionalConditions(
            licence,
            it,
          )
        },
        bespokeConditions = licence.bespokeConditions.map { it.text.toString() },
        createdByFullName = licence.createdByFullName,
        licenceVersion = licence.licenceVersion,
      ),
    )
    return this
  }

  fun build(auditEvents: List<AuditEvent>) = SarContent(
    Content(
      licences = sarLicences,
      auditEvents = auditEvents.map { toSarAuditEvent(it) }.sortedBy { it.eventTime },
    ),
    attachments = attachmentDetail,
  )

  private fun transformToSarAdditionalConditions(licence: Licence, entity: AdditionalCondition) = SarAdditionalCondition(
    code = entity.code,
    version = entity.version,
    category = entity.category,
    text = entity.expandedText,
    uploadSummary = entity.uploadSummary.map {
      transformToSarAdditionalConditionUploadSummary(
        licence.id,
        entity.id,
        it,
      )
    },
  )

  private fun transformToSarAdditionalConditionUploadSummary(
    licenceId: Long?,
    conditionId: Long?,
    entity: AdditionalConditionUploadSummary,
  ): SarAdditionalConditionUploadSummary {
    val attachmentNumber = attachmentIdSeq.andIncrement

    this.attachmentDetail.add(buildAttachmentDetail(attachmentNumber, entity, licenceId, conditionId))

    return SarAdditionalConditionUploadSummary(
      attachmentNumber = attachmentNumber,
      filename = entity.filename ?: UNAVAILABLE,
      imageType = entity.imageType ?: UNAVAILABLE,
      fileSize = entity.fileSize,
      uploadedTime = entity.uploadedTime,
      description = entity.description,
    )
  }

  private fun buildAttachmentDetail(
    attachmentNumber: Int,
    entity: AdditionalConditionUploadSummary,
    licenceId: Long?,
    conditionId: Long?,
  ) = SarAttachmentDetail(
    attachmentNumber = attachmentNumber,
    name = entity.description ?: UNAVAILABLE,
    contentType = entity.imageType ?: UNAVAILABLE,
    url = "$baseUrl/public/licences/$licenceId/conditions/$conditionId/image-upload",
    filename = entity.filename ?: UNAVAILABLE,
    filesize = entity.fileSize,
  )

  private fun transformToSarStandardConditions(entity: StandardCondition): SarStandardCondition = SarStandardCondition(
    code = entity.code,
    text = entity.text,
  )

  private fun toSarAuditEvent(entity: AuditEvent) = SarAuditEvent(
    licenceId = entity.licenceId,
    eventTime = entity.eventTime,
    username = entity.username,
    fullName = entity.fullName,
    eventType = SarAuditEventType.from(entity.eventType),
    summary = entity.summary,
    detail = entity.detail,
  )
}
