package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.com

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CaseLoadLicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceCreationType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.HARD_STOP
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.APPROVED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.IN_PROGRESS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.TIMED_OUT

object RelevantLicenceFinder {
  fun findRelevantLicencePerCase(licences: List<CaseLoadLicenceSummary>) = when {
    licences.any { it.kind == HARD_STOP } -> findHardStopLicenceToDisplay(licences)
    licences.any { it.licenceStatus == TIMED_OUT } -> findTimedOutLicenceToDisplay(licences)
    else -> findLicenceToDisplay(licences)
  }

  private fun findTimedOutLicenceToDisplay(licences: List<CaseLoadLicenceSummary>): CaseLoadLicenceSummary {
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

    return timedOutLicence.copy(licenceCreationType = LicenceCreationType.PRISON_WILL_CREATE_THIS_LICENCE)
  }

  private fun findHardStopLicenceToDisplay(licences: List<CaseLoadLicenceSummary>): CaseLoadLicenceSummary {
    val hardStopLicence = licences.find { it.kind == HARD_STOP }!!

    if (hardStopLicence.licenceId == null || hardStopLicence.licenceStatus == IN_PROGRESS) {
      return hardStopLicence.copy(
        licenceStatus = TIMED_OUT,
        licenceCreationType = LicenceCreationType.PRISON_WILL_CREATE_THIS_LICENCE,
      )
    }

    return hardStopLicence.copy(
      licenceStatus = TIMED_OUT,
      licenceCreationType = LicenceCreationType.LICENCE_CREATED_BY_PRISON,
    )
  }

  private fun findLicenceToDisplay(licences: List<CaseLoadLicenceSummary>): CaseLoadLicenceSummary {
    val licence: CaseLoadLicenceSummary = if (licences.size > 1) {
      licences.find { licence -> licence.licenceStatus !== APPROVED }!!
    } else {
      licences.first()
    }

    return if (licence.licenceId == null) {
      licence.copy(licenceCreationType = LicenceCreationType.LICENCE_NOT_STARTED)
    } else {
      licence.copy(licenceCreationType = LicenceCreationType.LICENCE_IN_PROGRESS)
    }
  }
}
