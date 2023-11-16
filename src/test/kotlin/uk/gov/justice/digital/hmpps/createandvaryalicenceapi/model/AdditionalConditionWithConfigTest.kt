package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.AdditionalConditionAp
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.AdditionalConditions
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.Input
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.InputType.TEXT
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.LicencePolicy
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.StandardConditions
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate
import java.time.LocalDateTime

class AdditionalConditionWithConfigTest {

  @Nested
  inner class `mapConditionsToConfig` {
    @Test
    fun `maps conditions and policy together`() {
      assertThat(
        mapConditionsToConfig(
          listOf(anAdditionalConditionEntity),
          aPolicy.additionalConditions,
        ),
      ).containsExactly(
        AdditionalConditionWithConfig(anAdditionalConditionEntity, policyApCondition),
      )
    }
  }

  @Nested
  inner class `checkConditionsReadyToSubmit` {
    @Test
    fun `returns if there are no conditions on the licence`() {
      assertThat(
        checkConditionsReadyToSubmit(
          emptyList(),
          aPolicy.additionalConditions,
        ),
      ).isEqualTo(
        emptyMap<String, Boolean>(),
      )
    }

    @Test
    fun `maps conditions with inputs to true`() {
      assertThat(
        checkConditionsReadyToSubmit(
          listOf(anAdditionalConditionEntity, anAdditionalConditionEntity.copy(conditionCode = "code2")),
          aPolicy.additionalConditions,
        ),
      ).isEqualTo(
        mapOf(
          "code" to true,
          "code2" to true,
        ),
      )
    }

    @Test
    fun `maps conditions with missing inputs to false`() {
      assertThat(
        checkConditionsReadyToSubmit(
          listOf(anAdditionalConditionEntity.copy(additionalConditionData = emptyList()), anAdditionalConditionEntity.copy(conditionCode = "code2", additionalConditionData = emptyList())),
          aPolicy.additionalConditions,
        ),
      ).isEqualTo(
        mapOf(
          "code" to false,
          "code2" to false,
        ),
      )
    }

    @Test
    fun `maps conditions that do not need inputs to true`() {
      assertThat(
        checkConditionsReadyToSubmit(
          listOf(anAdditionalConditionEntity.copy(additionalConditionData = emptyList())),
          aPolicy.additionalConditions.copy(ap = listOf(policyConditionWithoutInput)),
        ),
      ).isEqualTo(
        mapOf(
          "code" to true,
        ),
      )
    }
  }

  private companion object {
    val aLicenceEntity = Licence(
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

    val anInput = Input(
      type = TEXT,
      label = "Label",
      name = "name",
    )

    val policyApCondition = AdditionalConditionAp(
      code = "code",
      category = "category",
      text = "text",
      inputs = listOf(anInput),
      requiresInput = true,
    )

    val policyConditionWithoutInput = AdditionalConditionAp(
      code = "code",
      category = "category",
      text = "text",
      requiresInput = false,
    )

    val aPolicy = LicencePolicy(
      "2.1",
      standardConditions = StandardConditions(emptyList(), emptyList()),
      additionalConditions = AdditionalConditions(listOf(policyApCondition, policyApCondition.copy(code = "code2")), emptyList()),
      changeHints = emptyList(),
    )

    val someAdditionalConditionData = AdditionalConditionData(
      additionalCondition = AdditionalCondition(licence = aLicenceEntity, conditionVersion = "2.1"),
      dataField = "name",
    )

    val anAdditionalConditionEntity = AdditionalCondition(
      id = 1,
      conditionVersion = "2.1",
      licence = aLicenceEntity,
      conditionCode = "code",
      conditionCategory = "category",
      conditionSequence = 4,
      conditionText = "text",
      additionalConditionData = listOf(someAdditionalConditionData),
      additionalConditionUploadSummary = emptyList(),
      conditionType = "AP",
    )
  }
}