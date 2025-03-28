package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.AddAnother
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.AdditionalConditionAp
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.AdditionalConditionPss
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.AdditionalConditions
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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions.Case.CAPITALISED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions.Case.LOWER

val POLICY_V2_0 = LicencePolicy(
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
              Option("North East"),
              Option("North West"),
              Option("Greater Manchester"),
              Option("Yorkshire and Humberside"),
              Option("East Midlands"),
              Option("West Midlands"),
              Option("East of England"),
              Option("South West"),
              Option("South Central"),
              Option("London"),
              Option("Kent, Surrey and Sussex"),
              Option("Wales"),
            ),
            type = RADIO,
          ),
        ),
        requiresInput = true,
        text = "You must reside within the [INSERT REGION] while of no fixed abode, unless otherwise approved by your supervising officer.",
        tpl = "You must reside within the {probationRegion} probation region while of no fixed abode, unless otherwise approved by your supervising officer.",
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
              Option("any"),
              Option("any female"),
              Option("any male"),
            ),
            type = RADIO,
          ),
          Input(
            label = "Select the relevant age",
            name = "age",
            options = listOf(
              Option("16"),
              Option("18"),
            ),
            type = RADIO,
          ),
        ),
        requiresInput = true,
        text = "Not to reside (not even to stay for one night) in the same household as [ANY / ANY FEMALE / ANY MALE] child under the age of [INSERT AGE] without the prior approval of your supervising officer.",
        tpl = "Not to reside (not even to stay for one night) in the same household as {gender} child under the age of {age} without the prior approval of your supervising officer.",
        type = "RestrictionOfResidency",
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
        text = "Attend all appointments arranged for you with a [PSYCHIATRIST / PSYCHOLOGIST / MEDICAL PRACTITIONER].",
        tpl = "Attend all appointments arranged for you with a {appointmentType}.",
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
        code = "a7c57e4e-30fe-4797-9fe7-70a35dbd7b65",
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
        requiresInput = true,
        text = "Attend [INSERT APPOINTMENT TIME DATE AND ADDRESS], as directed, to address your dependency on, or propensity to misuse, a controlled drug.",
        tpl = "Attend {appointmentAddress}{appointmentDate}{appointmentTime}, as directed, to address your dependency on, or propensity to misuse, a controlled drug.",
        type = "AppointmentTimeAndPlace",
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
              Option("any"),
              Option("any female"),
              Option("any male"),
            ),
            type = RADIO,
          ),
          Input(
            label = "Select the relevant age",
            name = "age",
            options = listOf(
              Option("16 years"),
              Option("18 years"),
            ),
            type = RADIO,
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
        type = "UnsupervisedContact",
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
        category = "Participation in, or co-operation with, a programme or set of activities",
        categoryShort = "Programmes or activities",
        code = "89e656ec-77e8-4832-acc4-6ec05d3e9a98",
        inputs = listOf(
          Input(
            label = "Select all that apply",
            listType = "AND",
            name = "behaviourProblems",
            options = listOf(
              Option("alcohol"),
              Option("drug"),
              Option("sexual"),
              Option("violent"),
              Option("gambling"),
              Option("solvent abuse"),
              Option("anger"),
              Option("debt"),
              Option("prolific offending behaviour"),
              Option("offending behaviour"),
            ),
            type = CHECK,
          ),
          Input(
            includeBefore = " at the ",
            label = "Enter name of course or centre (optional)",
            name = "course",
            type = TEXT,
          ),
        ),
        requiresInput = true,
        text = "To comply with any requirements specified by your supervising officer for the purpose of ensuring that you address your alcohol / drug / sexual / violent / gambling / solvent abuse / anger / debt / prolific / offending behaviour problems at the [NAME OF COURSE / CENTRE].",
        tpl = "To comply with any requirements specified by your supervising officer for the purpose of ensuring that you address your {behaviourProblems} problems{course}.",
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
            options = listOf(
              Option("16 years"),
              Option("18 years"),
            ),
            type = RADIO,
          ),
        ),
        requiresInput = true,
        text = "Not to undertake work or other organised activity which will involve a person under the age of [INSERT AGE], either on a paid or unpaid basis without the prior approval of your supervising officer.",
        tpl = "Not to undertake work or other organised activity which will involve a person under the age of {age}, either on a paid or unpaid basis without the prior approval of your supervising officer.",
        type = "WorkingWithChildren",
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
        code = "72d281c3-b194-43ab-812d-fea0683ada65",
        requiresInput = false,
        text = "Not to own or possess a mobile phone with a photographic function without the prior approval of your supervising officer.",
      ),
      AdditionalConditionAp(
        category = "Possession, ownership, control or inspection of specified items or documents",
        categoryShort = "Items and documents",
        code = "ed607a91-fe3a-4816-8eb9-b447c945935c",
        requiresInput = false,
        text = "Not to own or use a camera without the prior approval of your supervising officer.",
      ),
      AdditionalConditionAp(
        category = "Possession, ownership, control or inspection of specified items or documents",
        categoryShort = "Items and documents",
        code = "680b3b27-43cc-46c6-9ba6-b10d4aba6531",
        requiresInput = false,
        text = "To make any device capable of making or storing digital images (including a camera and a mobile phone with a camera function) available for inspection on request by your supervising officer and/or a police officer.",
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
        requiresInput = false,
        text = "Not to delete the usage history on any internet enabled device or computer used and to allow such items to be inspected as required by the police or your supervising officer. Such inspection may include removal of the device for inspection and the installation of monitoring software.",
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
        inputs = listOf(
          Input(
            case = LOWER,
            label = "Select the relevant text",
            name = "gender",
            options = listOf(
              Option("men"),
              Option("women"),
              Option("women or men"),
            ),
            type = RADIO,
          ),
        ),
        requiresInput = true,
        text = "Notify your supervising officer of any developing intimate relationships with [WOMEN / MEN / WOMEN OR MEN].",
        tpl = "Notify your supervising officer of any developing intimate relationships with {gender}.",
        type = "IntimateRelationshipWithGender",
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
        requiresInput = false,
        text = "Provide your supervising officer with the details of any bank accounts to which you are a signatory and of any credit cards you possess. You must also notify your supervising officer when becoming a signatory to any new bank account or credit card, and provide the account/card details. This condition will be reviewed on a monthly basis and may be amended or removed if it is felt that the level of risk that you present has reduced appropriately.",
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
        category = "Curfew arrangement",
        code = "c2435d4a-20a0-47de-b080-e1e740d1514c",
        inputs = listOf(
          Input(
            label = "Enter the curfew address",
            name = "curfewAddress",
            type = ADDRESS,
          ),
          Input(
            label = "Enter the curfew start time",
            name = "curfewStart",
            type = TIME_PICKER,
          ),
          Input(
            label = "Enter the curfew end time",
            name = "curfewEnd",
            type = TIME_PICKER,
          ),
        ),
        requiresInput = true,
        subtext = "You must have PPCS approval if the curfew time is longer than 12 hours.",
        text = "Confine yourself to remain at [CURFEW ADDRESS] initially from [START OF CURFEW HOURS] until [END OF CURFEW HOURS] each day, and, thereafter, for such a period as may be reasonably notified to you by your supervising officer; and comply with such arrangements as may be reasonably put in place and notified to you by your supervising officer so as to allow for your whereabouts and your compliance with your curfew requirement be monitored (whether by electronic means involving your wearing an electronic tag or otherwise).",
        tpl = "Confine yourself to remain at {curfewAddress} initially from {curfewStart} until {curfewEnd} each day, and, thereafter, for such a period as may be reasonably notified to you by your supervising officer; and comply with such arrangements as may be reasonably put in place and notified to you by your supervising officer so as to allow for your whereabouts and your compliance with your curfew requirement be monitored (whether by electronic means involving your wearing an electronic tag or otherwise).",
        type = "CurfewAddress",
      ),
      AdditionalConditionAp(
        category = "Freedom of movement",
        code = "0f9a20f4-35c7-4c77-8af8-f200f153fa11",
        inputs = listOf(
          Input(
            label = "Enter the name of the area shown on the map",
            name = "outOfBoundArea",
            type = TEXT,
          ),
          Input(
            label = "Select a PDF map of the area this person must not enter",
            name = "outOfBoundFilename",
            type = FILE_UPLOAD,
          ),
        ),
        requiresInput = true,
        text = "Not to enter the area of [CLEARLY SPECIFIED AREA], as defined by the attached map, without the prior approval of your supervising officer.",
        tpl = "Not to enter the area of {outOfBoundArea}, as defined by the attached map, without the prior approval of your supervising officer.",
        type = "OutOfBoundsRegion",
      ),
      AdditionalConditionAp(
        category = "Freedom of movement",
        code = "42f71b40-84cd-446d-8647-f00bbb6c079c",
        inputs = listOf(
          Input(
            label = "Enter name or type of premises",
            name = "nameOfPremises",
            type = TEXT,
          ),
          Input(
            label = "Enter the address of the premises",
            name = "premisesAddress",
            type = ADDRESS,
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
        code = "bb401b88-2137-4154-be4a-5e05c168638a",
        requiresInput = false,
        text = "On release to be escorted by police to Approved Premises.",
      ),
      AdditionalConditionAp(
        category = "Freedom of movement",
        code = "5d0416a9-a4ce-4b2c-8636-0b7abaa3680a",
        requiresInput = false,
        text = "To only attend places of worship which have been previously agreed with your supervising officer.",
      ),
      AdditionalConditionAp(
        category = "Supervision in the community by the supervising officer, or other responsible officer, or organisation",
        categoryShort = "Supervision in the community",
        code = "4673ebe4-9fc0-4e48-87c9-eb17d5280867",
        inputs = listOf(
          Input(
            case = CAPITALISED,
            label = "Enter name of approved premises",
            name = "approvedPremises",
            type = TEXT,
          ),
          Input(
            label = "Enter a reporting time",
            name = "reportingTime",
            type = TIME_PICKER,
          ),
          Input(
            case = LOWER,
            handleIndefiniteArticle = true,
            label = "Select a review period",
            name = "reviewPeriod",
            options = listOf(
              Option("Weekly"),
              Option("Monthly"),
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
        text = "Report to staff at [NAME OF APPROVED PREMISES] at [TIME / DAILY], unless otherwise authorised by your supervising officer. This condition will be reviewed by your supervising officer on a [WEEKLY / MONTHLY / ETC] basis and may be amended or removed if it is felt that the level of risk you present has reduced appropriately.",
        tpl = "Report to staff at {approvedPremises} at {reportingTime}, unless otherwise authorised by your supervising officer. This condition will be reviewed by your supervising officer on {alternativeReviewPeriod || reviewPeriod} basis and may be amended or removed if it is felt that the level of risk you present has reduced appropriately.",
        type = "ReportToApprovedPremisesPolicyV2_0",
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
            label = "Enter a reporting time",
            name = "reportingTime",
            type = TIME_PICKER,
          ),
          Input(
            case = LOWER,
            handleIndefiniteArticle = true,
            label = "Select a review period",
            name = "reviewPeriod",
            options = listOf(
              Option("Weekly"),
              Option("Monthly"),
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
        tpl = "Report to staff at {policeStation} at {reportingTime}, unless otherwise authorised by your supervising officer. This condition will be reviewed by your supervising officer on {alternativeReviewPeriod || reviewPeriod} basis and may be amended or removed if it is felt that the level of risk you present has reduced appropriately.",
        type = "ReportToPoliceStationPolicyV2_0",
      ),
      AdditionalConditionAp(
        category = "Restriction of specified conduct or specified acts",
        categoryShort = "Restriction of conduct or acts",
        code = "7a9ca3bb-922a-433a-9601-1e475c6c0095",
        requiresInput = false,
        text = "Not to participate directly or indirectly in organising and/or contributing to any demonstration, meeting, gathering or website without the prior approval of your supervising officer. This condition will be reviewed on a monthly basis and may be amended or removed if your risk is assessed as having changed.",
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
        category = "Drug testing",
        code = "322bb3f7-2ee1-46aa-ae1c-3f743efd4327",
        inputs = listOf(
          Input(
            label = "Enter name of organisation",
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
        text = "Attend [INSERT NAME AND ADDRESS], as reasonably required by your supervising officer, to give a sample of oral fluid / urine in order to test whether you have any specified Class A and specified Class B drugs in your body, for the purpose of ensuring that you are complying with the condition of your licence requiring you to be of good behaviour. Not to take any action that could hamper or frustrate the drug testing process.",
        tpl = "Attend {name} {address}, as reasonably required by your supervising officer, to give a sample of oral fluid / urine in order to test whether you have any specified Class A and specified Class B drugs in your body, for the purpose of ensuring that you are complying with the condition of your licence requiring you to be of good behaviour. Not to take any action that could hamper or frustrate the drug testing process.",
        type = "DrugTestLocation",
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
              Option("exclusion zone"),
              Option("curfew"),
              Option("location monitoring"),
              Option("attendance at appointments"),
              Option("alcohol monitoring"),
              Option("alcohol abstinence"),
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
        code = "599bdcae-d545-461c-b1a9-02cb3d4ba268",
        inputs = listOf(
          Input(
            label = "Enter the timeframe",
            name = "timeframe",
            type = TEXT,
          ),
          Input(
            label = "Enter the end date",
            name = "endDate",
            type = DATE_PICKER,
          ),
        ),
        requiresInput = true,
        text = "You are subject to alcohol monitoring. Your alcohol intake will be electronically monitored for a period of [INSERT TIMEFRAME] ending on [END DATE], and you may not consume units of alcohol, unless otherwise permitted by your supervising officer.",
        tpl = "You are subject to alcohol monitoring. Your alcohol intake will be electronically monitored for a period of {timeframe} ending on {endDate}, and you may not consume units of alcohol, unless otherwise permitted by your supervising officer.",
        type = "AlcoholMonitoringPeriod",
      ),
      AdditionalConditionAp(
        category = "Terrorist personal search",
        code = "9678FD9E-F80D-423A-A6FB-B79909094887",
        requiresInput = false,
        text = "You must let the police search you if they ask. You must also let them search a vehicle you are with, like a car or a motorbike.",
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
  changeHints = listOf(),
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
  version = "2.0",
)
