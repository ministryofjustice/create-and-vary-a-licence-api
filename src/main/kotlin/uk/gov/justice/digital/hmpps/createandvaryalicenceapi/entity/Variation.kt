package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AppointmentPersonType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AppointmentTimeType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate
import java.time.LocalDateTime

abstract class Variation(
  id: Long = -1L,
  kind: LicenceKind,
  typeCode: LicenceType,
  version: String? = null,
  statusCode: LicenceStatus = LicenceStatus.VARIATION_IN_PROGRESS,
  nomsId: String? = null,
  bookingNo: String? = null,
  bookingId: Long? = null,
  crn: String? = null,
  pnc: String? = null,
  cro: String? = null,
  prisonCode: String? = null,
  prisonDescription: String? = null,
  prisonTelephone: String? = null,
  forename: String? = null,
  middleNames: String? = null,
  surname: String? = null,
  dateOfBirth: LocalDate? = null,
  conditionalReleaseDate: LocalDate? = null,
  actualReleaseDate: LocalDate? = null,
  sentenceStartDate: LocalDate? = null,
  sentenceEndDate: LocalDate? = null,
  licenceStartDate: LocalDate? = null,
  licenceExpiryDate: LocalDate? = null,
  licenceActivatedDate: LocalDateTime? = null,
  topupSupervisionStartDate: LocalDate? = null,
  topupSupervisionExpiryDate: LocalDate? = null,
  postRecallReleaseDate: LocalDate? = null,
  probationAreaCode: String? = null,
  probationAreaDescription: String? = null,
  probationPduCode: String? = null,
  probationPduDescription: String? = null,
  probationLauCode: String? = null,
  probationLauDescription: String? = null,
  probationTeamCode: String? = null,
  probationTeamDescription: String? = null,
  appointmentPersonType: AppointmentPersonType? = null,
  appointmentPerson: String? = null,
  appointmentTime: LocalDateTime? = null,
  appointmentTimeType: AppointmentTimeType? = null,
  appointmentAddress: String? = null,
  appointmentContact: String? = null,
  var spoDiscussion: String? = null,
  var vloDiscussion: String? = null,
  approvedDate: LocalDateTime? = null,
  approvedByUsername: String? = null,
  approvedByName: String? = null,
  supersededDate: LocalDateTime? = null,
  submittedDate: LocalDateTime? = null,
  dateCreated: LocalDateTime? = null,
  dateLastUpdated: LocalDateTime? = null,
  updatedByUsername: String? = null,
  standardConditions: List<StandardCondition> = emptyList(),
  additionalConditions: List<AdditionalCondition> = emptyList(),
  bespokeConditions: List<BespokeCondition> = emptyList(),
  responsibleCom: CommunityOffenderManager? = null,
  var variationOfId: Long? = null,
  licenceVersion: String? = "1.0",
  updatedBy: Staff? = null,
) : Licence(
  id = id,
  kind = kind,
  typeCode = typeCode,
  version = version,
  statusCode = statusCode,
  nomsId = nomsId,
  bookingNo = bookingNo,
  bookingId = bookingId,
  crn = crn,
  pnc = pnc,
  cro = cro,
  prisonCode = prisonCode,
  prisonDescription = prisonDescription,
  prisonTelephone = prisonTelephone,
  forename = forename,
  middleNames = middleNames,
  surname = surname,
  dateOfBirth = dateOfBirth,
  conditionalReleaseDate = conditionalReleaseDate,
  actualReleaseDate = actualReleaseDate,
  sentenceStartDate = sentenceStartDate,
  sentenceEndDate = sentenceEndDate,
  licenceStartDate = licenceStartDate,
  licenceExpiryDate = licenceExpiryDate,
  licenceActivatedDate = licenceActivatedDate,
  topupSupervisionStartDate = topupSupervisionStartDate,
  topupSupervisionExpiryDate = topupSupervisionExpiryDate,
  postRecallReleaseDate = postRecallReleaseDate,
  probationAreaCode = probationAreaCode,
  probationAreaDescription = probationAreaDescription,
  probationPduCode = probationPduCode,
  probationPduDescription = probationPduDescription,
  probationLauCode = probationLauCode,
  probationLauDescription = probationLauDescription,
  probationTeamCode = probationTeamCode,
  probationTeamDescription = probationTeamDescription,
  appointmentPersonType = appointmentPersonType,
  appointmentPerson = appointmentPerson,
  appointmentTimeType = appointmentTimeType,
  appointmentTime = appointmentTime,
  appointmentAddress = appointmentAddress,
  appointmentContact = appointmentContact,
  approvedDate = approvedDate,
  approvedByUsername = approvedByUsername,
  approvedByName = approvedByName,
  supersededDate = supersededDate,
  submittedDate = submittedDate,
  dateCreated = dateCreated,
  dateLastUpdated = dateLastUpdated,
  updatedByUsername = updatedByUsername,
  licenceVersion = licenceVersion,
  standardConditions = standardConditions,
  additionalConditions = additionalConditions,
  bespokeConditions = bespokeConditions,
  responsibleCom = responsibleCom,
  updatedBy = updatedBy,
) {

  fun updateSpoDiscussion(spoDiscussion: String?, staffMember: Staff?) {
    this.spoDiscussion = spoDiscussion
    recordUpdate(staffMember)
  }

  fun updateVloDiscussion(vloDiscussion: String?, staffMember: Staff?) {
    this.vloDiscussion = vloDiscussion
    recordUpdate(staffMember)
  }

  fun recordUpdate(staffMember: Staff?) {
    this.dateLastUpdated = LocalDateTime.now()
    this.updatedByUsername = staffMember?.username ?: SYSTEM_USER
    this.updatedBy = staffMember ?: updatedBy
  }
}
