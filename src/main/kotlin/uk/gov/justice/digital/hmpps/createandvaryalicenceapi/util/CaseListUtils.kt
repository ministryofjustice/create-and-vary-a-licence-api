package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ManagedCase
import java.time.Clock
import java.time.LocalDate

/**
 * Safely compare if two nullable dates are different.
 * Returns true if either date is different or one date is null
 */
fun isParoleEligible(ped: LocalDate?, clock: Clock? = null): Boolean {
  val today = LocalDate.now(clock)
  if (ped == null) return false
  return ped > today
}

fun isEligibleEDS(
  ped: LocalDate?,
  crd: LocalDate?,
  ard: LocalDate?,
  apd: LocalDate?,
  clock: Clock? = null,
): Boolean {
  if (ped == null) return true // All EDSs have PEDs, so if no ped, not an EDS and can stop the check here
  if (crd == null) return false // This should never be hit as a previous filter removes those without CRDs

  val today = LocalDate.now(clock)

  // if PED is in the future, they are OOS
  if (ped > today) return false

  // if ARD is not between CRD - 4 days and CRD (to account for bank holidays and weekends), then OOS
  if (ard != null && (crd.minusDays(4) > ard && ard < crd)) {
    return false
  }

  // an APD with a PED in the past means they were a successful parole applicant on a later attempt, so are OOS
  if (apd != null) {
    return false
  }

  return true
}

fun isBreachOfTopUpSupervision(offender: ManagedCase): Boolean =
  offender.nomisRecord?.imprisonmentStatus != null && offender.nomisRecord.imprisonmentStatus == "BOTUS"

fun isRecall(offender: ManagedCase): Boolean {
  val recall = offender.nomisRecord?.recall == true
  val crd = offender.nomisRecord?.conditionalReleaseDate
  val prrd = offender.nomisRecord?.postRecallReleaseDate

  // If a CRD but no PRRD it should NOT be treated as a recall
  if (crd != null && prrd == null) {
    return false
  }

  if (crd != null) {
    // If the PRRD > CRD - it should be treated as a recall
    // If PRRD <= CRD - should not be treated as a recall
    return prrd!!.isAfter(crd)
  }

  // Trust the Nomis recall flag as a fallback position - the above rules should always override
  return recall
}
