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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest.SarAppointmentPersonType
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

/**
 * Extracts the surname from a full name in "firstname surname" format.
 * Returns the surname if the name contains at least one space, otherwise returns null.
 */
fun extractLastname(fullName: String?): String? {
  if (fullName.isNullOrBlank()) return fullName
  val trimmed = fullName.trim()
  val lastSpaceIndex = trimmed.lastIndexOf(' ')
  return if (lastSpaceIndex > 0) {
    trimmed.substring(lastSpaceIndex + 1)
  } else {
    null
  }
}

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
        licenceId = licence.id,
        typeCode = SarLicenceType.from(licence.typeCode),
        statusCode = SarLicenceStatus.from(licence.statusCode!!),
        prisonNumber = licence.nomsId,
        dateLastUpdated = licence.dateLastUpdated,
        appointmentPersonLastName = extractLastname(licence.appointmentPerson),
        appointmentPersonType = SarAppointmentPersonType.from(licence.appointmentPersonType),
        appointmentTime = licence.appointmentTime,
        appointmentTimeType = SarAppointmentTimeType.from(licence.appointmentTimeType),
        appointmentAddress = licence.appointmentAddress,
        submittedDate = licence.submittedDate,
        submittedByLastName = extractLastname(licence.submittedByFullName),
        approvedDate = licence.approvedDate,
        approvedByLastName = extractLastname(licence.approvedByName),
        supersededDate = licence.supersededDate,
        createdByLastName = extractLastname(licence.createdByFullName),
        dateCreated = licence.dateCreated,
        licenceStartDate = licence.licenceStartDate,
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
        licenceVersion = licence.licenceVersion,
        policyVersion = licence.version,
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
      fileSize = entity.fileSize,
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
