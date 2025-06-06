package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull

@Entity
@Table(name = "licence_status")
data class LicenceStatus(
  @Id
  @NotNull
  val statusCode: String,

  val description: String,
)
