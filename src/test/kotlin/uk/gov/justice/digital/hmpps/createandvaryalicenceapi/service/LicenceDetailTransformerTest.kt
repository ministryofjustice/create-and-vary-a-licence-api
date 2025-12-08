package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.ApConditions
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.Conditions
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.PssConditions
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.additionalConditions.ConditionTypes
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.additionalConditions.ElectronicMonitoringAdditionalConditionWithRestriction
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.additionalConditions.ElectronicMonitoringType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.additionalConditions.MultipleExclusionZoneAdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.additionalConditions.SingleUploadAdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.someModelAdditionalConditions
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.ELECTRONIC_TAG_COND_CODE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.EVENT_EXCLUSION_COND_CODE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.EXCLUSION_ZONE_COND_CODE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.PolicyVersion
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.text.orEmpty
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.BespokeCondition as ModelBespokeCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.StandardCondition as ModelStandardCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.Licence as PublicLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.additionalConditions.GenericAdditionalCondition as PublicModelAdditionalCondition

class LicenceDetailTransformerTest {

  @Test
  fun transformToPublicLicence() {
    val actualLicence = modelLicence.transformToPublicLicence()
    assertThat(actualLicence).isEqualTo(publicLicence)
    assertThat(actualLicence.conditions.apConditions.additional).containsExactlyInAnyOrder(
      PublicModelAdditionalCondition(
        category = "",
        type = ConditionTypes.STANDARD,
        id = 1,
        code = "associateWith",
        text = "Do not associate with value1 for a period of value2",
      ),
      MultipleExclusionZoneAdditionalCondition(
        category = "",
        type = ConditionTypes.MULTIPLE_EXCLUSION_ZONE,
        id = 2,
        code = EXCLUSION_ZONE_COND_CODE,
        text = "Do not enter the area defined in the attached map.",
        hasImageUpload = true,
      ),
      MultipleExclusionZoneAdditionalCondition(
        category = "",
        type = ConditionTypes.MULTIPLE_EXCLUSION_ZONE,
        id = 3,
        code = EXCLUSION_ZONE_COND_CODE,
        text = "Do not enter the area defined in the attached map.",
        hasImageUpload = true,
      ),
      SingleUploadAdditionalCondition(
        category = "",
        type = ConditionTypes.SINGLE_UPLOAD,
        id = 4,
        code = EVENT_EXCLUSION_COND_CODE,
        text = "Do not enter the area defined in the attached map for the duration of The Event.",
        hasImageUpload = true,
      ),
      ElectronicMonitoringAdditionalConditionWithRestriction(
        category = "",
        type = ConditionTypes.ELECTRONIC_MONITORING,
        id = 5,
        code = ELECTRONIC_TAG_COND_CODE,
        text = "You must wear an electronic monitoring tag for curfew purposes.",
        restrictions = listOf(ElectronicMonitoringType.CURFEW),
      ),
    )
  }

  @Test
  fun checkAdditionalConditionsShowExpandedText() {
    val actualLicence = modelLicence.transformToPublicLicence()
    val publicModelCondition = modelLicence.additionalLicenceConditions[0]
    val privateAdditionalCondition = actualLicence.conditions.apConditions.additional[0]

    // Check expanded differs from non-expanded:
    assertThat(publicModelCondition.expandedText).isNotEqualTo(publicModelCondition.text)

    // Check that expanded has been selected:
    assertThat(privateAdditionalCondition.text).isEqualTo(publicModelCondition.expandedText)
  }

  private companion object {
    val someStandardConditions = listOf(
      ModelStandardCondition(
        id = 1,
        code = "goodBehaviour",
        sequence = 1,
        text = "Be of good behaviour",
      ),
      ModelStandardCondition(
        id = 2,
        code = "notBreakLaw",
        sequence = 1,
        text = "Do not break any law",
      ),
      ModelStandardCondition(
        id = 3,
        code = "attendMeetings",
        sequence = 1,
        text = "Attend meetings",
      ),
    )

    val someBespokeConditions = listOf(
      ModelBespokeCondition(
        id = 1,
        sequence = 1,
        text = "Bespoke one text",
      ),
      ModelBespokeCondition(
        id = 2,
        sequence = 2,
        text = "Bespoke two text",
      ),
    )

    val modelLicence = CrdLicence(
      id = 1,
      typeCode = LicenceType.AP,
      version = "2.1",
      statusCode = LicenceStatus.IN_PROGRESS,
      nomsId = "A1234AA",
      bookingNo = "123456",
      bookingId = 987654,
      crn = "A12345",
      pnc = "2019/123445",
      cro = "12345",
      prisonCode = "MDI",
      prisonDescription = "Moorland (HMP)",
      forename = "Person",
      surname = "One",
      approvedByUsername = "TestApprover",
      approvedDate = LocalDateTime.of(2023, 10, 11, 12, 0),
      dateOfBirth = LocalDate.of(1985, 12, 28),
      conditionalReleaseDate = LocalDate.of(2021, 10, 22),
      actualReleaseDate = LocalDate.of(2021, 10, 22),
      sentenceStartDate = LocalDate.of(2018, 10, 22),
      sentenceEndDate = LocalDate.of(2021, 10, 22),
      licenceStartDate = LocalDate.of(2021, 10, 22),
      licenceExpiryDate = LocalDate.of(2021, 10, 22),
      topupSupervisionStartDate = LocalDate.of(2021, 10, 22),
      topupSupervisionExpiryDate = LocalDate.of(2021, 10, 22),
      dateCreated = LocalDateTime.of(2023, 10, 11, 11, 30),
      dateLastUpdated = LocalDateTime.of(2023, 10, 11, 11, 30),

      comUsername = "X12345",
      comStaffId = 12345,
      comEmail = "test.com@probation.gov.uk",
      probationAreaCode = "N01",
      probationAreaDescription = "Wales",
      probationPduCode = "N01A",
      probationPduDescription = "Cardiff",
      probationLauCode = "N01A2",
      probationLauDescription = "Cardiff South",
      probationTeamCode = "NA01A2-A",
      probationTeamDescription = "Cardiff South Team A",
      createdByUsername = "TestCreator",
      standardLicenceConditions = someStandardConditions,
      standardPssConditions = someStandardConditions,
      additionalLicenceConditions = someModelAdditionalConditions(),
      additionalPssConditions = someModelAdditionalConditions(),
      bespokeConditions = someBespokeConditions,
      licenceVersion = "1.4",
      updatedByUsername = "TestUpdater",
    )

    val publicLicenceConditions = Conditions(
      apConditions = ApConditions(
        modelLicence.standardLicenceConditions?.transformToResourceStandard().orEmpty(),
        modelLicence.additionalLicenceConditions.transformToResourceAdditional(),
        modelLicence.bespokeConditions.transformToResourceBespoke(),
      ),
      pssConditions = PssConditions(
        modelLicence.standardPssConditions?.transformToResourceStandard().orEmpty(),
        modelLicence.additionalPssConditions.transformToResourceAdditional(),
      ),
    )
    val publicLicence = PublicLicence(
      id = modelLicence.id,
      kind = modelLicence.kind,
      licenceType = modelLicence.typeCode.mapToPublicLicenceType(),
      policyVersion = PolicyVersion.entries.find { it.version == modelLicence.version }!!,
      version = modelLicence.licenceVersion.orEmpty(),
      statusCode = modelLicence.statusCode!!,
      prisonNumber = modelLicence.nomsId.orEmpty(),
      bookingId = modelLicence.bookingId ?: 0,
      crn = modelLicence.crn.orEmpty(),
      approvedByUsername = modelLicence.approvedByUsername,
      approvedDateTime = modelLicence.approvedDate,
      createdByUsername = modelLicence.createdByUsername.orEmpty(),
      createdDateTime = modelLicence.dateCreated!!,
      updatedByUsername = modelLicence.updatedByUsername,
      updatedDateTime = modelLicence.dateLastUpdated,
      licenceStartDate = modelLicence.licenceStartDate,
      isInPssPeriod = modelLicence.isInPssPeriod ?: false,
      conditions = publicLicenceConditions,
    )
  }
}
