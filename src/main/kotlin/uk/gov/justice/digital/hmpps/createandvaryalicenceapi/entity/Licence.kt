package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.EnumType.STRING
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.IN_PROGRESS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType.AP
import java.time.LocalDate
import java.time.LocalDateTime

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

  var version: String? = null,

  @NotNull
  @Enumerated(STRING)
  var statusCode: LicenceStatus = IN_PROGRESS,

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
  val topupSupervisionStartDate: LocalDate? = null,
  val topupSupervisionExpiryDate: LocalDate? = null,
  val probationAreaCode: String? = null,
  val probationAreaDescription: String? = null,
  val probationPduCode: String? = null,
  val probationPduDescription: String? = null,
  val probationLauCode: String? = null,
  val probationLauDescription: String? = null,
  val probationTeamCode: String? = null,
  val probationTeamDescription: String? = null,
  val appointmentPerson: String? = null,
  val appointmentTime: LocalDateTime? = null,
  val appointmentAddress: String? = null,
  val appointmentContact: String? = null,
  val spoDiscussion: String? = null,
  val vloDiscussion: String? = null,
  val approvedDate: LocalDateTime? = null,
  val approvedByUsername: String? = null,
  val approvedByName: String? = null,
  val supersededDate: LocalDateTime? = null,
  var dateCreated: LocalDateTime? = null,
  val dateLastUpdated: LocalDateTime? = null,
  var updatedByUsername: String? = null,

  @OneToMany(
    mappedBy = "licence",
    fetch = FetchType.EAGER,
    cascade = [CascadeType.ALL],
    orphanRemoval = true,
    targetEntity = StandardCondition::class
  )
  @Fetch(value = FetchMode.SUBSELECT)
  @OrderBy("conditionSequence")
  var standardConditions: List<StandardCondition> = emptyList(),

  @OneToMany(mappedBy = "licence", fetch = FetchType.EAGER, cascade = [CascadeType.ALL], orphanRemoval = true)
  @Fetch(value = FetchMode.SUBSELECT)
  @OrderBy("conditionSequence")
  val additionalConditions: List<AdditionalCondition> = emptyList(),

  @OneToMany(mappedBy = "licence", fetch = FetchType.EAGER, cascade = [CascadeType.ALL], orphanRemoval = true)
  @Fetch(value = FetchMode.SUBSELECT)
  @OrderBy("conditionSequence")
  val bespokeConditions: List<BespokeCondition> = emptyList(),

  @ManyToOne
  @JoinColumn(name = "responsible_com_id", nullable = false)
  var responsibleCom: CommunityOffenderManager? = null,

  @ManyToOne
  @JoinColumn(name = "submitted_by_com_id", nullable = true)
  var submittedBy: CommunityOffenderManager? = null,

  @ManyToOne
  @JoinColumn(name = "created_by_com_id", nullable = false)
  var createdBy: CommunityOffenderManager? = null,

  @ManyToMany
  @JoinTable(
    name = "community_offender_manager_licence_mailing_list",
    joinColumns = [JoinColumn(name = "licence_id")],
    inverseJoinColumns = [JoinColumn(name = "community_offender_manager_id")]
  )
  val mailingList: MutableSet<CommunityOffenderManager> = mutableSetOf(),

  var variationOfId: Long? = null,
  var versionOfId: Long? = null,

) {
  fun copyLicence(newStatus: LicenceStatus): Licence {
    return Licence(
      typeCode = this.typeCode,
      version = this.version,
      statusCode = newStatus,
      nomsId = this.nomsId,
      bookingNo = this.bookingNo,
      bookingId = this.bookingId,
      crn = this.crn,
      pnc = this.pnc,
      cro = this.cro,
      prisonCode = this.prisonCode,
      prisonDescription = this.prisonDescription,
      prisonTelephone = this.prisonTelephone,
      forename = this.forename,
      middleNames = this.middleNames,
      surname = this.surname,
      dateOfBirth = this.dateOfBirth,
      conditionalReleaseDate = this.conditionalReleaseDate,
      actualReleaseDate = this.actualReleaseDate,
      sentenceStartDate = this.sentenceStartDate,
      sentenceEndDate = this.sentenceEndDate,
      licenceStartDate = this.licenceStartDate,
      licenceExpiryDate = this.licenceExpiryDate,
      topupSupervisionStartDate = this.topupSupervisionStartDate,
      topupSupervisionExpiryDate = this.topupSupervisionExpiryDate,
      probationAreaCode = this.probationAreaCode,
      probationAreaDescription = this.probationAreaDescription,
      probationPduCode = this.probationPduCode,
      probationPduDescription = this.probationPduDescription,
      probationLauCode = this.probationLauCode,
      probationLauDescription = this.probationLauDescription,
      probationTeamCode = this.probationTeamCode,
      probationTeamDescription = this.probationTeamDescription,
      appointmentPerson = this.appointmentPerson,
      appointmentTime = this.appointmentTime,
      appointmentAddress = this.appointmentAddress,
      appointmentContact = this.appointmentContact,
      responsibleCom = this.responsibleCom,
      dateCreated = LocalDateTime.now()
    )
  }
}
