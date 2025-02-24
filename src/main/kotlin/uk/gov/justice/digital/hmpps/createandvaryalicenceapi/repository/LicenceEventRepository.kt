package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.LicenceEvent

@Repository
interface LicenceEventRepository :
  JpaRepository<LicenceEvent, Long>,
  JpaSpecificationExecutor<LicenceEvent>
