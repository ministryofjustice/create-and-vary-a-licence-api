package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.hasChanged
import java.time.LocalDate
import java.time.format.DateTimeFormatter

val dateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("dd LLLL yyyy")

data class DateChanges(
  private val dates: List<DateChange>,
  val isMaterial: Boolean,
) : Iterable<DateChange> {
  fun toChanges(kind: LicenceKind) = dates
    .filter { it.changed && (kind.isHdc() || !it.type.hdcOnly) }
    .associate {
      it.type.name to mapOf("from" to it.oldDate?.toString(), "to" to it.newDate?.toString())
    }

  override fun iterator() = dates.iterator()
}

data class DateChange(
  val type: LicenceDateType,
  val newDate: LocalDate?,
  val oldDate: LocalDate?,
  val changed: Boolean = oldDate.hasChanged(newDate),
) {
  fun toDescription() = if (newDate == null) {
    "${type.description} has been removed"
  } else {
    "${type.description} has changed to ${newDate.format(dateFormat)}"
  }

  fun denotesMaterialChange(kind: LicenceKind) = changed && type.denotesMaterialChange && (kind.isHdc() || !type.hdcOnly)

  fun notifyOfChange(kind: LicenceKind) = changed && type.notifyOnChange && (kind.isHdc() || !type.hdcOnly)
}
