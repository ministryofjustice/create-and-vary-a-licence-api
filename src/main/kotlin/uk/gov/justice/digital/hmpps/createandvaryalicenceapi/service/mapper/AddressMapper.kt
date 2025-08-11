package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.mapper

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.address.Address
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.AddAddressRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response.AddressResponse
import java.time.LocalDateTime
import java.util.UUID

@Component
class AddressMapper {

  companion object {

    @JvmStatic
    fun copy(entity: Address?): Address? {
      if (entity == null) return null

      return Address(
        id = null,
        reference = UUID.randomUUID().toString(),
        uprn = entity.uprn,
        firstLine = entity.firstLine,
        secondLine = entity.secondLine,
        townOrCity = entity.townOrCity,
        county = entity.county,
        postcode = entity.postcode,
        source = entity.source,
        createdTimestamp = LocalDateTime.now(),
        lastUpdatedTimestamp = LocalDateTime.now(),
      )
    }

    @JvmStatic
    fun toResponse(entity: Address): AddressResponse = AddressResponse(
      reference = entity.reference,
      uprn =  entity.uprn,
      firstLine = entity.firstLine,
      secondLine = entity.secondLine,
      townOrCity = entity.townOrCity,
      county = entity.county,
      postcode = entity.postcode,
      source = entity.source,
    )
  }

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

  @Transactional
  fun update(entity: Address, request: AddAddressRequest) {
    with(entity) {
      uprn = request.uprn
      firstLine = request.firstLine
      secondLine = request.secondLine
      townOrCity = request.townOrCity
      county = request.county
      postcode = request.postcode
      source = request.source
      lastUpdatedTimestamp = LocalDateTime.now()
    }
  }
}
