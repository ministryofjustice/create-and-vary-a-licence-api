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
    now: Clock,
  ) = when {
    isAttentionNeeded(licenceCaCase?.statusCode ?: NOT_STARTED, licenceStartDate) -> ATTENTION_NEEDED
    isDueToBeReleasedInTheNextTwoWorkingDays || licenceStartDate?.isBefore(LocalDate.now(now)) ?: false -> RELEASES_IN_NEXT_TWO_WORKING_DAYS
    else -> FUTURE_RELEASES
  }

  private fun isAttentionNeeded(status: LicenceStatus, licenceStartDate: LocalDate?): Boolean = inflightStatuses.contains(status) && licenceStartDate == null
}
