package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util

class DetailedValidationException(
  val title: String,
  message: String? = title,
  val errors: Map<String, Any>,
) : Exception(message)
