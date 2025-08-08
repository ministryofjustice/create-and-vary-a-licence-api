package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.Positive
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

@Entity
@Table(name = "hdc_curfew_times")
data class HdcCurfewTimes(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @param:Positive
  val id: Long? = null,

  @ManyToOne
  @JoinColumn(name = "licence_id", nullable = false)
  var licence: Licence,

  val curfewTimesSequence: Int? = null,
  @Enumerated(EnumType.STRING)
  val fromDay: DayOfWeek? = null,
  val fromTime: LocalTime? = null,
  @Enumerated(EnumType.STRING)
  val untilDay: DayOfWeek? = null,
  val untilTime: LocalTime? = null,
  val createdTimestamp: LocalDateTime? = null,
) {
  override fun toString(): String = "HdcCurfewTimes(" +
    "id=$id, " +
    "licence=${licence.id}, " +
    "curfewTimesSequence=$curfewTimesSequence, " +
    "fromDay=$fromDay, " +
    "fromTime=$fromTime, " +
    "untilDay=$untilDay, " +
    "untilTime=$untilTime" +
    "created_timestamp=$createdTimestamp" +
    ")"
}
