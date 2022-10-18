package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalConditionData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.AdditionalConditionAp
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.ConditionChangeType.DELETED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.ConditionChangeType.NEW_OPTIONS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.ConditionChangeType.TEXT_CHANGE

class PolicyFunctionsTest {

  @Nested
  inner class `Condition changes` {
    @Test
    fun `no changes when conditions are empty`() {
      assertThat(licencePolicyChanges(emptyList(), emptyList(), emptyMap())).isEmpty()
    }

    @Test
    fun `no changes when single condition with no changes`() {
      val condition = additionalCondition().copy(code = "code-1", text = "some-text")
      val policyCondition = policyCondition().copy(code = "code-1", text = "some-text")

      assertThat(licencePolicyChanges(listOf(condition), listOf(policyCondition), emptyMap())).isEmpty()
    }

    @Test
    fun `no changes when new condition added`() {
      val condition = additionalCondition().copy(code = "code-1", text = "some-text")
      val matchingPolicyCondition = policyCondition().copy(code = "code-1", text = "some-text")
      val policyCondition = policyCondition().copy(code = "code-2", text = "some-text")

      assertThat(
        licencePolicyChanges(
          listOf(condition),
          listOf(matchingPolicyCondition, policyCondition),
          emptyMap()
        )
      ).isEmpty()
    }

    @Test
    fun `no changes when same placeholders`() {
      val placeHolders = mapOf("code-1" to mapOf("SOME CHANGE" to "ph-1"))
      val condition = additionalCondition().copy(
        code = "code-1", text = "some-text",
        data = listOf(
          AdditionalConditionData(field = "ph-1")
        )
      )
      val policyCondition = policyCondition().copy(code = "code-1", text = "some-text")

      assertThat(licencePolicyChanges(listOf(condition), listOf(policyCondition), placeHolders)).isEmpty()
    }

    @Disabled("Should pass?")
    @Test
    fun `if new placeholders are added to a condition on the policy then there should be a change?`() {
      val placeHolders = mapOf("code-1" to mapOf("SOME CHANGE" to "ph-1"))
      val condition = additionalCondition().copy(code = "code-1", text = "some-text", data = listOf())
      val policyCondition = policyCondition().copy(code = "code-1", text = "some-text")

      assertThat(licencePolicyChanges(listOf(condition), listOf(policyCondition), placeHolders)).isNotEmpty
    }

    @Test
    fun `change when text change`() {
      val condition = additionalCondition().copy(code = "code-1", text = "some-text")
      val policyCondition = policyCondition().copy(code = "code-1", text = "new-text")

      assertThat(licencePolicyChanges(listOf(condition), listOf(policyCondition), emptyMap()))
        .containsExactly(TEXT_CHANGE.of(condition, "new-text", emptyList()))
    }

    @Test
    fun `change when options change`() {
      val placeHolders = mapOf("code-1" to mapOf("SOME CHANGE" to "ph-2"))
      val condition = additionalCondition().copy(
        code = "code-1", text = "some-text",
        data = listOf(
          AdditionalConditionData(field = "ph-1")
        )
      )
      val policyCondition = policyCondition().copy(code = "code-1", text = "some-text")

      assertThat(licencePolicyChanges(listOf(condition), listOf(policyCondition), placeHolders)).containsExactly(
        NEW_OPTIONS.of(
          condition, "some-text", listOf(AdditionalConditionData(id = -1, field = "ph-1", value = null, sequence = -1))
        )
      )
    }

    @Test
    fun `previously used condition deleted`() {
      val condition = additionalCondition().copy(code = "code-1", text = "some-text")

      assertThat(
        licencePolicyChanges(
          listOf(condition),
          listOf(),
          emptyMap()
        )
      ).containsExactly(DELETED.of(condition, currentText = null, dataChanges = emptyList()))
    }
  }

  fun policyCondition() = AdditionalConditionAp(
    "default-code",
    "default-category",
    "default-text",
    "default-template",
    false,
    emptyList(),
    "default-category-short",
    "default-subtext"
  )

  fun additionalCondition() = AdditionalCondition(code = "code-1", text = "some-text")
}
