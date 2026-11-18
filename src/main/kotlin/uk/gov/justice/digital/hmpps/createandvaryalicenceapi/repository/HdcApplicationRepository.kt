package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcApplication

interface HdcApplicationRepository : JpaRepository<HdcApplication, Long> {
  fun findByLicenceId(licenceId: Long): HdcApplication?
}
