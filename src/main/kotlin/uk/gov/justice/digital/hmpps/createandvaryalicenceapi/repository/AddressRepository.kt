package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.address.Address

@Repository
interface AddressRepository :
  JpaRepository<Address, Long>,
  JpaSpecificationExecutor<Address> {

  fun findByReference(reference: String): Address?
}
