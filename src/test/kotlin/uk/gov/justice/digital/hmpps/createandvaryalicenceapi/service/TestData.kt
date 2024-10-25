package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HardStopLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.PrisonUser
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.StandardCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.VariationLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CaCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CaseloadItem
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CvlFields
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Prisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ProbationPractitioner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.HARD_STOP_CONDITION
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AppointmentPersonType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.CaViewCasesTab
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate
import java.time.LocalDateTime

object TestData {

  fun com() = CommunityOffenderManager(
    staffIdentifier = 2000,
    username = "smills",
    email = "testemail@probation.gov.uk",
    firstName = "X",
    lastName = "Y",
  )

  fun ca() = PrisonUser(
    username = "smills",
    email = "testemail@probation.gov.uk",
    firstName = "X",
    lastName = "Y",
  )

  fun hardStopAdditionalCondition(licence: Licence) = AdditionalCondition(
    licence = licence,
    id = 2L,
    conditionSequence = 1,
    conditionType = "AP",
    conditionCode = HARD_STOP_CONDITION.code,
    conditionText = HARD_STOP_CONDITION.text,
    expandedConditionText = HARD_STOP_CONDITION.text,
    conditionVersion = licence.version!!,
    additionalConditionData = emptyList(),
    additionalConditionUploadSummary = emptyList(),
    conditionCategory = HARD_STOP_CONDITION.categoryShort,
  )

  fun someEntityStandardConditions(licence: Licence) =
    listOf(
      StandardCondition(
        id = 1,
        conditionCode = "goodBehaviour",
        conditionSequence = 1,
        conditionText = "Be of good behaviour",
        conditionType = "AP",
        licence = licence,
      ),
      StandardCondition(
        id = 2,
        conditionCode = "notBreakLaw",
        conditionSequence = 2,
        conditionText = "Do not break any law",
        conditionType = "AP",
        licence = licence,
      ),
      StandardCondition(
        id = 3,
        conditionCode = "attendMeetings",
        conditionSequence = 3,
        conditionText = "Attend meetings",
        conditionType = "AP",
        licence = licence,
      ),
    )

  fun createCrdLicence() = CrdLicence(
    id = 1,
    typeCode = LicenceType.AP,
    version = "1.1",
    statusCode = LicenceStatus.IN_PROGRESS,
    nomsId = "A1234AA",
    bookingNo = "123456",
    bookingId = 54321,
    crn = "X12345",
    pnc = "2019/123445",
    cro = "12345",
    prisonCode = "MDI",
    prisonDescription = "Moorland (HMP)",
    forename = "John",
    surname = "Smith",
    dateOfBirth = LocalDate.of(1985, 12, 28),
    conditionalReleaseDate = LocalDate.of(2021, 10, 22),
    actualReleaseDate = LocalDate.of(2021, 10, 22),
    sentenceStartDate = LocalDate.of(2018, 10, 22),
    sentenceEndDate = LocalDate.of(2021, 10, 22),
    licenceStartDate = LocalDate.of(2021, 10, 22),
    licenceExpiryDate = LocalDate.of(2021, 10, 22),
    topupSupervisionStartDate = LocalDate.of(2021, 10, 22),
    topupSupervisionExpiryDate = LocalDate.of(2021, 10, 22),
    probationAreaCode = "N01",
    probationAreaDescription = "Wales",
    probationPduCode = "N01A",
    probationPduDescription = "Cardiff",
    probationLauCode = "N01A2",
    probationLauDescription = "Cardiff South",
    probationTeamCode = "NA01A2-A",
    probationTeamDescription = "Cardiff South Team A",
    dateCreated = LocalDateTime.of(2022, 7, 27, 15, 0, 0),
    standardConditions = emptyList(),
    responsibleCom = CommunityOffenderManager(
      staffIdentifier = 2000,
      username = "smills",
      email = "testemail@probation.gov.uk",
      firstName = "X",
      lastName = "Y",
    ),
    createdBy = CommunityOffenderManager(
      staffIdentifier = 2000,
      username = "smills",
      email = "testemail@probation.gov.uk",
      firstName = "X",
      lastName = "Y",
    ),
  ).let {
    it.copy(standardConditions = someEntityStandardConditions(it))
  }

  fun createHardStopLicence() = HardStopLicence(
    id = 1,
    typeCode = LicenceType.AP,
    version = "2.1",
    statusCode = LicenceStatus.IN_PROGRESS,
    nomsId = "A1234AA",
    bookingNo = "123456",
    bookingId = 54321,
    crn = "X12345",
    pnc = "2019/123445",
    cro = "12345",
    prisonCode = "MDI",
    prisonDescription = "Moorland (HMP)",
    forename = "John",
    surname = "Smith",
    dateOfBirth = LocalDate.of(1985, 12, 28),
    conditionalReleaseDate = LocalDate.of(2021, 10, 22),
    actualReleaseDate = LocalDate.of(2021, 10, 22),
    sentenceStartDate = LocalDate.of(2018, 10, 22),
    sentenceEndDate = LocalDate.of(2021, 10, 22),
    licenceStartDate = LocalDate.of(2021, 10, 22),
    licenceExpiryDate = LocalDate.of(2021, 10, 22),
    topupSupervisionStartDate = LocalDate.of(2021, 10, 22),
    topupSupervisionExpiryDate = LocalDate.of(2021, 10, 22),
    probationAreaCode = "N01",
    probationAreaDescription = "Wales",
    probationPduCode = "N01A",
    probationPduDescription = "Cardiff",
    probationLauCode = "N01A2",
    probationLauDescription = "Cardiff South",
    probationTeamCode = "NA01A2-A",
    probationTeamDescription = "Cardiff South Team A",
    dateCreated = LocalDateTime.of(2022, 7, 27, 15, 0, 0),
    standardConditions = emptyList(),
    responsibleCom = CommunityOffenderManager(
      staffIdentifier = 2000,
      username = "smills",
      email = "testemail@probation.gov.uk",
      firstName = "X",
      lastName = "Y",
    ),
    createdBy = PrisonUser(
      username = "smills",
      email = "testemail@probation.gov.uk",
      firstName = "X",
      lastName = "Y",
    ),
  ).let {
    it.copy(
      standardConditions = someEntityStandardConditions(it),
      additionalConditions = listOf(hardStopAdditionalCondition(it)),
    )
  }

  fun createVariationLicence() = VariationLicence(
    id = 1,
    typeCode = LicenceType.AP,
    version = "1.1",
    statusCode = LicenceStatus.VARIATION_IN_PROGRESS,
    nomsId = "A1234AA",
    bookingNo = "123456",
    bookingId = 54321,
    crn = "X12345",
    pnc = "2019/123445",
    cro = "12345",
    prisonCode = "MDI",
    prisonDescription = "Moorland (HMP)",
    forename = "John",
    surname = "Smith",
    dateOfBirth = LocalDate.of(1985, 12, 28),
    conditionalReleaseDate = LocalDate.of(2021, 10, 22),
    actualReleaseDate = LocalDate.of(2021, 10, 22),
    sentenceStartDate = LocalDate.of(2018, 10, 22),
    sentenceEndDate = LocalDate.of(2021, 10, 22),
    licenceStartDate = LocalDate.of(2021, 10, 22),
    licenceExpiryDate = LocalDate.of(2021, 10, 22),
    topupSupervisionStartDate = LocalDate.of(2021, 10, 22),
    topupSupervisionExpiryDate = LocalDate.of(2021, 10, 22),
    probationAreaCode = "N01",
    probationAreaDescription = "Wales",
    probationPduCode = "N01A",
    probationPduDescription = "Cardiff",
    probationLauCode = "N01A2",
    probationLauDescription = "Cardiff South",
    probationTeamCode = "NA01A2-A",
    probationTeamDescription = "Cardiff South Team A",
    dateCreated = LocalDateTime.of(2022, 7, 27, 15, 0, 0),
    standardConditions = emptyList(),
    responsibleCom = CommunityOffenderManager(
      staffIdentifier = 2000,
      username = "smills",
      email = "testemail@probation.gov.uk",
      firstName = "X",
      lastName = "Y",
    ),
    createdBy = com(),
    appointmentPersonType = AppointmentPersonType.SPECIFIC_PERSON,
  ).let {
    it.copy(standardConditions = someEntityStandardConditions(it))
  }

  fun createHdcLicence() = HdcLicence(
    id = 1,
    typeCode = LicenceType.AP,
    version = "1.1",
    statusCode = LicenceStatus.IN_PROGRESS,
    nomsId = "A1234AA",
    bookingNo = "123456",
    bookingId = 54321,
    crn = "X12345",
    pnc = "2019/123445",
    cro = "12345",
    prisonCode = "MDI",
    prisonDescription = "Moorland (HMP)",
    forename = "John",
    surname = "Smith",
    dateOfBirth = LocalDate.of(1985, 12, 28),
    conditionalReleaseDate = LocalDate.of(2021, 10, 22),
    actualReleaseDate = LocalDate.of(2021, 10, 22),
    sentenceStartDate = LocalDate.of(2018, 10, 22),
    sentenceEndDate = LocalDate.of(2021, 10, 22),
    licenceStartDate = LocalDate.of(2021, 10, 22),
    licenceExpiryDate = LocalDate.of(2021, 10, 22),
    topupSupervisionStartDate = LocalDate.of(2021, 10, 22),
    topupSupervisionExpiryDate = LocalDate.of(2021, 10, 22),
    probationAreaCode = "N01",
    probationAreaDescription = "Wales",
    probationPduCode = "N01A",
    probationPduDescription = "Cardiff",
    probationLauCode = "N01A2",
    probationLauDescription = "Cardiff South",
    probationTeamCode = "NA01A2-A",
    probationTeamDescription = "Cardiff South Team A",
    dateCreated = LocalDateTime.of(2022, 7, 27, 15, 0, 0),
    standardConditions = emptyList(),
    responsibleCom = CommunityOffenderManager(
      staffIdentifier = 2000,
      username = "smills",
      email = "testemail@probation.gov.uk",
      firstName = "X",
      lastName = "Y",
    ),
    createdBy = CommunityOffenderManager(
      staffIdentifier = 2000,
      username = "smills",
      email = "testemail@probation.gov.uk",
      firstName = "X",
      lastName = "Y",
    ),
  ).let {
    it.copy(
      standardConditions = someEntityStandardConditions(it),
      curfewTimes = emptyList(),
    )
  }

  fun prisonerSearchResult() = PrisonerSearchPrisoner(
    prisonerNumber = "A1234AA",
    bookingId = "123456",
    status = "ACTIVE IN",
    mostSeriousOffence = "Robbery",
    licenceExpiryDate = LocalDate.of(2021, 10, 22),
    topupSupervisionExpiryDate = LocalDate.of(2021, 10, 22),
    homeDetentionCurfewEligibilityDate = null,
    releaseDate = LocalDate.of(2021, 10, 22),
    confirmedReleaseDate = LocalDate.of(2021, 10, 22),
    conditionalReleaseDate = LocalDate.of(2021, 10, 22),
    paroleEligibilityDate = null,
    actualParoleDate = null,
    postRecallReleaseDate = null,
    legalStatus = "SENTENCED",
    indeterminateSentence = false,
    recall = false,
    prisonId = "MDI",
    locationDescription = "HMP Moorland",
    bookNumber = "12345A",
    firstName = "Bob",
    middleNames = null,
    lastName = "Mortimar",
    dateOfBirth = LocalDate.of(1985, 12, 28),
    conditionalReleaseDateOverrideDate = null,
    sentenceStartDate = LocalDate.of(2018, 10, 22),
    sentenceExpiryDate = LocalDate.of(2021, 10, 22),
    topupSupervisionStartDate = null,
    croNumber = null,
  )

  fun caseLoadItem() = CaseloadItem(
    cvl = CvlFields(
      licenceType = LicenceType.AP,
      hardStopDate = LocalDate.of(2023, 10, 12),
      hardStopWarningDate = LocalDate.of(2023, 10, 11),
      isInHardStopPeriod = true,
      isDueForEarlyRelease = true,
      isEligibleForEarlyRelease = true,
      isDueToBeReleasedInTheNextTwoWorkingDays = true,
    ),
    prisoner = Prisoner(
      prisonerNumber = "A1234AA",
      pncNumber = null,
      croNumber = null,
      bookingId = "123456",
      bookNumber = "12345A",
      firstName = "Bob",
      middleNames = null,
      lastName = "Mortimar",
      dateOfBirth = LocalDate.of(1985, 12, 28),
      status = "ACTIVE IN",
      prisonId = "MDI",
      prisonName = null,
      locationDescription = "HMP Moorland",
      legalStatus = "SENTENCED",
      imprisonmentStatus = null,
      imprisonmentStatusDescription = null,
      mostSeriousOffence = "Robbery",
      recall = false,
      indeterminateSentence = false,
      sentenceStartDate = LocalDate.of(2018, 10, 22),
      releaseDate = LocalDate.of(2021, 10, 22),
      confirmedReleaseDate = LocalDate.of(2021, 10, 22),
      sentenceExpiryDate = LocalDate.of(2021, 10, 22),
      licenceExpiryDate = LocalDate.of(2021, 10, 22),
      homeDetentionCurfewEligibilityDate = null,
      homeDetentionCurfewActualDate = null,
      homeDetentionCurfewEndDate = null,
      topupSupervisionStartDate = null,
      topupSupervisionExpiryDate = LocalDate.of(2021, 10, 22),
      paroleEligibilityDate = LocalDate.of(2021, 10, 22),
      postRecallReleaseDate = null,
      conditionalReleaseDate = LocalDate.of(2021, 10, 22),
      actualParoleDate = null,
      releaseOnTemporaryLicenceDate = null,
    ),
  )

  fun caCase() = CaCase(
    licenceId = 1,
    kind = LicenceKind.CRD,
    name = "Bob Mortimar",
    prisonerNumber = "A1234AA",
    releaseDate = LocalDate.of(2021, 10, 22),
    releaseDateLabel = "Confirmed release date",
    licenceStatus = LicenceStatus.IN_PROGRESS,
    nomisLegalStatus = "SENTENCED",
    lastWorkedOnBy = "John Smith",
    isDueForEarlyRelease = false,
    isInHardStopPeriod = false,
    tabType = CaViewCasesTab.FUTURE_RELEASES,
    probationPractitioner = ProbationPractitioner(staffUsername = "COM"),
  )
}
