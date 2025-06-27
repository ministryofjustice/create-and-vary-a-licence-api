package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.mapper

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.address.Address
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.AddAddressRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response.AddressResponse
import java.util.UUID

@Component
class AddressMapper {

  companion object {

    fun toEntity(request: AddAddressRequest): Address = Address(
      reference = request.reference ?: UUID.randomUUID().toString(),
      firstLine = request.firstLine,
      secondLine = request.secondLine,
      townOrCity = request.townOrCity,
      county = request.county,
      postcode = request.postcode,
      source = request.source,
    )

    fun toResponse(entity: Address): AddressResponse = AddressResponse(
      reference = entity.reference,
      firstLine = entity.firstLine,
      secondLine = entity.secondLine,
      townOrCity = entity.townOrCity,
      county = entity.county,
      postcode = entity.postcode,
      source = entity.source,
    )
  }
}
