package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.NotifyAttentionNeededLicence

/**
 * Returns csv format
 */
fun writeCsv(licences: List<NotifyAttentionNeededLicence>): StringBuilder {
  val csv = StringBuilder()
  csv.append("Noms ID,Prison Name,Noms LegalStatus,ARD,CRD\r\n")
  licences.forEach {
    csv.append("${it.nomsId},${it.prisonName},${it.legalStatus},${it.actualReleaseDate},${it.conditionalReleaseDate}\r\n")
  }
  return csv
}
