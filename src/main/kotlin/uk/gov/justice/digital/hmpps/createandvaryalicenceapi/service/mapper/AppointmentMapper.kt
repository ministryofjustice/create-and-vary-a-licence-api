package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.mapper

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Appointment

@Component
class AppointmentMapper {

  companion object {

    fun copy(entity: Appointment?): Appointment? {
      if (entity == null) return null

      return Appointment(
        id = null,
        personType = entity.personType,
        person = entity.person,
        timeType = entity.timeType,
        time = entity.time,
        telephoneContactNumber = entity.telephoneContactNumber,
        alternativeTelephoneContactNumber = entity.alternativeTelephoneContactNumber,
        addressText = entity.addressText,
        address = AddressMapper.copy(entity.address),
      )
    }
  }
}
