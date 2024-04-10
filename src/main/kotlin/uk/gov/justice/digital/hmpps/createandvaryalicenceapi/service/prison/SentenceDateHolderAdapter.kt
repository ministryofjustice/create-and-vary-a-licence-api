package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.SentenceDateHolder

object SentenceDateHolderAdapter {
  fun PrisonerSearchPrisoner.toSentenceDateHolder() = object : SentenceDateHolder {
    override val conditionalReleaseDate = this@toSentenceDateHolder.conditionalReleaseDate
    override val actualReleaseDate = confirmedReleaseDate
    override val licenceStartDate = this.actualReleaseDate ?: this.conditionalReleaseDate
  }
}
