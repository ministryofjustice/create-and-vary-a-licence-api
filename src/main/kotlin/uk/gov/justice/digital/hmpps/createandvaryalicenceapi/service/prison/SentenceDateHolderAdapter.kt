package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.SentenceDateHolder
import java.time.LocalDate

object SentenceDateHolderAdapter {
  fun PrisonerSearchPrisoner.toSentenceDateHolder(licenceStartDate: LocalDate?) = object : SentenceDateHolder {
    override val conditionalReleaseDate = this@toSentenceDateHolder.conditionalReleaseDate
    override val sentenceStartDate = this@toSentenceDateHolder.sentenceStartDate
    override val actualReleaseDate = confirmedReleaseDate
    override val licenceStartDate = licenceStartDate
    override val homeDetentionCurfewActualDate = this@toSentenceDateHolder.homeDetentionCurfewActualDate
    override val postRecallReleaseDate = this@toSentenceDateHolder.postRecallReleaseDate
  }

  fun Licence.toSentenceDateHolder() = object : SentenceDateHolder {
    override val conditionalReleaseDate = this@toSentenceDateHolder.conditionalReleaseDate
    override val sentenceStartDate = this@toSentenceDateHolder.sentenceStartDate
    override val actualReleaseDate = this@toSentenceDateHolder.actualReleaseDate
    override val licenceStartDate = this@toSentenceDateHolder.licenceStartDate
    override val homeDetentionCurfewActualDate = this@toSentenceDateHolder.homeDetentionCurfewActualDate
    override val postRecallReleaseDate = this@toSentenceDateHolder.postRecallReleaseDate
  }
}
