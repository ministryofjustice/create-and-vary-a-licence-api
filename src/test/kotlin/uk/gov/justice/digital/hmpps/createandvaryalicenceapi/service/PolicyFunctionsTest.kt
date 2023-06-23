package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.AdditionalConditionAp
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.Input
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.InputType.TEXT
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.Replacements
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.ConditionChangeType.DELETED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.ConditionChangeType.NEW_OPTIONS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.ConditionChangeType.REMOVED_NO_REPLACEMENTS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.ConditionChangeType.REPLACED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.ConditionChangeType.TEXT_CHANGE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.ACTIVE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType.PSS

class PolicyFunctionsTest {

  @Nested
  inner class `Policy changes` {
    @Test
    fun `no changes when conditions are empty`() {
      assertThat(
        licencePolicyChanges(
          licence().copy(version = "1.0", additionalLicenceConditions = emptyList()),
          emptyList<AdditionalConditionAp>(),
          emptyList<AdditionalConditionAp>(),
          emptyList<Replacements>(),
        ),
      ).isEmpty()
    }

    @Test
    fun `no changes if no change`() {
      val current = policyCondition().copy(code = "code-1", text = "some-text")
      val previous = policyCondition().copy(code = "code-1", text = "some-text")
      val licenceCondition = additionalCondition().copy(code = "code-1", sequence = 1, version = "1.0")

      assertThat(
        licencePolicyChanges(
          licence().copy(
            version = "2.0",
            additionalLicenceConditions = listOf(
              licenceCondition,
            ),
          ),
          listOf(previous),
          listOf(current),
          emptyList<Replacements>(),
        ),
      ).isEmpty()
    }

    @Test
    fun `changes if text change`() {
      val current = policyCondition().copy(code = "code-1", text = "new-text")
      val previous = policyCondition().copy(code = "code-1", text = "some-text")
      val licenceCondition = additionalCondition().copy(code = "code-1", sequence = 1, version = "1.0")

      assertThat(
        licencePolicyChanges(
          licence().copy(
            version = "2.0",
            additionalLicenceConditions = listOf(
              licenceCondition,
            ),
          ),
          listOf(previous),
          listOf(current),
          emptyList<Replacements>(),
        ),
      ).isEqualTo(
        listOf(
          LicenceConditionChanges(
            changeType = TEXT_CHANGE,
            code = "code-1",
            sequence = 1,
            previousText = "some-text",
            currentText = "new-text",
            addedInputs = emptyList(),
            removedInputs = emptyList(),
            suggestions = emptyList(),
          ),
        ),
      )
    }

    @Test
    fun `no changes if already migrated`() {
      val current = policyCondition().copy(code = "code-1", text = "new-text")
      val previous = policyCondition().copy(code = "code-1", text = "some-text")
      val licenceCondition = additionalCondition().copy(code = "code-1", sequence = 1, version = "2.0")

      assertThat(
        licencePolicyChanges(
          licence().copy(
            version = "2.0",
            additionalLicenceConditions = listOf(
              licenceCondition,
            ),
          ),
          listOf(previous),
          listOf(current),
          emptyList<Replacements>(),
        ),
      ).isEmpty()
    }
  }

  @Nested
  inner class `Condition changes` {
    @Test
    fun `no changes when conditions are empty`() {
      assertThat(
        conditionChanges(
          emptyList<AdditionalConditionAp>(),
          emptyList<AdditionalConditionAp>(),
          emptyList<Replacements>(),
        ),
      ).isEmpty()
    }

    @Test
    fun `no changes when single condition with no changes`() {
      val previous = policyCondition().copy(code = "code-1", text = "some-text")
      val after = policyCondition().copy(code = "code-1", text = "some-text")

      assertThat(conditionChanges(listOf(previous), listOf(after), emptyList())).isEmpty()
    }

    @Test
    fun `no changes when new condition added`() {
      val previous = policyCondition().copy(code = "code-1", text = "some-text")
      val matching = policyCondition().copy(code = "code-1", text = "some-text")
      val newCondition = policyCondition().copy(code = "code-2", text = "some-text")

      assertThat(
        conditionChanges(
          listOf(previous),
          listOf(matching, newCondition),
          emptyList(),
        ),
      ).isEmpty()
    }

    @Test
    fun `no changes when same placeholders`() {
      val previous = policyCondition().copy(
        code = "code-1",
        text = "some-text",
        inputs = listOf(input().copy(name = "one")),
      )
      val after = policyCondition().copy(
        code = "code-1",
        text = "some-text",
        inputs = listOf(input().copy(name = "one")),
      )

      assertThat(conditionChanges(listOf(previous), listOf(after), emptyList())).isEmpty()
    }

    @Disabled("This should be NEW_OPTIONS but does not affect this version update")
    @Test
    fun `if new placeholders are added to a condition on the policy then there should be a change`() {
      val previous = policyCondition().copy(code = "code-1", text = "some-text")
      val after = policyCondition()
        .copy(code = "code-1", text = "some-text", inputs = listOf(input().copy(name = "one")))

      assertThat(conditionChanges(listOf(previous), listOf(after), emptyList())).isNotEmpty
    }

    @Test
    fun `change when text change`() {
      val previous = policyCondition().copy(code = "code-1", text = "some-text")
      val after = policyCondition().copy(code = "code-1", text = "new-text")

      assertThat(conditionChanges(listOf(previous), listOf(after), emptyList()))
        .containsExactly(TEXT_CHANGE.update(previous, after, emptyList()))
    }

    @Test
    fun `change when options change`() {
      val previous = policyCondition().copy(
        code = "code-1",
        text = "some-text",
        inputs = listOf(input().copy(name = "one")),
      )
      val after = policyCondition().copy(code = "code-1", text = "some-text")

      assertThat(
        conditionChanges(
          listOf(previous),
          listOf(after),
          emptyList(),
        ),
      ).containsExactly(
        NEW_OPTIONS.update(
          previous,
          after,
          listOf(input().copy(name = "one")),
          listOf(),
        ),
      )
    }

    @Test
    fun `previously used condition deleted`() {
      val previous = policyCondition().copy(code = "code-1", text = "some-text")
      val replacements =
        Replacements("code-1", DELETED, alternatives = listOf(policyCondition().copy(code = "code-2", text = "text-2")))

      assertThat(
        conditionChanges(
          listOf(previous),
          emptyList(),
          listOf(replacements),
        ),
      ).containsExactly(DELETED.removal(previous, replacements.alternatives))
    }

    @Test
    fun `previously used condition replaced`() {
      val previous = policyCondition().copy(code = "code-1", text = "some-text")
      val replacements =
        Replacements(
          "code-1",
          REPLACED,
          alternatives = listOf(policyCondition().copy(code = "code-2", text = "text-2")),
        )

      assertThat(
        conditionChanges(
          listOf(previous),
          emptyList(),
          listOf(replacements),
        ),
      ).containsExactly(REPLACED.removal(previous, replacements.alternatives))
    }

    @Test
    fun `previously used condition with no replacements`() {
      val previous = policyCondition().copy(code = "code-1", text = "some-text")
      val replacements =
        Replacements("code-1", REMOVED_NO_REPLACEMENTS, alternatives = emptyList())

      assertThat(
        conditionChanges(
          listOf(previous),
          emptyList(),
          listOf(replacements),
        ),
      ).containsExactly(REMOVED_NO_REPLACEMENTS.removal(previous, emptyList()))
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
    "default-subtext",
    "default-type",
  )

  fun input() = Input(
    type = TEXT,
    label = "default-type",
    name = "default-type",
    listType = null,
    options = null,
    case = null,
    handleIndefiniteArticle = null,
    addAnother = null,
    includeBefore = null,
    subtext = null,
  )

  fun licence() = Licence(1, PSS, "2.0", ACTIVE, isVariation = true)

  fun additionalCondition() = AdditionalCondition(
    id = 2,
    version = "1.0",
    code = "code2",
    sequence = 6,
    category = "removedCategory",
    text = "removedText",
    data = emptyList(),
  )
}
