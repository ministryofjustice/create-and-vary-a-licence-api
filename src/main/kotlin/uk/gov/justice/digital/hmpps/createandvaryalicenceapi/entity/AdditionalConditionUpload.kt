package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import org.hibernate.Hibernate
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table
import javax.validation.constraints.NotNull

@Entity
@Table(name = "additional_condition_upload")
data class AdditionalConditionUpload(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @NotNull
  val id: Long = -1,

  @NotNull
  @Column(unique = true)
  val key: String,

  @NotNull
  val licenceId: Long,

  @NotNull
  val category: String,

  @NotNull
  val url: String,

  @NotNull
  val mineType: String,
) {
  @ManyToOne(cascade = [CascadeType.ALL])
  @JoinColumn(name = "additional_condition_id")
  lateinit var additionalCondition: AdditionalCondition

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as AdditionalConditionUpload

    return id != null && id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()

  @Override
  override fun toString(): String {
    return this::class.simpleName + "(id = $id )"
  }
}
