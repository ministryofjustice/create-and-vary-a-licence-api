package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class InputTest {

  @Nested
  inner class `getAllFieldNames` {
    @Test
    fun `returns a list of input field names`() {
      assertThat(anInput.getAllFieldNames()).isEqualTo(listOf("name"))
    }

    @Test
    fun `returned list includes nested conditional inputs`() {
      val fieldNames = anInputWithConditionalInputs.getAllFieldNames()
      assertThat(fieldNames).containsAll(
        listOf(
          "name",
          "conditionalName1",
          "conditionalName2",
        ),
      )
      assertThat(fieldNames.size).isEqualTo(3)
    }
  }

  private val anInput = Input(
    type = InputType.TEXT,
    label = "label",
    name = "name",
  )

  private val anInputWithConditionalInputs = anInput.copy(
    options = listOf(
      Option(
        value = "value",
        conditional = Conditional(
          inputs = listOf(
            ConditionalInput(
              type = InputType.TEXT,
              label = "conditional label 1",
              name = "conditionalName1",
            ),
            ConditionalInput(
              type = InputType.TEXT,
              label = "conditional label 2",
              name = "conditionalName2",
            ),
          ),
        ),
      ),
    ),
  )
}
