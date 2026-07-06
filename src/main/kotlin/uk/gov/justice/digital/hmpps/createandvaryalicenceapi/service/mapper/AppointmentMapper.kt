package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.mapper

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.ProbationContact

@Component
class AppointmentMapper {

  companion object {

    fun copy(entity: ProbationContact?): ProbationContact? {
      if (entity == null) return null

      return ProbationContact(
        id = null,
        personType = entity.personType,
        person = entity.person,
        appointmentTimeType = entity.appointmentTimeType,
        appointmentTime = entity.appointmentTime,
        telephoneContactNumber = entity.telephoneContactNumber,
        alternativeTelephoneContactNumber = entity.alternativeTelephoneContactNumber,
        addressText = entity.addressText,
        address = AddressMapper.copy(entity.address),
      )
    }
  }
}
