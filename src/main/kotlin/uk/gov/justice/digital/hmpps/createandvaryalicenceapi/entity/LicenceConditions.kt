package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import com.fasterxml.jackson.annotation.JsonProperty
import com.vladmihalcea.hibernate.type.json.JsonBinaryType
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

interface ILicenceCondition {
  var code: String
}

data class LicencePolicyDto(
  @JsonProperty("version")
  val version: String,
  val standardConditions: StandardConditions,
  val additionalConditions: AdditionalConditions,
)

data class StandardConditions(
  @JsonProperty("AP")
  val ap: List<Ap>,
  @JsonProperty("PSS")
  val pss: List<Pss>,
)

data class Ap(
  override var code: String,
  val text: String,
) : ILicenceCondition

data class Pss(
  override var code: String,
  val text: String,
) : ILicenceCondition

data class AdditionalConditions(
  @JsonProperty("AP")
  val ap: List<AdditionalConditionsAp>,
  @JsonProperty("PSS")
  val pss: List<AdditionalConditionsPss>,
)

data class AdditionalConditionsAp(
  override var code: String,
  val category: String,
  val text: String,
  val tpl: String?,
  val requiresInput: Boolean,
  val inputs: List<Input>?,
  val categoryShort: String?,
  val subtext: String?,
) : ILicenceCondition

data class Input(
  val type: String,
  val label: String,
  val name: String,
  val listType: String?,
  val options: List<Option>?,
  val case: String?,
  val handleIndefiniteArticle: Boolean?,
  val addAnother: AddAnother?,
  val includeBefore: String?,
)

data class Option(
  val value: String,
  val conditional: Conditional?,
)

data class Conditional(
  val inputs: List<ConditionalInput>,
)

data class ConditionalInput(
  val type: String,
  val label: String,
  val name: String,
  val case: String,
  val handleIndefiniteArticle: Boolean,
)

data class AddAnother(
  val label: String,
)

data class AdditionalConditionsPss(
  override var code: String,
  val category: String,
  val text: String,
  val tpl: String,
  val requiresInput: Boolean,
  val pssDates: Boolean?,
  val inputs: List<PassInput>,
) : ILicenceCondition

data class PassInput(
  val type: String,
  val label: String,
  val name: String,
  val includeBefore: String?,
)

@TypeDef(name = "jsonb", typeClass = JsonBinaryType::class)

@Entity
@Table(name = "licence_conditions")
class LicenceConditions {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "version", nullable = false)
  var version: Long? = null

  @Type(type = "jsonb")
  @Column(columnDefinition = "jsonb")
  var conditions: LicencePolicyDto? = null
}
