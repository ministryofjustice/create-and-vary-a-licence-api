package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateSentenceDatesRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus

data class SentenceChanges(
  val lsdChanged: Boolean,
  val ledChanged: Boolean,
  val sedChanged: Boolean,
  val tussdChanged: Boolean,
  val tusedChanged: Boolean,
  val crdChanged: Boolean,
  val ardChanged: Boolean,
  val isMaterial: Boolean
)

fun getSentenceChanges(sentenceDatesRequest: UpdateSentenceDatesRequest, licenceEntity: Licence): SentenceChanges {
  val lsdChanged = nullableDatesDiffer(sentenceDatesRequest.licenceStartDate, licenceEntity.licenceStartDate)
  val ledChanged = nullableDatesDiffer(sentenceDatesRequest.licenceExpiryDate, licenceEntity.licenceExpiryDate)
  val sedChanged = nullableDatesDiffer(sentenceDatesRequest.sentenceEndDate, licenceEntity.sentenceEndDate)
  val tussdChanged =
    nullableDatesDiffer(sentenceDatesRequest.topupSupervisionStartDate, licenceEntity.topupSupervisionStartDate)
  val tusedChanged =
    nullableDatesDiffer(sentenceDatesRequest.topupSupervisionExpiryDate, licenceEntity.topupSupervisionExpiryDate)
  val crdChanged =
    nullableDatesDiffer(sentenceDatesRequest.conditionalReleaseDate, licenceEntity.conditionalReleaseDate)
  val ardChanged =
    nullableDatesDiffer(sentenceDatesRequest.actualReleaseDate, licenceEntity.actualReleaseDate)

  val isMaterial =
    (
      lsdChanged || ledChanged || tussdChanged || tusedChanged ||
        (sedChanged && licenceEntity.statusCode == LicenceStatus.APPROVED)
      )

  return SentenceChanges(lsdChanged, ledChanged, sedChanged, tussdChanged, tusedChanged, crdChanged, ardChanged, isMaterial)
}
