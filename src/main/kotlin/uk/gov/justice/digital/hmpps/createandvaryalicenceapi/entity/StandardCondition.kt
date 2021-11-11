package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table
import javax.validation.constraints.NotNull

@Entity
@Table(name = "standard_condition")
data class StandardCondition(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @NotNull
  val id: Long = -1,

  @NotNull
  val licenceId: Long = -1,

  val conditionCode: String? = null,
  val conditionSequence: Int? = null,
  val conditionText: String? = null,
  val conditionType: String? = null,
)
