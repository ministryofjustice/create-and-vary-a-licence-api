package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.address

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType.IDENTITY
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

enum class Country {
  ENGLAND,
  SCOTLAND,
  WALES,
  NORTHERN_IRELAND,
}

enum class AddressSource {
  MANUAL,
  OS_PLACES,
}

@Entity
@Table(name = "address")
data class Address(
  @Id
  @GeneratedValue(strategy = IDENTITY)
  val id: Long? = null,

  @Column(nullable = false, unique = true)
  val reference: String,

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
  @Column(name = "country", nullable = true)
  val country: Country? = null,

  @Enumerated(EnumType.STRING)
  @Column(name = "source", nullable = false)
  val source: AddressSource,

  val createdTimestamp: LocalDateTime = LocalDateTime.now(),

  val lastUpdatedTimestamp: LocalDateTime = LocalDateTime.now(),
) {
  override fun toString(): String = listOf(
    reference,
    firstLine,
    secondLine.orEmpty(),
    townOrCity,
    county.orEmpty(),
    postcode,
    country?.name.orEmpty(),
    source.name,
  ).joinToString(",")
}
