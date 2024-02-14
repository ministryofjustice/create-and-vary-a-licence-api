package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AppointmentPersonType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AppointmentTimeType
import java.time.LocalDateTime
import kotlin.reflect.full.memberProperties

class VariationLicenceTest {

  @Test
  fun checkCopyMethod() {
    val submittedDate = LocalDateTime.now()
    val appointmentTime = LocalDateTime.now().plusDays(1)
    val appointmentTimeType = AppointmentTimeType.SPECIFIC_DATE_TIME
    val approvedDate = LocalDateTime.now().plusDays(2)
    val dateLastUpdated = LocalDateTime.now().plusDays(3)
    val licenceActivatedDate = LocalDateTime.now().plusDays(4)
    val supersededDate = LocalDateTime.now().plusDays(5)

    val variationLicence = TestData.createVariationLicence()
      .copy(
        licenceVersion = "1.2",
        spoDiscussion = "yes1",
        submittedDate = submittedDate,
        variationOfId = 1L,
        vloDiscussion = "Yes 2",
        appointmentAddress = "some address",
        appointmentTime = appointmentTime,
        appointmentTimeType = appointmentTimeType,
        submittedBy = TestData.com(),
        appointmentContact = "appointmentContact",
        appointmentPersonType = AppointmentPersonType.SPECIFIC_PERSON,
        appointmentPerson = "appointmentPerson",
        approvedByName = "approvedByName",
        approvedByUsername = "approvedByUsername",
        approvedDate = approvedDate,
        dateLastUpdated = dateLastUpdated,
        licenceActivatedDate = licenceActivatedDate,
        middleNames = "middleNames",
        prisonTelephone = "prisonTelephone",
        supersededDate = supersededDate,
        updatedByUsername = "updatedByUsername",
      )

    VariationLicence::class.memberProperties.forEach {
      if (it.get(variationLicence) == null) {
        fail { "${it.name} does not have a value set - needs to be set to test copy" }
      }
    }

    val copy = variationLicence.copy()

    val incorrectlyCopiedItems = VariationLicence::class.memberProperties
      .filter { it.get(variationLicence) != it.get(copy) }
      .map { it.name }

    assertThat(incorrectlyCopiedItems).isEmpty()
  }
}
