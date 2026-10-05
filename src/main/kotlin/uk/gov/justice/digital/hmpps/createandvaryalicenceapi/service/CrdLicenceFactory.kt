package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.time.LocalDateTime

@Component
class CrdLicenceFactory {

  fun createFromHdc(licence: HdcLicence, statusCode: LicenceStatus? = null) = CrdLicence(
    typeCode = licence.typeCode,
    version = licence.version,
    statusCode = statusCode ?: licence.statusCode,
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
    licenceActivatedDate = licence.licenceActivatedDate,
    topupSupervisionStartDate = licence.topupSupervisionStartDate,
    topupSupervisionExpiryDate = licence.topupSupervisionExpiryDate,
    postRecallReleaseDate = licence.postRecallReleaseDate,
    probationAreaCode = licence.probationAreaCode,
    probationAreaDescription = licence.probationAreaDescription,
    probationPduCode = licence.probationPduCode,
    probationPduDescription = licence.probationPduDescription,
    probationLauCode = licence.probationLauCode,
    probationLauDescription = licence.probationLauDescription,
    probationTeamCode = licence.probationTeamCode,
    probationTeamDescription = licence.probationTeamDescription,
    probationContact = null,
    approvedDate = licence.approvedDate,
    approvedByUsername = licence.approvedByUsername,
    approvedByName = licence.approvedByName,
    supersededDate = licence.supersededDate,
    submittedDate = licence.submittedDate,
    dateCreated = LocalDateTime.now(),
    dateLastUpdated = licence.dateLastUpdated,
    updatedByUsername = licence.updatedByUsername,
    licenceVersion = licence.licenceVersion,
    updatedBy = licence.updatedBy,
    createdBy = licence.createdBy,
    versionOfId = licence.id,
    responsibleCom = licence.getCom(),
  )
}
