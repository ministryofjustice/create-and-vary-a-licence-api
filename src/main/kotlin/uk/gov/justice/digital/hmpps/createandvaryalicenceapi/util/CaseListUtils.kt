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
