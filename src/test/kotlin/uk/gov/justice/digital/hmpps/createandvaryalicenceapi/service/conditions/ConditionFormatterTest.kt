package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.AdditionalConditionAp
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.Conditional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.ConditionalInput
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.Input
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.InputType.ADDRESS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.InputType.CHECK
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.InputType.DATE_PICKER
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.InputType.RADIO
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.InputType.TEXT
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.InputType.TIME_PICKER
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.Option
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions.Case.CAPITALISED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions.Case.LOWER

class ConditionFormatterTest {
  private val conditionFormatter = ConditionFormatter()

  @Test
  fun `Will return text verbatim when no placeholders exist`() {
    val conditionConfig = AdditionalConditionAp(
      code = "ed607a91-fe3a-4816-8eb9-b447c945935c",
      category = "Possession, ownership, control or inspection of specified items or documents",
      text = "Not to own or use a camera without the prior approval of your supervising officer.",
      requiresInput = false,
      categoryShort = "Items and documents",
      inputs = emptyList(),
      tpl = null,
      subtext = null,
      type = null,
    )

    val result = conditionFormatter.format(conditionConfig, emptyList())
    assertThat(result).isEqualTo("Not to own or use a camera without the prior approval of your supervising officer.")
  }

  @Test
  fun `Will replace single placeholder with a single value`() {
    val data = listOf(
      AdditionalConditionData(
        id = 1,
        dataField = "probationRegion",
        dataValue = "London",
        dataSequence = 0,
        additionalCondition = condition,
      ),
    )

    val conditionConfig = AdditionalConditionAp(
      code = "5db26ab3-9b6f-4bee-b2aa-53aa3f3be7dd",
      category = "Residence at a specific place",
      text =
      "You must reside within the [INSERT REGION] while of no fixed abode, unless otherwise approved by your supervising officer.",
      tpl =
      "You must reside within the {probationRegion} probation region while of no fixed abode, unless otherwise approved by your supervising officer.",
      requiresInput = true,
      inputs = emptyList(),
      categoryShort = null,
      subtext = null,
      type = null,
    )

    val result = conditionFormatter.format(conditionConfig, data)
    assertThat(result).isEqualTo(
      "You must reside within the London probation region while of no fixed abode, unless otherwise approved by your supervising officer.",
    )
  }

  @Test
  fun `Will remove optional placeholders that are not matched by data`() {
    val data = listOf(
      AdditionalConditionData(
        id = 1,
        dataField = "wrongName",
        dataValue = "London",
        dataSequence = 0,
        additionalCondition = condition,
      ),
    )

    val conditionConfig = AdditionalConditionAp(
      code = "5db26ab3-9b6f-4bee-b2aa-53aa3f3be7dd",
      category = "Residence at a specific place",
      text =
      "You must reside within the [INSERT REGION] while of no fixed abode, unless otherwise approved by your supervising officer.",
      tpl =
      "You must reside within the {probationRegion} probation region while of no fixed abode, unless otherwise approved by your supervising officer.",
      requiresInput = true,
      inputs = emptyList(),
      categoryShort = null,
      subtext = null,
      type = null,
    )

    val result = conditionFormatter.format(conditionConfig, data)
    assertThat(result).isEqualTo(
      "You must reside within the  probation region while of no fixed abode, unless otherwise approved by your supervising officer.",
    )
  }

  @Test
  fun `Will replace placeholders of type number`() {
    val data = listOf(
      AdditionalConditionData(
        id = 1,
        dataField = "age",
        dataValue = "18",
        dataSequence = 0,
        additionalCondition = condition,
      ),
    )

    val conditionConfig = AdditionalConditionAp(
      code = "5db26ab3-9b6f-4bee-b2aa-53aa3f3be7dd",
      category = "Participation in, or co-operation with, a programme or set of activities",
      text =
      "Not to undertake work or other organised activity which will involve a person under the age of [INSERT AGE], either on a paid or unpaid basis without the prior approval of your supervising officer.",
      tpl =
      "Not to undertake work or other organised activity which will involve a person under the age of {age}, either on a paid or unpaid basis without the prior approval of your supervising officer.",
      requiresInput = true,
      inputs = emptyList(),
      categoryShort = null,
      subtext = null,
      type = null,
    )

    val result = conditionFormatter.format(conditionConfig, data)
    assertThat(result).isEqualTo(
      "Not to undertake work or other organised activity which will involve a person under the age of 18, either on a paid or unpaid basis without the prior approval of your supervising officer.",
    )
  }

  @Test
  fun `Will replace multiple placeholders and adjust case to lower`() {
    val data = listOf(
      AdditionalConditionData(
        id = 1,
        dataField = "gender",
        dataValue = "Any",
        dataSequence = 0,
        additionalCondition = condition,
      ),
      AdditionalConditionData(
        id = 1,
        dataField = "age",
        dataValue = "18",
        dataSequence = 1,
        additionalCondition = condition,
      ),
    )

    val conditionConfig = AdditionalConditionAp(
      code = "5db26ab3-9b6f-4bee-b2aa-53aa3f3be7dd",
      category = "Participation in, or co-operation with, a programme or set of activities",
      text = "Not to reside (not even to stay for one night) in the same household as [ANY / ANY FEMALE / ANY MALE] child under the age of [INSERT AGE] without the prior approval of your supervising officer.",
      tpl = "Not to reside (not even to stay for one night) in the same household as {gender} child under the age of {age} without the prior approval of your supervising officer.",
      requiresInput = true,
      inputs = listOf(
        Input(
          type = RADIO,
          label = "Select the relevant text",
          name = "gender",
          case = LOWER,
          listType = null,
          options = emptyList(),
          handleIndefiniteArticle = null,
          addAnother = null,
          includeBefore = null,
          subtext = null,
        ),
      ),
      categoryShort = null,
      subtext = null,
      type = null,
    )

    val result = conditionFormatter.format(conditionConfig, data)
    assertThat(result).isEqualTo(
      "Not to reside (not even to stay for one night) in the same household as any child under the age of 18 without the prior approval of your supervising officer.",
    )
  }

  @Test
  fun `Will remove any placeholders without data items`() {
    val conditionConfig = AdditionalConditionAp(
      code = "5db26ab3-9b6f-4bee-b2aa-53aa3f3be7dd",
      category = "Participation in, or co-operation with, a programme or set of activities",
      text = "Not to reside (not even to stay for one night) in the same household as [ANY / ANY FEMALE / ANY MALE] child under the age of [INSERT AGE] without the prior approval of your supervising officer.",
      tpl = "Not to reside (not even to stay for one night) in the same household as {gender} child under the age of {age} without the prior approval of your supervising officer.",
      requiresInput = true,
      inputs = emptyList(),
      categoryShort = null,
      subtext = null,
      type = null,
    )

    val result = conditionFormatter.format(conditionConfig, emptyList())
    assertThat(result).isEqualTo(
      "Not to reside (not even to stay for one night) in the same household as  child under the age of  without the prior approval of your supervising officer.",
    )
  }

  @Test
  fun `Will replace placeholders for a list with and 'and' between them and include optional text for optional values`() {
    val data = listOf(
      AdditionalConditionData(
        id = 1,
        dataField = "behaviourProblems",
        dataValue = "alcohol",
        dataSequence = 0,
        additionalCondition = condition,
      ),
      AdditionalConditionData(
        id = 1,
        dataField = "behaviourProblems",
        dataValue = "drug",
        dataSequence = 1,
        additionalCondition = condition,
      ),
      AdditionalConditionData(
        id = 1,
        dataField = "course",
        dataValue = "Walthamstow Rehabilitation Clinic",
        dataSequence = 2,
        additionalCondition = condition,
      ),
    )

    val conditionConfig = AdditionalConditionAp(
      code = "5db26ab3-9b6f-4bee-b2aa-53aa3f3be7dd",
      category = "Participation in, or co-operation with, a programme or set of activities",
      text = "To comply with any requirements specified by your supervising officer for the purpose of ensuring that you address your alcohol / drug / sexual / violent / gambling / solvent abuse / anger / debt / prolific / offending behaviour problems at the [NAME OF COURSE / CENTRE].",
      tpl = "To comply with any requirements specified by your supervising officer for the purpose of ensuring that you address your {behaviourProblems} problems{course}.",
      requiresInput = true,
      inputs = listOf(
        Input(
          type = CHECK,
          label = "Select all that apply",
          name = "behaviourProblems",
          case = null,
          listType = "AND",
          options = emptyList(),
          handleIndefiniteArticle = null,
          addAnother = null,
          includeBefore = null,
          subtext = null,
        ),
        Input(
          type = TEXT,
          label = "Enter name of course or centre (optional)",
          name = "course",
          includeBefore = " at the ",
          subtext = null,
          options = emptyList(),
          handleIndefiniteArticle = null,
          addAnother = null,
          case = null,
          listType = null,
        ),
      ),
      categoryShort = null,
      subtext = null,
      type = null,
    )

    val result = conditionFormatter.format(conditionConfig, data)
    assertThat(result).isEqualTo(
      "To comply with any requirements specified by your supervising officer for the purpose of ensuring that you address your alcohol and drug problems at the Walthamstow Rehabilitation Clinic.",
    )
  }

  @Test
  fun `Will omit optional text from the sentence when an optional value is not supplied`() {
    val data = listOf(
      AdditionalConditionData(
        id = 1,
        dataField = "behaviourProblems",
        dataValue = "alcohol",
        dataSequence = 0,
        additionalCondition = condition,
      ),
      AdditionalConditionData(
        id = 1,
        dataField = "behaviourProblems",
        dataValue = "drug",
        dataSequence = 1,
        additionalCondition = condition,
      ),
    )

    val conditionConfig = AdditionalConditionAp(
      code = "5db26ab3-9b6f-4bee-b2aa-53aa3f3be7dd",
      category = "Participation in, or co-operation with, a programme or set of activities",
      text = "To comply with any requirements specified by your supervising officer for the purpose of ensuring that you address your alcohol / drug / sexual / violent / gambling / solvent abuse / anger / debt / prolific / offending behaviour problems at the [NAME OF COURSE / CENTRE].",
      tpl = "To comply with any requirements specified by your supervising officer for the purpose of ensuring that you address your {behaviourProblems} problems{course}.",
      requiresInput = true,
      inputs = listOf(
        Input(
          type = CHECK,
          label = "Select all that apply",
          name = "behaviourProblems",
          case = null,
          listType = "AND",
          options = emptyList(),
          handleIndefiniteArticle = null,
          addAnother = null,
          includeBefore = null,
          subtext = null,
        ),
        Input(
          type = TEXT,
          label = "Enter name of course or centre (optional)",
          name = "course",
          includeBefore = " at the ",
          subtext = null,
          options = emptyList(),
          handleIndefiniteArticle = null,
          addAnother = null,
          case = null,
          listType = null,
        ),
      ),
      categoryShort = null,
      subtext = null,
      type = null,
    )

    val result = conditionFormatter.format(conditionConfig, data)
    assertThat(result).isEqualTo(
      "To comply with any requirements specified by your supervising officer for the purpose of ensuring that you address your alcohol and drug problems.",
    )
  }

  @Test
  fun `Will make sense with multiple optional values - none supplied`() {
    val data = listOf(
      AdditionalConditionData(
        id = 1,
        dataField = "appointmentAddress",
        dataValue = "Harlow Clinic, High Street, London, W1 3GV",
        dataSequence = 0,
        additionalCondition = condition,
      ),
      AdditionalConditionData(
        id = 2,
        dataField = "appointmentDate",
        dataValue = "12th February 2022",
        dataSequence = 1,
        additionalCondition = condition,
      ),
    )

    val conditionConfig = AdditionalConditionAp(
      code = "5db26ab3-9b6f-4bee-b2aa-53aa3f3be7dd",
      category = "Participation in, or co-operation with, a programme or set of activities",
      text = "Attend [INSERT APPOINTMENT TIME DATE AND ADDRESS], as directed, to address your dependency on, or propensity to misuse, a controlled drug.",
      tpl = "Attend {appointmentAddress}{appointmentDate}{appointmentTime}, as directed, to address your dependency on, or propensity to misuse, a controlled drug.",
      requiresInput = true,
      inputs = listOf(
        Input(
          type = TIME_PICKER,
          label = "Enter time (optional)",
          name = "appointmentTime",
          includeBefore = " at ",
          case = null,
          listType = null,
          options = emptyList(),
          handleIndefiniteArticle = null,
          addAnother = null,
          subtext = null,
        ),
        Input(
          type = DATE_PICKER,
          label = "Enter date (optional)",
          name = "appointmentDate",
          includeBefore = " on ",
          subtext = null,
          options = emptyList(),
          handleIndefiniteArticle = null,
          addAnother = null,
          case = null,
          listType = null,
        ),
        Input(
          type = ADDRESS,
          label = "Enter the address for the appointment",
          name = "appointmentAddress",
          includeBefore = null,
          subtext = null,
          options = emptyList(),
          handleIndefiniteArticle = null,
          addAnother = null,
          case = null,
          listType = null,
        ),
      ),
      categoryShort = null,
      subtext = null,
      type = null,
    )

    val result = conditionFormatter.format(conditionConfig, data)
    assertThat(result).isEqualTo(
      "Attend Harlow Clinic, High Street, London, W1 3GV on 12th February 2022, as directed, to address your dependency on, or propensity to misuse, a controlled drug.",
    )
  }

  @Test
  fun `Will make sense with multiple optional values - two supplied`() {
    val data = listOf(
      AdditionalConditionData(
        id = 1,
        dataField = "appointmentAddress",
        dataValue = "Harlow Clinic, High Street, London, W1 3GV",
        dataSequence = 0,
        additionalCondition = condition,
      ),
      AdditionalConditionData(
        id = 2,
        dataField = "appointmentDate",
        dataValue = "12th February 2022",
        dataSequence = 1,
        additionalCondition = condition,
      ),
      AdditionalConditionData(
        id = 3,
        dataField = "appointmentTime",
        dataValue = "11=15 am",
        dataSequence = 2,
        additionalCondition = condition,
      ),
    )

    val conditionConfig = AdditionalConditionAp(
      code = "5db26ab3-9b6f-4bee-b2aa-53aa3f3be7dd",
      category = "Participation in, or co-operation with, a programme or set of activities",
      text = "Attend [INSERT APPOINTMENT TIME DATE AND ADDRESS], as directed, to address your dependency on, or propensity to misuse, a controlled drug.",
      tpl = "Attend {appointmentAddress}{appointmentDate}{appointmentTime}, as directed, to address your dependency on, or propensity to misuse, a controlled drug.",
      requiresInput = true,
      inputs = listOf(
        Input(
          type = TIME_PICKER,
          label = "Enter time (optional)",
          name = "appointmentTime",
          includeBefore = " at ",
          case = null,
          listType = null,
          options = emptyList(),
          handleIndefiniteArticle = null,
          addAnother = null,
          subtext = null,
        ),
        Input(
          type = DATE_PICKER,
          label = "Enter date (optional)",
          name = "appointmentDate",
          includeBefore = " on ",
          subtext = null,
          options = emptyList(),
          handleIndefiniteArticle = null,
          addAnother = null,
          case = null,
          listType = null,
        ),
        Input(
          type = ADDRESS,
          label = "Enter the address for the appointment",
          name = "appointmentAddress",
          includeBefore = null,
          subtext = null,
          options = emptyList(),
          handleIndefiniteArticle = null,
          addAnother = null,
          case = null,
          listType = null,
        ),
      ),
      categoryShort = null,
      subtext = null,
      type = null,
    )

    val result = conditionFormatter.format(conditionConfig, data)
    assertThat(result).isEqualTo(
      "Attend Harlow Clinic, High Street, London, W1 3GV on 12th February 2022 at 11=15 am, as directed, to address your dependency on, or propensity to misuse, a controlled drug.",
    )
  }

  @Test
  fun `Will replace placeholders for a list of values with commas and 'and' between (list type AND)`() {
    val data = listOf(
      AdditionalConditionData(
        id = 1,
        dataField = "behaviourProblems",
        dataValue = "alcohol",
        dataSequence = 0,
        additionalCondition = condition,
      ),
      AdditionalConditionData(
        id = 2,
        dataField = "behaviourProblems",
        dataValue = "drug",
        dataSequence = 1,
        additionalCondition = condition,
      ),
      AdditionalConditionData(
        id = 3,
        dataField = "behaviourProblems",
        dataValue = "sexual",
        dataSequence = 2,
        additionalCondition = condition,
      ),
      AdditionalConditionData(
        id = 4,
        dataField = "behaviourProblems",
        dataValue = "violent",
        dataSequence = 3,
        additionalCondition = condition,
      ),
      AdditionalConditionData(
        id = 5,
        dataField = "behaviourProblems",
        dataValue = "gambling",
        dataSequence = 4,
        additionalCondition = condition,
      ),
      AdditionalConditionData(
        id = 6,
        dataField = "behaviourProblems",
        dataValue = "anger",
        dataSequence = 5,
        additionalCondition = condition,
      ),
      AdditionalConditionData(
        id = 7,
        dataField = "course",
        dataValue = "AA meeting",
        dataSequence = 6,
        additionalCondition = condition,
      ),
    )

    val conditionConfig = AdditionalConditionAp(
      code = "5db26ab3-9b6f-4bee-b2aa-53aa3f3be7dd",
      category = "Participation in, or co-operation with, a programme or set of activities",
      text = "To comply with any requirements specified by your supervising officer for the purpose of ensuring that you address your alcohol / drug / sexual / violent / gambling / solvent abuse / anger / debt / prolific / offending behaviour problems at the [NAME OF COURSE / CENTRE].",
      tpl = "To comply with any requirements specified by your supervising officer for the purpose of ensuring that you address your {behaviourProblems} problems{course}.",
      requiresInput = true,
      inputs = listOf(
        Input(
          type = CHECK,
          label = "Select all that apply",
          name = "behaviourProblems",
          listType = "AND",
          includeBefore = null,
          case = null,
          options = emptyList(),
          handleIndefiniteArticle = null,
          addAnother = null,
          subtext = null,
        ),
        Input(
          type = TEXT,
          label = "Enter name of course or centre (optional)",
          name = "course",
          includeBefore = " at the ",
          subtext = null,
          options = emptyList(),
          handleIndefiniteArticle = null,
          addAnother = null,
          case = null,
          listType = null,
        ),

      ),
      categoryShort = null,
      subtext = null,
      type = null,
    )

    val result = conditionFormatter.format(conditionConfig, data)
    assertThat(result).isEqualTo(
      "To comply with any requirements specified by your supervising officer for the purpose of ensuring that you address your alcohol, drug, sexual, violent, gambling and anger problems at the AA meeting.",
    )
  }

  @Test
  fun `Will replace placeholders for a list of 2 values with an 'OR' between them (list type OR)`() {
    val data = listOf(
      AdditionalConditionData(
        id = 1,
        dataField = "name",
        dataValue = "Jane Doe",
        dataSequence = 0,
        additionalCondition = condition,
      ),
      AdditionalConditionData(
        id = 2,
        dataField = "name",
        dataValue = "John Doe",
        dataSequence = 1,
        additionalCondition = condition,
      ),
      AdditionalConditionData(
        id = 3,
        dataField = "socialServicesDepartment",
        dataValue = "East Hull Social Services",
        dataSequence = 2,
        additionalCondition = condition,
      ),
    )

    val conditionConfig = AdditionalConditionAp(
      code = "5db26ab3-9b6f-4bee-b2aa-53aa3f3be7dd",
      category = "Participation in, or co-operation with, a programme or set of activities",
      text = "Not to seek to approach or communicate with [INSERT NAME OF VICTIM AND / OR FAMILY MEMBERS] without the prior approval of your supervising officer and / or [INSERT NAME OF APPROPRIATE SOCIAL SERVICES DEPARTMENT].",
      tpl = "Not to seek to approach or communicate with {name} without the prior approval of your supervising officer{socialServicesDepartment}.",
      requiresInput = true,
      inputs = listOf(
        Input(
          type = TEXT,
          label = "Enter name of victim or family member",
          name = "name",
          listType = "OR",
          case = CAPITALISED,
          includeBefore = null,
          options = emptyList(),
          handleIndefiniteArticle = null,
          addAnother = null,
          subtext = null,
        ),
        Input(
          type = TEXT,
          label = "Enter social services department (optional)",
          name = "socialServicesDepartment",
          case = CAPITALISED,
          includeBefore = " and / or ",
          options = emptyList(),
          listType = null,
          handleIndefiniteArticle = null,
          addAnother = null,
          subtext = null,
        ),
      ),
      categoryShort = null,
      subtext = null,
      type = null,
    )
    val result = conditionFormatter.format(conditionConfig, data)
    assertThat(result).isEqualTo(
      "Not to seek to approach or communicate with Jane Doe or John Doe without the prior approval of your supervising officer and / or East Hull Social Services.",
    )
  }

  @Test
  fun `Will replace placeholders for a list of values with commas and an 'OR' between them (list type OR)`() {
    val data = listOf(
      AdditionalConditionData(
        id = 1,
        dataField = "name",
        dataValue = "Jane Doe",
        dataSequence = 0,
        additionalCondition = condition,
      ),
      AdditionalConditionData(
        id = 2,
        dataField = "name",
        dataValue = "John Doe",
        dataSequence = 1,
        additionalCondition = condition,
      ),
      AdditionalConditionData(
        id = 3,
        dataField = "name",
        dataValue = "Jack Dont",
        dataSequence = 2,
        additionalCondition = condition,
      ),
      AdditionalConditionData(
        id = 4,
        dataField = "socialServicesDepartment",
        dataValue = "East Hull Social Services",
        dataSequence = 3,
        additionalCondition = condition,
      ),
    )

    val conditionConfig = AdditionalConditionAp(
      code = "5db26ab3-9b6f-4bee-b2aa-53aa3f3be7dd",
      category = "Participation in, or co-operation with, a programme or set of activities",
      text = "Not to seek to approach or communicate with [INSERT NAME OF VICTIM AND / OR FAMILY MEMBERS] without the prior approval of your supervising officer and / or [INSERT NAME OF APPROPRIATE SOCIAL SERVICES DEPARTMENT].",
      tpl = "Not to seek to approach or communicate with {name} without the prior approval of your supervising officer{socialServicesDepartment}.",
      requiresInput = true,
      inputs = listOf(
        Input(
          type = TEXT,
          label = "Enter name of victim or family member",
          name = "name",
          listType = "OR",
          case = CAPITALISED,
          includeBefore = null,
          options = emptyList(),
          handleIndefiniteArticle = null,
          addAnother = null,
          subtext = null,
        ),
        Input(
          type = TEXT,
          label = "Enter social services department (optional)",
          name = "socialServicesDepartment",
          case = CAPITALISED,
          includeBefore = " and / or ",
          options = emptyList(),
          listType = null,
          handleIndefiniteArticle = null,
          addAnother = null,
          subtext = null,
        ),
      ),
      categoryShort = null,
      subtext = null,
      type = null,
    )
    val result = conditionFormatter.format(conditionConfig, data)
    assertThat(result).isEqualTo(
      "Not to seek to approach or communicate with Jane Doe, John Doe or Jack Dont without the prior approval of your supervising officer and / or East Hull Social Services.",
    )
  }

  @Test
  fun `Will correctly format an address`() {
    val data = listOf(
      AdditionalConditionData(
        id = 1,
        dataField = "appointmentAddress",
        dataValue = "123 Fake Street, , Fakestown, , LN123TO",
        dataSequence = 0,
        additionalCondition = condition,
      ),
    )

    val conditionConfig = AdditionalConditionAp(
      code = "5db26ab3-9b6f-4bee-b2aa-53aa3f3be7dd",
      category = "Participation in, or co-operation with, a programme or set of activities",
      text = "Attend [INSERT APPOINTMENT TIME DATE AND ADDRESS], as directed, to address your dependency on, or propensity to misuse, a controlled drug.",
      tpl = "Attend {appointmentAddress}{appointmentDate}{appointmentTime}, as directed, to address your dependency on, or propensity to misuse, a controlled drug.",
      requiresInput = true,
      inputs = listOf(
        Input(
          type = TIME_PICKER,
          label = "Enter time (optional)",
          name = "appointmentTime",
          includeBefore = " at ",
          case = null,
          listType = null,
          options = emptyList(),
          handleIndefiniteArticle = null,
          addAnother = null,
          subtext = null,
        ),
        Input(
          type = DATE_PICKER,
          label = "Enter date (optional)",
          name = "appointmentDate",
          includeBefore = " on ",
          case = null,
          options = emptyList(),
          listType = null,
          handleIndefiniteArticle = null,
          addAnother = null,
          subtext = null,
        ),
        Input(
          type = ADDRESS,
          label = "Enter the address for the appointment",
          name = "appointmentAddress",
          case = null,
          includeBefore = null,
          options = emptyList(),
          listType = null,
          handleIndefiniteArticle = null,
          addAnother = null,
          subtext = null,
        ),
      ),
      categoryShort = null,
      subtext = null,
      type = null,
    )
    val result = conditionFormatter.format(conditionConfig, data)
    assertThat(result).isEqualTo(
      "Attend 123 Fake Street, Fakestown, LN123TO, as directed, to address your dependency on, or propensity to misuse, a controlled drug.",
    )
  }

  @Test
  fun `Will replace placeholders for conditional reveal inputs`() {
    val data = listOf(
      AdditionalConditionData(
        id = 1,
        dataField = "approvedPremises",
        dataValue = "the police station",
        dataSequence = 0,
        additionalCondition = condition,
      ),
      AdditionalConditionData(
        id = 2,
        dataField = "reportingTime",
        dataValue = "2pm",
        dataSequence = 1,
        additionalCondition = condition,
      ),
      AdditionalConditionData(
        id = 3,
        dataField = "reviewPeriod",
        dataValue = "Other",
        dataSequence = 2,
        additionalCondition = condition,
      ),
      AdditionalConditionData(
        id = 4,
        dataField = "alternativeReviewPeriod",
        dataValue = "Fortnightly",
        dataSequence = 3,
        additionalCondition = condition,
      ),
    )

    val conditionConfig = AdditionalConditionAp(
      code = "5db26ab3-9b6f-4bee-b2aa-53aa3f3be7dd",
      category = "Participation in, or co-operation with, a programme or set of activities",
      text = "Report to staff at [NAME OF APPROVED PREMISES] at [TIME / DAILY], unless otherwise authorised by your supervising officer. This condition will be reviewed by your supervising officer on a [WEEKLY / MONTHLY / ETC] basis and may be amended or removed if it is felt that the level of risk you present has reduced appropriately.",
      tpl = "Report to staff at {approvedPremises} at {reportingTime}, unless otherwise authorised by your supervising officer. This condition will be reviewed by your supervising officer on {alternativeReviewPeriod || reviewPeriod} basis and may be amended or removed if it is felt that the level of risk you present has reduced appropriately.",
      requiresInput = true,
      inputs = listOf(
        Input(
          type = TEXT,
          label = "Enter name of approved premises",
          name = "approvedPremises",
          case = CAPITALISED,
          includeBefore = null,
          listType = null,
          options = emptyList(),
          handleIndefiniteArticle = null,
          addAnother = null,
          subtext = null,
        ),
        Input(
          type = TIME_PICKER,
          label = "Enter a reporting time",
          name = "reportingTime",
          includeBefore = null,
          case = null,
          options = emptyList(),
          listType = null,
          handleIndefiniteArticle = null,
          addAnother = null,
          subtext = null,
        ),
        Input(
          type = RADIO,
          label = "Select a review period",
          name = "reviewPeriod",
          includeBefore = null,
          case = null,
          options = listOf(
            Option(
              value = "Other",
              conditional = Conditional(
                inputs = listOf(
                  ConditionalInput(
                    type = TEXT,
                    label = "Enter a review period",
                    name = "alternativeReviewPeriod",
                    case = LOWER,
                    handleIndefiniteArticle = true,
                    includeBefore = null,
                    subtext = null,
                  ),
                ),
              ),
            ),
          ),
          listType = null,
          handleIndefiniteArticle = true,
          addAnother = null,
          subtext = null,
        ),
      ),
      categoryShort = null,
      subtext = null,
      type = null,
    )

    val result = conditionFormatter.format(conditionConfig, data)
    assertThat(result).isEqualTo(
      "Report to staff at The Police Station at 2pm, unless otherwise authorised by your supervising officer. This condition will be reviewed by your supervising officer on a fortnightly basis and may be amended or removed if it is felt that the level of risk you present has reduced appropriately.",
    )
  }

  @Test
  fun `Will replace adjust wording where values start with vowel`() {
    val data = listOf(
      AdditionalConditionData(
        id = 1,
        dataField = "approvedPremises",
        dataValue = "the police station",
        dataSequence = 0,
        additionalCondition = condition,
      ),
      AdditionalConditionData(
        id = 2,
        dataField = "reportingTime",
        dataValue = "2pm",
        dataSequence = 1,
        additionalCondition = condition,
      ),
      AdditionalConditionData(
        id = 3,
        dataField = "reviewPeriod",
        dataValue = "Other",
        dataSequence = 2,
        additionalCondition = condition,
      ),
      AdditionalConditionData(
        id = 4,
        dataField = "alternativeReviewPeriod",
        dataValue = "ongoing",
        dataSequence = 3,
        additionalCondition = condition,
      ),
    )

    val conditionConfig = AdditionalConditionAp(
      code = "5db26ab3-9b6f-4bee-b2aa-53aa3f3be7dd",
      category = "Participation in, or co-operation with, a programme or set of activities",
      text = "Report to staff at [NAME OF APPROVED PREMISES] at [TIME / DAILY], unless otherwise authorised by your supervising officer. This condition will be reviewed by your supervising officer on a [WEEKLY / MONTHLY / ETC] basis and may be amended or removed if it is felt that the level of risk you present has reduced appropriately.",
      tpl = "Report to staff at {approvedPremises} at {reportingTime}, unless otherwise authorised by your supervising officer. This condition will be reviewed by your supervising officer on {alternativeReviewPeriod || reviewPeriod} basis and may be amended or removed if it is felt that the level of risk you present has reduced appropriately.",
      requiresInput = true,
      inputs = listOf(
        Input(
          type = TEXT,
          label = "Enter name of approved premises",
          name = "approvedPremises",
          case = CAPITALISED,
          includeBefore = null,
          listType = null,
          options = emptyList(),
          handleIndefiniteArticle = null,
          addAnother = null,
          subtext = null,
        ),
        Input(
          type = TIME_PICKER,
          label = "Enter a reporting time",
          name = "reportingTime",
          includeBefore = null,
          case = null,
          options = emptyList(),
          listType = null,
          handleIndefiniteArticle = null,
          addAnother = null,
          subtext = null,
        ),
        Input(
          type = RADIO,
          label = "Select a review period",
          name = "reviewPeriod",
          includeBefore = null,
          case = null,
          options = listOf(
            Option(
              value = "Other",
              conditional = Conditional(
                inputs = listOf(
                  ConditionalInput(
                    type = TEXT,
                    label = "Enter a review period",
                    name = "alternativeReviewPeriod",
                    case = LOWER,
                    handleIndefiniteArticle = true,
                    includeBefore = null,
                    subtext = null,
                  ),
                ),
              ),
            ),
          ),
          listType = null,
          handleIndefiniteArticle = true,
          addAnother = null,
          subtext = null,
        ),
      ),
      categoryShort = null,
      subtext = null,
      type = null,
    )

    val result = conditionFormatter.format(conditionConfig, data)
    assertThat(result).isEqualTo(
      "Report to staff at The Police Station at 2pm, unless otherwise authorised by your supervising officer. This condition will be reviewed by your supervising officer on an ongoing basis and may be amended or removed if it is felt that the level of risk you present has reduced appropriately.",
    )
  }

  val licence = TestData.createCrdLicence()

  val condition = AdditionalCondition(
    id = 1,
    conditionCode = "5db26ab3-9b6f-4bee-b2aa-53aa3f3be7dd",
    conditionCategory = "Residence at a specific place",
    conditionSequence = 0,
    conditionText = "You must reside within the [INSERT REGION] while of no fixed abode, unless otherwise approved by your supervising officer.",
    additionalConditionData = mutableListOf(),
    additionalConditionUploadSummary = emptyList(),
    conditionVersion = "1.0",
    conditionType = "AP",
    licence = licence,
  )
}
