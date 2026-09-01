package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AppointmentTimeType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AppointmentType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AppointmentType.NO_APPOINTMENT_NEEDED

class ProbationContactTest {

  @ParameterizedTest
  @EnumSource(AppointmentType::class, names = ["NO_APPOINTMENT_NEEDED"], mode = EnumSource.Mode.EXCLUDE)
  fun `is missing appointment time when appointment time is null and an appointment is required`(appointmentType: AppointmentType) {
    val probationContact = ProbationContact(
      appointmentType = appointmentType,
      appointmentTimeType = null,
    )

    assertThat(probationContact.isMissingAppointmentTime()).isTrue
  }

  @Test
  fun `is not missing appointment time when appointment time is set and an appointment is required`() {
    val probationContact = ProbationContact(
      appointmentType = AppointmentType.SPECIFIC_PERSON,
      appointmentTimeType = AppointmentTimeType.IMMEDIATE_UPON_RELEASE,
    )

    assertThat(probationContact.isMissingAppointmentTime()).isFalse
  }

  @Test
  fun `is not missing appointment time when no appointment is needed even if appointment time is null`() {
    val probationContact = ProbationContact(
      appointmentType = NO_APPOINTMENT_NEEDED,
      appointmentTimeType = null,
    )

    assertThat(probationContact.isMissingAppointmentTime()).isFalse
  }
}
