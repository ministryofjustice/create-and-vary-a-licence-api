package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence.Companion.SYSTEM_USER
import java.time.LocalDateTime

interface Variation {
  var spoDiscussion: String?
  var vloDiscussion: String?
  var dateLastUpdated: LocalDateTime?
  var updatedByUsername: String?
  var variationOfId: Long?
  var updatedBy: Staff?

  fun updateSpoDiscussion(spoDiscussion: String?, staffMember: Staff?) {
    this.spoDiscussion = spoDiscussion
    recordUpdate(staffMember)
  }

  fun updateVloDiscussion(vloDiscussion: String?, staffMember: Staff?) {
    this.vloDiscussion = vloDiscussion
    recordUpdate(staffMember)
  }

  fun recordUpdate(staffMember: Staff?) {
    this.dateLastUpdated = LocalDateTime.now()
    this.updatedByUsername = staffMember?.username ?: SYSTEM_USER
    this.updatedBy = staffMember ?: updatedBy
  }
}
