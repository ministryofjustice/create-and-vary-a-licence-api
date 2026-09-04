package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalConditionUploadSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.BespokeCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CurfewTimes
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response.ImageUploadSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response.VariedAdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response.VariedBespokeCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.aModelHdcLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.aModelHdcVariation
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.aModelLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.aModelVariation
import java.time.LocalDateTime
import java.time.LocalTime

class VariationServiceTest {
  private val licenceService = mock<LicenceService>()
  private val variationService = VariationService(licenceService)

  @Test
  fun `should throw an exception if the licence is not a variation`() {
    val aLicence = aModelLicence()
    whenever(licenceService.getLicenceById(aLicence.id)).thenReturn(aLicence)
    val exception = assertThrows<IllegalArgumentException> {
      variationService.variationDiffFromParent(aLicence.id)
    }
    assertThat(exception.message).isEqualTo("Licence with id ${aLicence.id} is not a variation")
  }

  @Test
  fun `should return a list of removed licence conditions`() {
    val originalLicence = aModelLicence()
      .copy(
        additionalLicenceConditions = listOf(
          AdditionalCondition(
            code = "1",
            category = "category1",
            text = "text condition 1",
            expandedText = "expanded text 1",
            uploadSummary = emptyList(),
          ),
          AdditionalCondition(
            code = "2",
            category = "category2",
            text = "text condition 2",
            expandedText = "expanded text 2",
            uploadSummary = emptyList(),
          ),
        ),
        bespokeConditions = listOf(
          BespokeCondition(text = "bespoke1"),
          BespokeCondition(text = "bespoke2"),
        ),
      )

    val variationLicence = aModelVariation().copy(
      additionalLicenceConditions = emptyList(),
      bespokeConditions = emptyList(),
    )

    val result = variationService.compareLicenceConditions(originalLicence, variationLicence)
    assertThat(result.licenceConditionsAdded).isEmpty()
    assertThat(result.licenceConditionsAmended).isEmpty()
    assertThat(result.licenceConditionsRemoved).contains(
      VariedAdditionalCondition(category = "category1", condition = "expanded text 1"),
      VariedAdditionalCondition(category = "category2", condition = "expanded text 2"),
      VariedBespokeCondition(category = "Bespoke condition", condition = "bespoke1"),
      VariedBespokeCondition(category = "Bespoke condition", condition = "bespoke2"),
    )
  }

  @Test
  fun `should return a list of added conditions`() {
    val originalLicence = aModelLicence().copy(
      additionalLicenceConditions = emptyList(),
      bespokeConditions = emptyList(),
    )

    val variationLicence = aModelVariation()
      .copy(
        additionalLicenceConditions = listOf(
          AdditionalCondition(
            code = "1",
            category = "category1",
            text = "text condition 1",
            expandedText = "expanded text 1",
          ),
          AdditionalCondition(
            code = "2",
            category = "category2",
            text = "text condition 2",
            expandedText = "expanded text 2",
          ),
        ),
        bespokeConditions = listOf(
          BespokeCondition(text = "bespoke1"),
          BespokeCondition(text = "bespoke2"),
        ),
      )

    val result = variationService.compareLicenceConditions(originalLicence, variationLicence)

    assertThat(result.licenceConditionsAdded).contains(
      VariedAdditionalCondition(category = "category1", condition = "expanded text 1"),
      VariedAdditionalCondition(category = "category2", condition = "expanded text 2"),
      VariedBespokeCondition(category = "Bespoke condition", condition = "bespoke1"),
      VariedBespokeCondition(category = "Bespoke condition", condition = "bespoke2"),
    )
    assertThat(result.licenceConditionsAmended).isEmpty()
    assertThat(result.licenceConditionsRemoved).isEmpty()
  }

  @Test
  fun `should return a list of amended standard conditions, bespoke are either added to one and removed from other`() {
    val originalLicence = aModelLicence()
      .copy(
        additionalLicenceConditions = listOf(
          AdditionalCondition(
            code = "1",
            category = "category1",
            text = "text condition 1",
            expandedText = "expanded text 1",
          ),
        ),
        bespokeConditions = listOf(
          BespokeCondition(text = "bespoke1"),
        ),
      )

    val variationLicence = aModelLicence()
      .copy(
        additionalLicenceConditions = listOf(
          AdditionalCondition(
            code = "1",
            category = "category1",
            text = "text condition 1",
            expandedText = "expanded text 1 - amended",
          ),
        ),
        bespokeConditions = listOf(
          BespokeCondition(text = "bespoke1 - amended"),
        ),
      )

    val result = variationService.compareLicenceConditions(originalLicence, variationLicence)

    assertThat(result.licenceConditionsAdded).contains(
      VariedBespokeCondition(category = "Bespoke condition", condition = "bespoke1 - amended"),
    )
    assertThat(result.licenceConditionsRemoved).contains(
      VariedBespokeCondition(category = "Bespoke condition", condition = "bespoke1"),
    )
    assertThat(result.licenceConditionsAmended).contains(
      VariedAdditionalCondition(category = "category1", condition = "expanded text 1 - amended"),
    )
  }

  @Test
  fun `should no changes if the licence hasn't changed`() {
    val originalLicence = aModelLicence()
      .copy(
        additionalLicenceConditions = listOf(
          AdditionalCondition(
            code = "1",
            category = "category1",
            text = "text condition 1",
            expandedText = "expanded text 1",
          ),
        ),
        bespokeConditions = listOf(
          BespokeCondition(text = "bespoke1"),
        ),
      )

    val variationLicence = aModelVariation()
      .copy(
        additionalLicenceConditions = listOf(
          AdditionalCondition(
            code = "1",
            category = "category1",
            text = "text condition 1",
            expandedText = "expanded text 1",
            uploadSummary = emptyList(),
          ),
        ),
        bespokeConditions = listOf(
          BespokeCondition(text = "bespoke1"),
        ),
      )

    val result = variationService.compareLicenceConditions(originalLicence, variationLicence)

    assertThat(result.licenceConditionsAdded).isEmpty()
    assertThat(result.licenceConditionsRemoved).isEmpty()
    assertThat(result.licenceConditionsAmended).isEmpty()
  }

  @Test
  fun `should return changes to multiple exclusion zones`() {
    val originalLicence = aModelLicence()
      .copy(
        additionalLicenceConditions = listOf(
          AdditionalCondition(
            code = "1",
            category = "category 1",
            expandedText = "text condition 1",
          ),
          AdditionalCondition(
            code = "2",
            category = "Freedom of movement",
            expandedText = "Wales",
          ),
          AdditionalCondition(
            code = "2",
            category = "Freedom of movement",
            expandedText = "England",
          ),
        ),
      )

    val variationLicence = aModelVariation()
      .copy(
        additionalLicenceConditions = listOf(
          AdditionalCondition(
            code = "1",
            category = "category 1",
            expandedText = "amended text 1",
          ),
          AdditionalCondition(
            code = "2",
            category = "Freedom of movement",
            expandedText = "Wales",
          ),
          AdditionalCondition(
            code = "2",
            category = "Freedom of movement",
            expandedText = "Scotland",
          ),
        ),
      )

    val result = variationService.compareLicenceConditions(originalLicence, variationLicence)

    assertThat(result.licenceConditionsAdded).isEmpty()
    assertThat(result.licenceConditionsRemoved).isEmpty()
    assertThat(result.licenceConditionsAmended).contains(
      VariedAdditionalCondition(category = "category 1", condition = "amended text 1"),
      VariedAdditionalCondition(category = "Freedom of movement", condition = "Wales\n\nScotland"),
    )
  }

  @Test
  fun `should not return any changes when multiple exclusion zones haven't changed`() {
    val originalLicence = aModelLicence()
      .copy(
        additionalLicenceConditions = listOf(
          AdditionalCondition(
            code = "1",
            category = "category 1",
            expandedText = "text condition 1",
          ),
          AdditionalCondition(
            code = "2",
            category = "Freedom of movement",
            expandedText = "Wales",
          ),
          AdditionalCondition(
            code = "2",
            category = "Freedom of movement",
            expandedText = "England",
          ),
        ),
      )

    val variationLicence = aModelVariation()
      .copy(
        additionalLicenceConditions = listOf(
          AdditionalCondition(
            code = "1",
            category = "category 1",
            expandedText = "amended text 1",
          ),
          AdditionalCondition(
            code = "2",
            category = "Freedom of movement",
            expandedText = "Wales",
          ),
          AdditionalCondition(
            code = "2",
            category = "Freedom of movement",
            expandedText = "England",
          ),
        ),
      )

    val result = variationService.compareLicenceConditions(originalLicence, variationLicence)

    assertThat(result.licenceConditionsAdded).isEmpty()
    assertThat(result.licenceConditionsRemoved).isEmpty()
    assertThat(result.licenceConditionsAmended).contains(
      VariedAdditionalCondition(category = "category 1", condition = "amended text 1"),
    )
  }

  @Test
  fun `should return multiple exclusion zones that have been removed`() {
    val originalLicence = aModelLicence()
      .copy(
        additionalLicenceConditions = listOf(
          AdditionalCondition(
            code = "1",
            category = "category 1",
            expandedText = "text condition 1",
          ),
          AdditionalCondition(
            code = "2",
            category = "Freedom of movement",
            expandedText = "Wales",
          ),
          AdditionalCondition(
            code = "2",
            category = "Freedom of movement",
            expandedText = "England",
          ),
        ),
      )

    val variationLicence = aModelVariation()
      .copy(
        additionalLicenceConditions = listOf(
          AdditionalCondition(
            code = "1",
            category = "category 1",
            expandedText = "amended text 1",
          ),
        ),
      )

    val result = variationService.compareLicenceConditions(originalLicence, variationLicence)

    assertThat(result.licenceConditionsAdded).isEmpty()
    assertThat(result.licenceConditionsRemoved).contains(
      VariedAdditionalCondition(
        category = "Freedom of movement",
        condition = "Wales\n\nEngland",
      ),
    )
    assertThat(result.licenceConditionsAmended).contains(
      VariedAdditionalCondition(category = "category 1", condition = "amended text 1"),
    )
  }

  @Test
  fun `should return a change to an exclusion zone map`() {
    val uploadSummary = AdditionalConditionUploadSummary(id = 1)
    uploadSummary.thumbnailImage = "an image"

    val originalLicence = aModelLicence()
      .copy(
        additionalLicenceConditions = listOf(
          AdditionalCondition(
            code = "1",
            category = "Freedom of movement",
            expandedText = "Wales",
            uploadSummary = listOf(
              uploadSummary,
            ),
          ),
        ),
      )

    val variationUploadSummary = AdditionalConditionUploadSummary(id = 1)
    variationUploadSummary.thumbnailImage = "a different image"

    val variationLicence = aModelVariation()
      .copy(
        additionalLicenceConditions = listOf(
          AdditionalCondition(
            code = "1",
            category = "Freedom of movement",
            expandedText = "Wales",
            uploadSummary = listOf(variationUploadSummary),
          ),
        ),
      )

    val result = variationService.compareLicenceConditions(originalLicence, variationLicence)

    assertThat(result.licenceConditionsAdded).isEmpty()
    assertThat(result.licenceConditionsRemoved).isEmpty()
    assertThat(result.licenceConditionsAmended).contains(
      VariedAdditionalCondition(
        category = "Freedom of movement",
        condition = "Wales",
        uploadSummaries = listOf(ImageUploadSummary(thumbnailImage = "a different image")),
      ),
    )
  }

  @Test
  fun `should return added multiple exclusion zones when parent licence had none present`() {
    val originalLicence = aModelLicence()
      .copy(
        additionalLicenceConditions = listOf(
          AdditionalCondition(
            code = "1",
            category = "category 1",
            expandedText = "testCondition1",
          ),
        ),
      )

    val variationLicence = aModelVariation()
      .copy(
        additionalLicenceConditions = listOf(
          AdditionalCondition(
            code = "1",
            category = "category 1",
            expandedText = "amended text 1",
          ),
          AdditionalCondition(
            code = "2",
            category = "Freedom of movement",
            expandedText = "Wales",
          ),
          AdditionalCondition(
            code = "2",
            category = "Freedom of movement",
            expandedText = "England",
          ),
        ),
      )

    val result = variationService.compareLicenceConditions(originalLicence, variationLicence)

    assertThat(result.licenceConditionsAdded).contains(
      VariedAdditionalCondition(
        category = "Freedom of movement",
        condition = "Wales\n\nEngland",
      ),
    )
    assertThat(result.licenceConditionsRemoved).isEmpty()
    assertThat(result.licenceConditionsAmended).contains(
      VariedAdditionalCondition(category = "category 1", condition = "amended text 1"),
    )
  }

  @Test
  fun `should compare curfew conditions on HDC licences and return no changes`() {
    val originalLicence = aModelHdcLicence()
      .copy(
        weeklyCurfewTimes = listOf(
          CurfewTimes(
            fromTime = LocalTime.of(12, 0),
            untilTime = LocalTime.of(13, 0),
            createdTimestamp = LocalDateTime.now(),
          ),
        ),
      )
    val variationLicence = aModelHdcVariation().copy(weeklyCurfewTimes = originalLicence.weeklyCurfewTimes)

    whenever(licenceService.getLicenceById(variationLicence.id)).thenReturn(variationLicence)
    whenever(licenceService.getLicenceById(variationLicence.variationOf!!)).thenReturn(originalLicence)
    val result = variationService.variationDiffFromParent(variationLicence.id)

    assertThat(result.licenceConditionsAdded).isEmpty()
    assertThat(result.licenceConditionsRemoved).isEmpty()
    assertThat(result.licenceConditionsAmended).isEmpty()
    assertThat(result.hasUpdatedCurfewAddress).isFalse
    assertThat(result.hasUpdatedCurfewHours).isFalse
  }

  @Test
  fun `should compare curfew conditions on HDC licences and return if there are changes`() {
    val originalLicence = aModelHdcLicence().copy(
      weeklyCurfewTimes = listOf(
        CurfewTimes(
          fromTime = LocalTime.of(12, 0),
          untilTime = LocalTime.of(13, 0),
          createdTimestamp = LocalDateTime.now(),
        ),
      ),
    )
    val variationLicence = aModelHdcVariation()
      .copy(
        weeklyCurfewTimes = listOf(
          CurfewTimes(
            fromTime = LocalTime.of(12, 0),
            untilTime = LocalTime.of(14, 0),
            createdTimestamp = LocalDateTime.now(),
          ),
        ),
      )

    whenever(licenceService.getLicenceById(variationLicence.id)).thenReturn(variationLicence)
    whenever(licenceService.getLicenceById(variationLicence.variationOf!!)).thenReturn(originalLicence)

    val result = variationService.variationDiffFromParent(variationLicence.id)

    assertThat(result.licenceConditionsAdded).isEmpty()
    assertThat(result.licenceConditionsRemoved).isEmpty()
    assertThat(result.licenceConditionsAmended).isEmpty()
    assertThat(result.hasUpdatedCurfewAddress).isFalse
    assertThat(result.hasUpdatedCurfewHours).isTrue
  }
}
