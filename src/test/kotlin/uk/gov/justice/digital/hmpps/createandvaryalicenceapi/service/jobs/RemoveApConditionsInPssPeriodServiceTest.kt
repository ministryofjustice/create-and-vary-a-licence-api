package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs.RemoveApConditionsInPssPeriodService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence as EntityLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.StandardCondition as EntityStandardCondition

class RemoveApConditionsInPssPeriodServiceTest {
  private val licenceRepository = mock<LicenceRepository>()
  private val licenceConditionService = mock<LicenceConditionService>()

  private val service = Mockito.spy(
    RemoveApConditionsInPssPeriodService(
      licenceRepository,
      licenceConditionService,
    ),
  )

  @BeforeEach
  fun reset() {
    val authentication = mock<Authentication>()
    val securityContext = mock<SecurityContext>()

    whenever(authentication.name).thenReturn("smills")
    whenever(securityContext.authentication).thenReturn(authentication)
    SecurityContextHolder.setContext(securityContext)

    reset(
      licenceRepository,
      licenceConditionService,
    )
  }

  @Test
  fun `remove AP condition for in PSS period licence`() {
    whenever(licenceRepository.getAllVariedLicencesInPSSPeriod()).thenReturn(
      listOf(
        aLicenceEntity.copy(
          licenceExpiryDate = LocalDate.now().minusDays(1),
          topupSupervisionExpiryDate = LocalDate.now().plusDays(1),
          additionalConditions = listOf(
            AdditionalCondition(
              id = 1L,
              conditionVersion = "1.0",
              conditionCode = "code",
              conditionSequence = 5,
              conditionCategory = "oldCategory",
              conditionText = "oldText",
              additionalConditionData = someAdditionalConditionData,
              licence = aLicenceEntity,
              conditionType = "AP",
            ),
            AdditionalCondition(
              id = 3L,
              conditionVersion = "1.0",
              conditionCode = "code3",
              conditionSequence = 6,
              conditionCategory = "pssCategory",
              conditionText = "pssText",
              additionalConditionData = someAdditionalConditionData,
              licence = aLicenceEntity,
              conditionType = "PSS",
            ),
          ),
        ),
      ),
    )

    service.removeAPConditions()

    val licenceEntityCaptor = argumentCaptor<Licence>()
    val conditionIdCaptor = argumentCaptor<List<Long>>()

    verify(licenceConditionService, times(1)).deleteAdditionalConditions(
      licenceEntityCaptor.capture(),
      conditionIdCaptor.capture(),
    )

    assertThat(licenceEntityCaptor.firstValue.id).isEqualTo(aLicenceEntity.id)
    assertThat(conditionIdCaptor.allValues[0]).isEqualTo(listOf(1L))
  }

  @Test
  fun `remove AP conditions for in PSS period licence`() {
    whenever(licenceRepository.getAllVariedLicencesInPSSPeriod()).thenReturn(
      listOf(
        aLicenceEntity.copy(
          licenceExpiryDate = LocalDate.now().minusDays(1),
          topupSupervisionExpiryDate = LocalDate.now().plusDays(1),
          additionalConditions = listOf(
            AdditionalCondition(
              id = 1L,
              conditionVersion = "1.0",
              conditionCode = "code",
              conditionSequence = 5,
              conditionCategory = "oldCategory",
              conditionText = "oldText",
              additionalConditionData = someAdditionalConditionData,
              licence = aLicenceEntity,
              conditionType = "AP",
            ),
            AdditionalCondition(
              id = 3L,
              conditionVersion = "1.0",
              conditionCode = "code3",
              conditionSequence = 6,
              conditionCategory = "pssCategory",
              conditionText = "pssText",
              additionalConditionData = someAdditionalConditionData,
              licence = aLicenceEntity,
              conditionType = "AP",
            ),
          ),
        ),
      ),
    )

    service.removeAPConditions()

    val licenceEntityCaptor = argumentCaptor<Licence>()
    val conditionIdCaptor = argumentCaptor<List<Long>>()

    verify(licenceConditionService, times(1)).deleteAdditionalConditions(
      licenceEntityCaptor.capture(),
      conditionIdCaptor.capture(),
    )

    assertThat(licenceEntityCaptor.firstValue.id).isEqualTo(aLicenceEntity.id)
    assertThat(conditionIdCaptor.allValues[0]).isEqualTo(listOf(1L, 3L))
  }

  private companion object {
    val someEntityStandardConditions = listOf(
      EntityStandardCondition(
        id = 1,
        conditionCode = "goodBehaviour",
        conditionSequence = 1,
        conditionText = "Be of good behaviour",
        licence = mock(),
      ),
      EntityStandardCondition(
        id = 2,
        conditionCode = "notBreakLaw",
        conditionSequence = 2,
        conditionText = "Do not break any law",
        licence = mock(),
      ),
      EntityStandardCondition(
        id = 3,
        conditionCode = "attendMeetings",
        conditionSequence = 3,
        conditionText = "Attend meetings",
        licence = mock(),
      ),
    )

    val aLicenceEntity = EntityLicence(
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
      forename = "Bob",
      surname = "Mortimer",
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
      standardConditions = someEntityStandardConditions,
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
    )

    val someAdditionalConditionData = listOf(
      AdditionalConditionData(
        id = 1,
        dataField = "dataField",
        dataValue = "dataValue",
        additionalCondition = AdditionalCondition(licence = aLicenceEntity, conditionVersion = "1.0"),
      ),
    )
  }
}
