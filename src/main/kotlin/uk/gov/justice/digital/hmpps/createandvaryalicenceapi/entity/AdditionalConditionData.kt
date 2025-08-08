package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

@Entity
@Table(name = "additional_condition_data")
data class AdditionalConditionData(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @param:Positive
  val id: Long? = null,

  @ManyToOne
  @JoinColumn(name = "additional_condition_id", nullable = false)
  val additionalCondition: AdditionalCondition,

  @param:NotNull
  val dataSequence: Int = -1,

  val dataField: String? = null,
  val dataValue: String? = null,
) {
  override fun toString(): String = "AdditionalConditionData(" +
    "id=$id, " +
    "additionalCondition=${additionalCondition.id}, " +
    "dataSequence=$dataSequence, " +
    "dataField=$dataField, " +
    "dataValue=$dataValue" +
    ")"
}
