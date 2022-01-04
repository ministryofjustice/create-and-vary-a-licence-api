package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import javax.persistence.Basic
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table
import javax.validation.constraints.NotNull

@Entity
@Table(name = "additional_condition_upload_detail")
data class AdditionalConditionUploadDetail(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @NotNull
  val id: Long = -1,

  @NotNull
  val licenceId: Long,

  @NotNull
  val additionalConditionId: Long,

  @Basic
  val originalData: ByteArray? = null,

  @Basic
  val fullSizeImage: ByteArray? = null,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as AdditionalConditionUploadDetail

    if (id != other.id) return false
    if (licenceId != other.licenceId) return false
    if (additionalConditionId != other.additionalConditionId) return false

    return true
  }

  override fun hashCode(): Int {
    var result = id.hashCode()
    result = 31 * result + licenceId.hashCode()
    result = 31 * result + additionalConditionId.hashCode()
    return result
  }
}
