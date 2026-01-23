package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.SentenceDateHolderAdapter.toSentenceDateHolder
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.HARD_STOP
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.PRRD
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.TIME_SERVED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType.AP
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType.AP_PSS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType.PSS
import java.time.LocalDate

@Service
class CvlRecordService(
  private val eligibilityService: EligibilityService,
  private val releaseDateService: ReleaseDateService,
) {

  fun getCvlRecord(prisoner: PrisonerSearchPrisoner): CvlRecord {
    val recordList = getCvlRecords(listOf(prisoner))
    return recordList.first { it.nomisId == prisoner.prisonerNumber }
  }

  fun getCvlRecords(
    prisoners: List<PrisonerSearchPrisoner>,
  ): List<CvlRecord> {
    val nomisIdsToEligibility = eligibilityService.getEligibilityAssessments(prisoners)
    val nomisIdsToEligibleKinds =
      nomisIdsToEligibility.map { (nomisId, eligibility) -> nomisId to eligibility.eligibleKind }.toMap()
    val nomisIdsToLicenceStartDates = releaseDateService.getLicenceStartDates(prisoners, nomisIdsToEligibleKinds)
    return prisoners.map { prisoner ->
      val eligibility = nomisIdsToEligibility[prisoner.prisonerNumber]!!
      val licenceStartDate = nomisIdsToLicenceStartDates[prisoner.prisonerNumber]
      val hardStopKind = when {
        releaseDateService.isTimeServed(prisoner) -> TIME_SERVED
        releaseDateService.isInHardStopPeriod(licenceStartDate) -> HARD_STOP
        else -> null
      }
      val isInHardStopPeriod = releaseDateService.isInHardStopPeriod(licenceStartDate, hardStopKind)

      CvlRecord(
        nomisId = prisoner.prisonerNumber,
        licenceStartDate = licenceStartDate,
        isEligible = eligibility.isEligible,
        eligibleKind = eligibility.eligibleKind,
        ineligibilityReasons = eligibility.ineligibilityReasons,
        hardStopKind = hardStopKind,
        isInHardStopPeriod = isInHardStopPeriod,
        hardStopWarningDate = releaseDateService.getHardStopWarningDate(licenceStartDate, hardStopKind),
        hardStopDate = releaseDateService.getHardStopDate(licenceStartDate, hardStopKind),
        isEligibleForEarlyRelease = releaseDateService.isEligibleForEarlyRelease(
          prisoner.toSentenceDateHolder(
            licenceStartDate,
          ),
        ),
        isDueToBeReleasedInTheNextTwoWorkingDays = releaseDateService.isDueToBeReleasedInTheNextTwoWorkingDays(
          licenceStartDate,
        ),
        licenceType = getLicenceType(prisoner, licenceStartDate, eligibility.eligibleKind),
        isTimedOut = isInHardStopPeriod || hardStopKind == TIME_SERVED,
      )
    }
  }

  private fun getLicenceType(nomisRecord: PrisonerSearchPrisoner, licenceStartDate: LocalDate?, kind: LicenceKind?) = when {
    nomisRecord.licenceExpiryDate == null && nomisRecord.topupSupervisionExpiryDate == null -> AP
    nomisRecord.licenceExpiryDate == null -> PSS
    nomisRecord.topupSupervisionExpiryDate == null || nomisRecord.topupSupervisionExpiryDate <= nomisRecord.licenceExpiryDate -> AP
    kind == PRRD -> getRecallLicenceType(nomisRecord, licenceStartDate)
    else -> AP_PSS
  }

  private fun getRecallLicenceType(nomisRecord: PrisonerSearchPrisoner, licenceStartDate: LocalDate?) = when {
    // If release at SLED, go directly into PSS period
    releaseDateService.isReleaseAtLed(licenceStartDate, nomisRecord.licenceExpiryDate) -> PSS

    // If early release, the period spent on early release is AP
    else -> AP_PSS
  }
}
