package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ca

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.model.LicenceCaCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.CaViewCasesTab.ATTENTION_NEEDED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.CaViewCasesTab.FUTURE_RELEASES
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.CaViewCasesTab.RELEASES_IN_NEXT_TWO_WORKING_DAYS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.APPROVED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.IN_PROGRESS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.NOT_STARTED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.SUBMITTED
import java.time.Clock
import java.time.LocalDate

object Tabs {

  private val inflightStatuses = setOf(
    IN_PROGRESS,
    SUBMITTED,
    APPROVED,
    NOT_STARTED,
  )

  fun determineCaViewCasesTab(
    isDueToBeReleasedInTheNextTwoWorkingDays: Boolean,
    licenceStartDate: LocalDate?,
    licenceCaCase: LicenceCaCase?,
    timeServedCase: Boolean,
    now: Clock,
  ) = when {
    isAttentionNeeded(
      licenceCaCase?.statusCode ?: NOT_STARTED,
      licenceStartDate,
      timeServedCase,
      now,
    ) -> ATTENTION_NEEDED

    isDueToBeReleasedInTheNextTwoWorkingDays || timeServedCase -> RELEASES_IN_NEXT_TWO_WORKING_DAYS

    else -> FUTURE_RELEASES
  }

  private fun isAttentionNeeded(
    status: LicenceStatus,
    licenceStartDate: LocalDate?,
    timeServedCase: Boolean,
    now: Clock,
  ): Boolean {
    val today = LocalDate.now(now)

    val missingStartDate = inflightStatuses.contains(status) && licenceStartDate == null
    val startDateInPast = status == APPROVED && licenceStartDate?.isBefore(today) == true && !timeServedCase

    return missingStartDate || startDateInPast
  }
}
