package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Case
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.model.UnapprovedLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.NotifyRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs.promptingCom.PromptComNotification
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.CRD
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.PRRD
import uk.gov.service.notify.NotificationClient
import uk.gov.service.notify.NotificationClientException
import java.time.LocalDate
import java.time.Month

class NotifyServiceTest {
  private val notificationClient = mock<NotificationClient>()
  private val releaseDateService = mock<ReleaseDateService>()

  private val notifyService = NotifyService(
    enabled = true,
    selfLink = "http://somewhere",
    variationForApprovalTemplateId = TEMPLATE_ID,
    initialLicencePromptTemplateId = TEMPLATE_ID,
    datesChangedTemplateId = TEMPLATE_ID,
    variationApprovedTemplateId = TEMPLATE_ID,
    variationReferredTemplateId = TEMPLATE_ID,
    variationForReApprovalTemplateId = TEMPLATE_ID,
    unapprovedLicenceByCrdTemplateId = TEMPLATE_ID,
    hardStopLicenceApprovedTemplateId = TEMPLATE_ID,
    editedLicenceTimedOutTemplateId = TEMPLATE_ID,
    hardStopLicenceReviewOverdueTemplateId = TEMPLATE_ID,
    client = notificationClient,
    internalEmailAddress = INTERNAL_EMAIL_ADDRESS,
    releaseDateService = releaseDateService,
  )

  @Test
  fun `send licence initial licence create email`() {
    whenever(releaseDateService.isEligibleForEarlyRelease(any<LocalDate>())).thenReturn(true)

    val comToEmail = PromptComNotification(
      email = EMAIL_ADDRESS,
      comName = "Joe Bloggs",
      initialPromptCases = listOf(
        Case(name = "John Smith", crn = "X12444", licenceStartDate = LocalDate.parse("2022-11-20"), kind = CRD),
      ),
    )
    val expectedMap = mapOf(
      "comName" to "Joe Bloggs",
      "prisonersForRelease" to listOf("John Smith (CRN: X12444), who is due to leave prison as a standard release on Sunday 20 November 2022"),
      "createLicenceLink" to "http://somewhere/licence/create/caseload",
      "isEligibleForEarlyRelease" to "yes",
    )

    notifyService.sendInitialLicenceCreateEmails(listOf(comToEmail))
    verify(notificationClient).sendEmail(TEMPLATE_ID, EMAIL_ADDRESS, expectedMap, null)
  }

  @Test
  fun `send licence initial licence create email with multiple cases among which one prisoner is eligible for early release`() {
    whenever(releaseDateService.isEligibleForEarlyRelease(any<LocalDate>())).thenReturn(true)

    val comToEmail = PromptComNotification(
      email = EMAIL_ADDRESS,
      comName = "Joe Bloggs",
      initialPromptCases = listOf(
        Case(name = "Test one", crn = "X12444", licenceStartDate = LocalDate.parse("2022-11-09"), CRD),
        Case(name = "Test two", crn = "X12444", licenceStartDate = LocalDate.parse("2022-11-10"), PRRD),
        Case(name = "Test three", crn = "X12444", licenceStartDate = LocalDate.parse("2022-11-11"), CRD),
      ),
    )
    // Test One  (CRN: X12444), who is due to leave to leave prison as a standard release on Wednesday 09 November 2022, Test Two  (CRN: X12444), who is due to leave to leave prison following a fixed-term recall on Thursday 10 November 2022, Test Three  (CRN: X12444), who is due to leave to leave prison as a standard release on Friday 11 November 2022], "createLicenceLink" = "http://somewhere/licence/create/caseload", "isEligibleForEarlyRelease" = "yes"},
    val expectedMap = mapOf(
      "comName" to "Joe Bloggs",
      "prisonersForRelease" to listOf(
        "Test One (CRN: X12444), who is due to leave prison as a standard release on Wednesday 09 November 2022",
        "Test Two (CRN: X12444), who is due to leave prison following a fixed-term recall on Thursday 10 November 2022",
        "Test Three (CRN: X12444), who is due to leave prison as a standard release on Friday 11 November 2022",
      ),
      "createLicenceLink" to "http://somewhere/licence/create/caseload",
      "isEligibleForEarlyRelease" to "yes",
    )

    // , Test Three (CRN: X12444), who is due to leave prison as a standard release on Friday 11 November 2022], "createLicenceLink" = "http://somewhere/licence/create/caseload", "isEligibleForEarlyRelease" = "yes"},
    notifyService.sendInitialLicenceCreateEmails(listOf(comToEmail))
    verify(notificationClient).sendEmail(TEMPLATE_ID, EMAIL_ADDRESS, expectedMap, null)
  }

  @Test
  fun `send licence initial licence create email with multiple cases among which no prisoner is eligible for early release`() {
    val comToEmail = PromptComNotification(
      email = EMAIL_ADDRESS,
      comName = "Joe Bloggs",
      initialPromptCases = listOf(
        Case(name = "test one", crn = "X12444", licenceStartDate = LocalDate.parse("2022-11-09"), kind = PRRD),
        Case(name = "test two", crn = "X12444", licenceStartDate = LocalDate.parse("2022-11-10"), kind = CRD),
        Case(name = "test three", crn = "X12444", licenceStartDate = LocalDate.parse("2022-11-24"), kind = CRD),
      ),
    )
    val expectedMap = mapOf(
      "comName" to "Joe Bloggs",
      "prisonersForRelease" to listOf(
        "Test One (CRN: X12444), who is due to leave prison following a fixed-term recall on Wednesday 09 November 2022",
        "Test Two (CRN: X12444), who is due to leave prison as a standard release on Thursday 10 November 2022",
        "Test Three (CRN: X12444), who is due to leave prison as a standard release on Thursday 24 November 2022",
      ),
      "createLicenceLink" to "http://somewhere/licence/create/caseload",
      "isEligibleForEarlyRelease" to "no",
    )

    notifyService.sendInitialLicenceCreateEmails(listOf(comToEmail))
    verify(notificationClient).sendEmail(TEMPLATE_ID, EMAIL_ADDRESS, expectedMap, null)
  }

  @Test
  fun `send dates changed email to the COM`() {
    val datesChanged = listOf("Release date", "Licence end date")

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
      popName = "John Doe",
      licenceId = "1",
    )

    val expectedMap = mapOf(
      "comName" to "Joe Bloggs",
      "fullName" to "John Doe",
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
      popName = "John Doe",
      licenceId = "1",
    )

    val expectedMap = mapOf(
      "comName" to "Joe Bloggs",
      "fullName" to "John Doe",
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
      popName = "John Doe",
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
      popName = "John Doe",
      licenceId = "1",
    )

    val expectedMap = mapOf(
      "comName" to "Joe Bloggs",
      "fullName" to "John Doe",
      "caseListLink" to "http://somewhere/licence/vary/caseload",
    )

    verify(notificationClient).sendEmail(TEMPLATE_ID, EMAIL_ADDRESS, expectedMap, null)
  }

  @Test
  fun `send variation re-approval email`() {
    val lsd = LocalDate.of(2016, Month.FEBRUARY, 10)
    notifyService.sendVariationForReApprovalEmail(
      EMAIL_ADDRESS,
      "John",
      "Doe",
      "ABC123",
      lsd = lsd,
    )

    val expectedMap = mapOf(
      "prisonerFirstName" to "John",
      "prisonerLastName" to "Doe",
      "prisonerNumber" to "ABC123",
      "lsd" to "10 February 2016",
      "crd" to "",
    )

    verify(notificationClient).sendEmail(TEMPLATE_ID, EMAIL_ADDRESS, expectedMap, null)
  }

  @Test
  fun `No re-approval email is sent when LSD is empty`() {
    notifyService.sendVariationForReApprovalEmail(
      EMAIL_ADDRESS,
      "John",
      "Doe",
      "ABC123",
      lsd = null,
      crd = LocalDate.now(),
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
      datesChangedTemplateId = TEMPLATE_ID,
      variationApprovedTemplateId = TEMPLATE_ID,
      variationReferredTemplateId = TEMPLATE_ID,
      variationForReApprovalTemplateId = TEMPLATE_ID,
      unapprovedLicenceByCrdTemplateId = TEMPLATE_ID,
      client = notificationClient,
      internalEmailAddress = INTERNAL_EMAIL_ADDRESS,
      releaseDateService = releaseDateService,
      hardStopLicenceApprovedTemplateId = TEMPLATE_ID,
      editedLicenceTimedOutTemplateId = TEMPLATE_ID,
      hardStopLicenceReviewOverdueTemplateId = TEMPLATE_ID,
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
    verify(notificationClient).sendEmail(TEMPLATE_ID, "joe.bloggs@test.com", expectedMap, null)
    verify(notificationClient).sendEmail(TEMPLATE_ID, INTERNAL_EMAIL_ADDRESS, expectedMap, null)
  }

  @Nested
  inner class `approving hard stop licences` {
    @Test
    fun `send hard stop licence approved email to probation practitioner`() {
      notifyService.sendHardStopLicenceApprovedEmail(
        emailAddress = EMAIL_ADDRESS,
        firstName = "John",
        lastName = "Doe",
        crn = "A123456",
        lsd = LocalDate.of(2024, 4, 17),
        licenceId = "1",
      )

      val expectedMap = mapOf(
        "firstName" to "John",
        "lastName" to "Doe",
        "crn" to "A123456",
        "releaseDate" to "17 April 2024",
      )

      verify(notificationClient).sendEmail(TEMPLATE_ID, EMAIL_ADDRESS, expectedMap, null)
    }

    @Test
    fun `No hard stop approval licence email is sent when CRD is empty`() {
      notifyService.sendHardStopLicenceApprovedEmail(
        emailAddress = EMAIL_ADDRESS,
        firstName = "John",
        lastName = "Doe",
        crn = "A123456",
        lsd = null,
        licenceId = "1",
      )
      verifyNoInteractions(notificationClient)
    }

    @Test
    fun `No hard stop approval licence email is sent when email address is empty`() {
      notifyService.sendHardStopLicenceApprovedEmail(
        emailAddress = null,
        firstName = "John",
        lastName = "Doe",
        crn = "A123456",
        lsd = LocalDate.of(2024, 4, 17),
        licenceId = "1",
      )
      verifyNoInteractions(notificationClient)
    }
  }

  @Nested
  inner class `edited licences timed out` {
    @Test
    fun `send edited licence timed out email to COM`() {
      notifyService.sendEditedLicenceTimedOutEmail(
        emailAddress = EMAIL_ADDRESS,
        comName = "Joe Bloggs",
        firstName = "John",
        lastName = "Doe",
        crn = "A123456",
        crd = LocalDate.of(2024, 4, 17),
        licenceId = "1",
      )

      val expectedMap = mapOf(
        "comName" to "Joe Bloggs",
        "prisonerFirstName" to "John",
        "prisonerLastName" to "Doe",
        "crn" to "A123456",
        "crd" to "17 April 2024",
      )

      verify(notificationClient).sendEmail(TEMPLATE_ID, EMAIL_ADDRESS, expectedMap, null)
    }

    @Test
    fun `No edited licence timed out email is sent when CRD is empty`() {
      notifyService.sendEditedLicenceTimedOutEmail(
        emailAddress = EMAIL_ADDRESS,
        comName = "Joe Bloggs",
        firstName = "John",
        lastName = "Doe",
        crn = "A123456",
        crd = null,
        licenceId = "1",
      )
      verifyNoInteractions(notificationClient)
    }

    @Test
    fun `No edited licence timed out email is sent when email address is empty`() {
      notifyService.sendEditedLicenceTimedOutEmail(
        emailAddress = null,
        comName = "Joe Bloggs",
        firstName = "John",
        lastName = "Doe",
        crn = "A123456",
        crd = LocalDate.of(2024, 4, 17),
        licenceId = "1",
      )
      verifyNoInteractions(notificationClient)
    }
  }

  @Nested
  inner class `hard stop licence review overdue` {
    @Test
    fun `send hard stop licence review overdue email to COM`() {
      notifyService.sendHardStopLicenceReviewOverdueEmail(
        emailAddress = EMAIL_ADDRESS,
        comName = "Joe Bloggs",
        firstName = "John",
        lastName = "Doe",
        crn = "A123456",
        licenceId = "1",
      )

      val expectedMap = mapOf(
        "comName" to "Joe Bloggs",
        "firstName" to "John",
        "lastName" to "Doe",
        "crn" to "A123456",
      )

      verify(notificationClient).sendEmail(TEMPLATE_ID, EMAIL_ADDRESS, expectedMap, null)
    }

    @Test
    fun `No hard stop licence review overdue email is sent when email address is empty`() {
      notifyService.sendHardStopLicenceReviewOverdueEmail(
        emailAddress = null,
        comName = "Joe Bloggs",
        firstName = "John",
        lastName = "Doe",
        crn = "A123456",
        licenceId = "1",
      )
      verifyNoInteractions(notificationClient)
    }
  }

  private companion object {
    const val TEMPLATE_ID = "xxx-xxx-xxx-xxx"
    const val EMAIL_ADDRESS = "joe.bloggs@test.com"
    const val EMAIL_ADDRESS2 = "joe.bloggs2@test.com"
    const val INTERNAL_EMAIL_ADDRESS = "testemail@probation.gov.uk"
  }
}
