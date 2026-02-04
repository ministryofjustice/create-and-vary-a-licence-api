package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.PotentialHardstopCase

interface PotentialHardstopCaseRepository : JpaRepository<PotentialHardstopCase, Long>
