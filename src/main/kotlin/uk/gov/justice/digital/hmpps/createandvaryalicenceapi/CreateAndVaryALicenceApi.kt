package uk.gov.justice.digital.hmpps.createandvaryalicenceapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class CreateAndVaryALicenceApi

fun main(args: Array<String>) {
  runApplication<CreateAndVaryALicenceApi>(*args)
}
