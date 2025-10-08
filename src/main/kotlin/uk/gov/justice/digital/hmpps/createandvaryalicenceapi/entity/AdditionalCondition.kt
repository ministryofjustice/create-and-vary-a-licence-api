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
import jakarta.validation.constraints.Positive
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode

@Entity
@Table(name = "additional_condition")
data class AdditionalCondition(
  @Id
  @GeneratedValue(strategy = IDENTITY)
  @field:Positive
  open val id: Long? = null,

  @ManyToOne
  @JoinColumn(name = "licence_id", nullable = false)
  var licence: Licence,

  var conditionVersion: String,
  val conditionCode: String,
  var conditionCategory: String,
  var conditionSequence: Int? = null,
  var conditionText: String,
  var expandedConditionText: String? = null,
  var conditionType: String,

  // TODO consider lazy loading for these collections if performance issues arise
  @OneToMany(
    mappedBy = "additionalCondition",
    fetch = FetchType.EAGER,
    cascade = [CascadeType.ALL],
    orphanRemoval = true,
  )
  @Fetch(FetchMode.SUBSELECT)
  @OrderBy("dataSequence")
  val additionalConditionData: MutableList<AdditionalConditionData> = mutableListOf(),

  @OneToMany(
    mappedBy = "additionalCondition",
    fetch = FetchType.EAGER,
    cascade = [CascadeType.ALL],
    orphanRemoval = true,
  )
  @Fetch(FetchMode.SUBSELECT)
  @OrderBy("id")
  val additionalConditionUploadSummary: MutableList<AdditionalConditionUploadSummary> = mutableListOf(),
) {

  override fun toString(): String = "AdditionalCondition(" +
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
