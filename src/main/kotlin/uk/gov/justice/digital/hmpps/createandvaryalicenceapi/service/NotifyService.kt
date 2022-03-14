package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.EmailConfig
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.PduHeadProperties
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.PrisonerForRelease
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.PromptLicenceCreationRequest
import uk.gov.service.notify.NotificationClient
import uk.gov.service.notify.NotificationClientException
import java.time.format.DateTimeFormatter

@Service
class NotifyService(
  @Value("\${notify.enabled}") private val enabled: Boolean,
  @Value("\${notify.templates.licenceApproved}") private val licenceApprovedTemplateId: String,
  @Value("\${notify.templates.variationForApproval}") private val variationForApprovalTemplateId: String,
  @Value("\${notify.templates.initialLicencePrompt}") private val initialLicencePromptTemplateId: String,
  @Value("\${notify.templates.urgentLicencePrompt}") private val urgentLicencePromptTemplateId: String,
  private val client: NotificationClient,
  private val pduHeadProperties: PduHeadProperties,
) {
  fun sendLicenceApprovedEmail(emailAddress: String, values: Map<String, String>, reference: String) {
    // Hobbled this email - no current requirement to send this out - blanked out the email address to avoid sending
    // sendEmail(licenceApprovedTemplateId, emailAddress, values, reference)
    sendEmail(licenceApprovedTemplateId, "", values, reference)
  }

  // TODO: Add environment-specific link to approval cases or specific page to approve this variation
  fun sendVariationForApprovalEmail(pduCode: String, licenceId: String, firstName: String, lastName: String) {
    val pduHead = pduHeadProperties.contacts
      .getOrDefault(pduCode, EmailConfig(forename = "", surname = "", email = "", description = ""))

    if (pduHead.email.isNotBlank()) {
      val values: Map<String, String> = mapOf(
        Pair("pduHeadFirstName", pduHead.forename),
        Pair("licenceFirstName", firstName),
        Pair("licenceLastName", lastName),
      )
      sendEmail(variationForApprovalTemplateId, pduHead.email, values, null)
    } else {
      log.error("sendVariationForApproval: A contact was not configured for the head of PDU $pduCode")
    }
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
          cases.map { prisoner ->
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
      log.error("Email notification failed - templateId $templateId to $emailAddress", e)
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(NotifyService::class.java)
  }
}
