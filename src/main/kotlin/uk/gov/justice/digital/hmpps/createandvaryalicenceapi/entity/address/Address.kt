package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.address

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType.IDENTITY
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.Hibernate
import java.time.LocalDateTime

enum class AddressSource {
  MANUAL,
  OS_PLACES,
}

@Entity
@Table(name = "address")
class Address(
  @Id
  @GeneratedValue(strategy = IDENTITY)
  val id: Long? = null,

  @Column(nullable = false, unique = true)
  val reference: String,

  // Unique Property Reference Number
  @Column(nullable = true, unique = false)
  var uprn: String? = null,

  @Column(name = "first_line", nullable = false)
  var firstLine: String,

  @Column(name = "second_line")
  var secondLine: String? = null,

  @Column(name = "town_or_city", nullable = false)
  var townOrCity: String,

  @Column
  var county: String? = null,

  @Column(nullable = false)
  var postcode: String,

  @Enumerated(EnumType.STRING)
  @Column(name = "source", nullable = false)
  var source: AddressSource,

  val createdTimestamp: LocalDateTime = LocalDateTime.now(),

  var lastUpdatedTimestamp: LocalDateTime = LocalDateTime.now(),
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
    if (other == null) return false
    if (Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as Address
    return id != null && id == other.id
  }

  override fun hashCode(): Int = id?.hashCode() ?: 0
}
