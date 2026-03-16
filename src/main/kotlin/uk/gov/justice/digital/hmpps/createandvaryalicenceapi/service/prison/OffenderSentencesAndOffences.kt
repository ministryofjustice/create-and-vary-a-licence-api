package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison

import java.time.LocalDate

data class OffenderSentenceAndOffences(
  val bookingId: Long? = null,
  val sentenceSequence: Int? = null,
  val consecutiveToSequence: Int? = null,
  val lineSequence: Long? = null,
  val caseSequence: Int? = null,
  val caseReference: String? = null,
  val courtId: String? = null,
  val courtDescription: String? = null,
  val courtTypeCode: String? = null,
  val sentenceStatus: String? = null,
  val sentenceCategory: String? = null,
  val sentenceCalculationType: String? = null,
  val sentenceTypeDescription: String? = null,
  val sentenceDate: LocalDate? = null,
  val sentenceStartDate: LocalDate? = null,
  val sentenceEndDate: LocalDate? = null,
  val terms: MutableList<OffenderSentenceTerm?>? = null,
  val offences: MutableList<OffenderOffence?>? = null,
  val fineAmount: Double? = null,
  val revocationDates: MutableList<LocalDate?>? = null,
)

data class OffenderSentenceTerm(
  val years: Int? = null,
  val months: Int? = null,
  val weeks: Int? = null,
  val days: Int? = null,
  val code: String? = null,
)

data class OffenderOffence(
  val offenderChargeId: Long? = null,
  val offenceStartDate: LocalDate? = null,
  val offenceEndDate: LocalDate? = null,
  val offenceStatute: String? = null,
  val offenceCode: String? = null,
  val offenceDescription: String? = null,
  val indicators: MutableList<String?>? = null,
)
