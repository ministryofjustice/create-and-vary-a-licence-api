package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CaCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ComCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.NOT_STARTED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.TIMED_OUT

sealed interface CaseloadType<T> {
  val name: String
  fun kindExtractor(item: T): LicenceKind?
  fun isUnstarted(item: T): Boolean

  abstract class CaCaseload : CaseloadType<CaCase> {
    override fun kindExtractor(item: CaCase) = item.kind
    override fun isUnstarted(item: CaCase) = item.licenceStatus == NOT_STARTED || item.licenceStatus == TIMED_OUT
  }

  object CaPrisonCaseload : CaCaseload() {
    override val name: String = "CA_PRISON"
  }

  object CaProbationCaseload : CaCaseload() {
    override val name: String = "CA_PROBATION"
  }

  abstract class ComCaseload : CaseloadType<ComCase> {
    override fun kindExtractor(item: ComCase) = item.kind
    override fun isUnstarted(item: ComCase) = item.licenceStatus == NOT_STARTED
  }

  object ComCreateStaffCaseload : ComCaseload() {
    override val name: String = "COM_CREATE_STAFF"
  }

  object ComCreateTeamCaseload : ComCaseload() {
    override val name: String = "COM_CREATE_TEAM"
  }

  object ComVaryStaffCaseload : ComCaseload() {
    override val name: String = "COM_VARY_STAFF"
  }

  object ComVaryTeamCaseload : ComCaseload() {
    override val name: String = "COM_VARY_TEAM"
  }
}
