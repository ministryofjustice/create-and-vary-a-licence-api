package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.publicApi

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalConditionUploadSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest.SarAppointmentTimeType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest.SarLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest.SarLicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest.SarLicenceType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createCrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.toCrd
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AppointmentTimeType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType.AP
import java.time.LocalDate
import java.time.LocalDateTime

class SubjectAccessRequestResponseBuilderTest {

  @Test
  fun `should add licence and build SarContent correctly`() {
    val result = SubjectAccessRequestResponseBuilder("https://some-host")
      .addLicence(crdLicence)
      .build(emptyList())

    assertThat(result.content.licences).hasSize(1)
    assertThat(result.content.auditEvents).isEmpty()
    assertThat(result.attachments).isEmpty()

    with(result.content.licences.first()) {
      assertThat(id).isEqualTo(crdLicence.id)
      assertThat(kind).isEqualTo(crdLicence.kind)
      assertThat(typeCode).isEqualTo(SarLicenceType.AP)
      assertThat(statusCode).isEqualTo(SarLicenceStatus.valueOf(crdLicence.statusCode!!.name))
      assertThat(prisonNumber).isEqualTo(crdLicence.nomsId)
      assertThat(bookingId).isEqualTo(crdLicence.bookingId)
      assertThat(appointmentPerson).isEqualTo(crdLicence.appointmentPerson)
      assertThat(appointmentTime).isEqualTo(crdLicence.appointmentTime)
      assertThat(appointmentTimeType).isEqualTo(SarAppointmentTimeType.valueOf(crdLicence.appointmentTimeType!!.name))
      assertThat(appointmentAddress).isEqualTo(crdLicence.appointmentAddress)
      assertThat(appointmentContact).isEqualTo(crdLicence.appointmentContact)
      assertThat(approvedDate).isEqualTo(crdLicence.approvedDate)
      assertThat(approvedByUsername).isEqualTo(crdLicence.approvedByUsername)
      assertThat(submittedDate).isEqualTo(crdLicence.submittedDate)
      assertThat(approvedByName).isEqualTo(crdLicence.approvedByName)
      assertThat(supersededDate).isEqualTo(crdLicence.supersededDate)
      assertThat(dateCreated).isEqualTo(crdLicence.dateCreated)
      assertThat(createdByUsername).isEqualTo(crdLicence.createdByUsername)
      assertThat(dateLastUpdated).isEqualTo(crdLicence.dateLastUpdated)
      assertThat(updatedByUsername).isEqualTo(crdLicence.updatedByUsername)
      assertThat(standardLicenceConditions).hasSize(3)
      assertThat(standardPssConditions).isEmpty()
      assertThat(additionalLicenceConditions).isEmpty()
      assertThat(additionalPssConditions).isEmpty()
      assertThat(bespokeConditions).isEmpty()
      assertThat(createdByFullName).isEqualTo(crdLicence.createdByFullName)
      assertThat(licenceVersion).isEqualTo(crdLicence.licenceVersion)
    }
  }

  @Test
  fun `attachments are built correctly`() {
    val result = SubjectAccessRequestResponseBuilder("https://some-host")
      .addLicence(
        crdLicence.copy(
          additionalLicenceConditions = listOf(
            AdditionalCondition(
              id = 10L,
              code = "AC1",
              version = "1.0",
              category = "Category A",
              expandedText = "Condition text",
              readyToSubmit = true,
              requiresInput = true,
              uploadSummary = listOf(
                AdditionalConditionUploadSummary(
                  id = 1L,
                  filename = "file1.pdf",
                  fileType = "application/pdf",
                  imageType = "image/png",
                  fileSize = 1024,
                  imageSize = 2134,
                  uploadedTime = LocalDateTime.now(),
                  description = "Document 1",
                  thumbnailImage = "asasa",
                  uploadDetailId = 2L,
                ),
                AdditionalConditionUploadSummary(
                  id = 2L,
                  filename = "file2.pdf",
                  fileType = "application/pdf",
                  imageType = "image/jpeg",
                  fileSize = 1025,
                  imageSize = 2135,
                  uploadedTime = LocalDateTime.now(),
                  description = "Document 2",
                  thumbnailImage = "asasa",
                  uploadDetailId = 3L,
                ),
              ),
            ),
            AdditionalCondition(
              id = 11L,
              code = "AC1",
              version = "1.0",
              category = "Category B",
              expandedText = "Condition text - B ",
              readyToSubmit = true,
              requiresInput = true,
              uploadSummary = listOf(
                AdditionalConditionUploadSummary(
                  id = 3L,
                  filename = "file3.pdf",
                  fileType = "application/pdf",
                  imageType = "image/png",
                  fileSize = 1026,
                  imageSize = 2137,
                  uploadedTime = LocalDateTime.now(),
                  description = "Document 3",
                  thumbnailImage = "asasa",
                  uploadDetailId = 3L,
                ),
              ),
            ),
          ),
        ),
      )
      .build(emptyList())

    assertThat(result.content.licences).hasSize(1)
    assertThat(result.attachments).hasSize(3)

    with(result.attachments[0]) {
      assertThat(attachmentNumber).isEqualTo(0)
      assertThat(name).isEqualTo("Document 1")
      assertThat(contentType).isEqualTo("image/png")
      assertThat(url).isEqualTo("https://some-host/public/licences/1/conditions/10/image-upload")
      assertThat(filename).isEqualTo("file1.pdf")
      assertThat(filesize).isEqualTo(2134)

      val summary = result.content.licences.findAttachmentSummary(attachmentNumber)
      assertThat(summary.filename).isEqualTo(filename)
      assertThat(summary.imageType).isEqualTo(contentType)
      assertThat(summary.fileSize).isEqualTo(filesize)
      assertThat(summary.description).isEqualTo(name)
    }

    with(result.attachments[1]) {
      assertThat(attachmentNumber).isEqualTo(1)
      assertThat(name).isEqualTo("Document 2")
      assertThat(contentType).isEqualTo("image/jpeg")
      assertThat(url).isEqualTo("https://some-host/public/licences/1/conditions/10/image-upload")
      assertThat(filename).isEqualTo("file2.pdf")
      assertThat(filesize).isEqualTo(2135)

      val summary = result.content.licences.findAttachmentSummary(attachmentNumber)
      assertThat(summary.filename).isEqualTo(filename)
      assertThat(summary.imageType).isEqualTo(contentType)
      assertThat(summary.fileSize).isEqualTo(filesize)
      assertThat(summary.description).isEqualTo(name)
    }
    with(result.attachments[2]) {
      assertThat(attachmentNumber).isEqualTo(2)
      assertThat(name).isEqualTo("Document 3")
      assertThat(contentType).isEqualTo("image/png")
      assertThat(url).isEqualTo("https://some-host/public/licences/1/conditions/11/image-upload")
      assertThat(filename).isEqualTo("file3.pdf")
      assertThat(filesize).isEqualTo(2137)

      val summary = result.content.licences.findAttachmentSummary(attachmentNumber)
      assertThat(summary.filename).isEqualTo(filename)
      assertThat(summary.imageType).isEqualTo(contentType)
      assertThat(summary.fileSize).isEqualTo(filesize)
      assertThat(summary.description).isEqualTo(name)
    }
  }

  private fun List<SarLicence>.findAttachmentSummary(attachmentNumber: Int) = flatMap { licences ->
    licences.additionalLicenceConditions.flatMap { conditions -> conditions.uploadSummary }
  }.find { it.attachmentNumber == attachmentNumber }!!

  companion object {
    private val crdLicence = toCrd(
      licence = createCrdLicence().copy(
        version = "2.1",
        typeCode = AP,
        appointmentTimeType = AppointmentTimeType.SPECIFIC_DATE_TIME,
        appointmentAddress = "Test Address String",
      ),
      earliestReleaseDate = LocalDate.of(2024, 1, 3),
      isEligibleForEarlyRelease = true,
      hardStopDate = LocalDate.of(2024, 1, 1),
      hardStopWarningDate = LocalDate.of(2023, 12, 28),
      isInHardStopPeriod = true,
      isDueForEarlyRelease = true,
      isDueToBeReleasedInTheNextTwoWorkingDays = true,
      conditionPolicyData = emptyMap(),
    )
  }
}
