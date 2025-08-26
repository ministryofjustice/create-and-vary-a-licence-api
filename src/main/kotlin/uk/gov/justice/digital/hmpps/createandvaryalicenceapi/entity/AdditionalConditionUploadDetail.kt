package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

@Entity
@Table(name = "additional_condition_upload_detail")
data class AdditionalConditionUploadDetail(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @field:Positive
  open val id: Long? = null,

  @param:NotNull
  val licenceId: Long,

  @param:NotNull
  val additionalConditionId: Long,

  var originalDataDsUuid: String? = null,

  var fullSizeImageDsUuid: String? = null,
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
