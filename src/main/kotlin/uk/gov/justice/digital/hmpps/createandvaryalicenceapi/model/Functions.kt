package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import org.springframework.core.ParameterizedTypeReference

inline fun <reified T> typeReference() = object : ParameterizedTypeReference<T>() {}
