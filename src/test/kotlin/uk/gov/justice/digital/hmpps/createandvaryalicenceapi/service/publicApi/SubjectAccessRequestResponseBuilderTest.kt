package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.publicApi

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalConditionUploadSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ElectronicMonitoringProvider
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest.Content
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest.SarAppointmentPersonType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest.SarAppointmentTimeType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest.SarLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest.SarLicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest.SarLicenceType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createCrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.toCrd
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AppointmentTimeType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType.AP
import java.time.LocalDate
import java.time.LocalDateTime

class SubjectAccessRequestResponseBuilderTest {

  @Test
  fun `should add licence and build hmppsSubjectAccessRequestContent correctly`() {
    val crdLicenceWithEm = crdLicence.copy(
      electronicMonitoringProvider = ElectronicMonitoringProvider(
        isToBeTaggedForProgramme = true,
        programmeName = "a program",
      ),
    )

    val result = SubjectAccessRequestResponseBuilder("https://some-host")
      .addLicence(crdLicenceWithEm)
      .build()

    val content = result.content as Content
    val attachments = result.attachments

    assertThat(content.licences).hasSize(1)
    assertThat(attachments).isEmpty()

    with(content.licences.first()) {
      assertThat(kind).isEqualTo(crdLicence.kind)
      assertThat(typeCode).isEqualTo(SarLicenceType.AP)
      assertThat(statusCode).isEqualTo(SarLicenceStatus.valueOf(crdLicence.statusCode!!.name))
      assertThat(policyVersion).isEqualTo(crdLicence.version)
      assertThat(prisonNumber).isEqualTo(crdLicence.nomsId)
      assertThat(appointmentPersonLastName).isEqualTo(extractLastname(crdLicence.appointmentPerson))
      assertThat(appointmentPersonType).isEqualTo(SarAppointmentPersonType.from(crdLicence.appointmentPersonType))
      assertThat(appointmentTime).isEqualTo(crdLicence.appointmentTime)
      assertThat(appointmentTimeType).isEqualTo(SarAppointmentTimeType.from(crdLicence.appointmentTimeType))
      assertThat(appointmentAddress).isEqualTo(crdLicence.appointmentAddress)
      assertThat(approvedDate).isEqualTo(crdLicence.approvedDate)
      assertThat(submittedDate).isEqualTo(crdLicence.submittedDate)
      assertThat(approvedByLastName).isEqualTo(extractLastname(crdLicence.approvedByName))
      assertThat(supersededDate).isEqualTo(crdLicence.supersededDate)
      assertThat(dateCreated).isEqualTo(crdLicence.dateCreated)
      assertThat(dateLastUpdated).isEqualTo(crdLicence.dateLastUpdated)
      assertThat(standardLicenceConditions).hasSize(3)
      assertThat(standardPssConditions).isEmpty()
      assertThat(additionalLicenceConditions).isEmpty()
      assertThat(additionalPssConditions).isEmpty()
      assertThat(bespokeConditions).isEmpty()
      assertThat(createdByLastName).isEqualTo(extractLastname(crdLicence.createdByFullName))
      assertThat(licenceVersion).isEqualTo(crdLicence.licenceVersion)
      assertThat(isToBeTaggedForProgramme).isEqualTo(crdLicenceWithEm.electronicMonitoringProvider!!.isToBeTaggedForProgramme)
      assertThat(programmeName).isEqualTo(crdLicenceWithEm.electronicMonitoringProvider.programmeName)
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
                ),
              ),
            ),
          ),
        ),
      )
      .build()

    val content = result.content as Content

    with(result.attachments!![0]) {
      assertThat(attachmentNumber).isEqualTo(0)
      assertThat(name).isEqualTo("Document 1")
      assertThat(contentType).isEqualTo("image/png")
      assertThat(url).isEqualTo("https://some-host/public/licences/1/conditions/10/image-upload")
      assertThat(filename).isEqualTo("file1.pdf")
      assertThat(filesize).isEqualTo(2134)

      val summary = content.licences.findAttachmentSummary(attachmentNumber)
      assertThat(summary.filename).isEqualTo(filename)
      assertThat(summary.imageType).isEqualTo(contentType)
      assertThat(summary.description).isEqualTo(name)
    }

    with(result.attachments!![1]) {
      assertThat(attachmentNumber).isEqualTo(1)
      assertThat(name).isEqualTo("Document 2")
      assertThat(contentType).isEqualTo("image/jpeg")
      assertThat(url).isEqualTo("https://some-host/public/licences/1/conditions/10/image-upload")
      assertThat(filename).isEqualTo("file2.pdf")
      assertThat(filesize).isEqualTo(2135)

      val summary = content.licences.findAttachmentSummary(attachmentNumber)
      assertThat(summary.filename).isEqualTo(filename)
      assertThat(summary.imageType).isEqualTo(contentType)
      assertThat(summary.description).isEqualTo(name)
    }
    with(result.attachments!![2]) {
      assertThat(attachmentNumber).isEqualTo(2)
      assertThat(name).isEqualTo("Document 3")
      assertThat(contentType).isEqualTo("image/png")
      assertThat(url).isEqualTo("https://some-host/public/licences/1/conditions/11/image-upload")
      assertThat(filename).isEqualTo("file3.pdf")

      val summary = content.licences.findAttachmentSummary(attachmentNumber)
      assertThat(summary.filename).isEqualTo(filename)
      assertThat(summary.imageType).isEqualTo(contentType)
      assertThat(summary.description).isEqualTo(name)
    }

    with(content.licences.first()) {
      assertThat(isToBeTaggedForProgramme).isNull()
      assertThat(programmeName).isNull()
    }
  }

  private fun List<SarLicence>.findAttachmentSummary(attachmentNumber: Int) = flatMap { licences ->
    licences.additionalLicenceConditions.flatMap { conditions -> conditions.uploadSummary }
  }.find { it.attachmentNumber == attachmentNumber }!!

  @Test
  fun `extractLastname should return lastname from firstname lastname format`() {
    assertThat(extractLastname("John Smith")).isEqualTo("Smith")
    assertThat(extractLastname("Anne Approver")).isEqualTo("Approver")
    assertThat(extractLastname("Test Client")).isEqualTo("Client")
  }

  @Test
  fun `extractLastname should handle multiple spaces and return last word`() {
    assertThat(extractLastname("John Middle Smith")).isEqualTo("Smith")
    assertThat(extractLastname("Mary Jane Watson")).isEqualTo("Watson")
  }

  @Test
  fun `extractLastname should return original name if no space`() {
    assertThat(extractLastname("SingleName")).isNull()
  }

  @Test
  fun `extractLastname should handle null and blank strings`() {
    assertThat(extractLastname(null)).isNull()
    assertThat(extractLastname("")).isEqualTo("")
    assertThat(extractLastname("   ")).isEqualTo("   ")
  }

  @Test
  fun `extractLastname should handle names with leading and trailing spaces`() {
    assertThat(extractLastname("  John Smith  ")).isEqualTo("Smith")
  }

  @Test
  fun `Lastname extraction is applied to createdByFullName, approvedByName, and appointmentPerson`() {
    val licence = crdLicence.copy(
      createdByFullName = "Test Client",
      approvedByName = "Anne Approver",
      appointmentPerson = "John Smith",
    )

    val result = SubjectAccessRequestResponseBuilder("https://some-host")
      .addLicence(licence)
      .build()

    val content = result.content as Content

    with(content.licences.first()) {
      assertThat(createdByLastName).isEqualTo("Client")
      assertThat(approvedByLastName).isEqualTo("Approver")
      assertThat(appointmentPersonLastName).isEqualTo("Smith")
    }
  }

  companion object {
    private val crdLicence = toCrd(
      licence = createCrdLicence().copy(
        version = "2.1",
        typeCode = AP,
        licenceVersion = "2.0",
        appointment = TestData.createAppointment(timeType = AppointmentTimeType.SPECIFIC_DATE_TIME),
      ),
      earliestReleaseDate = LocalDate.of(2024, 1, 3),
      isEligibleForEarlyRelease = true,
      hardStopDate = LocalDate.of(2024, 1, 1),
      hardStopWarningDate = LocalDate.of(2023, 12, 28),
      isInHardStopPeriod = true,

      isDueToBeReleasedInTheNextTwoWorkingDays = true,
      conditionPolicyData = emptyMap(),
    )
  }
}
