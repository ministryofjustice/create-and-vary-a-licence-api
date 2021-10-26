package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table
import javax.validation.constraints.NotNull

@Entity
@Table(name = "additional_condition_data")
data class AdditionalConditionData(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @NotNull
  val id: Long = -1,

  @ManyToOne
  @JoinColumn(name = "additional_condition_id", nullable = false)
  val additionalCondition: AdditionalCondition,

  @NotNull
  val dataSequence: Int = -1,

  val dataField: String? = null,
  val dataValue: String? = null,
)
