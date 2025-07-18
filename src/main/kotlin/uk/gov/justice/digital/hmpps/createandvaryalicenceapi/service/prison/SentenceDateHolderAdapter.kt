package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.SentenceDateHolder
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import java.time.LocalDate

object SentenceDateHolderAdapter {
  fun PrisonerSearchPrisoner.toSentenceDateHolder(licenceStartDate: LocalDate?, kind: LicenceKind) = object : SentenceDateHolder {
    override val conditionalReleaseDate = this@toSentenceDateHolder.conditionalReleaseDate
    override val actualReleaseDate = confirmedReleaseDate
    override val licenceStartDate = licenceStartDate
    override val homeDetentionCurfewActualDate = this@toSentenceDateHolder.homeDetentionCurfewActualDate
    override val postRecallReleaseDate = this@toSentenceDateHolder.postRecallReleaseDate
    override val kind = kind
  }

  fun SentenceDateHolder.reifySentenceDates() = SentenceDateHolderImpl(
    conditionalReleaseDate = this.conditionalReleaseDate,
    actualReleaseDate = this.actualReleaseDate,
    licenceStartDate = licenceStartDate,
    homeDetentionCurfewActualDate = this.homeDetentionCurfewActualDate,
    postRecallReleaseDate = this.postRecallReleaseDate,
    kind = this.kind,
  )

  data class SentenceDateHolderImpl(
    override val licenceStartDate: LocalDate?,
    override val conditionalReleaseDate: LocalDate?,
    override val actualReleaseDate: LocalDate?,
    override val homeDetentionCurfewActualDate: LocalDate?,
    override val postRecallReleaseDate: LocalDate?,
    override val kind: LicenceKind,
  ) : SentenceDateHolder
}
