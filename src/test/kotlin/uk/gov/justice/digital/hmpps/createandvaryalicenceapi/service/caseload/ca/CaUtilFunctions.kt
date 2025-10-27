package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ca

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.time.LocalDate

fun createLicenceCase(
  kind: LicenceKind = LicenceKind.CRD,
  licenceId: Long = 1L,
  versionOfId: Long? = null,
  licenceStatus: LicenceStatus = LicenceStatus.IN_PROGRESS,
  nomisId: String = "A1234AA",
  surname: String? = "One",
  forename: String? = "Person",
  prisonCode: String? = "BAI",
  prisonDescription: String? = "Moorland (HMP)",
  conditionalReleaseDate: LocalDate? = LocalDate.of(2021, 10, 22),
  actualReleaseDate: LocalDate? = LocalDate.of(2021, 10, 22),
  licenceStartDate: LocalDate? = LocalDate.of(2021, 10, 22),
  postRecallReleaseDate: LocalDate? = null,
  updatedByFirstName: String? = "X",
  updatedByLastName: String? = "Y",
  comUsername: String? = "com-user",
  homeDetentionCurfewActualDate: LocalDate? = null,
) = LicenceCase(
  kind = kind,
  licenceId = licenceId,
  versionOfId = versionOfId,
  licenceStatus = licenceStatus,
  prisonNumber = nomisId,
  surname = surname,
  forename = forename,
  prisonCode = prisonCode,
  prisonDescription = prisonDescription,
  conditionalReleaseDate = conditionalReleaseDate,
  actualReleaseDate = actualReleaseDate,
  licenceStartDate = licenceStartDate,
  postRecallReleaseDate = postRecallReleaseDate,
  updatedByFirstName = updatedByFirstName,
  updatedByLastName = updatedByLastName,
  comUsername = comUsername,
  homeDetentionCurfewActualDate = homeDetentionCurfewActualDate,
)
