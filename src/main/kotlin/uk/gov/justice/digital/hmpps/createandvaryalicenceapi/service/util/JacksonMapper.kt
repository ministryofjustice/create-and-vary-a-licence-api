package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.util

import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinFeature
import tools.jackson.module.kotlin.KotlinModule

fun createMapper(): ObjectMapper = JsonMapper.builder()
  .findAndAddModules()
  .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
  .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
  .addModule(
    KotlinModule.Builder()
      .configure(KotlinFeature.StrictNullChecks, false)
      .build(),
  )
  .build()
