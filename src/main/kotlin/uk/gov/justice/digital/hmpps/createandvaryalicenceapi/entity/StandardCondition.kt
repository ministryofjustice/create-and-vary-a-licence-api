package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.Positive

@Entity
@Table(name = "standard_condition")
data class StandardCondition(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @param:Positive
  val id: Long? = null,

  @ManyToOne
  @JoinColumn(name = "licence_id", nullable = false)
  var licence: Licence,

  val conditionCode: String,
  val conditionSequence: Int,
  val conditionText: String,
  val conditionType: String,
) {
  override fun toString(): String = "StandardCondition(" +
    "id=$id, " +
    "licence=${licence.id}, " +
    "conditionCode=$conditionCode, " +
    "conditionSequence=$conditionSequence, " +
    "conditionText=$conditionText, " +
    "conditionType=$conditionType" +
    ")"
}
