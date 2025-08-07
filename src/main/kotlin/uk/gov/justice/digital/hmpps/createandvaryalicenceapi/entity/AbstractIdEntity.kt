package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import jakarta.persistence.Column
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType.IDENTITY
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import jakarta.validation.constraints.Positive

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
  @param:Positive
  protected val idInternal: Long? = null,
) {

  val id: Long
    get() = idInternal ?: -1

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || this::class != other::class) return false
    other as AbstractIdEntity
    return idInternal != null && idInternal == other.idInternal
  }

  override fun hashCode(): Int = id.hashCode()

  override fun toString(): String = this::class.simpleName + "(id=$id)"
}
