package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.SentenceDateHolder
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions.convertToTitleCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate
import java.time.LocalDateTime

interface LicenceCase : SentenceDateHolder {
  val kind: LicenceKind
  val licenceId: Long
  val versionOfId: Long?
  val licenceStatus: LicenceStatus
  val prisonNumber: String
  val surname: String?
  val forename: String?
  val updatedByFirstName: String?
  val updatedByLastName: String?
  val comUsername: String?

  val updatedByFullName: String?
    get() = listOfNotNull(updatedByFirstName, updatedByLastName)
      .joinToString(" ")
      .ifBlank { null }

  fun isReviewNeeded(): Boolean = kind == LicenceKind.HARD_STOP &&
    licenceStatus == LicenceStatus.ACTIVE &&
    (this as? LicenceComCase)?.reviewDate == null

  val fullName: String
    get() = listOfNotNull(forename, surname)
      .joinToString(" ")
      .convertToTitleCase()
      .trim()
}

data class LicenceCaCase(
  override val kind: LicenceKind,
  override val licenceId: Long,
  override val versionOfId: Long?,
  override val licenceStatus: LicenceStatus,
  override val prisonNumber: String,
  override val surname: String?,
  override val forename: String?,
  override val conditionalReleaseDate: LocalDate?,
  override val actualReleaseDate: LocalDate?,
  override val licenceStartDate: LocalDate?,
  override val postRecallReleaseDate: LocalDate?,
  override val homeDetentionCurfewActualDate: LocalDate?,
  override val updatedByFirstName: String?,
  override val updatedByLastName: String?,
  override val comUsername: String?,
  val prisonCode: String?,
  val prisonDescription: String?,
) : LicenceCase

data class LicenceComCase(
  override val kind: LicenceKind,
  override val licenceId: Long,
  override val versionOfId: Long?,
  override val licenceStatus: LicenceStatus,
  override val prisonNumber: String,
  override val surname: String?,
  override val forename: String?,
  override val conditionalReleaseDate: LocalDate?,
  override val actualReleaseDate: LocalDate?,
  override val licenceStartDate: LocalDate?,
  override val postRecallReleaseDate: LocalDate?,
  override val homeDetentionCurfewActualDate: LocalDate?,
  override val updatedByFirstName: String?,
  override val updatedByLastName: String?,
  override val comUsername: String?,
  val typeCode: LicenceType,
  val reviewDate: LocalDateTime?,
  val crn: String?,
  val dateCreated: LocalDateTime,
  val approvedByName: String?,
  val approvedDate: LocalDateTime?,
) : LicenceCase

private const val LICENCE_CASE = "uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceCaCase"
private const val LICENCE_COM_CASE = "uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceComCase"

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
