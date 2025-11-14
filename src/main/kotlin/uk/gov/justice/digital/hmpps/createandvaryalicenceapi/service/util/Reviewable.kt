package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.util

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence.Companion.SYSTEM_USER
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Staff
import java.time.LocalDateTime

interface Reviewable {
  var dateLastUpdated: LocalDateTime?
  var updatedBy: Staff?
  var updatedByUsername: String?
  var reviewDate: LocalDateTime?

  fun markAsReviewed(staff: Staff?) {
    reviewDate = LocalDateTime.now()
    dateLastUpdated = LocalDateTime.now()
    updatedByUsername = staff?.username ?: SYSTEM_USER
    updatedBy = staff ?: this.updatedBy
  }
}
