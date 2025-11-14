package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.timeserved

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.timeserved.CommunicationMethod
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.timeserved.ContactStatus

@ValidOtherCommunication
data class TimeServedProbationConfirmContactRequest(

  @JsonProperty("contactStatus")
  @field:NotNull(message = "Confirm if you have contacted the probation team")
  private val _contactStatus: ContactStatus? = null,

  @field:NotEmpty(message = "Choose a form of communication")
  val communicationMethods: List<CommunicationMethod>,

  val otherCommunicationDetail: String? = null,
) {
  val contactStatus: ContactStatus
    get() = _contactStatus!!
}
