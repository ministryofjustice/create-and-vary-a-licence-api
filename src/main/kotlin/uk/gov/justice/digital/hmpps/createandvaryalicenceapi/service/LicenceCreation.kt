package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.VariationLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.IN_PROGRESS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.VARIATION_IN_PROGRESS
import java.time.LocalDateTime

object LicenceCreation {
  fun createCopyToEdit(licence: CrdLicence, creator: CommunityOffenderManager): Licence {
    with(licence) {
      return licence.copy(
        id = -1,
        dateCreated = LocalDateTime.now(),
        statusCode = IN_PROGRESS,
        licenceVersion = getNextLicenceVersion(this.licenceVersion!!),
        versionOfId = licence.id,
        createdBy = creator,
      )
    }
  }

  fun createVariation(licence: Licence, creator: CommunityOffenderManager): Licence {
    with(licence) {
      return VariationLicence(
        id = -1,
        typeCode = this.typeCode,
        version = this.version,
        statusCode = VARIATION_IN_PROGRESS,
        variationOfId = licence.id,
        createdBy = creator,
        nomsId = this.nomsId,
        bookingNo = this.bookingNo,
        bookingId = this.bookingId,
        crn = this.crn,
        pnc = this.pnc,
        cro = this.cro,

        prisonCode = this.prisonCode,
        prisonDescription = this.prisonDescription,
        prisonTelephone = this.prisonTelephone,
        forename = this.forename,
        middleNames = this.middleNames,
        surname = this.surname,
        dateOfBirth = this.dateOfBirth,
        conditionalReleaseDate = this.conditionalReleaseDate,
        actualReleaseDate = this.actualReleaseDate,
        sentenceStartDate = this.sentenceStartDate,
        sentenceEndDate = this.sentenceEndDate,
        licenceStartDate = this.licenceStartDate,
        licenceExpiryDate = this.licenceExpiryDate,
        topupSupervisionStartDate = this.topupSupervisionStartDate,
        topupSupervisionExpiryDate = this.topupSupervisionExpiryDate,
        probationAreaCode = this.probationAreaCode,
        probationAreaDescription = this.probationAreaDescription,
        probationPduCode = this.probationPduCode,
        probationPduDescription = this.probationPduDescription,
        probationLauCode = this.probationLauCode,
        probationLauDescription = this.probationLauDescription,
        probationTeamCode = this.probationTeamCode,
        probationTeamDescription = this.probationTeamDescription,
        appointmentPerson = this.appointmentPerson,
        appointmentTime = this.appointmentTime,
        appointmentTimeType = this.appointmentTimeType,
        appointmentAddress = this.appointmentAddress,
        appointmentContact = this.appointmentContact,
        responsibleCom = this.responsibleCom,
        dateCreated = LocalDateTime.now(),
        licenceVersion = getVariationVersion(this.licenceVersion!!),
      )
    }
  }

  private fun getNextLicenceVersion(currentVersion: String): String {
    val (majorVersion, minorVersion) = getVersionParts(currentVersion)
    return "$majorVersion.${minorVersion + 1}"
  }

  private fun getVariationVersion(currentVersion: String): String {
    val (majorVersion) = getVersionParts(currentVersion)
    return "${majorVersion + 1}.0"
  }

  private fun getVersionParts(version: String): Pair<Int, Int> {
    val parts = version.split(".")
    return parts[0].toInt() to parts[1].toInt()
  }
}
