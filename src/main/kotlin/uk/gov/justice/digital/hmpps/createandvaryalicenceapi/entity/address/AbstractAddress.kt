package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.address

import jakarta.persistence.Column
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.MappedSuperclass
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AbstractIdEntity
import java.time.LocalDateTime

@MappedSuperclass
abstract class AbstractAddress(
  id: Long? = null,

  @Column(nullable = false, unique = true)
  open val reference: String,

  @Column
  open var uprn: String? = null,

  @Column(name = "first_line", nullable = false)
  open var firstLine: String,

  @Column(name = "second_line")
  open var secondLine: String? = null,

  @Column(name = "town_or_city", nullable = false)
  open var townOrCity: String,

  @Column
  open var county: String? = null,

  @Column(nullable = false)
  open var postcode: String,

  @Enumerated(EnumType.STRING)
  @Column(name = "source", nullable = false)
  open var source: AddressSource,
  @Column(name = "created_timestamp", nullable = false)
  open val createdTimestamp: LocalDateTime = LocalDateTime.now(),
  @Column(name = "last_updated_timestamp", nullable = false)
  open var lastUpdatedTimestamp: LocalDateTime = LocalDateTime.now(),
) : AbstractIdEntity(id) {

  fun isSame(other: AbstractAddress): Boolean = firstLine == other.firstLine &&
    secondLine.orEmpty() == other.secondLine.orEmpty() &&
    townOrCity == other.townOrCity &&
    county.orEmpty() == other.county.orEmpty() &&
    postcode == other.postcode

  override fun toString(): String = listOf(
    reference,
    uprn.orEmpty(),
    firstLine,
    secondLine.orEmpty(),
    townOrCity,
    county.orEmpty(),
    postcode,
    source.name,
  ).joinToString(",")
}
