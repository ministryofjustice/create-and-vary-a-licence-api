package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import java.util.UUID

@Entity
@Table(name = "exclusion_zone_uploads")
class ExclusionZoneUpload(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @NotNull
  val id: Long = -1,

  @ManyToOne
  @JoinColumn(name = "additional_condition_id", nullable = false)
  val additionalCondition: AdditionalCondition,

  @ManyToOne
  @JoinColumn(name = "licence_id", nullable = false)
  val licence: Licence,

  val pdfId: UUID,
  val thumbnailId: UUID,
  val fullImageId: UUID,
)
