package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.service.notify.NotificationClient
import uk.gov.service.notify.NotificationClientException

class NotifyServiceTest {
  private val notificationClient = mock<NotificationClient>()

  private val notifyService = NotifyService(
    enabled = true,
    licenceApprovedTemplateId = TEMPLATE_ID,
    client = notificationClient,
  )

  @Test
  fun `send licence approved email`() {
    notifyService.sendLicenceApprovedEmail(EMAIL_ADDRESS, mapOf(Pair("key", "value")), REFERENCE)
    verify(notificationClient).sendEmail(TEMPLATE_ID, EMAIL_ADDRESS, mapOf(Pair("key", "value")), REFERENCE)
  }

  @Test
  fun `No email is sent when notify is not enabled`() {
    NotifyService(enabled = false, licenceApprovedTemplateId = TEMPLATE_ID, client = notificationClient)
      .sendLicenceApprovedEmail(EMAIL_ADDRESS, mapOf(Pair("key", "value")), REFERENCE)
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
      notifyService.sendLicenceApprovedEmail(EMAIL_ADDRESS, mapOf(Pair("key", "value")), REFERENCE)
    }
  }

  private companion object {
    const val TEMPLATE_ID = "xxx-xxx-xxx-xxx"
    const val EMAIL_ADDRESS = "joe.bloggs@mail.com"
    const val REFERENCE = "licence-id"
  }
}
