package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import jakarta.persistence.Column
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType.IDENTITY
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import jakarta.validation.constraints.Positive
import org.hibernate.Hibernate

/**
 * Base mapped superclass for entities with a generated primary key.
 *
 * The nullable `idInternal` is assigned by the database upon persistence,
 * using the IDENTITY generation strategy.
 * The public `id` property exposes the ID or returns -1 if not yet assigned.
 *
 * Equality and hashCode implementations are based on `idInternal`.
 */
@MappedSuperclass
abstract class AbstractIdEntity(
  @Id
  @GeneratedValue(strategy = IDENTITY)
  @Column(name = "id")
  @field:Positive
  protected open val idInternal: Long? = null,
) {

  open val id: Long
    get() = idInternal ?: -1

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null) return false
    if (Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as AbstractIdEntity
    return this.idInternal != null && this.idInternal == other.idInternal
  }

  override fun hashCode(): Int = idInternal?.hashCode() ?: 0

  override fun toString(): String = "${Hibernate.getClass(this).simpleName}(id=$idInternal)"
}
