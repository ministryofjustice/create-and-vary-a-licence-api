package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.OneToMany
import javax.persistence.OrderBy
import javax.persistence.Table
import javax.validation.constraints.NotNull

@Entity
@Table(name = "additional_term")
data class AdditionalTerm(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @NotNull
  val id: Long = -1,

  @NotNull
  val licenceId: Long = -1,

  val termCode: String? = null,
  val termSequence: Int? = null,
  val termText: String? = null,

  @JoinColumn(name = "additionalTermId")
  @OrderBy("dataSequence")
  @OneToMany
  val additionalTermData: List<AdditionalTermData> = ArrayList(),
)
