package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.IN_PROGRESS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType.AP
import java.time.LocalDate
import java.time.LocalDateTime
import javax.persistence.Entity
import javax.persistence.EnumType.STRING
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.OneToMany
import javax.persistence.OrderBy
import javax.persistence.Table
import javax.validation.constraints.NotNull

@Entity
@Table(name = "licence")
data class Licence(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @NotNull
  val id: Long = -1,

  @NotNull
  @Enumerated(STRING)
  val typeCode: LicenceType = AP,

  val version: String? = null,

  @NotNull
  @Enumerated(STRING)
  val statusCode: LicenceStatus = IN_PROGRESS,

  val nomsId: String? = null,
  val bookingNo: String? = null,
  val bookingId: Long? = null,
  val crn: String? = null,
  val pnc: String? = null,
  val cro: String? = null,
  val prisonCode: String? = null,
  val prisonDescription: String? = null,
  val prisonTelephone: String? = null,
  val forename: String? = null,
  val middleNames: String? = null,
  val surname: String? = null,
  val dateOfBirth: LocalDate? = null,
  val conditionalReleaseDate: LocalDate? = null,
  val actualReleaseDate: LocalDate? = null,
  val sentenceStartDate: LocalDate? = null,
  val sentenceEndDate: LocalDate? = null,
  val licenceStartDate: LocalDate? = null,
  val licenceExpiryDate: LocalDate? = null,
  val comFirstName: String? = null,
  val comLastName: String? = null,
  val comUsername: String? = null,
  val comStaffId: Long? = null,
  val comEmail: String? = null,
  val comTelephone: String? = null,
  val probationAreaCode: String? = null,
  val probationLduCode: String? = null,
  val appointmentPerson: String? = null,
  val appointmentDate: LocalDate? = null,
  val appointmentTime: String? = null,
  val appointmentAddress: String? = null,
  val approvedDate: LocalDateTime? = null,
  val approvedByUsername: String? = null,
  val supersededDate: LocalDateTime? = null,
  val dateCreated: LocalDateTime? = null,
  val createByUsername: String? = null,
  val dateLastUpdated: LocalDateTime? = null,
  val updatedByUsername: String? = null,

  @JoinColumn(name = "licenceId")
  @OrderBy("termSequence")
  @OneToMany
  val standardTerms: List<StandardTerm> = ArrayList(),

  @JoinColumn(name = "licenceId")
  @OrderBy("termSequence")
  @OneToMany
  val additionalTerms: List<AdditionalTerm> = ArrayList(),

  @JoinColumn(name = "licenceId")
  @OrderBy("termSequence")
  @OneToMany
  val bespokeTerms: List<BespokeTerm> = ArrayList(),
)
