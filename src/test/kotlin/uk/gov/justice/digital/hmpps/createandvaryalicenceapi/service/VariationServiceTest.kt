package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.BespokeCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.aModelLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.aModelVariation

class VariationServiceTest {
  private val licenceService = mock<LicenceService>()
  private val variationService = VariationService(licenceService)

  @Test
  fun `should return a list of removed licence conditions`() {
    val originalLicence = aModelLicence()
      .copy(
        additionalLicenceConditions = listOf(
          AdditionalCondition(
            id = 1,
            code = "1",
            category = "category1",
            text = "text condition 1",
            expandedText = "expanded text 1",
            uploadSummary = emptyList(),
          ),
          AdditionalCondition(
            id = 2,
            code = "2",
            category = "category2",
            text = "text condition 2",
            expandedText = "expanded text 2",
            uploadSummary = emptyList(),
          ),
        ),
        bespokeConditions = listOf(
          BespokeCondition(id = 1, text = "bespoke1"),
          BespokeCondition(id = 2, text = "bespoke2"),
        ),
      )

    val variationLicence = aModelVariation().copy(
      additionalLicenceConditions = emptyList(),
      bespokeConditions = emptyList(),
    )

    val result = variationService.compareVariationToOriginal(originalLicence, variationLicence)
    assertThat(result.licenceConditionsAdded).isEmpty()
    assertThat(result.licenceConditionsAmended).isEmpty()
    assertThat(result.licenceConditionsRemoved).contains(
      VariedAdditionalCondition("category1", "expanded text 1"),
      VariedAdditionalCondition("category2", "expanded text 2"),
      VariedBespokeCondition("Bespoke condition", "bespoke1"),
      VariedBespokeCondition("Bespoke condition", "bespoke2"),
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
            id = 1,
            code = "1",
            category = "category1",
            text = "text condition 1",
            expandedText = "expanded text 1",
            uploadSummary = emptyList(),
          ),
          AdditionalCondition(
            id = 2,
            code = "2",
            category = "category2",
            text = "text condition 2",
            expandedText = "expanded text 2",
            uploadSummary = emptyList(),
          ),
        ),
        bespokeConditions = listOf(
          BespokeCondition(id = 1, text = "bespoke1"),
          BespokeCondition(id = 2, text = "bespoke2"),
        ),
      )

    val result = variationService.compareVariationToOriginal(originalLicence, variationLicence)

    assertThat(result.licenceConditionsAdded).contains(
      VariedAdditionalCondition("category1", "expanded text 1"),
      VariedAdditionalCondition("category2", "expanded text 2"),
      VariedBespokeCondition("Bespoke condition", "bespoke1"),
      VariedBespokeCondition("Bespoke condition", "bespoke2"),
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
            id = 1,
            code = "1",
            category = "category1",
            text = "text condition 1",
            expandedText = "expanded text 1",
            uploadSummary = emptyList(),
          ),
        ),
        bespokeConditions = listOf(
          BespokeCondition(id = 1, text = "bespoke1"),
        ),
      )

    val variationLicence = aModelLicence()
      .copy(
        additionalLicenceConditions = listOf(
          AdditionalCondition(
            id = 1,
            code = "1",
            category = "category1",
            text = "text condition 1",
            expandedText = "expanded text 1 - amended",
            uploadSummary = emptyList(),
          ),
        ),
        bespokeConditions = listOf(
          BespokeCondition(id = 2, text = "bespoke1 - amended"),
        ),
      )

    val result = variationService.compareVariationToOriginal(originalLicence, variationLicence)

    assertThat(result.licenceConditionsAdded).contains(
      VariedBespokeCondition("Bespoke condition", "bespoke1 - amended"),
    )
    assertThat(result.licenceConditionsRemoved).contains(
      VariedBespokeCondition("Bespoke condition", "bespoke1"),
    )
    assertThat(result.licenceConditionsAmended).contains(
      VariedAdditionalCondition("category1", "expanded text 1 - amended"),
    )
  }

  @Test
  fun `should no changes if the licence hasn't changed`() {
    val originalLicence = aModelLicence()
      .copy(
        additionalLicenceConditions = listOf(
          AdditionalCondition(
            id = 1,
            code = "1",
            category = "category1",
            text = "text condition 1",
            expandedText = "expanded text 1",
            uploadSummary = emptyList(),
          ),
        ),
        bespokeConditions = listOf(
          BespokeCondition(id = 1, text = "bespoke1"),
        ),
      )

    val variationLicence = aModelVariation()
      .copy(
        additionalLicenceConditions = listOf(
          AdditionalCondition(
            id = 1,
            code = "1",
            category = "category1",
            text = "text condition 1",
            expandedText = "expanded text 1",
            uploadSummary = emptyList(),
          ),
        ),
        bespokeConditions = listOf(
          BespokeCondition(id = 1, text = "bespoke1"),
        ),
      )

    val result = variationService.compareVariationToOriginal(originalLicence, variationLicence)

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
            id = 1,
            code = "1",
            category = "category 1",
            expandedText = "text condition 1",
          ),
          AdditionalCondition(
            id = 2,
            code = "2",
            category = "Freedom of movement",
            expandedText = "Wales",
          ),
          AdditionalCondition(
            id = 3,
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
            id = 1,
            code = "1",
            category = "category 1",
            expandedText = "amended text 1",
          ),
          AdditionalCondition(
            id = 2,
            code = "2",
            category = "Freedom of movement",
            expandedText = "Wales",
          ),
          AdditionalCondition(
            id = 3,
            code = "2",
            category = "Freedom of movement",
            expandedText = "Scotland",
          ),
        ),
      )

    val result = variationService.compareVariationToOriginal(originalLicence, variationLicence)

    assertThat(result.licenceConditionsAdded).isEmpty()
    assertThat(result.licenceConditionsRemoved).isEmpty()
    assertThat(result.licenceConditionsAmended).contains(
      VariedAdditionalCondition("category 1", "amended text 1"),
      VariedAdditionalCondition("Freedom of movement", "Wales\n\nScotland"),
    )
  }
}
