package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.Positive

@Entity
@Table(name = "hdc_curfew_address")
data class HdcCurfewAddress(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @field:Positive
  open val id: Long? = null,

  @OneToOne
  @JoinColumn(name = "licence_id", nullable = false)
  var licence: Licence,

  var addressLine1: String? = null,
  val addressLine2: String? = null,
  val townOrCity: String? = null,
  val county: String? = null,
  val postcode: String? = null,
) {
  override fun toString(): String = "HdcCurfewAddress(" +
    "id=$id, " +
    "licence=${licence.id}, " +
    "addressLine1=$addressLine1, " +
    "addressLine2=$addressLine2, " +
    "townOrCity=$townOrCity, " +
    "county=$county, " +
    "postcode=$postcode" +
    ")"
}
