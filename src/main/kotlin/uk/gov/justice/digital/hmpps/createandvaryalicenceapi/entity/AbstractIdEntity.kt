package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import jakarta.persistence.Column
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType.IDENTITY
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import jakarta.validation.constraints.Positive

@MappedSuperclass
abstract class AbstractIdEntity(
  @Id
  @GeneratedValue(strategy = IDENTITY)
  @Column(name = "id")
  @param:Positive
  protected val idInternal: Long? = null,
) {

  val id: Long
    get() = idInternal ?: -1

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || this::class != other::class) return false
    other as AbstractIdEntity
    return id != null && id == other.id
  }

  override fun hashCode(): Int = id.hashCode()

  override fun toString(): String = this::class.simpleName + "(id=$id)"
}
