package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import org.hibernate.annotations.LazyCollection
import org.hibernate.annotations.LazyCollectionOption
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType.IDENTITY
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.OneToMany
import javax.persistence.OrderBy
import javax.persistence.Table
import javax.validation.constraints.NotNull

@Entity
@Table(name = "additional_condition")
data class AdditionalCondition(
  @Id
  @GeneratedValue(strategy = IDENTITY)
  @NotNull
  val id: Long = -1,

  @NotNull
  val licenceId: Long = -1,

  val conditionCode: String? = null,
  val conditionSequence: Int? = null,
  val conditionText: String? = null,

  @JoinColumn(name = "additionalTermId")
  @Fetch(FetchMode.SUBSELECT)
  @LazyCollection(LazyCollectionOption.FALSE)
  @OrderBy("dataSequence")
  @OneToMany
  val additionalConditionData: List<AdditionalConditionData> = emptyList(),
)
