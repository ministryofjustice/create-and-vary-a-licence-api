package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import jakarta.persistence.Basic
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.PrimaryKeyJoinColumn
import jakarta.persistence.SecondaryTable
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull

@Entity
@Table(name = "additional_condition_upload_detail")
@SecondaryTable(
  name = "additional_condition_upload_summary",
  pkJoinColumns = [PrimaryKeyJoinColumn(name = "additional_condition_id")],
)
data class AdditionalConditionDocuments(
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
  var originalDataDsUuid: String? = null,
  var fullSizeImageDsUuid: String? = null,
  @Basic
  @Column(name = "thumbnailImage", table = "additional_condition_upload_summary")
  val thumbnailImage: ByteArray? = null,
  @Column(name = "thumbnailImageDsUuid", table = "additional_condition_upload_summary")
  var thumbnailImageDsUuid: String? = null,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as AdditionalConditionDocuments

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
