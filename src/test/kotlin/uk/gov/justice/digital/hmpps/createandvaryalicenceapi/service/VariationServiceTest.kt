package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.BespokeCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CurfewTimes
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response.VariedAdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response.VariedBespokeCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.aModelHdcLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.aModelHdcVariation
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.aModelLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.aModelVariation
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.variations.VariationService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.variations.compareLicenceConditions
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
      variationService.calculateDiffFromOriginal(aLicence.id)
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

    val result = compareLicenceConditions(originalLicence, variationLicence)
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
    val result = variationService.calculateDiffFromOriginal(variationLicence.id)

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

    val result = variationService.calculateDiffFromOriginal(variationLicence.id)

    assertThat(result.licenceConditionsAdded).isEmpty()
    assertThat(result.licenceConditionsRemoved).isEmpty()
    assertThat(result.licenceConditionsAmended).isEmpty()
    assertThat(result.hasUpdatedCurfewAddress).isFalse
    assertThat(result.hasUpdatedCurfewHours).isTrue
  }
}
