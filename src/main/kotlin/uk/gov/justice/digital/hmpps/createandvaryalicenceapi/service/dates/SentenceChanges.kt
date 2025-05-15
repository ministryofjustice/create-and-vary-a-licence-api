package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import java.time.LocalDate

data class SentenceChanges(
  val dates: List<DateChange>,
  val isMaterial: Boolean,
) {
  fun toChanges(kind: LicenceKind) = dates
    .filter { (kind.isHdc() || !it.type.hdcOnly) && it.changed }
    .associate {
      it.type.name to mapOf("from" to it.oldDate?.toString(), "to" to it.newDate?.toString())
    }
}

data class DateChange(
  val type: LicenceDateType,
  val changed: Boolean,
  val newDate: LocalDate?,
  val oldDate: LocalDate?,
) {
  fun toDescription() = "${type.description} has changed to ${newDate?.format(dateFormat)}"
}
