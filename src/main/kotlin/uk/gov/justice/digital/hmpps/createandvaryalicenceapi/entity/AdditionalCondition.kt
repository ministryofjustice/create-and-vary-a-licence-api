package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType.IDENTITY
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode

@Entity
@Table(name = "additional_condition")
data class AdditionalCondition(
  @Id
  @GeneratedValue(strategy = IDENTITY)
  @NotNull
  val id: Long = -1,

  @ManyToOne
  @JoinColumn(name = "licence_id", nullable = false)
  var licence: Licence,

  val conditionVersion: String,
  val conditionCode: String? = null,
  var conditionCategory: String? = null,
  var conditionSequence: Int? = null,
  var conditionText: String? = null,
  var expandedConditionText: String? = null,
  var conditionType: String? = null,

  @OneToMany(mappedBy = "additionalCondition", fetch = FetchType.EAGER, cascade = [CascadeType.ALL], orphanRemoval = true)
  @Fetch(FetchMode.SUBSELECT)
  @OrderBy("dataSequence")
  val additionalConditionData: List<AdditionalConditionData> = emptyList(),

  @OneToMany(mappedBy = "additionalCondition", fetch = FetchType.EAGER, cascade = [CascadeType.ALL], orphanRemoval = true)
  @Fetch(FetchMode.SUBSELECT)
  @OrderBy("id")
  val additionalConditionUploadSummary: List<AdditionalConditionUploadSummary> = emptyList(),
) {

  override fun toString(): String {
    return "AdditionalCondition(" +
      "id=$id, " +
      "licence=${licence.id}, " +
      "conditionVersion='$conditionVersion', " +
      "conditionCode=$conditionCode, " +
      "conditionCategory=$conditionCategory, " +
      "conditionSequence=$conditionSequence, " +
      "conditionText=$conditionText, " +
      "expandedConditionText=$expandedConditionText, " +
      "conditionType=$conditionType, " +
      "additionalConditionData=$additionalConditionData, " +
      "additionalConditionUploadSummary=$additionalConditionUploadSummary" +
      ")"
  }
}
