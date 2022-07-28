package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.OmuContact
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateOmuEmailRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.OmuContactRepository
import java.time.LocalDateTime
import javax.persistence.EntityNotFoundException

@Service
class OmuService(private val omuRepository: OmuContactRepository) {
  /**
   * Get OMU email address belonging to the prison code
   */
  fun getOmuContactEmail(prisonCode: String): OmuContact? {
    return omuRepository.findByPrisonCode(prisonCode)
  }

  /**
   * Create or update OMU contact email address belonging to the prison code
   */
  fun updateOmuEmail(prisonCode: String, contactRequest: UpdateOmuEmailRequest): OmuContact {
    return this.omuRepository.saveAndFlush(
      this.omuRepository.findByPrisonCode(prisonCode)?.copy(
        email = contactRequest.email,
        dateLastUpdated = LocalDateTime.now()
      )
        ?: OmuContact(
          prisonCode = prisonCode,
          email = contactRequest.email,
          dateCreated = LocalDateTime.now()
        )
    )
  }

  /**
   * delete an OMU contact email address if no longer valid
   */
  fun deleteOmuEmail(prisonCode: String) {
    omuRepository.findByPrisonCode(prisonCode)?.let { this.omuRepository.delete(it) } ?: throw EntityNotFoundException(
      prisonCode
    )
  }
}
