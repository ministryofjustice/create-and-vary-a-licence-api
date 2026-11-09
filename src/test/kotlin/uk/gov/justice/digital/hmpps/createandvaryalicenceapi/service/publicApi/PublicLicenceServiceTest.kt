package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.publicApi

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.address.AddressSource
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.address.hdc.CvlAccommodationType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalConditionData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.BespokeCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.StandardCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.ApConditions
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.Conditions
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.PssConditions
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.PublicCurfewTimes
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.PublicHdcCurfewAddress
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.PublicHdcLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.additionalConditions.ElectronicMonitoringAdditionalConditionWithRestriction
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createCrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions.upload.UploadFileConditionsService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.mapToPublicLicenceType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.ELECTRONIC_TAG_COND_CODE_14A
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.ElectronicMonitoringType.ATTENDANCE_AT_APPOINTMENTS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.ElectronicMonitoringType.CURFEW
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.ElectronicMonitoringType.RESTRICTION_ZONE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.PolicyVersion
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.transformToResourceAdditional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.transformToResourceBespoke
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.transformToResourceStandard
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CurfewTimes as ModelCurfewTimes
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.HdcCurfewAddress as ModelHdcCurfewAddress
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.HdcLicence as ModelHdcLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.HdcVariationLicence as ModelHdcVariationLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.Licence as PublicLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.LicenceType as PublicLicenceType

class PublicLicenceServiceTest {
  private val licenceRepository = mock<LicenceRepository>()
  private val licenceService = mock<LicenceService>()
  private val uploadFileConditionsService = mock<UploadFileConditionsService>()

  private val service = PublicLicenceService(
    licenceRepository,
    licenceService,
    uploadFileConditionsService,
    publicApiHdcDetailsEnabled = true,
  )

  @BeforeEach
  fun reset() {
    reset(
      licenceRepository,
      licenceService,
      uploadFileConditionsService,
    )
  }

  @Nested
  inner class `Get licence by CRN` {
    @Test
    fun `service returns a list of licence summaries by crn`() {
      whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(
        listOf(
          aLicenceEntity,
        ),
      )

      val licenceSummaries = service.getAllLicencesByCrn("A12345")

      assertThat(licenceSummaries).hasSize(1)

      with(licenceSummaries.first()) {
        assertThat(id).isEqualTo(1L)
        assertThat(licenceType).isEqualTo(PublicLicenceType.AP)
        assertThat(policyVersion).isEqualTo(PolicyVersion.V1_0)
        assertThat(version).isEqualTo("1.4")
        assertThat(statusCode).isEqualTo(LicenceStatus.IN_PROGRESS)
        assertThat(prisonNumber).isEqualTo("A1234BC")
        assertThat(bookingId).isEqualTo(987654L)
        assertThat(crn).isEqualTo("A12345")
        assertThat(approvedByUsername).isEqualTo("testapprover")
        assertThat(approvedDateTime).isEqualTo(LocalDateTime.parse("2023-10-11T13:00"))
        assertThat(createdByUsername).isEqualTo("testcom")
        assertThat(createdDateTime).isEqualTo(LocalDateTime.parse("2023-10-11T11:00"))
        assertThat(updatedByUsername).isEqualTo("testupdater")
        assertThat(updatedDateTime).isEqualTo(LocalDateTime.parse("2023-10-11T12:00"))
        assertThat(isInPssPeriod).isFalse()
      }

      verify(licenceRepository, times(1)).findAllByCrnAndStatusCodeIn(
        aLicenceEntity.crn!!,
        LicenceStatus.IN_FLIGHT_LICENCES,
      )
    }

    @Test
    fun `service returns a list of licence summaries by crn where approved username and approved date are not present`() {
      whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(
        listOf(
          aLicenceEntity.copy(approvedByUsername = null, approvedDate = null),
        ),
      )

      val licenceSummaries = service.getAllLicencesByCrn("A12345")

      assertThat(licenceSummaries).hasSize(1)

      with(licenceSummaries.first()) {
        assertThat(id).isEqualTo(1L)
        assertThat(approvedByUsername).isNull()
        assertThat(approvedDateTime).isNull()
      }
    }

    @Test
    fun `service handles producing licence summaries with missing created dates`() {
      whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(
        listOf(
          aLicenceEntity.copy(dateCreated = null),
        ),
      )

      val licenceSummaries = service.getAllLicencesByCrn("A12345")

      assertThat(licenceSummaries).hasSize(1)

      with(licenceSummaries.first()) {
        assertThat(id).isEqualTo(1)
        assertThat(createdDateTime).isNull()
      }
    }

    @Test
    fun `service returns a list of licence summaries by crn where updated username and updated date are not present`() {
      whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(
        listOf(
          aLicenceEntity.copy(updatedByUsername = null, dateLastUpdated = null),
        ),
      )

      val licenceSummaries = service.getAllLicencesByCrn("A12345")

      assertThat(licenceSummaries).hasSize(1)

      with(licenceSummaries.first()) {
        assertThat(id).isEqualTo(1L)
        assertThat(updatedByUsername).isNull()
        assertThat(updatedDateTime).isNull()
      }
    }

    @Test
    fun `service throws an error for null fields when querying for a list of licence summaries by crn`() {
      whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(
        listOf(
          aLicenceEntity.copy(createdBy = null),
        ),
      )

      val exception = assertThrows<IllegalStateException> {
        service.getAllLicencesByCrn("A12345")
      }

      assertThat(exception).isInstanceOf(IllegalStateException::class.java)
        .hasMessage("licence: 1 has no COM/creator")
    }
  }

  @Nested
  inner class `Get licence by prison number` {
    @Test
    fun `service returns a list of licence summaries by prison number`() {
      whenever(licenceRepository.findAllByNomsIdAndStatusCodeIn(any(), any())).thenReturn(
        listOf(
          aLicenceEntity,
        ),
      )

      val licenceSummaries = service.getAllLicencesByPrisonNumber("A1234BC")

      assertThat(licenceSummaries).hasSize(1)

      with(licenceSummaries.first()) {
        assertThat(id).isEqualTo(1L)
        assertThat(licenceType).isEqualTo(PublicLicenceType.AP)
        assertThat(policyVersion).isEqualTo(PolicyVersion.V1_0)
        assertThat(version).isEqualTo("1.4")
        assertThat(statusCode).isEqualTo(LicenceStatus.IN_PROGRESS)
        assertThat(prisonNumber).isEqualTo("A1234BC")
        assertThat(bookingId).isEqualTo(987654L)
        assertThat(crn).isEqualTo("A12345")
        assertThat(approvedByUsername).isEqualTo("testapprover")
        assertThat(approvedDateTime).isEqualTo(LocalDateTime.parse("2023-10-11T13:00"))
        assertThat(createdByUsername).isEqualTo("testcom")
        assertThat(createdDateTime).isEqualTo(LocalDateTime.parse("2023-10-11T11:00"))
        assertThat(updatedByUsername).isEqualTo("testupdater")
        assertThat(updatedDateTime).isEqualTo(LocalDateTime.parse("2023-10-11T12:00"))
        assertThat(isInPssPeriod).isFalse()
      }

      verify(licenceRepository, times(1)).findAllByNomsIdAndStatusCodeIn(
        aLicenceEntity.nomsId!!,
        LicenceStatus.IN_FLIGHT_LICENCES,
      )
    }

    @Test
    fun `service throws an error for null fields when querying for a list of licence summaries by prison number`() {
      whenever(licenceRepository.findAllByNomsIdAndStatusCodeIn(any(), any())).thenReturn(
        listOf(
          aLicenceEntity.copy(licenceVersion = null),
        ),
      )

      val exception = assertThrows<IllegalStateException> {
        service.getAllLicencesByPrisonNumber("A1234BC")
      }

      assertThat(exception).isInstanceOf(IllegalStateException::class.java)
        .hasMessage("Null field retrieved: version for licence 1")
    }
  }

  @Nested
  inner class `Get exclusion zone image by condition ID` {
    @Test
    fun `service returns an exclusion zone image by condition ID`() {
      whenever(uploadFileConditionsService.getImage(1L, 2L)).thenReturn(byteArrayOf(1, 2, 3))

      val image = service.getImageUpload(1L, 2L)

      assertThat(image).isEqualTo(byteArrayOf(1, 2, 3))
    }
  }

  @Nested
  inner class `Get licence by id` {

    @Test
    fun `service returns a licence by id`() {
      // Given
      val licenceId: Long = 12345
      whenever(licenceService.getLicenceById(any())).thenReturn(modelLicence)

      // When
      val actualLicence = service.getLicenceById(licenceId)

      // Then
      assertThat(actualLicence).usingRecursiveComparison().isEqualTo(publicLicence)
    }

    @Test
    fun `service returns a hdc licence with out hdc detail when publicApiHdcDetailsEnabled is false`() {
      // Given
      val service = PublicLicenceService(
        licenceRepository,
        licenceService,
        uploadFileConditionsService,
        publicApiHdcDetailsEnabled = false,
      )
      val licenceId: Long = 12345
      whenever(licenceService.getLicenceById(any())).thenReturn(modelHdcLicence)
      // When
      val actualLicence = service.getLicenceById(licenceId)

      // Then
      assertThat(actualLicence).isInstanceOf(PublicLicence::class.java)
      assertThat(actualLicence).isNotInstanceOf(PublicHdcLicence::class.java)
    }

    @Test
    fun `service returns a hdc licence with hdc detail when publicApiHdcDetailsEnabled is true`() {
      // Given
      val service = PublicLicenceService(
        licenceRepository,
        licenceService,
        uploadFileConditionsService,
        publicApiHdcDetailsEnabled = true,
      )
      val licenceId: Long = 12345
      whenever(licenceService.getLicenceById(any())).thenReturn(modelHdcLicence)
      // When
      val actualLicence = service.getLicenceById(licenceId)

      // Then
      assertThat(actualLicence).isInstanceOf(PublicLicence::class.java)
      assertThat(actualLicence).isInstanceOf(PublicHdcLicence::class.java)
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `service returns HDC licence and variation with public curfew details`(isVariation: Boolean) {
      // Given

      val licence = if (isVariation) {
        ModelHdcVariationLicence(
          id = modelHdcLicence.id,
          typeCode = modelHdcLicence.typeCode,
          version = modelHdcLicence.version,
          statusCode = LicenceStatus.VARIATION_IN_PROGRESS,
          licenceVersion = modelHdcLicence.licenceVersion,
          weeklyCurfewTimes = modelHdcLicence.weeklyCurfewTimes,
          firstNightCurfewTimes = modelHdcLicence.firstNightCurfewTimes,
          curfewAddress = modelHdcLicence.curfewAddress,
        )
      } else {
        modelHdcLicence
      }
      whenever(licenceService.getLicenceById(12345)).thenReturn(licence)

      // When
      val actualLicence = service.getLicenceById(12345) as PublicHdcLicence

      // Then
      assertThat(actualLicence.id).isEqualTo(modelHdcLicence.id)
      assertThat(actualLicence.kind).isEqualTo(if (isVariation) "HDC_VARIATION" else "HDC")
      assertThat(actualLicence.statusCode).isEqualTo(licence.statusCode)
      assertThat(actualLicence.curfewAddress).usingRecursiveComparison().isEqualTo(
        PublicHdcCurfewAddress(
          uprn = "uprn-123",
          firstLine = "1 Test Street",
          secondLine = "Test Area",
          townOrCity = "Test Town",
          county = "Test County",
          postcode = "AB1 2CD",
        ),
      )
      assertThat(actualLicence.firstNightCurfewTimes).usingRecursiveComparison().isEqualTo(
        PublicCurfewTimes(
          fromDay = null,
          fromTime = LocalTime.of(22, 0),
          untilDay = null,
          untilTime = LocalTime.of(6, 0),
        ),
      )
      assertThat(actualLicence.curfewTimes).usingRecursiveComparison().isEqualTo(
        listOf(
          PublicCurfewTimes(
            fromDay = DayOfWeek.MONDAY,
            fromTime = LocalTime.of(20, 0),
            untilDay = DayOfWeek.TUESDAY,
            untilTime = LocalTime.of(8, 0),
          ),
          PublicCurfewTimes(
            fromDay = DayOfWeek.TUESDAY,
            fromTime = LocalTime.of(21, 0),
            untilDay = DayOfWeek.WEDNESDAY,
            untilTime = LocalTime.of(9, 0),
          ),
        ),
      )
    }

    @Test
    fun `service returns a licence by id handles EM condition 14a`() {
      val licenceId: Long = 12345
      whenever(licenceService.getLicenceById(any())).thenReturn(
        modelLicence.copy(
          additionalLicenceConditions = listOf(
            AdditionalCondition(
              id = 5,
              code = ELECTRONIC_TAG_COND_CODE_14A,
              sequence = 5,
              text = "You must wear an electronic monitoring tag for [REASON] purposes.",
              expandedText = "You must wear an electronic monitoring tag for curfew purposes.",
              data = listOf(
                AdditionalConditionData(
                  id = 1,
                  field = "electronicMonitoringTypes",
                  value = "curfew",
                  sequence = 1,
                ),
                AdditionalConditionData(
                  id = 2,
                  field = "electronicMonitoringTypes",
                  value = "that you attend appointments",
                  sequence = 2,
                ),
                AdditionalConditionData(
                  id = 3,
                  field = "electronicMonitoringTypes",
                  value = "that you do not leave areas you must stay in (restriction zones)",
                  sequence = 3,
                ),
              ),
              readyToSubmit = true,
              requiresInput = true,
            ),
          ),
        ),
      )
      val actualLicence = service.getLicenceById(licenceId)

      val additionalConditions = actualLicence.conditions.apConditions.additional
      assertThat(additionalConditions).hasSize(1)
      val conditionWithRestriction = additionalConditions[0] as ElectronicMonitoringAdditionalConditionWithRestriction

      assertThat(conditionWithRestriction.restrictions).containsExactly(
        CURFEW,
        ATTENDANCE_AT_APPOINTMENTS,
        RESTRICTION_ZONE,
      )
    }

    @Test
    fun `service throws an error for null fields when querying for a list of licence summaries by crn`() {
      whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(
        listOf(
          aLicenceEntity.copy(createdBy = null),
        ),
      )

      val exception = assertThrows<IllegalStateException> {
        service.getAllLicencesByCrn("A12345")
      }

      assertThat(exception).isInstanceOf(IllegalStateException::class.java)
        .hasMessage("licence: 1 has no COM/creator")

      verify(licenceRepository, times(1)).findAllByCrnAndStatusCodeIn(any(), any())
    }
  }

  private companion object {

    val someStandardConditions = listOf(
      StandardCondition(
        id = 1,
        code = "goodBehaviour",
        sequence = 1,
        text = "Be of good behaviour",
      ),
      StandardCondition(
        id = 2,
        code = "notBreakLaw",
        sequence = 1,
        text = "Do not break any law",
      ),
      StandardCondition(
        id = 3,
        code = "attendMeetings",
        sequence = 1,
        text = "Attend meetings",
      ),
    )

    val someAssociationData = listOf(
      AdditionalConditionData(
        id = 1,
        field = "field1",
        value = "value1",
        sequence = 1,
      ),
      AdditionalConditionData(
        id = 2,
        field = "numberOfCurfews",
        value = "value2",
        sequence = 2,
      ),
    )

    val someAdditionalConditions = listOf(
      AdditionalCondition(
        id = 1,
        code = "associateWith",
        sequence = 1,
        text = "Do not associate with [NAME] for a period of [TIME PERIOD]",
        expandedText = "Do not associate with value1 for a period of value2",
        data = someAssociationData,
        readyToSubmit = true,
        requiresInput = true,
      ),
    )

    val someBespokeConditions = listOf(
      BespokeCondition(
        id = 1,
        sequence = 1,
        text = "Bespoke one text",
      ),
      BespokeCondition(
        id = 2,
        sequence = 2,
        text = "Bespoke two text",
      ),
    )

    val modelHdcLicence = ModelHdcLicence(
      id = 12345,
      typeCode = LicenceType.AP,
      version = "2.1",
      statusCode = LicenceStatus.IN_PROGRESS,
      licenceVersion = "1.4",
      weeklyCurfewTimes = listOf(
        ModelCurfewTimes(
          curfewTimesSequence = 2,
          fromDay = DayOfWeek.TUESDAY,
          fromTime = LocalTime.of(21, 0),
          untilDay = DayOfWeek.WEDNESDAY,
          untilTime = LocalTime.of(9, 0),
          createdTimestamp = null,
        ),
        ModelCurfewTimes(
          curfewTimesSequence = 1,
          fromDay = DayOfWeek.MONDAY,
          fromTime = LocalTime.of(20, 0),
          untilDay = DayOfWeek.TUESDAY,
          untilTime = LocalTime.of(8, 0),
          createdTimestamp = null,
        ),
      ),
      firstNightCurfewTimes = ModelCurfewTimes(
        fromTime = LocalTime.of(22, 0),
        untilTime = LocalTime.of(6, 0),
        createdTimestamp = null,
      ),
      curfewAddress = ModelHdcCurfewAddress(
        uprn = "uprn-123",
        firstLine = "1 Test Street",
        secondLine = "Test Area",
        townOrCity = "Test Town",
        county = "Test County",
        postcode = "AB1 2CD",
        source = AddressSource.MANUAL,
        accommodationType = CvlAccommodationType.RESIDENTIAL,
        postReleaseResidentialChecksCompleted = false,
        postReleaseResidentialChecksNotCompletedReason = "Old reason",
        createdTimestamp = null,
        lastUpdatedTimestamp = null,
      ),
    )

    val modelLicence = CrdLicence(
      id = 1,
      typeCode = LicenceType.AP,
      version = "2.1",
      statusCode = LicenceStatus.IN_PROGRESS,
      nomsId = "A1234AA",
      bookingNo = "123456",
      bookingId = 987654,
      crn = "A12345",
      pnc = "2019/123445",
      cro = "12345",
      prisonCode = "MDI",
      prisonDescription = "Moorland (HMP)",
      forename = "Person",
      surname = "One",
      approvedByUsername = "TestApprover",
      approvedDate = LocalDateTime.of(2023, 10, 11, 12, 0),
      dateOfBirth = LocalDate.of(1985, 12, 28),
      conditionalReleaseDate = LocalDate.of(2021, 10, 22),
      actualReleaseDate = LocalDate.of(2021, 10, 22),
      sentenceStartDate = LocalDate.of(2018, 10, 22),
      sentenceEndDate = LocalDate.of(2021, 10, 22),
      licenceStartDate = LocalDate.of(2021, 10, 22),
      licenceExpiryDate = LocalDate.of(2021, 10, 22),
      topupSupervisionStartDate = LocalDate.of(2021, 10, 22),
      topupSupervisionExpiryDate = LocalDate.of(2021, 10, 22),
      dateCreated = LocalDateTime.of(2023, 10, 11, 11, 30),
      dateLastUpdated = LocalDateTime.of(2023, 10, 11, 11, 30),

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
      createdByUsername = "TestCreator",
      standardLicenceConditions = someStandardConditions,
      standardPssConditions = someStandardConditions,
      additionalLicenceConditions = someAdditionalConditions,
      additionalPssConditions = someAdditionalConditions,
      bespokeConditions = someBespokeConditions,
      licenceVersion = "1.4",
      updatedByUsername = "TestUpdater",
    )

    val aCom = CommunityOffenderManager(
      staffIdentifier = 2000,
      staffCode = "test-code",
      username = "testcom",
      email = "testcom@probation.gov.uk",
      firstName = "Test",
      lastName = "Com",
    )

    val aLicenceEntity = createCrdLicence().copy(
      id = 1L,
      crn = "A12345",
      nomsId = "A1234BC",
      version = "1.0",
      bookingId = 987654,
      forename = "Test",
      surname = "Person",
      dateOfBirth = LocalDate.parse("1985-01-01"),
      typeCode = LicenceType.AP,
      statusCode = LicenceStatus.IN_PROGRESS,
      licenceVersion = "1.4",
      approvedByUsername = "testapprover",
      approvedByName = "Test Approver",
      approvedDate = LocalDateTime.of(2023, 10, 11, 13, 0, 0),
      dateCreated = LocalDateTime.of(2023, 10, 11, 11, 0, 0),
      dateLastUpdated = LocalDateTime.of(2023, 10, 11, 12, 0, 0),
      updatedByUsername = "testupdater",
      createdBy = aCom,
    )

    val publicLicenceConditions = Conditions(
      apConditions = ApConditions(
        modelLicence.standardLicenceConditions?.transformToResourceStandard().orEmpty(),
        modelLicence.additionalLicenceConditions.transformToResourceAdditional(),
        modelLicence.bespokeConditions.transformToResourceBespoke(),
      ),
      pssConditions = PssConditions(
        modelLicence.standardPssConditions?.transformToResourceStandard().orEmpty(),
        modelLicence.additionalPssConditions.transformToResourceAdditional(),
      ),
    )
    val publicLicence = PublicLicence(
      id = modelLicence.id,
      kind = modelLicence.kind,
      licenceType = modelLicence.typeCode.mapToPublicLicenceType(),
      policyVersion = PolicyVersion.entries.find { it.version == modelLicence.version }!!,
      version = modelLicence.licenceVersion.orEmpty(),
      statusCode = modelLicence.statusCode!!,

      prisonNumber = modelLicence.nomsId.orEmpty(),
      bookingId = modelLicence.bookingId ?: 0,
      crn = modelLicence.crn.orEmpty(),
      approvedByUsername = modelLicence.approvedByUsername,
      approvedDateTime = modelLicence.approvedDate,
      createdByUsername = modelLicence.createdByUsername.orEmpty(),
      createdDateTime = modelLicence.dateCreated!!,
      updatedByUsername = modelLicence.updatedByUsername,
      updatedDateTime = modelLicence.dateLastUpdated,
      isInPssPeriod = modelLicence.isInPssPeriod ?: false,
      conditions = publicLicenceConditions,
      licenceStartDate = modelLicence.licenceStartDate,
    )
  }
}
