package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.OmuContact
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence as EntityLicence

class ElectronicMonitoringEndDateChangeListenerTest {
  private val omuService = mock<OmuService>()
  private val notifyService = mock<NotifyService>()

  private val service = ElectronicMonitoringEndDateChangeListener(
    notifyService,
    omuService,
  )

  @BeforeEach
  fun reset() {
    val authentication = mock<Authentication>()
    val securityContext = mock<SecurityContext>()

    whenever(authentication.name).thenReturn("smills")
    whenever(securityContext.authentication).thenReturn(authentication)
    SecurityContextHolder.setContext(securityContext)

    reset(
      notifyService,
      omuService
    )
  }

  @Nested
  inner class IsDateChangeRelevant {
    @Test
    fun `relevant if all conditions met `() {
      assertThat(
        service.isDateChangeRelevant(
          licence = licence,
          sentenceChanges = sentenceChanges
        )
      ).isTrue
    }

    @ParameterizedTest
    @EnumSource(
      value = LicenceStatus::class,
      names = ["IN_PROGRESS", "SUBMITTED", "APPROVED", "VARIATION_IN_PROGRESS", "VARIATION_SUBMITTED", "VARIATION_APPROVED"]
    )
    fun `relevant for status codes `(statusCode: LicenceStatus) {
      assertThat(
        service.isDateChangeRelevant(
          licence = licence.copy(statusCode = statusCode),
          sentenceChanges = sentenceChanges
        )
      ).isTrue
    }

    @ParameterizedTest
    @EnumSource(
      value = LicenceStatus::class,
      names = ["REJECTED", "INACTIVE", "ACTIVE", "RECALLED", "VARIATION_REJECTED"]
    )
    fun `not relevant for status codes `(statusCode: LicenceStatus) {
      assertThat(
        service.isDateChangeRelevant(
          licence = licence.copy(statusCode = statusCode),
          sentenceChanges = sentenceChanges
        )
      ).isFalse
    }

    @Test
    fun `not relevant if condition 14b not present `() {
      assertThat(
        service.isDateChangeRelevant(
          licence = licence.copy(additionalConditions = listOf(condition14b.copy(conditionCode = "not-condition-14b"))),
          sentenceChanges = sentenceChanges
        )
      ).isFalse
    }

    @Test
    fun `not relevant if no dates changed`() {
      assertThat(
        service.isDateChangeRelevant(
          licence = licence,
          sentenceChanges = sentenceChanges.copy(
            ardChanged = false,
            ledChanged = false,
            crdChanged = false
          )
        )
      ).isFalse
    }

    @Test
    fun `relevant if ard changes`() {
      assertThat(
        service.isDateChangeRelevant(
          licence = licence,
          sentenceChanges = sentenceChanges.copy(
            ardChanged = true,
            ledChanged = false,
            crdChanged = false
          )
        )
      ).isTrue
    }

    @Test
    fun `relevant if crd changes`() {
      assertThat(
        service.isDateChangeRelevant(
          licence = licence,
          sentenceChanges = sentenceChanges.copy(
            ardChanged = false,
            ledChanged = false,
            crdChanged = true
          )
        )
      ).isTrue
    }

    @Test
    fun `relevant if led changes`() {
      assertThat(
        service.isDateChangeRelevant(
          licence = licence,
          sentenceChanges = sentenceChanges.copy(
            ardChanged = false,
            ledChanged = true,
            crdChanged = false
          )
        )
      ).isTrue
    }
  }

  @Nested
  inner class Modify {
    @Test
    fun `text is updated`() {
      val updatedLicence = service.modify(licence, sentenceChanges)
      val originalCondition = licence.additionalConditions[0]
      val updatedCondition = updatedLicence.additionalConditions[0]
      assertThat(originalCondition.expandedConditionText).isEqualTo("ending on Tuesday 13th November 2021")
      assertThat(updatedCondition.expandedConditionText).isEqualTo("ending on Friday 22 October 2021")
    }

    @Test
    fun `date is updated`() {
      val updatedLicence = service.modify(licence, sentenceChanges)
      val originalConditionDate = licence.additionalConditions[0].additionalConditionData[0]
      val updatedConditionDate = updatedLicence.additionalConditions[0].additionalConditionData[0]
      assertThat(originalConditionDate.dataValue).isEqualTo("Tuesday 13th November 2021")
      assertThat(updatedConditionDate.dataValue).isEqualTo("Friday 22 October 2021")
    }
  }

  @Nested
  inner class `After Flush` {

    @Test
    fun `after flush`() {

      whenever(omuService.getOmuContactEmail(any())).thenReturn(
        OmuContact(
          prisonCode = licence.prisonCode!!,
          email = "a-user@email.org",
          dateCreated = LocalDateTime.now()
        )
      )

      service.afterFlush(
        licence = licence,
        sentenceChanges = sentenceChanges
      )

      verify(notifyService).sendElectronicMonitoringEndDatesChangedEmail(
        licence.id.toString(),
        "a-user@email.org",
        licence.forename!!,
        licence.surname!!,
        licence.nomsId!!,
        "release date and licence end date"
      )
    }
  }

  private companion object {
    val skeletonRecord = EntityLicence(
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
      standardConditions = emptyList(),
      additionalConditions = emptyList(),
      mailingList = mutableSetOf(
        CommunityOffenderManager(
          staffIdentifier = 2000,
          username = "smills",
          email = "testemail@probation.gov.uk",
          firstName = "X",
          lastName = "Y"
        )
      ),
      responsibleCom = CommunityOffenderManager(
        staffIdentifier = 2000,
        username = "smills",
        email = "testemail@probation.gov.uk",
        firstName = "X",
        lastName = "Y"
      ),
      createdBy = CommunityOffenderManager(
        staffIdentifier = 2000,
        username = "smills",
        email = "testemail@probation.gov.uk",
        firstName = "X",
        lastName = "Y"
      ),
    )

    private val skeletonCondition14b: AdditionalCondition =
      AdditionalCondition(
        conditionVersion = "1",
        conditionCode = CONDITION_CODE_FOR_14B,
        conditionCategory = "Electronic monitoring",
        conditionSequence = 1,
        conditionText = "ending on [INSERT END DATE]",
        expandedConditionText = "ending on Tuesday 13th November 2021",
        licence = skeletonRecord,
        additionalConditionData = emptyList()
      )

    private val condition14b: AdditionalCondition = skeletonCondition14b.copy(
      additionalConditionData = listOf(
        AdditionalConditionData(
          dataSequence = 0,
          dataField = CONDITION_14B_END_DATE,
          dataValue = "Tuesday 13th November 2021",
          additionalCondition = skeletonCondition14b
        ),
        AdditionalConditionData(
          dataSequence = 1,
          dataField = "infoInputReviewed",
          dataValue = "false",
          additionalCondition = skeletonCondition14b
        )
      )
    )

    val licence = skeletonRecord.copy(additionalConditions = listOf(condition14b))

    val sentenceChanges = SentenceChanges(
      lsdChanged = true,
      ledChanged = true,
      sedChanged = true,
      tussdChanged = true,
      tusedChanged = true,
      crdChanged = true,
      ardChanged = true,
      isMaterial = true
    )
  }
}
