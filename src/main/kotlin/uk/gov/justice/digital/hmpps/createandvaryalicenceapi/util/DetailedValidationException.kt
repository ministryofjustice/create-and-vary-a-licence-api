package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util

class DetailedValidationException(
  val title: String,
  message: String? = title,
  val errors: Iterable<Pair<String, String>>,
) : Exception(message)
