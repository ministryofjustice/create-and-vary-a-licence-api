package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

interface ModelVariation : Licence {
  val variationOf: Long?
}
