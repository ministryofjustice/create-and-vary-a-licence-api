package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.EmailConfig
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.PduHeadProperties
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.PrisonerForRelease
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.PromptLicenceCreationRequest
import uk.gov.service.notify.NotificationClient
import uk.gov.service.notify.NotificationClientException
import java.time.LocalDate

class NotifyServiceTest {
  private val notificationClient = mock<NotificationClient>()

  private val cardiffPduHead = EmailConfig(forename = "Bill", surname = "Excellent", description = "Cardiff", email = "cardiff@test.com")
  private val gwentPduHead = EmailConfig(forename = "Ted", surname = "Excellent", description = "Gwent", email = "gwent@test.com")
  private val pduHeadProperties = PduHeadProperties(
    contacts = mapOf(
      Pair("CARDIFF", cardiffPduHead),
      Pair("GWENT", gwentPduHead),
    )
  )

  private val notifyService = NotifyService(
    enabled = true,
    selfLink = "http://somewhere",
    licenceApprovedTemplateId = TEMPLATE_ID,
    variationForApprovalTemplateId = TEMPLATE_ID,
    initialLicencePromptTemplateId = TEMPLATE_ID,
    urgentLicencePromptTemplateId = TEMPLATE_ID,
    client = notificationClient,
    pduHeadProperties = pduHeadProperties,
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
  fun `No email is sent when notify is not enabled`() {
    NotifyService(
      enabled = false,
      selfLink = "http://somewhere",
      licenceApprovedTemplateId = TEMPLATE_ID,
      variationForApprovalTemplateId = TEMPLATE_ID,
      initialLicencePromptTemplateId = TEMPLATE_ID,
      urgentLicencePromptTemplateId = TEMPLATE_ID,
      client = notificationClient,
      pduHeadProperties = pduHeadProperties,
    ).sendVariationForApprovalEmail("CARDIFF", "1", "First", "Last")

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
      notifyService.sendVariationForApprovalEmail("CARDIFF", "1", "First", "Last")
    }
  }

  @Test
  fun `can read the configured PDU head information for Cardiff`() {
    notifyService.sendVariationForApprovalEmail("CARDIFF", "1", "First", "Last")
    verify(notificationClient).sendEmail(
      TEMPLATE_ID,
      "cardiff@test.com",
      mapOf(
        Pair("pduHeadFirstName", "Bill"),
        Pair("licenceFirstName", "First"),
        Pair("licenceLastName", "Last"),
        Pair("approvalCasesLink", "http://somewhere/licence/vary-approve/list"),
      ),
      null,
    )
  }

  @Test
  fun `can read the configured PDU head information for Gwent`() {
    notifyService.sendVariationForApprovalEmail("GWENT", "1", "First", "Last")
    verify(notificationClient).sendEmail(
      TEMPLATE_ID,
      "gwent@test.com",
      mapOf(
        Pair("pduHeadFirstName", "Ted"),
        Pair("licenceFirstName", "First"),
        Pair("licenceLastName", "Last"),
        Pair("approvalCasesLink", "http://somewhere/licence/vary-approve/list"),
      ),
      null,
    )
  }

  @Test
  fun `swallows the error and does not send email when a PDU head is not configured`() {
    assertDoesNotThrow {
      notifyService.sendVariationForApprovalEmail("NOT-PRESENT", "1", "First", "Last")
    }
    verifyNoInteractions(notificationClient)
  }

  private companion object {
    const val TEMPLATE_ID = "xxx-xxx-xxx-xxx"
    const val EMAIL_ADDRESS = "joe.bloggs@mail.com"
    const val REFERENCE = "licence-id"
  }
}
