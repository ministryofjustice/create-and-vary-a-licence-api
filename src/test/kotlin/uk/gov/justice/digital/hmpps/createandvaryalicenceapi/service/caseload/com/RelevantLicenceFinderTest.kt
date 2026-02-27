package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.com

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceCreationType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceCreationType.LICENCE_CHANGES_NOT_APPROVED_IN_TIME
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceCreationType.LICENCE_CREATED_BY_PRISON
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceCreationType.LICENCE_IN_PROGRESS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceCreationType.LICENCE_NOT_STARTED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceCreationType.PRISON_WILL_CREATE_THIS_LICENCE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.com.RelevantLicenceFinder.findRelevantLicencePerCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.HARD_STOP
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.APPROVED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.IN_PROGRESS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.NOT_STARTED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.TIMED_OUT
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType

class RelevantLicenceFinderTest {
  @Test
  fun `Hard stop case for not started licence returns PRISON_WILL_CREATE_THIS_LICENCE and TIMED_OUT`() {
    val hardStop = createLicenceSummary(licenceId = null, kind = HARD_STOP, licenceStatus = IN_PROGRESS)

    val result = findRelevantLicencePerCase(listOf(hardStop))

    assertThat(result.licenceStatus).isEqualTo(TIMED_OUT)
    assertThat(result.licenceCreationType).isEqualTo(PRISON_WILL_CREATE_THIS_LICENCE)
  }

  @Test
  fun `Time served case for not started licence returns PRISON_WILL_CREATE_THIS_LICENCE and TIMED_OUT`() {
    val timeServed = createLicenceSummary(licenceId = null, kind = LicenceKind.TIME_SERVED, licenceStatus = IN_PROGRESS)

    val result = findRelevantLicencePerCase(listOf(timeServed))

    assertThat(result.licenceStatus).isEqualTo(TIMED_OUT)
    assertThat(result.licenceCreationType).isEqualTo(PRISON_WILL_CREATE_THIS_LICENCE)
  }

  @Test
  fun `Hard stop case with existing licence returns LICENCE_CREATED_BY_PRISON and TIMED_OUT`() {
    val hardStop = createLicenceSummary(licenceId = 2L, kind = HARD_STOP, licenceStatus = APPROVED)

    val result = findRelevantLicencePerCase(listOf(hardStop))

    assertThat(result.licenceStatus).isEqualTo(TIMED_OUT)
    assertThat(result.licenceCreationType).isEqualTo(LICENCE_CREATED_BY_PRISON)
  }

  @Test
  fun `Time served case with existing licence returns LICENCE_CREATED_BY_PRISON and TIMED_OUT`() {
    val timeServed = createLicenceSummary(licenceId = 2L, kind = LicenceKind.TIME_SERVED, licenceStatus = APPROVED)

    val result = findRelevantLicencePerCase(listOf(timeServed))

    assertThat(result.licenceStatus).isEqualTo(TIMED_OUT)
    assertThat(result.licenceCreationType).isEqualTo(LICENCE_CREATED_BY_PRISON)
  }

  @Test
  fun `Timed out case with previously approved licence returns LICENCE_CHANGES_NOT_APPROVED_IN_TIME`() {
    val timedOut = createLicenceSummary(licenceId = 3L, licenceStatus = TIMED_OUT, versionOf = 2L)
    val approved = createLicenceSummary(licenceId = 2L, licenceStatus = APPROVED)

    val result = findRelevantLicencePerCase(listOf(timedOut, approved))

    assertThat(result.licenceStatus).isEqualTo(TIMED_OUT)
    assertThat(result.licenceCreationType).isEqualTo(LICENCE_CHANGES_NOT_APPROVED_IN_TIME)
  }

  @Test
  fun `Timed out case with no previously approved licence returns PRISON_WILL_CREATE_THIS_LICENCE`() {
    val timedOut = createLicenceSummary(licenceId = 3L, licenceStatus = TIMED_OUT)

    val result = findRelevantLicencePerCase(listOf(timedOut))

    assertThat(result.licenceStatus).isEqualTo(TIMED_OUT)
    assertThat(result.licenceCreationType).isEqualTo(PRISON_WILL_CREATE_THIS_LICENCE)
  }

  @Test
  fun `An in progress case with more than one licence will return the non-approved version`() {
    val inProgress = createLicenceSummary(licenceId = 4L, licenceStatus = IN_PROGRESS)
    val approved = createLicenceSummary(licenceId = 5L, licenceStatus = APPROVED)

    val result = findRelevantLicencePerCase(listOf(inProgress, approved))

    assertThat(result.licenceStatus).isEqualTo(IN_PROGRESS)
    assertThat(result.licenceCreationType).isEqualTo(LICENCE_IN_PROGRESS)
  }

  @Test
  fun `A not started licence will return the not started case`() {
    val notStarted = createLicenceSummary(licenceId = null, licenceStatus = NOT_STARTED)

    val result = findRelevantLicencePerCase(listOf(notStarted))

    assertThat(result.licenceStatus).isEqualTo(NOT_STARTED)
    assertThat(result.licenceCreationType).isEqualTo(LICENCE_NOT_STARTED)
  }

  @Test
  fun `An in progress case will return the not started case`() {
    val inprogress = createLicenceSummary(licenceId = 1, licenceStatus = IN_PROGRESS)

    val result = findRelevantLicencePerCase(listOf(inprogress))

    assertThat(result.licenceStatus).isEqualTo(IN_PROGRESS)
    assertThat(result.licenceCreationType).isEqualTo(LICENCE_IN_PROGRESS)
  }

  private fun createLicenceSummary(
    licenceId: Long? = 1L,
    licenceStatus: LicenceStatus = IN_PROGRESS,
    kind: LicenceKind = LicenceKind.CRD,
    licenceType: LicenceType = LicenceType.AP,
    crn: String? = "CRN1",
    nomisId: String = "NOMIS1",
    name: String = "John Doe",
    versionOf: Long? = null,
    licenceCreationType: LicenceCreationType? = null,
  ) = ComCreateCaseloadLicenceDto(
    licenceId = licenceId,
    licenceStatus = licenceStatus,
    kind = kind,
    licenceType = licenceType,
    crn = crn,
    nomisId = nomisId,
    name = name,
    versionOf = versionOf,
    licenceCreationType = licenceCreationType,
    isReviewNeeded = false,
    releaseDate = null,
    isRestricted = false,
  )
}
