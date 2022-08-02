package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.OmuContact

interface OmuContactRepository : JpaRepository<OmuContact, Long> {
  fun findByPrisonCode(prisonCode: String): OmuContact?
}
