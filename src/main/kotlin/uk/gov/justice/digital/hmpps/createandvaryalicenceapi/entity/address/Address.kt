package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.address

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.validation.constraints.Positive
import java.time.LocalDateTime

enum class AddressSource {
  MANUAL,
  OS_PLACES,
}

@Entity
@Table(name = "address")
data class Address(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @field:Positive
  open val id: Long? = null,

  @Column(nullable = false, unique = true)
  val reference: String,

  // Unique Property Reference Number
  @Column(nullable = true, unique = false)
  val uprn: String? = null,

  @Column(name = "first_line", nullable = false)
  val firstLine: String,

  @Column(name = "second_line")
  val secondLine: String? = null,

  @Column(name = "town_or_city", nullable = false)
  val townOrCity: String,

  @Column
  val county: String? = null,

  @Column(nullable = false)
  val postcode: String,

  @Enumerated(EnumType.STRING)
  @Column(name = "source", nullable = false)
  val source: AddressSource,

  val createdTimestamp: LocalDateTime = LocalDateTime.now(),

  val lastUpdatedTimestamp: LocalDateTime = LocalDateTime.now(),
) {
  override fun toString(): String = listOf(
    uprn.orEmpty(),
    reference,
    firstLine,
    secondLine.orEmpty(),
    townOrCity,
    county.orEmpty(),
    postcode,
    source.name,
  ).joinToString(",")

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || this::class != other::class) return false
    other as Address
    return id != null && id == other.id
  }

  override fun hashCode(): Int = id?.hashCode() ?: 0
}
