package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.publicApi

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.timeserved.TimeServedExternalRecord
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalConditionUploadSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.StandardCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.SupportsElectronicMonitoring
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest.Content
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest.SarAdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest.SarAdditionalConditionUploadSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest.SarAppointmentTimeType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest.SarExternalRecord
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest.SarLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest.SarLicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest.SarLicenceType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest.SarStandardCondition
import uk.gov.justice.hmpps.kotlin.sar.Attachment
import uk.gov.justice.hmpps.kotlin.sar.HmppsSubjectAccessRequestContent
import java.util.concurrent.atomic.AtomicInteger

private const val UNAVAILABLE = "unavailable"

// We should be able to remove this and make everything non-nullable after migration
private const val UNAVAILABLE_SIZE = -1

class SubjectAccessRequestResponseBuilder(val baseUrl: String) {
  private val attachmentIdSeq = AtomicInteger()
  private val sarLicences: MutableList<SarLicence> = mutableListOf()
  private val attachmentDetail: MutableList<Attachment> = mutableListOf()
  private val externalRecords: MutableList<SarExternalRecord> = mutableListOf()

  fun addLicence(licence: Licence): SubjectAccessRequestResponseBuilder {
    val supportsElectronicMonitoring = licence is SupportsElectronicMonitoring
    val isToBeTaggedForProgramme =
      if (supportsElectronicMonitoring) licence.electronicMonitoringProvider?.isToBeTaggedForProgramme else null
    val programmeName = if (supportsElectronicMonitoring) licence.electronicMonitoringProvider?.programmeName else null

    sarLicences.add(
      SarLicence(
        kind = licence.kind,
        typeCode = SarLicenceType.from(licence.typeCode),
        statusCode = SarLicenceStatus.from(licence.statusCode!!),
        prisonNumber = licence.nomsId,
        dateLastUpdated = licence.dateLastUpdated,
        appointmentPerson = licence.appointmentPerson,
        appointmentTime = licence.appointmentTime,
        appointmentTimeType = SarAppointmentTimeType.from(licence.appointmentTimeType),
        appointmentAddress = licence.appointmentAddress,
        appointmentContact = licence.appointmentContact,
        appointmentTelephoneNumber = licence.appointmentTelephoneNumber,
        appointmentAlternativeTelephoneNumber = licence.appointmentAlternativeTelephoneNumber,
        approvedDate = licence.approvedDate,
        submittedDate = licence.submittedDate,
        approvedByName = licence.approvedByName,
        supersededDate = licence.supersededDate,
        dateCreated = licence.dateCreated,
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
        isToBeTaggedForProgramme = isToBeTaggedForProgramme,
        programmeName = programmeName,
      ),
    )
    return this
  }

  fun build() = HmppsSubjectAccessRequestContent(
    Content(
      licences = sarLicences,
      timeServedExternalRecords = externalRecords,
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
      uploadedTime = entity.uploadedTime,
      description = entity.description,
    )
  }

  private fun buildAttachmentDetail(
    attachmentNumber: Int,
    entity: AdditionalConditionUploadSummary,
    licenceId: Long?,
    conditionId: Long?,
  ) = Attachment(
    attachmentNumber = attachmentNumber,
    name = entity.description ?: UNAVAILABLE,
    contentType = entity.imageType ?: UNAVAILABLE,
    url = "$baseUrl/public/licences/$licenceId/conditions/$conditionId/image-upload",
    filename = entity.filename ?: UNAVAILABLE,
    filesize = entity.imageSize ?: UNAVAILABLE_SIZE,
  )

  private fun transformToSarStandardConditions(entity: StandardCondition): SarStandardCondition = SarStandardCondition(
    code = entity.code,
    text = entity.text,
  )

  fun addTimeServedExternalRecord(record: TimeServedExternalRecord): SubjectAccessRequestResponseBuilder {
    with(record) {
      externalRecords.add(
        SarExternalRecord(
          prisonNumber = nomsId,
          reason = reason,
          prisonCode = prisonCode,
          dateCreated = dateCreated,
          dateLastUpdated = dateLastUpdated,
        ),
      )
    }
    return this
  }
}
