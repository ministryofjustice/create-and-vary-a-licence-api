package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.PrisonerForRelease
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.PromptLicenceCreationRequest
import uk.gov.service.notify.NotificationClient
import uk.gov.service.notify.NotificationClientException
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Service
class NotifyService(
  @Value("\${notify.enabled}") private val enabled: Boolean,
  @Value("\${notify.templates.licenceApproved}") private val licenceApprovedTemplateId: String,
  @Value("\${notify.templates.initialLicencePrompt}") private val initialLicencePromptTemplateId: String,
  @Value("\${notify.templates.urgentLicencePrompt}") private val urgentLicencePromptTemplateId: String,
  private val client: NotificationClient
) {
  fun sendLicenceApprovedEmail(emailAddress: String, values: Map<String, String>, reference: String) {
    sendEmail(licenceApprovedTemplateId, emailAddress, values, reference)
  }

  fun sendInitialLicenceCreateEmails(comsToEmail: List<PromptLicenceCreationRequest>) {
    comsToEmail.forEach {
      if (it.initialPromptCases.isNotEmpty()) {
        sendLicenceCreateEmail(initialLicencePromptTemplateId, it.email, it.comName, it.initialPromptCases)
      }
      if (it.urgentPromptCases.isNotEmpty()) {
        sendLicenceCreateEmail(urgentLicencePromptTemplateId, it.email, it.comName, it.urgentPromptCases)
      }
    }
  }

  private fun sendLicenceCreateEmail(templateId: String, emailAddress: String, comName: String, cases: List<PrisonerForRelease>) {
    sendEmail(
      templateId,
      emailAddress,
      mapOf(
        Pair("comName", comName),
        Pair(
          "prisonersForRelease",
          cases.map {
            prisoner ->
            "${prisoner.name} who will leave custody on ${prisoner.releaseDate.format(DateTimeFormatter.ofPattern("dd LLLL yyyy"))}"
          }
        ),
      ),
      null
    )
  }

  private fun sendEmail(templateId: String, emailAddress: String, values: Map<String, Any>, reference: String?) {
    if (!enabled) {
      log.info("Notification disabled: Did not send notification to $emailAddress for $templateId ref $reference")
      return
    }

    if (emailAddress.isBlank()) {
      log.info("Blank email address: Did not send notification for $templateId ref $reference")
      return
    }

    try {
      client.sendEmail(templateId, emailAddress, values, reference)
    } catch (e: NotificationClientException) {
      log.error("Email notification failed", e)
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(NotifyService::class.java)
  }
}
