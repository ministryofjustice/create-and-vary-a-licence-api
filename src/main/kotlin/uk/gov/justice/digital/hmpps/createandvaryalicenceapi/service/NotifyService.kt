package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Case
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.NotifyRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.model.UnapprovedLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions.convertToTitleCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs.promptingCom.PromptComNotification
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.PRRD
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.TimeServedConsiderations
import uk.gov.service.notify.NotificationClient
import uk.gov.service.notify.NotificationClientException
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class NotifyService(
  @param:Value("\${notify.enabled}") private val enabled: Boolean,
  @param:Value("\${self.link}") private val selfLink: String,
  @param:Value("\${notify.templates.variationForApproval}") private val variationForApprovalTemplateId: String,
  @param:Value("\${notify.templates.omuReApproval}") private val licenceRequiresOmuReApprovalTemplateId: String,
  @param:Value("\${notify.templates.initialLicencePrompt}") private val initialLicencePromptTemplateId: String,
  @param:Value("\${notify.templates.datesChanged}") private val datesChangedTemplateId: String,
  @param:Value("\${notify.templates.variationApproved}") private val variationApprovedTemplateId: String,
  @param:Value("\${notify.templates.variationReferred}") private val variationReferredTemplateId: String,
  @param:Value("\${notify.templates.unapprovedLicence}") private val unapprovedLicenceByCrdTemplateId: String,
  @param:Value("\${notify.templates.hardStopLicenceApproved}") private val hardStopLicenceApprovedTemplateId: String,
  @param:Value("\${notify.templates.reviewableLicenceApproved}") private val reviewableLicenceApprovedTemplateId: String,
  @param:Value("\${notify.templates.editedLicenceTimedOut}") private val editedLicenceTimedOutTemplateId: String,
  @param:Value("\${notify.templates.hardStopLicenceReviewOverdue}") private val hardStopLicenceReviewOverdueTemplateId: String,
  @param:Value("\${notify.templates.licenceReviewOverdue}") private val licenceReviewOverdueTemplateId: String,
  @param:Value("\${internalEmailAddress}") private val internalEmailAddress: String,
  private val client: NotificationClient,
  private val releaseDateService: ReleaseDateService,
  @param:Value("\${feature.toggle.timeServed.enabled:false}")
  private val isTimeServedLogicEnabled: Boolean = false,
) {
  val dateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("dd LLLL yyyy")
  val releaseDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE dd LLLL yyyy")

  fun sendVariationForApprovalEmail(
    notifyRequest: NotifyRequest,
    licenceId: String,
    firstName: String,
    lastName: String,
    crn: String,
    comName: String,
  ) {
    if (notifyRequest.email == null || notifyRequest.name == null) {
      log.error("Notification failed (variationSubmitted) - email address not present for the PDU head for licence ID: $licenceId")
      return
    }
    val values: Map<String, String> = mapOf(
      "pduHeadName" to notifyRequest.name,
      "licenceFirstName" to firstName,
      "licenceLastName" to lastName,
      "approvalCasesLink" to selfLink.plus("/licence/vary-approve/list"),
      "comName" to comName,
      "crn" to crn,
    )
    if (sendEmail(variationForApprovalTemplateId, notifyRequest.email, values)) {
      log.info("Notification sent to ${notifyRequest.email} VARIATION FOR APPROVAL submitted by $comName for $licenceId $firstName $lastName with crn $crn")
    }
  }

  fun sendLicenceToOmuForReApprovalEmail(
    emailAddress: String?,
    firstName: String,
    lastName: String,
    prisonerNumber: String?,
    lsd: LocalDate?,
    crd: LocalDate? = null,
  ) {
    if (emailAddress == null || lsd == null) {
      log.error("Notification failed (omuReapproval) for PrisonerNumber $prisonerNumber - OMU email and CRD must be present")
      return
    }
    val values: Map<String, String> = mapOf(
      "prisonerFirstName" to firstName,
      "prisonerLastName" to lastName,
      "prisonerNumber" to (prisonerNumber ?: "unknown"),
      "crd" to (crd?.format(dateFormat) ?: ""),
      "lsd" to lsd.format(dateFormat),
    )
    if (sendEmail(licenceRequiresOmuReApprovalTemplateId, emailAddress, values)) {
      log.info("Notification sent to OMU $emailAddress FOR RE_APPROVAL for OMU PrisonerNumber $prisonerNumber")
    }
  }

  fun sendVariationApprovedEmail(
    creatorEmail: String,
    creatorName: String,
    comEmail: String,
    comName: String,
    popName: String,
    licenceId: String,
  ) {
    val contacts = getContacts(creatorEmail, creatorName, comEmail, comName)
    if (contacts.isEmpty()) {
      log.error("Notification failed (variationApproved) - email addresses not present for licence ID: $licenceId")
      return
    }

    contacts.forEach {
      val values: Map<String, String> = mapOf(
        "comName" to it.name,
        "fullName" to popName,
        "caseListLink" to selfLink.plus("/licence/vary/caseload"),
      )
      if (sendEmail(variationApprovedTemplateId, it.email, values)) {
        log.info("Notification sent to ${it.email} VARIATION APPROVED for $licenceId $popName")
      }
    }
  }

  fun sendUnapprovedLicenceEmail(unapprovedLicenceEmailData: List<UnapprovedLicence>) {
    if (unapprovedLicenceEmailData.isEmpty()) {
      log.info("There were no emails to send to the probation practitioner to inform them of any edited emails that weren't re-approved by CRD date")
      return
    }
    unapprovedLicenceEmailData.forEach {
      val offenderDetails: Map<String, String> = mapOf(
        "crn" to "${it.crn}",
        "prisonerFirstName" to "${it.forename}",
        "prisonerLastName" to "${it.surname}",
        "comName" to "${it.comFirstName} ${it.comLastName}",
      )

      if (sendEmail(unapprovedLicenceByCrdTemplateId, "${it.comEmail}", offenderDetails)) {
        log.info("Notification sent to ${it.comEmail} informing edited licence not reapproved by CRD for prisoner ${it.forename} ${it.surname}")
      }
      sendEmail(
        unapprovedLicenceByCrdTemplateId,
        internalEmailAddress,
        offenderDetails,
      )
    }
  }

  fun sendVariationReferredEmail(
    creatorEmail: String,
    creatorName: String,
    comEmail: String,
    comName: String,
    popName: String,
    licenceId: String,
  ) {
    val contacts = getContacts(creatorEmail, creatorName, comEmail, comName)
    if (contacts.isEmpty()) {
      log.error("Notification failed (variationReferred) - email addresses not present for licence ID: $licenceId")
      return
    }
    contacts.forEach {
      val values: Map<String, String> = mapOf(
        "comName" to it.name,
        "fullName" to popName,
        "caseListLink" to selfLink.plus("/licence/vary/caseload"),
      )
      if (sendEmail(variationReferredTemplateId, it.email, values)) {
        log.info("Notification sent to ${it.email} VARIATION REFERRED for $licenceId $popName")
      }
    }
  }

  fun sendDatesChangedEmail(
    licenceId: String,
    emailAddress: String?,
    comFullName: String,
    offenderFullName: String,
    crn: String?,
    datesChangedDescription: List<String>,
  ) {
    if (emailAddress.isNullOrBlank()) {
      log.error("Notification failed (datesChangedEmail) - email address not present for licence Id $licenceId")
      return
    }
    val values: Map<String, Any> = mapOf(
      "comFullName" to comFullName,
      "offenderFullName" to offenderFullName,
      "crn" to crn!!,
      "dateDescriptions" to datesChangedDescription,
      "caseloadLink" to selfLink.plus("/licence/create/caseload"),
    )
    if (sendEmail(datesChangedTemplateId, emailAddress, values)) {
      log.info("Notification sent to $emailAddress DATES CHANGED for $licenceId $offenderFullName")
    }
  }

  fun sendInitialLicenceCreateEmails(comsToEmail: List<PromptComNotification>) {
    log.info(
      """
      sending initial emails to ${comsToEmail.size} COMS.
      * initial emails to send: ${comsToEmail.filter { it.initialPromptCases.isNotEmpty() }.size}
      """.trimIndent(),
    )
    comsToEmail.forEach {
      if (it.initialPromptCases.isNotEmpty()) {
        sendLicenceCreateEmail(initialLicencePromptTemplateId, it.email, it.comName, it.initialPromptCases)
      }
    }
  }

  fun sendReviewableLicenceApprovedEmail(
    emailAddress: String?,
    firstName: String,
    lastName: String,
    crn: String?,
    lsd: LocalDate?,
    licenceId: String,
    prisonName: String,
    isTimeServedLicence: Boolean = false,
  ) {
    val isTimeServed = isTimeServedLogicEnabled && isTimeServedLicence
    val templateId = if (isTimeServedLogicEnabled) reviewableLicenceApprovedTemplateId else hardStopLicenceApprovedTemplateId
    if (emailAddress == null || lsd == null) {
      val approvalType = if (isTimeServed) "timeServedLicenceApproved" else "hardStopLicenceApproved"
      log.error(
        "Notification failed ($approvalType) " +
          "for licence $licenceId - email and CRD must be present",
      )

      return
    }

    val reasonForStandardLicence = if (isTimeServed) {
      "this person was released immediately following sentencing having served time on remand"
    } else {
      "none was submitted in time for their final release checks"
    }

    val values = buildMap<String, String> {
      put("firstName", firstName)
      put("lastName", lastName)
      put("crn", crn ?: "")
      put("releaseDate", lsd.format(dateFormat))
      if (isTimeServedLogicEnabled) {
        put("reasonForStandardLicence", reasonForStandardLicence)
        put("prisonName", prisonName)
      }
    }

    if (sendEmail(templateId, emailAddress, values)) {
      val licenceStatus = if (isTimeServed) "TIME SERVED" else "HARD STOP"
      log.info(
        "Notification sent to $emailAddress $licenceStatus LICENCE APPROVED " +
          "for $licenceId $firstName $lastName",
      )
    }
  }

  @TimeServedConsiderations("If a COM is not present, where should this email be sent to?")
  fun sendEditedLicenceTimedOutEmail(
    emailAddress: String?,
    comName: String,
    firstName: String,
    lastName: String,
    crn: String?,
    crd: LocalDate?,
    licenceId: String,
  ) {
    if (emailAddress == null || crd == null) {
      log.error("Notification failed (editedLicenceTimedOut) for licence $licenceId - email and CRD must be present")
      return
    }
    val values: Map<String, String> = mapOf(
      "comName" to comName,
      "prisonerFirstName" to firstName,
      "prisonerLastName" to lastName,
      "crn" to crn!!,
      "crd" to crd.format(dateFormat),
    )
    if (sendEmail(editedLicenceTimedOutTemplateId, emailAddress, values)) {
      log.info("Notification sent to $emailAddress EDITED LICENCE TIMED OUT for $licenceId $firstName $lastName")
    }
  }

  fun sendLicenceReviewOverdueEmail(
    emailAddress: String?,
    comName: String,
    firstName: String,
    lastName: String,
    crn: String?,
    licenceId: String,
    isTimeServedLicence: Boolean,
  ) {
    val isTimeServed = isTimeServedLogicEnabled && isTimeServedLicence
    val templateId = if (isTimeServedLogicEnabled) licenceReviewOverdueTemplateId else hardStopLicenceReviewOverdueTemplateId
    if (emailAddress == null) {
      val licenceReviewOverdueType = if (isTimeServed) "timeServedLicenceReviewOverdue" else "hardStopLicenceReviewOverdue"
      log.error("Notification failed ($licenceReviewOverdueType) for licence $licenceId - email and CRD must be present")
      return
    }
    val reasonForStandardLicence = if (isTimeServed) {
      "this person was released immediately following sentencing having served time on remand"
    } else {
      "none was submitted in time for their final release checks"
    }
    val values = buildMap<String, String> {
      put("comName", comName)
      put("firstName", firstName)
      put("lastName", lastName)
      put("crn", crn!!)
      if (isTimeServedLogicEnabled) {
        put("reasonForStandardLicence", reasonForStandardLicence)
      }
    }
    if (sendEmail(templateId, emailAddress, values)) {
      val licenceStatus = if (isTimeServed) "TIME SERVED" else "HARD STOP"
      log.info("Notification sent to $emailAddress $licenceStatus LICENCE REVIEW OVERDUE for $licenceId $firstName $lastName")
    }
  }

  internal fun sendLicenceCreateEmail(
    templateId: String,
    emailAddress: String,
    comName: String,
    cases: List<Case>,
  ) {
    log.info("Sending licence create email to: $emailAddress for ${cases.size}")
    sendEmail(
      templateId,
      emailAddress,
      mapOf(
        "comName" to comName,
        "prisonersForRelease" to cases.map { prisoner ->
          val releaseType = if (prisoner.kind == PRRD) {
            "following a fixed-term recall"
          } else {
            "as a standard release"
          }
          val releaseDate = prisoner.licenceStartDate.format(releaseDateFormat)
          "${prisoner.name.convertToTitleCase()} (CRN: ${prisoner.crn}), who is due to leave prison $releaseType on $releaseDate"
        },
        "createLicenceLink" to selfLink.plus("/licence/create/caseload"),
        "isEligibleForEarlyRelease" to if (cases.any { releaseDateService.isEligibleForEarlyRelease(it.licenceStartDate) }) "yes" else "no",
      ),
    )

    cases.map { prisoner ->
      var promptType = "REMINDER"
      if (templateId == initialLicencePromptTemplateId) {
        promptType = "INITIAL PROMPT"
      }
      log.info(
        "Notification sent to $emailAddress $promptType for ${prisoner.name} being release on ${
          prisoner.licenceStartDate.format(
            dateFormat,
          )
        }",
      )
    }
  }

  private fun sendEmail(
    templateId: String,
    emailAddress: String,
    values: Map<String, Any>,
  ): Boolean {
    if (!enabled) {
      log.info("Notification disabled: Did not send notification to $emailAddress for $templateId")
      return false
    }

    if (emailAddress.isBlank()) {
      log.info("Notification - blank email address: Did not send for template ID $templateId")
      return false
    }

    try {
      client.sendEmail(templateId, emailAddress, values, null)
      log.info("Notification - sent email for template ID $templateId")
      return true
    } catch (e: NotificationClientException) {
      log.error("Notification failed - templateId $templateId to $emailAddress", e)
      return false
    }
  }

  private fun getContacts(creatorEmail: String, creatorName: String, comEmail: String, comName: String): List<Contact> = when {
    creatorEmail.isBlank() && comEmail.isNotBlank() -> listOf(Contact(comEmail, comName))
    comEmail.isBlank() && creatorEmail.isNotBlank() -> listOf(Contact(creatorEmail, creatorName))
    creatorEmail == comEmail -> listOf(Contact(creatorEmail, creatorName))
    else -> listOf(
      Contact(creatorEmail, creatorName),
      Contact(comEmail, comName),
    )
  }

  companion object {
    private val log = LoggerFactory.getLogger(NotifyService::class.java)
  }

  private data class Contact(
    var email: String,
    var name: String,
  )
}
