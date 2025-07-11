package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.time.LocalDate

@Repository
interface LicenceRepository :
  JpaRepository<Licence, Long>,
  JpaSpecificationExecutor<Licence> {
  fun findAllByNomsId(nomsId: String): List<Licence>
  fun findAllByNomsIdAndStatusCodeIn(nomsId: String, status: List<LicenceStatus>): List<Licence>
  fun findAllByCrnAndStatusCodeIn(crn: String, status: List<LicenceStatus>): List<Licence>
  fun findByStatusCodeAndProbationAreaCode(statusCode: LicenceStatus, probationAreaCode: String): List<Licence>

  fun findAllByBookingIdAndStatusCodeInAndKindIn(
    bookingId: Long,
    status: List<LicenceStatus>,
    kind: List<LicenceKind>,
  ): List<Licence>

  @Query(
    """
  SELECT l FROM Licence l
    WHERE l.kind IN ('CRD')  
      AND l.statusCode = 'IN_PROGRESS'
      AND l.conditionalReleaseDate <= :cutoffDate
""",
  )
  fun getAllLicencesToTimeOut(
    cutoffDate: LocalDate = LocalDate.now().plusDays(14),
  ): List<Licence>

  fun findAllByCrnInAndStatusCodeIn(crn: List<String>, status: List<LicenceStatus>): List<Licence>

  @Query(
    """
    SELECT l.bookingId
        FROM Licence l
        WHERE l.nomsId IN :nomsIds
        AND l.statusCode IN :status
    """,
  )
  fun findBookingIdsForLicencesInState(
    nomsIds: List<String>,
    status: List<LicenceStatus>,
  ): Set<Long>

  @Query(
    """
    SELECT l
        FROM Licence l
        WHERE l.kind IN ('HDC','CRD','PRRD')
        AND l.versionOfId IN :versionOfId
        AND l.statusCode IN :status
    """,
  )
  fun findAllByVersionOfIdInAndStatusCodeIn(versionOfId: List<Long>, status: List<LicenceStatus>): List<Licence>

  @Query(
    """
    SELECT l.crn as crn, l.forename as forename, l.surname as surname, s.first_name as comFirstName, s.last_name as comLastName, s.email as comEmail
        FROM licence l
        JOIN staff s ON l.submitted_by_com_id = s.id
        WHERE l.kind IN ('CRD', 'HDC', 'PRRD')
        AND l.licence_start_date = CURRENT_DATE
        AND l.status_code = 'SUBMITTED'
        AND (
            EXISTS (SELECT 1 FROM audit_event ae WHERE l.id = ae.licence_id AND ae.detail LIKE '%APPROVED%')
            OR l.version_of_id IS NOT NULL
        ) ORDER BY l.id
    """,
    nativeQuery = true,
  )
  fun getEditedLicencesNotReApprovedByLsd(): List<EditedLicenceNotReApproved>

  @Query(
    """
    SELECT l
        FROM Licence l 
        WHERE l.licenceStartDate <= CURRENT_DATE
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
        AND l.licenceStartDate > :releasedAfterDate
        ORDER BY l.licenceStartDate ASC
    """,
  )
  fun getRecentlyApprovedLicences(prisonCodes: List<String>, releasedAfterDate: LocalDate): List<Licence>

  @Query(
    """
      SELECT l
      FROM Licence l
      WHERE l.kind IN (
      uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.VARIATION,
      uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.HDC_VARIATION)
      AND (l.licenceExpiryDate < CURRENT_DATE AND l.topupSupervisionExpiryDate >= CURRENT_DATE
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

  @Query(
    """
    SELECT l
        FROM Licence l
        WHERE l.statusCode != uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.INACTIVE
        AND COALESCE(l.topupSupervisionExpiryDate, l.licenceExpiryDate) < CURRENT_DATE
    """,
  )
  fun getLicencesPassedExpiryDate(): List<Licence>

  @Query(
    """
    SELECT l
        FROM Licence l
        WHERE l.kind != (
            uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.HDC
        )
        AND l.statusCode  IN (
            uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.IN_PROGRESS,
            uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.SUBMITTED,
            uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.TIMED_OUT
        )
        AND l.licenceStartDate < CURRENT_DATE
    """,
  )
  fun getDraftLicencesPassedReleaseDate(): List<Licence>

  @Query(
    """
    SELECT COUNT(*)
        FROM Licence l
        LEFT JOIN Licence l2 ON l.id = l2.variationOfId
        WHERE l.kind IN (
            uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.HARD_STOP
        )
        AND l.statusCode = 'ACTIVE'
        AND l.reviewDate IS NULL
        AND l.responsibleCom = :com
        AND l2.variationOfId IS NULL
    """,
  )
  fun getLicenceReviewCountForCom(com: CommunityOffenderManager?): Long

  @Query(
    """
    SELECT new uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.TeamCountsDto(l.probationTeamCode, COUNT(*))
        FROM Licence l
        LEFT JOIN Licence l2 ON l.id = l2.variationOfId
        WHERE l.kind IN (
            uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.HARD_STOP
        )
        AND l.statusCode = 'ACTIVE'
        AND l.reviewDate IS NULL
        AND l.probationTeamCode IN ( :teamCodes )
        AND l2.variationOfId IS NULL
        GROUP BY l.probationTeamCode
    """,
  )
  fun getLicenceReviewCountForTeams(teamCodes: List<String>): List<TeamCountsDto>

  @Query(
    """
      SELECT l
      FROM Licence l
      WHERE l.statusCode = 'SUBMITTED'
      AND l.prisonCode IN ( :prisonCodes )
  """,
  )
  fun getLicencesReadyForApproval(prisonCodes: List<String>): List<Licence>

  @Query(
    """
      SELECT l
        FROM Licence l
        WHERE (l.id = :licenceId AND l.statusCode = uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.ACTIVE)
        OR l.variationOfId = :licenceId
        AND l.statusCode != uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.INACTIVE
    """,
  )
  fun findLicenceAndVariations(licenceId: Long): List<Licence>

  @Query(
    """
      SELECT l
      FROM Licence l
      WHERE l.statusCode in (uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.IN_PROGRESS, uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.SUBMITTED, uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.APPROVED)
      AND l.id > :lastUpdatedLicenceId
      ORDER BY l.id ASC
      LIMIT :numberOfLicences
    """,
  )
  fun findLicencesToBatchUpdateLsd(numberOfLicences: Long, lastUpdatedLicenceId: Long?): List<Licence>
}

interface EditedLicenceNotReApproved {
  fun getCrn(): String?
  fun getForename(): String?
  fun getSurname(): String?
  fun getComFirstName(): String?
  fun getComLastName(): String?
  fun getComEmail(): String?
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

@Schema(description = "Describes a team and the respective count of their cases that need review")
data class TeamCountsDto(
  @Schema(description = "The team code", example = "ABC123")
  val teamCode: String,

  @Schema(description = "A count of cases that need to be reviewed", example = "42")
  val count: Long,
)
