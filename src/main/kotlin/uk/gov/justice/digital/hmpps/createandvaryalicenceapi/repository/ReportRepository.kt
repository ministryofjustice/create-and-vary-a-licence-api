package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.model.UpcomingReleasesWithMonitoringConditions
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.ELECTRONIC_TAG_COND_CODE_14A
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.ELECTRONIC_TAG_COND_CODE_14B
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.ELECTRONIC_TAG_COND_CODE_14C
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.ELECTRONIC_TAG_COND_CODE_14D
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.ELECTRONIC_TAG_COND_CODE_14E

@Transactional(readOnly = true, propagation = Propagation.NOT_SUPPORTED)
@Repository
interface ReportRepository : JpaRepository<Licence, Long> {
  @Query(
    """
WITH condition_lookup (condition_code, condition_value) AS (
  VALUES
    ('${ELECTRONIC_TAG_COND_CODE_14A}', '14a'),
    ('${ELECTRONIC_TAG_COND_CODE_14B}', '14b'),
    ('${ELECTRONIC_TAG_COND_CODE_14C}', '14c'),
    ('${ELECTRONIC_TAG_COND_CODE_14D}', '14d'),
    ('${ELECTRONIC_TAG_COND_CODE_14E}', '14e')
),
ranked AS (
  SELECT
    l.noms_id AS nomis_number,
    l.crn,
    l.status_code,
    l.licence_start_date,
    ROW_NUMBER() OVER (
      PARTITION BY l.noms_id, l.crn
      ORDER BY
        CASE WHEN l.status_code = 'SUBMITTED' THEN 0 ELSE 1 END,
        l.id DESC
    ) AS rn
  FROM licence AS l
  LEFT JOIN additional_condition AS ac
    ON ac.licence_id = l.id
  INNER JOIN condition_lookup AS cl
    ON cl.condition_code = ac.condition_code
  WHERE l.status_code IN ('SUBMITTED', 'APPROVED')
)
SELECT nomis_number "prisonNumber", crn, status_code "status", licence_start_date "licenceStartDate" 
FROM ranked
WHERE rn = 1
ORDER BY nomis_number
  """,
    nativeQuery = true,
  )
  fun getUpcomingReleasesWithMonitoringConditions(): List<UpcomingReleasesWithMonitoringConditions>
}
