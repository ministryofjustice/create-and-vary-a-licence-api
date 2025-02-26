package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull

@Entity
@Table(name = "standard_condition")
data class StandardCondition(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @NotNull
  val id: Long = -1,

  @ManyToOne
  @JoinColumn(name = "licence_id", nullable = false)
  var licence: Licence,

  val conditionCode: String? = null,
  val conditionSequence: Int? = null,
  val conditionText: String? = null,
  val conditionType: String? = null,
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
