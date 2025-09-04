package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class RequiresCom(
  val question: String = "",
  val called: String = "",
)
