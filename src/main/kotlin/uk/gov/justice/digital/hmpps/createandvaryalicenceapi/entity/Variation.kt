package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence.Companion.SYSTEM_USER
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.time.LocalDateTime

interface Variation {
  val id: Long
  var spoDiscussion: String?
  var vloDiscussion: String?
  var dateLastUpdated: LocalDateTime?
  var updatedByUsername: String?
  var variationOfId: Long?
  var updatedBy: Staff?
  var statusCode: LicenceStatus
  var approvedByUsername: String?
  var approvedDate: LocalDateTime?
  var approvedByName: String?
  var createdBy: CommunityOffenderManager?

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

  fun referVariation(staffMember: Staff?) {
    this.statusCode = LicenceStatus.VARIATION_REJECTED
    recordUpdate(staffMember)
  }

  fun approveVariation(username: String, staffMember: Staff?) {
    this.statusCode = LicenceStatus.VARIATION_APPROVED
    this.approvedByUsername = username
    this.approvedDate = LocalDateTime.now()
    this.approvedByName = "${staffMember?.firstName} ${staffMember?.lastName}"
    recordUpdate(staffMember)
  }
}
