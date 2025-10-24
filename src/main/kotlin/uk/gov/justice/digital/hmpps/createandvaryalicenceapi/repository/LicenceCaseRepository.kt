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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.time.LocalDate

private const val LICENCE_CASE = "uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.model.LicenceCaCase"
private const val LICENCE_COM_CASE = "uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.model.LicenceComCase"
private const val LICENCE_APPROVER_CASE = "uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.model.LicenceApproverCase"

@Transactional(readOnly = true, propagation = Propagation.NOT_SUPPORTED)
@Repository
interface LicenceCaseRepository : JpaRepository<Licence, Long> {

  @Query(
    """
        SELECT new $LICENCE_CASE(
            l.kind,
            l.idInternal,
            l.versionOfId,
            l.statusCode,
            l.nomsId as prisonNumber,
            l.surname,
            l.forename,
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
        )
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
        SELECT new $LICENCE_COM_CASE(
          l.kind,
          l.idInternal,
          l.versionOfId,
          l.statusCode,
          l.nomsId as prisonNumber,
          l.surname,
          l.forename,
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
          l.crn,
          l.dateCreated,
          l.approvedByName,
          l.approvedDate
        )
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
        SELECT new $LICENCE_APPROVER_CASE(
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
          l.conditionalReleaseDate,
          l.actualReleaseDate,
          l.postRecallReleaseDate,
          l.approvedByName,
          l.approvedDate,
          l.prisonCode,
          l.prisonDescription,
          l.variationOfId
        )
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
    SELECT new $LICENCE_APPROVER_CASE(
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
      l.conditionalReleaseDate,
      l.actualReleaseDate,
      l.postRecallReleaseDate,
      l.approvedByName,
      l.approvedDate,
      l.prisonCode,
      l.prisonDescription,
      l.variationOfId
    )
    FROM Licence l
        LEFT JOIN l.responsibleCom com
        LEFT JOIN l.updatedBy updatedBy
        WHERE l.prisonCode IN :prisonCodes
        AND l.statusCode IN ('ACTIVE','APPROVED') 
        AND l.licenceStartDate > :releasedAfterDate
    ORDER BY l.approvedDate DESC
    """,
  )
  fun findRecentlyApprovedLicenceCasesAfter(
    prisonCodes: List<String>,
    releasedAfterDate: LocalDate,
  ): List<LicenceApproverCase>

  @Query(
    """
        SELECT new $LICENCE_APPROVER_CASE(
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
          l.conditionalReleaseDate,
          l.actualReleaseDate,
          l.postRecallReleaseDate,
          l.approvedByName,
          l.approvedDate,
          l.prisonCode,
          l.prisonDescription,
          l.variationOfId
        )
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
           COALESCE(
             CONCAT(com.first_name, ' ', com.last_name),
             CONCAT(ca.first_name, ' ', ca.last_name)
           ) AS full_name
    FROM licence l
    LEFT JOIN staff com ON com.id = l.submitted_by_com_id
    LEFT JOIN staff ca ON ca.id = l.submitted_by_ca_id
    WHERE l.id IN :licenceIds
  """,
    nativeQuery = true,
  )
  fun findSubmittedByNames(licenceIds: List<Long>): List<LicenceSubmitName>
}
