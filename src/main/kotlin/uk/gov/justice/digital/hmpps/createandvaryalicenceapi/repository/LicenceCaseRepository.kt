package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.model.LicenceCaCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.model.LicenceComCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus

private const val LICENCE_CASE = "uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.model.LicenceCaCase"
private const val LICENCE_COM_CASE = "uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.model.LicenceComCase"

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
}
