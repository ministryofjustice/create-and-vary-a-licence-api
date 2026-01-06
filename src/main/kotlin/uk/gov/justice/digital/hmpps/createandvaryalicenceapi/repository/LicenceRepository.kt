package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.TeamCountsDto
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.model.EditedLicenceNotReApproved
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.model.PrisonNumberAndStatus
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

  fun findAllByBookingIdAndStatusCodeInAndKindIn(
    bookingId: Long,
    status: List<LicenceStatus>,
    kind: List<LicenceKind>,
  ): List<Licence>

  @Query(
    """
  SELECT l FROM Licence l
    WHERE l.kind IN ('CRD', 'PRRD')  
      AND l.statusCode = 'IN_PROGRESS'
      AND l.licenceStartDate <= :cutoffDate
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
    SELECT l.nomsId AS prisonNumber, l.statusCode AS statusCode
    FROM Licence l
    WHERE l.nomsId IN :nomsIds
    """,
  )
  fun findStatesByPrisonNumbers(
    nomsIds: List<String>,
  ): List<PrisonNumberAndStatus>

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
        AND l.version_of_id IS NOT NULL
        ORDER BY l.id
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
            uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.HARD_STOP,
            uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.TIME_SERVED
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
    SELECT l.probationTeamCode, COUNT(*) as count
        FROM Licence l
        LEFT JOIN Licence l2 ON l.id = l2.variationOfId
        WHERE l.kind IN (
            uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.HARD_STOP,
            uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.TIME_SERVED
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

  @Modifying(clearAutomatically = true)
  @Query(
    """
    UPDATE Licence l
      SET l.kind = :newKind, 
          l.eligibleKind = :newEligibleKind
      WHERE l.id = :id
    """,
  )
  fun updateLicenceKinds(id: Long, newKind: LicenceKind, newEligibleKind: LicenceKind?)
}
