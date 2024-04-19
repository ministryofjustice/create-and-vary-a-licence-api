package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HardStopLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.time.LocalDate

@Repository
interface LicenceRepository : JpaRepository<Licence, Long>, JpaSpecificationExecutor<Licence> {
  fun findAllByNomsId(nomsId: String): List<Licence>
  fun findAllByNomsIdAndStatusCodeIn(nomsId: String, status: List<LicenceStatus>): List<Licence>
  fun findAllByCrnAndStatusCodeIn(crn: String, status: List<LicenceStatus>): List<Licence>
  fun findByStatusCodeAndProbationAreaCode(statusCode: LicenceStatus, probationAreaCode: String): List<Licence>
  fun findAllByVersionOfIdInAndStatusCodeIn(versionOfId: List<Long>, status: List<LicenceStatus>): List<CrdLicence>
  fun findByBookingIdAndStatusCodeOrderByDateCreatedDesc(bookingId: Long, status: LicenceStatus): CrdLicence?
  fun findAllByBookingIdAndStatusCodeInAndKindIn(
    bookingId: Long,
    status: List<LicenceStatus>,
    kind: List<LicenceKind>,
  ): List<Licence>

  @Query(
    """
    SELECT new uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.UnapprovedLicence( l.crn, l.forename, l.surname, com.firstName , com.lastName , com.email)
        FROM CrdLicence l 
        JOIN l.submittedBy com
        WHERE (l.actualReleaseDate = CURRENT_DATE OR l.conditionalReleaseDate = CURRENT_DATE) 
        AND l.statusCode = 'SUBMITTED'
        AND (
            (EXISTS (SELECT 1 FROM AuditEvent ae WHERE l.id = ae.licenceId AND ae.detail LIKE '%APPROVED%')) 
         OR (l.versionOfId IS NOT NULL) 
        )
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
      FROM VariationLicence l
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

  @Query(
    """
      SELECT *
        FROM licence
        WHERE kind = 'CRD'
        AND status_code = 'IN_PROGRESS' 
        AND conditional_release_date - (INTERVAL '14' DAY) <= CURRENT_DATE;
  """,
    nativeQuery = true,
  )
  fun getAllLicencesToTimeOut(): List<CrdLicence>

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
        WHERE l.statusCode  IN (
            uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.IN_PROGRESS,
            uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.SUBMITTED
        )
        AND COALESCE(l.actualReleaseDate, l.conditionalReleaseDate) < CURRENT_DATE
    """,
  )
  fun getDraftLicencesPassedReleaseDate(): List<Licence>

  @Query(
    """
    SELECT COUNT(*)
        FROM Licence l
        LEFT JOIN Licence l2 ON l.id = l2.variationOfId
        WHERE l.kind IN (
            uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.HARD_STOP,
            'TIME_SERVED'
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
            uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.HARD_STOP,
            'TIME_SERVED'
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
      SELECT *
        FROM licence
        WHERE kind = 'HARD_STOP'
        AND review_date IS NULL
        AND DATE(licence_activated_date) + INTERVAL '5' DAY = CURRENT_DATE;
    """,
    nativeQuery = true,
  )
  fun getHardStopLicencesNeedingReview(): List<HardStopLicence>
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
