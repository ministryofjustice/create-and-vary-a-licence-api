package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.OmuContact
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateOmuEmailRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.OmuContactRepository
import java.time.LocalDateTime

@Service
class OmuService(private val omuRepository: OmuContactRepository) {
  /**
   * Get OMU email address belonging to the prison code
   */
  fun getOmuContactEmail(prisonCode: String): OmuContact? = omuRepository.findByPrisonCode(prisonCode)

  /**
   * Create or update OMU contact email address belonging to the prison code
   */
  @Transactional
  fun updateOmuEmail(prisonCode: String, contactRequest: UpdateOmuEmailRequest): OmuContact {
    val contact = this.omuRepository.findByPrisonCode(prisonCode)
    contact?.let {
      it.email = contactRequest.email
      it.dateLastUpdated = LocalDateTime.now()
      return it
    }

    return this.omuRepository.saveAndFlush(
      OmuContact(
        prisonCode = prisonCode,
        email = contactRequest.email,
        dateCreated = LocalDateTime.now(),
      ),
    )
  }

  /**
   * delete an OMU contact email address if no longer valid
   */
  fun deleteOmuEmail(prisonCode: String) {
    omuRepository.findByPrisonCode(prisonCode)?.let { this.omuRepository.delete(it) } ?: throw EntityNotFoundException(
      prisonCode,
    )
  }
}
