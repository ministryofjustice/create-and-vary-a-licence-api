package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType.IDENTITY
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
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
  val id: Long? = null,

  @ManyToOne
  @JoinColumn(name = "licence_id", nullable = false)
  var licence: Licence? = null,

  val conditionCode: String? = null,
  var conditionSequence: Int? = null,
  var conditionText: String? = null,

  @OneToMany(mappedBy = "additionalCondition", fetch = FetchType.EAGER, cascade = [CascadeType.ALL], orphanRemoval = true)
  @Fetch(FetchMode.SUBSELECT)
  @OrderBy("dataSequence")
  val additionalConditionData: List<AdditionalConditionData> = emptyList(),
)
