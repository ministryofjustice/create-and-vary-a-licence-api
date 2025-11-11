package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.model

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus

interface PrisonNumberAndStatus {
  val prisonNumber: String
  val statusCode: LicenceStatus
}
