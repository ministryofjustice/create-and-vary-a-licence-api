package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import jakarta.persistence.Column
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.MappedSuperclass
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.address.AddressSource

@MappedSuperclass
abstract class AbstractAddress(
  id: Long? = null,
  val reference: String,
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
) : AbstractIdEntity(idInternal = id)
