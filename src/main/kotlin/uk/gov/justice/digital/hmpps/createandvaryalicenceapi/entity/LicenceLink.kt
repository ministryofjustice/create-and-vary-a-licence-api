package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

enum class LicenceLinkType {
  CRD_REPLACEMENT_FOR_OPTED_OUT_LICENCE,
}

@Entity
class LicenceLink(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  val fromLicenceId: Long,

  val toLicenceId: Long,

  @Enumerated(EnumType.STRING)
  val linkType: LicenceLinkType,
)
