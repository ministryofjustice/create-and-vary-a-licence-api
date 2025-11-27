package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CaCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ComCreateCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ComVaryCase
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

  abstract class ComCreateCaseload : CaseloadType<ComCreateCase> {
    override fun kindExtractor(item: ComCreateCase) = item.kind
    override fun isUnstarted(item: ComCreateCase) = item.licenceStatus == NOT_STARTED
  }

  object ComCreateStaffCaseload : ComCreateCaseload() {
    override val name: String = "COM_CREATE_STAFF"
  }

  object ComCreateTeamCaseload : ComCreateCaseload() {
    override val name: String = "COM_CREATE_TEAM"
  }

  abstract class ComVaryCaseload : CaseloadType<ComVaryCase> {
    override fun kindExtractor(item: ComVaryCase) = item.kind
    override fun isUnstarted(item: ComVaryCase) = item.licenceStatus == NOT_STARTED
  }

  object ComVaryStaffCaseload : ComVaryCaseload() {
    override val name: String = "COM_VARY_STAFF"
  }

  object ComVaryTeamCaseload : ComVaryCaseload() {
    override val name: String = "COM_VARY_TEAM"
  }
}
