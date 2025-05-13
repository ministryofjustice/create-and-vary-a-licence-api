package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.SentenceDateHolder
import java.time.LocalDate

object SentenceDateHolderAdapter {
  fun PrisonerSearchPrisoner.toSentenceDateHolder(licenceStartDate: LocalDate?) = object : SentenceDateHolder {
    override val conditionalReleaseDate = this@toSentenceDateHolder.conditionalReleaseDate
    override val actualReleaseDate = confirmedReleaseDate
    override val licenceStartDate = licenceStartDate
    override val homeDetentionCurfewActualDate = this@toSentenceDateHolder.homeDetentionCurfewActualDate
  }

  fun SentenceDateHolder.reifySentenceDates() = SentenceDateHolderImpl(
    conditionalReleaseDate = this.conditionalReleaseDate,
    actualReleaseDate = this.actualReleaseDate,
    licenceStartDate = licenceStartDate,
    homeDetentionCurfewActualDate = this.homeDetentionCurfewActualDate,
  )

  data class SentenceDateHolderImpl(
    override val licenceStartDate: LocalDate?,
    override val conditionalReleaseDate: LocalDate?,
    override val actualReleaseDate: LocalDate?,
    override val homeDetentionCurfewActualDate: LocalDate?,
  ) : SentenceDateHolder
}
