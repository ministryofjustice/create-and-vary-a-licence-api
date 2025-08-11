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
@Table(name = "bespoke_condition")
data class BespokeCondition(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @field:Positive
  open val id: Long? = null,

  @ManyToOne
  @JoinColumn(name = "licence_id", nullable = false)
  var licence: Licence,

  val conditionSequence: Int? = null,
  val conditionText: String,
) {
  override fun toString(): String = "BespokeCondition(" +
    "id=$id, " +
    "licence=${licence.id}, " +
    "conditionSequence=$conditionSequence, " +
    "conditionText=$conditionText" +
    ")"
}
