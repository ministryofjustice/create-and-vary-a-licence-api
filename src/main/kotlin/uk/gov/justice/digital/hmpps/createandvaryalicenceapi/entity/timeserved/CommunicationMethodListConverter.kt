package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.timeserved

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
class CommunicationMethodListConverter : AttributeConverter<List<CommunicationMethod>, String> {

  override fun convertToDatabaseColumn(attribute: List<CommunicationMethod>?): String? = attribute?.joinToString(",") { it.name }

  override fun convertToEntityAttribute(dbData: String?): List<CommunicationMethod> = dbData
    ?.split(",")
    ?.map { CommunicationMethod.valueOf(it) }
    ?: emptyList()
}
