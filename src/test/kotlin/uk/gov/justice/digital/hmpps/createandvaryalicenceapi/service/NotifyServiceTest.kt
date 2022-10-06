package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.PrisonerForRelease
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.NotifyRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.PromptLicenceCreationRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.UnapprovedLicence
import uk.gov.service.notify.NotificationClient
import uk.gov.service.notify.NotificationClientException
import java.time.LocalDate
import java.time.Month

class NotifyServiceTest {
  private val notificationClient = mock<NotificationClient>()

  private val notifyService = NotifyService(
    enabled = true,
    selfLink = "http://somewhere",
    licenceApprovedTemplateId = TEMPLATE_ID,
    variationForApprovalTemplateId = TEMPLATE_ID,
    initialLicencePromptTemplateId = TEMPLATE_ID,
    urgentLicencePromptTemplateId = TEMPLATE_ID,
    datesChangedTemplateId = TEMPLATE_ID,
    variationApprovedTemplateId = TEMPLATE_ID,
    variationReferredTemplateId = TEMPLATE_ID,
    variationForReApprovalTemplateId = TEMPLATE_ID,
    unapprovedLicenceByCrdTemplateId = TEMPLATE_ID,
    client = notificationClient,
  )

  @Test
  fun `send licence approved email`() {
    notifyService.sendLicenceApprovedEmail(EMAIL_ADDRESS, mapOf(Pair("key", "value")), REFERENCE)
    // This email is currently not enabled, will not send and should have no interaction with the client
    verifyNoInteractions(notificationClient)

    // When reinstated this is the verification
    // verify(notificationClient).sendEmail(TEMPLATE_ID, EMAIL_ADDRESS, mapOf(Pair("key", "value")), REFERENCE)
  }

  @Test
  fun `send licence initial licence create email`() {
    val comToEmail = PromptLicenceCreationRequest(
      email = EMAIL_ADDRESS, comName = "Joe Bloggs",
      initialPromptCases = listOf(
        PrisonerForRelease(name = "John Smith", releaseDate = LocalDate.parse("2022-11-20"))
      )
    )
    val expectedMap = mapOf(
      Pair("comName", "Joe Bloggs"),
      Pair("prisonersForRelease", listOf("John Smith who will leave custody on 20 November 2022")),
      Pair("createLicenceLink", "http://somewhere/licence/create/caseload"),
    )

    notifyService.sendInitialLicenceCreateEmails(listOf(comToEmail))
    verify(notificationClient).sendEmail(TEMPLATE_ID, EMAIL_ADDRESS, expectedMap, null)
  }

  @Test
  fun `send licence urgent licence create email`() {
    val comToEmail = PromptLicenceCreationRequest(
      email = EMAIL_ADDRESS, comName = "Joe Bloggs",
      urgentPromptCases = listOf(
        PrisonerForRelease(name = "John Smith", releaseDate = LocalDate.parse("2022-11-20"))
      )
    )
    val expectedMap = mapOf(
      Pair("comName", "Joe Bloggs"),
      Pair("prisonersForRelease", listOf("John Smith who will leave custody on 20 November 2022")),
      Pair("createLicenceLink", "http://somewhere/licence/create/caseload"),
    )

    notifyService.sendInitialLicenceCreateEmails(listOf(comToEmail))
    verify(notificationClient).sendEmail(TEMPLATE_ID, EMAIL_ADDRESS, expectedMap, null)
  }

  @Test
  fun `send dates changed email to the COM`() {
    val datesChanged = mapOf(
      Pair("Release date", true),
      Pair("Licence end date", true),
      Pair("Sentence end date", false),
      Pair("Top up supervision start date", false),
      Pair("Top up supervision end date", false),
    )

    notifyService.sendDatesChangedEmail(
      "1",
      EMAIL_ADDRESS,
      "Joe Bloggs",
      "James Jonas",
      "X11111",
      datesChanged,
    )

    val expectedMap = mapOf(
      Pair("comFullName", "Joe Bloggs"),
      Pair("offenderFullName", "James Jonas"),
      Pair("crn", "X11111"),
      Pair("dateDescriptions", listOf("Release date", "Licence end date")),
      Pair("caseloadLink", "http://somewhere/licence/create/caseload")
    )

    verify(notificationClient).sendEmail(TEMPLATE_ID, EMAIL_ADDRESS, expectedMap, null)
  }

  @Test
  fun `send variation approved email - just the creator`() {
    notifyService.sendVariationApprovedEmail(
      creatorEmail = EMAIL_ADDRESS,
      creatorName = "Joe Bloggs",
      comEmail = "",
      comName = "",
      popName = "Peter Falk",
      licenceId = "1",
    )

    val expectedMap = mapOf(
      Pair("comName", "Joe Bloggs"),
      Pair("fullName", "Peter Falk"),
      Pair("caseListLink", "http://somewhere/licence/vary/caseload")
    )

    verify(notificationClient).sendEmail(TEMPLATE_ID, EMAIL_ADDRESS, expectedMap, null)
  }

  @Test
  fun `send variation approved email - creator and COM are the same`() {
    notifyService.sendVariationApprovedEmail(
      creatorEmail = EMAIL_ADDRESS,
      creatorName = "Joe Bloggs",
      comEmail = EMAIL_ADDRESS,
      comName = "Joe Bloggs",
      popName = "Peter Falk",
      licenceId = "1",
    )

    val expectedMap = mapOf(
      Pair("comName", "Joe Bloggs"),
      Pair("fullName", "Peter Falk"),
      Pair("caseListLink", "http://somewhere/licence/vary/caseload")
    )

    verify(notificationClient).sendEmail(TEMPLATE_ID, EMAIL_ADDRESS, expectedMap, null)
  }

  @Test
  fun `send variation approved email - creator and COM are different`() {
    notifyService.sendVariationApprovedEmail(
      creatorEmail = EMAIL_ADDRESS,
      creatorName = "Joe Bloggs",
      comEmail = EMAIL_ADDRESS2,
      comName = "Joe Bloggs-Two",
      popName = "Peter Falk",
      licenceId = "1",
    )
    verify(notificationClient, times(2)).sendEmail(any(), any(), any(), anyOrNull())
  }

  @Test
  fun `send variation referred email - just the creator`() {
    notifyService.sendVariationReferredEmail(
      creatorEmail = EMAIL_ADDRESS,
      creatorName = "Joe Bloggs",
      comEmail = "",
      comName = "",
      popName = "Peter Falk",
      licenceId = "1",
    )

    val expectedMap = mapOf(
      Pair("comName", "Joe Bloggs"),
      Pair("fullName", "Peter Falk"),
      Pair("caseListLink", "http://somewhere/licence/vary/caseload")
    )

    verify(notificationClient).sendEmail(TEMPLATE_ID, EMAIL_ADDRESS, expectedMap, null)
  }

  @Test
  fun `send variation re-approval email`() {
    val dateTime = LocalDate.of(2016, Month.FEBRUARY, 10)
    notifyService.sendVariationForReApprovalEmail(
      EMAIL_ADDRESS,
      "Peter",
      "Falk",
      "ABC123",
      dateTime
    )

    val expectedMap = mapOf(
      Pair("prisonerFirstName", "Peter"),
      Pair("prisonerLastName", "Falk"),
      Pair("prisonerNumber", "ABC123"),
      Pair("crd", "10 February 2016")
    )

    verify(notificationClient).sendEmail(TEMPLATE_ID, EMAIL_ADDRESS, expectedMap, null)
  }

  @Test
  fun `No re-approval email is sent when CRD is empty`() {
    notifyService.sendVariationForReApprovalEmail(
      EMAIL_ADDRESS,
      "Peter",
      "Falk",
      "ABC123",
      null
    )
    verifyNoInteractions(notificationClient)
  }

  @Test
  fun `No email is sent when notify is not enabled`() {
    NotifyService(
      enabled = false,
      selfLink = "http://somewhere",
      licenceApprovedTemplateId = TEMPLATE_ID,
      variationForApprovalTemplateId = TEMPLATE_ID,
      initialLicencePromptTemplateId = TEMPLATE_ID,
      urgentLicencePromptTemplateId = TEMPLATE_ID,
      datesChangedTemplateId = TEMPLATE_ID,
      variationApprovedTemplateId = TEMPLATE_ID,
      variationReferredTemplateId = TEMPLATE_ID,
      variationForReApprovalTemplateId = TEMPLATE_ID,
      unapprovedLicenceByCrdTemplateId = TEMPLATE_ID,
      client = notificationClient,
    ).sendVariationForApprovalEmail(NotifyRequest("", ""), "1", "First", "Last")

    verifyNoInteractions(notificationClient)
  }

  @Test
  fun `No email is sent when email address is empty`() {
    notifyService.sendLicenceApprovedEmail("", mapOf(Pair("key", "value")), REFERENCE)
    verifyNoInteractions(notificationClient)
  }

  @Test
  fun `Notify service catches and swallows exceptions`() {
    whenever(notificationClient.sendEmail(any(), any(), any(), any())).thenThrow(NotificationClientException("error"))
    assertDoesNotThrow {
      notifyService.sendVariationForApprovalEmail(NotifyRequest("", ""), "1", "First", "Last")
    }
  }

  @Test
  fun `swallows the error and does not send email when contact info is null`() {
    assertDoesNotThrow {
      notifyService.sendVariationForApprovalEmail(NotifyRequest(null, null), "1", "First", "Last")
    }
    verifyNoInteractions(notificationClient)
  }

  @Test
  fun `send unapproved licence email to probation practitioner`() {
    val emailContent = listOf(
      UnapprovedLicence(
        "a123",
        "jim",
        "smith",
        "comFirst",
        "comLast",
        EMAIL_ADDRESS
      )
    )

    val expectedMap = mapOf(
      Pair("crn", "a123"),
      Pair("prisonerFirstName", "jim"),
      Pair("prisonerLastName", "smith"),
      Pair("comName", "comFirst comLast")
    )

    notifyService.sendUnapprovedLicenceEmail(emailContent)
    verify(notificationClient).sendEmail(TEMPLATE_ID, "joe.bloggs@mail.com", expectedMap, null)
  }

  private companion object {
    const val TEMPLATE_ID = "xxx-xxx-xxx-xxx"
    const val EMAIL_ADDRESS = "joe.bloggs@mail.com"
    const val EMAIL_ADDRESS2 = "joe.bloggs2@mail.com"
    const val REFERENCE = "licence-id"
  }
}
