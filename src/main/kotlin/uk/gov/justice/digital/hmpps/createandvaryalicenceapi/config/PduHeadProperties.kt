package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "pdu")
data class PduHeadProperties(
  val contacts: Map<String, EmailConfig> = mapOf()
)

data class EmailConfig(
  val forename: String,
  val surname: String,
  val description: String,
  val email: String,
)
