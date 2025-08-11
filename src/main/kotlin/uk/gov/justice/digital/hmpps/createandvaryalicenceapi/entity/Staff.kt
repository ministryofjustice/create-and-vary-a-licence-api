package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorColumn
import jakarta.persistence.DiscriminatorType
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Inheritance
import jakarta.persistence.InheritanceType
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.address.Address
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions.convertToTitleCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.StaffKind
import java.time.LocalDateTime
import java.util.Objects

@Entity
@Table(name = "staff")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "kind", discriminatorType = DiscriminatorType.STRING)
abstract class Staff(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @field:Positive
  open val id: Long? = null,

  @param:NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "kind", insertable = false, updatable = false)
  var kind: StaffKind,

  @Column(unique = true)
  override var username: String,

  var email: String?,

  override var firstName: String?,

  override var lastName: String?,

  var lastUpdatedTimestamp: LocalDateTime? = null,

  @OneToMany(
    cascade = [CascadeType.ALL],
    fetch = FetchType.LAZY,
    orphanRemoval = true,
  )
  @JoinTable(
    name = "STAFF_SAVED_APPOINTMENT_ADDRESS",
    joinColumns = [JoinColumn(name = "staff_id")],
    inverseJoinColumns = [JoinColumn(name = "address_id")],
    uniqueConstraints = [UniqueConstraint(columnNames = ["staff_id", "address_id"])],
  )
  var savedAppointmentAddresses: MutableSet<Address> = mutableSetOf(),
) : Creator {

  val fullName
    get() = "$firstName $lastName".convertToTitleCase()

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is Staff) return false
    if (id != other.id) return false
    return true
  }

  override fun hashCode(): Int = Objects.hash(id)

  override fun toString(): String = "Staff(" +
    "id=$id, " +
    "kind=$kind, " +
    "username='$username', " +
    "email=$email, " +
    "firstName=$firstName, " +
    "lastName=$lastName, " +
    "lastUpdatedTimestamp=$lastUpdatedTimestamp" +
    ")"
}
