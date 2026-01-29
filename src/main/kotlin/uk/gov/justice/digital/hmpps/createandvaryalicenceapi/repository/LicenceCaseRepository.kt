package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.model.LicenceApproverCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.model.LicenceCaCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.model.LicenceComCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.model.LicenceSubmitName
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.model.LicenceVaryApproverCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.time.LocalDateTime

@Transactional(readOnly = true, propagation = Propagation.NOT_SUPPORTED)
@Repository
interface LicenceCaseRepository : JpaRepository<Licence, Long> {

  @Query(
    """
        SELECT 
            l.kind,
            l.idInternal,
            l.versionOfId,
            l.statusCode,
            l.nomsId as prisonNumber,
            l.surname,
            l.forename,
            l.sentenceStartDate,
            l.conditionalReleaseDate,
            l.actualReleaseDate,
            l.licenceStartDate,
            l.postRecallReleaseDate,
            l.homeDetentionCurfewActualDate,
            updatedBy.firstName,
            updatedBy.lastName,
            com.username,
            l.prisonCode,
            l.prisonDescription
        FROM Licence l
            LEFT JOIN l.responsibleCom com
            LEFT JOIN l.updatedBy updatedBy
        WHERE l.statusCode IN :statusCodes AND l.prisonCode IN :prisonCodes
        ORDER BY l.licenceStartDate ASC
    """,
  )
  fun findLicenceCases(
    statusCodes: List<LicenceStatus>,
    prisonCodes: List<String>,
  ): List<LicenceCaCase>

  @Query(
    """
        SELECT 
          l.kind,
          l.idInternal,
          l.versionOfId,
          l.statusCode,
          l.nomsId as prisonNumber,
          l.prisonCode,
          l.surname,
          l.forename,
          l.sentenceStartDate,
          l.conditionalReleaseDate,
          l.actualReleaseDate,
          l.licenceStartDate,
          l.postRecallReleaseDate,
          l.homeDetentionCurfewActualDate,
          updatedBy.firstName,
          updatedBy.lastName,
          com.username,
          l.typeCode,
          l.reviewDate,
          l.crn
        FROM Licence l
            LEFT JOIN l.responsibleCom com
            LEFT JOIN l.updatedBy updatedBy
        WHERE l.statusCode IN :statusCodes AND l.crn IN :crns
        ORDER BY l.licenceStartDate ASC
    """,
  )
  fun findLicenceCasesForCom(
    statusCodes: List<LicenceStatus>,
    crns: List<String>,
  ): List<LicenceComCase>

  @Query(
    """
        SELECT 
          l.licenceStartDate,
          l.kind,
          l.idInternal,
          l.versionOfId,
          l.statusCode,
          l.nomsId as prisonNumber,
          l.surname,
          l.forename,
          updatedBy.firstName,
          updatedBy.lastName,
          com.username,
          l.sentenceStartDate,
          l.conditionalReleaseDate,
          l.actualReleaseDate,
          l.postRecallReleaseDate,
          l.approvedByName,
          l.approvedDate,
          l.prisonCode,
          l.prisonDescription,
          l.variationOfId
        FROM Licence l
          LEFT JOIN l.responsibleCom com
          LEFT JOIN l.updatedBy updatedBy
        WHERE l.statusCode = 'SUBMITTED'
          AND l.prisonCode IN :prisons
        ORDER BY l.licenceStartDate ASC
    """,
  )
  fun findLicenceCasesReadyForApproval(prisons: List<String>): List<LicenceApproverCase>

  @Query(
    """
    SELECT
      l.licenceStartDate,
      l.kind,
      l.idInternal,
      l.versionOfId,
      l.statusCode,
      l.nomsId as prisonNumber,
      l.surname,
      l.forename,
      updatedBy.firstName,
      updatedBy.lastName,
      com.username,
      l.sentenceStartDate,
      l.conditionalReleaseDate,
      l.actualReleaseDate,
      l.postRecallReleaseDate,
      l.approvedByName,
      l.approvedDate,
      l.prisonCode,
      l.prisonDescription,
      l.variationOfId
    FROM Licence l
        LEFT JOIN l.responsibleCom com
        LEFT JOIN l.updatedBy updatedBy
        WHERE l.prisonCode IN :prisonCodes
        AND l.statusCode IN ('ACTIVE','APPROVED') 
        AND (l.licenceActivatedDate IS NULL or l.licenceActivatedDate > :activatedAfterDate)
    ORDER BY l.approvedDate DESC
    """,
  )
  fun findRecentlyApprovedLicenceCasesAfter(
    prisonCodes: List<String>,
    activatedAfterDate: LocalDateTime,
  ): List<LicenceApproverCase>

  @Query(
    """
        SELECT 
          l.licenceStartDate,
          l.kind,
          l.idInternal,
          l.versionOfId,
          l.statusCode,
          l.nomsId as prisonNumber,
          l.surname,
          l.forename,
          updatedBy.firstName,
          updatedBy.lastName,
          com.username,
          l.sentenceStartDate,
          l.conditionalReleaseDate,
          l.actualReleaseDate,
          l.postRecallReleaseDate,
          l.approvedByName,
          l.approvedDate,
          l.prisonCode,
          l.prisonDescription,
          l.variationOfId
        FROM Licence l
          LEFT JOIN l.responsibleCom com
          LEFT JOIN l.updatedBy updatedBy
        WHERE l.idInternal = :id
        ORDER BY l.idInternal
    """,
  )
  fun findLicenceApproverCase(id: Long): LicenceApproverCase

  @Query(
    value = """
    SELECT l.id AS licence_id,
           s.first_name,
           s.last_name
    FROM licence l
    LEFT JOIN staff s ON s.id = l.submitted_by_com_id OR s.id = l.submitted_by_ca_id
    WHERE l.id IN :licenceIds
    """,
    nativeQuery = true,
  )
  fun findSubmittedByNames(licenceIds: List<Long>): List<LicenceSubmitName>

  @Query(
    """
        SELECT 
          l.idInternal,
          l.crn,
          l.licenceStartDate,
          l.nomsId as prisonNumber,
          com.username,
          l.typeCode,
          l.dateCreated
        FROM Licence l
          LEFT JOIN l.responsibleCom com
        WHERE l.probationPduCode in :probationPduCodes and l.statusCode = 'VARIATION_SUBMITTED'
        ORDER BY l.idInternal
    """,
  )
  fun findSubmittedVariationsByPduCodes(probationPduCodes: List<String>?): List<LicenceVaryApproverCase>

  @Query(
    """
        SELECT 
          l.idInternal,
          l.crn,
          l.licenceStartDate,
          l.nomsId as prisonNumber,
          com.username,
          l.typeCode,
          l.dateCreated
        FROM Licence l
          LEFT JOIN l.responsibleCom com
        WHERE l.probationAreaCode = :probationAreaCode and l.statusCode = 'VARIATION_SUBMITTED' 
           ORDER BY l.idInternal
    """,
  )
  fun findSubmittedVariationsByRegion(probationAreaCode: String): List<LicenceVaryApproverCase>
}
