package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HardStopLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcVariationLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.PrrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.VariationLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ElectronicMonitoringProvider
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceSummaryApproverView
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Prisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.PrrdLicenceResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions.convertToTitleCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonApiPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.CaseloadResult
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.fullName
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.ElectronicMonitoringProviderStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate
import java.util.Base64
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalCondition as EntityAdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionData as EntityAdditionalConditionData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionUploadSummary as EntityAdditionalConditionUploadSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AuditEvent as EntityAuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.BespokeCondition as EntityBespokeCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.ElectronicMonitoringProvider as EntityElectronicMonitoringProvider
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcCurfewAddress as EntityHdcCurfewAddress
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcCurfewTimes as EntityHdcCurfewTimes
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence as EntityLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.LicenceEvent as EntityLicenceEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.StandardCondition as EntityStandardCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalCondition as ModelAdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalConditionData as ModelAdditionalConditionData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalConditionUploadSummary as ModelAdditionalConditionUploadSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AuditEvent as ModelAuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.BespokeCondition as ModelBespokeCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CrdLicence as ModelCrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.FoundProbationRecord as ModelFoundProbationRecord
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.HardStopLicence as ModelHardstopLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.HdcCurfewAddress as ModelHdcCurfewAddress
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.HdcCurfewTimes as ModelHdcCurfewTimes
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.HdcLicence as ModelHdcLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.HdcVariationLicence as ModelHdcVariationLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceEvent as ModelLicenceEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.StandardCondition as ModelStandardCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.VariationLicence as ModelVariationLicence

/*
** Functions which transform JPA entity objects into their API model equivalents.
** Mostly pass-thru but some translations, so useful to keep the database objects separate from API objects.
*/

fun transformToLicenceSummary(
  licence: Licence,
  hardStopDate: LocalDate?,
  hardStopWarningDate: LocalDate?,
  isInHardStopPeriod: Boolean,
  isDueForEarlyRelease: Boolean,
  isDueToBeReleasedInTheNextTwoWorkingDays: Boolean,
): LicenceSummary = LicenceSummary(
  kind = licence.kind,
  licenceId = licence.id,
  licenceType = licence.typeCode,
  licenceStatus = licence.statusCode,
  nomisId = licence.nomsId!!,
  surname = licence.surname,
  forename = licence.forename,
  crn = licence.crn,
  dateOfBirth = licence.dateOfBirth,
  prisonCode = licence.prisonCode,
  prisonDescription = licence.prisonDescription,
  probationAreaCode = licence.probationAreaCode,
  probationAreaDescription = licence.probationAreaDescription,
  probationPduCode = licence.probationPduCode,
  probationPduDescription = licence.probationPduDescription,
  probationLauCode = licence.probationLauCode,
  probationLauDescription = licence.probationLauDescription,
  probationTeamCode = licence.probationTeamCode,
  probationTeamDescription = licence.probationTeamDescription,
  conditionalReleaseDate = licence.conditionalReleaseDate,
  actualReleaseDate = licence.actualReleaseDate,
  sentenceStartDate = licence.sentenceStartDate,
  sentenceEndDate = licence.sentenceEndDate,
  licenceStartDate = licence.licenceStartDate,
  licenceExpiryDate = licence.licenceExpiryDate,
  topupSupervisionStartDate = licence.topupSupervisionStartDate,
  topupSupervisionExpiryDate = licence.topupSupervisionExpiryDate,
  postRecallReleaseDate = licence.postRecallReleaseDate,
  comUsername = licence.responsibleCom.username,
  bookingId = licence.bookingId,
  dateCreated = licence.dateCreated,
  approvedByName = licence.approvedByName,
  approvedDate = licence.approvedDate,
  submittedDate = licence.submittedDate,
  licenceVersion = licence.licenceVersion,
  versionOf = getVersionOf(licence),
  isReviewNeeded = when (licence) {
    is HardStopLicence -> (licence.statusCode == LicenceStatus.ACTIVE && licence.reviewDate == null)
    else -> false
  },
  hardStopDate = hardStopDate,
  hardStopWarningDate = hardStopWarningDate,
  isInHardStopPeriod = isInHardStopPeriod,
  isDueForEarlyRelease = isDueForEarlyRelease,
  isDueToBeReleasedInTheNextTwoWorkingDays = isDueToBeReleasedInTheNextTwoWorkingDays,
  updatedByFullName = licence.getUpdatedByFullName(),
  homeDetentionCurfewActualDate = if (licence.isHdcLicence()) licence.homeDetentionCurfewActualDate else null,
)

fun toHardstop(
  licence: HardStopLicence,
  earliestReleaseDate: LocalDate?,
  isEligibleForEarlyRelease: Boolean,
  hardStopDate: LocalDate?,
  hardStopWarningDate: LocalDate?,
  isInHardStopPeriod: Boolean,
  isDueForEarlyRelease: Boolean,
  isDueToBeReleasedInTheNextTwoWorkingDays: Boolean,
  conditionSubmissionStatus: Map<String, Boolean>,
) = ModelHardstopLicence(
  id = licence.id,
  typeCode = licence.typeCode,
  version = licence.version,
  statusCode = licence.statusCode,
  nomsId = licence.nomsId,
  bookingNo = licence.bookingNo,
  bookingId = licence.bookingId,
  crn = licence.crn,
  pnc = licence.pnc,
  cro = licence.cro,
  prisonCode = licence.prisonCode,
  prisonDescription = licence.prisonDescription,
  prisonTelephone = licence.prisonTelephone,
  forename = licence.forename,
  middleNames = licence.middleNames,
  surname = licence.surname,
  dateOfBirth = licence.dateOfBirth,
  conditionalReleaseDate = licence.conditionalReleaseDate,
  actualReleaseDate = licence.actualReleaseDate,
  sentenceStartDate = licence.sentenceStartDate,
  sentenceEndDate = licence.sentenceEndDate,
  licenceStartDate = licence.licenceStartDate,
  licenceExpiryDate = licence.licenceExpiryDate,
  topupSupervisionStartDate = licence.topupSupervisionStartDate,
  topupSupervisionExpiryDate = licence.topupSupervisionExpiryDate,
  postRecallReleaseDate = licence.postRecallReleaseDate,
  comUsername = licence.responsibleCom.username,
  comStaffId = licence.responsibleCom.staffIdentifier,
  comEmail = licence.responsibleCom.email,
  responsibleComFullName = with(licence.responsibleCom) { "$firstName $lastName" },
  updatedByFullName = licence.getUpdatedByFullName(),
  probationAreaCode = licence.probationAreaCode,
  probationAreaDescription = licence.probationAreaDescription,
  probationPduCode = licence.probationPduCode,
  probationPduDescription = licence.probationPduDescription,
  probationLauCode = licence.probationLauCode,
  probationLauDescription = licence.probationLauDescription,
  probationTeamCode = licence.probationTeamCode,
  probationTeamDescription = licence.probationTeamDescription,
  appointmentPersonType = licence.appointmentPersonType,
  appointmentPerson = licence.appointmentPerson,
  appointmentTime = licence.appointmentTime,
  appointmentTimeType = licence.appointmentTimeType,
  appointmentAddress = licence.appointmentAddress,
  appointmentContact = licence.appointmentContact,
  reviewDate = licence.reviewDate,
  approvedDate = licence.approvedDate,
  approvedByUsername = licence.approvedByUsername,
  approvedByName = licence.approvedByName,
  submittedDate = licence.submittedDate,
  supersededDate = licence.supersededDate,
  dateCreated = licence.dateCreated,
  createdByUsername = licence.getCreator().username,
  dateLastUpdated = licence.dateLastUpdated,
  updatedByUsername = licence.updatedByUsername,
  standardLicenceConditions = licence.standardConditions.transformToModelStandard("AP"),
  standardPssConditions = licence.standardConditions.transformToModelStandard("PSS"),
  additionalLicenceConditions = licence.additionalConditions.transformToModelAdditional(
    "AP",
    conditionSubmissionStatus,
  ),
  additionalPssConditions = licence.additionalConditions.transformToModelAdditional("PSS", conditionSubmissionStatus),
  bespokeConditions = licence.bespokeConditions.transformToModelBespoke(),
  createdByFullName = with(licence.getCreator()) { "$firstName $lastName" },
  isInPssPeriod = if (licence.typeCode === LicenceType.PSS) true else licence.isInPssPeriod(),
  isActivatedInPssPeriod = licence.isActivatedInPssPeriod(),
  licenceVersion = licence.licenceVersion,
  earliestReleaseDate = earliestReleaseDate,
  isEligibleForEarlyRelease = isEligibleForEarlyRelease,
  isInHardStopPeriod = isInHardStopPeriod,
  isDueForEarlyRelease = isDueForEarlyRelease,
  isDueToBeReleasedInTheNextTwoWorkingDays = isDueToBeReleasedInTheNextTwoWorkingDays,
  hardStopDate = hardStopDate,
  hardStopWarningDate = hardStopWarningDate,
  submittedByFullName = licence.getSubmittedByFullName(),
)

fun toVariation(
  licence: VariationLicence,
  earliestReleaseDate: LocalDate?,
  isEligibleForEarlyRelease: Boolean,
  conditionSubmissionStatus: Map<String, Boolean>,
): ModelVariationLicence = ModelVariationLicence(
  id = licence.id,
  typeCode = licence.typeCode,
  version = licence.version,
  statusCode = licence.statusCode,
  nomsId = licence.nomsId,
  bookingNo = licence.bookingNo,
  bookingId = licence.bookingId,
  crn = licence.crn,
  pnc = licence.pnc,
  cro = licence.cro,
  prisonCode = licence.prisonCode,
  prisonDescription = licence.prisonDescription,
  prisonTelephone = licence.prisonTelephone,
  forename = licence.forename,
  middleNames = licence.middleNames,
  surname = licence.surname,
  dateOfBirth = licence.dateOfBirth,
  conditionalReleaseDate = licence.conditionalReleaseDate,
  actualReleaseDate = licence.actualReleaseDate,
  sentenceStartDate = licence.sentenceStartDate,
  sentenceEndDate = licence.sentenceEndDate,
  licenceStartDate = licence.licenceStartDate,
  licenceExpiryDate = licence.licenceExpiryDate,
  topupSupervisionStartDate = licence.topupSupervisionStartDate,
  topupSupervisionExpiryDate = licence.topupSupervisionExpiryDate,
  postRecallReleaseDate = licence.postRecallReleaseDate,
  comUsername = licence.responsibleCom.username,
  comStaffId = licence.responsibleCom.staffIdentifier,
  comEmail = licence.responsibleCom.email,
  responsibleComFullName = with(licence.responsibleCom) { "$firstName $lastName" },
  updatedByFullName = licence.getUpdatedByFullName(),
  probationAreaCode = licence.probationAreaCode,
  probationAreaDescription = licence.probationAreaDescription,
  probationPduCode = licence.probationPduCode,
  probationPduDescription = licence.probationPduDescription,
  probationLauCode = licence.probationLauCode,
  probationLauDescription = licence.probationLauDescription,
  probationTeamCode = licence.probationTeamCode,
  probationTeamDescription = licence.probationTeamDescription,
  appointmentPersonType = licence.appointmentPersonType,
  appointmentPerson = licence.appointmentPerson,
  appointmentTime = licence.appointmentTime,
  appointmentTimeType = licence.appointmentTimeType,
  appointmentAddress = licence.appointmentAddress,
  appointmentContact = licence.appointmentContact,
  spoDiscussion = licence.spoDiscussion,
  vloDiscussion = licence.vloDiscussion,
  approvedDate = licence.approvedDate,
  approvedByUsername = licence.approvedByUsername,
  approvedByName = licence.approvedByName,
  submittedDate = licence.submittedDate,
  supersededDate = licence.supersededDate,
  dateCreated = licence.dateCreated,
  createdByUsername = licence.getCreator().username,
  dateLastUpdated = licence.dateLastUpdated,
  updatedByUsername = licence.updatedByUsername,
  standardLicenceConditions = licence.standardConditions.transformToModelStandard("AP"),
  standardPssConditions = licence.standardConditions.transformToModelStandard("PSS"),
  additionalLicenceConditions = licence.additionalConditions.transformToModelAdditional(
    "AP",
    conditionSubmissionStatus,
  ),
  additionalPssConditions = licence.additionalConditions.transformToModelAdditional("PSS", conditionSubmissionStatus),
  bespokeConditions = licence.bespokeConditions.transformToModelBespoke(),
  variationOf = licence.variationOfId,
  createdByFullName = with(licence.getCreator()) { "$firstName $lastName" },
  isInPssPeriod = if (licence.typeCode === LicenceType.PSS) true else licence.isInPssPeriod(),
  isActivatedInPssPeriod = licence.isActivatedInPssPeriod(),
  licenceVersion = licence.licenceVersion,
  earliestReleaseDate = earliestReleaseDate,
  isEligibleForEarlyRelease = isEligibleForEarlyRelease,
  submittedByFullName = licence.getSubmittedByFullName(),
)

fun toPrrd(
  licence: PrrdLicence,
  earliestReleaseDate: LocalDate?,
  isEligibleForEarlyRelease: Boolean,
  hardStopDate: LocalDate?,
  hardStopWarningDate: LocalDate?,
  isInHardStopPeriod: Boolean,
  isDueForEarlyRelease: Boolean,
  isDueToBeReleasedInTheNextTwoWorkingDays: Boolean,
  conditionSubmissionStatus: Map<String, Boolean>,
) = PrrdLicenceResponse(
  id = licence.id,
  typeCode = licence.typeCode,
  version = licence.version,
  statusCode = licence.statusCode,
  nomsId = licence.nomsId,
  bookingNo = licence.bookingNo,
  bookingId = licence.bookingId,
  crn = licence.crn,
  pnc = licence.pnc,
  cro = licence.cro,
  prisonCode = licence.prisonCode,
  prisonDescription = licence.prisonDescription,
  prisonTelephone = licence.prisonTelephone,
  forename = licence.forename,
  middleNames = licence.middleNames,
  surname = licence.surname,
  dateOfBirth = licence.dateOfBirth,
  conditionalReleaseDate = licence.conditionalReleaseDate,
  actualReleaseDate = licence.actualReleaseDate,
  sentenceStartDate = licence.sentenceStartDate,
  sentenceEndDate = licence.sentenceEndDate,
  licenceStartDate = licence.licenceStartDate,
  licenceExpiryDate = licence.licenceExpiryDate,
  topupSupervisionStartDate = licence.topupSupervisionStartDate,
  topupSupervisionExpiryDate = licence.topupSupervisionExpiryDate,
  postRecallReleaseDate = licence.postRecallReleaseDate!!,
  comUsername = licence.responsibleCom.username,
  comStaffId = licence.responsibleCom.staffIdentifier,
  comEmail = licence.responsibleCom.email,
  responsibleComFullName = with(licence.responsibleCom) { "$firstName $lastName" },
  updatedByFullName = licence.getUpdatedByFullName(),
  probationAreaCode = licence.probationAreaCode,
  probationAreaDescription = licence.probationAreaDescription,
  probationPduCode = licence.probationPduCode,
  probationPduDescription = licence.probationPduDescription,
  probationLauCode = licence.probationLauCode,
  probationLauDescription = licence.probationLauDescription,
  probationTeamCode = licence.probationTeamCode,
  probationTeamDescription = licence.probationTeamDescription,
  appointmentPersonType = licence.appointmentPersonType,
  appointmentPerson = licence.appointmentPerson,
  appointmentTime = licence.appointmentTime,
  appointmentTimeType = licence.appointmentTimeType,
  appointmentAddress = licence.appointmentAddress,
  appointmentContact = licence.appointmentContact,
  approvedDate = licence.approvedDate,
  approvedByUsername = licence.approvedByUsername,
  approvedByName = licence.approvedByName,
  submittedDate = licence.submittedDate,
  supersededDate = licence.supersededDate,
  dateCreated = licence.dateCreated,
  createdByUsername = licence.getCreator().username,
  dateLastUpdated = licence.dateLastUpdated,
  updatedByUsername = licence.updatedByUsername,
  standardLicenceConditions = licence.standardConditions.transformToModelStandard("AP"),
  standardPssConditions = licence.standardConditions.transformToModelStandard("PSS"),
  additionalLicenceConditions = licence.additionalConditions.transformToModelAdditional(
    "AP",
    conditionSubmissionStatus,
  ),
  additionalPssConditions = licence.additionalConditions.transformToModelAdditional("PSS", conditionSubmissionStatus),
  bespokeConditions = licence.bespokeConditions.transformToModelBespoke(),
  createdByFullName = with(licence.getCreator()) { "$firstName $lastName" },
  isInPssPeriod = if (licence.typeCode === LicenceType.PSS) true else licence.isInPssPeriod(),
  isActivatedInPssPeriod = licence.isActivatedInPssPeriod(),
  licenceVersion = licence.licenceVersion,
  earliestReleaseDate = earliestReleaseDate,
  isEligibleForEarlyRelease = isEligibleForEarlyRelease,
  hardStopDate = hardStopDate,
  hardStopWarningDate = hardStopWarningDate,
  isInHardStopPeriod = isInHardStopPeriod,
  isDueForEarlyRelease = isDueForEarlyRelease,
  isDueToBeReleasedInTheNextTwoWorkingDays = isDueToBeReleasedInTheNextTwoWorkingDays,
  submittedByFullName = licence.getSubmittedByFullName(),
  electronicMonitoringProvider = licence.electronicMonitoringProvider?.let { transformToModelElectronicMonitoringProvider(it) },
  electronicMonitoringProviderStatus = determineElectronicMonitoringProviderStatus(licence.electronicMonitoringProvider),
)

fun toCrd(
  licence: CrdLicence,
  earliestReleaseDate: LocalDate?,
  isEligibleForEarlyRelease: Boolean,
  hardStopDate: LocalDate?,
  hardStopWarningDate: LocalDate?,
  isInHardStopPeriod: Boolean,
  isDueForEarlyRelease: Boolean,
  isDueToBeReleasedInTheNextTwoWorkingDays: Boolean,
  conditionSubmissionStatus: Map<String, Boolean>,
) = ModelCrdLicence(
  id = licence.id,
  typeCode = licence.typeCode,
  version = licence.version,
  statusCode = licence.statusCode,
  nomsId = licence.nomsId,
  bookingNo = licence.bookingNo,
  bookingId = licence.bookingId,
  crn = licence.crn,
  pnc = licence.pnc,
  cro = licence.cro,
  prisonCode = licence.prisonCode,
  prisonDescription = licence.prisonDescription,
  prisonTelephone = licence.prisonTelephone,
  forename = licence.forename,
  middleNames = licence.middleNames,
  surname = licence.surname,
  dateOfBirth = licence.dateOfBirth,
  conditionalReleaseDate = licence.conditionalReleaseDate,
  actualReleaseDate = licence.actualReleaseDate,
  sentenceStartDate = licence.sentenceStartDate,
  sentenceEndDate = licence.sentenceEndDate,
  licenceStartDate = licence.licenceStartDate,
  licenceExpiryDate = licence.licenceExpiryDate,
  topupSupervisionStartDate = licence.topupSupervisionStartDate,
  topupSupervisionExpiryDate = licence.topupSupervisionExpiryDate,
  postRecallReleaseDate = licence.postRecallReleaseDate,
  comUsername = licence.responsibleCom.username,
  comStaffId = licence.responsibleCom.staffIdentifier,
  comEmail = licence.responsibleCom.email,
  responsibleComFullName = with(licence.responsibleCom) { "$firstName $lastName" },
  updatedByFullName = licence.getUpdatedByFullName(),
  probationAreaCode = licence.probationAreaCode,
  probationAreaDescription = licence.probationAreaDescription,
  probationPduCode = licence.probationPduCode,
  probationPduDescription = licence.probationPduDescription,
  probationLauCode = licence.probationLauCode,
  probationLauDescription = licence.probationLauDescription,
  probationTeamCode = licence.probationTeamCode,
  probationTeamDescription = licence.probationTeamDescription,
  appointmentPersonType = licence.appointmentPersonType,
  appointmentPerson = licence.appointmentPerson,
  appointmentTime = licence.appointmentTime,
  appointmentTimeType = licence.appointmentTimeType,
  appointmentAddress = licence.appointmentAddress,
  appointmentContact = licence.appointmentContact,
  approvedDate = licence.approvedDate,
  approvedByUsername = licence.approvedByUsername,
  approvedByName = licence.approvedByName,
  submittedDate = licence.submittedDate,
  supersededDate = licence.supersededDate,
  dateCreated = licence.dateCreated,
  createdByUsername = licence.getCreator().username,
  dateLastUpdated = licence.dateLastUpdated,
  updatedByUsername = licence.updatedByUsername,
  standardLicenceConditions = licence.standardConditions.transformToModelStandard("AP"),
  standardPssConditions = licence.standardConditions.transformToModelStandard("PSS"),
  additionalLicenceConditions = licence.additionalConditions.transformToModelAdditional(
    "AP",
    conditionSubmissionStatus,
  ),
  additionalPssConditions = licence.additionalConditions.transformToModelAdditional("PSS", conditionSubmissionStatus),
  bespokeConditions = licence.bespokeConditions.transformToModelBespoke(),
  createdByFullName = with(licence.getCreator()) { "$firstName $lastName" },
  isInPssPeriod = if (licence.typeCode === LicenceType.PSS) true else licence.isInPssPeriod(),
  isActivatedInPssPeriod = licence.isActivatedInPssPeriod(),
  licenceVersion = licence.licenceVersion,
  earliestReleaseDate = earliestReleaseDate,
  isEligibleForEarlyRelease = isEligibleForEarlyRelease,
  hardStopDate = hardStopDate,
  hardStopWarningDate = hardStopWarningDate,
  isInHardStopPeriod = isInHardStopPeriod,
  isDueForEarlyRelease = isDueForEarlyRelease,
  isDueToBeReleasedInTheNextTwoWorkingDays = isDueToBeReleasedInTheNextTwoWorkingDays,
  submittedByFullName = licence.getSubmittedByFullName(),
  electronicMonitoringProvider = licence.electronicMonitoringProvider?.let { transformToModelElectronicMonitoringProvider(it) },
  electronicMonitoringProviderStatus = determineElectronicMonitoringProviderStatus(licence.electronicMonitoringProvider),
)

fun toHdc(
  licence: HdcLicence,
  earliestReleaseDate: LocalDate?,
  isEligibleForEarlyRelease: Boolean,
  hardStopDate: LocalDate?,
  hardStopWarningDate: LocalDate?,
  isInHardStopPeriod: Boolean,
  isDueForEarlyRelease: Boolean,
  isDueToBeReleasedInTheNextTwoWorkingDays: Boolean,
  conditionSubmissionStatus: Map<String, Boolean>,
) = ModelHdcLicence(
  id = licence.id,
  typeCode = licence.typeCode,
  version = licence.version,
  statusCode = licence.statusCode,
  nomsId = licence.nomsId,
  bookingNo = licence.bookingNo,
  bookingId = licence.bookingId,
  crn = licence.crn,
  pnc = licence.pnc,
  cro = licence.cro,
  prisonCode = licence.prisonCode,
  prisonDescription = licence.prisonDescription,
  prisonTelephone = licence.prisonTelephone,
  forename = licence.forename,
  middleNames = licence.middleNames,
  surname = licence.surname,
  dateOfBirth = licence.dateOfBirth,
  conditionalReleaseDate = licence.conditionalReleaseDate,
  actualReleaseDate = licence.actualReleaseDate,
  sentenceStartDate = licence.sentenceStartDate,
  sentenceEndDate = licence.sentenceEndDate,
  licenceStartDate = licence.licenceStartDate,
  licenceExpiryDate = licence.licenceExpiryDate,
  homeDetentionCurfewActualDate = licence.homeDetentionCurfewActualDate,
  homeDetentionCurfewEndDate = licence.homeDetentionCurfewEndDate,
  topupSupervisionStartDate = licence.topupSupervisionStartDate,
  topupSupervisionExpiryDate = licence.topupSupervisionExpiryDate,
  postRecallReleaseDate = licence.postRecallReleaseDate,
  comUsername = licence.responsibleCom.username,
  comStaffId = licence.responsibleCom.staffIdentifier,
  comEmail = licence.responsibleCom.email,
  responsibleComFullName = with(licence.responsibleCom) { "$firstName $lastName" },
  updatedByFullName = licence.getUpdatedByFullName(),
  probationAreaCode = licence.probationAreaCode,
  probationAreaDescription = licence.probationAreaDescription,
  probationPduCode = licence.probationPduCode,
  probationPduDescription = licence.probationPduDescription,
  probationLauCode = licence.probationLauCode,
  probationLauDescription = licence.probationLauDescription,
  probationTeamCode = licence.probationTeamCode,
  probationTeamDescription = licence.probationTeamDescription,
  appointmentPersonType = licence.appointmentPersonType,
  appointmentPerson = licence.appointmentPerson,
  appointmentTime = licence.appointmentTime,
  appointmentTimeType = licence.appointmentTimeType,
  appointmentAddress = licence.appointmentAddress,
  appointmentContact = licence.appointmentContact,
  approvedDate = licence.approvedDate,
  approvedByUsername = licence.approvedByUsername,
  approvedByName = licence.approvedByName,
  submittedDate = licence.submittedDate,
  supersededDate = licence.supersededDate,
  dateCreated = licence.dateCreated,
  createdByUsername = licence.getCreator().username,
  dateLastUpdated = licence.dateLastUpdated,
  updatedByUsername = licence.updatedByUsername,
  standardLicenceConditions = licence.standardConditions.transformToModelStandard("AP"),
  standardPssConditions = licence.standardConditions.transformToModelStandard("PSS"),
  additionalLicenceConditions = licence.additionalConditions.transformToModelAdditional(
    "AP",
    conditionSubmissionStatus,
  ),
  additionalPssConditions = licence.additionalConditions.transformToModelAdditional("PSS", conditionSubmissionStatus),
  bespokeConditions = licence.bespokeConditions.transformToModelBespoke(),
  createdByFullName = with(licence.getCreator()) { "$firstName $lastName" },
  isInPssPeriod = if (licence.typeCode === LicenceType.PSS) true else licence.isInPssPeriod(),
  isActivatedInPssPeriod = licence.isActivatedInPssPeriod(),
  licenceVersion = licence.licenceVersion,
  earliestReleaseDate = earliestReleaseDate,
  isEligibleForEarlyRelease = isEligibleForEarlyRelease,
  hardStopDate = hardStopDate,
  hardStopWarningDate = hardStopWarningDate,
  isInHardStopPeriod = isInHardStopPeriod,
  isDueForEarlyRelease = isDueForEarlyRelease,
  isDueToBeReleasedInTheNextTwoWorkingDays = isDueToBeReleasedInTheNextTwoWorkingDays,
  submittedByFullName = licence.getSubmittedByFullName(),
  curfewTimes = licence.curfewTimes.transformToModelCurfewTimes(),
  curfewAddress = licence.curfewAddress?.let { transformToModelHdcCurfewAddress(it) },
  electronicMonitoringProvider = licence.electronicMonitoringProvider?.let { transformToModelElectronicMonitoringProvider(it) },
  electronicMonitoringProviderStatus = determineElectronicMonitoringProviderStatus(licence.electronicMonitoringProvider),
)

fun toHdcVariation(
  licence: HdcVariationLicence,
  earliestReleaseDate: LocalDate?,
  isEligibleForEarlyRelease: Boolean,
  conditionSubmissionStatus: Map<String, Boolean>,
) = ModelHdcVariationLicence(
  id = licence.id,
  typeCode = licence.typeCode,
  version = licence.version,
  statusCode = licence.statusCode,
  nomsId = licence.nomsId,
  bookingNo = licence.bookingNo,
  bookingId = licence.bookingId,
  crn = licence.crn,
  pnc = licence.pnc,
  cro = licence.cro,
  prisonCode = licence.prisonCode,
  prisonDescription = licence.prisonDescription,
  prisonTelephone = licence.prisonTelephone,
  forename = licence.forename,
  middleNames = licence.middleNames,
  surname = licence.surname,
  dateOfBirth = licence.dateOfBirth,
  conditionalReleaseDate = licence.conditionalReleaseDate,
  actualReleaseDate = licence.actualReleaseDate,
  sentenceStartDate = licence.sentenceStartDate,
  sentenceEndDate = licence.sentenceEndDate,
  licenceStartDate = licence.licenceStartDate,
  licenceExpiryDate = licence.licenceExpiryDate,
  homeDetentionCurfewActualDate = licence.homeDetentionCurfewActualDate,
  homeDetentionCurfewEndDate = licence.homeDetentionCurfewEndDate,
  topupSupervisionStartDate = licence.topupSupervisionStartDate,
  topupSupervisionExpiryDate = licence.topupSupervisionExpiryDate,
  postRecallReleaseDate = licence.postRecallReleaseDate,
  comUsername = licence.responsibleCom.username,
  comStaffId = licence.responsibleCom.staffIdentifier,
  comEmail = licence.responsibleCom.email,
  responsibleComFullName = with(licence.responsibleCom) { "$firstName $lastName" },
  updatedByFullName = licence.getUpdatedByFullName(),
  probationAreaCode = licence.probationAreaCode,
  probationAreaDescription = licence.probationAreaDescription,
  probationPduCode = licence.probationPduCode,
  probationPduDescription = licence.probationPduDescription,
  probationLauCode = licence.probationLauCode,
  probationLauDescription = licence.probationLauDescription,
  probationTeamCode = licence.probationTeamCode,
  probationTeamDescription = licence.probationTeamDescription,
  appointmentPerson = licence.appointmentPerson,
  appointmentPersonType = licence.appointmentPersonType,
  appointmentTime = licence.appointmentTime,
  appointmentTimeType = licence.appointmentTimeType,
  appointmentAddress = licence.appointmentAddress,
  appointmentContact = licence.appointmentContact,
  spoDiscussion = licence.spoDiscussion,
  vloDiscussion = licence.vloDiscussion,
  approvedDate = licence.approvedDate,
  approvedByUsername = licence.approvedByUsername,
  submittedDate = licence.submittedDate,
  approvedByName = licence.approvedByName,
  supersededDate = licence.supersededDate,
  dateCreated = licence.dateCreated,
  createdByUsername = licence.getCreator().username,
  dateLastUpdated = licence.dateLastUpdated,
  updatedByUsername = licence.updatedByUsername,
  standardLicenceConditions = licence.standardConditions.transformToModelStandard("AP"),
  standardPssConditions = licence.standardConditions.transformToModelStandard("PSS"),
  additionalLicenceConditions = licence.additionalConditions.transformToModelAdditional(
    "AP",
    conditionSubmissionStatus,
  ),
  additionalPssConditions = licence.additionalConditions.transformToModelAdditional("PSS", conditionSubmissionStatus),
  bespokeConditions = licence.bespokeConditions.transformToModelBespoke(),
  variationOf = licence.variationOfId,
  createdByFullName = with(licence.getCreator()) { "$firstName $lastName" },
  isInPssPeriod = licence.isInPssPeriod(),
  isActivatedInPssPeriod = licence.isActivatedInPssPeriod(),
  licenceVersion = licence.licenceVersion,
  earliestReleaseDate = earliestReleaseDate,
  isEligibleForEarlyRelease = isEligibleForEarlyRelease,
  isReviewNeeded = false,
  submittedByFullName = licence.getSubmittedByFullName(),
  curfewTimes = licence.curfewTimes.transformToModelCurfewTimes(),
  curfewAddress = licence.curfewAddress?.let { transformToModelHdcCurfewAddress(it) },
)

// Transform a list of entity standard conditions to model standard conditions
fun List<EntityStandardCondition>.transformToModelStandard(conditionType: String): List<ModelStandardCondition> = filter { condition -> condition.conditionType == conditionType }.map(::transform)

fun transform(entity: EntityStandardCondition): ModelStandardCondition = ModelStandardCondition(
  id = entity.id,
  code = entity.conditionCode,
  sequence = entity.conditionSequence,
  text = entity.conditionText,
)

// Transform a list of entity additional conditions to model additional conditions
fun List<EntityAdditionalCondition>.transformToModelAdditional(
  conditionType: String,
  conditionSubmissionStatus: Map<String, Boolean>,
): List<ModelAdditionalCondition> = filter { condition -> condition.conditionType == conditionType }.map {
  transform(
    it,
    conditionSubmissionStatus[it.conditionCode]!!,
  )
}

fun transform(entity: EntityAdditionalCondition, readyToSubmit: Boolean): ModelAdditionalCondition = ModelAdditionalCondition(
  id = entity.id,
  code = entity.conditionCode,
  version = entity.conditionVersion,
  category = entity.conditionCategory,
  sequence = entity.conditionSequence,
  text = entity.conditionText,
  expandedText = entity.expandedConditionText,
  data = entity.additionalConditionData.transformToModelAdditionalData(),
  uploadSummary = entity.additionalConditionUploadSummary.transformToModelAdditionalConditionUploadSummary(),
  readyToSubmit = readyToSubmit,
)

// Transform a list of entity additional condition data to model additional condition data
fun List<EntityAdditionalConditionData>.transformToModelAdditionalData(): List<ModelAdditionalConditionData> = map(::transform)

fun transform(entity: EntityAdditionalConditionData): ModelAdditionalConditionData = ModelAdditionalConditionData(
  id = entity.id,
  sequence = entity.dataSequence,
  field = entity.dataField,
  value = entity.dataValue,
)

// Transform a list of entity bespoke conditions to model bespoke conditions
fun List<EntityBespokeCondition>.transformToModelBespoke(): List<ModelBespokeCondition> = map(::transform)

fun transform(entity: EntityBespokeCondition): ModelBespokeCondition = ModelBespokeCondition(
  id = entity.id,
  sequence = entity.conditionSequence,
  text = entity.conditionText,
)

// Transform a list of entity additional condition uploads to model additional condition uploads
fun List<EntityAdditionalConditionUploadSummary>.transformToModelAdditionalConditionUploadSummary(): List<ModelAdditionalConditionUploadSummary> = map(::transform)

fun transform(entity: EntityAdditionalConditionUploadSummary): ModelAdditionalConditionUploadSummary = ModelAdditionalConditionUploadSummary(
  id = entity.id,
  filename = entity.filename,
  fileType = entity.fileType,
  fileSize = entity.fileSize,
  uploadedTime = entity.uploadedTime,
  description = entity.description,
  thumbnailImage = entity.thumbnailImage?.toBase64(),
  uploadDetailId = entity.uploadDetailId,
)

// Transform a list of entity hdc curfew times to model hdc curfew times
fun List<EntityHdcCurfewTimes>.transformToModelCurfewTimes(): List<ModelHdcCurfewTimes> = map(::transform)

private fun transform(entity: EntityHdcCurfewTimes): ModelHdcCurfewTimes = ModelHdcCurfewTimes(
  id = entity.id,
  curfewTimesSequence = entity.curfewTimesSequence,
  fromDay = entity.fromDay,
  fromTime = entity.fromTime,
  untilDay = entity.untilDay,
  untilTime = entity.untilTime,
)

fun ByteArray.toBase64(): String = String(Base64.getEncoder().encode(this))

fun List<EntityAuditEvent>.transformToModelAuditEvents(): List<ModelAuditEvent> = map(::transform)

private fun transform(entity: EntityAuditEvent): ModelAuditEvent = ModelAuditEvent(
  id = entity.id,
  licenceId = entity.licenceId,
  eventTime = entity.eventTime,
  username = entity.username,
  fullName = entity.fullName,
  eventType = entity.eventType,
  summary = entity.summary,
  detail = entity.detail,
  changes = entity.changes,
)

fun List<EntityLicenceEvent>.transformToModelEvents(): List<ModelLicenceEvent> = map(::transform)

fun transform(entity: EntityLicenceEvent): ModelLicenceEvent = ModelLicenceEvent(
  id = entity.id,
  licenceId = entity.licenceId,
  eventType = entity.eventType,
  username = entity.username,
  forenames = entity.forenames,
  surname = entity.surname,
  eventDescription = entity.eventDescription,
  eventTime = entity.eventTime,
)

fun CaseloadResult.transformToModelFoundProbationRecord(
  licence: Licence,
  hardStopDate: LocalDate?,
  hardStopWarningDate: LocalDate?,
  isInHardStopPeriod: Boolean,
  isDueForEarlyRelease: Boolean,
  isDueToBeReleasedInTheNextTwoWorkingDays: Boolean,
): ModelFoundProbationRecord {
  val hdcad = if (licence.isHdcLicence()) licence.homeDetentionCurfewActualDate else null
  return ModelFoundProbationRecord(
    kind = licence.kind,
    bookingId = licence.bookingId,
    name = "${name.forename} ${name.surname}".convertToTitleCase(),
    crn = licence.crn,
    nomisId = licence.nomsId,
    comName = staff.name?.fullName()?.convertToTitleCase(),
    comStaffCode = staff.code,
    teamName = team.description,
    releaseDate = licence.licenceStartDate,
    licenceId = licence.id,
    versionOf = getVersionOf(licence),
    licenceType = licence.typeCode,
    licenceStatus = licence.statusCode,
    isOnProbation = licence.statusCode.isOnProbation(),
    hardStopDate = hardStopDate,
    hardStopWarningDate = hardStopWarningDate,
    isInHardStopPeriod = isInHardStopPeriod,
    isDueForEarlyRelease = isDueForEarlyRelease,
    isDueToBeReleasedInTheNextTwoWorkingDays = isDueToBeReleasedInTheNextTwoWorkingDays,
    releaseDateLabel = when (licence.licenceStartDate) {
      null -> "CRD"
      licence.actualReleaseDate -> "Confirmed release date"
      hdcad -> "HDCAD"
      else -> "CRD"
    },
    isReviewNeeded = when (licence) {
      is HardStopLicence -> (licence.statusCode == LicenceStatus.ACTIVE && licence.reviewDate == null)
      else -> false
    },
  )
}

fun CaseloadResult.transformToUnstartedRecord(
  bookingId: Long?,
  releaseDate: LocalDate?,
  licenceType: LicenceType?,
  licenceStatus: LicenceStatus?,
  hardStopDate: LocalDate?,
  hardStopWarningDate: LocalDate?,
  isInHardStopPeriod: Boolean,
  isDueForEarlyRelease: Boolean,
  isDueToBeReleasedInTheNextTwoWorkingDays: Boolean,
  releaseDateLabel: String,
): ModelFoundProbationRecord = ModelFoundProbationRecord(
  kind = null,
  bookingId = bookingId,
  name = name.fullName(),
  crn = crn,
  nomisId = nomisId,
  comName = staff.name?.fullName()?.convertToTitleCase(),
  comStaffCode = staff.code,
  teamName = team.description,
  releaseDate = releaseDate,
  licenceId = null,
  licenceType = licenceType,
  licenceStatus = licenceStatus,
  isOnProbation = false,
  hardStopDate = hardStopDate,
  hardStopWarningDate = hardStopWarningDate,
  isInHardStopPeriod = isInHardStopPeriod,
  isDueForEarlyRelease = isDueForEarlyRelease,
  isDueToBeReleasedInTheNextTwoWorkingDays = isDueToBeReleasedInTheNextTwoWorkingDays,
  releaseDateLabel = releaseDateLabel,
  isReviewNeeded = false,
)

fun Licence.getUpdatedByFullName(): String? {
  val staffMember = this.updatedBy
  return if (staffMember != null) {
    "${staffMember.firstName} ${staffMember.lastName}"
  } else {
    null
  }
}

fun Licence.getSubmittedByFullName(): String? {
  val staffMember = when (this) {
    is HardStopLicence -> this.submittedBy
    is PrrdLicence -> this.submittedBy
    is CrdLicence -> this.submittedBy
    is VariationLicence -> this.submittedBy
    is HdcLicence -> this.submittedBy
    is HdcVariationLicence -> this.submittedBy
    else -> error("Unexpected licence type: $this")
  }
  return if (staffMember != null) {
    "${staffMember.firstName} ${staffMember.lastName}"
  } else {
    null
  }
}

fun transformToApprovalLicenceSummary(
  licence: EntityLicence,
  hardStopDate: LocalDate?,
  hardStopWarningDate: LocalDate?,
  isInHardStopPeriod: Boolean,
  isDueForEarlyRelease: Boolean,
  isDueToBeReleasedInTheNextTwoWorkingDays: Boolean,
): LicenceSummaryApproverView = LicenceSummaryApproverView(
  licenceId = licence.id,
  forename = licence.forename,
  surname = licence.surname,
  dateOfBirth = licence.dateOfBirth,
  licenceStatus = licence.statusCode,
  kind = licence.kind,
  licenceType = licence.typeCode,
  nomisId = licence.nomsId,
  crn = licence.crn,
  bookingId = licence.bookingId,
  prisonCode = licence.prisonCode,
  prisonDescription = licence.prisonDescription,
  probationAreaCode = licence.probationAreaCode,
  probationAreaDescription = licence.probationAreaDescription,
  probationPduCode = licence.probationPduCode,
  probationPduDescription = licence.probationPduDescription,
  probationLauCode = licence.probationLauCode,
  probationLauDescription = licence.probationLauDescription,
  probationTeamCode = licence.probationTeamCode,
  probationTeamDescription = licence.probationTeamDescription,
  comUsername = licence.responsibleCom.username,
  conditionalReleaseDate = licence.conditionalReleaseDate,
  actualReleaseDate = licence.actualReleaseDate,
  sentenceStartDate = licence.sentenceStartDate,
  sentenceEndDate = licence.sentenceEndDate,
  licenceStartDate = licence.licenceStartDate,
  licenceExpiryDate = licence.licenceExpiryDate,
  topupSupervisionStartDate = licence.topupSupervisionStartDate,
  topupSupervisionExpiryDate = licence.topupSupervisionExpiryDate,
  postRecallReleaseDate = licence.postRecallReleaseDate,
  dateCreated = licence.dateCreated,
  submittedDate = licence.submittedDate,
  approvedDate = licence.approvedDate,
  approvedByName = licence.approvedByName,
  licenceVersion = licence.licenceVersion,
  versionOf = getVersionOf(licence),
  isReviewNeeded = when (licence) {
    is HardStopLicence -> (licence.statusCode == LicenceStatus.ACTIVE && licence.reviewDate == null)
    else -> false
  },
  updatedByFullName = licence.getUpdatedByFullName(),
  submittedByFullName = licence.getSubmittedByFullName(),
  hardStopDate = hardStopDate,
  hardStopWarningDate = hardStopWarningDate,
  isInHardStopPeriod = isInHardStopPeriod,
  isDueForEarlyRelease = isDueForEarlyRelease,
  isDueToBeReleasedInTheNextTwoWorkingDays = isDueToBeReleasedInTheNextTwoWorkingDays,
)

fun getVersionOf(licence: Licence): Long? = when (licence) {
  is CrdLicence -> licence.versionOfId
  is HdcLicence -> licence.versionOfId
  is PrrdLicence -> licence.versionOfId
  else -> null
}

fun PrisonerSearchPrisoner.toPrisoner() = Prisoner(
  prisonerNumber = this.prisonerNumber,
  pncNumber = this.pncNumber,
  croNumber = this.croNumber,
  bookingId = this.bookingId,
  bookNumber = this.bookNumber,
  firstName = this.firstName,
  middleNames = this.middleNames,
  lastName = this.lastName,
  dateOfBirth = this.dateOfBirth,
  status = this.status,
  prisonId = this.prisonId,
  locationDescription = this.locationDescription,
  prisonName = this.prisonName,
  legalStatus = this.legalStatus,
  imprisonmentStatus = this.imprisonmentStatus,
  imprisonmentStatusDescription = this.imprisonmentStatusDescription,
  mostSeriousOffence = this.mostSeriousOffence,
  recall = this.recall,
  indeterminateSentence = this.indeterminateSentence,
  sentenceStartDate = this.sentenceStartDate,
  releaseDate = this.releaseDate,
  confirmedReleaseDate = this.confirmedReleaseDate,
  sentenceExpiryDate = this.sentenceExpiryDate,
  licenceExpiryDate = this.licenceExpiryDate,
  homeDetentionCurfewEligibilityDate = this.homeDetentionCurfewEligibilityDate,
  homeDetentionCurfewActualDate = this.homeDetentionCurfewActualDate,
  homeDetentionCurfewEndDate = this.homeDetentionCurfewEndDate,
  topupSupervisionStartDate = this.topupSupervisionStartDate,
  topupSupervisionExpiryDate = this.topupSupervisionExpiryDate,
  paroleEligibilityDate = this.paroleEligibilityDate,
  postRecallReleaseDate = this.postRecallReleaseDate,
  conditionalReleaseDate = this.conditionalReleaseDate,
  actualParoleDate = this.actualParoleDate,
  releaseOnTemporaryLicenceDate = this.releaseOnTemporaryLicenceDate,
)

fun Prisoner.toPrisonerSearchPrisoner() = PrisonerSearchPrisoner(
  prisonerNumber = this.prisonerNumber!!,
  pncNumber = this.pncNumber,
  croNumber = this.croNumber,
  bookingId = this.bookingId,
  bookNumber = this.bookNumber,
  firstName = this.firstName!!,
  middleNames = this.middleNames,
  lastName = this.lastName!!,
  dateOfBirth = this.dateOfBirth!!,
  status = this.status,
  prisonId = this.prisonId,
  locationDescription = this.locationDescription,
  prisonName = this.prisonName,
  legalStatus = this.legalStatus,
  imprisonmentStatus = this.imprisonmentStatus,
  imprisonmentStatusDescription = this.imprisonmentStatusDescription,
  mostSeriousOffence = this.mostSeriousOffence,
  recall = this.recall,
  indeterminateSentence = this.indeterminateSentence,
  sentenceStartDate = this.sentenceStartDate,
  releaseDate = this.releaseDate,
  confirmedReleaseDate = this.confirmedReleaseDate,
  sentenceExpiryDate = this.sentenceExpiryDate,
  licenceExpiryDate = this.licenceExpiryDate,
  homeDetentionCurfewEligibilityDate = this.homeDetentionCurfewEligibilityDate,
  homeDetentionCurfewActualDate = this.homeDetentionCurfewActualDate,
  homeDetentionCurfewEndDate = this.homeDetentionCurfewEndDate,
  topupSupervisionStartDate = this.topupSupervisionStartDate,
  topupSupervisionExpiryDate = this.topupSupervisionExpiryDate,
  paroleEligibilityDate = this.paroleEligibilityDate,
  postRecallReleaseDate = this.postRecallReleaseDate,
  conditionalReleaseDate = this.conditionalReleaseDate,
  actualParoleDate = this.actualParoleDate,
  releaseOnTemporaryLicenceDate = this.releaseOnTemporaryLicenceDate,
)

fun PrisonApiPrisoner.toPrisonerSearchPrisoner() = PrisonerSearchPrisoner(
  prisonerNumber = this.offenderNo,
  bookingId = this.bookingId.toString(),
  firstName = this.firstName,
  middleNames = this.middleName,
  lastName = this.lastName,
  dateOfBirth = this.dateOfBirth,
  legalStatus = this.legalStatus,
  mostSeriousOffence = this.offenceHistory.find { it.mostSerious }?.offenceDescription,
  conditionalReleaseDate = this.sentenceDetail.conditionalReleaseOverrideDate
    ?: this.sentenceDetail.conditionalReleaseDate,
  confirmedReleaseDate = this.sentenceDetail.confirmedReleaseDate,
  homeDetentionCurfewEligibilityDate = this.sentenceDetail.homeDetentionCurfewEligibilityDate,
  homeDetentionCurfewActualDate = this.sentenceDetail.homeDetentionCurfewActualDate,
  topupSupervisionStartDate = this.sentenceDetail.topupSupervisionStartDate,
  topupSupervisionExpiryDate = this.sentenceDetail.topupSupervisionExpiryOverrideDate
    ?: this.sentenceDetail.topupSupervisionExpiryDate,
  paroleEligibilityDate = this.sentenceDetail.paroleEligibilityOverrideDate
    ?: this.sentenceDetail.paroleEligibilityDate,
)

fun transformToModelHdcCurfewAddress(entity: EntityHdcCurfewAddress): ModelHdcCurfewAddress = ModelHdcCurfewAddress(
  id = entity.id,
  addressLine1 = entity.addressLine1,
  addressLine2 = entity.addressLine2,
  townOrCity = entity.townOrCity,
  county = entity.county,
  postcode = entity.postcode,
)

fun transformToModelElectronicMonitoringProvider(entity: EntityElectronicMonitoringProvider): ElectronicMonitoringProvider = ElectronicMonitoringProvider(
  isToBeTaggedForProgramme = entity.isToBeTaggedForProgramme,
  programmeName = entity.programmeName,
)

fun determineElectronicMonitoringProviderStatus(
  electronicMonitoringProvider: EntityElectronicMonitoringProvider?,
): ElectronicMonitoringProviderStatus = when {
  electronicMonitoringProvider == null -> ElectronicMonitoringProviderStatus.NOT_NEEDED
  electronicMonitoringProvider.isToBeTaggedForProgramme == null -> ElectronicMonitoringProviderStatus.NOT_STARTED
  else -> ElectronicMonitoringProviderStatus.COMPLETE
}
