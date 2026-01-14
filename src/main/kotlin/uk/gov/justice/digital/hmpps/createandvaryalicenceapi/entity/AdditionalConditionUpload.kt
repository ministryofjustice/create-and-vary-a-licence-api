package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.time.LocalDateTime

@Entity
@Table(name = "additional_condition_upload")
data class AdditionalConditionUpload(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @field:Positive
  open val id: Long? = null,

  @ManyToOne(fetch = FetchType.LAZY, cascade = [CascadeType.REFRESH])
  @JoinColumn(name = "additional_condition_id", nullable = false)
  var additionalCondition: AdditionalCondition,

  val filename: String? = null,
  val fileType: String? = null,
  val imageType: String? = null,
  val fileSize: Int = 0,
  val imageSize: Int? = 0,
  @param:NotNull
  val uploadedTime: LocalDateTime = LocalDateTime.now(),
  val description: String? = null,
  val thumbnailImageDsUuid: String? = null,
  var originalDataDsUuid: String? = null,
  var fullSizeImageDsUuid: String? = null,
) {

  override fun toString(): String = "AdditionalConditionUpload(id=$id, filename=$filename, fileType=$fileType, imageType=$imageType, fileSize=$fileSize, imageSize=$imageSize, uploadedTime=$uploadedTime, description=$description, thumbnailImageDsUuid=$thumbnailImageDsUuid, originalDataDsUuid=$originalDataDsUuid, fullSizeImageDsUuid=$fullSizeImageDsUuid)"

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as AdditionalConditionUpload

    if (id != other.id) return false
    if (filename != other.filename) return false
    if (fileType != other.fileType) return false
    if (imageType != other.imageType) return false
    if (fileSize != other.fileSize) return false
    if (imageSize != other.imageSize) return false
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
    result = 31 * result + (imageSize ?: 0)
    result = 31 * result + uploadedTime.hashCode()
    result = 31 * result + (description?.hashCode() ?: 0)
    return result
  }
}
