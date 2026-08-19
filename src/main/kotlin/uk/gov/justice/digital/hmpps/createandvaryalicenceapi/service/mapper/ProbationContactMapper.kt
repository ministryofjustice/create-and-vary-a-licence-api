package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.mapper

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.ProbationContact

@Component
class ProbationContactMapper {

  companion object {

    fun copy(entity: ProbationContact?): ProbationContact? {
      if (entity == null) return null

      return ProbationContact(
        id = null,
        appointmentType = entity.appointmentType,
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
