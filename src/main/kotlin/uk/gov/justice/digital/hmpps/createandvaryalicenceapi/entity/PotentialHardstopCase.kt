package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

enum class PotentialHardstopCaseStatus {
  PENDING,
  PROCESSING,
  PROCESSED,
}

@Entity
@Table(name = "potential_hardstop_cases")
class PotentialHardstopCase(
  @ManyToOne(fetch = FetchType.LAZY, cascade = [CascadeType.REFRESH])
  @JoinColumn(name = "licence_id", nullable = false)
  val licence: Licence,

  @Enumerated(EnumType.STRING)
  val status: PotentialHardstopCaseStatus = PotentialHardstopCaseStatus.PENDING,

  var dateCreated: LocalDateTime = LocalDateTime.now(),

  var dateLastUpdated: LocalDateTime = LocalDateTime.now(),
) : AbstractIdEntity()
