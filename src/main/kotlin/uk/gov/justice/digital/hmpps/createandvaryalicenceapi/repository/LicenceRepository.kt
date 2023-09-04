package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.time.LocalDate

@Repository
interface LicenceRepository : JpaRepository<Licence, Long>, JpaSpecificationExecutor<Licence> {
  fun findAllByNomsIdAndStatusCodeIn(nomsId: String, status: List<LicenceStatus>): List<Licence>
  fun findAllByCrnAndStatusCodeIn(crn: String, status: List<LicenceStatus>): List<Licence>
  fun findByStatusCodeAndProbationAreaCode(statusCode: LicenceStatus, probationAreaCode: String): List<Licence>
  fun findAllByVersionOfIdAndStatusCodeIn(versionOfId: Long, status: List<LicenceStatus>): List<Licence>

  @Query(
    """
    SELECT new uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.UnapprovedLicence( l.crn, l.forename, l.surname, com.firstName , com.lastName , com.email)
        FROM Licence l 
        JOIN l.submittedBy com
        WHERE (l.actualReleaseDate = CURRENT_DATE OR l.conditionalReleaseDate = CURRENT_DATE) 
        AND l.statusCode = 'SUBMITTED'
        AND EXISTS 
                (SELECT 1 FROM AuditEvent ae WHERE l.id = ae.licenceId AND ae.detail LIKE '%APPROVED%')
    """,
  )
  fun getEditedLicencesNotReApprovedByCrd(): List<UnapprovedLicence>

  @Query(
    """
    SELECT l
        FROM Licence l 
        WHERE (l.actualReleaseDate <= CURRENT_DATE OR l.conditionalReleaseDate <= CURRENT_DATE) 
        AND l.statusCode = 'APPROVED'
    """,
  )
  fun getApprovedLicencesOnOrPassedReleaseDate(): List<Licence>

  @Query(
    """
    SELECT l
        FROM Licence l
        WHERE l.prisonCode IN :prisonCodes 
        AND l.statusCode IN (
            uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.ACTIVE,
            uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.APPROVED
        )
        AND (COALESCE(l.actualReleaseDate, l.conditionalReleaseDate) > :releasedAfterDate)
        ORDER BY l.conditionalReleaseDate ASC
    """,
  )
  fun getRecentlyApprovedLicences(prisonCodes: List<String>, releasedAfterDate: LocalDate): List<Licence>

  @Query(
    """
      SELECT l
      FROM Licence l
      WHERE (l.licenceExpiryDate < CURRENT_DATE AND l.topupSupervisionExpiryDate >= CURRENT_DATE
      AND l.typeCode IN (uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType.AP_PSS))
      AND l.statusCode IN (
      uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.VARIATION_IN_PROGRESS,
      uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.VARIATION_SUBMITTED,
      uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.VARIATION_REJECTED,
      uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.VARIATION_APPROVED
  )
  """,
  )
  fun getAllVariedLicencesInPSSPeriod(): List<Licence>
}

@Schema(description = "Describes a prisoner's first and last name, their CRN if present and a COM's contact details for use in an email to COM")
data class UnapprovedLicence(
  @Schema(description = "The Crime Reference Number", example = "Z882661")
  val crn: String? = null,

  @Schema(description = "The prisoner's first name", example = "Jim")
  val forename: String? = null,

  @Schema(description = "The prisoner's last name", example = "Smith")
  val surname: String? = null,

  @Schema(description = "The COM's first name", example = "Joseph")
  val comFirstName: String? = null,

  @Schema(description = "The COM's last name", example = "Bloggs")
  val comLastName: String? = null,

  @Schema(description = "The COM's email address", example = "jbloggs@probation.gov.uk")
  val comEmail: String? = null,
)
