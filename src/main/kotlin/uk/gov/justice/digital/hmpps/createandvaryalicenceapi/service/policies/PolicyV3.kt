package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.AddAnother
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.AdditionalConditionAp
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.AdditionalConditionPss
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.AdditionalConditions
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.ChangeHint
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.Conditional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.ConditionalInput
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.Input
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.InputType.ADDRESS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.InputType.CHECK
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.InputType.DATE_PICKER
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.InputType.FILE_UPLOAD
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.InputType.RADIO
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.InputType.TEXT
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.InputType.TIME_PICKER
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.LicencePolicy
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.Option
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.StandardConditionAp
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.StandardConditionPss
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.StandardConditions
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.Case.CAPITALISED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.Case.LOWER

val POLICY_V3 = LicencePolicy(
  additionalConditions = AdditionalConditions(
    ap = listOf(
      AdditionalConditionAp(
        category = "Residence at a specific place",
        code = "5db26ab3-9b6f-4bee-b2aa-53aa3f3be7dd",
        inputs = listOf(
          Input(
            label = "Select the probation region",
            name = "probationRegion",
            options = listOf(
              Option(
                value = "North East",
              ),
              Option(
                value = "North West",
              ),
              Option(
                value = "Greater Manchester",
              ),
              Option(
                value = "Yorkshire and Humberside",
              ),
              Option(
                value = "East Midlands",
              ),
              Option(
                value = "West Midlands",
              ),
              Option(
                value = "East of England",
              ),
              Option(
                value = "South West",
              ),
              Option(
                value = "South Central",
              ),
              Option(
                value = "London",
              ),
              Option(
                value = "Kent, Surrey and Sussex",
              ),
              Option(
                value = "Wales",
              ),
            ),
            type = RADIO,
          ),
        ),
        requiresInput = true,
        text = "You must reside overnight within [REGION] probation region while of no fixed abode, unless otherwise approved by your supervising officer.",
        tpl = "You must reside overnight within {probationRegion} probation region while of no fixed abode, unless otherwise approved by your supervising officer.",
        type = "RegionOfResidence",
      ),
      AdditionalConditionAp(
        category = "Restriction of residency",
        code = "fce34fb2-02f4-4eb0-9b8d-d091e11451fa",
        inputs = listOf(
          Input(
            case = LOWER,
            label = "Select the relevant text",
            name = "gender",
            options = listOf(
              Option(
                value = "any",
              ),
              Option(
                value = "any female",
              ),
              Option(
                value = "any male",
              ),
            ),
            type = RADIO,
          ),
          Input(
            label = "Enter the relevant age",
            name = "age",
            type = TEXT,
          ),
        ),
        requiresInput = true,
        text = "Not to reside (not even to stay for one night) in the same household as [ANY / ANY FEMALE / ANY MALE] child under the age of [INSERT AGE] without the prior approval of your supervising officer.",
        tpl = "Not to reside (not even to stay for one night) in the same household as {gender} child under the age of {age} without the prior approval of your supervising officer.",
        type = "RestrictionOfResidencyPolicyV3",
      ),
      AdditionalConditionAp(
        category = "Making or maintaining contact with a person",
        categoryShort = "Contact with a person",
        code = "b72fdbf2-0dc9-4e7f-81e4-c2ccb5d1bc90",
        inputs = listOf(
          Input(
            case = LOWER,
            label = "Select all the relevant options",
            listType = "AND",
            name = "appointmentType",
            options = listOf(
              Option("Psychiatrist"),
              Option("Psychologist"),
              Option("Medical practitioner"),
            ),
            type = CHECK,
          ),
        ),
        requiresInput = true,
        text = "Attend all appointments arranged for you with a [PSYCHIATRIST / PSYCHOLOGIST / MEDICAL PRACTITIONER] unless otherwise approved by your supervising officer.",
        tpl = "Attend all appointments arranged for you with a {appointmentType} unless otherwise approved by your supervising officer.",
        type = "MedicalAppointmentType",
      ),
      AdditionalConditionAp(
        category = "Making or maintaining contact with a person",
        categoryShort = "Contact with a person",
        code = "9ae2a336-3491-4667-aaed-dd852b09b4b9",
        requiresInput = false,
        text = "Receive home visits from a Mental Health Worker.",
      ),
      AdditionalConditionAp(
        category = "Making or maintaining contact with a person",
        categoryShort = "Contact with a person",
        code = "75a6aac6-02a7-4414-af14-942be6736892",
        requiresInput = false,
        text = "Should you return to the UK and Islands before the expiry date of your licence then your licence conditions will be in force and you must report within two working days to your supervising officer.",
      ),
      AdditionalConditionAp(
        category = "Making or maintaining contact with a person",
        categoryShort = "Contact with a person",
        code = "4858cd8b-bca6-4f11-b6ee-439e27216d7d",
        inputs = listOf(
          Input(
            addAnother = AddAnother("Add another person"),
            case = CAPITALISED,
            label = "Enter name of victim or family member",
            listType = "OR",
            name = "name",
            type = TEXT,
          ),
          Input(
            case = CAPITALISED,
            includeBefore = " and / or ",
            label = "Enter social services department (optional)",
            name = "socialServicesDepartment",
            type = TEXT,
          ),
        ),
        requiresInput = true,
        text = "Not to seek to approach or communicate with [INSERT NAME OF VICTIM AND / OR FAMILY MEMBERS] without the prior approval of your supervising officer and / or [INSERT NAME OF APPROPRIATE SOCIAL SERVICES DEPARTMENT].",
        tpl = "Not to seek to approach or communicate with {name} without the prior approval of your supervising officer{socialServicesDepartment}.",
        type = "NoContactWithVictim",
      ),
      AdditionalConditionAp(
        category = "Making or maintaining contact with a person",
        categoryShort = "Contact with a person",
        code = "4a5fed48-0fb9-4711-8ddf-b46ddfd90246",
        inputs = listOf(
          Input(
            case = LOWER,
            label = "Select the relevant gender",
            name = "gender",
            options = listOf(
              Option(
                value = "any",
              ),
              Option(
                value = "any female",
              ),
              Option(
                value = "any male",
              ),
            ),
            type = RADIO,
          ),
          Input(
            label = "Select the relevant age",
            name = "age",
            type = TEXT,
          ),
          Input(
            case = CAPITALISED,
            includeBefore = " and / or ",
            label = "Enter social services department (optional)",
            name = "socialServicesDepartment",
            type = TEXT,
          ),
        ),
        requiresInput = true,
        text = "Not to have unsupervised contact with [ANY / ANY FEMALE / ANY MALE] children under the age of [INSERT AGE] without the prior approval of your supervising officer and / or [INSERT NAME OF APPROPRIATE SOCIAL SERVICES DEPARTMENT] except where that contact is inadvertent and not reasonably avoidable in the course of lawful daily life.",
        tpl = "Not to have unsupervised contact with {gender} children under the age of {age} without the prior approval of your supervising officer{socialServicesDepartment} except where that contact is inadvertent and not reasonably avoidable in the course of lawful daily life.",
        type = "UnsupervisedContactPolicyV3",
      ),
      AdditionalConditionAp(
        category = "Making or maintaining contact with a person",
        categoryShort = "Contact with a person",
        code = "355700a9-6184-40c0-9759-0dfed1994e1e",
        inputs = listOf(
          Input(
            addAnother = AddAnother("Add another person"),
            case = CAPITALISED,
            label = "Enter name of offender or individual",
            listType = "OR",
            name = "nameOfIndividual",
            type = TEXT,
          ),
        ),
        requiresInput = true,
        text = "Not to contact or associate with [NAMED OFFENDER(S) / NAMED INDIVIDUAL(S)] without the prior approval of your supervising officer.",
        tpl = "Not to contact or associate with {nameOfIndividual} without the prior approval of your supervising officer.",
        type = "NamedIndividuals",
      ),
      AdditionalConditionAp(
        category = "Making or maintaining contact with a person",
        categoryShort = "Contact with a person",
        code = "0aa669bf-db8a-4b8e-b8ba-ca82fc245b94",
        requiresInput = false,
        text = "Not to contact or associate with a known sex offender other than when compelled by attendance at a Treatment Programme or when residing at Approved Premises without the prior approval of your supervising officer.",
      ),
      AdditionalConditionAp(
        category = "Making or maintaining contact with a person",
        categoryShort = "Contact with a person",
        code = "cc80d02b-0b62-4940-bac6-0bcd374c725e",
        requiresInput = false,
        text = "Not to contact directly or indirectly any person who is a serving or remand prisoner or detained in State custody, without the prior approval of your supervising officer.",
      ),
      AdditionalConditionAp(
        category = "Making or maintaining contact with a person",
        categoryShort = "Contact with a person",
        code = "18b69f61-800f-46b2-95c4-2019d33e34d6",
        inputs = listOf(
          Input(
            addAnother = AddAnother("Add another group or organisation"),
            label = "Enter the name of group or organisation",
            listType = "OR",
            name = "nameOfOrganisation",
            type = TEXT,
          ),
        ),
        requiresInput = true,
        text = "Not to associate with any person currently or formerly associated with [NAMES OF SPECIFIC GROUPS OR ORGANISATIONS] without the prior approval of your supervising officer.",
        tpl = "Not to associate with any person currently or formerly associated with {nameOfOrganisation} without the prior approval of your supervising officer.",
        type = "NamedOrganisation",
      ),
      AdditionalConditionAp(
        category = "Making or maintaining contact with a person",
        categoryShort = "Contact with a person",
        code = "48c4ae87-b8d8-45d1-aded-daefe8ad07fe",
        requiresInput = false,
        text = "Not to engage or attempt to engage with commercial sexual services. Including companionship/friendship style services. This includes all services whether sexual/intimate or not.",
      ),
      HARD_STOP_CONDITION,
      AdditionalConditionAp(
        category = "Making or maintaining contact with a person",
        categoryShort = "Contact with a person",
        code = "86e7a2cb-33b5-4079-84a4-f6579347c890",
        requiresInput = false,
        text = "Not to approach or contact any employee, contractor or volunteer working on behalf of HM Prison and Probation Service directly or indirectly outside of appointments/reporting requirements without the prior approval of your supervising officer.",
      ),
      AdditionalConditionAp(
        category = "Making or maintaining contact with a person",
        categoryShort = "Contact with a person",
        code = "1905aa46-59dd-4eb7-b009-81467cc8c426",
        requiresInput = false,
        text = "Not to attempt to bribe, blackmail or coerce employee, contractor or volunteer working on behalf of HM Prison and Probation Service directly or indirectly to undertake actions on your behalf.",
      ),
      AdditionalConditionAp(
        category = "Participation in, or co-operation with, a programme or set of activities",
        categoryShort = "Programmes or activities",
        code = "89e656ec-77e8-4832-acc4-6ec05d3e9a98",
        inputs = listOf(
          Input(
            label = "Select all that apply",
            listType = "AND",
            name = "behaviourProblems",
            options = listOf(
              Option(
                value = "sexual offending",
              ),
              Option(
                value = "violent offending",
              ),
              Option(
                value = "gambling",
              ),
              Option(
                value = "prolific offending",
              ),
              Option(
                value = "offending behaviour",
              ),
            ),
            type = CHECK,
          ),
        ),
        requiresInput = true,
        text = "To comply with any requirements specified by your supervising officer for the purpose of ensuring that you address your [SEXUAL OFFENDING / VIOLENT OFFENDING / GAMBLING / PROLIFIC OFFENDING / OFFENDING BEHAVIOUR].",
        tpl = "To comply with any requirements specified by your supervising officer for the purpose of ensuring that you address your {behaviourProblems}.",
        type = "BehaviourProblems",
      ),
      AdditionalConditionAp(
        category = "Participation in, or co-operation with, a programme or set of activities",
        categoryShort = "Programmes or activities",
        code = "9da214a3-c6ae-45e1-a465-12e22adf7c87",
        inputs = listOf(
          Input(
            label = "Select the relevant age",
            name = "age",
            type = TEXT,
          ),
        ),
        requiresInput = true,
        text = "Not to undertake work or other organised activity which will involve a person under the age of [INSERT AGE], either on a paid or unpaid basis without the prior approval of your supervising officer.",
        tpl = "Not to undertake work or other organised activity which will involve a person under the age of {age}, either on a paid or unpaid basis without the prior approval of your supervising officer.",
        type = "WorkingWithChildrenPolicyV3",
      ),
      AdditionalConditionAp(
        category = "Participation in, or co-operation with, a programme or set of activities",
        categoryShort = "Programmes or activities",
        code = "0EDB6D01-46B6-408F-971C-0EBFF5FA93F0",
        requiresInput = false,
        text = "To engage with the Integrated Offender Management Team, and follow their instructions.",
      ),
      AdditionalConditionAp(
        category = "Participation in, or co-operation with, a programme or set of activities",
        categoryShort = "Programmes or activities",
        code = "625feb82-a5ab-4490-82e9-241218f775c5",
        inputs = listOf(
          Input(
            label = "Select all that apply",
            listType = "AND",
            name = "services",
            options = listOf(
              Option(
                value = "housing",
              ),
              Option(
                value = "benefits",
              ),

              Option(
                value = "early help",
              ),

              Option(
                value = "children's services",
              ),

              Option(
                value = "your support networks",
              ),

              Option(
                value = "an education provider",
              ),
            ),
            type = CHECK,
          ),
        ),
        requiresInput = true,
        text = "To comply with any requirements specified by your supervising officer to register and engage with [HOUSING / BENEFITS / EARLY HELP / CHILDREN'S SERVICES / YOUR SUPPORT NETWORKS / AN EDUCATION PROVIDER].",
        tpl = "To comply with any requirements specified by your supervising officer to register and engage with {services}.",
        type = "RegisterForServices",
      ),
      AdditionalConditionAp(
        category = "Participation in, or co-operation with, a programme or set of activities",
        categoryShort = "Programmes or activities",
        code = "b149bc47-b279-427b-9ddc-c472fdc74ba5",
        requiresInput = false,
        text = "To attend and engage with any appointments with staff at Job Centre Plus, unless otherwise approved by your supervising officer.",
      ),
      AdditionalConditionAp(
        category = "Possession, ownership, control or inspection of specified items or documents",
        categoryShort = "Items and documents",
        code = "8e52e16e-1abf-4251-baca-2fabfcb243d0",
        requiresInput = false,
        text = "Not to own or possess more than one mobile phone or SIM card without the prior approval of your supervising officer and to provide your supervising officer with details of that mobile telephone or one you have regular use of, including the IMEI number and the SIM card that you possess.",
      ),
      AdditionalConditionAp(
        category = "Possession, ownership, control or inspection of specified items or documents",
        categoryShort = "Items and documents",
        code = "5fa04bbf-6b7c-4b65-9388-a0115cd365a6",
        requiresInput = false,
        text = "To surrender your passport(s) to your supervising officer and to notify your supervising officer of any intention to apply for a new passport.",
      ),
      AdditionalConditionAp(
        category = "Possession, ownership, control or inspection of specified items or documents",
        categoryShort = "Items and documents",
        code = "bfbc693c-ab65-4042-920e-ddb085bc7aba",
        requiresInput = false,
        text = "Not to use or access any computer or device which is internet enabled without the prior approval of your supervising officer; and only for the purpose, and only at a specific location, as specified by that officer.",
      ),
      AdditionalConditionAp(
        category = "Possession, ownership, control or inspection of specified items or documents",
        categoryShort = "Items and documents",
        code = "2d67f68a-8adf-47a9-a68d-a6fc9f2c4556",
        inputs = listOf(
          Input(
            label = "Select all that apply",
            listType = "OR",
            name = "deviceTypes",
            options = listOf(
              Option(
                value = "internet enabled device",
              ),
              Option(
                value = "computer",
              ),
              Option(
                value = "mobile phone",
              ),
              Option(
                value = "digital cameras",
              ),
            ),
            type = CHECK,
          ),
        ),
        requiresInput = true,
        text = "Not to delete the usage history on any [INTERNET ENABLED DEVICE / COMPUTER / MOBILE PHONE / DIGITAL CAMERAS] used and to allow such items to be inspected as requested. Such inspection may include removal of the device for inspection and the installation of monitoring software.",
        tpl = "Not to delete the usage history on any {deviceTypes} used and to allow such items to be inspected as requested. Such inspection may include removal of the device for inspection and the installation of monitoring software.",
        type = "UsageHistory",
      ),
      AdditionalConditionAp(
        category = "Possession, ownership, control or inspection of specified items or documents",
        categoryShort = "Items and documents",
        code = "3932e5c9-4d21-4251-a747-ce6dc52dc9c0",
        inputs = listOf(
          Input(
            addAnother = AddAnother("Add another item"),
            handleIndefiniteArticle = true,
            label = "Enter the name of the item",
            listType = "OR",
            name = "item",
            type = TEXT,
          ),
        ),
        requiresInput = true,
        text = "Not to own or possess a [SPECIFIED ITEM] without the prior approval of your supervising officer.",
        tpl = "Not to own or possess {item} without the prior approval of your supervising officer.",
        type = "SpecifiedItem",
      ),
      AdditionalConditionAp(
        category = "Disclosure of information",
        code = "2a93b784-b8cb-49ed-95e2-a0df60723cda",
        requiresInput = false,
        text = "Provide your supervising officer with details (such as make, model, colour, registration) of any vehicle you own, hire for more than a short journey or have regular use of, prior to any journey taking place.",
      ),
      AdditionalConditionAp(
        category = "Disclosure of information",
        code = "db2d7e24-b130-4c7e-a1bf-6bb5f3036c02",
        requiresInput = false,
        text = "Notify your supervising officer of any developing relationships, including status changes such as engagement, marriage, pregnancies or the ending of any relationships, and disclose the details of the person you are in a relationship with.",
      ),
      AdditionalConditionAp(
        category = "Disclosure of information",
        code = "c5e91330-748d-46f3-93f6-bbe5ea8324ce",
        requiresInput = false,
        text = "Notify your supervising officer of any developing personal relationships, whether intimate or not, with any person you know or believe to be resident in a household containing children under the age of 18. This includes persons known to you prior to your time in custody with whom you are renewing or developing a personal relationship with.",
      ),
      AdditionalConditionAp(
        category = "Disclosure of information",
        code = "79ac033f-9d7a-4dab-8344-475106e58b71",
        requiresInput = false,
        text = "To notify your supervising officer of the details of any passport that you possess (including passport number), and of any intention to apply for a new passport.",
      ),
      AdditionalConditionAp(
        category = "Disclosure of information",
        code = "8686a815-b7f0-43b6-9886-f01df6a48773",
        inputs = listOf(
          Input(
            label = "Select all that apply",
            listType = "AND",
            name = "accountTypes",
            options = listOf(
              Option(
                value = "bank accounts",
              ),
              Option(
                value = "crypto currency accounts or wallets",
              ),
            ),
            type = CHECK,
          ),
        ),
        requiresInput = true,
        text = "Provide your supervising officer with the details of any [BANK ACCOUNTS / CREDIT CARDS / CRYPTO CURRENCY ACCOUNTS OR WALLETS] to which you have access or control over, including those held by a third party. You must also notify your supervising officer when you have access or control over any new accounts/wallets, and provide the details.",
        tpl = "Provide your supervising officer with the details of any {accountTypes} to which you have access or control over, including those held by a third party. You must also notify your supervising officer when you have access or control over any new accounts/wallets, and provide the details.",
        type = "BankAccountDetails",
      ),
      AdditionalConditionAp(
        category = "Curfew arrangement",
        code = "0a370862-5426-49c1-b6d4-3d074d78a81a",
        inputs = listOf(
          Input(
            label = "Select the number of curfews needed",
            name = "numberOfCurfews",
            options = listOf(
              Option(
                value = "One curfew",
                conditional = Conditional(
                  inputs = listOf(
                    ConditionalInput(
                      label = "Enter the curfew start time",
                      name = "curfewStart",
                      type = TIME_PICKER,
                    ),
                    ConditionalInput(
                      label = "Enter the curfew end time",
                      name = "curfewEnd",
                      type = TIME_PICKER,
                    ),
                  ),
                ),
              ),
              Option(
                value = "Two curfews",
                conditional = Conditional(
                  inputs = listOf(
                    ConditionalInput(
                      label = "First curfew – enter the start time",
                      name = "curfewStart",
                      type = TIME_PICKER,
                    ),
                    ConditionalInput(
                      label = "First curfew – enter the end time",
                      name = "curfewEnd",
                      type = TIME_PICKER,
                    ),
                    ConditionalInput(
                      label = "Second curfew – enter the start time",
                      name = "curfewStart2",
                      type = TIME_PICKER,
                    ),
                    ConditionalInput(
                      label = "Second curfew – enter the end time",
                      name = "curfewEnd2",
                      type = TIME_PICKER,
                    ),
                  ),
                ),
              ),
              Option(
                value = "Three curfews",
                conditional = Conditional(
                  inputs = listOf(
                    ConditionalInput(
                      label = "First curfew – enter the start time",
                      name = "curfewStart",
                      type = TIME_PICKER,
                    ),
                    ConditionalInput(
                      label = "First curfew – enter the end time",
                      name = "curfewEnd",
                      type = TIME_PICKER,
                    ),
                    ConditionalInput(
                      label = "Second curfew – enter the start time",
                      name = "curfewStart2",
                      type = TIME_PICKER,
                    ),
                    ConditionalInput(
                      label = "Second curfew – enter the end time",
                      name = "curfewEnd2",
                      type = TIME_PICKER,
                    ),
                    ConditionalInput(
                      label = "Third curfew – enter the start time",
                      name = "curfewStart3",
                      type = TIME_PICKER,
                    ),
                    ConditionalInput(
                      label = "Third curfew – enter the end time",
                      name = "curfewEnd3",
                      type = TIME_PICKER,
                    ),
                  ),
                ),
              ),
            ),
            type = RADIO,
          ),
          Input(
            case = LOWER,
            handleIndefiniteArticle = true,
            label = "Select a review period",
            name = "reviewPeriod",
            options = listOf(
              Option(
                value = "Weekly",
              ),
              Option(
                value = "Monthly",
              ),
              Option(
                conditional = Conditional(
                  inputs = listOf(
                    ConditionalInput(
                      case = LOWER,
                      handleIndefiniteArticle = true,
                      label = "Enter a review period",
                      name = "alternativeReviewPeriod",
                      type = TEXT,
                    ),
                  ),
                ),
                value = "Other",
              ),
            ),
            type = RADIO,
          ),
        ),
        requiresInput = true,
        text = "Confine yourself to an address approved by your supervising officer between the hours of [TIME] and [TIME] daily unless otherwise authorised by your supervising officer. This condition will be reviewed by your supervising officer on a [WEEKLY / MONTHLY / ETC] basis and may be amended or removed if it is felt that the level of risk that you present has reduced appropriately.",
        tpl = "Confine yourself to an address approved by your supervising officer between the hours of {curfewStart} and {curfewEnd} daily unless otherwise authorised by your supervising officer. This condition will be reviewed by your supervising officer on {alternativeReviewPeriod || reviewPeriod} basis and may be amended or removed if it is felt that the level of risk that you present has reduced appropriately.",
        type = "CurfewTerms",
      ),
      AdditionalConditionAp(
        category = "Freedom of movement",
        code = "0f9a20f4-35c7-4c77-8af8-f200f153fa11",
        inputs = listOf(
          Input(
            label = "Select a PDF map of the area this person must not enter",
            name = "outOfBoundFilename",
            type = FILE_UPLOAD,
          ),
        ),
        requiresInput = true,
        text = "Not to enter the area as defined by the attached map without the prior approval of your supervising officer.",
        tpl = "Not to enter the area as defined by the attached map without the prior approval of your supervising officer.",
        type = "OutOfBoundsRegionPolicyV3",
      ),
      AdditionalConditionAp(
        category = "Freedom of movement",
        code = "42f71b40-84cd-446d-8647-f00bbb6c079c",
        inputs = listOf(
          Input(
            label = "Choose what information to enter",
            name = "nameTypeAndOrAddress",
            options = listOf(
              Option(
                conditional = Conditional(
                  inputs = listOf(
                    ConditionalInput(
                      label = "Name or type of premises",
                      name = "nameOfPremises",
                      type = TEXT,
                    ),
                  ),
                ),
                value = "Just a name or type of premises",
              ),
              Option(
                value = "Just an address",
                conditional = Conditional(
                  inputs = listOf(
                    ConditionalInput(
                      label = "Enter the address of the premises",
                      name = "premisesAddress",
                      type = ADDRESS,
                    ),
                  ),
                ),
              ),
              Option(
                value = "A name or type of premises and an address",
                conditional = Conditional(
                  inputs = listOf(
                    ConditionalInput(
                      label = "Name or type of premises",
                      name = "nameOfPremises",
                      type = TEXT,
                    ),
                    ConditionalInput(
                      label = "Enter the address of the premises",
                      name = "premisesAddress",
                      type = ADDRESS,
                    ),
                  ),
                ),
              ),
            ),
            type = RADIO,
          ),
        ),
        requiresInput = true,
        text = "Not to enter [NAME / TYPE OF PREMISES / ADDRESS / ROAD] without the prior approval of your supervising officer.",
        tpl = "Not to enter {nameOfPremises} {premisesAddress} without the prior approval of your supervising officer.",
        type = "OutOfBoundsPremises",
      ),
      AdditionalConditionAp(
        category = "Freedom of movement",
        code = "c4a17002-88a3-43b4-b3f7-82ff476cb217",
        inputs = listOf(
          Input(
            case = LOWER,
            label = "Enter area or type of premises",
            name = "typeOfPremises",
            type = TEXT,
          ),
        ),
        requiresInput = true,
        text = "Not to enter or remain in sight of any [CHILDREN'S PLAY AREA, SWIMMING BATHS, SCHOOL ETC] without the prior approval of your supervising officer.",
        tpl = "Not to enter or remain in sight of any {typeOfPremises} without the prior approval of your supervising officer.",
        type = "OutOfBoundsPremisesType",
      ),
      AdditionalConditionAp(
        category = "Freedom of movement",
        code = "5d0416a9-a4ce-4b2c-8636-0b7abaa3680a",
        requiresInput = false,
        text = "To only attend places of worship which have been previously agreed with your supervising officer.",
      ),
      AdditionalConditionAp(
        category = "Freedom of movement",
        code = "99195049-f355-46fb-b7d8-aef87a1b19c5",
        inputs = listOf(
          Input(
            label = "Enter the name of the event",
            name = "eventName",
            type = TEXT,
          ),
          Input(
            label = "Select a PDF map of the area this person must not enter",
            name = "outOfBoundFilename",
            type = FILE_UPLOAD,
          ),
        ),
        requiresInput = true,
        text = "Not to enter the area as defined by the attached map, during the period that [NAME OF EVENT] takes place, including all occasions that the event takes place, without the prior permission of your supervising officer.",
        tpl = "Not to enter the area as defined by the attached map, during the period that {eventName} takes place, including all occasions that the event takes place, without the prior permission of your supervising officer.",
        type = "OutOfBoundsEvent",
      ),
      AdditionalConditionAp(
        category = "Freedom of movement",
        code = "1e9a66c6-f083-4c29-b209-b625252afbe5",
        requiresInput = false,
        text = "Notify your supervising officer of any travel outside of your home county, including on public transport, prior to any such journey taking place unless otherwise specified by your supervising officer.",
      ),
      AdditionalConditionAp(
        category = "Supervision in the community by the supervising officer, or other responsible officer, or organisation",
        categoryShort = "Supervision in the community",
        code = "4673ebe4-9fc0-4e48-87c9-eb17d5280867",
        inputs = listOf(
          Input(
            label = "Choose what information to enter",
            name = "addressOrGeneric",
            options = listOf(
              Option(
                value = "The approved premises where you reside",
              ),
              Option(
                value = "Name of approved premises",
                conditional = Conditional(
                  inputs = listOf(
                    ConditionalInput(
                      label = "Enter name of approved premises",
                      name = "approvedPremises",
                      type = TEXT,
                    ),
                  ),
                ),
              ),
            ),
            type = RADIO,
          ),
          Input(
            label = "Select when the person needs to report",
            name = "reportingFrequency",
            options = listOf(
              Option(
                value = "Daily",
              ),
              Option(
                value = "Monday to Friday",
              ),
              Option(
                conditional = Conditional(
                  inputs = listOf(
                    ConditionalInput(
                      label = "Enter the other frequency",
                      name = "alternativeReportingFrequency",
                      type = TEXT,
                    ),
                  ),
                ),
                value = "Other",
              ),
            ),
            type = RADIO,
          ),
          Input(
            label = "Select how many times each day they need to report",
            name = "numberOfReportingTimes",
            options = listOf(
              Option(
                conditional = Conditional(
                  inputs = listOf(
                    ConditionalInput(
                      label = "Enter the reporting time",
                      name = "reportingTime",
                      type = TIME_PICKER,
                    ),
                  ),
                ),
                value = "Once a day",
              ),
              Option(
                conditional = Conditional(
                  inputs = listOf(
                    ConditionalInput(
                      label = "Enter the first reporting time",
                      name = "reportingTime1",
                      type = TIME_PICKER,
                    ),
                    ConditionalInput(
                      includeBefore = " and ",
                      label = "Enter the second reporting time",
                      name = "reportingTime2",
                      type = TIME_PICKER,
                    ),
                  ),
                ),
                value = "Twice a day",
              ),
            ),
            subtext = "If you want to add more than 2 reporting times per day, this must be done as a bespoke condition approved by PPCS.",
            type = RADIO,
          ),
          Input(
            case = LOWER,
            handleIndefiniteArticle = true,
            label = "Select a review period",
            name = "reviewPeriod",
            options = listOf(
              Option(
                value = "Weekly",
              ),
              Option(
                value = "Monthly",
              ),
              Option(
                conditional = Conditional(
                  inputs = listOf(
                    ConditionalInput(
                      case = LOWER,
                      handleIndefiniteArticle = true,
                      label = "Enter a review period",
                      name = "alternativeReviewPeriod",
                      type = TEXT,
                    ),
                  ),
                ),
                value = "Other",
              ),
            ),
            type = RADIO,
          ),
        ),
        requiresInput = true,
        text = "Report to staff at [THE APPROVED PREMISES WHERE YOU RESIDE / NAME OF APPROVED PREMISES] at [TIME / DAILY / OTHER], unless otherwise authorised by your supervising officer. This condition will be reviewed by your supervising officer on a [WEEKLY / MONTHLY / ETC] basis and may be amended or removed if it is felt that the level of risk you present has reduced appropriately.",
        tpl = "Report to staff at {approvedPremises} at {reportingTime}{reportingTime1}{reportingTime2} {alternativeReportingFrequency || reportingFrequency}, unless otherwise authorised by your supervising officer. This condition will be reviewed by your supervising officer on {alternativeReviewPeriod || reviewPeriod} basis and may be amended or removed if it is felt that the level of risk you present has reduced appropriately.",
        type = "ReportToApprovedPremisesPolicyV3",
      ),
      AdditionalConditionAp(
        category = "Supervision in the community by the supervising officer, or other responsible officer, or organisation",
        categoryShort = "Supervision in the community",
        code = "2027ae19-04a2-4fa6-8d1b-a62dffba2e62",
        inputs = listOf(
          Input(
            case = CAPITALISED,
            label = "Enter name of police station",
            name = "policeStation",
            type = TEXT,
          ),
          Input(
            label = "Select when the person needs to report",
            name = "reportingFrequency",
            options = listOf(
              Option(
                value = "Daily",
              ),
              Option(
                value = "Monday to Friday",
              ),
              Option(
                conditional = Conditional(
                  inputs = listOf(
                    ConditionalInput(
                      label = "Enter the other frequency",
                      name = "alternativeReportingFrequency",
                      type = TEXT,
                    ),
                  ),
                ),
                value = "Other",
              ),
            ),
            type = RADIO,
          ),
          Input(
            label = "Select how many times each day they need to report",
            name = "numberOfReportingTimes",
            options = listOf(
              Option(
                conditional = Conditional(
                  inputs = listOf(
                    ConditionalInput(
                      label = "Enter the reporting time",
                      name = "reportingTime",
                      type = TIME_PICKER,
                    ),
                  ),
                ),
                value = "Once a day",
              ),
              Option(
                conditional = Conditional(
                  inputs = listOf(
                    ConditionalInput(
                      label = "Enter the first reporting time",
                      name = "reportingTime1",
                      type = TIME_PICKER,
                    ),
                    ConditionalInput(
                      includeBefore = " and ",
                      label = "Enter the second reporting time",
                      name = "reportingTime2",
                      type = TIME_PICKER,
                    ),
                  ),
                ),
                value = "Twice a day",
              ),
            ),
            subtext = "If you want to add more than 2 reporting times per day, this must be done as a bespoke condition approved by PPCS.",
            type = RADIO,
          ),
          Input(
            case = LOWER,
            handleIndefiniteArticle = true,
            label = "Select a review period",
            name = "reviewPeriod",
            options = listOf(
              Option(
                value = "Weekly",
              ),
              Option(
                value = "Monthly",
              ),
              Option(
                conditional = Conditional(
                  inputs = listOf(
                    ConditionalInput(
                      case = LOWER,
                      handleIndefiniteArticle = true,
                      label = "Enter a review period",
                      name = "alternativeReviewPeriod",
                      type = TEXT,
                    ),
                  ),
                ),
                value = "Other",
              ),
            ),
            type = RADIO,
          ),
        ),
        requiresInput = true,
        text = "Report to staff at [NAME OF POLICE STATION] at [TIME / DAILY], unless otherwise authorised by your supervising officer. This condition will be reviewed by your supervising officer on a [WEEKLY / MONTHLY / ETC] basis and may be amended or removed if it is felt that the level of risk you present has reduced appropriately.",
        tpl = "Report to staff at {policeStation} at {reportingTime}{reportingTime1}{reportingTime2} {alternativeReportingFrequency || reportingFrequency}, unless otherwise authorised by your supervising officer. This condition will be reviewed by your supervising officer on {alternativeReviewPeriod || reviewPeriod} basis and may be amended or removed if it is felt that the level of risk you present has reduced appropriately.",
        type = "ReportToPoliceStation",
      ),
      AdditionalConditionAp(
        category = "Restriction of specified conduct or specified acts",
        categoryShort = "Restriction of conduct or acts",
        code = "7a9ca3bb-922a-433a-9601-1e475c6c0095",
        requiresInput = false,
        text = "Not to participate directly or indirectly in organising and/or contributing to any demonstration, meeting, gathering or website without the prior approval of your supervising officer. This condition will be reviewed on a monthly basis and may be amended or removed if your risk is assessed as having changed.",
      ),
      AdditionalConditionAp(
        category = "Restriction of specified conduct or specified acts",
        categoryShort = "Restriction of conduct or acts",
        code = "985d339a-b652-40e3-b0a8-5aafd5e121f1",
        requiresInput = false,
        text = "Not to partake in gambling, or making payments for other games of chance without the prior permission of your supervising officer.",
      ),
      AdditionalConditionAp(
        category = "Restriction of specified conduct or specified acts",
        categoryShort = "Restriction of conduct or acts",
        code = "3b6b7c4f-6c06-438f-97a4-03c4167484dc",
        inputs = listOf(
          Input(
            label = "Select when to contact the supervising officer",
            name = "contactType",
            options = listOf(
              Option(
                value = "Notify your supervising officer if you",
              ),
              Option(
                value = "Request permission from your supervising officer before you",
              ),
            ),
            type = RADIO,
          ),
          Input(
            label = "Select all that apply",
            listType = "OR",
            name = "contentTypes",
            options = listOf(
              Option(
                value = "social networking",
              ),
              Option(
                value = "video sharing",
              ),
              Option(
                value = "online chat-rooms",
              ),
              Option(
                value = "podcasts",
              ),
            ),
            type = CHECK,
          ),
        ),
        requiresInput = true,
        text = "[NOTIFY YOUR SUPERVISING OFFICER IF YOU / REQUEST PERMISSION FROM YOUR SUPERVISING OFFICER BEFORE YOU] upload, add, modify or stream any material on any site or app related to [SOCIAL NETWORKING / VIDEO SHARING / ONLINE CHAT-ROOMS / PODCASTS].",
        tpl = "{contactType} upload, add, modify or stream any material on any site or app related to {contentTypes}.",
        type = "TypesOfWebsites",
      ),
      AdditionalConditionAp(
        category = "Restriction of specified conduct or specified acts",
        categoryShort = "Restriction of conduct or acts",
        code = "50c8c01e-ec95-45f9-ad48-efa89c4faec0",
        inputs = listOf(
          Input(
            label = "Enter the type of website or app",
            name = "contentType",
            type = TEXT,
          ),
        ),
        requiresInput = true,
        text = "Not to access any site or app related to [TYPE OF WEBSITE/APP] on any devices without the permission of your supervising officer.",
        tpl = "Not to access any site or app related to {contentType} on any devices without the permission of your supervising officer.",
        type = "WebsiteAccess",
      ),

      AdditionalConditionAp(
        category = "Restriction of specified conduct or specified acts",
        categoryShort = "Restriction of conduct or acts",
        code = "762677a4-4593-4bd2-a4bc-55d49b4c4230",
        inputs = listOf(
          Input(
            label = "Select all that apply",
            listType = "OR",
            name = "services",
            options = listOf(
              Option(
                value = "virtual private networks (VPNs)",
              ),
              Option(
                value = "cloud storage",
              ),
              Option(
                value = "virtual desktops",
              ),
              Option(
                value = "automatic deletion of content",
              ),
            ),
            type = CHECK,
          ),
        ),
        requiresInput = true,
        text = "Not to use or install software related to [VIRTUAL PRIVATE NETWORKS (VPNs) / CLOUD STORAGE / VIRTUAL DESKTOPS / AUTOMATIC DELETION OF CONTENT] on any approved devices without the permission of your supervising officer.",
        tpl = "Not to use or install software related to {services} on any approved devices without the permission of your supervising officer.",
        type = "DigitalServices",
      ),
      AdditionalConditionAp(
        category = "Extremism",
        code = "86f8b3d6-be31-48b2-a29e-5cf662c95ad1",
        requiresInput = false,
        text = "Not to contact directly or indirectly any person whom you know or believe to have been charged or convicted of any extremist related offence, without the prior approval of your supervising officer.",
      ),
      AdditionalConditionAp(
        category = "Extremism",
        code = "9785d8f8-31a9-4c32-a06d-eff049ecebcd",
        requiresInput = false,
        text = "Not to attend or organise any meetings or gatherings other than those convened solely for the purposes of worship without the prior approval of your supervising officer.",
      ),
      AdditionalConditionAp(
        category = "Extremism",
        code = "9efba199-87d4-468e-a5a1-1c0945571afa",
        requiresInput = false,
        text = "Not to give or engage in the delivery of any lecture, talk, or sermon whether part of an act of worship or not, without the prior approval of your supervising officer.",
      ),
      AdditionalConditionAp(
        category = "Extremism",
        code = "3d771cc6-b85f-47e4-9e13-75bfb80706f4",
        requiresInput = false,
        text = "Not to engage in any discussion or act to promote grooming or influencing of an individual or a group for the purpose of extremism or radicalisation.",
      ),
      AdditionalConditionAp(
        category = "Extremism",
        code = "e0421c22-1be7-4d06-aba7-3c17822b0c1c",
        requiresInput = false,
        text = "Not to have in your possession any printed or electronically recorded material or handwritten notes which contain encoded information or that promote the destruction of or hatred for any religious or ethnic group or that celebrates, justifies or promotes acts of violence, or that contain information about military or paramilitary technology, weapons, techniques or tactics without the prior approval of your supervising officer.",
      ),
      AdditionalConditionAp(
        category = "Polygraph",
        code = "1dc7ee29-df47-48a8-90b6-69e286692d8a",
        requiresInput = false,
        text = "To comply with any instruction given by your supervising officer requiring you to attend polygraph testing. To participate in polygraph sessions and examinations as instructed by or under the authority of your supervising officer and to comply with any instruction given to you during a polygraph session by the person conducting the polygraph.",
      ),
      AdditionalConditionAp(
        category = "Drug, alcohol and solvent abuse",
        code = "322bb3f7-2ee1-46aa-ae1c-3f743efd4327",
        requiresInput = false,
        text = "Attend a location, as required by your supervising officer, to give a sample of oral fluid / urine in order to test whether you have any specified Class A and specified Class B drugs in your body, for the purpose of ensuring that you are complying with the condition of your licence requiring you to be of good behaviour. Do not take any action that could hamper or frustrate the drug testing process.",
      ),
      AdditionalConditionAp(
        category = "Drug, alcohol and solvent abuse",
        code = "f1d2888b-be86-4732-8874-44cb867865c2",
        inputs = listOf(
          Input(
            label = "Select all that apply",
            listType = "AND",
            name = "substanceTypes",
            options = listOf(
              Option(
                value = "a controlled drug",
              ),
              Option(
                value = "alcohol",
              ),
              Option(
                value = "solvents",
              ),
            ),
            type = CHECK,
          ),
        ),
        requiresInput = true,
        text = "Attend a location, as directed by your supervising officer, to address your dependency on, or propensity to misuse, [A  CONTROLLED DRUG / ALCOHOL / SOLVENTS].",
        tpl = "Attend a location, as directed by your supervising officer, to address your dependency on, or propensity to misuse, {substanceTypes}.",
        type = "SubstanceMisuse",
      ),
      AdditionalConditionAp(
        category = "Electronic monitoring",
        code = "fd129172-bdd3-4d97-a4a0-efd7b47a49d4",
        inputs = listOf(
          Input(
            label = "Select all that apply",
            listType = "AND",
            name = "electronicMonitoringTypes",
            options = listOf(
              Option(
                value = "exclusion zone",
              ),
              Option(
                value = "curfew",
              ),
              Option(
                value = "location monitoring",
              ),
              Option(
                value = "attendance at appointments",
              ),
              Option(
                value = "alcohol monitoring",
              ),
              Option(
                value = "alcohol abstinence",
              ),
            ),
            type = CHECK,
          ),
        ),
        requiresInput = true,
        text = "Allow person(s) as designated by your supervising officer to install an electronic monitoring tag on you and access to install any associated equipment in your property, and for the purpose of ensuring that equipment is functioning correctly. You must not damage or tamper with these devices and ensure that the tag is charged, and report to your supervising officer and the EM provider immediately if the tag or the associated equipment are not working correctly. This will be for the purpose of monitoring your [INSERT TYPES OF CONDITIONS TO BE ELECTRONICALLY MONITORED HERE] licence condition(s) unless otherwise authorised by your supervising officer.",
        tpl = "Allow person(s) as designated by your supervising officer to install an electronic monitoring tag on you and access to install any associated equipment in your property, and for the purpose of ensuring that equipment is functioning correctly. You must not damage or tamper with these devices and ensure that the tag is charged, and report to your supervising officer and the EM provider immediately if the tag or the associated equipment are not working correctly. This will be for the purpose of monitoring your {electronicMonitoringTypes} licence condition(s) unless otherwise authorised by your supervising officer.",
        type = "ElectronicMonitoringTypes",
      ),
      AdditionalConditionAp(
        category = "Electronic monitoring",
        code = "524f2fd6-ad53-47dd-8edc-2161d3dd2ed4",
        inputs = listOf(
          Input(
            label = "Enter the end date",
            name = "endDate",
            type = DATE_PICKER,
          ),
        ),
        requiresInput = true,
        text = "You will be subject to trail monitoring. Your whereabouts will be electronically monitored by GPS Satellite Tagging, ending on [INSERT END DATE], and you must cooperate with the monitoring as directed by your supervising officer unless otherwise authorised by your supervising officer.",
        tpl = "You will be subject to trail monitoring. Your whereabouts will be electronically monitored by GPS Satellite Tagging, ending on {endDate}, and you must cooperate with the monitoring as directed by your supervising officer unless otherwise authorised by your supervising officer.",
        type = "ElectronicMonitoringPeriod",
        skippable = true,
      ),
      AdditionalConditionAp(
        category = "Electronic monitoring",
        code = "86e6f2a9-bb60-40f8-9ac4-310ebc72ac2f",
        inputs = listOf(
          Input(
            label = "Enter the approved address",
            name = "approvedAddress",
            type = ADDRESS,
          ),
        ),
        requiresInput = true,
        text = "You must stay at [APPROVED ADDRESS] between 5pm and midnight every day until your electronic tag is installed unless otherwise authorised by your supervising officer.",
        tpl = "You must stay at {approvedAddress} between 5pm and midnight every day until your electronic tag is installed unless otherwise authorised by your supervising officer.",
        type = "ApprovedAddress",
      ),
      AdditionalConditionAp(
        category = "Electronic monitoring",
        code = "d36a3b77-30ba-40ce-8953-83e761d3b487",
        inputs = listOf(
          Input(
            label = "Enter the end date",
            name = "endDate",
            type = DATE_PICKER,
          ),
        ),
        requiresInput = true,
        text = "You must not drink any alcohol until [END DATE] unless your probation officer says you can. You will need to wear an electronic tag all the time so we can check this.",
        tpl = "You must not drink any alcohol until {endDate} unless your probation officer says you can. You will need to wear an electronic tag all the time so we can check this.",
        type = "AlcoholRestrictionPeriod",
        skippable = true,
      ),
      AdditionalConditionAp(
        category = "Electronic monitoring",
        code = "2F8A5418-C6E4-4F32-9E58-64B23550E504",
        inputs = listOf(
          Input(
            label = "Enter the end date",
            name = "endDate",
            type = DATE_PICKER,
          ),
        ),
        requiresInput = true,
        text = "You will need to wear an electronic tag all the time until [END DATE] so we can check how much alcohol you are drinking, and if you are drinking alcohol when you have been told you must not. To help you drink less alcohol you must take part in any activities, like treatment programmes, your probation officer asks you to.",
        tpl = "You will need to wear an electronic tag all the time until {endDate} so we can check how much alcohol you are drinking, and if you are drinking alcohol when you have been told you must not. To help you drink less alcohol you must take part in any activities, like treatment programmes, your probation officer asks you to.",
        type = "ElectronicTagPeriod",
        skippable = true,
      ),
      AdditionalConditionAp(
        category = "Terrorist personal search",
        code = "9678FD9E-F80D-423A-A6FB-B79909094887",
        requiresInput = false,
        text = "You must let the police search you if they ask. You must also let them search a vehicle you are with, like a car or a motorbike.",
      ),
      AdditionalConditionAp(
        category = "Serious organised crime",
        code = "e8478345-019a-4335-9656-73a77ffd3c42",
        inputs = listOf(
          Input(
            label = "Enter the value in £",
            name = "value",
            type = TEXT,
          ),
        ),
        requiresInput = true,
        text = "Not to have cash in your possession in excess of the value of [VALUE IN £] without the prior approval of your supervising officer.",
        tpl = "Not to have cash in your possession in excess of the value of £{value} without the prior approval of your supervising officer.",
        type = "CashInPossession",
      ),
      AdditionalConditionAp(
        category = "Serious organised crime",
        code = "001328d0-d8bb-48ca-9f99-9c305081d0a2",
        inputs = listOf(
          Input(
            label = "Enter the value in £",
            name = "value",
            type = TEXT,
          ),
        ),
        requiresInput = true,
        text = "Provide your supervising officer with details of any assets, property or possessions worth over [VALUE IN £].",
        tpl = "Provide your supervising officer with details of any assets, property or possessions worth over £{value}.",
        type = "ValueOfAssets",
      ),
      AdditionalConditionAp(
        category = "Serious organised crime",
        code = "a9ef7376-2ab6-4490-a568-a275f4f649ab",
        inputs = listOf(
          Input(
            label = "Select all that apply",
            listType = "AND",
            name = "evidenceOfIncome",
            options = listOf(
              Option(
                value = "payslips",
              ),
              Option(
                value = "bank statements",
              ),
              Option(
                value = "details of all forms of income",
              ),
            ),
            type = CHECK,
          ),
        ),
        requiresInput = true,
        text = "Provide your supervising officer with copies of your [PAYSLIPS / BANK STATEMENTS / DETAILS OF ALL FORMS OF INCOME] upon their request and no less than once a month.",
        tpl = "Provide your supervising officer with copies of your {evidenceOfIncome} upon their request and no less than once a month.",
        type = "EvidenceOfIncome",
      ),
      AdditionalConditionAp(
        category = "Serious organised crime",
        code = "62e035ff-e755-472b-8047-3c5ea1d5b74e",
        requiresInput = false,
        text = "Provide your supervising officer with details of any money transfers which you initiate or receive.",
      ),
      AdditionalConditionAp(
        category = "Serious organised crime",
        code = "4e52fd9c-d436-4f82-9032-7813b130f620",
        requiresInput = false,
        text = "Provide your supervising officer with the details of the full postal addresses of all premises and storage facilities, including business addresses, to which you have a right of access.",
      ),
    ),
    pss = listOf(
      AdditionalConditionPss(
        category = "Drug appointment",
        code = "62c83b80-2223-4562-a195-0670f4072088",
        inputs = listOf(
          Input(
            includeBefore = " at ",
            label = "Enter time (optional)",
            name = "appointmentTime",
            type = TIME_PICKER,
          ),
          Input(
            includeBefore = " on ",
            label = "Enter date (optional)",
            name = "appointmentDate",
            type = DATE_PICKER,
          ),
          Input(
            label = "Enter the address for the appointment",
            name = "appointmentAddress",
            type = ADDRESS,
          ),
        ),
        pssDates = true,
        requiresInput = true,
        text = "Attend [INSERT APPOINTMENT TIME DATE AND ADDRESS], as directed, to address your dependency on, or propensity to misuse, a controlled drug.",
        tpl = "Attend {appointmentAddress}{appointmentDate}{appointmentTime}, as directed, to address your dependency on, or propensity to misuse, a controlled drug.",
        type = "AppointmentTimeAndPlaceDuringPss",
        skippable = true,
      ),
      AdditionalConditionPss(
        category = "Drug testing",
        code = "fda24aa9-a2b0-4d49-9c87-23b0a7be4013",
        inputs = listOf(
          Input(
            label = "Enter name",
            name = "name",
            type = TEXT,
          ),
          Input(
            label = "Enter address",
            name = "address",
            type = ADDRESS,
          ),
        ),
        requiresInput = true,
        text = "Attend [INSERT NAME AND ADDRESS], as reasonably required by your supervisor, to give a sample of oral fluid / urine in order to test whether you have any specified Class A or specified Class B drugs in your body, for the purpose of ensuring that you are complying with the requirement of your supervision period requiring you to be of good behaviour.",
        tpl = "Attend {name} {address}, as reasonably required by your supervisor, to give a sample of oral fluid / urine in order to test whether you have any specified Class A or specified Class B drugs in your body, for the purpose of ensuring that you are complying with the requirement of your supervision period requiring you to be of good behaviour.",
        type = "DrugTestLocation",
      ),
    ),
  ),
  changeHints = listOf(
    ChangeHint(
      previousCode = "599bdcae-d545-461c-b1a9-02cb3d4ba268",
      replacements = listOf(
        "d36a3b77-30ba-40ce-8953-83e761d3b487",
        "2F8A5418-C6E4-4F32-9E58-64B23550E504",
      ),
    ),
    ChangeHint(
      previousCode = "c2435d4a-20a0-47de-b080-e1e740d1514c",
      replacements = listOf(
        "0a370862-5426-49c1-b6d4-3d074d78a81a",
        "fd129172-bdd3-4d97-a4a0-efd7b47a49d4",
      ),
    ),
    ChangeHint(
      previousCode = "a7c57e4e-30fe-4797-9fe7-70a35dbd7b65",
      replacements = listOf(
        "f1d2888b-be86-4732-8874-44cb867865c2",
      ),
    ),
    ChangeHint(
      previousCode = "72d281c3-b194-43ab-812d-fea0683ada65",
      replacements = listOf(
        "3932e5c9-4d21-4251-a747-ce6dc52dc9c0",
      ),
    ),
    ChangeHint(
      previousCode = "ed607a91-fe3a-4816-8eb9-b447c945935c",
      replacements = listOf(
        "3932e5c9-4d21-4251-a747-ce6dc52dc9c0",
      ),
    ),
    ChangeHint(
      previousCode = "680b3b27-43cc-46c6-9ba6-b10d4aba6531",
      replacements = listOf(
        "2d67f68a-8adf-47a9-a68d-a6fc9f2c4556",
      ),
    ),
    ChangeHint(
      previousCode = "bb401b88-2137-4154-be4a-5e05c168638a",
      replacements = emptyList(),
    ),
  ),
  standardConditions = StandardConditions(
    standardConditionsAp = listOf(
      StandardConditionAp(
        code = "9ce9d594-e346-4785-9642-c87e764bee37",
        text = "Be of good behaviour and not behave in a way which undermines the purpose of the licence period.",
      ),
      StandardConditionAp(
        code = "3b19fdb0-4ca3-4615-9fdd-61fabc1587af",
        text = "Not commit any offence.",
      ),
      StandardConditionAp(
        code = "3361683a-504a-4357-ae22-6aa01b370b4a",
        text = "Keep in touch with the supervising officer in accordance with instructions given by the supervising officer.",
      ),
      StandardConditionAp(
        code = "9fc04065-df29-4bda-9b1d-bced8335c356",
        text = "Receive visits from the supervising officer in accordance with any instructions given by the supervising officer.",
      ),
      StandardConditionAp(
        code = "e670ac69-eda2-4b04-a0a1-a3c8492fe1e6",
        text = "Reside permanently at an address approved by the supervising officer and obtain the prior permission of the supervising officer for any stay of one or more nights at a different address.",
      ),
      StandardConditionAp(
        code = "78A5F860-4791-48F2-B707-D6D4413850EE",
        text = "Tell the supervising officer if you use a name which is different to the name or names which appear on your licence.",
      ),
      StandardConditionAp(
        code = "6FA6E492-F0AB-4E76-B868-63813DB44696",
        text = "Tell the supervising officer if you change or add any contact details, including phone number or email.",
      ),
      StandardConditionAp(
        code = "88069445-08cb-4f16-915f-5a162d085c26",
        text = "Not undertake work, or a particular type of work, unless it is approved by the supervising officer and notify the supervising officer in advance of any proposal to undertake work or a particular type of work.",
      ),
      StandardConditionAp(
        code = "7d416906-0e94-4fde-ae86-8339d339ccb7",
        text = "Not travel outside the United Kingdom, the Channel Islands or the Isle of Man except with the prior permission of the supervising officer or for the purposes of immigration deportation or removal.",
      ),
    ),
    standardConditionsPss = listOf(
      StandardConditionPss(
        code = "b3cd4a30-11fd-4715-9ebb-ed89f5386e1f",
        text = "Be of good behaviour and not behave in a way that undermines the rehabilitative purpose of the supervision period.",
      ),
      StandardConditionPss(
        code = "b950407d-2270-45b8-9666-3ad58a17d0be",
        text = "Not commit any offence.",
      ),
      StandardConditionPss(
        code = "93413832-9954-4907-a64d-eb8a56e34afb",
        text = "Keep in touch with your supervisor in accordance with instructions given by your supervisor.",
      ),
      StandardConditionPss(
        code = "9288e01c-e40e-4040-8b6e-57092361f422",
        text = "Receive visits from your supervisor in accordance with instructions given by your supervisor.",
      ),
      StandardConditionPss(
        code = "8e15cf42-f8e0-4408-a33e-d16a3448b7bd",
        text = "Reside permanently at an address approved by your supervisor and obtain the prior permission of the supervisor for any stay of one or more nights at a different address.",
      ),
      StandardConditionPss(
        code = "0ed57797-2745-4592-a78b-8e4d712c580e",
        text = "Not undertake work, or a particular type of work, unless it is approved by your supervisor and notify your supervisor in advance of any proposal to undertake work or a particular type of work.",
      ),
      StandardConditionPss(
        code = "c8966621-088a-4b87-9a19-752ff8b900c6",
        text = "Not travel outside the United Kingdom, the Channel Islands or the Isle of Man except with the prior permission of your supervisor or in order to comply with a legal obligation (whether or not arising under the law of any part of the United Kingdom, the Channel Islands or the Isle of Man).",
      ),
      StandardConditionPss(
        code = "579060fd-e412-471c-94d7-2fefa06d5052",
        text = "Participate in activities in accordance with any instructions given by your supervisor.",
      ),
    ),
  ),
  version = "3.0",
)
