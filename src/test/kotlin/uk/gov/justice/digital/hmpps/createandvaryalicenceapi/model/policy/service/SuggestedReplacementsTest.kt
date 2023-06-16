package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.AdditionalConditionAp
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.AdditionalConditions
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.ChangeHint
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.LicencePolicy
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.Replacements
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.StandardConditions
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.getSuggestedReplacements
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.ConditionChangeType.DELETED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.ConditionChangeType.REMOVED_NO_REPLACEMENTS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.ConditionChangeType.REPLACED

class SuggestedReplacementsTest {

  @Test
  fun `no conditions and missing policy`() {
    assertThat(getSuggestedReplacements(null, policy())).isEmpty()
  }

  @Test
  fun `policy with no replacements`() {
    val old = policy().copy(
      additionalConditions = additionalConditions().copy(ap = listOf(condition().copy(code = "code-1"))),
    )

    val new = policy().copy(
      additionalConditions = additionalConditions(),
    )

    assertThat(getSuggestedReplacements(old, new)).containsExactly(
      Replacements("code-1", REMOVED_NO_REPLACEMENTS, alternatives = emptyList()),
    )
  }

  @Test
  fun `policy with no removed conditions`() {
    val old = policy().copy(
      additionalConditions = additionalConditions().copy(
        ap = listOf(
          condition().copy(code = "code-1"),
          condition().copy(code = "code-2"),
        ),
      ),
    )

    val new = policy().copy(
      additionalConditions = additionalConditions().copy(
        ap = listOf(
          condition().copy(code = "code-1"),
          condition().copy(code = "code-2"),
        ),
      ),
    )

    assertThat(getSuggestedReplacements(old, new)).isEmpty()
  }

  @Test
  fun `condition replaced with single suggestion in new licence`() {
    val old = policy().copy(
      additionalConditions = additionalConditions().copy(ap = listOf(condition().copy(code = "code-1"))),
    )

    val new = policy().copy(
      additionalConditions = additionalConditions().copy(ap = listOf(condition().copy(code = "code-2"))),
      changeHints = listOf(ChangeHint("code-1", listOf("code-2"))),
    )

    assertThat(getSuggestedReplacements(old, new)).containsExactly(
      Replacements("code-1", REPLACED, alternatives = listOf(condition().copy(code = "code-2"))),
    )
  }

  @Test
  fun `condition deleted with single suggestion that exists in old licence`() {
    val old = policy().copy(
      additionalConditions = additionalConditions().copy(
        ap = listOf(
          condition().copy(code = "code-1"),
          condition().copy(code = "code-2"),
        ),
      ),
    )

    val new = policy().copy(
      additionalConditions = additionalConditions().copy(
        ap = listOf(
          condition().copy(code = "code-2"),
        ),
      ),
      changeHints = listOf(ChangeHint("code-1", listOf("code-2"))),
    )

    assertThat(getSuggestedReplacements(old, new)).containsExactly(
      Replacements("code-1", DELETED, alternatives = listOf(condition().copy(code = "code-2"))),
    )
  }

  @Test
  fun `condition deleted with multiple suggestions that exist in old licence`() {
    val old = policy().copy(
      additionalConditions = additionalConditions().copy(
        ap = listOf(
          condition().copy(code = "code-1"),
          condition().copy(code = "code-2"),
          condition().copy(code = "code-3"),
        ),
      ),
    )

    val new = policy().copy(
      additionalConditions = additionalConditions().copy(
        ap = listOf(
          condition().copy(code = "code-2"),
          condition().copy(code = "code-3"),
        ),
      ),
      changeHints = listOf(ChangeHint("code-1", listOf("code-2", "code-3"))),
    )

    assertThat(getSuggestedReplacements(old, new)).containsExactly(
      Replacements(
        "code-1",
        DELETED,
        alternatives = listOf(condition().copy(code = "code-2"), condition().copy(code = "code-3")),
      ),
    )
  }

  @Test
  fun `condition deleted with mixed suggestions that some exist on old licence and some on new`() {
    val old = policy().copy(
      additionalConditions = additionalConditions().copy(
        ap = listOf(
          condition().copy(code = "code-1"),
          condition().copy(code = "code-2"),
          condition().copy(code = "code-3"),
        ),
      ),
    )

    val new = policy().copy(
      additionalConditions = additionalConditions().copy(
        ap = listOf(
          condition().copy(code = "code-2"),
          condition().copy(code = "code-3"),
          condition().copy(code = "code-4"),
        ),
      ),
      changeHints = listOf(ChangeHint("code-1", listOf("code-2", "code-4"))),
    )

    assertThat(getSuggestedReplacements(old, new)).containsExactly(
      Replacements(
        "code-1",
        DELETED,
        alternatives = listOf(condition().copy(code = "code-2"), condition().copy(code = "code-4")),
      ),
    )
  }

  fun policy() = LicencePolicy(
    "V1",
    standardConditions = StandardConditions(emptyList(), emptyList()),
    additionalConditions = additionalConditions(),
    changeHints = emptyList(),
  )

  fun additionalConditions() = AdditionalConditions(emptyList(), emptyList())

  fun condition() = AdditionalConditionAp(
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
}
