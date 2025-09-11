package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class TimeServedConsiderations(
  val question: String = "",
)
