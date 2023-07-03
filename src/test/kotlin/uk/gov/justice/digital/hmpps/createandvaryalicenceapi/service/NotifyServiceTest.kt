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
    variationForApprovalTemplateId = TEMPLATE_ID,
    initialLicencePromptTemplateId = TEMPLATE_ID,
    urgentLicencePromptTemplateId = TEMPLATE_ID,
    datesChangedTemplateId = TEMPLATE_ID,
    variationApprovedTemplateId = TEMPLATE_ID,
    variationReferredTemplateId = TEMPLATE_ID,
    variationForReApprovalTemplateId = TEMPLATE_ID,
    unapprovedLicenceByCrdTemplateId = TEMPLATE_ID,
    client = notificationClient,
    internalEmailAddress = INTERNAL_EMAIL_ADDRESS,
  )

  @Test
  fun `send licence initial licence create email`() {
    val comToEmail = PromptLicenceCreationRequest(
      email = EMAIL_ADDRESS,
      comName = "Joe Bloggs",
      initialPromptCases = listOf(
        PrisonerForRelease(name = "John Smith", releaseDate = LocalDate.parse("2022-11-20")),
      ),
    )
    val expectedMap = mapOf(
      "comName" to "Joe Bloggs",
      "prisonersForRelease" to listOf("John Smith who will leave custody on 20 November 2022"),
      "createLicenceLink" to "http://somewhere/licence/create/caseload",
    )

    notifyService.sendInitialLicenceCreateEmails(listOf(comToEmail))
    verify(notificationClient).sendEmail(TEMPLATE_ID, EMAIL_ADDRESS, expectedMap, null)
  }

  @Test
  fun `send licence urgent licence create email`() {
    val comToEmail = PromptLicenceCreationRequest(
      email = EMAIL_ADDRESS,
      comName = "Joe Bloggs",
      urgentPromptCases = listOf(
        PrisonerForRelease(name = "John Smith", releaseDate = LocalDate.parse("2022-11-20")),
      ),
    )
    val expectedMap = mapOf(
      "comName" to "Joe Bloggs",
      "prisonersForRelease" to listOf("John Smith who will leave custody on 20 November 2022"),
      "createLicenceLink" to "http://somewhere/licence/create/caseload",
    )

    notifyService.sendInitialLicenceCreateEmails(listOf(comToEmail))
    verify(notificationClient).sendEmail(TEMPLATE_ID, EMAIL_ADDRESS, expectedMap, null)
  }

  @Test
  fun `send dates changed email to the COM`() {
    val datesChanged = mapOf(
      "Release date" to true,
      "Licence end date" to true,
      "Sentence end date" to false,
      "Top up supervision start date" to false,
      "Top up supervision end date" to false,
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
      "comFullName" to "Joe Bloggs",
      "offenderFullName" to "James Jonas",
      "crn" to "X11111",
      "dateDescriptions" to listOf("Release date", "Licence end date"),
      "caseloadLink" to "http://somewhere/licence/create/caseload",
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
      "comName" to "Joe Bloggs",
      "fullName" to "Peter Falk",
      "caseListLink" to "http://somewhere/licence/vary/caseload",
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
      "comName" to "Joe Bloggs",
      "fullName" to "Peter Falk",
      "caseListLink" to "http://somewhere/licence/vary/caseload",
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
      "comName" to "Joe Bloggs",
      "fullName" to "Peter Falk",
      "caseListLink" to "http://somewhere/licence/vary/caseload",
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
      dateTime,
    )

    val expectedMap = mapOf(
      "prisonerFirstName" to "Peter",
      "prisonerLastName" to "Falk",
      "prisonerNumber" to "ABC123",
      "crd" to "10 February 2016",
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
      null,
    )
    verifyNoInteractions(notificationClient)
  }

  @Test
  fun `No email is sent when notify is not enabled`() {
    NotifyService(
      enabled = false,
      selfLink = "http://somewhere",
      variationForApprovalTemplateId = TEMPLATE_ID,
      initialLicencePromptTemplateId = TEMPLATE_ID,
      urgentLicencePromptTemplateId = TEMPLATE_ID,
      datesChangedTemplateId = TEMPLATE_ID,
      variationApprovedTemplateId = TEMPLATE_ID,
      variationReferredTemplateId = TEMPLATE_ID,
      variationForReApprovalTemplateId = TEMPLATE_ID,
      unapprovedLicenceByCrdTemplateId = TEMPLATE_ID,
      client = notificationClient,
      internalEmailAddress = INTERNAL_EMAIL_ADDRESS,
    ).sendVariationForApprovalEmail(NotifyRequest("", ""), "1", "First", "Last", "crn", "ComName")

    verifyNoInteractions(notificationClient)
  }

  @Test
  fun `Notify service catches and swallows exceptions`() {
    whenever(notificationClient.sendEmail(any(), any(), any(), any())).thenThrow(NotificationClientException("error"))
    assertDoesNotThrow {
      notifyService.sendVariationForApprovalEmail(NotifyRequest("", ""), "1", "First", "Last", "crn", "ComName")
    }
  }

  @Test
  fun `swallows the error and does not send email when contact info is null`() {
    assertDoesNotThrow {
      notifyService.sendVariationForApprovalEmail(NotifyRequest(null, null), "1", "First", "Last", "crn", "ComName")
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
        EMAIL_ADDRESS,
      ),
    )

    val expectedMap = mapOf(
      "crn" to "a123",
      "prisonerFirstName" to "jim",
      "prisonerLastName" to "smith",
      "comName" to "comFirst comLast",
    )

    notifyService.sendUnapprovedLicenceEmail(emailContent)
    verify(notificationClient).sendEmail(TEMPLATE_ID, "joe.bloggs@mail.com", expectedMap, null)
    verify(notificationClient).sendEmail(TEMPLATE_ID, INTERNAL_EMAIL_ADDRESS, expectedMap, null)
  }

  private companion object {
    const val TEMPLATE_ID = "xxx-xxx-xxx-xxx"
    const val EMAIL_ADDRESS = "joe.bloggs@mail.com"
    const val EMAIL_ADDRESS2 = "joe.bloggs2@mail.com"
    const val REFERENCE = "licence-id"
    const val INTERNAL_EMAIL_ADDRESS = "testemail@probation.gov.uk"
  }
}
