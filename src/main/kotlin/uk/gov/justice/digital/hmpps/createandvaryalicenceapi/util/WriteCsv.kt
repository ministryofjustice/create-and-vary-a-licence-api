package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.NotifyAttentionNeededLicence

/**
 * Returns csv format
 */
fun writeCsv(licences: List<NotifyAttentionNeededLicence>): String {
  val csv = StringBuilder()
  csv.append("Noms ID,Prison Name,Noms Legal Status,ARD,CRD,Licence Start Date\r\n")
  licences.forEach {
    csv.append("${it.nomsId},${it.prisonName},${it.legalStatus},${it.actualReleaseDate},${it.conditionalReleaseDate},${it.licenceStartDate}\r\n")
  }
  return csv.toString()
}
