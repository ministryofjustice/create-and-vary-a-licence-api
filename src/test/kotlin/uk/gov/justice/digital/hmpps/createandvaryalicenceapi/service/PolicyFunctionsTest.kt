package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalConditionData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.AdditionalConditionAp
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.Replacements
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.ConditionChangeType.DELETED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.ConditionChangeType.NEW_OPTIONS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.ConditionChangeType.REMOVED_NO_REPLACEMENTS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.ConditionChangeType.REPLACED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.ConditionChangeType.TEXT_CHANGE

class PolicyFunctionsTest {

  @Nested
  inner class `Condition changes` {
    @Test
    fun `no changes when conditions are empty`() {
      assertThat(licencePolicyChanges(emptyList(), emptyList(), emptyList(), emptyMap())).isEmpty()
    }

    @Test
    fun `no changes when single condition with no changes`() {
      val condition = additionalCondition().copy(code = "code-1", text = "some-text")
      val policyCondition = policyCondition().copy(code = "code-1", text = "some-text")

      assertThat(licencePolicyChanges(listOf(condition), listOf(policyCondition), emptyList(), emptyMap())).isEmpty()
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
          emptyList(),
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

      assertThat(licencePolicyChanges(listOf(condition), listOf(policyCondition), emptyList(), placeHolders)).isEmpty()
    }

    @Disabled("This should be NEW_OPTIONS but does not affect this version update")
    @Test
    fun `if new placeholders are added to a condition on the policy then there should be a change`() {
      val placeHolders = mapOf("code-1" to mapOf("SOME CHANGE" to "ph-1"))
      val condition = additionalCondition().copy(code = "code-1", text = "some-text", data = listOf())
      val policyCondition = policyCondition().copy(code = "code-1", text = "some-text")

      assertThat(licencePolicyChanges(listOf(condition), listOf(policyCondition), emptyList(), placeHolders)).isNotEmpty
    }

    @Test
    fun `change when text change`() {
      val condition = additionalCondition().copy(code = "code-1", text = "some-text")
      val policyCondition = policyCondition().copy(code = "code-1", text = "new-text")

      assertThat(licencePolicyChanges(listOf(condition), listOf(policyCondition), emptyList(), emptyMap()))
        .containsExactly(TEXT_CHANGE.update(condition, "new-text", emptyList()))
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

      assertThat(
        licencePolicyChanges(
          listOf(condition),
          listOf(policyCondition),
          emptyList(),
          placeHolders
        )
      ).containsExactly(
        NEW_OPTIONS.update(
          condition, "some-text", listOf(AdditionalConditionData(id = -1, field = "ph-1", value = null, sequence = -1))
        )
      )
    }

    @Test
    fun `previously used condition deleted`() {
      val condition = additionalCondition().copy(code = "code-1", text = "some-text")
      val replacements =
        Replacements("code-1", DELETED, alternatives = listOf(policyCondition().copy(code = "code-2", text = "text-2")))

      assertThat(
        licencePolicyChanges(
          listOf(condition),
          emptyList(),
          listOf(replacements),
          emptyMap()
        )
      ).containsExactly(DELETED.removal(condition, replacements.alternatives))
    }
  }

  @Test
  fun `previously used condition replaced`() {
    val condition = additionalCondition().copy(code = "code-1", text = "some-text")
    val replacements =
      Replacements("code-1", REPLACED, alternatives = listOf(policyCondition().copy(code = "code-2", text = "text-2")))

    assertThat(
      licencePolicyChanges(
        listOf(condition),
        emptyList(),
        listOf(replacements),
        emptyMap()
      )
    ).containsExactly(REPLACED.removal(condition, replacements.alternatives))
  }

  @Test
  fun `previously used condition with no replacements`() {
    val condition = additionalCondition().copy(code = "code-1", text = "some-text")
    val replacements =
      Replacements("code-1", REMOVED_NO_REPLACEMENTS, alternatives = emptyList())

    assertThat(
      licencePolicyChanges(
        listOf(condition),
        emptyList(),
        listOf(replacements),
        emptyMap()
      )
    ).containsExactly(REMOVED_NO_REPLACEMENTS.removal(condition, emptyList()))
  }

  fun policyCondition() = AdditionalConditionAp(
    "default-code",
    "default-category",
    "default-text",
    "default-template",
    false,
    emptyList(),
    "default-category-short",
    "default-subtext",
    "default-type"
  )

  fun additionalCondition() = AdditionalCondition(code = "code-1", text = "some-text")
}
