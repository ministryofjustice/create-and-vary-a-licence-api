package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.com

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceCreationType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceCreationType.LICENCE_CREATED_BY_PRISON
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceCreationType.LICENCE_IN_PROGRESS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceCreationType.LICENCE_NOT_STARTED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceCreationType.PRISON_WILL_CREATE_THIS_LICENCE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.HARD_STOP
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.TIME_SERVED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.APPROVED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.IN_PROGRESS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.TIMED_OUT

object RelevantLicenceFinder {
  fun findRelevantLicencePerCase(licences: List<ComCreateCaseloadLicenceDto>) = when {
    licences.any { it.kind == HARD_STOP } -> prepareLicenceForDisplayByKind(licences, HARD_STOP)
    licences.any { it.kind == TIME_SERVED } -> prepareLicenceForDisplayByKind(licences, TIME_SERVED)
    licences.any { it.licenceStatus == TIMED_OUT } -> findTimedOutLicenceToDisplay(licences)
    else -> findLicenceToDisplay(licences)
  }

  private fun findTimedOutLicenceToDisplay(licences: List<ComCreateCaseloadLicenceDto>): ComCreateCaseloadLicenceDto {
    val timedOutLicence = licences.find { it.licenceStatus == TIMED_OUT }!!

    if (timedOutLicence.versionOf != null) {
      val previouslyApproved = licences.find { licence -> licence.licenceId == timedOutLicence.versionOf }
      if (previouslyApproved != null) {
        return previouslyApproved.copy(
          licenceStatus = TIMED_OUT,
          licenceCreationType = LicenceCreationType.LICENCE_CHANGES_NOT_APPROVED_IN_TIME,
        )
      }
    }

    return timedOutLicence.copy(licenceCreationType = PRISON_WILL_CREATE_THIS_LICENCE)
  }

  private fun prepareLicenceForDisplayByKind(licences: List<ComCreateCaseloadLicenceDto>, kind: LicenceKind): ComCreateCaseloadLicenceDto {
    val licence = licences.find { it.kind == kind }!!
    val creationType = if (licence.licenceId == null || licence.licenceStatus == IN_PROGRESS) {
      PRISON_WILL_CREATE_THIS_LICENCE
    } else {
      LICENCE_CREATED_BY_PRISON
    }
    return licence.copy(
      licenceStatus = TIMED_OUT,
      licenceCreationType = creationType,
    )
  }

  private fun findLicenceToDisplay(licences: List<ComCreateCaseloadLicenceDto>): ComCreateCaseloadLicenceDto {
    val licence: ComCreateCaseloadLicenceDto = if (licences.size > 1) {
      licences.find { licence -> licence.licenceStatus !== APPROVED }!!
    } else {
      licences.first()
    }

    return if (licence.licenceId == null) {
      licence.copy(licenceCreationType = LICENCE_NOT_STARTED)
    } else {
      licence.copy(licenceCreationType = LICENCE_IN_PROGRESS)
    }
  }
}
