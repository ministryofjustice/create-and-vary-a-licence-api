package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.publicApi

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import org.springframework.core.io.ClassPathResource
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionUploadDetail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionUploadSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionUploadDetailRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.ApConditions
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.BespokeCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.Conditions
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.PssConditions
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.additionalConditions.StandardAdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licencePolicy.StandardCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.LicenceStatus as PublicLicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.LicenceSummary as ModelPublicLicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.LicenceType as PublicLicenceType

class PublicLicenceServiceTest {
  private val licenceRepository = mock<LicenceRepository>()
  private val additionalConditionRepository = mock<AdditionalConditionRepository>()
  private val additionalConditionUploadDetailRepository = mock<AdditionalConditionUploadDetailRepository>()
  private val licenceService = mock<LicenceService>()
  private val modelLicenceMock = mock<uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Licence>()

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
      modelLicenceMock
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

      assertThat(licenceSummary)
        .extracting {
          Tuple.tuple(
            it.id, it.licenceType, it.policyVersion, it.version, it.statusCode, it.prisonNumber, it.bookingId,
            it.crn, it.approvedByUsername, it.approvedDateTime, it.createdByUsername, it.createdDateTime,
            it.updatedByUsername, it.updatedDateTime, it.isInPssPeriod,
          )
        }
        .isEqualTo(
          Tuple.tuple(
            1L,
            PublicLicenceType.AP,
            "1.0",
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

      assertThat(licenceSummary)
        .extracting {
          Tuple.tuple(
            it.id, it.licenceType, it.policyVersion, it.version, it.statusCode, it.prisonNumber, it.bookingId,
            it.crn, it.approvedByUsername, it.approvedDateTime, it.createdByUsername, it.createdDateTime,
            it.updatedByUsername, it.updatedDateTime, it.isInPssPeriod,
          )
        }
        .isEqualTo(
          Tuple.tuple(
            1L,
            PublicLicenceType.AP,
            "1.0",
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

      assertThat(licenceSummary)
        .extracting {
          Tuple.tuple(
            it.id, it.licenceType, it.policyVersion, it.version, it.statusCode, it.prisonNumber, it.bookingId,
            it.crn, it.approvedByUsername, it.approvedDateTime, it.createdByUsername, it.createdDateTime,
            it.updatedByUsername, it.updatedDateTime, it.isInPssPeriod,
          )
        }
        .isEqualTo(
          Tuple.tuple(
            1L,
            PublicLicenceType.AP,
            "1.0",
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

      assertThat(exception)
        .isInstanceOf(IllegalStateException::class.java)
        .hasMessage("Null field retrieved: createdByUsername for licence 1")

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

      assertThat(exception)
        .isInstanceOf(IllegalStateException::class.java)
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

      assertThat(licenceSummary)
        .extracting {
          Tuple.tuple(
            it.id, it.licenceType, it.policyVersion, it.version, it.statusCode, it.prisonNumber, it.bookingId,
            it.crn, it.approvedByUsername, it.approvedDateTime, it.createdByUsername, it.createdDateTime,
            it.updatedByUsername, it.updatedDateTime, it.isInPssPeriod,
          )
        }
        .isEqualTo(
          Tuple.tuple(
            1L,
            PublicLicenceType.AP,
            "1.0",
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

      assertThat(licenceSummary)
        .extracting {
          Tuple.tuple(
            it.id, it.licenceType, it.policyVersion, it.version, it.statusCode, it.prisonNumber, it.bookingId,
            it.crn, it.approvedByUsername, it.approvedDateTime, it.createdByUsername, it.createdDateTime,
            it.updatedByUsername, it.updatedDateTime, it.isInPssPeriod,
          )
        }
        .isEqualTo(
          Tuple.tuple(
            1L,
            PublicLicenceType.AP,
            "1.0",
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

      assertThat(licenceSummary)
        .extracting {
          Tuple.tuple(
            it.id, it.licenceType, it.policyVersion, it.version, it.statusCode, it.prisonNumber, it.bookingId,
            it.crn, it.approvedByUsername, it.approvedDateTime, it.createdByUsername, it.createdDateTime,
            it.updatedByUsername, it.updatedDateTime, it.isInPssPeriod,
          )
        }
        .isEqualTo(
          Tuple.tuple(
            1L,
            PublicLicenceType.AP,
            "1.0",
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

      assertThat(exception)
        .isInstanceOf(IllegalStateException::class.java)
        .hasMessage("Null field retrieved: policyVersion for licence 1")

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

      assertThat(exception)
        .isInstanceOf(IllegalStateException::class.java)
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

      assertThat(exception)
        .isInstanceOf(EntityNotFoundException::class.java)
        .hasMessage("Licence 1 not found")

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

      assertThat(exception)
        .isInstanceOf(EntityNotFoundException::class.java)
        .hasMessage("Condition 1 not found")

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

      assertThat(exception)
        .isInstanceOf(EntityNotFoundException::class.java)
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

      assertThat(exception)
        .isInstanceOf(EntityNotFoundException::class.java)
        .hasMessage("Condition 1 upload details not found")

      verify(licenceRepository, times(1)).findById(1L)
      verify(additionalConditionRepository, times(1)).findById(1L)
      verify(additionalConditionUploadDetailRepository, times(1)).findById(1L)
    }
  }

  private companion object {

    val aCom = CommunityOffenderManager(
      staffIdentifier = 2000,
      username = "testcom",
      email = "testcom@probation.gov.uk",
      firstName = "Test",
      lastName = "Com",
    )

    val aLicenceEntity = Licence(
      id = 1L,
      crn = "A12345",
      nomsId = "A1234BC",
      bookingId = 987654,
      forename = "Test",
      surname = "Person",
      dateOfBirth = LocalDate.parse("1985-01-01"),
      typeCode = LicenceType.AP,
      statusCode = LicenceStatus.IN_PROGRESS,
      version = "1.4",
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

    private val bespokeCondition = listOf(BespokeCondition("You should not visit Y"))
    private val standardConditions = listOf(
      StandardCondition(
        "fda24aa9-a2b0-4d49-9c87-23b0a7be4013",
        " as reasonably required by your supervisor, to give a sample of oral fluid",
      ),
    )
    private val additionalConditions = listOf(
      StandardAdditionalCondition(
        type = "STANDARD",
        id = 3568,
        category = "Drug testing",
        code = "fda24aa9-a2b0-4d49-9c87-23b0a7be4013",
        text = "Attend [INSERT NAME AND ADDRESS], as reasonably required by your supervisor, to give a sample of oral fluid / urine in order to test whether you have any specified Class A or specified Class B drugs in your body, for the purpose of ensuring that you are complying with the requirement of your supervision period requiring you to be of good behaviour.",

        ),
    )
    private val pssConditions = PssConditions(standardConditions, additionalConditions)
    private val apConditions = ApConditions(
      standard = standardConditions,
      additional = additionalConditions,
      bespoke = bespokeCondition,
    )
    val licenceConditions = Conditions(
      apConditions, pssConditions,
    )

    val pubLicence = uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.Licence(
      id = 1,
      licenceType = uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.LicenceType.AP,
      policyVersion = "2.1",
      version = "1.4",
      statusCode = uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.LicenceStatus.IN_PROGRESS,
      prisonNumber = "A1234AA",
      bookingId = 987654L,
      crn = "A12345",
      approvedByUsername = "TestApprover",
      approvedDateTime = LocalDateTime.of(2023, 10, 11, 12, 0, 0),
      createdByUsername = "TestCreator",
      createdDateTime = LocalDateTime.of(2023, 10, 11, 11, 0, 0),
      updatedByUsername = "TestUpdater",
      updatedDateTime = LocalDateTime.of(2023, 10, 11, 11, 30, 0),
      isInPssPeriod = false,
      conditions = licenceConditions,

      )


  }

  /****************/

  @Nested
  inner class `Get licence by id` {
    @Test
    fun `service returns a licence by id`() {
      val licenceId: Long
      licenceId = 12345
      doReturn(pubLicence).whenever(modelLicenceMock).transformToPublicLicence()
      whenever(licenceService.getLicenceById(any())).thenReturn(modelLicenceMock)
      val actualLicence = service.getLicenceById(licenceId)

      assertThat(actualLicence).isEqualTo(pubLicence)
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

      assertThat(licenceSummary)
        .extracting {
          Tuple.tuple(
            it.id, it.licenceType, it.policyVersion, it.version, it.statusCode, it.prisonNumber, it.bookingId,
            it.crn, it.approvedByUsername, it.approvedDateTime, it.createdByUsername, it.createdDateTime,
            it.updatedByUsername, it.updatedDateTime, it.isInPssPeriod,
          )
        }
        .isEqualTo(
          Tuple.tuple(
            1L,
            PublicLicenceType.AP,
            "1.0",
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

      assertThat(licenceSummary)
        .extracting {
          Tuple.tuple(
            it.id, it.licenceType, it.policyVersion, it.version, it.statusCode, it.prisonNumber, it.bookingId,
            it.crn, it.approvedByUsername, it.approvedDateTime, it.createdByUsername, it.createdDateTime,
            it.updatedByUsername, it.updatedDateTime, it.isInPssPeriod,
          )
        }
        .isEqualTo(
          Tuple.tuple(
            1L,
            PublicLicenceType.AP,
            "1.0",
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

      assertThat(exception)
        .isInstanceOf(IllegalStateException::class.java)
        .hasMessage("Null field retrieved: createdByUsername for licence 1")

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

      assertThat(exception)
        .isInstanceOf(IllegalStateException::class.java)
        .hasMessage("No matching licence status found")

      verify(licenceRepository, times(1)).findAllByCrnAndStatusCodeIn(any(), any())
    }
  }

}
