package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import java.time.LocalDateTime
import javax.persistence.Basic
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table
import javax.validation.constraints.NotNull

@Entity
@Table(name = "additional_condition_upload_summary")
data class AdditionalConditionUploadSummary(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @NotNull
  val id: Long = -1,

  @ManyToOne
  @JoinColumn(name = "additional_condition_id", nullable = false)
  val additionalCondition: AdditionalCondition,

  val filename: String? = null,

  val fileType: String? = null,

  val fileSize: Int = 0,

  @NotNull
  val uploadedTime: LocalDateTime = LocalDateTime.now(),

  val description: String? = null,

  @Basic
  val thumbnailImage: ByteArray? = null,

  @NotNull
  val uploadDetailId: Long
) {

  override fun toString(): String {
    return "AdditionalConditionUploadSummary(id=$id, fileName=$filename, fileType=$fileType, fileSize=$fileSize, uploadedTime=$uploadedTime, description=$description)"
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as AdditionalConditionUploadSummary

    if (id != other.id) return false
    if (filename != other.filename) return false
    if (fileType != other.fileType) return false
    if (fileSize != other.fileSize) return false
    if (uploadedTime != other.uploadedTime) return false
    if (description != other.description) return false

    return true
  }

  override fun hashCode(): Int {
    var result = id.hashCode()
    result = 31 * result + (filename?.hashCode() ?: 0)
    result = 31 * result + (fileType?.hashCode() ?: 0)
    result = 31 * result + fileSize
    result = 31 * result + uploadedTime.hashCode()
    result = 31 * result + (description?.hashCode() ?: 0)
    return result
  }
}
