package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Appointment
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HardStopLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcVariationLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.PrisonUser
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.PrrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.StandardCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.VariationLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.address.Address
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.address.AddressSource
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.timeserved.TimeServedLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalConditionData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalConditionUploadSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ApprovalCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.BespokeCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CaCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.EligibilityAssessment
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceKinds
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ProbationPractitioner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.VaryApproverCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs.promptingCom.PromptCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.ELECTRONIC_TAG_COND_CODE_14A
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.EVENT_EXCLUSION_COND_CODE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.EXCLUSION_ZONE_COND_CODE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.HARD_STOP_CONDITION
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.MULTIPLE_UPLOAD_COND_CODE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.OffenceHistory
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonApiPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerHdcStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.RecallType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.SentenceAndRecallType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.SentenceDetail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.CaseloadResult
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.CommunityManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.CommunityManagerWithoutUser
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.Detail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.Name
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ProbationCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.StaffDetail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.TeamDetail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AppointmentPersonType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AppointmentTimeType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.CaViewCasesTab
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType.AP
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalCondition as ModelAdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CrdLicence as ModelCrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.HdcVariationLicence as ModelHdcVariationLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.StandardCondition as ModelStandardCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.VariationLicence as ModelVariationLicence

object TestData {
  private val TEN_DAYS_FROM_NOW = LocalDate.now().plusDays(10)

  fun communityOffenderManager() = CommunityOffenderManager(
    id = 1,
    staffIdentifier = 2000,
    staffCode = "test-code-1",
    username = "tcom1",
    email = "testemail1@probation.gov.uk",
    firstName = "X",
    lastName = "Y",
  )

  fun anotherCommunityOffenderManager() = CommunityOffenderManager(
    id = 2,
    staffIdentifier = 3000,
    staffCode = "test-code-2",
    username = "tcom2",
    email = "testemail2@probation.gov.uk",
    firstName = "A",
    lastName = "B",
  )

  fun aCommunityManagerWithoutUser() = CommunityManagerWithoutUser(
    code = "X1234",
    id = 2000L,
    team = TeamDetail(
      code = "NA01A2-A",
      description = "Cardiff South Team A",
      borough = Detail(
        code = "N01A",
        description = "Cardiff",
      ),
      district = Detail(
        code = "N01A2",
        description = "Cardiff South",
      ),
      provider = Detail(
        code = "N01",
        description = "Wales",
      ),
    ),
    provider = Detail(
      code = "N01",
      description = "Wales",
    ),
    case = ProbationCase(crn = "A123456", nomisId = "A1234AA"),
    name = Name("Joe", null, "Bloggs"),
    allocationDate = LocalDate.of(2000, 1, 1),
    unallocated = false,
  )

  fun createCommunityManager(id: Long, nomisId: String, unallocated: Boolean = true): CommunityManagerWithoutUser = CommunityManagerWithoutUser(
    id = id,
    code = "staff-code-$id",
    case = ProbationCase(
      crn = "A${id}2345",
      nomisId = nomisId,
    ),
    name = Name(
      forename = "Test$id",
      middleName = "Middle$id",
      surname = "Test$id",
    ),
    allocationDate = LocalDate.of(2022, 1, 2),
    team = TeamDetail(
      code = "team-code-$id",
      description = "staff-description-$id",
      borough = Detail("borough-code-$id", "borough-description-$id"),
      district = Detail("district-code-$id", "district-description-$id"),
      provider = Detail("probationArea-code-$id", "probationArea-description-$id"),
    ),
    provider = Detail("probationArea-code-$id", "probationArea-description-$id"),
    unallocated = unallocated,
  )

  fun anAdditionalCondition(id: Long, licence: Licence) = AdditionalCondition(
    id = id,
    licence = licence,
    conditionType = "AP",
    conditionCode = "de9306cb-cf1f-41e0-bef8-49dc369945c9",
    conditionCategory = "condition category",
    conditionText = "condition text",
    conditionVersion = "1.0",
  )

  fun aCvlRecord(
    nomsId: String = "A1234AA",
    licenceStartDate: LocalDate? = LocalDate.of(2021, 10, 22),
    kind: LicenceKind? = LicenceKind.CRD,
    isEligible: Boolean = true,
    hardStopKind: LicenceKind? = null,
    hardStopDate: LocalDate? = null,
    hardStopWarningDate: LocalDate? = null,
    isEligibleForEarlyRelease: Boolean = false,
    isInHardStopPeriod: Boolean = false,
    isDueToBeReleasedInTheNextTwoWorkingDays: Boolean = false,
    licenceType: LicenceType = AP,
  ) = CvlRecord(
    nomisId = nomsId,
    licenceStartDate = licenceStartDate,
    isEligible = isEligible,
    eligibleKind = kind,
    ineligibilityReasons = emptyList(),
    hardStopKind = hardStopKind,
    isEligibleForEarlyRelease = isEligibleForEarlyRelease,
    isInHardStopPeriod = isInHardStopPeriod,
    isDueToBeReleasedInTheNextTwoWorkingDays = isDueToBeReleasedInTheNextTwoWorkingDays,
    hardStopDate = hardStopDate,
    hardStopWarningDate = hardStopWarningDate,
    licenceType = licenceType,
  )

  fun anEligibilityAssessment() = EligibilityAssessment(
    genericIneligibilityReasons = emptyList(),
    crdIneligibilityReasons = emptyList(),
    prrdIneligibilityReasons = emptyList(),
    hdcIneligibilityReasons = listOf("An HDC reason"),
  )

  fun anIneligibleEligibilityAssessment() = EligibilityAssessment(
    genericIneligibilityReasons = listOf("A CRD reason", "A PRRD reason"),
    crdIneligibilityReasons = listOf("A CRD reason"),
    prrdIneligibilityReasons = listOf("A PRRD reason"),
    hdcIneligibilityReasons = listOf("An HDC reason"),
  )

  private fun hardStopAdditionalCondition(licence: Licence) = AdditionalCondition(
    licence = licence,
    id = 2L,
    conditionSequence = 1,
    conditionType = "AP",
    conditionCode = HARD_STOP_CONDITION.code,
    conditionText = HARD_STOP_CONDITION.text,
    expandedConditionText = HARD_STOP_CONDITION.text,
    conditionVersion = licence.version!!,
    additionalConditionData = mutableListOf(),
    additionalConditionUpload = mutableListOf(),
    conditionCategory = HARD_STOP_CONDITION.categoryShort!!,
  )

  private fun someEntityStandardConditions(licence: Licence) = listOf(
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

  fun createPrrdLicence() = PrrdLicence(
    id = 1,
    typeCode = AP,
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
    actualReleaseDate = LocalDate.of(2021, 9, 22),
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
    responsibleCom = communityOffenderManager(),
    createdBy = communityOffenderManager(),
    postRecallReleaseDate = LocalDate.now(),
  ).let {
    it.copy(standardConditions = someEntityStandardConditions(it))
  }

  fun createCrdLicence() = CrdLicence(
    id = 1,
    typeCode = AP,
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
    responsibleCom = communityOffenderManager(),
    createdBy = communityOffenderManager(),
  ).let {
    it.copy(standardConditions = someEntityStandardConditions(it))
  }

  fun createHardStopLicence() = HardStopLicence(
    id = 1,
    eligibleKind = LicenceKind.CRD,
    typeCode = AP,
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
    responsibleCom = communityOffenderManager(),
    createdBy = prisonUser(),
  ).let {
    it.copy(
      standardConditions = someEntityStandardConditions(it),
      additionalConditions = listOf(hardStopAdditionalCondition(it)),
    )
  }

  fun createTimeServedLicence() = TimeServedLicence(
    id = 1,
    eligibleKind = LicenceKind.CRD,
    typeCode = AP,
    version = "2.1",
    statusCode = LicenceStatus.ACTIVE,
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
    responsibleCom = communityOffenderManager(),
    createdBy = prisonUser(),
  ).let {
    it.copy(
      standardConditions = someEntityStandardConditions(it),
      additionalConditions = listOf(hardStopAdditionalCondition(it)),
    )
  }

  fun createVariationLicence() = VariationLicence(
    id = 1,
    typeCode = AP,
    eligibleKind = LicenceKind.CRD,
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
    responsibleCom = communityOffenderManager(),
    createdBy = communityOffenderManager(),
    appointment = createAppointment(),
  ).let {
    it.copy(standardConditions = someEntityStandardConditions(it))
  }

  fun createAddress(
    reference: String = "REF-${UUID.randomUUID()}",
    firstLine: String = "123 Test Street",
    secondLine: String? = null,
    townOrCity: String = "Testville",
    county: String? = "Testshire",
    postcode: String = "TE5 7AA",
    source: AddressSource = AddressSource.MANUAL,
    created: LocalDateTime = LocalDateTime.now(),
    updated: LocalDateTime = created,
  ): Address = Address(
    reference = reference,
    firstLine = firstLine,
    secondLine = secondLine,
    townOrCity = townOrCity,
    county = county,
    postcode = postcode,
    source = source,
    createdTimestamp = created,
    lastUpdatedTimestamp = updated,
  )

  fun createAppointment(
    id: Long? = null,
    personType: AppointmentPersonType? = AppointmentPersonType.SPECIFIC_PERSON,
    person: String? = if (personType == AppointmentPersonType.SPECIFIC_PERSON) "Test Officer" else null,
    timeType: AppointmentTimeType? = AppointmentTimeType.SPECIFIC_DATE_TIME,
    time: LocalDateTime? = LocalDateTime.now().plusDays(1),
    telephoneContactNumber: String? = "07123456789",
    alternativeTelephoneContactNumber: String? = "07000000000",
    addressText: String? = "Meeting at Testville Probation Office",
    address: Address? = createAddress(),
    created: LocalDateTime = LocalDateTime.now(),
    updated: LocalDateTime = created,
  ): Appointment = Appointment(
    id = id,
    personType = personType,
    person = person,
    timeType = timeType,
    time = time,
    telephoneContactNumber = telephoneContactNumber,
    alternativeTelephoneContactNumber = alternativeTelephoneContactNumber,
    addressText = addressText,
    address = address,
    dateCreated = created,
    dateLastUpdated = updated,
  )

  fun createHdcLicence() = HdcLicence(
    id = 1,
    typeCode = AP,
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
    homeDetentionCurfewActualDate = LocalDate.of(2021, 10, 22),
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
    responsibleCom = communityOffenderManager(),
    createdBy = communityOffenderManager(),
  ).let {
    it.copy(
      standardConditions = someEntityStandardConditions(it),
      curfewTimes = emptyList(),
    )
  }

  fun createHdcVariationLicence() = HdcVariationLicence(
    id = 1,
    typeCode = AP,
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
    homeDetentionCurfewActualDate = LocalDate.of(2021, 10, 22),
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
    appointment = createAppointment(),
    responsibleCom = communityOffenderManager(),
    createdBy = communityOffenderManager(),
  ).let {
    it.copy(standardConditions = someEntityStandardConditions(it), curfewTimes = mutableListOf())
  }

  fun prisonerSearchResult(
    prisonerNumber: String = "A1234AA",
    conditionalReleaseDate: LocalDate? = LocalDate.of(2021, 10, 22),
    conditionalReleaseDateOverrideDate: LocalDate? = null,
    sentenceStartDate: LocalDate? = LocalDate.of(2018, 10, 22),
    confirmedReleaseDate: LocalDate? = LocalDate.of(2021, 10, 22),
    postRecallReleaseDate: LocalDate? = null,
  ) = PrisonerSearchPrisoner(
    prisonerNumber = prisonerNumber,
    bookingId = "123456",
    status = "ACTIVE IN",
    mostSeriousOffence = "Robbery",
    licenceExpiryDate = LocalDate.of(2021, 10, 22),
    topupSupervisionExpiryDate = LocalDate.of(2021, 10, 22),
    releaseDate = LocalDate.of(2021, 10, 22),
    confirmedReleaseDate = confirmedReleaseDate,
    conditionalReleaseDate = conditionalReleaseDate,
    conditionalReleaseDateOverrideDate = conditionalReleaseDateOverrideDate,
    legalStatus = "SENTENCED",
    indeterminateSentence = false,
    recall = false,
    prisonId = "MDI",
    locationDescription = "HMP Moorland",
    bookNumber = "12345A",
    firstName = "A",
    lastName = "Prisoner",
    dateOfBirth = LocalDate.of(1985, 12, 28),
    sentenceStartDate = sentenceStartDate,
    sentenceExpiryDate = LocalDate.of(2021, 10, 22),
    postRecallReleaseDate = postRecallReleaseDate,
    homeDetentionCurfewEligibilityDate = null,
    homeDetentionCurfewActualDate = null,
    paroleEligibilityDate = null,
    actualParoleDate = null,
  )

  fun createPrisonerSearchResult(
    today: LocalDate = LocalDate.now(),
    tomorrow: LocalDate = today.plusDays(1),
    prisonerNumber: String = "A1234AA",
    bookingId: String = "123",
    status: String = "ACTIVE",
    prisonId: String = "ABC",
    bookNumber: String = "12345A",
    firstName: String = "Test1",
    lastName: String = "Person1",
    dateOfBirth: LocalDate = LocalDate.of(1985, 1, 1),
    mostSeriousOffence: String = "Robbery",
    licenceExpiryDate: LocalDate? = today.plusYears(1),
    topupSupervisionExpiryDate: LocalDate? = licenceExpiryDate,
    homeDetentionCurfewEligibilityDate: LocalDate? = null,
    releaseDate: LocalDate? = tomorrow,
    confirmedReleaseDate: LocalDate? = null,
    conditionalReleaseDate: LocalDate? = null,
    paroleEligibilityDate: LocalDate? = null,
    actualParoleDate: LocalDate? = null,
    postRecallReleaseDate: LocalDate? = null,
    legalStatus: String = "SENTENCED",
    indeterminateSentence: Boolean = false,
    recall: Boolean = false,
  ): PrisonerSearchPrisoner = PrisonerSearchPrisoner(
    prisonerNumber = prisonerNumber,
    bookingId = bookingId,
    status = status,
    mostSeriousOffence = mostSeriousOffence,
    licenceExpiryDate = licenceExpiryDate,
    topupSupervisionExpiryDate = topupSupervisionExpiryDate,
    homeDetentionCurfewEligibilityDate = homeDetentionCurfewEligibilityDate,
    releaseDate = releaseDate,
    confirmedReleaseDate = confirmedReleaseDate,
    conditionalReleaseDate = conditionalReleaseDate,
    paroleEligibilityDate = paroleEligibilityDate,
    actualParoleDate = actualParoleDate,
    postRecallReleaseDate = postRecallReleaseDate,
    legalStatus = legalStatus,
    indeterminateSentence = indeterminateSentence,
    recall = recall,
    prisonId = prisonId,
    bookNumber = bookNumber,
    firstName = firstName,
    lastName = lastName,
    dateOfBirth = dateOfBirth,
  )

  fun aPrisonApiPrisoner() = PrisonApiPrisoner(
    offenderNo = "A1234AA",
    firstName = "A",
    lastName = "Prisoner",
    dateOfBirth = LocalDate.of(1985, 12, 28),
    bookingId = 123456,
    offenceHistory = listOf(
      anOffenceHistory(),
    ),
    legalStatus = "SENTENCED",
    sentenceDetail = SentenceDetail(
      confirmedReleaseDate = LocalDate.of(2021, 10, 22),
      conditionalReleaseDate = LocalDate.of(2021, 10, 22),
      sentenceExpiryDate = LocalDate.of(2023, 10, 22),
      licenceExpiryDate = LocalDate.of(2023, 10, 22),
    ),
    agencyId = "ABC",
    status = "ACTIVE IN",
  )

  private fun anOffenceHistory() = OffenceHistory(
    offenceDescription = "SOME_OFFENCE",
    offenceCode = "123",
    mostSerious = true,
  )

  fun promptCase() = PromptCase(
    prisoner = prisonerSearchResult(),
    crn = "A1234",
    comStaffCode = "B1234",
    comName = "John Doe",
    comAllocationDate = LocalDate.parse("2025-01-27"),
  )

  fun offenderManager() = CommunityManager(
    code = "staff-code-1",
    id = 2000L,
    team = TeamDetail(
      code = "NA01A2-A",
      description = "Cardiff South Team A",
      borough = Detail(
        code = "N01A",
        description = "Cardiff",
      ),
      district = Detail(
        code = "N01A2",
        description = "Cardiff South",
      ),
      provider = Detail(
        code = "N01",
        description = "Wales",
      ),
    ),
    provider = Detail(
      code = "N01",
      description = "Wales",
    ),
    case = ProbationCase("crn-1", "A1234AA"),
    name = Name("forenames", null, "surname"),
    allocationDate = LocalDate.of(2022, 1, 2),
    unallocated = false,
    username = "aComUser",
  )

  fun caseloadResult() = CaseloadResult(
    "A123456",
    "A1234AA",
    name = Name("Test", surname = "Surname"),
    staff = StaffDetail(
      "A01B02C",
      Name("Staff", surname = "Surname"),
    ),
    team = TeamDetail(
      "A01B02",
      "Test Team",
      Detail("B01", "Test borough"),
      Detail("D01", "Test district"),
      Detail("P01", "Test provider"),
    ),
    allocationDate = LocalDate.of(2023, 5, 24),
  )

  fun caCase() = CaCase(
    licenceId = 1,
    kind = LicenceKind.CRD,
    name = "A Prisoner",
    prisonerNumber = "A1234AA",
    releaseDate = LocalDate.of(2021, 10, 22),
    releaseDateLabel = "Confirmed release date",
    licenceStatus = LicenceStatus.IN_PROGRESS,
    nomisLegalStatus = "SENTENCED",
    lastWorkedOnBy = "John Smith",
    isInHardStopPeriod = false,
    tabType = CaViewCasesTab.FUTURE_RELEASES,
    probationPractitioner = ProbationPractitioner(allocated = true),
  )

  fun approvalCase() = ApprovalCase(
    licenceId = 1,
    name = "A Prisoner",
    prisonerNumber = "A1234AA",
    submittedByFullName = "John Smith",
    releaseDate = LocalDate.of(2021, 10, 22),
    urgentApproval = false,
    approvedBy = null,
    approvedOn = null,
    probationPractitioner = ProbationPractitioner(allocated = true),
    kind = LicenceKind.CRD,
    prisonCode = "MDI",
    prisonDescription = "Moorland (HMP)",
  )

  fun varyApprovalCase() = VaryApproverCase(
    licenceId = 1,
    name = "A Prisoner",
    crnNumber = "X12348",
    licenceType = AP,
    variationRequestDate = LocalDate.of(2023, 11, 24),
    releaseDate = LocalDate.of(2021, 10, 22),
    probationPractitioner = ProbationPractitioner(allocated = true),
  )

  fun hdcPrisonerStatus() = PrisonerHdcStatus(
    approvalStatus = "REJECTED",
    bookingId = 1,
    passed = true,
  )

  private fun someModelStandardConditions() = listOf(
    ModelStandardCondition(
      id = 1,
      code = "goodBehaviour",
      sequence = 1,
      text = "Be of good behaviour",
    ),
    ModelStandardCondition(
      id = 2,
      code = "notBreakLaw",
      sequence = 1,
      text = "Do not break any law",
    ),
    ModelStandardCondition(
      id = 3,
      code = "attendMeetings",
      sequence = 1,
      text = "Attend meetings",
    ),
  )

  fun aSentenceAndRecallType(recallType: RecallType = aRecallType()) = SentenceAndRecallType(
    sentenceType = "A_SENTENCE",
    recallType,
  )

  fun aRecallType(isStandardRecall: Boolean = false, isFixedTermRecall: Boolean = true) = RecallType(
    recallName = "A_RECALL",
    isStandardRecall,
    isFixedTermRecall,
  )

  private fun someModelAssociationData() = listOf(
    AdditionalConditionData(id = 1, field = "field1", value = "value1", sequence = 1),
    AdditionalConditionData(id = 2, field = "numberOfCurfews", value = "value2", sequence = 2),
  )

  fun someOldModelAdditionalConditions() = listOf(
    ModelAdditionalCondition(
      id = 1,
      code = "599bdcae-d545-461c-b1a9-02cb3d4ba268",
      readyToSubmit = true,
      requiresInput = true,
    ),
  )

  fun someModelAdditionalConditions() = listOf(
    ModelAdditionalCondition(
      id = 1,
      category = "Associates",
      code = "associateWith",
      sequence = 1,
      text = "Do not associate with [NAME] for a period of [TIME PERIOD]",
      expandedText = "Do not associate with value1 for a period of value2",
      data = someModelAssociationData(),
      readyToSubmit = true,
      requiresInput = true,
    ),
    ModelAdditionalCondition(
      id = 2,
      category = "FreedomOfMovement",
      code = EXCLUSION_ZONE_COND_CODE,
      sequence = 2,
      text = "Do not enter the area defined in the attached map.",
      expandedText = "Do not enter the area defined in the attached map.",
      data = someModelAssociationData(),
      readyToSubmit = true,
      requiresInput = true,
      uploadSummary = listOf(
        AdditionalConditionUploadSummary(
          id = 1,
          fileSize = 1,
        ),
      ),
    ),
    ModelAdditionalCondition(
      id = 3,
      category = "FreedomOfMovement",
      code = EXCLUSION_ZONE_COND_CODE,
      sequence = 3,
      text = "Do not enter the area defined in the attached map.",
      expandedText = "Do not enter the area defined in the attached map.",
      data = someModelAssociationData(),
      readyToSubmit = true,
      requiresInput = true,
      uploadSummary = listOf(
        AdditionalConditionUploadSummary(
          id = 2,
          fileSize = 1,
        ),
      ),
    ),
    ModelAdditionalCondition(
      id = 4,
      category = "FreedomOfMovement",
      code = EVENT_EXCLUSION_COND_CODE,
      sequence = 4,
      text = "Do not enter the area defined in the attached map for the duration of [EVENT NAME].",
      expandedText = "Do not enter the area defined in the attached map for the duration of The Event.",
      data = someModelAssociationData(),
      readyToSubmit = true,
      requiresInput = true,
      uploadSummary = listOf(
        AdditionalConditionUploadSummary(
          id = 3,
          fileSize = 1,
        ),
      ),
    ),
    ModelAdditionalCondition(
      id = 5,
      code = ELECTRONIC_TAG_COND_CODE_14A,
      sequence = 5,
      text = "You must wear an electronic monitoring tag for [REASON] purposes.",
      expandedText = "You must wear an electronic monitoring tag for curfew purposes.",
      data = listOf(
        AdditionalConditionData(
          id = 5,
          field = "electronicMonitoringTypes",
          value = "curfew",
          sequence = 1,
        ),
      ),
      readyToSubmit = true,
      requiresInput = true,
    ),
    ModelAdditionalCondition(
      id = 6,
      category = "FreedomOfMovement",
      code = MULTIPLE_UPLOAD_COND_CODE,
      sequence = 6,
      text = "Do not enter the area defined in the attached map.",
      expandedText = "Do not enter the area defined in the attached map.",
      data = someModelAssociationData(),
      readyToSubmit = true,
      requiresInput = true,
      uploadSummary = listOf(
        AdditionalConditionUploadSummary(
          id = 2,
          fileSize = 1,
        ),
      ),
    ),
  )

  private fun someModelBespokeConditions() = listOf(
    BespokeCondition(id = 1, sequence = 1, text = "Bespoke one text"),
    BespokeCondition(id = 2, sequence = 2, text = "Bespoke two text"),
  )

  fun aModelLicence() = ModelCrdLicence(
    id = 1,
    typeCode = AP,
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
    forename = "Person",
    surname = "One",
    dateOfBirth = LocalDate.of(1985, 12, 28),
    conditionalReleaseDate = LocalDate.of(2021, 10, 22),
    actualReleaseDate = LocalDate.of(2021, 10, 22),
    sentenceStartDate = LocalDate.of(2018, 10, 22),
    sentenceEndDate = LocalDate.of(2021, 10, 22),
    licenceStartDate = LocalDate.of(2021, 10, 22),
    licenceExpiryDate = LocalDate.of(2021, 10, 22),
    topupSupervisionStartDate = LocalDate.of(2021, 10, 22),
    topupSupervisionExpiryDate = LocalDate.of(2021, 10, 22),
    comUsername = "X12345",
    comStaffId = 12345,
    comEmail = "test.com@probation.gov.uk",
    probationAreaCode = "N01",
    probationAreaDescription = "Wales",
    probationPduCode = "N01A",
    probationPduDescription = "Cardiff",
    probationLauCode = "N01A2",
    probationLauDescription = "Cardiff South",
    probationTeamCode = "NA01A2-A",
    probationTeamDescription = "Cardiff South Team A",
    dateCreated = LocalDateTime.now(),
    createdByUsername = "X12345",
    standardLicenceConditions = someModelStandardConditions(),
    standardPssConditions = someModelStandardConditions(),
    additionalLicenceConditions = someModelAdditionalConditions(),
    additionalPssConditions = someModelAdditionalConditions(),
    bespokeConditions = someModelBespokeConditions(),
  )

  fun aModelVariation() = ModelVariationLicence(
    id = 2,
    typeCode = AP,
    eligibleKind = LicenceKinds.CRD,
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
    forename = "Person",
    surname = "One",
    dateOfBirth = LocalDate.of(1985, 12, 28),
    conditionalReleaseDate = LocalDate.of(2021, 10, 22),
    actualReleaseDate = LocalDate.of(2021, 10, 22),
    sentenceStartDate = LocalDate.of(2018, 10, 22),
    sentenceEndDate = LocalDate.of(2021, 10, 22),
    licenceStartDate = LocalDate.of(2021, 10, 22),
    licenceExpiryDate = LocalDate.of(2021, 10, 22),
    topupSupervisionStartDate = LocalDate.of(2021, 10, 22),
    topupSupervisionExpiryDate = LocalDate.of(2021, 10, 22),
    comUsername = "X12345",
    comStaffId = 12345,
    comEmail = "test.com@probation.gov.uk",
    probationAreaCode = "N01",
    probationAreaDescription = "Wales",
    probationPduCode = "N01A",
    probationPduDescription = "Cardiff",
    probationLauCode = "N01A2",
    probationLauDescription = "Cardiff South",
    probationTeamCode = "NA01A2-A",
    probationTeamDescription = "Cardiff South Team A",
    dateCreated = LocalDateTime.now(),
    createdByUsername = "X12345",
    standardLicenceConditions = someModelStandardConditions(),
    standardPssConditions = someModelStandardConditions(),
    additionalLicenceConditions = someModelAdditionalConditions(),
    additionalPssConditions = someModelAdditionalConditions(),
    bespokeConditions = someModelBespokeConditions(),
    variationOf = 1,
  )

  fun aModelHdcVariation() = ModelHdcVariationLicence(
    id = 2,
    typeCode = AP,
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
    forename = "Person",
    surname = "One",
    dateOfBirth = LocalDate.of(1985, 12, 28),
    conditionalReleaseDate = LocalDate.of(2021, 10, 22),
    actualReleaseDate = LocalDate.of(2021, 10, 22),
    sentenceStartDate = LocalDate.of(2018, 10, 22),
    sentenceEndDate = LocalDate.of(2021, 10, 22),
    licenceStartDate = LocalDate.of(2021, 10, 22),
    licenceExpiryDate = LocalDate.of(2021, 10, 22),
    topupSupervisionStartDate = LocalDate.of(2021, 10, 22),
    topupSupervisionExpiryDate = LocalDate.of(2021, 10, 22),
    comUsername = "X12345",
    comStaffId = 12345,
    comEmail = "test.com@probation.gov.uk",
    probationAreaCode = "N01",
    probationAreaDescription = "Wales",
    probationPduCode = "N01A",
    probationPduDescription = "Cardiff",
    probationLauCode = "N01A2",
    probationLauDescription = "Cardiff South",
    probationTeamCode = "NA01A2-A",
    probationTeamDescription = "Cardiff South Team A",
    dateCreated = LocalDateTime.now(),
    createdByUsername = "X12345",
    standardLicenceConditions = someModelStandardConditions(),
    standardPssConditions = someModelStandardConditions(),
    additionalLicenceConditions = someModelAdditionalConditions(),
    additionalPssConditions = someModelAdditionalConditions(),
    bespokeConditions = someModelBespokeConditions(),
    variationOf = 1,
    homeDetentionCurfewActualDate = LocalDate.of(2021, 10, 22),
    homeDetentionCurfewEndDate = LocalDate.of(2022, 10, 22),
  )

  fun aLicenceSummary(
    id: Long = 1,
    kind: LicenceKind = LicenceKind.CRD,
    type: LicenceType = LicenceType.AP_PSS,
    status: LicenceStatus = LicenceStatus.IN_PROGRESS,
    nomsId: String = "AB1234E",
    startDate: LocalDate = TEN_DAYS_FROM_NOW,
  ) = LicenceSummary(
    licenceId = id,
    kind = kind,
    licenceType = type,
    nomisId = nomsId,
    licenceStatus = status,
    comUsername = "joebloggs",
    forename = "test",
    surname = "user",
    crn = "X12345",
    dateOfBirth = LocalDate.of(1985, 12, 28),
    prisonCode = "MDI",
    prisonDescription = "Moorland (HMP)",
    probationAreaCode = "N01",
    probationAreaDescription = "Wales",
    probationPduCode = "N01A",
    probationPduDescription = "Cardiff",
    probationLauCode = "N01A2",
    probationLauDescription = "Cardiff South",
    probationTeamCode = "NA01A2-A",
    probationTeamDescription = "Cardiff South Team A",
    conditionalReleaseDate = LocalDate.of(2022, 12, 28),
    actualReleaseDate = LocalDate.of(2022, 12, 30),
    sentenceStartDate = LocalDate.of(2018, 10, 22),
    sentenceEndDate = LocalDate.of(2021, 10, 22),
    licenceStartDate = startDate,
    licenceExpiryDate = LocalDate.of(2021, 10, 22),
    topupSupervisionStartDate = LocalDate.of(2021, 10, 22),
    topupSupervisionExpiryDate = LocalDate.of(2021, 10, 22),
    bookingId = 54321,
    dateCreated = LocalDateTime.of(2022, 7, 27, 15, 0, 0),
    approvedByName = "approver name",
    approvedDate = LocalDateTime.of(2023, 9, 19, 16, 38, 42),
    licenceVersion = "1.0",
    isReviewNeeded = false,
    updatedByFullName = "X Y",
  )

  fun aProbationCase(nomisId: String = "AB1234E", crn: String = "X12348") = ProbationCase(
    nomisId = nomisId,
    crn = crn,
    croNumber = "AB01/234567C",
  )

  fun prisonUser(): PrisonUser = PrisonUser(
    username = "tca",
    email = "testemail@probation.gov.uk",
    firstName = "X",
    lastName = "Y",
  )
}
