package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull

@Entity
@Table(name = "licence_type")
data class LicenceType(
  @Id
  @param:NotNull
  val typeCode: String,

  val description: String,
)
