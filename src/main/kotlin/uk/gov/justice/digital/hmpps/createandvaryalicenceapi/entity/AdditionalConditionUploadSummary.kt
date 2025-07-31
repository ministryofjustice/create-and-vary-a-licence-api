package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import jakarta.persistence.Basic
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

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

  val imageType: String? = null,

  val fileSize: Int = 0,

  @NotNull
  val uploadedTime: LocalDateTime = LocalDateTime.now(),

  val description: String? = null,

  @Basic
  val thumbnailImage: ByteArray? = null,
  val thumbnailImageDsUuid: String? = null,

  @NotNull
  val uploadDetailId: Long,
) {

  @Transient
  var preloadedThumbnailImage: ByteArray? = null

  override fun toString(): String = "AdditionalConditionUploadSummary(id=$id, fileName=$filename, fileType=$fileType, imageType=$imageType, fileSize=$fileSize, uploadedTime=$uploadedTime, description=$description)"

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as AdditionalConditionUploadSummary

    if (id != other.id) return false
    if (filename != other.filename) return false
    if (fileType != other.fileType) return false
    if (imageType != other.imageType) return false
    if (fileSize != other.fileSize) return false
    if (uploadedTime != other.uploadedTime) return false
    if (description != other.description) return false

    return true
  }

  override fun hashCode(): Int {
    var result = id.hashCode()
    result = 31 * result + (filename?.hashCode() ?: 0)
    result = 31 * result + (fileType?.hashCode() ?: 0)
    result = 31 * result + (imageType?.hashCode() ?: 0)
    result = 31 * result + fileSize
    result = 31 * result + uploadedTime.hashCode()
    result = 31 * result + (description?.hashCode() ?: 0)
    return result
  }
}
