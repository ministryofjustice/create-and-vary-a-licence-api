package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.address.Address
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AppointmentPersonType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AppointmentTimeType
import java.time.LocalDateTime

@Entity
@Table(name = "APPOINTMENT")
class Appointment(

  id: Long? = null,
  @Enumerated(EnumType.STRING)
  var personType: AppointmentPersonType? = null,
  var person: String? = null,
  @Enumerated(EnumType.STRING)
  var timeType: AppointmentTimeType? = null,
  var time: LocalDateTime? = null,
  var telephoneContactNumber: String? = null,
  var alternativeTelephoneContactNumber: String? = null,
  var addressText: String? = null,

  @OneToOne(
    cascade = [CascadeType.ALL],
    fetch = FetchType.LAZY,
    orphanRemoval = true,
  )
  @JoinTable(
    name = "APPOINTMENT_ADDRESS",
    joinColumns = [JoinColumn(name = "appointment_id")],
    inverseJoinColumns = [JoinColumn(name = "address_id")],
    uniqueConstraints = [UniqueConstraint(columnNames = ["appointment_id", "address_id"])],
  )
  var address: Address? = null,
  var dateCreated: LocalDateTime? = LocalDateTime.now(),
  var dateLastUpdated: LocalDateTime? = null,
) : AbstractIdEntity(idInternal = id)
