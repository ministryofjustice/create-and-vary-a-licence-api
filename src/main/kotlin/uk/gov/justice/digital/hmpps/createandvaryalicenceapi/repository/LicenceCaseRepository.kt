package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.time.LocalDate

data class LicenceCase(
  val kind: LicenceKind,
  val licenceId: Long,
  val versionOfId: Long?,
  val licenceStatus: LicenceStatus,
  val prisonNumber: String,
  val surname: String?,
  val forename: String?,
  val prisonCode: String?,
  val prisonDescription: String?,
  val conditionalReleaseDate: LocalDate?,
  val actualReleaseDate: LocalDate?,
  val licenceStartDate: LocalDate?,
  val postRecallReleaseDate: LocalDate?,
  val homeDetentionCurfewActualDate: LocalDate?,
  val updatedByFirstName: String?,
  val updatedByLastName: String?,
  val comUsername: String?,
) {
  val updatedByFullName: String?
    get() = listOfNotNull(updatedByFirstName, updatedByLastName)
      .joinToString(" ")
      .ifBlank { null }
}

private const val LICENCE_CASE = "uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceCase"

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
            l.prisonCode,
            l.prisonDescription,
            l.conditionalReleaseDate,
            l.actualReleaseDate,
            l.licenceStartDate,
            l.postRecallReleaseDate,
            l.homeDetentionCurfewActualDate,
            updatedBy.firstName, updatedBy.lastName,
            com.username
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
  ): List<LicenceCase>
}
