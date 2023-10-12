package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.publicApi

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.LicenceStatus as PublicLicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.LicenceSummary as ModelPublicLicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.LicenceType as PublicLicenceType

class PublicLicenceServiceTest {
  private val licenceRepository = mock<LicenceRepository>()

  private val service = PublicLicenceService(licenceRepository)

  @BeforeEach
  fun reset() {
    reset(licenceRepository)
  }

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
  }
}
