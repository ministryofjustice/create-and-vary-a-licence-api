package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus

@Repository
interface LicenceRepository : JpaRepository<Licence, Long>, JpaSpecificationExecutor<Licence> {
  fun findAllByNomsIdAndStatusCodeIn(nomsId: String, status: List<LicenceStatus>): List<Licence>
  fun findAllByCrnAndStatusCodeIn(crn: String, status: List<LicenceStatus>): List<Licence>
  fun findByStatusCodeAndProbationAreaCode(statusCode: LicenceStatus, probationAreaCode: String): List<Licence>

  @Query(
    """
    SELECT new uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.UnapprovedLicence( l.crn, l.forename, l.surname, com.firstName , com.lastName , com.email)
        FROM Licence l 
        JOIN l.submittedBy com
        WHERE (l.actualReleaseDate = CURRENT_DATE OR l.conditionalReleaseDate = CURRENT_DATE) 
        AND l.statusCode = 'SUBMITTED'
        AND EXISTS 
                (SELECT 1 FROM AuditEvent ae WHERE l.id = ae.licenceId AND ae.detail LIKE '%APPROVED%')
    """
  )
  fun getLicencesNotApprovedByCRD(): List<UnapprovedLicence>
}

data class UnapprovedLicence(
  val crn: String? = null,
  val forename: String? = null,
  val surname: String? = null,
  val comFirstName: String? = null,
  val comLastName: String? = null,
  val comEmail: String? = null
)
