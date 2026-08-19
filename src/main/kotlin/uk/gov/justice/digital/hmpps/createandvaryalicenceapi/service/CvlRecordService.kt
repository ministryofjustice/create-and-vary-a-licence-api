package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.HdcStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.SentenceDateHolderAdapter.toSentenceDateHolder
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.HARD_STOP
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.TIME_SERVED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType.AP

@Service
class CvlRecordService(
  private val eligibilityService: EligibilityService,
  private val releaseDateService: ReleaseDateService,
  private val hdcService: HdcService,
) {

  fun getCvlRecord(prisoner: PrisonerSearchPrisoner): CvlRecord {
    val recordList = getCvlRecords(listOf(prisoner))
    return recordList.first { it.nomisId == prisoner.prisonerNumber }
  }

  fun getCvlRecords(
    prisoners: List<PrisonerSearchPrisoner>,
  ): List<CvlRecord> {
    val bookingIdsToHdcStatus = hdcService.getHdcStatus(prisoners)
    val nomisIdsToEligibility = eligibilityService.getEligibilityAssessments(prisoners, bookingIdsToHdcStatus)
    val nomisIdsToEligibleKinds =
      nomisIdsToEligibility.map { (nomisId, eligibility) -> nomisId to eligibility.eligibleKind }.toMap()
    val nomisIdsToLicenceStartDates = releaseDateService.getLicenceStartDates(prisoners, nomisIdsToEligibleKinds)
    return prisoners.map { prisoner ->
      val eligibility = nomisIdsToEligibility[prisoner.prisonerNumber]!!
      val licenceStartDate = nomisIdsToLicenceStartDates[prisoner.prisonerNumber]
      val creationKind = when {
        releaseDateService.isTimeServed(prisoner) -> TIME_SERVED
        releaseDateService.isInHardStopPeriod(licenceStartDate) -> HARD_STOP
        else -> eligibility.eligibleKind?.licenceKind
      }
      val isInHardStopPeriod = releaseDateService.isInHardStopPeriod(licenceStartDate, creationKind)

      CvlRecord(
        nomisId = prisoner.prisonerNumber,
        licenceStartDate = licenceStartDate,
        isEligible = eligibility.isEligible,
        eligibleKind = eligibility.eligibleKind,
        ineligibilityReasons = eligibility.ineligibilityReasons,
        creationKind = creationKind,
        isInHardStopPeriod = isInHardStopPeriod,
        hardStopWarningDate = releaseDateService.getHardStopWarningDate(licenceStartDate, creationKind),
        hardStopDate = releaseDateService.getHardStopDate(licenceStartDate, creationKind),
        isEligibleForEarlyRelease = releaseDateService.isEligibleForEarlyRelease(
          prisoner.toSentenceDateHolder(
            licenceStartDate,
          ),
        ),
        isDueToBeReleasedInTheNextTwoWorkingDays = releaseDateService.isDueToBeReleasedInTheNextTwoWorkingDays(
          licenceStartDate,
        ),
        licenceType = AP,
        isTimedOut = isInHardStopPeriod || creationKind == TIME_SERVED,
        hdcStatus = bookingIdsToHdcStatus[prisoner.bookingId?.toLong()] ?: HdcStatus.NOT_A_HDC_RELEASE,
      )
    }
  }
}
