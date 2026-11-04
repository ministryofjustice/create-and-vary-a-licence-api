package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.additionalConditions

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonTypeName
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@JsonTypeName(ConditionTypes.STANDARD)
@Schema(
  description = "Describes an instance of a additional condition on the licence",
)
data class GenericAdditionalCondition(
  @field:Schema(description = "The ID of the condition", example = "123456")
  override val id: Long,

  @field:Schema(
    description = "The category to which the additional condition belongs",
    example = "Residence at a specific place",
  )
  override val category: String,

  @field:Schema(description = "Discriminator for condition type", example = ConditionTypes.STANDARD)
  override val type: String = ConditionTypes.STANDARD,

  @field:Schema(
    description = "The code shared by all conditions of this type",
    example = "5a105297-dce1-4d18-b9ea-4195b46b7594",
  )
  override val code: String,

  @field:Schema(
    description = "A combination of inputted text and template text including any user input",
    example = "You must not enter the location X",
  )
  override val text: String,

  @field:Schema(
    description = "The electronic monitoring end date for conditions 14b (524f2fd6-ad53-47dd-8edc-2161d3dd2ed4) and 14e (2F8A5418-C6E4-4F32-9E58-64B23550E504). May be null or omitted when not entered. Omitted for other STANDARD conditions.",
    example = "2028-01-16",
    type = "string",
    format = "date",
    nullable = true,
    requiredMode = Schema.RequiredMode.NOT_REQUIRED,
  )
  @field:JsonFormat(pattern = "yyyy-MM-dd")
  @field:JsonInclude(JsonInclude.Include.NON_NULL)
  val endDate: LocalDate? = null,

) : AdditionalCondition
