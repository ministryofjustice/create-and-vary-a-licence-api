package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.mapper

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.address.Address
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.AddAddressRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response.AddressResponse
import java.util.UUID

@Component
object AddressMapper {
  fun copy(entity: Address?): Address? = entity?.copy(
    id = -1,
    reference = UUID.randomUUID().toString(),
  )

  fun toEntity(request: AddAddressRequest): Address = Address(
    reference = UUID.randomUUID().toString(),
    uprn = request.uprn,
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
