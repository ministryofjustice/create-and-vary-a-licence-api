package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table
import javax.validation.constraints.NotNull

@Entity
@Table(name = "additional_term_data")
data class AdditionalTermData(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @NotNull
  val id: Long = -1,

  @NotNull
  val additionalTermId: Long = -1,

  @NotNull
  val dataSequence: Int = -1,

  val dataDescription: String? = null,
  val dataFormat: String? = null,
  val dataValue: String? = null,
)
