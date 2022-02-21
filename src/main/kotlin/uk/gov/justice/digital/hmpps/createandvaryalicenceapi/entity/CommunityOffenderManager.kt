package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import java.time.LocalDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.ManyToMany
import javax.persistence.OneToMany
import javax.persistence.Table
import javax.validation.constraints.NotNull

@Entity
@Table(name = "community_offender_manager")
data class CommunityOffenderManager(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @NotNull
  val id: Long = -1,

  @Column(unique = true)
  val staffIdentifier: Long,

  @Column(unique = true)
  val username: String,

  val email: String?,

  val firstName: String?,

  val lastName: String?,

  @OneToMany(mappedBy = "responsibleCom")
  val licencesResponsibleFor: List<Licence> = emptyList(),

  @OneToMany(mappedBy = "createdBy")
  val licencesCreated: List<Licence> = emptyList(),

  @OneToMany(mappedBy = "submittedBy")
  val licencesSubmitted: List<Licence> = emptyList(),

  @ManyToMany(mappedBy = "mailingList")
  val licencesSubscribedTo: List<Licence> = emptyList(),

  val lastUpdatedTimestamp: LocalDateTime? = null,
)
