package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.publicApi

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple.tuple
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.core.io.ClassPathResource
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionUploadDetail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionUploadSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionUploadDetailRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.ApConditions
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.Conditions
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.PssConditions
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createCrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.mapToPublicLicenceType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.PolicyVersion
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.transformToResourceAdditional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.transformToResourceBespoke
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.transformToResourceStandard
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.Licence as PublicLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.LicenceStatus as PublicLicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.LicenceSummary as ModelPublicLicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.LicenceType as PublicLicenceType

class PublicLicenceServiceTest {
  private val licenceRepository = mock<LicenceRepository>()
  private val additionalConditionRepository = mock<AdditionalConditionRepository>()
  private val additionalConditionUploadDetailRepository = mock<AdditionalConditionUploadDetailRepository>()
  private val licenceService = mock<LicenceService>()

  private val service = PublicLicenceService(
    licenceRepository,
    additionalConditionRepository,
    additionalConditionUploadDetailRepository,
    licenceService,
  )

  @BeforeEach
  fun reset() {
    reset(
      licenceRepository,
      additionalConditionRepository,
      additionalConditionUploadDetailRepository,
      licenceService,
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
      val licenceSummary = licenceSummaries.first()

      assertThat(licenceSummaries.size).isEqualTo(1)

      assertThat(licenceSummary).isExactlyInstanceOf(ModelPublicLicenceSummary::class.java)

      assertThat(licenceSummary).extracting {
        tuple(
          it.id, it.licenceType, it.policyVersion, it.version, it.statusCode, it.prisonNumber, it.bookingId,
          it.crn, it.approvedByUsername, it.approvedDateTime, it.createdByUsername, it.createdDateTime,
          it.updatedByUsername, it.updatedDateTime, it.isInPssPeriod,
        )
      }.isEqualTo(
        tuple(
          1L,
          PublicLicenceType.AP,
          PolicyVersion.V1_0,
          "1.4",
          PublicLicenceStatus.IN_PROGRESS,
          "A1234BC",
          987654L,
          "A12345",
          "testapprover",
          LocalDateTime.parse("2023-10-11T13:00"),
          "testcom",
          LocalDateTime.parse("2023-10-11T11:00"),
          "testupdater",
          LocalDateTime.parse("2023-10-11T12:00"),
          false,
        ),
      )

      verify(licenceRepository, times(1)).findAllByCrnAndStatusCodeIn(any(), any())
    }

    @Test
    fun `service returns a list of licence summaries by crn where approved username and approved date are not present`() {
      whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(
        listOf(
          aLicenceEntity.copy(approvedByUsername = null, approvedDate = null),
        ),
      )

      val licenceSummaries = service.getAllLicencesByCrn("A12345")
      val licenceSummary = licenceSummaries.first()

      assertThat(licenceSummaries.size).isEqualTo(1)

      assertThat(licenceSummary).isExactlyInstanceOf(ModelPublicLicenceSummary::class.java)

      assertThat(licenceSummary).extracting {
        tuple(
          it.id, it.licenceType, it.policyVersion, it.version, it.statusCode, it.prisonNumber, it.bookingId,
          it.crn, it.approvedByUsername, it.approvedDateTime, it.createdByUsername, it.createdDateTime,
          it.updatedByUsername, it.updatedDateTime, it.isInPssPeriod,
        )
      }.isEqualTo(
        tuple(
          1L,
          PublicLicenceType.AP,
          PolicyVersion.V1_0,
          "1.4",
          PublicLicenceStatus.IN_PROGRESS,
          "A1234BC",
          987654L,
          "A12345",
          null,
          null,
          "testcom",
          LocalDateTime.parse("2023-10-11T11:00"),
          "testupdater",
          LocalDateTime.parse("2023-10-11T12:00"),
          false,
        ),
      )

      verify(licenceRepository, times(1)).findAllByCrnAndStatusCodeIn(any(), any())
    }

    @Test
    fun `service returns a list of licence summaries by crn where updated username and updated date are not present`() {
      whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(
        listOf(
          aLicenceEntity.copy(updatedByUsername = null, dateLastUpdated = null),
        ),
      )

      val licenceSummaries = service.getAllLicencesByCrn("A12345")
      val licenceSummary = licenceSummaries.first()

      assertThat(licenceSummaries.size).isEqualTo(1)

      assertThat(licenceSummary).isExactlyInstanceOf(ModelPublicLicenceSummary::class.java)

      assertThat(licenceSummary).extracting {
        tuple(
          it.id, it.licenceType, it.policyVersion, it.version, it.statusCode, it.prisonNumber, it.bookingId,
          it.crn, it.approvedByUsername, it.approvedDateTime, it.createdByUsername, it.createdDateTime,
          it.updatedByUsername, it.updatedDateTime, it.isInPssPeriod,
        )
      }.isEqualTo(
        tuple(
          1L,
          PublicLicenceType.AP,
          PolicyVersion.V1_0,
          "1.4",
          PublicLicenceStatus.IN_PROGRESS,
          "A1234BC",
          987654L,
          "A12345",
          "testapprover",
          LocalDateTime.parse("2023-10-11T13:00"),
          "testcom",
          LocalDateTime.parse("2023-10-11T11:00"),
          null,
          null,
          false,
        ),
      )

      verify(licenceRepository, times(1)).findAllByCrnAndStatusCodeIn(any(), any())
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

    @Test
    fun `service throws an error for an unmapped field when querying a list of licence summaries by crn`() {
      whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(
        listOf(
          aLicenceEntity.copy(statusCode = LicenceStatus.NOT_STARTED),
        ),
      )

      val exception = assertThrows<IllegalStateException> {
        service.getAllLicencesByCrn("A12345")
      }

      assertThat(exception).isInstanceOf(IllegalStateException::class.java)
        .hasMessage("No matching licence status found")

      verify(licenceRepository, times(1)).findAllByCrnAndStatusCodeIn(any(), any())
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
      val licenceSummary = licenceSummaries.first()

      assertThat(licenceSummaries.size).isEqualTo(1)

      assertThat(licenceSummary).isExactlyInstanceOf(ModelPublicLicenceSummary::class.java)

      assertThat(licenceSummary).extracting {
        tuple(
          it.id, it.licenceType, it.policyVersion, it.version, it.statusCode, it.prisonNumber, it.bookingId,
          it.crn, it.approvedByUsername, it.approvedDateTime, it.createdByUsername, it.createdDateTime,
          it.updatedByUsername, it.updatedDateTime, it.isInPssPeriod,
        )
      }.isEqualTo(
        tuple(
          1L,
          PublicLicenceType.AP,
          PolicyVersion.V1_0,
          "1.4",
          PublicLicenceStatus.IN_PROGRESS,
          "A1234BC",
          987654L,
          "A12345",
          "testapprover",
          LocalDateTime.parse("2023-10-11T13:00"),
          "testcom",
          LocalDateTime.parse("2023-10-11T11:00"),
          "testupdater",
          LocalDateTime.parse("2023-10-11T12:00"),
          false,
        ),
      )

      verify(licenceRepository, times(1)).findAllByNomsIdAndStatusCodeIn(any(), any())
    }

    @Test
    fun `service returns a list of licence summaries by prison number where approved username and approved date are not present`() {
      whenever(licenceRepository.findAllByNomsIdAndStatusCodeIn(any(), any())).thenReturn(
        listOf(
          aLicenceEntity.copy(approvedByUsername = null, approvedDate = null),
        ),
      )

      val licenceSummaries = service.getAllLicencesByPrisonNumber("A1234BC")
      val licenceSummary = licenceSummaries.first()

      assertThat(licenceSummaries.size).isEqualTo(1)

      assertThat(licenceSummary).isExactlyInstanceOf(ModelPublicLicenceSummary::class.java)

      assertThat(licenceSummary).extracting {
        tuple(
          it.id, it.licenceType, it.policyVersion, it.version, it.statusCode, it.prisonNumber, it.bookingId,
          it.crn, it.approvedByUsername, it.approvedDateTime, it.createdByUsername, it.createdDateTime,
          it.updatedByUsername, it.updatedDateTime, it.isInPssPeriod,
        )
      }.isEqualTo(
        tuple(
          1L,
          PublicLicenceType.AP,
          PolicyVersion.V1_0,
          "1.4",
          PublicLicenceStatus.IN_PROGRESS,
          "A1234BC",
          987654L,
          "A12345",
          null,
          null,
          "testcom",
          LocalDateTime.parse("2023-10-11T11:00"),
          "testupdater",
          LocalDateTime.parse("2023-10-11T12:00"),
          false,
        ),
      )

      verify(licenceRepository, times(1)).findAllByNomsIdAndStatusCodeIn(any(), any())
    }

    @Test
    fun `service returns a list of licence summaries by prison number where updated username and updated date are not present`() {
      whenever(licenceRepository.findAllByNomsIdAndStatusCodeIn(any(), any())).thenReturn(
        listOf(
          aLicenceEntity.copy(updatedByUsername = null, dateLastUpdated = null),
        ),
      )

      val licenceSummaries = service.getAllLicencesByPrisonNumber("A12345")
      val licenceSummary = licenceSummaries.first()

      assertThat(licenceSummaries.size).isEqualTo(1)

      assertThat(licenceSummary).isExactlyInstanceOf(ModelPublicLicenceSummary::class.java)

      assertThat(licenceSummary).extracting {
        tuple(
          it.id, it.licenceType, it.policyVersion, it.version, it.statusCode, it.prisonNumber, it.bookingId,
          it.crn, it.approvedByUsername, it.approvedDateTime, it.createdByUsername, it.createdDateTime,
          it.updatedByUsername, it.updatedDateTime, it.isInPssPeriod,
        )
      }.isEqualTo(
        tuple(
          1L,
          PublicLicenceType.AP,
          PolicyVersion.V1_0,
          "1.4",
          PublicLicenceStatus.IN_PROGRESS,
          "A1234BC",
          987654L,
          "A12345",
          "testapprover",
          LocalDateTime.parse("2023-10-11T13:00"),
          "testcom",
          LocalDateTime.parse("2023-10-11T11:00"),
          null,
          null,
          false,
        ),
      )

      verify(licenceRepository, times(1)).findAllByNomsIdAndStatusCodeIn(any(), any())
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

      verify(licenceRepository, times(1)).findAllByNomsIdAndStatusCodeIn(any(), any())
    }

    @Test
    fun `service throws an error for an unmapped field when querying a list of licence summaries by prison number`() {
      whenever(licenceRepository.findAllByNomsIdAndStatusCodeIn(any(), any())).thenReturn(
        listOf(
          aLicenceEntity.copy(statusCode = LicenceStatus.NOT_STARTED),
        ),
      )

      val exception = assertThrows<IllegalStateException> {
        service.getAllLicencesByPrisonNumber("A1234BC")
      }

      assertThat(exception).isInstanceOf(IllegalStateException::class.java)
        .hasMessage("No matching licence status found")

      verify(licenceRepository, times(1)).findAllByNomsIdAndStatusCodeIn(any(), any())
    }
  }

  @Nested
  inner class `Get exclusion zone image by condition ID` {
    @Test
    fun `service returns an exclusion zone image by condition ID`() {
      whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))
      whenever(additionalConditionRepository.findById(1L)).thenReturn(Optional.of(anAdditionalConditionEntityWithUpload))
      whenever(additionalConditionUploadDetailRepository.findById(1L)).thenReturn(
        Optional.of(
          anAdditionalConditionUploadDetailEntity,
        ),
      )

      val image = service.getImageUpload(1L, 1L)

      assertThat(image).isEqualTo(ClassPathResource("test_map.jpg").inputStream.readAllBytes())

      verify(licenceRepository, times(1)).findById(1L)
      verify(additionalConditionRepository, times(1)).findById(1L)
      verify(additionalConditionUploadDetailRepository, times(1)).findById(1L)
    }

    @Test
    fun `service throws error retrieving an exclusion zone image by condition ID for no licence`() {
      whenever(licenceRepository.findById(1L)).thenReturn(Optional.empty())

      val exception = assertThrows<EntityNotFoundException> {
        service.getImageUpload(1L, 1L)
      }

      assertThat(exception).isInstanceOf(EntityNotFoundException::class.java).hasMessage("Licence 1 not found")

      verify(licenceRepository, times(1)).findById(1L)
      verify(additionalConditionRepository, times(0)).findById(1L)
      verify(additionalConditionUploadDetailRepository, times(0)).findById(1L)
    }

    @Test
    fun `service throws error retrieving an exclusion zone image by condition ID for no condition`() {
      whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))
      whenever(additionalConditionRepository.findById(1L)).thenReturn(Optional.empty())

      val exception = assertThrows<EntityNotFoundException> {
        service.getImageUpload(1L, 1L)
      }

      assertThat(exception).isInstanceOf(EntityNotFoundException::class.java).hasMessage("Condition 1 not found")

      verify(licenceRepository, times(1)).findById(1L)
      verify(additionalConditionRepository, times(1)).findById(1L)
      verify(additionalConditionUploadDetailRepository, times(0)).findById(1L)
    }

    @Test
    fun `service throws error retrieving an exclusion zone image by condition ID without an upload`() {
      whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))
      whenever(additionalConditionRepository.findById(1L)).thenReturn(
        Optional.of(
          anAdditionalConditionEntityWithoutUpload,
        ),
      )

      val exception = assertThrows<EntityNotFoundException> {
        service.getImageUpload(1L, 1L)
      }

      assertThat(exception).isInstanceOf(EntityNotFoundException::class.java)
        .hasMessage("Condition 1 upload details not found")

      verify(licenceRepository, times(1)).findById(1L)
      verify(additionalConditionRepository, times(1)).findById(1L)
      verify(additionalConditionUploadDetailRepository, times(0)).findById(1L)
    }

    @Test
    fun `service throws error retrieving an exclusion zone image by condition ID for no upload details`() {
      whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))
      whenever(additionalConditionRepository.findById(1L)).thenReturn(Optional.of(anAdditionalConditionEntityWithUpload))
      whenever(additionalConditionUploadDetailRepository.findById(1L)).thenReturn(Optional.empty())

      val exception = assertThrows<EntityNotFoundException> {
        service.getImageUpload(1L, 1L)
      }

      assertThat(exception).isInstanceOf(EntityNotFoundException::class.java)
        .hasMessage("Condition 1 upload details not found")

      verify(licenceRepository, times(1)).findById(1L)
      verify(additionalConditionRepository, times(1)).findById(1L)
      verify(additionalConditionUploadDetailRepository, times(1)).findById(1L)
    }
  }

  /****************/
  private companion object {

    val someStandardConditions = listOf(
      uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.StandardCondition(
        id = 1,
        code = "goodBehaviour",
        sequence = 1,
        text = "Be of good behaviour",
      ),
      uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.StandardCondition(
        id = 2,
        code = "notBreakLaw",
        sequence = 1,
        text = "Do not break any law",
      ),
      uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.StandardCondition(
        id = 3,
        code = "attendMeetings",
        sequence = 1,
        text = "Attend meetings",
      ),
    )

    val someAssociationData = listOf(
      uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalConditionData(
        id = 1,
        field = "field1",
        value = "value1",
        sequence = 1,
      ),
      uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalConditionData(
        id = 2,
        field = "numberOfCurfews",
        value = "value2",
        sequence = 2,
      ),
    )

    val someAdditionalConditions = listOf(
      uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalCondition(
        id = 1,
        code = "associateWith",
        sequence = 1,
        text = "Do not associate with [NAME] for a period of [TIME PERIOD]",
        expandedText = "Do not associate with value1 for a period of value2",
        data = someAssociationData,
        readyToSubmit = true,
      ),
    )

    val someBespokeConditions = listOf(
      uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.BespokeCondition(
        id = 1,
        sequence = 1,
        text = "Bespoke one text",
      ),
      uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.BespokeCondition(
        id = 2,
        sequence = 2,
        text = "Bespoke two text",
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
      forename = "Bob",
      surname = "Mortimer",
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
      comEmail = "stephen.mills@nps.gov.uk",
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

    val someAdditionalConditionData = listOf(
      AdditionalConditionData(
        id = 1,
        dataField = "outOfBoundArea",
        dataValue = "Bristol town centre",
        additionalCondition = AdditionalCondition(licence = aLicenceEntity, conditionVersion = "1.0"),
      ),
      AdditionalConditionData(
        id = 2,
        dataField = "outOfBoundFile",
        dataValue = "test.pdf",
        additionalCondition = AdditionalCondition(licence = aLicenceEntity, conditionVersion = "1.0"),
      ),
    )

    val someUploadSummaryData = AdditionalConditionUploadSummary(
      id = 1,
      filename = "test.pdf",
      fileType = "application/pdf",
      description = "Description",
      thumbnailImage = ByteArray(0),
      additionalCondition = someAdditionalConditionData[0].additionalCondition,
      uploadDetailId = 1,
    )

    val anAdditionalConditionEntityWithUpload = AdditionalCondition(
      id = 1,
      conditionVersion = "1.0",
      licence = aLicenceEntity,
      conditionCode = "outOfBounds",
      conditionCategory = "Freedom of movement",
      conditionSequence = 1,
      conditionText = "text",
      additionalConditionData = someAdditionalConditionData,
      additionalConditionUploadSummary = listOf(someUploadSummaryData),
    )

    val anAdditionalConditionUploadDetailEntity = AdditionalConditionUploadDetail(
      id = 1,
      licenceId = 1,
      additionalConditionId = 1,
      fullSizeImage = ClassPathResource("test_map.jpg").inputStream.readAllBytes(),
      originalData = ClassPathResource("Test_map_2021-12-06_112550.pdf").inputStream.readAllBytes(),
    )

    val anAdditionalConditionEntityWithoutUpload = AdditionalCondition(
      id = 1,
      licence = aLicenceEntity,
      conditionVersion = "1.0",
      conditionCode = "outOfBounds",
      conditionCategory = "Freedom of movement",
      conditionSequence = 1,
      conditionText = "text",
      additionalConditionData = someAdditionalConditionData,
      additionalConditionUploadSummary = emptyList(),
    )

    val publicLicenseConditions = Conditions(
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
      licenceType = modelLicence.typeCode.mapToPublicLicenceType(),
      policyVersion = PolicyVersion.entries.find { it.version == modelLicence.version }!!,
      version = modelLicence.licenceVersion.orEmpty(),
      statusCode = PublicLicenceStatus.valueOf(
        modelLicence.statusCode.toString(),
      ),

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
      conditions = publicLicenseConditions,
    )
  }

  @Nested
  inner class `Get licence by id` {
    @Test
    fun `service returns a licence by id`() {
      val licenceId: Long
      licenceId = 12345
      whenever(licenceService.getLicenceById(any())).thenReturn(modelLicence)
      val actualLicence = service.getLicenceById(licenceId)

      assertThat(actualLicence).isEqualTo(publicLicence)
    }

    @Test
    fun `service returns a list of licence summaries by crn where approved username and approved date are not present`() {
      whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(
        listOf(
          aLicenceEntity.copy(approvedByUsername = null, approvedDate = null),
        ),
      )

      val licenceSummaries = service.getAllLicencesByCrn("A12345")
      val licenceSummary = licenceSummaries.first()

      assertThat(licenceSummaries.size).isEqualTo(1)

      assertThat(licenceSummary).isExactlyInstanceOf(ModelPublicLicenceSummary::class.java)

      assertThat(licenceSummary).extracting {
        tuple(
          it.id, it.licenceType, it.policyVersion, it.version, it.statusCode, it.prisonNumber, it.bookingId,
          it.crn, it.approvedByUsername, it.approvedDateTime, it.createdByUsername, it.createdDateTime,
          it.updatedByUsername, it.updatedDateTime, it.isInPssPeriod,
        )
      }.isEqualTo(
        tuple(
          1L,
          PublicLicenceType.AP,
          PolicyVersion.V1_0,
          "1.4",
          PublicLicenceStatus.IN_PROGRESS,
          "A1234BC",
          987654L,
          "A12345",
          null,
          null,
          "testcom",
          LocalDateTime.parse("2023-10-11T11:00"),
          "testupdater",
          LocalDateTime.parse("2023-10-11T12:00"),
          false,
        ),
      )

      verify(licenceRepository, times(1)).findAllByCrnAndStatusCodeIn(any(), any())
    }

    @Test
    fun `service returns a list of licence summaries by crn where updated username and updated date are not present`() {
      whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(
        listOf(
          aLicenceEntity.copy(updatedByUsername = null, dateLastUpdated = null),
        ),
      )

      val licenceSummaries = service.getAllLicencesByCrn("A12345")
      val licenceSummary = licenceSummaries.first()

      assertThat(licenceSummaries.size).isEqualTo(1)

      assertThat(licenceSummary).isExactlyInstanceOf(ModelPublicLicenceSummary::class.java)

      assertThat(licenceSummary).extracting {
        tuple(
          it.id, it.licenceType, it.policyVersion, it.version, it.statusCode, it.prisonNumber, it.bookingId,
          it.crn, it.approvedByUsername, it.approvedDateTime, it.createdByUsername, it.createdDateTime,
          it.updatedByUsername, it.updatedDateTime, it.isInPssPeriod,
        )
      }.isEqualTo(
        tuple(
          1L,
          PublicLicenceType.AP,
          PolicyVersion.V1_0,
          "1.4",
          PublicLicenceStatus.IN_PROGRESS,
          "A1234BC",
          987654L,
          "A12345",
          "testapprover",
          LocalDateTime.parse("2023-10-11T13:00"),
          "testcom",
          LocalDateTime.parse("2023-10-11T11:00"),
          null,
          null,
          false,
        ),
      )

      verify(licenceRepository, times(1)).findAllByCrnAndStatusCodeIn(any(), any())
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

    @Test
    fun `service throws an error for an unmapped field when querying a list of licence summaries by crn`() {
      whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(
        listOf(
          aLicenceEntity.copy(statusCode = LicenceStatus.NOT_STARTED),
        ),
      )

      val exception = assertThrows<IllegalStateException> {
        service.getAllLicencesByCrn("A12345")
      }

      assertThat(exception).isInstanceOf(IllegalStateException::class.java)
        .hasMessage("No matching licence status found")

      verify(licenceRepository, times(1)).findAllByCrnAndStatusCodeIn(any(), any())
    }
  }
}
